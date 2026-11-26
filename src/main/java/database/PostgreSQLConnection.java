/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// kelas ini tugasnya cuma satu: bikin koneksi JDBC ke database PostgreSQL
// semua DAO (BarangDAO, StrukDAO) minta koneksi lewat sini, jadi kalau URL/user/password berubah cukup ganti di satu tempat
public class PostgreSQLConnection {

    private static final String URL = "jdbc:postgresql://localhost:5433/siqlo";
    private static final String USER = "felicia";
    private static final String PASSWORD = "ivel1103";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
