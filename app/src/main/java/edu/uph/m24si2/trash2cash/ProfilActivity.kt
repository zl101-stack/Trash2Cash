package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class ProfilActivity : AppCompatActivity() {

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnEditAvatar).setOnClickListener {
            toast("Fitur ganti foto profil segera hadir!")
        }

        // Menu Akun
        findViewById<LinearLayout>(R.id.menuEditProfil).setOnClickListener {
            showEditProfilDialog()
        }
        findViewById<LinearLayout>(R.id.menuKeamanan).setOnClickListener {
            showGantiPasswordDialog()
        }

        // Switch notifikasi
        val switchNotif = findViewById<SwitchCompat>(R.id.switchNotif)
        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            toast(if (isChecked) "Notifikasi diaktifkan" else "Notifikasi dimatikan")
        }

        // Menu Lainnya
        findViewById<LinearLayout>(R.id.menuBantuan).setOnClickListener {
            showInfoDialog(
                "Pusat Bantuan",
                "Hubungi kami di:\n📧 support@trash2cash.id\n📱 WhatsApp: 0812-3456-7890\n\nJam operasional: Senin-Jumat, 09.00-17.00 WIB"
            )
        }
        findViewById<LinearLayout>(R.id.menuTentang).setOnClickListener {
            showInfoDialog(
                "Tentang Trash2Cash",
                "🌿 Trash2Cash v1.0.0\n\nBerawal dari Satu Botol, Berdampak untuk Bumi.\n\nKumpulkan botol plastik, dapatkan reward langsung di aplikasi, dan ikut ciptakan lingkungan yang lebih bersih.\n\n© 2024 Trash2Cash. All rights reserved."
            )
        }

        // Logout
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        val uid  = user.uid

        // Tampilkan dari Firebase Auth dulu
        val tvNama  = findViewById<TextView>(R.id.tvNamaProfil)
        val tvEmail = findViewById<TextView>(R.id.tvEmailProfil)
        tvNama.text  = user.displayName?.ifBlank { "Pengguna" } ?: "Pengguna"
        tvEmail.text = user.email ?: ""

        // Load stats dari Realtime Database
        FirebaseDatabase.getInstance().getReference("users/$uid")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val nama       = snap.child("nama").getValue(String::class.java) ?: ""
                    val saldo      = snap.child("saldo").getValue(Long::class.java) ?: 0L
                    val botol      = snap.child("jumlahBotol").getValue(Int::class.java) ?: 0
                    val berat      = snap.child("beratPlastik").getValue(Int::class.java) ?: 0

                    if (nama.isNotBlank()) tvNama.text = nama

                    findViewById<TextView>(R.id.tvStatBotol).text = botol.toString()
                    findViewById<TextView>(R.id.tvStatSaldo).text = "Rp ${fmt(saldo)}"
                    findViewById<TextView>(R.id.tvStatKg).text    = "${fmtBerat(berat)} g"
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun showEditProfilDialog() {
        val user = auth.currentUser ?: return
        val input = android.widget.EditText(this).apply {
            hint    = "Nama lengkap"
            setText(user.displayName ?: "")
            background = getDrawable(R.drawable.selector_input_field)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Nama")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val namaBaru = input.text.toString().trim()
                if (namaBaru.isEmpty()) { toast("Nama tidak boleh kosong"); return@setPositiveButton }

                // Update Firebase Auth display name
                val profileUpdate = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = namaBaru
                }
                user.updateProfile(profileUpdate).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Update Realtime Database
                        FirebaseDatabase.getInstance()
                            .getReference("users/${user.uid}/nama")
                            .setValue(namaBaru)
                        findViewById<TextView>(R.id.tvNamaProfil).text = namaBaru
                        toast("Nama berhasil diperbarui!")
                    } else {
                        toast("Gagal memperbarui nama")
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showGantiPasswordDialog() {
        val user = auth.currentUser ?: return
        val email = user.email ?: ""

        AlertDialog.Builder(this)
            .setTitle("Ganti Password")
            .setMessage("Email reset password akan dikirim ke:\n$email")
            .setPositiveButton("Kirim") { _, _ ->
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            toast("Email reset password terkirim ke $email")
                        } else {
                            toast("Gagal mengirim email reset")
                        }
                    }
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
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun fmt(n: Long) = String.format("%,d", n).replace(',', '.')
    private fun fmtBerat(n: Int) = String.format("%,d", n).replace(',', '.')
}
