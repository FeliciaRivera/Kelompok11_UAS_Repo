/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.model;

// code ini ibarat kayak bikin aturan atau "kontrak" wajib
// tujuannya buat mastiin kalau kelas manapun yang ngehubungin interface ini, wajib banget punya fitur atau method buat nyetak struk
public interface CetakStruk {

    // nah, kalau code ini cuma buat ngenalin nama method nya aja,
    // nanti detail gimana cara programnya nyetak bakal ditulis di masing masing kelas yang make interface ini
    // >>> versi GUI (MVC): cetak() sekarang MENGEMBALIKAN teks struk (String), bukan langsung nampilin window.
    // jadi Model cuma nyusun isi struk, yang nampilin ke layar itu tugas View (FXML) lewat Controller.
    String cetak();
}
