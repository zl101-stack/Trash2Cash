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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

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
    private var saldoTersedia   = 0L

    // RTDB listener untuk saldo live
    private var saldoListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tarik_saldo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }
        initViews()
        listenSaldoRtdb()   // RTDB: saldo live
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        saldoListener?.let { FirebaseManager.rtdbSaldoRef().removeEventListener(it) }
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

    // ── RTDB: listen saldo secara realtime ───────────────────────────
    private fun listenSaldoRtdb() {
        saldoListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                saldoTersedia = (snap.value as? Number)?.toLong() ?: 0L
                tvSaldoTersedia.text = "Rp ${FirebaseManager.formatRupiah(saldoTersedia)}"
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        FirebaseManager.rtdbSaldoRef().addValueEventListener(saldoListener!!)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Pilih e-wallet
        val ewallets = mapOf(
            btnDana to "DANA", btnOvo to "OVO",
            btnGopay to "GoPay", btnShopeepay to "ShopeePay"
        )
        ewallets.forEach { (btn, name) ->
            btn.setOnClickListener { selectedEwallet = name; updateEwalletUI(btn) }
        }

        // Chip nominal cepat
        mapOf(chip10k to 10_000L, chip25k to 25_000L, chip50k to 50_000L).forEach { (chip, amount) ->
            chip.setOnClickListener {
                etNominal.setText(amount.toString())
                updateChipUI(chip)
            }
        }

        btnKonfirmasi.setOnClickListener { processWithdraw() }
    }

    private fun processWithdraw() {
        val nomor      = etNomorAkun.text.toString().trim()
        val nominalStr = etNominal.text.toString().trim().replace(".", "").replace(",", "")
        val nominal    = nominalStr.toLongOrNull() ?: 0L

        if (nomor.isEmpty())    { toast("Masukkan nomor akun $selectedEwallet"); return }
        if (nomor.length < 10)  { toast("Nomor akun tidak valid"); return }
        if (nominal < 10_000)   { toast("Minimal penarikan Rp 10.000"); return }
        if (nominal > saldoTersedia) { toast("Saldo tidak mencukupi"); return }

        setLoading(true)
        val saldoBaru = saldoTersedia - nominal

        // 1. Kurangi saldo di RTDB (langsung terasa di semua screen)
        FirebaseManager.rtdbSaldoRef().setValue(saldoBaru)
            .addOnSuccessListener {
                // 2. Catat riwayat penarikan ke Firestore
                saveWithdrawalToFirestore(nominal, nomor)
            }
            .addOnFailureListener {
                setLoading(false)
                toast("Gagal memproses penarikan, coba lagi")
            }
    }

    // ── Firestore: simpan catatan penarikan ──────────────────────────
    private fun saveWithdrawalToFirestore(nominal: Long, nomor: String) {
        val data = hashMapOf(
            "ewallet"   to selectedEwallet,
            "nomor"     to nomor,
            "nominal"   to nominal,
            "status"    to "berhasil",
            "waktu"     to FirebaseManager.nowString(),
            "timestamp" to System.currentTimeMillis()
        )
        FirebaseManager.fsWithdrawalsCol().add(data)
            .addOnSuccessListener {
                setLoading(false)
                showSuccessDialog(nominal, nomor)
            }
            .addOnFailureListener {
                // Saldo sudah terkurangi di RTDB, hanya pencatatan Firestore gagal
                // Tetap anggap sukses dari sisi user
                setLoading(false)
                showSuccessDialog(nominal, nomor)
            }
    }

    private fun showSuccessDialog(nominal: Long, nomor: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Penarikan Berhasil! 🎉")
            .setMessage(
                "Rp ${FirebaseManager.formatRupiah(nominal)} berhasil dikirim ke " +
                "$selectedEwallet ($nomor).\nProses dalam 1×24 jam."
            )
            .setPositiveButton("Selesai") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        btnKonfirmasi.isEnabled = !loading
        btnKonfirmasi.text = if (loading) "Memproses…" else "Konfirmasi Penarikan"
    }

    private fun updateEwalletUI(selected: LinearLayout) {
        listOf(btnDana, btnOvo, btnGopay, btnShopeepay).forEach { btn ->
            val isSelected = btn == selected
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_ewallet_selected else R.drawable.bg_ewallet_normal
            )
            (btn.getChildAt(1) as? TextView)?.setTextColor(
                getColor(if (isSelected) R.color.green_primary else R.color.text_secondary)
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
                getColor(if (isSelected) R.color.surface_white else R.color.green_primary)
            )
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
