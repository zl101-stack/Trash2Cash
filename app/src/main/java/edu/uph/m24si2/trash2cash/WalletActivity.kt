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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class WalletActivity : AppCompatActivity() {

    private lateinit var tvSaldoWallet: TextView
    private lateinit var tvTotalRewardWallet: TextView
    private lateinit var tvTotalTarik: TextView
    private lateinit var btnTarikSaldo: LinearLayout
    private lateinit var tvLihatSemua: TextView
    private lateinit var containerTransaksi: LinearLayout

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        tvSaldoWallet       = findViewById(R.id.tvSaldoWallet)
        tvTotalRewardWallet = findViewById(R.id.tvTotalRewardWallet)
        tvTotalTarik        = findViewById(R.id.tvTotalTarik)
        btnTarikSaldo       = findViewById(R.id.btnTarikSaldo)
        tvLihatSemua        = findViewById(R.id.tvLihatSemua)
        containerTransaksi  = findViewById(R.id.containerTransaksi)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        btnTarikSaldo.setOnClickListener {
            startActivity(Intent(this, TarikSaldoActivity::class.java))
        }

        tvLihatSemua.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }

        // E-wallet shortcut semuanya buka TarikSaldo
        listOf(R.id.wDana, R.id.wOvo, R.id.wGopay, R.id.wShopeepay).forEach { id ->
            findViewById<LinearLayout>(id).setOnClickListener {
                startActivity(Intent(this, TarikSaldoActivity::class.java))
            }
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData() // Refresh setiap kali kembali dari TarikSaldo
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val saldo       = snap.child("saldo").getValue(Long::class.java) ?: 0L
                val totalReward = snap.child("totalReward").getValue(Long::class.java) ?: 0L

                tvSaldoWallet.text       = "Rp ${fmt(saldo)}"
                tvTotalRewardWallet.text = "Rp ${fmt(totalReward)}"

                // Hitung total penarikan dari aktivitas
                var totalTarik = 0L
                snap.child("aktivitas").children.forEach { child ->
                    if (child.child("status").getValue(String::class.java) == "penarikan") {
                        val judul = child.child("judul").getValue(String::class.java) ?: ""
                        val angka = judul.replace("-Rp", "").replace(".", "").trim().toLongOrNull() ?: 0L
                        totalTarik += angka
                    }
                }
                tvTotalTarik.text = "Rp ${fmt(totalTarik)}"

                // Render 5 transaksi terakhir
                loadRecentTransactions(uid)
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun loadRecentTransactions(uid: String) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("users/$uid/aktivitas")

        ref.orderByChild("timestamp").limitToLast(5)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    containerTransaksi.removeAllViews()
                    val list = mutableListOf<DataSnapshot>()
                    snap.children.forEach { list.add(0, it) }

                    val items = if (list.isEmpty()) getDummyTransaksi() else list.map { child ->
                        Triple(
                            child.child("judul").getValue(String::class.java) ?: "",
                            child.child("deskripsi").getValue(String::class.java) ?: "",
                            child.child("status").getValue(String::class.java) ?: "diterima"
                        )
                    }

                    val inflater = LayoutInflater.from(this@WalletActivity)
                    items.take(5).forEach { (judul, desc, status) ->
                        val view = inflater.inflate(R.layout.item_riwayat, containerTransaksi, false)
                        view.findViewById<TextView>(R.id.tvJudul).apply {
                            text = judul
                            setTextColor(getColor(when (status) {
                                "ditolak"   -> R.color.error_red
                                "penarikan" -> android.R.color.holo_orange_dark
                                else        -> R.color.green_primary
                            }))
                        }
                        view.findViewById<TextView>(R.id.tvDeskripsi).text = desc
                        val iconContainer = view.findViewById<FrameLayout>(R.id.iconContainer)
                        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
                        when (status) {
                            "ditolak" -> {
                                iconContainer.setBackgroundResource(R.drawable.bg_icon_red_circle)
                                ivIcon.setColorFilter(getColor(R.color.error_red))
                            }
                            "penarikan" -> {
                                iconContainer.setBackgroundResource(R.drawable.bg_icon_orange_circle)
                                ivIcon.setImageResource(R.drawable.ic_withdraw)
                                ivIcon.setColorFilter(getColor(android.R.color.holo_orange_dark))
                            }
                            else -> {
                                iconContainer.setBackgroundResource(R.drawable.bg_icon_green_circle)
                                ivIcon.clearColorFilter()
                            }
                        }
                        containerTransaksi.addView(view)
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun getDummyTransaksi() = listOf(
        Triple("+Rp500",    "Botol PET diterima",  "diterima"),
        Triple("+Rp500",    "Botol PET diterima",  "diterima"),
        Triple("-Rp10.000", "Penarikan ke GoPay",  "penarikan"),
        Triple("Ditolak",   "Bukan botol plastik", "ditolak"),
        Triple("+Rp500",    "Botol PET diterima",  "diterima")
    )

    private fun fmt(amount: Long) = String.format("%,d", amount).replace(',', '.')
}
