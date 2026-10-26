package edu.uph.m24si2.trash2cash

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

class TarikSaldoActivity : AppCompatActivity() {

    private lateinit var tvSaldoTersedia: TextView
    private lateinit var etNomorAkun: EditText
    private lateinit var etNominal: EditText
    private lateinit var btnKonfirmasi: Button
    private lateinit var btnBack: View
    private lateinit var chip10k: TextView
    private lateinit var chip25k: TextView
    private lateinit var chip50k: TextView
    private lateinit var btnDana: LinearLayout
    private lateinit var btnOvo: LinearLayout
    private lateinit var btnGopay: LinearLayout
    private lateinit var btnShopeepay: LinearLayout

    private var selectedEwallet = "DANA"
    private var saldoTersedia = 0L
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tarik_saldo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        initViews()
        loadSaldo()
        setupListeners()
    }

    private fun initViews() {
        tvSaldoTersedia = findViewById(R.id.tvSaldoTersedia)
        etNomorAkun     = findViewById(R.id.etNomorAkun)
        etNominal       = findViewById(R.id.etNominal)
        btnKonfirmasi   = findViewById(R.id.btnKonfirmasi)
        btnBack         = findViewById(R.id.btnBack)
        chip10k         = findViewById(R.id.chip10k)
        chip25k         = findViewById(R.id.chip25k)
        chip50k         = findViewById(R.id.chip50k)
        btnDana         = findViewById(R.id.btnDana)
        btnOvo          = findViewById(R.id.btnOvo)
        btnGopay        = findViewById(R.id.btnGopay)
        btnShopeepay    = findViewById(R.id.btnShopeepay)
    }

    private fun loadSaldo() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users/$uid/saldo")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    saldoTersedia = snap.getValue(Long::class.java) ?: 0L
                    tvSaldoTersedia.text = "Rp ${formatRupiah(saldoTersedia)}"
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // E-wallet selection
        val ewallets = mapOf(
            btnDana to "DANA", btnOvo to "OVO",
            btnGopay to "GoPay", btnShopeepay to "ShopeePay"
        )
        ewallets.forEach { (btn, name) ->
            btn.setOnClickListener {
                selectedEwallet = name
                updateEwalletUI(btn)
            }
        }

        // Chip nominal
        val chips = mapOf(chip10k to 10000L, chip25k to 25000L, chip50k to 50200L)
        chips.forEach { (chip, amount) ->
            chip.setOnClickListener {
                etNominal.setText(amount.toString())
                updateChipUI(chip)
            }
        }

        // Konfirmasi
        btnKonfirmasi.setOnClickListener { processWithdraw() }
    }

    private fun updateEwalletUI(selected: LinearLayout) {
        listOf(btnDana, btnOvo, btnGopay, btnShopeepay).forEach { btn ->
            val isSelected = btn == selected
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_ewallet_selected else R.drawable.bg_ewallet_normal
            )
            val label = btn.getChildAt(1) as TextView
            label.setTextColor(
                if (isSelected) getColor(R.color.green_primary) else getColor(R.color.text_secondary)
            )
        }
    }

    private fun updateChipUI(selected: TextView) {
        listOf(chip10k, chip25k, chip50k).forEach { chip ->
            val isSelected = chip == selected
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_nominal_chip_selected else R.drawable.bg_nominal_chip
            )
            chip.setTextColor(
                if (isSelected) getColor(R.color.surface_white) else getColor(R.color.green_primary)
            )
        }
    }

    private fun processWithdraw() {
        val nomor = etNomorAkun.text.toString().trim()
        val nominalStr = etNominal.text.toString().trim().replace(".", "").replace(",", "")
        val nominal = nominalStr.toLongOrNull() ?: 0L

        if (nomor.isEmpty()) { toast("Masukkan nomor akun $selectedEwallet"); return }
        if (nomor.length < 10) { toast("Nomor akun tidak valid"); return }
        if (nominal < 10000) { toast("Minimal penarikan Rp 10.000"); return }
        if (nominal > saldoTersedia) { toast("Saldo tidak mencukupi"); return }

        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$uid")

        // Kurangi saldo
        val saldoBaru = saldoTersedia - nominal
        dbRef.child("saldo").setValue(saldoBaru)

        // Catat riwayat penarikan
        val riwayatRef = dbRef.child("aktivitas").push()
        val riwayat = mapOf(
            "judul"     to "-Rp ${formatRupiah(nominal)}",
            "deskripsi" to "Penarikan ke $selectedEwallet ($nomor)",
            "status"    to "penarikan",
            "waktu"     to getCurrentTime(),
            "timestamp" to System.currentTimeMillis()
        )
        riwayatRef.setValue(riwayat)

        showSuccessDialog(nominal, nomor)
    }

    private fun showSuccessDialog(nominal: Long, nomor: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Penarikan Berhasil! 🎉")
            .setMessage("Rp ${formatRupiah(nominal)} berhasil dikirim ke $selectedEwallet ($nomor).\nProses dalam 1x24 jam.")
            .setPositiveButton("Selesai") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun formatRupiah(amount: Long) = String.format("%,d", amount).replace(',', '.')

    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id"))
        return sdf.format(java.util.Date())
    }
}
