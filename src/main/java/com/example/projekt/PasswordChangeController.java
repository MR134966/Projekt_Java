package com.example.projekt;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
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


        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Wszystkie pola są wymagane.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            statusLabel.setText("Nowe hasła nie są identyczne.");
            return;
        }


        if (newPassword.equals(currentPassword)) {
            statusLabel.setText("Nowe hasło nie może być takie samo jak aktualne.");
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
                transaction.rollback();
                return;
            }


            if (!currentPassword.equals(managedUser.getPasswordHash())) {
                statusLabel.setText("Błędne aktualne hasło.");
                transaction.rollback();
                return;
            }


            managedUser.setPassword(newPassword);

            session.merge(managedUser);

            transaction.commit();

            statusLabel.setText("Hasło zostało pomyślnie zmienione!");
            if (dialogStage != null) {
                dialogStage.close();
            }

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            statusLabel.setText("Błąd zmiany hasła: " + e.getMessage());
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