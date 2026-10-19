package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: android.widget.EditText
    private lateinit var etPassword: android.widget.EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvError: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase Auth
        auth = Firebase.auth

        // Cek jika user sudah login, langsung ke MainActivity
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupListeners() {

        // Tombol Login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!validateInput(email, password)) return@setOnClickListener

            setLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        goToMain()
                    } else {
                        val message = task.exception?.message ?: "Login gagal, coba lagi."
                        showError(mapFirebaseError(message))
                    }
                }
        }

        // Navigasi ke Register
        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot Password
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showError("Masukkan email kamu dulu untuk reset password.")
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showError("Email reset password sudah dikirim ke $email")
                        tvError.setTextColor(getColor(R.color.green_primary))
                    } else {
                        showError("Gagal mengirim email reset. Periksa email kamu.")
                    }
                }
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
            // Pindahkan cursor ke akhir
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        tvError.visibility = View.GONE
        tvError.setTextColor(getColor(R.color.error_red))

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
        return true
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Memuat…" else getString(R.string.btn_login)
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun mapFirebaseError(message: String): String {
        return when {
            message.contains("no user record") || message.contains("user-not-found") ->
                "Akun dengan email ini tidak ditemukan."
            message.contains("password is invalid") || message.contains("wrong-password") ->
                "Kata sandi salah. Coba lagi."
            message.contains("network") ->
                "Tidak ada koneksi internet. Periksa jaringan kamu."
            message.contains("too many requests") ->
                "Terlalu banyak percobaan. Coba lagi nanti."
            message.contains("user-disabled") ->
                "Akun ini telah dinonaktifkan."
            else -> "Login gagal: $message"
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
