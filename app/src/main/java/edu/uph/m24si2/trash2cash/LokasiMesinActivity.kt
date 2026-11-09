package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
<<<<<<< Updated upstream
=======
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
>>>>>>> Stashed changes
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
<<<<<<< Updated upstream
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import edu.uph.m24si2.trash2cash.databinding.ActivityLokasiMesinBinding
=======
import com.google.firebase.database.ValueEventListener
>>>>>>> Stashed changes

/**
 * LokasiMesinActivity — hybrid load:
 *
 * Firestore → data statis mesin (nama, alamat, lat, lng, jarak)
 * RTDB      → status mesin live "online"/"offline" — ditulis ESP32
 *
 * Jika Firestore belum ada datanya (pertama kali), seed otomatis dipanggil.
 */
class LokasiMesinActivity : AppCompatActivity() {

<<<<<<< Updated upstream
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
=======
    private lateinit var etCariLokasi: EditText
    private lateinit var containerMesin: LinearLayout

    // Cache mesin dari Firestore
    private data class MesinData(
        val id: String,
        val nama: String,
        val alamat: String,
        val lat: Double,
        val lng: Double,
        val jarak: String,
        var statusOnline: Boolean = true   // diisi dari RTDB
>>>>>>> Stashed changes
    )

    private val mesinList = mutableListOf<MesinData>()

    // RTDB listeners per mesin — disimpan untuk dilepas saat destroy
    private val rtdbListeners = mutableMapOf<String, ValueEventListener>()

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

<<<<<<< Updated upstream
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
=======
        etCariLokasi   = findViewById(R.id.etCariLokasi)
        containerMesin = findViewById(R.id.containerMesin)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnMyLocation).setOnClickListener {
            Toast.makeText(this, "Mendeteksi lokasi kamu…", Toast.LENGTH_SHORT).show()
        }

        setupSearch()
        loadMesinFromFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Lepas semua RTDB listener status mesin
        rtdbListeners.forEach { (machineId, listener) ->
            FirebaseManager.rtdbMachineStatusRef(machineId).removeEventListener(listener)
        }
        rtdbListeners.clear()
    }

    // ── Firestore: load data statis semua mesin ──────────────────────
    private fun loadMesinFromFirestore() {
        FirebaseManager.fsMachinesCol.get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    // Belum ada data → seed dulu lalu load ulang
                    FirebaseManager.seedMachines()
                    loadMesinFromFirestore()
                    return@addOnSuccessListener
                }

                mesinList.clear()
                snap.documents.forEach { doc ->
                    mesinList.add(
                        MesinData(
                            id     = doc.id,
                            nama   = doc.getString("nama") ?: "",
                            alamat = doc.getString("alamat") ?: "",
                            lat    = doc.getDouble("lat") ?: 0.0,
                            lng    = doc.getDouble("lng") ?: 0.0,
                            jarak  = doc.getString("jarak") ?: "-"
                        )
                    )
                }

                // Setelah dapat data Firestore, listen status RTDB tiap mesin
                mesinList.forEach { mesin -> listenMachineStatus(mesin) }
                renderMesinList(mesinList)
>>>>>>> Stashed changes
            }
            .addOnFailureListener {
                // Firestore gagal → tampilkan data hardcoded fallback
                loadFallbackData()
            }
    }

    // ── RTDB: listen status "online"/"offline" tiap mesin ───────────
    private fun listenMachineStatus(mesin: MesinData) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val status = snap.child("status").getValue(String::class.java) ?: "offline"
                mesin.statusOnline = status == "online"
                // Re-render hanya item ini
                updateMesinStatusUI(mesin)
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        rtdbListeners[mesin.id] = listener
        FirebaseManager.rtdbMachineStatusRef(mesin.id).addValueEventListener(listener)
    }

    // ── Render seluruh list mesin ────────────────────────────────────
    private fun renderMesinList(list: List<MesinData>) {
        containerMesin.removeAllViews()
        if (list.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text    = "Tidak ada mesin ditemukan."
                gravity = android.view.Gravity.CENTER
                setPadding(0, 40, 0, 40)
                setTextColor(getColor(R.color.text_hint))
            }
            containerMesin.addView(tvEmpty)
            return
        }
        list.forEach { mesin -> containerMesin.addView(buildMesinCard(mesin)) }
    }

    // ── Update hanya badge status 1 mesin tanpa re-render semua ─────
    private fun updateMesinStatusUI(mesin: MesinData) {
        val tag = containerMesin.findViewWithTag<View>("badge_${mesin.id}")
        if (tag is TextView) {
            tag.text = if (mesin.statusOnline) "Online" else "Offline"
            tag.setTextColor(getColor(
                if (mesin.statusOnline) R.color.green_primary else R.color.text_hint
            ))
            tag.setBackgroundResource(
                if (mesin.statusOnline) R.drawable.bg_online_badge else R.drawable.bg_nominal_chip
            )
        }
        // Update button rute — disable jika offline
        val btnRute = containerMesin.findViewWithTag<View>("rute_${mesin.id}")
        if (btnRute is LinearLayout) {
            btnRute.alpha  = if (mesin.statusOnline) 1f else 0.4f
            btnRute.isEnabled = mesin.statusOnline
        }

        // ── Firebase Realtime Listeners ──────────────────────────────
        setupFirebaseListeners()
    }

<<<<<<< Updated upstream
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
=======
    // ── Build card View untuk satu mesin ────────────────────────────
    private fun buildMesinCard(mesin: MesinData): View {
        val inflater = LayoutInflater.from(this)
        // Gunakan layout item_mesin yang sudah ada di activity_lokasi_mesin
        // Karena kita inflate dinamis, buat LinearLayout manual yang mirip card
        val card = layoutInflater.inflate(
            R.layout.item_mesin_card, containerMesin, false
        )

        card.findViewById<TextView>(R.id.tvNamaMesin).text   = mesin.nama
        card.findViewById<TextView>(R.id.tvAlamatMesin).text = mesin.alamat
        card.findViewById<TextView>(R.id.tvJarakMesin).text  = mesin.jarak

        // Badge status — diberi tag untuk update parsial
        val tvStatus = card.findViewById<TextView>(R.id.tvStatusMesin)
        tvStatus.tag  = "badge_${mesin.id}"
        tvStatus.text = if (mesin.statusOnline) "Online" else "Offline"
        tvStatus.setTextColor(getColor(
            if (mesin.statusOnline) R.color.green_primary else R.color.text_hint
        ))
        tvStatus.setBackgroundResource(
            if (mesin.statusOnline) R.drawable.bg_online_badge else R.drawable.bg_nominal_chip
        )

        // Tombol Rute — diberi tag untuk update parsial
        val btnRute = card.findViewById<LinearLayout>(R.id.btnRuteMesin)
        btnRute.tag       = "rute_${mesin.id}"
        btnRute.alpha     = if (mesin.statusOnline) 1f else 0.4f
        btnRute.isEnabled = mesin.statusOnline
        btnRute.setOnClickListener { openMaps(mesin) }

        return card
    }

    // ── Buka Google Maps dengan koordinat mesin ──────────────────────
    private fun openMaps(mesin: MesinData) {
        val uri    = Uri.parse("geo:${mesin.lat},${mesin.lng}?q=${mesin.lat},${mesin.lng}(${Uri.encode(mesin.nama)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
>>>>>>> Stashed changes
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback browser
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${mesin.lat},${mesin.lng}")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

<<<<<<< Updated upstream
    // ═══════════════════════════════════════════════════════════════════
    //  LIFECYCLE CLEANUP
    // ═══════════════════════════════════════════════════════════════════

    override fun onDestroy() {
        super.onDestroy()
        // Hapus listener untuk mencegah memory leak dan callback yang tidak diinginkan
        beratListener?.let { beratRef.removeEventListener(it) }
        kapasitasListener?.let { kapasitasRef.removeEventListener(it) }
=======
    // ── Search filter ────────────────────────────────────────────────
    private fun setupSearch() {
        etCariLokasi.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                val query    = s.toString().lowercase()
                val filtered = if (query.isEmpty()) mesinList
                               else mesinList.filter {
                                   it.nama.lowercase().contains(query) ||
                                   it.alamat.lowercase().contains(query)
                               }
                renderMesinList(filtered)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    // ── Fallback jika Firestore tidak tersedia ───────────────────────
    private fun loadFallbackData() {
        mesinList.apply {
            clear()
            add(MesinData("lippo-medan",    "Trash2Cash - Lippo Plaza Medan", "Lippo Plaza Medan, Lt. 6, Jl. Imam Bonjol No.6", -3.5952, 98.6722, "0.3 km", true))
            add(MesinData("kampus-a",       "Trash2Cash - Kampus A",          "Jl. Pendidikan No. 1, Jakarta",                   -6.2088, 106.8456, "0.5 km", true))
            add(MesinData("sun-plaza-medan","Trash2Cash - Sun Plaza",          "Sun Plaza, Lt. 3, Medan",                         -3.5873, 98.6744,  "1.2 km", false))
        }
        mesinList.forEach { listenMachineStatus(it) }
        renderMesinList(mesinList)
>>>>>>> Stashed changes
    }
}
