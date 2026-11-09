package edu.uph.m24si2.trash2cash

/**
 * Model data untuk satu item riwayat transaksi.
 *
 * Dipakai di: RiwayatActivity, WalletActivity
 *
 * status:
 *   "diterima"  → botol diterima (dari RTDB, ditulis ESP32)
 *   "ditolak"   → botol ditolak  (dari RTDB, ditulis ESP32)
 *   "penarikan" → tarik saldo    (dari Firestore, ditulis app)
 */
data class RiwayatItem(
    val judul: String,
    val deskripsi: String,
    val waktu: String,
    val status: String,
    val timestamp: Long
)
