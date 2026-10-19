package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Header views
    private lateinit var tvGreeting: TextView

    // Saldo views
    private lateinit var tvSaldo: TextView
    private lateinit var btnTarikSaldo: LinearLayout

    // Stats views
    private lateinit var tvJumlahBotol: TextView
    private lateinit var tvBeratPlastik: TextView
    private lateinit var tvTotalReward: TextView

    // Target bulanan views
    private lateinit var progressTarget: ProgressBar
    private lateinit var tvProgressPersen: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var tvMotivasi: TextView

    // Aktivitas views
    private lateinit var tvAktivitas1Judul: TextView
    private lateinit var tvAktivitas1Desc: TextView
    private lateinit var tvAktivitas1Waktu: TextView
    private lateinit var tvAktivitas2Judul: TextView
    private lateinit var tvAktivitas2Desc: TextView
    private lateinit var tvAktivitas2Waktu: TextView
    private lateinit var tvAktivitas3Judul: TextView
    private lateinit var tvAktivitas3Desc: TextView
    private lateinit var tvAktivitas3Waktu: TextView

    // Navigation views
    private lateinit var navBeranda: LinearLayout
    private lateinit var navMesin: LinearLayout
    private lateinit var navRiwayat: LinearLayout
    private lateinit var navWallet: LinearLayout
    private lateinit var navProfil: LinearLayout

    // Other
    private lateinit var tvLihatSemuaMesin: TextView
    private lateinit var tvLihatSemuaAktivitas: TextView
    private lateinit var btnNotification: View
    private lateinit var btnProfile: View

    // Data konstanta
    private val TARGET_BOTOL = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        // Jika belum login, kembali ke LoginActivity
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        initViews()
        setupListeners()
        loadUserData()
    }

    private fun initViews() {
        tvGreeting           = findViewById(R.id.tvGreeting)
        tvSaldo              = findViewById(R.id.tvSaldo)
        btnTarikSaldo        = findViewById(R.id.btnTarikSaldo)
        tvJumlahBotol        = findViewById(R.id.tvJumlahBotol)
        tvBeratPlastik       = findViewById(R.id.tvBeratPlastik)
        tvTotalReward        = findViewById(R.id.tvTotalReward)
        progressTarget       = findViewById(R.id.progressTarget)
        tvProgressPersen     = findViewById(R.id.tvProgressPersen)
        tvProgressLabel      = findViewById(R.id.tvProgressLabel)
        tvMotivasi           = findViewById(R.id.tvMotivasi)
        tvAktivitas1Judul    = findViewById(R.id.tvAktivitas1Judul)
        tvAktivitas1Desc     = findViewById(R.id.tvAktivitas1Desc)
        tvAktivitas1Waktu    = findViewById(R.id.tvAktivitas1Waktu)
        tvAktivitas2Judul    = findViewById(R.id.tvAktivitas2Judul)
        tvAktivitas2Desc     = findViewById(R.id.tvAktivitas2Desc)
        tvAktivitas2Waktu    = findViewById(R.id.tvAktivitas2Waktu)
        tvAktivitas3Judul    = findViewById(R.id.tvAktivitas3Judul)
        tvAktivitas3Desc     = findViewById(R.id.tvAktivitas3Desc)
        tvAktivitas3Waktu    = findViewById(R.id.tvAktivitas3Waktu)
        navBeranda           = findViewById(R.id.navBeranda)
        navMesin             = findViewById(R.id.navMesin)
        navRiwayat           = findViewById(R.id.navRiwayat)
        navWallet            = findViewById(R.id.navWallet)
        navProfil            = findViewById(R.id.navProfil)
        tvLihatSemuaMesin    = findViewById(R.id.tvLihatSemuaMesin)
        tvLihatSemuaAktivitas= findViewById(R.id.tvLihatSemuaAktivitas)
        btnNotification      = findViewById(R.id.btnNotification)
        btnProfile           = findViewById(R.id.btnProfile)
    }

    private fun setupListeners() {

        // Tarik Saldo
        btnTarikSaldo.setOnClickListener {
            Toast.makeText(this, "Fitur Tarik Saldo segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // Notifikasi
        btnNotification.setOnClickListener {
            Toast.makeText(this, "Notifikasi", Toast.LENGTH_SHORT).show()
        }

        // Profil header
        btnProfile.setOnClickListener {
            activateNav(navProfil)
            Toast.makeText(this, "Halaman Profil segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // Lihat semua mesin
        tvLihatSemuaMesin.setOnClickListener {
            activateNav(navMesin)
            Toast.makeText(this, "Halaman Mesin segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // Lihat semua aktivitas
        tvLihatSemuaAktivitas.setOnClickListener {
            activateNav(navRiwayat)
            Toast.makeText(this, "Halaman Riwayat segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        navBeranda.setOnClickListener { activateNav(navBeranda) }

        navMesin.setOnClickListener {
            activateNav(navMesin)
            Toast.makeText(this, "Halaman Mesin segera hadir!", Toast.LENGTH_SHORT).show()
        }

        navRiwayat.setOnClickListener {
            activateNav(navRiwayat)
            Toast.makeText(this, "Halaman Riwayat segera hadir!", Toast.LENGTH_SHORT).show()
        }

        navWallet.setOnClickListener {
            activateNav(navWallet)
            Toast.makeText(this, "Halaman Wallet segera hadir!", Toast.LENGTH_SHORT).show()
        }

        navProfil.setOnClickListener {
            activateNav(navProfil)
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        // Tampilkan nama user dari Firebase Auth
        val displayName = user.displayName
        val email = user.email ?: ""
        val namaAwal = when {
            !displayName.isNullOrBlank() -> displayName.split(" ").first()
            email.isNotBlank() -> email.substringBefore("@")
            else -> "Pengguna"
        }
        tvGreeting.text = "Halo, $namaAwal! 👋"

        // Load data dari Firebase Realtime Database
        val uid = user.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // User baru — inisialisasi data default
                    initDefaultUserData(uid)
                    return
                }

                // Ambil data saldo
                val saldo = snapshot.child("saldo").getValue(Long::class.java) ?: 0L
                tvSaldo.text = "Rp ${formatRupiah(saldo)}"

                // Ambil statistik
                val jumlahBotol = snapshot.child("jumlahBotol").getValue(Int::class.java) ?: 0
                val beratPlastik = snapshot.child("beratPlastik").getValue(Int::class.java) ?: 0
                val totalReward = snapshot.child("totalReward").getValue(Long::class.java) ?: 0L

                tvJumlahBotol.text = jumlahBotol.toString()
                tvBeratPlastik.text = "${formatBerat(beratPlastik)} g"
                tvTotalReward.text = "Rp ${formatRupiah(totalReward)}"

                // Hitung target bulanan
                updateTargetProgress(jumlahBotol)

                // Load aktivitas terbaru
                loadAktivitasTerbaru(uid)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Gagal memuat data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun initDefaultUserData(uid: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        val user = auth.currentUser

        val defaultData = mapOf(
            "nama"         to (user?.displayName ?: ""),
            "email"        to (user?.email ?: ""),
            "saldo"        to 0L,
            "jumlahBotol"  to 0,
            "beratPlastik" to 0,
            "totalReward"  to 0L,
            "createdAt"    to System.currentTimeMillis()
        )

        dbRef.setValue(defaultData)

        // Tampilkan nilai default di UI
        tvSaldo.text = "Rp 0"
        tvJumlahBotol.text = "0"
        tvBeratPlastik.text = "0 g"
        tvTotalReward.text = "Rp 0"
        updateTargetProgress(0)
    }

    private fun loadAktivitasTerbaru(uid: String) {
        val aktivitasRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .child("aktivitas")

        aktivitasRef.orderByChild("timestamp")
            .limitToLast(3)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Tampilkan data placeholder default
                        setAktivitasPlaceholder()
                        return
                    }

                    // Ambil aktivitas dan urutkan dari terbaru
                    val list = mutableListOf<DataSnapshot>()
                    snapshot.children.forEach { list.add(0, it) }

                    // Isi item 1
                    list.getOrNull(0)?.let { item ->
                        tvAktivitas1Judul.text = item.child("judul").getValue(String::class.java) ?: "+Rp0"
                        tvAktivitas1Desc.text  = item.child("deskripsi").getValue(String::class.java) ?: ""
                        tvAktivitas1Waktu.text = item.child("waktu").getValue(String::class.java) ?: ""
                        // Warna merah jika ditolak
                        val status1 = item.child("status").getValue(String::class.java) ?: "diterima"
                        tvAktivitas1Judul.setTextColor(
                            if (status1 == "ditolak") getColor(R.color.error_red)
                            else getColor(R.color.green_primary)
                        )
                    }

                    // Isi item 2
                    list.getOrNull(1)?.let { item ->
                        tvAktivitas2Judul.text = item.child("judul").getValue(String::class.java) ?: "+Rp0"
                        tvAktivitas2Desc.text  = item.child("deskripsi").getValue(String::class.java) ?: ""
                        tvAktivitas2Waktu.text = item.child("waktu").getValue(String::class.java) ?: ""
                        val status2 = item.child("status").getValue(String::class.java) ?: "diterima"
                        tvAktivitas2Judul.setTextColor(
                            if (status2 == "ditolak") getColor(R.color.error_red)
                            else getColor(R.color.green_primary)
                        )
                    }

                    // Isi item 3
                    list.getOrNull(2)?.let { item ->
                        tvAktivitas3Judul.text = item.child("judul").getValue(String::class.java) ?: "+Rp0"
                        tvAktivitas3Desc.text  = item.child("deskripsi").getValue(String::class.java) ?: ""
                        tvAktivitas3Waktu.text = item.child("waktu").getValue(String::class.java) ?: ""
                        val status3 = item.child("status").getValue(String::class.java) ?: "diterima"
                        tvAktivitas3Judul.setTextColor(
                            if (status3 == "ditolak") getColor(R.color.error_red)
                            else getColor(R.color.green_primary)
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    setAktivitasPlaceholder()
                }
            })
    }

    private fun setAktivitasPlaceholder() {
        tvAktivitas1Judul.text = "+Rp500"
        tvAktivitas1Desc.text  = "Botol PET diterima"
        tvAktivitas1Waktu.text = "Hari ini 14:32"
        tvAktivitas2Judul.text = "+Rp500"
        tvAktivitas2Desc.text  = "Botol PET diterima"
        tvAktivitas2Waktu.text = "Kemarin 17:45"
        tvAktivitas3Judul.text = "+Rp1.000"
        tvAktivitas3Desc.text  = "Botol PET diterima"
        tvAktivitas3Waktu.text = "2 hari lalu 09:10"
    }

    private fun updateTargetProgress(jumlahBotol: Int) {
        val persen = ((jumlahBotol.toFloat() / TARGET_BOTOL) * 100).toInt().coerceIn(0, 100)
        progressTarget.progress = persen
        tvProgressPersen.text = "$persen%"
        tvProgressLabel.text = "$jumlahBotol dari $TARGET_BOTOL botol"

        tvMotivasi.text = when {
            persen >= 100 -> "Luar biasa! Target bulan ini sudah tercapai! 🎉"
            persen >= 75  -> "Hampir sampai! Tinggal sedikit lagi!"
            persen >= 50  -> "Setengah jalan! Terus semangat kumpulkan botol!"
            persen >= 25  -> "Bagus! Terus kumpulkan botol untuk dampak yang lebih besar!"
            else          -> "Yuk mulai kumpulkan botol untuk bumi yang lebih bersih!"
        }
    }

    private fun activateNav(selected: LinearLayout) {
        val allNavs = listOf(navBeranda, navMesin, navRiwayat, navWallet, navProfil)
        val allNavTexts = listOf(
            navBeranda.getChildAt(1) as TextView,
            navMesin.getChildAt(1) as TextView,
            navRiwayat.getChildAt(1) as TextView,
            navWallet.getChildAt(1) as TextView,
            navProfil.getChildAt(1) as TextView
        )

        allNavs.forEachIndexed { index, nav ->
            if (nav == selected) {
                nav.setBackgroundResource(R.drawable.bg_nav_active_indicator)
                allNavTexts[index].setTextColor(getColor(R.color.green_primary))
                allNavTexts[index].typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                nav.setBackgroundResource(0)
                allNavTexts[index].setTextColor(getColor(R.color.text_hint))
                allNavTexts[index].typeface = android.graphics.Typeface.DEFAULT
            }
        }
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluar Akun")
            .setMessage("Apakah kamu yakin ingin keluar?")
            .setPositiveButton("Keluar") { _, _ ->
                auth.signOut()
                goToLogin()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Format angka ke Rupiah: 25500 -> "25.500"
    private fun formatRupiah(amount: Long): String {
        return String.format("%,d", amount).replace(',', '.')
    }

    // Format berat gram: 1200 -> "1.200"
    private fun formatBerat(gram: Int): String {
        return String.format("%,d", gram).replace(',', '.')
    }
}
