package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

/**
 * LokasiMesinActivity — hybrid load:
 *
 * Firestore → data statis mesin (nama, alamat, lat, lng, jarak)
 * RTDB      → status mesin live "online"/"offline" — ditulis ESP32
 *
 * Jika Firestore belum ada datanya (pertama kali), seed otomatis dipanggil.
 */
class LokasiMesinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLokasiMesinBinding

    // ── Firebase Realtime Database ──────────────────────────────────
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance(
            "https://trash2cash-1c6f4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )

    private val mesinRef = database.getReference("Mesin/Mesin_01")
    private val beratRef = mesinRef.child("berat_kg")
    private val kapasitasRef = mesinRef.child("kapasitas_persen")

    private var beratListener: ValueEventListener? = null
    private var kapasitasListener: ValueEventListener? = null

    // Cache mesin dari Firestore
    private data class MesinData(
        val id: String,
        val nama: String,
        val alamat: String,
        val lat: Double,
        val lng: Double,
        val jarak: String,
        var statusOnline: Boolean = true
    )

    private val mesinList = mutableListOf<MesinData>()
    private val rtdbListeners = mutableMapOf<String, ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLokasiMesinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnBack) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnMyLocation.setOnClickListener {
            Toast.makeText(this, "Mendeteksi lokasi kamu...", Toast.LENGTH_SHORT).show()
        }

        setupSearch()
        setupFirebaseListeners()
        loadMesinFromFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtdbListeners.forEach { (machineId, listener) ->
            FirebaseManager.rtdbMachineStatusRef(machineId).removeEventListener(listener)
        }
        rtdbListeners.clear()
        beratListener?.let { beratRef.removeEventListener(it) }
        kapasitasListener?.let { kapasitasRef.removeEventListener(it) }
    }

    // ── Firestore: load data statis semua mesin ──────────────────────
    private fun loadMesinFromFirestore() {
        FirebaseManager.fsMachinesCol.get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
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

                mesinList.forEach { mesin -> listenMachineStatus(mesin) }
                renderMesinList(mesinList)
            }
            .addOnFailureListener {
                loadFallbackData()
            }
    }

    // ── RTDB: listen status "online"/"offline" tiap mesin ───────────
    private fun listenMachineStatus(mesin: MesinData) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val status = snap.child("status").getValue(String::class.java) ?: "offline"
                mesin.statusOnline = status == "online"
                updateMesinStatusUI(mesin)
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        rtdbListeners[mesin.id] = listener
        FirebaseManager.rtdbMachineStatusRef(mesin.id).addValueEventListener(listener)
    }

    // ── Render seluruh list mesin ────────────────────────────────────
    private fun renderMesinList(list: List<MesinData>) {
        binding.containerMesin.removeAllViews()
        if (list.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text    = "Tidak ada mesin ditemukan."
                gravity = android.view.Gravity.CENTER
                setPadding(0, 40, 0, 40)
                setTextColor(getColor(R.color.text_hint))
            }
            binding.containerMesin.addView(tvEmpty)
            return
        }
        list.forEach { mesin -> binding.containerMesin.addView(buildMesinCard(mesin)) }
    }

    // ── Update hanya badge status 1 mesin tanpa re-render semua ─────
    private fun updateMesinStatusUI(mesin: MesinData) {
        val tag = binding.containerMesin.findViewWithTag<View>("badge_${mesin.id}")
        if (tag is TextView) {
            tag.text = if (mesin.statusOnline) "Online" else "Offline"
            tag.setTextColor(getColor(
                if (mesin.statusOnline) R.color.green_primary else R.color.text_hint
            ))
            tag.setBackgroundResource(
                if (mesin.statusOnline) R.drawable.bg_online_badge else R.drawable.bg_nominal_chip
            )
        }
        val btnRute = binding.containerMesin.findViewWithTag<View>("rute_${mesin.id}")
        if (btnRute is LinearLayout) {
            btnRute.alpha  = if (mesin.statusOnline) 1f else 0.4f
            btnRute.isEnabled = mesin.statusOnline
        }
    }

    // ── Build card View untuk satu mesin ────────────────────────────
    private fun buildMesinCard(mesin: MesinData): View {
        val card = layoutInflater.inflate(
            R.layout.item_mesin_card, binding.containerMesin, false
        )

        card.findViewById<TextView>(R.id.tvNamaMesin).text   = mesin.nama
        card.findViewById<TextView>(R.id.tvAlamatMesin).text = mesin.alamat
        card.findViewById<TextView>(R.id.tvJarakMesin).text  = mesin.jarak

        val tvStatus = card.findViewById<TextView>(R.id.tvStatusMesin)
        tvStatus.tag  = "badge_${mesin.id}"
        tvStatus.text = if (mesin.statusOnline) "Online" else "Offline"
        tvStatus.setTextColor(getColor(
            if (mesin.statusOnline) R.color.green_primary else R.color.text_hint
        ))
        tvStatus.setBackgroundResource(
            if (mesin.statusOnline) R.drawable.bg_online_badge else R.drawable.bg_nominal_chip
        )

        val btnRute = card.findViewById<LinearLayout>(R.id.btnRuteMesin)
        btnRute.tag       = "rute_${mesin.id}"
        btnRute.alpha     = if (mesin.statusOnline) 1f else 0.4f
        btnRute.isEnabled = mesin.statusOnline
        btnRute.setOnClickListener { openMaps(mesin) }

        return card
    }

    // ── Search filter ────────────────────────────────────────────────
    private fun setupSearch() {
        binding.etCariLokasi.addTextChangedListener(object : android.text.TextWatcher {
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

    // ── Realtime listeners telemetry (berat & kapasitas Mesin_01) ───
    private fun setupFirebaseListeners() {
        beratListener = beratRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val berat = snapshot.getValue(Double::class.java)
                    if (berat != null) {
                        binding.tvBeratMesin.text = "Berat: %.2f Kg".format(berat)
                        updateConnectionStatus(true)
                    } else {
                        binding.tvBeratMesin.text = "Berat: -- Kg"
                    }
                } catch (e: Exception) {
                    binding.tvBeratMesin.text = "Berat: -- Kg"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvBeratMesin.text = "Berat: -- Kg"
                updateConnectionStatus(false)
            }
        })

        kapasitasListener = kapasitasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val kapasitas = snapshot.getValue(Double::class.java)
                    if (kapasitas != null) {
                        binding.tvKapasitasMesin.text = "Kapasitas: %.1f%%".format(kapasitas)
                        val persenInt = kapasitas.toInt().coerceIn(0, 100)
                        binding.progressKapasitas.progress = persenInt
                        binding.tvProgressPersen.text = "$persenInt%"
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
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvKapasitasMesin.text = "Kapasitas: -- %"
                binding.progressKapasitas.progress = 0
                binding.tvProgressPersen.text = "--%"
                updateConnectionStatus(false)
            }
        })
    }

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

    private fun openMaps(mesin: MesinData) {
        val uri    = Uri.parse("geo:${mesin.lat},${mesin.lng}?q=${mesin.lat},${mesin.lng}(${Uri.encode(mesin.nama)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${mesin.lat},${mesin.lng}")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun loadFallbackData() {
        mesinList.apply {
            clear()
            add(MesinData("lippo-medan",    "Trash2Cash - Lippo Plaza Medan", "Lippo Plaza Medan, Lt. 6, Jl. Imam Bonjol No.6", -3.5952, 98.6722, "0.3 km", true))
            add(MesinData("kampus-a",       "Trash2Cash - Kampus A",          "Jl. Pendidikan No. 1, Jakarta",                   -6.2088, 106.8456, "0.5 km", true))
            add(MesinData("sun-plaza-medan","Trash2Cash - Sun Plaza",          "Sun Plaza, Lt. 3, Medan",                         -3.5873, 98.6744,  "1.2 km", false))
        }
        mesinList.forEach { listenMachineStatus(it) }
        renderMesinList(mesinList)
    }
}
