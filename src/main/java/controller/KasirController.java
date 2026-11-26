/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.controller;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import main.java.database.BarangDAO;
import main.java.database.StrukDAO;
import main.java.model.Aksesoris;
import main.java.model.Barang;
import main.java.model.CetakStruk;
import main.java.model.Pakaian;
import main.java.model.Pembayaran;
import main.java.model.PembayaranNonTunai;
import main.java.model.PembayaranTunai;
import main.java.model.Struk;

// controller layar Kasir (kasir.fxml)
// alurnya sama kayak App.java versi CLI: pilih barang -> hitung total -> pilih metode bayar -> konfirmasi -> cetak struk
// bedanya sekarang lewat form GUI, dan hasilnya disimpan ke PostgreSQL
public class KasirController {

    private static final String KODE_TOKO = "1510";
    private static final String NOMOR_KASIR = "31";

    // ---------- katalog ----------
    @FXML private TextField txtCari;
    @FXML private TableView<Barang> tblKatalog;
    @FXML private TableColumn<Barang, String> colKatTipe;
    @FXML private TableColumn<Barang, String> colKatKode;
    @FXML private TableColumn<Barang, String> colKatNama;
    @FXML private TableColumn<Barang, String> colKatHarga;
    @FXML private Spinner<Integer> spnQty;

    // ---------- keranjang ----------
    @FXML private TableView<Barang> tblKeranjang;
    @FXML private TableColumn<Barang, String> colKerNama;
    @FXML private TableColumn<Barang, String> colKerQty;
    @FXML private TableColumn<Barang, String> colKerHarga;
    @FXML private TableColumn<Barang, String> colKerSubtotal;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDiskon;
    @FXML private Label lblTotal;
    @FXML private Label lblInfoKupon;

    // ---------- pembayaran ----------
    @FXML private ToggleGroup tgMetode;
    @FXML private RadioButton rbTunai;
    @FXML private RadioButton rbNonTunai;
    @FXML private TextField txtUangDiterima;
    @FXML private Label lblKembalian;
    @FXML private TextField txtNomorKartu;
    @FXML private ComboBox<String> cmbJenisKartu;

    private final ObservableList<Barang> katalog = FXCollections.observableArrayList();
    private final ObservableList<Barang> keranjang = FXCollections.observableArrayList();

    private final BarangDAO barangDAO = new BarangDAO();
    private final StrukDAO strukDAO = new StrukDAO();

    @FXML
    private void initialize() {
        // tabel katalog. PropertyValueFactory manggil getter (getKodeBarang, getNamaBarang) dari model
        colKatTipe.setCellValueFactory(c -> new SimpleStringProperty(tipeBarang(c.getValue())));
        colKatKode.setCellValueFactory(new PropertyValueFactory<>("kodeBarang"));
        colKatNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colKatHarga.setCellValueFactory(c -> new SimpleStringProperty(rupiah(c.getValue().getHarga())));

        FilteredList<Barang> katalogTersaring = new FilteredList<>(katalog, b -> true);
        txtCari.textProperty().addListener((obs, lama, baru) -> {
            String kata = baru.trim().toLowerCase();
            katalogTersaring.setPredicate(b -> kata.isEmpty()
                    || b.getNamaBarang().toLowerCase().contains(kata)
                    || b.getKodeBarang().toLowerCase().contains(kata));
        });
        tblKatalog.setItems(katalogTersaring);
        tblKatalog.setOnMouseClicked(e -> { if (e.getClickCount() == 2) handleTambah(); });

        spnQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));

        // tabel keranjang
        colKerNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colKerQty.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQty())));
        colKerHarga.setCellValueFactory(c -> new SimpleStringProperty(rupiah(c.getValue().getHarga())));
        colKerSubtotal.setCellValueFactory(c -> new SimpleStringProperty(rupiah(c.getValue().hitungSubtotal())));
        tblKeranjang.setItems(keranjang);

        // pembayaran
        cmbJenisKartu.getItems().addAll("JCB", "VISA", "Mastercard", "BCA Debit", "Mandiri Debit", "BNI Debit");
        tgMetode.selectedToggleProperty().addListener((obs, lama, baru) -> aturFormPembayaran());
        txtUangDiterima.textProperty().addListener((obs, lama, baru) -> updateKembalian());
        aturFormPembayaran();
        updateRingkasan();
    }

    // dipanggil MainWindowController tiap tab Kasir dibuka: ambil katalog terbaru dari database
    public void muatData() {
        try {
            katalog.setAll(barangDAO.getAllBarang());
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal memuat katalog", e.getMessage());
        }
    }

    @FXML
    private void handleTambah() {
        Barang dipilih = tblKatalog.getSelectionModel().getSelectedItem();
        if (dipilih == null) {
            tampilkanPesan(Alert.AlertType.WARNING, "Belum ada barang dipilih", "Pilih barang di tabel katalog terlebih dahulu.");
            return;
        }
        int qty = bacaQty();

        // kalau barang yang sama udah ada di keranjang, qty-nya ditambah aja
        for (Barang b : keranjang) {
            if (b.getKodeBarang().equals(dipilih.getKodeBarang())) {
                b.setQty(b.getQty() + qty);
                tblKeranjang.refresh();
                updateRingkasan();
                return;
            }
        }
        // object baru (bukan object katalog) supaya qty di keranjang nggak ngubah data katalog
        keranjang.add(salinBarang(dipilih, qty));
        updateRingkasan();
    }

    @FXML
    private void handleHapusItem() {
        Barang dipilih = tblKeranjang.getSelectionModel().getSelectedItem();
        if (dipilih != null) {
            keranjang.remove(dipilih);
            updateRingkasan();
        }
    }

    @FXML
    private void handleKosongkan() {
        keranjang.clear();
        updateRingkasan();
    }

    @FXML
    private void handleBayar() {
        // 1) keranjang nggak boleh kosong (di versi CLI ini masih bisa lolos)
        if (keranjang.isEmpty()) {
            tampilkanPesan(Alert.AlertType.WARNING, "Keranjang kosong", "Tambahkan minimal satu barang sebelum membayar.");
            return;
        }

        // 2) waktu transaksi & nomor resi (nomor urut diambil dari database)
        LocalDateTime sekarang = LocalDateTime.now();
        String tanggal = sekarang.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String waktu = sekarang.format(DateTimeFormatter.ofPattern("HH:mm"));
        int nomorUrut;
        try {
            nomorUrut = strukDAO.getNomorUrutBerikutnya(sekarang.toLocalDate());
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Database error", "Gagal membuat nomor resi.\n" + e.getMessage());
            return;
        }
        String nomorResi = KODE_TOKO + "-" + sekarang.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "-" + NOMOR_KASIR + "-" + String.format("%04d", nomorUrut);

        Struk struk = buatStruk(nomorResi, tanggal, waktu);

        // 3) bikin object Pembayaran sesuai pilihan (polymorphism)
        Pembayaran pembayaran;
        if (rbTunai.isSelected()) {
            float uangDiterima;
            try {
                uangDiterima = bacaNominal(txtUangDiterima.getText());
            } catch (NumberFormatException e) {
                tampilkanPesan(Alert.AlertType.WARNING, "Input tidak valid", "Uang diterima harus berupa angka, mis. 1200000.");
                return;
            }
            pembayaran = new PembayaranTunai(struk.getTotal(), tanggal, uangDiterima);
        } else {
            String nomor = txtNomorKartu.getText().trim();
            long nomorKartu;
            try {
                if (!nomor.matches("\\d{12,19}")) throw new NumberFormatException();
                nomorKartu = Long.parseLong(nomor);
            } catch (NumberFormatException e) {
                tampilkanPesan(Alert.AlertType.WARNING, "Nomor kartu tidak valid", "Nomor kartu harus 12-19 digit angka.");
                return;
            }
            if (cmbJenisKartu.getValue() == null) {
                tampilkanPesan(Alert.AlertType.WARNING, "Jenis kartu kosong", "Pilih jenis kartu terlebih dahulu.");
                return;
            }
            pembayaran = new PembayaranNonTunai(struk.getTotal(), tanggal, nomorKartu, cmbJenisKartu.getValue());
        }
        struk.setPembayaran(pembayaran);

        // 4) konfirmasi pembayaran. tunai yang kurang ditolak, keranjang tetap disimpan biar kasir bisa koreksi
        if (!pembayaran.konfirmasiPembayaran()) {
            if (pembayaran instanceof PembayaranTunai) {
                PembayaranTunai pt = (PembayaranTunai) pembayaran;
                float kurang = pt.getJumlahBayar() - pt.getUangDiterima();
                tampilkanPesan(Alert.AlertType.ERROR, "Pembayaran ditolak",
                        "Uang kurang " + rupiah(kurang) + ". Transaksi belum diproses.");
            } else {
                tampilkanPesan(Alert.AlertType.ERROR, "Pembayaran ditolak", "Pembayaran kartu gagal diverifikasi.");
            }
            return;
        }

        // 5) simpan ke database
        try {
            strukDAO.insertStruk(struk);
        } catch (SQLException e) {
            tampilkanPesan(Alert.AlertType.ERROR, "Gagal menyimpan transaksi", e.getMessage());
            return;
        }

        // 6) tampilkan lewat interface CetakStruk: konfirmasi PAID dulu (khusus non-tunai), lalu struk lengkap
        if (pembayaran instanceof CetakStruk) {
            CetakStruk konfirmasiOnline = (CetakStruk) pembayaran;
            StrukViewController.tampilkan("Transaksi Berhasil", konfirmasiOnline.cetak(), tblKatalog.getScene().getWindow());
        }
        CetakStruk tampilan = struk;
        StrukViewController.tampilkan("Struk " + nomorResi, tampilan.cetak(), tblKatalog.getScene().getWindow());

        resetTransaksi();
    }

    // ---------------- helper ----------------

    // rakit object Struk dari isi keranjang, lalu hitung total (aturan diskon ada di model Struk)
    private Struk buatStruk(String nomorResi, String tanggal, String waktu) {
        Struk struk = new Struk(KODE_TOKO, nomorResi, tanggal, waktu);
        for (Barang b : keranjang) {
            struk.tambahBarang(b);
        }
        struk.hitungTotal();
        return struk;
    }

    private void updateRingkasan() {
        Struk sementara = buatStruk("-", "", "");
        lblSubtotal.setText(rupiah(sementara.getSubtotal()));
        lblDiskon.setText("-" + rupiah(sementara.getDiskon()));
        lblTotal.setText(rupiah(sementara.getTotal()));
        lblInfoKupon.setText(sementara.getDiskon() > 0
                ? "Welcome Coupon diterapkan."
                : "Welcome Coupon " + rupiah(Struk.KUPON_WELCOME) + " berlaku untuk belanja min. " + rupiah(Struk.MIN_KUPON));
        updateKembalian();
    }

    // preview kembalian langsung saat kasir mengetik uang diterima
    private void updateKembalian() {
        lblKembalian.getStyleClass().remove("teks-merah");
        String teks = txtUangDiterima.getText();
        if (teks == null || teks.isBlank() || keranjang.isEmpty()) {
            lblKembalian.setText("-");
            return;
        }
        try {
            float selisih = bacaNominal(teks) - buatStruk("-", "", "").getTotal();
            if (selisih >= 0) {
                lblKembalian.setText(selisih == 0 ? "Rp 0 (uang pas)" : rupiah(selisih));
            } else {
                lblKembalian.setText("Kurang " + rupiah(-selisih));
                lblKembalian.getStyleClass().add("teks-merah");
            }
        } catch (NumberFormatException e) {
            lblKembalian.setText("Format angka salah");
            lblKembalian.getStyleClass().add("teks-merah");
        }
    }

    private void aturFormPembayaran() {
        boolean tunai = rbTunai.isSelected();
        txtUangDiterima.setDisable(!tunai);
        txtNomorKartu.setDisable(tunai);
        cmbJenisKartu.setDisable(tunai);
    }

    private void resetTransaksi() {
        keranjang.clear();
        txtUangDiterima.clear();
        txtNomorKartu.clear();
        cmbJenisKartu.getSelectionModel().clearSelection();
        rbTunai.setSelected(true);
        spnQty.getValueFactory().setValue(1);
        updateRingkasan();
    }

    private Barang salinBarang(Barang b, int qty) {
        if (b instanceof Pakaian) {
            Pakaian p = (Pakaian) b;
            return new Pakaian(p.getKodeBarang(), p.getNamaBarang(), qty, p.getHarga(), p.getKategori());
        }
        Aksesoris a = (Aksesoris) b;
        return new Aksesoris(a.getKodeBarang(), a.getNamaBarang(), qty, a.getHarga(), a.getJenis());
    }

    private int bacaQty() {
        try {
            int qty = Integer.parseInt(spnQty.getEditor().getText().trim());
            if (qty >= 1 && qty <= 99) {
                spnQty.getValueFactory().setValue(qty);
                return qty;
            }
        } catch (NumberFormatException e) {
            // kalau editor diisi bukan angka, pakai nilai spinner terakhir
        }
        return spnQty.getValue();
    }

    // terima input "1200000", "1.200.000", "1,200,000", atau "Rp 1.200.000"
    private float bacaNominal(String teks) {
        String bersih = teks.trim().replace("Rp", "").replaceAll("[\\s.,]", "");
        if (bersih.isEmpty()) throw new NumberFormatException();
        return Float.parseFloat(bersih);
    }

    private String tipeBarang(Barang b) {
        return (b instanceof Pakaian) ? "Pakaian" : "Aksesoris";
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
