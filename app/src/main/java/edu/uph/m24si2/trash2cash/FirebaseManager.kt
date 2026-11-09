package edu.uph.m24si2.trash2cash

import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

/**
 * FirebaseManager — single access point untuk semua Firebase operations.
 *
 * Arsitektur hybrid:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  REALTIME DATABASE (RTDB)  — data live dari ESP32               │
 * │  Path: users/{uid}/                                             │
 * │    ├── saldo          (Long)   ← ESP32 tambah setiap botol      │
 * │    ├── jumlahBotol    (Int)    ← ESP32 increment                │
 * │    ├── beratPlastik   (Int)    ← ESP32 increment (gram)         │
 * │    ├── totalReward    (Long)   ← ESP32 increment                │
 * │    └── aktivitas/{id}          ← ESP32 push setiap botol masuk  │
 * │          ├── judul    (String) "+Rp500"                         │
 * │          ├── deskripsi(String) "Botol PET diterima"             │
 * │          ├── status   (String) "diterima" / "ditolak"           │
 * │          ├── waktu    (String) "12 Jun 2025 14:32"              │
 * │          └── timestamp(Long)                                    │
 * │                                                                 │
 * │  Path: machines/{machineId}/                                    │
 * │    ├── status   (String) "online" / "offline" ← ESP32 tulis    │
 * │    └── lastSeen (Long)   timestamp             ← ESP32 tulis    │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  FIRESTORE — data statis, tidak perlu realtime                  │
 * │  Collection: users/{uid}                                        │
 * │    ├── nama         (String)                                    │
 * │    ├── email        (String)                                    │
 * │    ├── telepon      (String)                                    │
 * │    ├── createdAt    (Long)                                       │
 * │    └── notifikasi   (Boolean)                                   │
 * │                                                                 │
 * │  Collection: machines/{machineId}                               │
 * │    ├── nama         (String) "Trash2Cash - Lippo Plaza Medan"   │
 * │    ├── alamat       (String) "Lippo Plaza Medan, Lt. 6"         │
 * │    ├── lat          (Double)                                    │
 * │    ├── lng          (Double)                                    │
 * │    └── jarak        (String) "0.3 km"                           │
 * │                                                                 │
 * │  Collection: withdrawals/{uid}/records/{id}                     │
 * │    ├── ewallet      (String) "DANA"                             │
 * │    ├── nomor        (String) "08xxxxxxxxxx"                     │
 * │    ├── nominal      (Long)                                      │
 * │    ├── status       (String) "berhasil" / "pending"             │
 * │    ├── waktu        (String)                                    │
 * │    └── timestamp    (Long)                                      │
 * └─────────────────────────────────────────────────────────────────┘
 */
object FirebaseManager {

    // Firebase Auth
    val auth get() = Firebase.auth
    val currentUser get() = auth.currentUser
    val uid get() = auth.currentUser?.uid ?: ""

    // ── Realtime Database ────────────────────────────────────────────
    val rtdb = FirebaseDatabase.getInstance()

    /** Root node user di RTDB: users/{uid} */
    fun rtdbUserRef(uid: String = FirebaseManager.uid) =
        rtdb.getReference("users").child(uid)

    /** Saldo user di RTDB */
    fun rtdbSaldoRef(uid: String = FirebaseManager.uid) =
        rtdbUserRef(uid).child("saldo")

    /** Statistik user di RTDB (jumlahBotol, beratPlastik, totalReward) */
    fun rtdbStatsRef(uid: String = FirebaseManager.uid) =
        rtdbUserRef(uid)

    /** Aktivitas botol di RTDB (push oleh ESP32) */
    fun rtdbAktivitasRef(uid: String = FirebaseManager.uid) =
        rtdbUserRef(uid).child("aktivitas")

    /** Status mesin di RTDB (ditulis ESP32) */
    fun rtdbMachineStatusRef(machineId: String) =
        rtdb.getReference("machines").child(machineId)

    // ── Firestore ────────────────────────────────────────────────────
    val firestore = FirebaseFirestore.getInstance()

    /** Dokumen profil user di Firestore */
    fun fsUserDoc(uid: String = FirebaseManager.uid) =
        firestore.collection("users").document(uid)

    /** Collection mesin di Firestore */
    val fsMachinesCol get() =
        firestore.collection("machines")

    /** Dokumen mesin spesifik di Firestore */
    fun fsMachineDoc(machineId: String) =
        fsMachinesCol.document(machineId)

    /** Collection riwayat penarikan user di Firestore */
    fun fsWithdrawalsCol(uid: String = FirebaseManager.uid) =
        firestore.collection("withdrawals").document(uid).collection("records")

    // ── Helpers ──────────────────────────────────────────────────────

    /** Format Long ke Rupiah: 25500 → "25.500" */
    fun formatRupiah(amount: Long): String =
        String.format("%,d", amount).replace(',', '.')

    /** Format Int berat gram: 1200 → "1.200" */
    fun formatBerat(gram: Int): String =
        String.format("%,d", gram).replace(',', '.')

    /** Timestamp sekarang ke string tanggal: "12 Jun 2025 14:32" */
    fun nowString(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id"))
        return sdf.format(java.util.Date())
    }

    /** Default data profil user baru untuk Firestore */
    fun defaultUserProfile(
        nama: String = "",
        email: String = "",
        telepon: String = ""
    ) = hashMapOf(
        "nama"       to nama,
        "email"      to email,
        "telepon"    to telepon,
        "notifikasi" to true,
        "createdAt"  to System.currentTimeMillis()
    )

    /** Default data live user baru untuk RTDB */
    fun defaultUserRtdb(
        nama: String = "",
        email: String = ""
    ) = mapOf(
        "nama"         to nama,
        "email"        to email,
        "saldo"        to 0L,
        "jumlahBotol"  to 0,
        "beratPlastik" to 0,
        "totalReward"  to 0L,
        "createdAt"    to System.currentTimeMillis()
    )

    /** Data seed mesin untuk Firestore (jalankan sekali saat setup) */
    fun seedMachines() {
        val machines = listOf(
            mapOf(
                "id"     to "lippo-medan",
                "nama"   to "Trash2Cash - Lippo Plaza Medan",
                "alamat" to "Lippo Plaza Medan, Lt. 6, Jl. Imam Bonjol No.6, Medan",
                "lat"    to -3.5952,
                "lng"    to 98.6722,
                "jarak"  to "0.3 km"
            ),
            mapOf(
                "id"     to "kampus-a",
                "nama"   to "Trash2Cash - Kampus A",
                "alamat" to "Jl. Pendidikan No. 1, Jakarta",
                "lat"    to -6.2088,
                "lng"    to 106.8456,
                "jarak"  to "0.5 km"
            ),
            mapOf(
                "id"     to "sun-plaza-medan",
                "nama"   to "Trash2Cash - Sun Plaza",
                "alamat" to "Sun Plaza, Lt. 3, Medan",
                "lat"    to -3.5873,
                "lng"    to 98.6744,
                "jarak"  to "1.2 km"
            )
        )
        machines.forEach { m ->
            val id = m["id"] as String
            fsMachineDoc(id).set(m)
        }
    }
}
