package com.example.projekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;


import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class MoviesApp extends Application {

    private static void ensureAdminUserPassword() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            User adminUser;
            String adminUsername = "admin";
            String adminPasswordToSet = "admin";

            Query<User> query = session.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class
            );
            query.setParameter("username", adminUsername);
            adminUser = query.uniqueResult();

            if (adminUser != null) {
                System.out.println("Znaleziono użytkownika admin.");
                adminUser.setPassword(adminPasswordToSet);
                session.merge(adminUser);
            } else {
                System.out.println("Użytkownik admin nie istnieje. Tworzenie nowego użytkownika admin.");
                adminUser = new User();
                adminUser.setUsername(adminUsername);
                adminUser.setPassword(adminPasswordToSet);
                session.persist(adminUser);
            }

            tx.commit();
            System.out.println("Użytkownik 'admin' gotowy z poprawnie ustawionym hasłem.");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            System.err.println("Błąd podczas zapewniania istnienia/aktualizacji hasła użytkownika admin: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("pl"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projekt/login-view.fxml"), bundle);
        Parent root = loader.load();

        Scene scene = new Scene(root);

        URL cssUrl = getClass().getResource("/com/example/projekt/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Nie można znaleźć pliku CSS: styles.css");
        }

        if (bundle.containsKey("app.title")) {
            primaryStage.setTitle(bundle.getString("app.title"));
        } else {
            primaryStage.setTitle("FrameKeeper");
        }
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        HibernateUtil.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        ensureAdminUserPassword();
        launch(args);
    }
}