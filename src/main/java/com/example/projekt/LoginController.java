package com.example.projekt;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private User currentUser;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Proszę podać nazwę użytkownika i hasło.");
            return;
        }

        if (authenticate(username, password)) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("pl"));
                FXMLLoader loader;
                String fxmlPath;
                String appTitleKey;
                Parent rootNode;

                if (currentUser.getRole() != null && currentUser.getRole().equalsIgnoreCase("ADMIN")) {
                    fxmlPath = "/com/example/projekt/hello-view.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
                    rootNode = loader.load();
                    MoviesController moviesController = loader.getController();
                    moviesController.setCurrentUser(currentUser);
                    appTitleKey = "app.title.admin";
                } else if (currentUser.getRole() != null && currentUser.getRole().equalsIgnoreCase("USER")) {
                    fxmlPath = "/com/example/projekt/client-view.fxml";
                    loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
                    rootNode = loader.load();
                    ClientController clientController = loader.getController();
                    clientController.setCurrentUser(currentUser);
                    appTitleKey = "app.title.client";
                } else {
                    errorLabel.setText("Brak uprawnień lub nieznana rola użytkownika.");
                    currentUser = null;
                    return;
                }

                Stage stage = (Stage) usernameField.getScene().getWindow();
                Scene scene = new Scene(rootNode);

                URL cssUrl = getClass().getResource("/com/example/projekt/styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("OSTRZEŻENIE: Nie można znaleźć pliku CSS: styles.css. Upewnij się, że jest w src/main/resources/com/example/projekt/");
                }

                if (bundle.containsKey(appTitleKey)) {
                    stage.setTitle(bundle.getString(appTitleKey));
                } else {
                    stage.setTitle("FrameKeeper");
                }
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.setResizable(true);
                stage.show();

            } catch (IOException e) {
                System.err.println("Błąd ładowania widoku FXML: " + e.getMessage());
                e.printStackTrace();
                errorLabel.setText("Wystąpił błąd podczas ładowania interfejsu użytkownika.");
            } catch (Exception e) {
                System.err.println("Nieoczekiwany błąd po zalogowaniu: " + e.getMessage());
                e.printStackTrace();
                errorLabel.setText("Wystąpił nieoczekiwany błąd. Spróbuj ponownie.");
            }
        }
    }

    private boolean authenticate(String username, String plainPassword) {
        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Query<User> query = session.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class
            );
            query.setParameter("username", username);

            User user = query.uniqueResult();

            tx.commit();

            if (user != null && user.checkPassword(plainPassword)) {
                this.currentUser = user;
                errorLabel.setText("");
                return true;
            } else {
                errorLabel.setText("Nieprawidłowa nazwa użytkownika lub hasło.");
                this.currentUser = null;
                return false;
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            System.err.println("Błąd autentykacji: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("Błąd bazy danych podczas logowania.");
            this.currentUser = null;
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }
}