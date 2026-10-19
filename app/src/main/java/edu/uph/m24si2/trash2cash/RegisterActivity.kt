package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etFullName: android.widget.EditText
    private lateinit var etPhone: android.widget.EditText
    private lateinit var etEmail: android.widget.EditText
    private lateinit var etPassword: android.widget.EditText
    private lateinit var etConfirmPassword: android.widget.EditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var cbTerms: CheckBox
    private lateinit var tvError: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase Auth
        auth = Firebase.auth

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {

        // Tombol Daftar
        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validateInput(fullName, phone, email, password, confirmPassword)) return@setOnClickListener

            setLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        // Update display name di Firebase
                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                            displayName = fullName
                        }
                        auth.currentUser?.updateProfile(profileUpdates)

                        // Kirim email verifikasi
                        auth.currentUser?.sendEmailVerification()

                        // Langsung ke Login setelah daftar berhasil
                        showSuccessAndGoLogin()
                    } else {
                        val message = task.exception?.message ?: "Pendaftaran gagal."
                        showError(mapFirebaseError(message))
                    }
                }
        }

        // Navigasi ke Login
        tvGoToLogin.setOnClickListener {
            finish() // Kembali ke LoginActivity
        }

        // Toggle show/hide password
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        // Toggle show/hide confirm password
        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)
        }
    }

    private fun validateInput(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        tvError.visibility = View.GONE
        tvError.setTextColor(getColor(R.color.error_red))

        if (fullName.isEmpty()) {
            showError("Nama lengkap tidak boleh kosong.")
            return false
        }
        if (fullName.length < 3) {
            showError("Nama lengkap minimal 3 karakter.")
            return false
        }
        if (phone.isEmpty()) {
            showError("Nomor telepon tidak boleh kosong.")
            return false
        }
        if (phone.length < 9) {
            showError("Nomor telepon tidak valid.")
            return false
        }
        if (email.isEmpty()) {
            showError("Email tidak boleh kosong.")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Format email tidak valid.")
            return false
        }
        if (password.isEmpty()) {
            showError("Kata sandi tidak boleh kosong.")
            return false
        }
        if (password.length < 6) {
            showError("Kata sandi minimal 6 karakter.")
            return false
        }
        if (confirmPassword != password) {
            showError("Konfirmasi kata sandi tidak cocok.")
            return false
        }
        if (!cbTerms.isChecked) {
            showError("Kamu harus menyetujui Syarat & Ketentuan.")
            return false
        }
        return true
    }

    private fun setLoading(loading: Boolean) {
        btnRegister.isEnabled = !loading
        btnRegister.text = if (loading) "Memuat…" else getString(R.string.btn_register)
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun showSuccessAndGoLogin() {
        tvError.setTextColor(getColor(R.color.green_primary))
        tvError.text = "Pendaftaran berhasil! Silakan login."
        tvError.visibility = View.VISIBLE

        // Delay sebentar lalu kembali ke Login
        btnRegister.postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }, 1500)
    }

    private fun mapFirebaseError(message: String): String {
        return when {
            message.contains("email address is already in use") || message.contains("email-already-in-use") ->
                "Email ini sudah terdaftar. Silakan login."
            message.contains("email address is badly formatted") ->
                "Format email tidak valid."
            message.contains("network") ->
                "Tidak ada koneksi internet. Periksa jaringan kamu."
            message.contains("too many requests") ->
                "Terlalu banyak percobaan. Coba lagi nanti."
            else -> "Pendaftaran gagal: $message"
        }
    }
}
