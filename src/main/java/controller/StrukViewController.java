/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

// controller jendela struk (struk_view.fxml)
// dipakai buat nampilin hasil cetak() dari object yang implements CetakStruk (Struk & PembayaranNonTunai)
public class StrukViewController {

    @FXML private Label lblJudul;
    @FXML private TextArea txtIsi;

    public void setIsi(String judul, String isi) {
        lblJudul.setText(judul);
        txtIsi.setText(isi);
    }

    @FXML
    private void handleTutup() {
        ((Stage) txtIsi.getScene().getWindow()).close();
    }

    // buka struk_view.fxml sebagai jendela baru (modal), mirip openTableStage() di contoh materi TM10
    public static void tampilkan(String judul, String isi, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(StrukViewController.class.getResource("/main/resources/struk_view.fxml"));
            Scene scene = new Scene(loader.load(), 500, 700);
            scene.getStylesheets().add(StrukViewController.class
                    .getResource("/main/resources/css/style.css").toExternalForm());

            StrukViewController controller = loader.getController();
            controller.setIsi(judul, isi);

            Stage stage = new Stage();
            stage.setTitle(judul);
            stage.setScene(scene);
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
