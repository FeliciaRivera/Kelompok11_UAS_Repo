/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.model;

import java.util.ArrayList;
import java.util.Locale;

// kelas Struk ini ibarat mesin kasir nya
// dia yang bakal ngumpulin semua barang, ngitung total, dan wajib bisa nyetak
// >>> versi GUI (MVC): Struk murni jadi MODEL. Semua kode Swing (JFrame, LogoKotak, tampilkanJendela)
// udah dihapus, karena urusan tampilan sekarang dipegang View (FXML) + Controller.
public class Struk implements CetakStruk {

    // code ini buat nyimpen data data dasar struk kayak kode toko, nomor resi, dan waktu transaksi
    // semuanya di set private biar datanya nggak gampang bocor atau diubah sembarangan dari luar
    private String kodeToko;
    private String nomorResi;
    private String tanggal;
    private String waktu;

    /* kalau code ini nunjukin konsep Association (Hubungan antar kelas)
    * ceritanya 1 lembar struk bisa nyimpen banyak Barang makanya pakai ArrayList, dan 1 lembar Struk cuma punya 1 jenis Pembayaran
    */
    private ArrayList<Barang> daftarBarang;
    private Pembayaran pembayaran;

    // code ini buat nyimpen angka angka penting, subtotal, diskon, dan total akhir
    private float subtotal;
    private float diskon;
    private float total;

    private static final int LEBAR = 48;   // lebar kertas struk

    // aturan Welcome Coupon sesuai struk asli: potongan Rp25.000 dengan syarat belanja minimal Rp250.000
    public static final float KUPON_WELCOME = 25000f;
    public static final float MIN_KUPON = 250000f;

     // ini tugasnya buat nyalain mesin pas ada transaksi baru
    // disini kita ngisi data awal struk sekaligus nyiapin keranjang kosong buat nampung barang
    public Struk(String kodeToko, String nomorResi, String tanggal, String waktu) {
        this.kodeToko = kodeToko;
        this.nomorResi = nomorResi;
        this.tanggal = tanggal;
        this.waktu = waktu;
        this.daftarBarang = new ArrayList<>();
        this.subtotal = 0f;
        this.diskon = 0f;
        this.total = 0f;
    }

    // metode ini ibarat kasir lagi nge scan barang, setiap ada barang baru, dimasukin ke 'daftarBarang'
    public void tambahBarang(Barang barang) {
        daftarBarang.add(barang);
    }

    // metode ini buat nyatet si pembeli ini bayarnya pakai apa (Tunai atau Non Tunai)
    public void setPembayaran(Pembayaran pembayaran) {
        this.pembayaran = pembayaran;
    }

    public void setKodeToko(String kodeToko) { this.kodeToko = kodeToko; }

    public String getKodeToko() { return kodeToko; }
    public String getNomorResi() { return nomorResi; }
    public String getTanggal() { return tanggal; }
    public String getWaktu() { return waktu; }
    public ArrayList<Barang> getDaftarBarang() { return daftarBarang; }
    public Pembayaran getPembayaran() { return pembayaran; }
    public float getSubtotal() { return subtotal; }
    public float getDiskon() { return diskon; }
    public float getTotal() { return total; }

    /* code ini buat ngitung total belanjaan.
    * harga Siqlo itu TAX INCLUSIVE (Pajak udah masuk di harga)
    * jadi, di metode ini kita cukup ngitung total harga biasa aja
    * nilai pajaknya nggak perlu ditambahin lagi ke total biar pelanggannya nggak kena tagih pajak dua kali
    * >>> versi GUI: diskon Welcome Coupon sekarang cuma berlaku kalau subtotal >= Rp250.000 (Min. Kupon)
    *     sebelumnya diskon selalu dipotong, jadi belanja kecil bisa bikin total minus
    */
    public float hitungTotal() {
        subtotal = 0f;
        for (Barang b : daftarBarang) {
            subtotal += b.hitungSubtotal();
        }
        diskon = (subtotal >= MIN_KUPON) ? KUPON_WELCOME : 0f;
        total = subtotal - diskon;
        return total;
    }

    /* ini implementasi wajib dari interface CetakStruk
    * code ini bakal ngerakit struk secara utuh dari atas sampai bawah,
    * >>> versi GUI: hasilnya dikembalikan sebagai String, nanti Controller yang nampilin ke jendela struk (View)
    */
    @Override
    public String cetak() {
        StringBuilder sb = new StringBuilder();

        // KOP STRUK
        sb.append(tengah("SIQLO")).append("\n");
        sb.append(tengah("WWW.SIQLO.COM")).append("\n\n");
        sb.append("Siqlo PIK Avenue\n");
        sb.append("Jl. Pantai Indah Barat No.1 RT004 RW 003\n");
        sb.append("Kamal Muara, Penjaringan\n");
        sb.append("Jakarta 14470\n");
        sb.append("PT. Fast Retailing Indonesia\n");
        sb.append("Tel: 021-29752950\n\n");
        sb.append(tengah("** Receipt **")).append("\n\n");

        sb.append(tanggal).append("\n");
        sb.append(kolom("<" + kodeToko + ">", "[" + waktu + "]")).append("\n\n");

        int unitTerjual = 0;
        for (Barang b : daftarBarang) {
            // harga yang dicetak = subtotal per baris (qty x harga), biar bener kalau qty lebih dari 1
            String hargaT = rupiah(b.hitungSubtotal()) + " T";

            if (b instanceof Pakaian) {
                Pakaian p = (Pakaian) b;
                unitTerjual += p.getQty();
                sb.append("  ").append(p.getNamaBarang()).append("\n");
                sb.append(String.format("  %-19s%3d%24s",
                        p.getKodeBarang(), p.getQty(), hargaT)).append("\n");

            } else if (b instanceof Aksesoris) {
                Aksesoris a = (Aksesoris) b;
                String nama = a.getQty() > 1 ? a.getNamaBarang() + " x" + a.getQty() : a.getNamaBarang();
                sb.append(String.format("  %-22s%24s", nama, hargaT)).append("\n");

            } else {
                sb.append(String.format("  %-22s%24s", b.getNamaBarang(), hargaT)).append("\n");
            }
        }

        sb.append(garis('-')).append("\n");

        // REKAP HARGA
        sb.append(String.format("  %-19s%3d", "Unit yang terjual", unitTerjual)).append("\n");
        sb.append(kolom("Subtotal", rupiah(subtotal))).append("\n");
        if (diskon > 0) {
            sb.append(kolom("  Welcome Coupon " + rupiah(diskon), "-" + rupiah(diskon))).append("\n");
            sb.append("    Min. Kupon ").append(rupiah(MIN_KUPON)).append("\n");
        }
        sb.append(kolom("TOTAL", rupiah(total))).append("\n\n");

        // METODE PEMBAYARAN
        sb.append("Metode Pembayaran\n");

        Long nomorPelanggan = null;   // >>> Long (ngikutin tipe nomorKartu yang sekarang long) <<<

        if (pembayaran instanceof PembayaranNonTunai) {
            PembayaranNonTunai pnt = (PembayaranNonTunai) pembayaran;
            sb.append(kolom("Online card payment", rupiah(pnt.getJumlahBayar()))).append("\n");
            sb.append("  ").append(pnt.getJenisKartu()).append("\n");
            nomorPelanggan = pnt.getNomorKartu();   // long -> Long (autoboxing)

        } else if (pembayaran instanceof PembayaranTunai) {
            PembayaranTunai pt = (PembayaranTunai) pembayaran;
            sb.append(kolom("Tunai", rupiah(pt.getJumlahBayar()))).append("\n");
            sb.append(kolom("  Uang diterima", rupiah(pt.getUangDiterima()))).append("\n");
            sb.append(kolom("  Kembalian", rupiah(pt.hitungKembalian()))).append("\n");
        }

        // Hitung pajak (tax inclusive, dihitung dari TOTAL setelah diskon, sama kayak struk asli)
        float hargaSebelumPajak = total / (1 + Barang.TARIF_PPN);
        float totalHarga = (float) Math.floor(hargaSebelumPajak);
        float dppNilaiLain = (float) Math.floor(hargaSebelumPajak * 11 / 12);
        float ppn = total - totalHarga;

        sb.append("Tax inclusive:\n");
        sb.append("  Total harga ").append(rupiah(totalHarga)).append("\n");
        sb.append("    DPP nilai lain ").append(rupiah(dppNilaiLain)).append("\n");
        sb.append(kolom("    PPN", rupiah(ppn))).append("\n\n");

        // IDENTITAS TRANSAKSI
        if (nomorPelanggan != null) {
            // Masking: tampilkan 4 digit terakhir saja
            String masked = "*********" + String.format("%04d", nomorPelanggan % 10000);
            sb.append(kolom("No. Pelanggan", masked)).append("\n");
        }
        sb.append(kolom("Receipt no.", nomorResi)).append("\n");
        sb.append(garis('='));

        return sb.toString();
    }


    private String kolom(String kiri, String kanan) {
        if (kanan.isEmpty()) return kiri;
        int spasi = LEBAR - kiri.length() - kanan.length();
        if (spasi < 1) spasi = 1;
        return kiri + " ".repeat(spasi) + kanan;
    }

    private String tengah(String teks) {
        int spasi = (LEBAR - teks.length()) / 2;
        return " ".repeat(Math.max(spasi, 0)) + teks;
    }

    private String garis(char c) {
        return String.valueOf(c).repeat(LEBAR);
    }

    private String rupiah(float nilai) {
        return String.format(Locale.US, "Rp %,.0f", nilai);
    }
}
