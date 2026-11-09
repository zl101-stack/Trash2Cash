package edu.uph.m24si2.trash2cash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.Query

/**
 * RiwayatActivity — gabungkan dua sumber data:
 *
 * 1. RTDB  → aktivitas botol (diterima / ditolak) — ditulis ESP32
 * 2. Firestore → riwayat penarikan saldo — ditulis TarikSaldoActivity
 *
 * Keduanya digabung, diurutkan by timestamp terbaru, lalu dirender.
 */
class RiwayatActivity : AppCompatActivity() {

    private lateinit var tabSemua: TextView
    private lateinit var tabDiterima: TextView
    private lateinit var tabDitolak: TextView
    private lateinit var containerRiwayat: LinearLayout
    private lateinit var scrollRiwayat: View
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvTotalDiterima: TextView
    private lateinit var tvTotalDitolak: TextView
    private lateinit var tvTotalPenarikan: TextView

    private var allItems  = listOf< RiwayatItem>()
    private var activeTab = "semua"

    // RTDB listener
    private var rtdbListener: ValueEventListener? = null

    // Cache masing-masing sumber
    private var itemsRtdb       = listOf<RiwayatItem>()
    private var itemsFirestore  = listOf<RiwayatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_riwayat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }
        initViews()
        setupTabs()
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        loadFromFirestore()  // Firestore: riwayat penarikan (sekali)
        listenFromRtdb()     // RTDB: aktivitas botol (realtime)
    }

    override fun onDestroy() {
        super.onDestroy()
        rtdbListener?.let { FirebaseManager.rtdbAktivitasRef().removeEventListener(it) }
    }

    private fun initViews() {
        tabSemua         = findViewById(R.id.tabSemua)
        tabDiterima      = findViewById(R.id.tabDiterima)
        tabDitolak       = findViewById(R.id.tabDitolak)
        containerRiwayat = findViewById(R.id.containerRiwayat)
        scrollRiwayat    = findViewById(R.id.scrollRiwayat)
        layoutEmpty      = findViewById(R.id.layoutEmpty)
        progressLoading  = findViewById(R.id.progressLoading)
        tvTotalDiterima  = findViewById(R.id.tvTotalDiterima)
        tvTotalDitolak   = findViewById(R.id.tvTotalDitolak)
        tvTotalPenarikan = findViewById(R.id.tvTotalPenarikan)
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener    { setActiveTab("semua") }
        tabDiterima.setOnClickListener { setActiveTab("diterima") }
        tabDitolak.setOnClickListener  { setActiveTab("ditolak") }
    }

    // ── RTDB: listen aktivitas botol dari ESP32 ──────────────────────
    private fun listenFromRtdb() {
        rtdbListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val list = mutableListOf<RiwayatItem>()
                snap.children.forEach { child ->
                    list.add(RiwayatItem(
                        judul     = child.child("judul").getValue(String::class.java) ?: "",
                        deskripsi = child.child("deskripsi").getValue(String::class.java) ?: "",
                        waktu     = child.child("waktu").getValue(String::class.java) ?: "",
                        status    = child.child("status").getValue(String::class.java) ?: "diterima",
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    ))
                }
                // Urutkan terbaru dulu
                itemsRtdb = list.sortedByDescending { it.timestamp }
                mergeAndRender()
            }
            override fun onCancelled(e: DatabaseError) { mergeAndRender() }
        }
        FirebaseManager.rtdbAktivitasRef()
            .orderByChild("timestamp")
            .addValueEventListener(rtdbListener!!)
    }

    // ── Firestore: ambil riwayat penarikan ───────────────────────────
    private fun loadFromFirestore() {
        FirebaseManager.fsWithdrawalsCol()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val nominal   = doc.getLong("nominal") ?: return@mapNotNull null
                    val ewallet   = doc.getString("ewallet") ?: ""
                    val nomor     = doc.getString("nomor") ?: ""
                    RiwayatItem(
                        judul     = "-Rp ${FirebaseManager.formatRupiah(nominal)}",
                        deskripsi = "Penarikan ke $ewallet ($nomor)",
                        waktu     = doc.getString("waktu") ?: "",
                        status    = "penarikan",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
                itemsFirestore = list
                mergeAndRender()
            }
            .addOnFailureListener { mergeAndRender() }
    }

    // ── Gabungkan RTDB + Firestore, urutkan by timestamp ────────────
    private fun mergeAndRender() {
        progressLoading.visibility = View.GONE

        val merged = (itemsRtdb + itemsFirestore)
            .sortedByDescending { it.timestamp }

        // Jika keduanya kosong, gunakan dummy supaya tampilan tidak kosong
        allItems = merged.ifEmpty { getDummyData() }

        updateSummary()
        renderList()
    }

    private fun updateSummary() {
        tvTotalDiterima.text  = allItems.count { it.status == "diterima" }.toString()
        tvTotalDitolak.text   = allItems.count { it.status == "ditolak" }.toString()
        tvTotalPenarikan.text = allItems.count { it.status == "penarikan" }.toString()
    }

    private fun setActiveTab(tab: String) {
        activeTab = tab
        listOf(tabSemua, tabDiterima, tabDitolak).forEach {
            it.setBackgroundResource(R.drawable.bg_tab_inactive)
            it.setTextColor(getColor(R.color.text_secondary))
        }
        val activeView = when (tab) {
            "diterima" -> tabDiterima
            "ditolak"  -> tabDitolak
            else       -> tabSemua
        }
        activeView.setBackgroundResource(R.drawable.bg_tab_active)
        activeView.setTextColor(getColor(R.color.surface_white))
        renderList()
    }

    private fun renderList() {
        val filtered = when (activeTab) {
            "diterima" -> allItems.filter { it.status == "diterima" }
            "ditolak"  -> allItems.filter { it.status == "ditolak" }
            else       -> allItems
        }

        containerRiwayat.removeAllViews()

        if (filtered.isEmpty()) {
            scrollRiwayat.visibility = View.GONE
            layoutEmpty.visibility   = View.VISIBLE
            return
        }

        scrollRiwayat.visibility = View.VISIBLE
        layoutEmpty.visibility   = View.GONE

        val inflater = LayoutInflater.from(this)
        filtered.forEach { item ->
            val view          = inflater.inflate(R.layout.item_riwayat, containerRiwayat, false)
            val tvJudul       = view.findViewById<TextView>(R.id.tvJudul)
            val tvDeskripsi   = view.findViewById<TextView>(R.id.tvDeskripsi)
            val tvWaktu       = view.findViewById<TextView>(R.id.tvWaktu)
            val iconContainer = view.findViewById<FrameLayout>(R.id.iconContainer)
            val ivIcon        = view.findViewById<ImageView>(R.id.ivIcon)

            tvJudul.text     = item.judul
            tvDeskripsi.text = item.deskripsi
            tvWaktu.text     = item.waktu

            when (item.status) {
                "ditolak" -> {
                    tvJudul.setTextColor(getColor(R.color.error_red))
                    iconContainer.setBackgroundResource(R.drawable.bg_icon_red_circle)
                    ivIcon.setColorFilter(getColor(R.color.error_red))
                }
                "penarikan" -> {
                    tvJudul.setTextColor(getColor(android.R.color.holo_orange_dark))
                    iconContainer.setBackgroundResource(R.drawable.bg_icon_orange_circle)
                    ivIcon.setImageResource(R.drawable.ic_withdraw)
                    ivIcon.setColorFilter(getColor(android.R.color.holo_orange_dark))
                }
                else -> {
                    tvJudul.setTextColor(getColor(R.color.green_primary))
                    iconContainer.setBackgroundResource(R.drawable.bg_icon_green_circle)
                    ivIcon.clearColorFilter()
                }
            }
            containerRiwayat.addView(view)
        }
    }

    private fun getDummyData() = listOf(
        RiwayatItem("+Rp500",    "Botol PET diterima",   "Hari ini 14:32",    "diterima",  System.currentTimeMillis()),
        RiwayatItem("+Rp500",    "Botol PET diterima",   "Hari ini 13:10",    "diterima",  System.currentTimeMillis() - 5_400_000),
        RiwayatItem("-Rp10.000", "Penarikan ke GoPay",   "12 Des 2024 09:21", "penarikan", System.currentTimeMillis() - 172_800_000),
        RiwayatItem("Ditolak",   "Bukan botol plastik",  "Kemarin 16:30",     "ditolak",   System.currentTimeMillis() - 64_800_000),
        RiwayatItem("+Rp500",    "Botol PET diterima",   "12 Des 2024 10:15", "diterima",  System.currentTimeMillis() - 180_000_000)
    )
}
