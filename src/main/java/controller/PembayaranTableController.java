/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.controller;

import java.sql.SQLException;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import main.java.database.PembayaranDAO;
import main.java.model.Pembayaran;
import main.java.model.PembayaranNonTunai;
import main.java.model.PembayaranTunai;

// controller layar Pembayaran (pembayaran_table.fxml)
// 1 controller untuk 1 keluarga inheritance: Pembayaran, PembayaranTunai, PembayaranNonTunai
// (polanya sama kayak PersonTableController di latihan yang ngurus Person, Seller, Buyer)
public class PembayaranTableController {

    @FXML private ComboBox<String> cmbFilter;
    @FXML private TableView<Pembayaran> tblPembayaran;
    @FXML private TableColumn<Pembayaran, String> colId;
    @FXML private TableColumn<Pembayaran, String> colTanggal;
    @FXML private TableColumn<Pembayaran, String> colMetode;
    @FXML private TableColumn<Pembayaran, String> colJumlah;
    @FXML private TableColumn<Pembayaran, String> colUangDiterima;
    @FXML private TableColumn<Pembayaran, String> colKembalian;
    @FXML private TableColumn<Pembayaran, String> colJenisKartu;
    @FXML private TableColumn<Pembayaran, String> colNomorKartu;
    @FXML private Label lblRingkasan;

    private final ObservableList<Pembayaran> data = FXCollections.observableArrayList();
    private final PembayaranDAO pembayaranDAO = new PembayaranDAO();

    @FXML
    private void initialize() {
        cmbFilter.getItems().addAll("Semua", "Tunai", "Non-Tunai");
        cmbFilter.setValue("Semua");

        // kolom umum (milik class induk Pembayaran)
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colTanggal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTanggalBayar()));
        colMetode.setCellValueFactory(c -> new SimpleStringProperty(metode(c.getValue())));
        colJumlah.setCellValueFactory(c -> new SimpleStringProperty(rupiah(c.getValue().getJumlahBayar())));

        // kolom khusus subclass: isinya tergantung tipe object (instanceof)
        colUangDiterima.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue() instanceof PembayaranTunai)
                        ? rupiah(((PembayaranTunai) c.getValue()).getUangDiterima()) : "-"));
        colKembalian.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue() instanceof PembayaranTunai)
                        ? rupiah(((PembayaranTunai) c.getValue()).hitungKembalian()) : "-"));
        colJenisKartu.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue() instanceof PembayaranNonTunai)
                        ? ((PembayaranNonTunai) c.getValue()).getJenisKartu() : "-"));
        colNomorKartu.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue() instanceof PembayaranNonTunai)
                        ? "************" + String.format("%04d", ((PembayaranNonTunai) c.getValue()).getNomorKartu() % 10000)
                        : "-"));

        tblPembayaran.setItems(data);
    }

    // dipanggil MainWindowController tiap tab Pembayaran dibuka
    public void muatData() {
        try {
            data.setAll(pembayaranDAO.getAllPembayaran());
            handleFilter();
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal memuat pembayaran", e.getMessage());
        }
    }

    // filter tabel berdasarkan metode yang dipilih di ComboBox
    @FXML
    private void handleFilter() {
        String pilihan = cmbFilter.getValue();
        if ("Tunai".equals(pilihan)) {
            tblPembayaran.setItems(data.filtered(p -> p instanceof PembayaranTunai));
        } else if ("Non-Tunai".equals(pilihan)) {
            tblPembayaran.setItems(data.filtered(p -> p instanceof PembayaranNonTunai));
        } else {
            tblPembayaran.setItems(data);
        }
        updateRingkasan();
    }

    // ringkasan jumlah transaksi dan total per metode
    private void updateRingkasan() {
        float totalTunai = 0f, totalKartu = 0f;
        for (Pembayaran p : data) {
            if (p instanceof PembayaranTunai) totalTunai += p.getJumlahBayar();
            else totalKartu += p.getJumlahBayar();
        }
        lblRingkasan.setText(data.size() + " pembayaran  ·  Tunai " + rupiah(totalTunai)
                + "  ·  Non-Tunai " + rupiah(totalKartu));
    }

    private String metode(Pembayaran p) {
        return (p instanceof PembayaranNonTunai) ? "Non-Tunai" : "Tunai";
    }

    private String rupiah(float nilai) {
        return String.format(Locale.US, "Rp %,.0f", nilai);
    }

    private void tampilkanPesan(Alert.AlertType tipe, String judul, String isi) {
        Alert alert = new Alert(tipe);
        alert.setTitle(judul);
        alert.setHeaderText(judul);
        alert.setContentText(isi);
        alert.showAndWait();
    }
}
