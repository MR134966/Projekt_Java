package com.example.projekt;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;


import com.example.projekt.User;
import com.example.projekt.MoviesController;

public class ScreenManager {

    private static ScreenManager instance;
    private Stage primaryStage;

    private ScreenManager() {

    }

    public static ScreenManager getInstance() {
        if (instance == null) {
            instance = new ScreenManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void loadAndSetScene(String fxmlFile, ActionEvent event, String windowTitleKey, User currentUser) {
        try {
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else if (primaryStage != null) {
                stage = primaryStage; // Użyj primaryStage, jeśli event nie dostarcza źródła
            } else {
                System.err.println("Nie można uzyskać Stage do zmiany sceny: primaryStage nie jest ustawione, a event jest null lub nie pochodzi od Node.");
                return;
            }

            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("pl")); // Użyj Locale.forLanguageTag dla spójności
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile), bundle);
            Parent root = loader.load();

            Object controller = loader.getController();

            // Przekazywanie currentUser do odpowiednich kontrolerów
            if (controller instanceof MoviesController && currentUser != null) {
                // Sprawdzamy, czy fxmlFile odpowiada widokowi filmów, aby uniknąć niepotrzebnego rzutowania
                // lub upewnij się, że tylko MoviesController potrzebuje currentUser w ten sposób
                if ("/com/example/projekt/hello-view.fxml".equals(fxmlFile)) { // Dostosuj ścieżkę, jeśli jest inna
                     ((MoviesController) controller).setCurrentUser(currentUser);
                }
            }
            // Można dodać inne warunki `else if` dla innych kontrolerów, jeśli potrzebują currentUser
            // np. if (controller instanceof UserProfileController && currentUser != null) { ... }


            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/com/example/projekt/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Nie można znaleźć pliku CSS: /com/example/projekt/styles.css");
            }

            stage.setScene(scene);
            if (windowTitleKey != null && bundle.containsKey(windowTitleKey)) {
                stage.setTitle(bundle.getString(windowTitleKey));
            } else if (bundle.containsKey("window.title")) { // Domyślny tytuł
                stage.setTitle(bundle.getString("window.title"));
            }
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Błąd ładowania widoku", "Nie można załadować widoku: " + fxmlFile, "Szczegóły: " + e.getMessage());
        } catch (IllegalStateException e) { // Obsługa błędu, jeśli getResource zwróci null
            e.printStackTrace();
            showAlert("Błąd zasobu", "Nie można znaleźć pliku FXML: " + fxmlFile, "Szczegóły: " + e.getMessage());
        }
    }

    public void switchToLoginView(ActionEvent event) {
        loadAndSetScene("/com/example/projekt/login-view.fxml", event, "window.title", null);
    }

    public void switchToMoviesView(ActionEvent event, User currentUser) {
        // Upewnij się, że ścieżka do hello-view.fxml jest poprawna
        loadAndSetScene("/com/example/projekt/hello-view.fxml", event, "window.title.movies", currentUser);
    }

    private void showAlert(String title, String header, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}