package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
 * WalletActivity — hybrid load:
 *
 * RTDB      → saldo live (listener realtime)
 * RTDB      → totalReward live (listener realtime)
 * Firestore → riwayat penarikan untuk hitung totalTarik & tampil 5 transaksi
 * Firestore → riwayat penarikan digabung dengan aktivitas RTDB (5 terakhir)
 */
class WalletActivity : AppCompatActivity() {

    private lateinit var tvSaldoWallet: TextView
    private lateinit var tvTotalRewardWallet: TextView
    private lateinit var tvTotalTarik: TextView
    private lateinit var btnTarikSaldo: LinearLayout
    private lateinit var tvLihatSemua: TextView
    private lateinit var containerTransaksi: LinearLayout

    // RTDB listener
    private var rtdbStatsListener: ValueEventListener? = null

    // Cache data kedua sumber
    private var itemsRtdb      = listOf<RiwayatItem>()
    private var itemsFirestore = listOf<RiwayatItem>()
    private var totalTarik     = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        initViews()
        setupListeners()
        listenRtdbStats()      // RTDB: saldo + totalReward live
        loadFirestoreData()    // Firestore: penarikan (one-shot, di-refresh di onResume)
        loadRtdbAktivitas()    // RTDB: aktivitas botol 5 terakhir
    }

    override fun onResume() {
        super.onResume()
        // Refresh Firestore setiap kembali dari TarikSaldo
        loadFirestoreData()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtdbStatsListener?.let { FirebaseManager.rtdbUserRef().removeEventListener(it) }
    }

    private fun initViews() {
        tvSaldoWallet       = findViewById(R.id.tvSaldoWallet)
        tvTotalRewardWallet = findViewById(R.id.tvTotalRewardWallet)
        tvTotalTarik        = findViewById(R.id.tvTotalTarik)
        btnTarikSaldo       = findViewById(R.id.btnTarikSaldo)
        tvLihatSemua        = findViewById(R.id.tvLihatSemua)
        containerTransaksi  = findViewById(R.id.containerTransaksi)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        btnTarikSaldo.setOnClickListener {
            startActivity(Intent(this, TarikSaldoActivity::class.java))
        }
        tvLihatSemua.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
        // Shortcut e-wallet semuanya buka TarikSaldo
        listOf(R.id.wDana, R.id.wOvo, R.id.wGopay, R.id.wShopeepay).forEach { id ->
            findViewById<LinearLayout>(id).setOnClickListener {
                startActivity(Intent(this, TarikSaldoActivity::class.java))
            }
        }
    }

    // ── RTDB: saldo + totalReward live ──────────────────────────────
    private fun listenRtdbStats() {
        rtdbStatsListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val saldo       = (snap.child("saldo").value as? Number)?.toLong() ?: 0L
                val totalReward = (snap.child("totalReward").value as? Number)?.toLong() ?: 0L

                tvSaldoWallet.text       = "Rp ${FirebaseManager.formatRupiah(saldo)}"
                tvTotalRewardWallet.text = "Rp ${FirebaseManager.formatRupiah(totalReward)}"
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        FirebaseManager.rtdbUserRef().addValueEventListener(rtdbStatsListener!!)
    }

    // ── Firestore: penarikan → hitung totalTarik + cache items ──────
    private fun loadFirestoreData() {
        FirebaseManager.fsWithdrawalsCol()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                var total = 0L
                val list  = snap.documents.mapNotNull { doc ->
                    val nominal = doc.getLong("nominal") ?: return@mapNotNull null
                    val ewallet = doc.getString("ewallet") ?: ""
                    val nomor   = doc.getString("nomor") ?: ""
                    total += nominal
                    RiwayatItem(
                        judul     = "-Rp ${FirebaseManager.formatRupiah(nominal)}",
                        deskripsi = "Penarikan ke $ewallet ($nomor)",
                        waktu     = doc.getString("waktu") ?: "",
                        status    = "penarikan",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
                totalTarik     = total
                itemsFirestore = list
                tvTotalTarik.text = "Rp ${FirebaseManager.formatRupiah(totalTarik)}"
                renderTransaksi()
            }
    }

    // ── RTDB: 5 aktivitas botol terbaru ─────────────────────────────
    private fun loadRtdbAktivitas() {
        FirebaseManager.rtdbAktivitasRef()
            .orderByChild("timestamp")
            .limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val list = mutableListOf<RiwayatItem>()
                    snap.children.forEach { child ->
                        list.add(0, RiwayatItem(
                            judul     = child.child("judul").getValue(String::class.java) ?: "",
                            deskripsi = child.child("deskripsi").getValue(String::class.java) ?: "",
                            waktu     = child.child("waktu").getValue(String::class.java) ?: "",
                            status    = child.child("status").getValue(String::class.java) ?: "diterima",
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        ))
                    }
                    itemsRtdb = list
                    renderTransaksi()
                }
                override fun onCancelled(e: DatabaseError) { renderTransaksi() }
            })
    }

    // ── Gabungkan & render 5 transaksi terbaru ───────────────────────
    private fun renderTransaksi() {
        val merged = (itemsRtdb + itemsFirestore)
            .sortedByDescending { it.timestamp }
            .ifEmpty { getDummyTransaksi() }

        containerTransaksi.removeAllViews()
        val inflater = LayoutInflater.from(this)

        merged.take(5).forEach { item ->
            val view          = inflater.inflate(R.layout.item_riwayat, containerTransaksi, false)
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
            containerTransaksi.addView(view)
        }
    }

    private fun getDummyTransaksi() = listOf(
        RiwayatItem("+Rp500",    "Botol PET diterima",  "Hari ini 14:32",    "diterima",  System.currentTimeMillis()),
        RiwayatItem("+Rp500",    "Botol PET diterima",  "Hari ini 13:10",    "diterima",  System.currentTimeMillis() - 5_400_000),
        RiwayatItem("-Rp10.000", "Penarikan ke GoPay",  "12 Des 2024 09:21", "penarikan", System.currentTimeMillis() - 172_800_000),
        RiwayatItem("Ditolak",   "Bukan botol plastik", "Kemarin 16:30",     "ditolak",   System.currentTimeMillis() - 64_800_000),
        RiwayatItem("+Rp500",    "Botol PET diterima",  "12 Des 2024 10:15", "diterima",  System.currentTimeMillis() - 180_000_000)
    )
}
