package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.uph.m24si2.trash2cash.databinding.ActivityLokasiMesinBinding

class LokasiMesinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLokasiMesinBinding

    // ── Firebase Realtime Database ──────────────────────────────────
    // URL eksplisit diperlukan karena database berada di region asia-southeast1
    // dan google-services.json tidak menyertakan database URL.
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance(
            "https://trash2cash-1c6f4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )

    // Reference ke path Mesin_01
    private val mesinRef = database.getReference("Mesin/Mesin_01")
    private val beratRef = mesinRef.child("berat_kg")
    private val kapasitasRef = mesinRef.child("kapasitas_persen")

    // Simpan listener agar bisa dihapus di onDestroy (mencegah memory leak)
    private var beratListener: ValueEventListener? = null
    private var kapasitasListener: ValueEventListener? = null

    // ── Data lokasi mesin (lat, lng, nama) ────────────────────────────
    private val mesinList = listOf(
        Triple(-3.5952, 98.6722, "Trash2Cash Lippo Plaza Medan Lt.6"),
        Triple(-6.2088, 106.8456, "Trash2Cash Kampus A"),
        Triple(-3.5873, 98.6744, "Trash2Cash Sun Plaza Medan Lt.3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding
        binding = ActivityLokasiMesinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        // ── UI Interactions (existing) ───────────────────────────────
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRuteMesin1.setOnClickListener { openMaps(mesinList[0]) }
        binding.btnRuteMesin2.setOnClickListener { openMaps(mesinList[1]) }

        binding.btnMyLocation.setOnClickListener {
            Toast.makeText(this, "Mendeteksi lokasi kamu...", Toast.LENGTH_SHORT).show()
        }

        binding.etCariLokasi.setOnEditorActionListener { _, _, _ ->
            val query = binding.etCariLokasi.text.toString()
            if (query.isNotEmpty()) {
                Toast.makeText(this, "Mencari: $query", Toast.LENGTH_SHORT).show()
            }
            false
        }

        // ── Firebase Realtime Listeners ──────────────────────────────
        setupFirebaseListeners()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FIREBASE REALTIME DATABASE LISTENERS
    // ═══════════════════════════════════════════════════════════════════

    private fun setupFirebaseListeners() {
        // ── Listener 1: berat_kg ──────────────────────────────────────
        beratListener = beratRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    // Firebase menyimpan angka sebagai Double
                    val berat = snapshot.getValue(Double::class.java)
                    if (berat != null) {
                        binding.tvBeratMesin.text = "Berat: %.2f Kg".format(berat)
                        updateConnectionStatus(true)
                    } else {
                        binding.tvBeratMesin.text = "Berat: -- Kg"
                    }
                } catch (e: Exception) {
                    binding.tvBeratMesin.text = "Berat: -- Kg"
                    Toast.makeText(
                        this@LokasiMesinActivity,
                        "Gagal membaca data berat: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvBeratMesin.text = "Berat: -- Kg"
                updateConnectionStatus(false)
                Toast.makeText(
                    this@LokasiMesinActivity,
                    "Database error (berat): ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // ── Listener 2: kapasitas_persen ─────────────────────────────
        kapasitasListener = kapasitasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val kapasitas = snapshot.getValue(Double::class.java)
                    if (kapasitas != null) {
                        // Update TextView
                        binding.tvKapasitasMesin.text = "Kapasitas: %.1f%%".format(kapasitas)

                        // Update Circular ProgressBar (integer 0-100)
                        val persenInt = kapasitas.toInt().coerceIn(0, 100)
                        binding.progressKapasitas.progress = persenInt

                        // Update label persentase di tengah lingkaran
                        binding.tvProgressPersen.text = "$persenInt%"

                        // Ubah warna indikator berdasarkan kapasitas
                        when {
                            persenInt >= 80 -> binding.progressKapasitas
                                .setIndicatorColor(getColor(R.color.error_red))
                            persenInt >= 50 -> binding.progressKapasitas
                                .setIndicatorColor(getColor(R.color.green_accent))
                            else -> binding.progressKapasitas
                                .setIndicatorColor(getColor(R.color.green_primary))
                        }

                        updateConnectionStatus(true)
                    } else {
                        binding.tvKapasitasMesin.text = "Kapasitas: -- %"
                        binding.progressKapasitas.progress = 0
                        binding.tvProgressPersen.text = "--%"
                    }
                } catch (e: Exception) {
                    binding.tvKapasitasMesin.text = "Kapasitas: -- %"
                    binding.progressKapasitas.progress = 0
                    binding.tvProgressPersen.text = "--%"
                    Toast.makeText(
                        this@LokasiMesinActivity,
                        "Gagal membaca data kapasitas: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvKapasitasMesin.text = "Kapasitas: -- %"
                binding.progressKapasitas.progress = 0
                binding.tvProgressPersen.text = "--%"
                updateConnectionStatus(false)
                Toast.makeText(
                    this@LokasiMesinActivity,
                    "Database error (kapasitas): ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Update badge status koneksi di header card.
     */
    private fun updateConnectionStatus(connected: Boolean) {
        if (connected) {
            binding.tvStatusKoneksi.text = "Online"
            binding.tvStatusKoneksi.setTextColor(getColor(R.color.green_primary))
            binding.tvStatusKoneksi.setBackgroundResource(R.drawable.bg_online_badge)
        } else {
            binding.tvStatusKoneksi.text = "Menunggu..."
            binding.tvStatusKoneksi.setTextColor(getColor(R.color.text_hint))
            binding.tvStatusKoneksi.setBackgroundResource(R.drawable.bg_nominal_chip)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GOOGLE MAPS NAVIGATION
    // ═══════════════════════════════════════════════════════════════════

    private fun openMaps(mesin: Triple<Double, Double, String>) {
        val (lat, lng, nama) = mesin
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(nama)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback ke browser
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LIFECYCLE CLEANUP
    // ═══════════════════════════════════════════════════════════════════

    override fun onDestroy() {
        super.onDestroy()
        // Hapus listener untuk mencegah memory leak dan callback yang tidak diinginkan
        beratListener?.let { beratRef.removeEventListener(it) }
        kapasitasListener?.let { kapasitasRef.removeEventListener(it) }
    }
}
