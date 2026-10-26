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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

data class RiwayatItem(
    val judul: String,
    val deskripsi: String,
    val waktu: String,
    val status: String,
    val timestamp: Long
)

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

    private var allItems = listOf<RiwayatItem>()
    private var activeTab = "semua"
    private val auth = Firebase.auth

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
        loadRiwayat()
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initViews() {
        tabSemua          = findViewById(R.id.tabSemua)
        tabDiterima       = findViewById(R.id.tabDiterima)
        tabDitolak        = findViewById(R.id.tabDitolak)
        containerRiwayat  = findViewById(R.id.containerRiwayat)
        scrollRiwayat     = findViewById(R.id.scrollRiwayat)
        layoutEmpty       = findViewById(R.id.layoutEmpty)
        progressLoading   = findViewById(R.id.progressLoading)
        tvTotalDiterima   = findViewById(R.id.tvTotalDiterima)
        tvTotalDitolak    = findViewById(R.id.tvTotalDitolak)
        tvTotalPenarikan  = findViewById(R.id.tvTotalPenarikan)
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener    { setActiveTab("semua") }
        tabDiterima.setOnClickListener { setActiveTab("diterima") }
        tabDitolak.setOnClickListener  { setActiveTab("ditolak") }
    }

    private fun setActiveTab(tab: String) {
        activeTab = tab
        // Update tab UI
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

    private fun loadRiwayat() {
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$uid/aktivitas")

        ref.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    progressLoading.visibility = View.GONE
                    val list = mutableListOf<RiwayatItem>()
                    snap.children.forEach { child ->
                        list.add(0, RiwayatItem(
                            judul      = child.child("judul").getValue(String::class.java) ?: "",
                            deskripsi  = child.child("deskripsi").getValue(String::class.java) ?: "",
                            waktu      = child.child("waktu").getValue(String::class.java) ?: "",
                            status     = child.child("status").getValue(String::class.java) ?: "diterima",
                            timestamp  = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        ))
                    }

                    // Gunakan dummy data jika kosong (untuk preview)
                    allItems = if (list.isEmpty()) getDummyData() else list
                    updateSummary()
                    renderList()
                }

                override fun onCancelled(e: DatabaseError) {
                    progressLoading.visibility = View.GONE
                    allItems = getDummyData()
                    updateSummary()
                    renderList()
                }
            })
    }

    private fun updateSummary() {
        tvTotalDiterima.text  = allItems.count { it.status == "diterima" }.toString()
        tvTotalDitolak.text   = allItems.count { it.status == "ditolak" }.toString()
        tvTotalPenarikan.text = allItems.count { it.status == "penarikan" }.toString()
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
            val view = inflater.inflate(R.layout.item_riwayat, containerRiwayat, false)

            val tvJudul      = view.findViewById<TextView>(R.id.tvJudul)
            val tvDeskripsi  = view.findViewById<TextView>(R.id.tvDeskripsi)
            val tvWaktu      = view.findViewById<TextView>(R.id.tvWaktu)
            val iconContainer = view.findViewById<FrameLayout>(R.id.iconContainer)
            val ivIcon       = view.findViewById<ImageView>(R.id.ivIcon)

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
        RiwayatItem("+Rp500",         "Botol PET diterima",         "Hari ini 14:32",    "diterima",  System.currentTimeMillis()),
        RiwayatItem("+Rp500",         "Botol PET diterima",         "Hari ini 13:10",    "diterima",  System.currentTimeMillis() - 5400000),
        RiwayatItem("+Rp500",         "Botol PET diterima",         "Kemarin 17:45",     "diterima",  System.currentTimeMillis() - 59400000),
        RiwayatItem("-Rp10.000",      "Penarikan ke GoPay",         "12 Des 2024 09:21", "penarikan", System.currentTimeMillis() - 172800000),
        RiwayatItem("Ditolak",        "Bukan botol plastik",        "Kemarin 16:30",     "ditolak",   System.currentTimeMillis() - 64800000),
        RiwayatItem("+Rp500",         "Botol PET diterima",         "12 Des 2024 10:15", "diterima",  System.currentTimeMillis() - 180000000)
    )
}
