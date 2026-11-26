/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.model;

import java.util.Locale;


// kelas ini spesial karena nerapin dua konsep OOP sekaligus
// pertama ada Inheritance (extends Pembayaran), Mewarisi sifat sifat dasar dari kelas induk Pembayaran
// kedua Interface (implements CetakStruk), Wajib ngikutin "kontrak" dari CetakStruk buat nyediain fitur cetak.
public class PembayaranNonTunai extends Pembayaran implements CetakStruk {

    // code ini buat nyimpen data spesifik kartu pelanggan, nomor dan jenis kartunya
    // tetap diset private, biar datanya aman
    // >>> versi GUI: tipe nomorKartu diganti int -> long, karena int maksimal cuma 10 digit,
    // sedangkan nomor kartu asli 16 digit. long tetap angka, jadi validasi angkanya sama.
    private long nomorKartu;
    private String jenisKartu;

    // ini tugasnya buat nyiapin data awal pas user pilih bayar pakai kartu
    public PembayaranNonTunai(float jumlahBayar, String tanggalBayar,
                               long nomorKartu, String jenisKartu) {
        // code 'super' ini buat menyampaikan data jumlah dan tanggal bayar biar diurus sama kelas induk Pembayaran
        super(jumlahBayar, tanggalBayar);
        // kalau yang ini buat ngurus data nomor dan jenis kartu di kelas ini sendiri
        this.nomorKartu = nomorKartu;
        this.jenisKartu = jenisKartu;
    }

    // masih sama ini buat ngatur dan ngambil data kartu kalau sewaktu waktu dibutuhin
    public void setNomorKartu(long nomorKartu) { this.nomorKartu = nomorKartu; }
    public void setJenisKartu(String jenisKartu) { this.jenisKartu = jenisKartu; }

    public long getNomorKartu() { return nomorKartu; }
    public String getJenisKartu() { return jenisKartu; }

    @Override
    public boolean konfirmasiPembayaran() {
        return nomorKartu > 0;    // kalau nomor kartunya ada isinya, berarti transaksinya valid
    }

    // override cetak() dari interface CetakStruk -> isinya konfirmasi "PAID" (beda total sama Struk.cetak())
    // hasilnya dikembalikan sebagai String, nanti Controller yang nampilin ke jendela (View)
    @Override
    public String cetak() {
        // nomor kartu disamarkan, cuma 4 digit terakhir yang keliatan (sama kayak di struk utama)
        String masked = "************" + String.format("%04d", nomorKartu % 10000);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================\n");
        sb.append("               TRANSAKSI BERHASIL\n");
        sb.append("================================================\n");
        sb.append(String.format("%-24s%24s", "Status", "PAID")).append("\n");
        sb.append(String.format("%-24s%24s", "Metode Pembayaran", "Online card payment")).append("\n");
        sb.append(String.format("%-24s%24s", "  Jenis Kartu", jenisKartu)).append("\n");
        sb.append(String.format("%-24s%24s", "  No. Pelanggan", masked)).append("\n");
        sb.append(String.format("%-24s%24s", "Jumlah Bayar",
                String.format(Locale.US, "Rp %,.0f", getJumlahBayar()))).append("\n");
        sb.append(String.format("%-24s%24s", "Tanggal", getTanggalBayar())).append("\n");
        sb.append("================================================\n");
        return sb.toString();
    }
}
