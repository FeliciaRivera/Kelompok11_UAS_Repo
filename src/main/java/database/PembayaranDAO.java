/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import main.java.model.Pembayaran;
import main.java.model.PembayaranNonTunai;
import main.java.model.PembayaranTunai;

// DAO buat keluarga class Pembayaran (pola Joined Table, sama kayak PersonDAO di latihan)
// tabel yang diurus: pembayaran (parent) + pembayaran_tunai / pembayaran_non_tunai (child)
public class PembayaranDAO {

    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // query dasar: tabel parent di-LEFT JOIN ke kedua tabel child
    private static final String SELECT_PEMBAYARAN =
            "SELECT p.id, p.nomor_resi, p.jumlah_bayar, p.tanggal_bayar, "
          + "       t.uang_diterima, n.nomor_kartu, n.jenis_kartu "
          + "FROM pembayaran p "
          + "LEFT JOIN pembayaran_tunai t     ON t.pembayaran_id = p.id "
          + "LEFT JOIN pembayaran_non_tunai n ON n.pembayaran_id = p.id ";

    // ambil semua pembayaran (dipakai tab Pembayaran)
    public List<Pembayaran> getAllPembayaran() throws SQLException {
        List<Pembayaran> list = new ArrayList<>();
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PEMBAYARAN + "ORDER BY p.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(buatObject(rs));
            }
        }
        return list;
    }

    // ambil pembayaran milik 1 struk (dipakai StrukDAO waktu merakit ulang riwayat struk)
    // connection-nya dikirim dari StrukDAO supaya tidak buka koneksi baru untuk setiap struk
    public Pembayaran getPembayaranByResi(Connection conn, String nomorResi) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PEMBAYARAN + "WHERE p.nomor_resi = ?")) {
            ps.setString(1, nomorResi);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? buatObject(rs) : null;
            }
        }
    }

    // simpan pembayaran: insert ke tabel parent dulu, lalu ke tabel child sesuai tipe object
    // connection dari StrukDAO, jadi ikut 1 transaction dengan penyimpanan struk (commit / rollback bareng)
    public void insertPembayaran(Connection conn, String nomorResi, Pembayaran pembayaran) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO pembayaran (nomor_resi, jumlah_bayar, tanggal_bayar) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nomorResi);
            ps.setFloat(2, pembayaran.getJumlahBayar());
            ps.setDate(3, Date.valueOf(LocalDate.parse(pembayaran.getTanggalBayar(), FORMAT_TANGGAL)));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                pembayaran.setId(rs.getInt(1));   // id dari database disimpan ke object
            }
        }

        if (pembayaran instanceof PembayaranTunai) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pembayaran_tunai (pembayaran_id, uang_diterima) VALUES (?, ?)")) {
                ps.setInt(1, pembayaran.getId());
                ps.setFloat(2, ((PembayaranTunai) pembayaran).getUangDiterima());
                ps.executeUpdate();
            }
        } else if (pembayaran instanceof PembayaranNonTunai) {
            PembayaranNonTunai pnt = (PembayaranNonTunai) pembayaran;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pembayaran_non_tunai (pembayaran_id, nomor_kartu, jenis_kartu) VALUES (?, ?, ?)")) {
                ps.setInt(1, pembayaran.getId());
                ps.setLong(2, pnt.getNomorKartu());
                ps.setString(3, pnt.getJenisKartu());
                ps.executeUpdate();
            }
        }
    }

    // helper: 1 baris hasil query -> object PembayaranNonTunai atau PembayaranTunai (polymorphism)
    // kalau kolom jenis_kartu terisi berarti barisnya ada di pembayaran_non_tunai
    private Pembayaran buatObject(ResultSet rs) throws SQLException {
        float jumlah = rs.getFloat("jumlah_bayar");
        String tanggal = rs.getDate("tanggal_bayar").toLocalDate().format(FORMAT_TANGGAL);
        String jenisKartu = rs.getString("jenis_kartu");

        Pembayaran pembayaran;
        if (jenisKartu != null) {
            pembayaran = new PembayaranNonTunai(jumlah, tanggal, rs.getLong("nomor_kartu"), jenisKartu);
        } else {
            pembayaran = new PembayaranTunai(jumlah, tanggal, rs.getFloat("uang_diterima"));
        }
        pembayaran.setId(rs.getInt("id"));
        return pembayaran;
    }
}
