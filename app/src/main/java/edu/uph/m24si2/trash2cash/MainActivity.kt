package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class MainActivity : AppCompatActivity() {

    // Header
    private lateinit var tvGreeting: TextView
    private lateinit var btnNotification: View
    private lateinit var btnProfile: View

    // Saldo card  — data dari RTDB
    private lateinit var tvSaldo: TextView
    private lateinit var btnTarikSaldo: LinearLayout

    // Stats row  — data dari RTDB
    private lateinit var tvJumlahBotol: TextView
    private lateinit var tvBeratPlastik: TextView
    private lateinit var tvTotalReward: TextView

    // Target bulanan  — dihitung dari RTDB
    private lateinit var progressTarget: ProgressBar
    private lateinit var tvProgressPersen: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var tvMotivasi: TextView

    // Aktivitas terbaru  — data dari RTDB
    private lateinit var tvAktivitas1Judul: TextView
    private lateinit var tvAktivitas1Desc: TextView
    private lateinit var tvAktivitas1Waktu: TextView
    private lateinit var tvAktivitas2Judul: TextView
    private lateinit var tvAktivitas2Desc: TextView
    private lateinit var tvAktivitas2Waktu: TextView
    private lateinit var tvAktivitas3Judul: TextView
    private lateinit var tvAktivitas3Desc: TextView
    private lateinit var tvAktivitas3Waktu: TextView

    // Bottom navigation
    private lateinit var navBeranda: LinearLayout
    private lateinit var navMesin: LinearLayout
    private lateinit var navRiwayat: LinearLayout
    private lateinit var navWallet: LinearLayout
    private lateinit var navProfil: LinearLayout

    // Shortcut links
    private lateinit var tvLihatSemuaMesin: TextView
    private lateinit var tvLihatSemuaAktivitas: TextView
    private lateinit var cardMesinTerdekat: View

    // Listener RTDB — disimpan agar bisa dilepas saat destroy
    private var rtdbUserListener: ValueEventListener? = null
    private var rtdbAktivitasListener: ValueEventListener? = null

    private val TARGET_BOTOL = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (FirebaseManager.currentUser == null) { goToLogin(); return }

        initViews()
        setupListeners()
        loadNamaFromFirestore()   // Firestore: profil statis
        listenRtdbStats()         // RTDB: saldo + stats live
        listenRtdbAktivitas()     // RTDB: aktivitas botol live
    }

    override fun onDestroy() {
        super.onDestroy()
        // Lepas listener RTDB supaya tidak memory leak
        rtdbUserListener?.let {
            FirebaseManager.rtdbUserRef().removeEventListener(it)
        }
        rtdbAktivitasListener?.let {
            FirebaseManager.rtdbAktivitasRef().removeEventListener(it)
        }
    }

    private fun initViews() {
        tvGreeting            = findViewById(R.id.tvGreeting)
        btnNotification       = findViewById(R.id.btnNotification)
        btnProfile            = findViewById(R.id.btnProfile)
        tvSaldo               = findViewById(R.id.tvSaldo)
        btnTarikSaldo         = findViewById(R.id.btnTarikSaldo)
        tvJumlahBotol         = findViewById(R.id.tvJumlahBotol)
        tvBeratPlastik        = findViewById(R.id.tvBeratPlastik)
        tvTotalReward         = findViewById(R.id.tvTotalReward)
        progressTarget        = findViewById(R.id.progressTarget)
        tvProgressPersen      = findViewById(R.id.tvProgressPersen)
        tvProgressLabel       = findViewById(R.id.tvProgressLabel)
        tvMotivasi            = findViewById(R.id.tvMotivasi)
        tvAktivitas1Judul     = findViewById(R.id.tvAktivitas1Judul)
        tvAktivitas1Desc      = findViewById(R.id.tvAktivitas1Desc)
        tvAktivitas1Waktu     = findViewById(R.id.tvAktivitas1Waktu)
        tvAktivitas2Judul     = findViewById(R.id.tvAktivitas2Judul)
        tvAktivitas2Desc      = findViewById(R.id.tvAktivitas2Desc)
        tvAktivitas2Waktu     = findViewById(R.id.tvAktivitas2Waktu)
        tvAktivitas3Judul     = findViewById(R.id.tvAktivitas3Judul)
        tvAktivitas3Desc      = findViewById(R.id.tvAktivitas3Desc)
        tvAktivitas3Waktu     = findViewById(R.id.tvAktivitas3Waktu)
        navBeranda            = findViewById(R.id.navBeranda)
        navMesin              = findViewById(R.id.navMesin)
        navRiwayat            = findViewById(R.id.navRiwayat)
        navWallet             = findViewById(R.id.navWallet)
        navProfil             = findViewById(R.id.navProfil)
        tvLihatSemuaMesin     = findViewById(R.id.tvLihatSemuaMesin)
        tvLihatSemuaAktivitas = findViewById(R.id.tvLihatSemuaAktivitas)
        cardMesinTerdekat     = findViewById(R.id.cardMesinTerdekat)
    }

    private fun setupListeners() {
        btnTarikSaldo.setOnClickListener {
            startActivity(Intent(this, TarikSaldoActivity::class.java))
        }
        btnNotification.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }
        tvLihatSemuaMesin.setOnClickListener {
            startActivity(Intent(this, LokasiMesinActivity::class.java))
            activateNav(navMesin)
        }
        cardMesinTerdekat.setOnClickListener {
            startActivity(Intent(this, LokasiMesinActivity::class.java))
            activateNav(navMesin)
        }
        tvLihatSemuaAktivitas.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
            activateNav(navRiwayat)
        }
        navBeranda.setOnClickListener  { activateNav(navBeranda) }
        navMesin.setOnClickListener    {
            activateNav(navMesin)
            startActivity(Intent(this, LokasiMesinActivity::class.java))
        }
        navRiwayat.setOnClickListener  {
            activateNav(navRiwayat)
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
        navWallet.setOnClickListener   {
            activateNav(navWallet)
            startActivity(Intent(this, WalletActivity::class.java))
        }
        navProfil.setOnClickListener   {
            activateNav(navProfil)
            startActivity(Intent(this, ProfilActivity::class.java))
        }
    }

    // ── Firestore: ambil nama profil (sekali saja) ───────────────────
    private fun loadNamaFromFirestore() {
        val user = FirebaseManager.currentUser ?: return

        // Fallback dari Firebase Auth dulu supaya cepat tampil
        val fallbackNama = when {
            !user.displayName.isNullOrBlank() -> user.displayName!!.split(" ").first()
            !user.email.isNullOrBlank()       -> user.email!!.substringBefore("@")
            else                              -> "Pengguna"
        }
        tvGreeting.text = "Halo, $fallbackNama! 👋"

        // Ambil nama dari Firestore (lebih akurat jika user sudah edit profil)
        FirebaseManager.fsUserDoc().get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nama = doc.getString("nama")?.trim()
                    if (!nama.isNullOrBlank()) {
                        val namaAwal = nama.split(" ").first()
                        tvGreeting.text = "Halo, $namaAwal! 👋"
                    }
                } else {
                    // Dokumen belum ada → buat sekarang (user lama sebelum migrasi)
                    val profile = FirebaseManager.defaultUserProfile(
                        nama   = user.displayName ?: "",
                        email  = user.email ?: ""
                    )
                    FirebaseManager.fsUserDoc().set(profile)

                    // Pastikan RTDB juga ada
                    FirebaseManager.rtdbUserRef().get().addOnSuccessListener { snap ->
                        if (!snap.exists()) {
                            FirebaseManager.rtdbUserRef().setValue(
                                FirebaseManager.defaultUserRtdb(
                                    nama  = user.displayName ?: "",
                                    email = user.email ?: ""
                                )
                            )
                        }
                    }
                }
            }
    }

    // ── RTDB: listen perubahan saldo & statistik secara realtime ────
    private fun listenRtdbStats() {
        rtdbUserListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val saldo        = (snap.child("saldo").value as? Number)?.toLong() ?: 0L
                val jumlahBotol  = (snap.child("jumlahBotol").value as? Number)?.toInt() ?: 0
                val beratPlastik = (snap.child("beratPlastik").value as? Number)?.toInt() ?: 0
                val totalReward  = (snap.child("totalReward").value as? Number)?.toLong() ?: 0L

                tvSaldo.text        = "Rp ${FirebaseManager.formatRupiah(saldo)}"
                tvJumlahBotol.text  = jumlahBotol.toString()
                tvBeratPlastik.text = "${FirebaseManager.formatBerat(beratPlastik)} g"
                tvTotalReward.text  = "Rp ${FirebaseManager.formatRupiah(totalReward)}"

                updateTargetProgress(jumlahBotol)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        FirebaseManager.rtdbUserRef().addValueEventListener(rtdbUserListener!!)
    }

    // ── RTDB: listen 3 aktivitas terbaru dari ESP32 ─────────────────
    private fun listenRtdbAktivitas() {
        rtdbAktivitasListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (!snap.exists()) { setAktivitasPlaceholder(); return }

                val list = mutableListOf<DataSnapshot>()
                snap.children.forEach { list.add(0, it) }

                fun bind(
                    ds: DataSnapshot?,
                    tvJudul: TextView, tvDesc: TextView, tvWaktu: TextView
                ) {
                    ds ?: return
                    val status = ds.child("status").getValue(String::class.java) ?: "diterima"
                    tvJudul.text = ds.child("judul").getValue(String::class.java) ?: ""
                    tvDesc.text  = ds.child("deskripsi").getValue(String::class.java) ?: ""
                    tvWaktu.text = ds.child("waktu").getValue(String::class.java) ?: ""
                    tvJudul.setTextColor(getColor(when (status) {
                        "ditolak"   -> R.color.error_red
                        "penarikan" -> android.R.color.holo_orange_dark
                        else        -> R.color.green_primary
                    }))
                }

                bind(list.getOrNull(0), tvAktivitas1Judul, tvAktivitas1Desc, tvAktivitas1Waktu)
                bind(list.getOrNull(1), tvAktivitas2Judul, tvAktivitas2Desc, tvAktivitas2Waktu)
                bind(list.getOrNull(2), tvAktivitas3Judul, tvAktivitas3Desc, tvAktivitas3Waktu)
            }
            override fun onCancelled(error: DatabaseError) { setAktivitasPlaceholder() }
        }
        FirebaseManager.rtdbAktivitasRef()
            .orderByChild("timestamp")
            .limitToLast(3)
            .addValueEventListener(rtdbAktivitasListener!!)
    }

    private fun setAktivitasPlaceholder() {
        tvAktivitas1Judul.text = "+Rp500";  tvAktivitas1Desc.text = "Botol PET diterima"
        tvAktivitas1Waktu.text = "Hari ini 14:32"
        tvAktivitas2Judul.text = "+Rp500";  tvAktivitas2Desc.text = "Botol PET diterima"
        tvAktivitas2Waktu.text = "Kemarin 17:45"
        tvAktivitas3Judul.text = "+Rp1.000"; tvAktivitas3Desc.text = "Botol PET diterima"
        tvAktivitas3Waktu.text = "2 hari lalu 09:10"
        listOf(tvAktivitas1Judul, tvAktivitas2Judul, tvAktivitas3Judul).forEach {
            it.setTextColor(getColor(R.color.green_primary))
        }
    }

    private fun updateTargetProgress(jumlahBotol: Int) {
        val persen = ((jumlahBotol.toFloat() / TARGET_BOTOL) * 100).toInt().coerceIn(0, 100)
        progressTarget.progress = persen
        tvProgressPersen.text   = "$persen%"
        tvProgressLabel.text    = "$jumlahBotol dari $TARGET_BOTOL botol"
        tvMotivasi.text = when {
            persen >= 100 -> "Luar biasa! Target bulan ini sudah tercapai! 🎉"
            persen >= 75  -> "Hampir sampai! Tinggal sedikit lagi!"
            persen >= 50  -> "Setengah jalan! Terus semangat kumpulkan botol!"
            persen >= 25  -> "Bagus! Terus kumpulkan botol untuk dampak lebih besar!"
            else          -> "Yuk mulai kumpulkan botol untuk bumi yang lebih bersih!"
        }
    }

    private fun activateNav(selected: LinearLayout) {
        val allNavs = listOf(navBeranda, navMesin, navRiwayat, navWallet, navProfil)
        allNavs.forEach { nav ->
            val isActive = nav == selected
            nav.setBackgroundResource(if (isActive) R.drawable.bg_nav_active_indicator else 0)
            (nav.getChildAt(1) as? TextView)?.apply {
                setTextColor(getColor(if (isActive) R.color.green_primary else R.color.text_hint))
                typeface = if (isActive) android.graphics.Typeface.DEFAULT_BOLD
                           else android.graphics.Typeface.DEFAULT
            }
            (nav.getChildAt(0) as? ImageView)?.setColorFilter(
                getColor(if (isActive) R.color.green_primary else R.color.text_hint)
            )
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
