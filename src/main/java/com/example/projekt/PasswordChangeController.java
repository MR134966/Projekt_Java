package com.example.projekt;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.hibernate.Session;
import org.hibernate.Transaction;


public class PasswordChangeController {

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;

    private User currentUser;
    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        statusLabel.getStyleClass().removeAll("error-label", "success-label");


        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Wszystkie pola są wymagane.");
            statusLabel.getStyleClass().add("error-label");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            statusLabel.setText("Nowe hasła nie są identyczne.");
            statusLabel.getStyleClass().add("error-label");
            return;
        }

        if (newPassword.equals(currentPassword)) {
            statusLabel.setText("Nowe hasło nie może być takie samo jak aktualne.");
            statusLabel.getStyleClass().add("error-label");
            return;
        }


        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            User managedUser = session.get(User.class, currentUser.getId());

            if (managedUser == null) {
                statusLabel.setText("Błąd: Użytkownik nie znaleziony w bazie danych.");
                statusLabel.getStyleClass().add("error-label");
                transaction.rollback();
                return;
            }

            if (!currentPassword.equals(managedUser.getPasswordHash())) {
                statusLabel.setText("Błędne aktualne hasło.");
                statusLabel.getStyleClass().add("error-label");
                transaction.rollback();
                return;
            }

            managedUser.setPassword(newPassword);

            session.merge(managedUser);

            transaction.commit();

            statusLabel.setText("Hasło zostało pomyślnie zmienione!");
            statusLabel.getStyleClass().add("success-label");
            if (dialogStage != null) {
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(event -> dialogStage.close());
                pause.play();
            }

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            statusLabel.setText("Błąd zmiany hasła: " + e.getMessage());
            statusLabel.getStyleClass().add("error-label");
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}