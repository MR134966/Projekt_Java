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
                stage = primaryStage;
            } else {
                System.err.println("Nie można uzyskać Stage do zmiany sceny: primaryStage nie jest ustawione, a event jest null lub nie pochodzi od Node.");
                return;
            }

            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("pl"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile), bundle);
            Parent root = loader.load();

            Object controller = loader.getController();


            if (controller instanceof MoviesController && currentUser != null) {


                if ("/com/example/projekt/hello-view.fxml".equals(fxmlFile)) {
                     ((MoviesController) controller).setCurrentUser(currentUser);
                }
            }



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
            } else if (bundle.containsKey("window.title")) {
                stage.setTitle(bundle.getString("window.title"));
            }
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Błąd ładowania widoku", "Nie można załadować widoku: " + fxmlFile, "Szczegóły: " + e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            showAlert("Błąd zasobu", "Nie można znaleźć pliku FXML: " + fxmlFile, "Szczegóły: " + e.getMessage());
        }
    }

    public void switchToLoginView(ActionEvent event) {
        loadAndSetScene("/com/example/projekt/login-view.fxml", event, "window.title", null);
    }

    public void switchToMoviesView(ActionEvent event, User currentUser) {

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