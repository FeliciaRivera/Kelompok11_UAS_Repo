/*
 * Program Proyek GUI (JavaFX + MVC) dari Class Diagram Struk Siqlo
 * @author: Deio Castello Sujati (825250002), Felicia Rivera (825250003), MUhammad Ubait Dhaifullah (825250012)
 */

package main.java.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// View utama: load layout main_window.fxml lalu tampilkan di primaryStage
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/resources/main_window.fxml"));
        Scene scene = new Scene(loader.load(), 1180, 740);
        scene.getStylesheets().add(getClass().getResource("/main/resources/css/style.css").toExternalForm());

        primaryStage.setTitle("Siqlo PIK Avenue - Sistem Kasir");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}