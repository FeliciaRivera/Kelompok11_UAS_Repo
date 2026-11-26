/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.controller;

import java.sql.Connection;
import java.sql.SQLException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import main.java.database.PostgreSQLConnection;

// controller jendela utama: ngatur tab (Kasir, Katalog Barang, Riwayat Struk, Pembayaran), menu bar, dan status koneksi database
public class MainWindowController {

    @FXML private TabPane tabPane;
    @FXML private Tab tabKasir;
    @FXML private Tab tabBarang;
    @FXML private Tab tabRiwayat;
    @FXML private Tab tabPembayaran;
    @FXML private Label lblStatusDb;

    // controller dari file fxml yang di-include (nama field = fx:id + "Controller")
    @FXML private KasirController kasirViewController;
    @FXML private BarangTableController barangViewController;
    @FXML private RiwayatStrukController riwayatViewController;
    @FXML private PembayaranTableController pembayaranViewController;

    @FXML
    private void initialize() {
        if (cekKoneksi()) {
            kasirViewController.muatData();
        }

        // tiap pindah tab, data di tab itu dimuat ulang dari database biar selalu sinkron
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            if (baru == tabKasir) {
                kasirViewController.muatData();
            } else if (baru == tabBarang) {
                barangViewController.muatData();
            } else if (baru == tabRiwayat) {
                riwayatViewController.muatData();
            } else if (baru == tabPembayaran) {
                pembayaranViewController.muatData();
            }
        });
    }

    @FXML
    private void handleOpenKasir() { tabPane.getSelectionModel().select(tabKasir); }

    @FXML
    private void handleOpenBarang() { tabPane.getSelectionModel().select(tabBarang); }

    @FXML
    private void handleOpenRiwayat() { tabPane.getSelectionModel().select(tabRiwayat); }

    @FXML
    private void handleOpenPembayaran() { tabPane.getSelectionModel().select(tabPembayaran); }

    @FXML
    private void handleExit() { Platform.exit(); }

    @FXML
    private void handleTentang() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tentang Aplikasi");
        alert.setHeaderText("Sistem Kasir Siqlo - Proyek GUI PBO Kelompok 11");
        alert.setContentText("JavaFX (MVC) + PostgreSQL (JDBC)\n\n"
                + "Deio Castello Sujati (825250002)\n"
                + "Felicia Rivera (825250003)\n"
                + "Muhammad Ubait Dhaifullah (825250012)");
        alert.showAndWait();
    }

    // cek sekali di awal apakah PostgreSQL bisa dihubungi, hasilnya ditampilkan di pojok kanan header
    private boolean cekKoneksi() {
        try (Connection conn = PostgreSQLConnection.getConnection()) {
            if (!conn.isValid(2)) {   // cek koneksinya benar-benar aktif (timeout 2 detik)
                throw new SQLException("Koneksi ke database tidak valid.");
            }
            lblStatusDb.setText("● Database terhubung");
            lblStatusDb.getStyleClass().add("status-ok");
            return true;
        } catch (SQLException e) {
            lblStatusDb.setText("● Database tidak terhubung");
            lblStatusDb.getStyleClass().add("status-gagal");
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Koneksi Database");
                alert.setHeaderText("Tidak bisa terhubung ke PostgreSQL");
                alert.setContentText("Pastikan server PostgreSQL berjalan dan database beserta tabelnya sudah dibuat "
                        + "(jalankan database/siqlo_schema.sql di pgAdmin).\n\nDetail: " + e.getMessage());
                alert.showAndWait();
            });
            return false;
        }
    }
}