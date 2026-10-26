package edu.uph.m24si2.trash2cash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LokasiMesinActivity : AppCompatActivity() {

    // Koordinat mesin (lat, lng, nama)
    private val mesinList = listOf(
        Triple(-3.5952,  98.6722, "Trash2Cash Lippo Plaza Medan Lt.6"),
        Triple(-6.2088, 106.8456, "Trash2Cash Kampus A"),
        Triple(-3.5873,  98.6744, "Trash2Cash Sun Plaza Medan Lt.3")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lokasi_mesin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnBack)) { v, insets ->
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
            insets
        }

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        // Buka Google Maps dengan rute ke lokasi mesin
        findViewById<LinearLayout>(R.id.btnRuteMesin1).setOnClickListener {
            openMaps(mesinList[0])
        }
        findViewById<LinearLayout>(R.id.btnRuteMesin2).setOnClickListener {
            openMaps(mesinList[1])
        }

        // My location button
        findViewById<android.view.View>(R.id.btnMyLocation).setOnClickListener {
            Toast.makeText(this, "Mendeteksi lokasi kamu...", Toast.LENGTH_SHORT).show()
        }

        // Search
        val etCari = findViewById<EditText>(R.id.etCariLokasi)
        etCari.setOnEditorActionListener { _, _, _ ->
            val query = etCari.text.toString()
            if (query.isNotEmpty()) {
                Toast.makeText(this, "Mencari: $query", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

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
}
