package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.userProfileChangeRequest

/**
 * RegisterActivity — hybrid write on register:
 *
 * Firebase Auth   → createUserWithEmailAndPassword + updateProfile (displayName)
 * Firestore       → simpan profil statis (nama, email, telepon, notifikasi, createdAt)
 * RTDB            → inisialisasi data live (saldo=0, jumlahBotol=0, dst.)
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var cbTerms: CheckBox
    private lateinit var tvError: TextView

    private var isPasswordVisible        = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        initViews()
        setupListeners()
    }

    private fun initViews() {
        etFullName            = findViewById(R.id.etFullName)
        etPhone               = findViewById(R.id.etPhone)
        etEmail               = findViewById(R.id.etEmail)
        etPassword            = findViewById(R.id.etPassword)
        etConfirmPassword     = findViewById(R.id.etConfirmPassword)
        btnRegister           = findViewById(R.id.btnRegister)
        tvGoToLogin           = findViewById(R.id.tvGoToLogin)
        ivTogglePassword      = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        cbTerms               = findViewById(R.id.cbTerms)
        tvError               = findViewById(R.id.tvError)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            val fullName        = etFullName.text.toString().trim()
            val phone           = etPhone.text.toString().trim()
            val email           = etEmail.text.toString().trim()
            val password        = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validateInput(fullName, phone, email, password, confirmPassword)) return@setOnClickListener
            doRegister(fullName, phone, email, password)
        }

        tvGoToLogin.setOnClickListener { finish() }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.transformationMethod = if (isConfirmPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            ivToggleConfirmPassword.setImageResource(
                if (isConfirmPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }
    }

    // ── Step 1: Firebase Auth createUser ────────────────────────────
    private fun doRegister(fullName: String, phone: String, email: String, password: String) {
        setLoading(true)

        FirebaseManager.auth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    setLoading(false)
                    showError(mapFirebaseError(task.exception?.message ?: ""))
                    return@addOnCompleteListener
                }

                val user = FirebaseManager.auth.currentUser ?: run {
                    setLoading(false)
                    showError("Terjadi kesalahan, coba lagi.")
                    return@addOnCompleteListener
                }

                // Step 2: Update displayName di Firebase Auth
                user.updateProfile(userProfileChangeRequest { displayName = fullName })

                // Step 3: Kirim email verifikasi (opsional, tidak blokir alur)
                user.sendEmailVerification()

                // Step 4: Tulis ke Firestore + RTDB secara paralel
                saveUserData(user.uid, fullName, phone, email)
            }
    }

    // ── Step 4a: Firestore — profil statis ──────────────────────────
    // ── Step 4b: RTDB    — data live awal ───────────────────────────
    private fun saveUserData(uid: String, fullName: String, phone: String, email: String) {
        // Firestore: profil statis
        val profileData = FirebaseManager.defaultUserProfile(
            nama     = fullName,
            email    = email,
            telepon  = phone
        )
        FirebaseManager.fsUserDoc(uid).set(profileData)
            .addOnFailureListener {
                // Gagal Firestore tidak blokir alur — RTDB tetap jalan
            }

        // RTDB: data live (saldo, botol, berat)
        FirebaseManager.rtdbUserRef(uid).setValue(
            FirebaseManager.defaultUserRtdb(nama = fullName, email = email)
        ).addOnCompleteListener {
            // Selesai — apapun hasilnya lanjut ke sukses
            setLoading(false)
            showSuccessAndGoLogin()
        }
    }

    private fun showSuccessAndGoLogin() {
        tvError.setTextColor(getColor(R.color.green_primary))
        tvError.text       = "Pendaftaran berhasil! Silakan login."
        tvError.visibility = View.VISIBLE

        btnRegister.postDelayed({
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }, 1_500)
    }

    private fun validateInput(
        fullName: String, phone: String,
        email: String, password: String, confirmPassword: String
    ): Boolean {
        tvError.visibility = View.GONE
        tvError.setTextColor(getColor(R.color.error_red))

        return when {
            fullName.isEmpty()             -> { showError("Nama lengkap tidak boleh kosong."); false }
            fullName.length < 3            -> { showError("Nama lengkap minimal 3 karakter."); false }
            phone.isEmpty()                -> { showError("Nomor telepon tidak boleh kosong."); false }
            phone.length < 9              -> { showError("Nomor telepon tidak valid."); false }
            email.isEmpty()               -> { showError("Email tidak boleh kosong."); false }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                           -> { showError("Format email tidak valid."); false }
            password.isEmpty()            -> { showError("Kata sandi tidak boleh kosong."); false }
            password.length < 6           -> { showError("Kata sandi minimal 6 karakter."); false }
            confirmPassword != password   -> { showError("Konfirmasi kata sandi tidak cocok."); false }
            !cbTerms.isChecked            -> { showError("Kamu harus menyetujui Syarat & Ketentuan."); false }
            else                          -> true
        }
    }

    private fun setLoading(loading: Boolean) {
        btnRegister.isEnabled = !loading
        btnRegister.text = if (loading) "Mendaftar…" else getString(R.string.btn_register)
    }

    private fun showError(message: String) {
        tvError.text       = message
        tvError.visibility = View.VISIBLE
    }

    private fun mapFirebaseError(message: String): String = when {
        message.contains("email address is already in use") ||
        message.contains("email-already-in-use")
            -> "Email ini sudah terdaftar. Silakan login."
        message.contains("badly formatted")
            -> "Format email tidak valid."
        message.contains("network")
            -> "Tidak ada koneksi internet."
        message.contains("too many requests")
            -> "Terlalu banyak percobaan. Coba lagi nanti."
        else -> "Pendaftaran gagal: $message"
    }
}
