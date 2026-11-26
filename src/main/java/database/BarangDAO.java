/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.java.model.Aksesoris;
import main.java.model.Barang;
import main.java.model.Pakaian;

// DAO (Data Access Object) buat tabel barang + pakaian + aksesoris (pola Joined Table)
// isinya CRUD: ambil semua, tambah, ubah, hapus
public class BarangDAO {

    // ambil semua barang. LEFT JOIN ke pakaian & aksesoris buat nentuin object yang dibikin (polymorphism)
    public List<Barang> getAllBarang() throws SQLException {
        List<Barang> list = new ArrayList<>();
        String sql = "SELECT b.kode_barang, b.nama_barang, b.harga, "
                   + "       p.kode_barang AS kode_pakaian, p.kategori, a.jenis "
                   + "FROM barang b "
                   + "LEFT JOIN pakaian p   ON p.kode_barang = b.kode_barang "
                   + "LEFT JOIN aksesoris a ON a.kode_barang = b.kode_barang "
                   + "ORDER BY (p.kode_barang IS NULL), b.nama_barang";

        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String kode = rs.getString("kode_barang");
                String nama = rs.getString("nama_barang");
                float harga = rs.getFloat("harga");

                // qty katalog = 1 (qty sebenarnya ditentukan kasir pas transaksi)
                Barang barang;
                if (rs.getString("kode_pakaian") != null) {
                    barang = new Pakaian(kode, nama, 1, harga, rs.getString("kategori"));
                } else {
                    barang = new Aksesoris(kode, nama, 1, harga, rs.getString("jenis"));
                }
                list.add(barang);
            }
        }
        return list;
    }

    // tambah barang baru: insert ke tabel parent (barang) lalu ke tabel child (pakaian / aksesoris)
    // pakai transaction biar dua insert itu sukses bareng atau gagal bareng
    public void insertBarang(Barang barang) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO barang (kode_barang, nama_barang, harga) VALUES (?, ?, ?)")) {
                    ps.setString(1, barang.getKodeBarang());
                    ps.setString(2, barang.getNamaBarang());
                    ps.setFloat(3, barang.getHarga());
                    ps.executeUpdate();
                }
                insertChild(conn, barang);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ubah barang: update tabel parent, lalu child dihapus & diisi ulang (biar bisa ganti tipe Pakaian <-> Aksesoris)
    public void updateBarang(Barang barang) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE barang SET nama_barang = ?, harga = ? WHERE kode_barang = ?")) {
                    ps.setString(1, barang.getNamaBarang());
                    ps.setFloat(2, barang.getHarga());
                    ps.setString(3, barang.getKodeBarang());
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Barang dengan kode " + barang.getKodeBarang() + " tidak ditemukan.");
                    }
                }
                try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM pakaian WHERE kode_barang = ?");
                     PreparedStatement ps2 = conn.prepareStatement("DELETE FROM aksesoris WHERE kode_barang = ?")) {
                    ps1.setString(1, barang.getKodeBarang());
                    ps1.executeUpdate();
                    ps2.setString(1, barang.getKodeBarang());
                    ps2.executeUpdate();
                }
                insertChild(conn, barang);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // hapus barang. baris di pakaian/aksesoris ikut kehapus (ON DELETE CASCADE).
    // kalau barang udah pernah terjual, PostgreSQL nolak (ON DELETE RESTRICT di struk_barang)
    public void deleteBarang(String kodeBarang) throws SQLException {
        try (Connection conn = PostgreSQLConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM barang WHERE kode_barang = ?")) {
            ps.setString(1, kodeBarang);
            ps.executeUpdate();
        }
    }

    // helper: insert ke tabel child sesuai tipe object (instanceof)
    private void insertChild(Connection conn, Barang barang) throws SQLException {
        if (barang instanceof Pakaian) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO pakaian (kode_barang, kategori) VALUES (?, ?)")) {
                ps.setString(1, barang.getKodeBarang());
                ps.setString(2, ((Pakaian) barang).getKategori());
                ps.executeUpdate();
            }
        } else if (barang instanceof Aksesoris) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO aksesoris (kode_barang, jenis) VALUES (?, ?)")) {
                ps.setString(1, barang.getKodeBarang());
                ps.setString(2, ((Aksesoris) barang).getJenis());
                ps.executeUpdate();
            }
        }
    }
}
