/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.controller;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import main.java.database.StrukDAO;
import main.java.model.CetakStruk;
import main.java.model.PembayaranNonTunai;
import main.java.model.Struk;

// controller layar Riwayat Struk (riwayat_struk.fxml)
// nampilin semua transaksi dari database, preview struk, hapus, dan ekspor ke file teks
public class RiwayatStrukController {

    @FXML private TableView<Struk> tblStruk;
    @FXML private TableColumn<Struk, String> colResi;
    @FXML private TableColumn<Struk, String> colTanggal;
    @FXML private TableColumn<Struk, String> colWaktu;
    @FXML private TableColumn<Struk, String> colItem;
    @FXML private TableColumn<Struk, String> colTotal;
    @FXML private TableColumn<Struk, String> colMetode;
    @FXML private TextArea txtPreview;
    @FXML private Label lblRingkasan;

    private final ObservableList<Struk> data = FXCollections.observableArrayList();
    private final StrukDAO strukDAO = new StrukDAO();

    @FXML
    private void initialize() {
        colResi.setCellValueFactory(new PropertyValueFactory<>("nomorResi"));
        colTanggal.setCellValueFactory(new PropertyValueFactory<>("tanggal"));
        colWaktu.setCellValueFactory(new PropertyValueFactory<>("waktu"));
        colItem.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getDaftarBarang().size())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(rupiah(c.getValue().getTotal())));
        colMetode.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().getPembayaran() instanceof PembayaranNonTunai)
                        ? "Kartu (" + ((PembayaranNonTunai) c.getValue().getPembayaran()).getJenisKartu() + ")"
                        : "Tunai"));
        tblStruk.setItems(data);

        // pilih baris -> struk lengkap ditampilkan lewat interface CetakStruk
        tblStruk.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            if (baru == null) {
                txtPreview.clear();
            } else {
                CetakStruk tampilan = baru;
                txtPreview.setText(tampilan.cetak());
            }
        });
    }

    public void muatData() {
        try {
            data.setAll(strukDAO.getAllStruk());
            float omzet = 0f;
            for (Struk s : data) omzet += s.getTotal();
            lblRingkasan.setText(data.size() + " transaksi  ·  total " + rupiah(omzet));
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal memuat riwayat", e.getMessage());
        }
    }

    @FXML
    private void handleMuatUlang() {
        muatData();
    }

    @FXML
    private void handleHapus() {
        Struk dipilih = tblStruk.getSelectionModel().getSelectedItem();
        if (dipilih == null) {
            tampilkanPesan(Alert.AlertType.WARNING, "Belum ada yang dipilih", "Pilih transaksi yang ingin dihapus.");
            return;
        }
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus transaksi " + dipilih.getNomorResi() + "?", ButtonType.YES, ButtonType.NO);
        konfirmasi.setHeaderText("Konfirmasi hapus");
        Optional<ButtonType> jawab = konfirmasi.showAndWait();
        if (jawab.isEmpty() || jawab.get() != ButtonType.YES) return;

        try {
            strukDAO.deleteStruk(dipilih.getNomorResi());
            muatData();
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal menghapus", e.getMessage());
        }
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
