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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import main.java.database.BarangDAO;
import main.java.model.Aksesoris;
import main.java.model.Barang;
import main.java.model.Pakaian;

// controller layar Katalog Barang (barang_table.fxml): CRUD barang ke PostgreSQL
// polanya ngikutin PersonTableController di materi TM10/TM12 (form + TableView + Submit/Update/Delete)
public class BarangTableController {

    @FXML private ComboBox<String> cmbTipe;
    @FXML private TextField txtKode;
    @FXML private TextField txtNama;
    @FXML private TextField txtHarga;
    @FXML private Label lblKeterangan;
    @FXML private TextField txtKeterangan;
    @FXML private Button btnSubmit;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    @FXML private TableView<Barang> tblBarang;
    @FXML private TableColumn<Barang, String> colTipe;
    @FXML private TableColumn<Barang, String> colKode;
    @FXML private TableColumn<Barang, String> colNama;
    @FXML private TableColumn<Barang, String> colHarga;
    @FXML private TableColumn<Barang, String> colKeterangan;

    private final ObservableList<Barang> data = FXCollections.observableArrayList();
    private final BarangDAO barangDAO = new BarangDAO();

    @FXML
    private void initialize() {
        cmbTipe.getItems().addAll("Pakaian", "Aksesoris");
        cmbTipe.setValue("Pakaian");
        // label keterangan ikut tipe: Pakaian punya 'kategori', Aksesoris punya 'jenis'
        cmbTipe.valueProperty().addListener((obs, lama, baru) ->
                lblKeterangan.setText("Pakaian".equals(baru) ? "Kategori" : "Jenis"));

        colTipe.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue() instanceof Pakaian) ? "Pakaian" : "Aksesoris"));
        colKode.setCellValueFactory(new PropertyValueFactory<>("kodeBarang"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colHarga.setCellValueFactory(c -> new SimpleStringProperty(
                String.format(Locale.US, "Rp %,.0f", c.getValue().getHarga())));
        colKeterangan.setCellValueFactory(c -> new SimpleStringProperty(keterangan(c.getValue())));
        tblBarang.setItems(data);

        tblBarang.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            if (baru != null) isiForm(baru);
        });
        aturTombol(false);
    }

    public void muatData() {
        try {
            data.setAll(barangDAO.getAllBarang());
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal memuat barang", e.getMessage());
        }
    }

    @FXML
    private void handleSubmit() {
        Barang barang = bacaForm();
        if (barang == null) return;
        try {
            barangDAO.insertBarang(barang);
            muatData();
            clearForm();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {   // unique_violation
                tampilkanPesan(Alert.AlertType.WARNING, "Kode sudah dipakai",
                        "Barang dengan kode " + barang.getKodeBarang() + " sudah ada.");
            } else {
                tampilkanPesan(Alert.AlertType.ERROR, "Gagal menyimpan barang", e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (tblBarang.getSelectionModel().getSelectedItem() == null) return;
        Barang barang = bacaForm();
        if (barang == null) return;
        try {
            barangDAO.updateBarang(barang);
            muatData();
            clearForm();
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal mengubah barang", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Barang dipilih = tblBarang.getSelectionModel().getSelectedItem();
        if (dipilih == null) return;

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus " + dipilih.getNamaBarang() + " dari katalog?", ButtonType.YES, ButtonType.NO);
        konfirmasi.setHeaderText("Konfirmasi hapus");
        Optional<ButtonType> jawab = konfirmasi.showAndWait();
        if (jawab.isEmpty() || jawab.get() != ButtonType.YES) return;

        try {
            barangDAO.deleteBarang(dipilih.getKodeBarang());
            muatData();
            clearForm();
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {   // foreign_key_violation
                tampilkanPesan(Alert.AlertType.WARNING, "Tidak bisa dihapus",
                        dipilih.getNamaBarang() + " sudah tercatat di riwayat transaksi, jadi tidak bisa dihapus.");
            } else {
                tampilkanPesan(Alert.AlertType.ERROR, "Gagal menghapus barang", e.getMessage());
            }
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    // ---------------- helper ----------------

    // baca & validasi isi form, lalu bikin object Pakaian / Aksesoris (polymorphism)
    private Barang bacaForm() {
        String kode = txtKode.getText().trim();
        String nama = txtNama.getText().trim();
        String ket = txtKeterangan.getText().trim();
        if (kode.isEmpty() || nama.isEmpty()) {
            tampilkanPesan(Alert.AlertType.WARNING, "Data belum lengkap", "Kode dan nama barang wajib diisi.");
            return null;
        }
        float harga;
        try {
            harga = Float.parseFloat(txtHarga.getText().trim().replaceAll("[\\s.,]", ""));
            if (harga < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            tampilkanPesan(Alert.AlertType.WARNING, "Harga tidak valid", "Harga harus berupa angka positif, mis. 599000.");
            return null;
        }
        if ("Pakaian".equals(cmbTipe.getValue())) {
            return new Pakaian(kode, nama, 1, harga, ket);
        }
        return new Aksesoris(kode, nama, 1, harga, ket);
    }

    private void isiForm(Barang b) {
        cmbTipe.setValue((b instanceof Pakaian) ? "Pakaian" : "Aksesoris");
        txtKode.setText(b.getKodeBarang());
        txtNama.setText(b.getNamaBarang());
        txtHarga.setText(String.format(Locale.US, "%.0f", b.getHarga()));
        txtKeterangan.setText(keterangan(b));
        txtKode.setDisable(true);   // kode = primary key, nggak diubah lewat Update
        aturTombol(true);
    }

    private void clearForm() {
        tblBarang.getSelectionModel().clearSelection();
        cmbTipe.setValue("Pakaian");
        txtKode.clear();
        txtNama.clear();
        txtHarga.clear();
        txtKeterangan.clear();
        txtKode.setDisable(false);
        aturTombol(false);
    }

    private void aturTombol(boolean adaPilihan) {
        btnSubmit.setDisable(adaPilihan);
        btnUpdate.setDisable(!adaPilihan);
        btnDelete.setDisable(!adaPilihan);
    }

    private String keterangan(Barang b) {
        if (b instanceof Pakaian) return ((Pakaian) b).getKategori();
        if (b instanceof Aksesoris) return ((Aksesoris) b).getJenis();
        return "";
    }

    private void tampilkanPesan(Alert.AlertType tipe, String judul, String isi) {
        Alert alert = new Alert(tipe);
        alert.setTitle(judul);
        alert.setHeaderText(judul);
        alert.setContentText(isi);
        alert.showAndWait();
    }
}
