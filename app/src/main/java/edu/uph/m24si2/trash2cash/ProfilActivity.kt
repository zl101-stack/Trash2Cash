package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * ProfilActivity — hybrid load:
 *
 * Firestore → nama, email, telepon, notifikasi (profil statis)
 * RTDB      → jumlahBotol, saldo, beratPlastik (stats live dari ESP32)
 */
class ProfilActivity : AppCompatActivity() {

    private lateinit var tvNamaProfil: TextView
    private lateinit var tvEmailProfil: TextView
    private lateinit var tvStatBotol: TextView
    private lateinit var tvStatSaldo: TextView
    private lateinit var tvStatKg: TextView
    private lateinit var switchNotif: SwitchCompat

    // RTDB listener untuk stats
    private var rtdbStatsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        initViews()
        setupMenuListeners()
        loadProfilFromFirestore()   // Firestore: data profil statis
        listenStatsFromRtdb()       // RTDB: stats live
    }

    override fun onDestroy() {
        super.onDestroy()
        rtdbStatsListener?.let { FirebaseManager.rtdbUserRef().removeEventListener(it) }
    }

    private fun initViews() {
        tvNamaProfil  = findViewById(R.id.tvNamaProfil)
        tvEmailProfil = findViewById(R.id.tvEmailProfil)
        tvStatBotol   = findViewById(R.id.tvStatBotol)
        tvStatSaldo   = findViewById(R.id.tvStatSaldo)
        tvStatKg      = findViewById(R.id.tvStatKg)
        switchNotif   = findViewById(R.id.switchNotif)
    }

    // ── Firestore: load nama, email, telepon, notifikasi ────────────
    private fun loadProfilFromFirestore() {
        val user = FirebaseManager.currentUser ?: return

        // Tampilkan dari Firebase Auth dulu (cepat)
        tvNamaProfil.text  = user.displayName?.ifBlank { "Pengguna" } ?: "Pengguna"
        tvEmailProfil.text = user.email ?: ""

        // Ambil dari Firestore (lebih akurat, sudah include telepon dll)
        FirebaseManager.fsUserDoc().get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Buat dokumen baru jika belum ada
                    FirebaseManager.fsUserDoc().set(
                        FirebaseManager.defaultUserProfile(
                            nama  = user.displayName ?: "",
                            email = user.email ?: ""
                        )
                    )
                    return@addOnSuccessListener
                }
                val nama      = doc.getString("nama")?.trim()
                val notifikasi = doc.getBoolean("notifikasi") ?: true

                if (!nama.isNullOrBlank()) tvNamaProfil.text = nama
                switchNotif.isChecked = notifikasi
            }
    }

    // ── RTDB: listen stats live (botol, saldo, berat) ───────────────
    private fun listenStatsFromRtdb() {
        rtdbStatsListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val botol  = snap.child("jumlahBotol").getValue(Int::class.java) ?: 0
                val saldo  = snap.child("saldo").getValue(Long::class.java) ?: 0L
                val berat  = snap.child("beratPlastik").getValue(Int::class.java) ?: 0

                tvStatBotol.text = botol.toString()
                tvStatSaldo.text = "Rp ${FirebaseManager.formatRupiah(saldo)}"
                tvStatKg.text    = "${FirebaseManager.formatBerat(berat)} g"
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        FirebaseManager.rtdbUserRef().addValueEventListener(rtdbStatsListener!!)
    }

    private fun setupMenuListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnEditAvatar).setOnClickListener {
            toast("Fitur ganti foto profil segera hadir!")
        }

        // Edit Profil → update Firestore + Firebase Auth displayName
        findViewById<LinearLayout>(R.id.menuEditProfil).setOnClickListener {
            showEditProfilDialog()
        }

        // Keamanan → ganti password via email reset
        findViewById<LinearLayout>(R.id.menuKeamanan).setOnClickListener {
            showGantiPasswordDialog()
        }

        // Switch notifikasi → update Firestore
        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            FirebaseManager.fsUserDoc()
                .update("notifikasi", isChecked)
                .addOnFailureListener {
                    // Jika update gagal, kembalikan switch
                    switchNotif.isChecked = !isChecked
                    toast("Gagal menyimpan pengaturan notifikasi")
                }
        }

        // Bantuan
        findViewById<LinearLayout>(R.id.menuBantuan).setOnClickListener {
            showInfoDialog(
                "Pusat Bantuan",
                "Hubungi kami di:\n\n" +
                "📧 support@trash2cash.id\n" +
                "📱 WhatsApp: 0812-3456-7890\n\n" +
                "Jam operasional:\nSenin–Jumat, 09.00–17.00 WIB"
            )
        }

        // Tentang
        findViewById<LinearLayout>(R.id.menuTentang).setOnClickListener {
            showInfoDialog(
                "Tentang Trash2Cash",
                "🌿 Trash2Cash v1.0.0\n\n" +
                "Berawal dari Satu Botol,\nBerdampak untuk Bumi.\n\n" +
                "Kumpulkan botol plastik, dapatkan reward langsung di aplikasi, " +
                "dan ikut ciptakan lingkungan yang lebih bersih.\n\n" +
                "© 2024 Trash2Cash. All rights reserved."
            )
        }

        // Logout
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    // ── Edit Profil: update Firestore + Firebase Auth ────────────────
    private fun showEditProfilDialog() {
        val user = FirebaseManager.currentUser ?: return

        // Layout dialog dengan nama + telepon
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 30, 60, 10)
        }
        val etNama = EditText(this).apply {
            hint = "Nama lengkap"
            setText(tvNamaProfil.text)
            background = getDrawable(R.drawable.selector_input_field)
            setPadding(40, 30, 40, 30)
        }
        val etTelepon = EditText(this).apply {
            hint = "Nomor telepon"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            background = getDrawable(R.drawable.selector_input_field)
            setPadding(40, 30, 40, 30)
        }
        // Isi telepon dari Firestore
        FirebaseManager.fsUserDoc().get().addOnSuccessListener { doc ->
            etTelepon.setText(doc.getString("telepon") ?: "")
        }

        layout.addView(etNama)
        layout.addView(View(this).apply { minimumHeight = 16 })
        layout.addView(etTelepon)

        AlertDialog.Builder(this)
            .setTitle("Edit Profil")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru    = etNama.text.toString().trim()
                val teleponBaru = etTelepon.text.toString().trim()

                if (namaBaru.isEmpty()) { toast("Nama tidak boleh kosong"); return@setPositiveButton }

                // 1. Update Firebase Auth displayName
                user.updateProfile(userProfileChangeRequest { displayName = namaBaru })
                    .addOnSuccessListener {
                        // 2. Update Firestore
                        FirebaseManager.fsUserDoc().update(
                            mapOf("nama" to namaBaru, "telepon" to teleponBaru)
                        ).addOnSuccessListener {
                            tvNamaProfil.text = namaBaru
                            toast("Profil berhasil diperbarui!")
                        }
                    }
                    .addOnFailureListener { toast("Gagal memperbarui profil") }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Ganti Password: kirim email reset ───────────────────────────
    private fun showGantiPasswordDialog() {
        val email = FirebaseManager.currentUser?.email ?: return
        AlertDialog.Builder(this)
            .setTitle("Ganti Password")
            .setMessage("Email reset password akan dikirim ke:\n$email")
            .setPositiveButton("Kirim Email") { _, _ ->
                FirebaseManager.auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { toast("Email reset password terkirim ke $email") }
                    .addOnFailureListener { toast("Gagal mengirim email reset") }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showInfoDialog(judul: String, isi: String) {
        AlertDialog.Builder(this)
            .setTitle(judul)
            .setMessage(isi)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar Akun")
            .setMessage("Apakah kamu yakin ingin keluar dari akun ini?")
            .setPositiveButton("Keluar") { _, _ ->
                FirebaseManager.auth.signOut()
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
