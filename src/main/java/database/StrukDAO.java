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
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import main.java.model.Aksesoris;
import main.java.model.Barang;
import main.java.model.Pakaian;
import main.java.model.Struk;

// DAO buat nyimpen & ngambil riwayat transaksi dari PostgreSQL
// tabel yang diurus: struk dan struk_barang (data pembayaran diurus PembayaranDAO)
public class StrukDAO {

    // format tanggal/waktu yang dipakai di model Struk (String) -> dikonversi ke DATE/TIME PostgreSQL
    private static final DateTimeFormatter FORMAT_TANGGAL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_WAKTU = DateTimeFormatter.ofPattern("HH:mm");

    // nomor urut transaksi berikutnya di tanggal tsb (ngisi 4 digit terakhir nomor resi, misal 0098)
    // versi CLI nomor urutnya hardcode "0098", jadi semua resi sama
    public int getNomorUrutBerikutnya(LocalDate tanggal) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(RIGHT(nomor_resi, 4) AS INTEGER)), 0) + 1 "
                   + "FROM struk WHERE tanggal = ?";
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // simpan 1 struk lengkap (header, daftar barang, pembayaran) dalam 1 transaction
    public void insertStruk(Struk struk) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) header struk
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO struk (nomor_resi, kode_toko, tanggal, waktu, subtotal, diskon, total) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, struk.getNomorResi());
                    ps.setString(2, struk.getKodeToko());
                    ps.setDate(3, keDate(struk.getTanggal()));
                    ps.setTime(4, Time.valueOf(LocalTime.parse(struk.getWaktu(), FORMAT_WAKTU)));
                    ps.setFloat(5, struk.getSubtotal());
                    ps.setFloat(6, struk.getDiskon());
                    ps.setFloat(7, struk.getTotal());
                    ps.executeUpdate();
                }

                // 2) daftar barang (association Struk 1 -- 1..* Barang)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO struk_barang (nomor_resi, kode_barang, qty, harga) VALUES (?, ?, ?, ?)")) {
                    for (Barang b : struk.getDaftarBarang()) {
                        ps.setString(1, struk.getNomorResi());
                        ps.setString(2, b.getKodeBarang());
                        ps.setInt(3, b.getQty());
                        ps.setFloat(4, b.getHarga());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 3) pembayaran disimpan lewat PembayaranDAO, tapi tetap di connection & transaction yang sama
                PembayaranDAO pembayaranDAO = new PembayaranDAO();
                pembayaranDAO.insertPembayaran(conn, struk.getNomorResi(), struk.getPembayaran());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ambil semua riwayat struk, dirakit ulang jadi object Struk lengkap (barang + pembayaran)
    public List<Struk> getAllStruk() throws SQLException {
        List<Struk> list = new ArrayList<>();

        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement psStruk = conn.prepareStatement(
                     "SELECT nomor_resi, kode_toko, tanggal, waktu FROM struk ORDER BY tanggal DESC, waktu DESC, nomor_resi DESC");
             PreparedStatement psBarang = conn.prepareStatement(
                     "SELECT sb.kode_barang, b.nama_barang, sb.qty, sb.harga, "
                   + "       p.kode_barang AS kode_pakaian, p.kategori, a.jenis "
                   + "FROM struk_barang sb "
                   + "JOIN barang b          ON b.kode_barang = sb.kode_barang "
                   + "LEFT JOIN pakaian p    ON p.kode_barang = sb.kode_barang "
                   + "LEFT JOIN aksesoris a  ON a.kode_barang = sb.kode_barang "
                   + "WHERE sb.nomor_resi = ? ORDER BY sb.id");
             ResultSet rs = psStruk.executeQuery()) {

            PembayaranDAO pembayaranDAO = new PembayaranDAO();
            while (rs.next()) {
                String resi = rs.getString("nomor_resi");
                Struk struk = new Struk(
                        rs.getString("kode_toko"),
                        resi,
                        rs.getDate("tanggal").toLocalDate().format(FORMAT_TANGGAL),
                        rs.getTime("waktu").toLocalTime().format(FORMAT_WAKTU));

                // barang di struk ini (pakai harga snapshot dari struk_barang)
                psBarang.setString(1, resi);
                try (ResultSet rb = psBarang.executeQuery()) {
                    while (rb.next()) {
                        String kode = rb.getString("kode_barang");
                        String nama = rb.getString("nama_barang");
                        int qty = rb.getInt("qty");
                        float harga = rb.getFloat("harga");
                        if (rb.getString("kode_pakaian") != null) {
                            struk.tambahBarang(new Pakaian(kode, nama, qty, harga, rb.getString("kategori")));
                        } else {
                            struk.tambahBarang(new Aksesoris(kode, nama, qty, harga, rb.getString("jenis")));
                        }
                    }
                }
                struk.hitungTotal();

                // pembayaran struk ini diambil lewat PembayaranDAO (object PembayaranTunai / PembayaranNonTunai)
                struk.setPembayaran(pembayaranDAO.getPembayaranByResi(conn, resi));

                list.add(struk);
            }
        }
        return list;
    }

    // hapus 1 struk. struk_barang & pembayaran (beserta child-nya) ikut kehapus karena ON DELETE CASCADE
    public void deleteStruk(String nomorResi) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM struk WHERE nomor_resi = ?")) {
            ps.setString(1, nomorResi);
            ps.executeUpdate();
        }
    }

    private Date keDate(String tanggal) {
        return Date.valueOf(LocalDate.parse(tanggal, FORMAT_TANGGAL));
    }
}
