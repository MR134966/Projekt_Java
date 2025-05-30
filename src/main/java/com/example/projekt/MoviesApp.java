package com.example.projekt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class MoviesApp extends Application {

    private static void ensureAdminUserPassword() {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = JPAUtil.createEntityManager();
            tx = em.getTransaction();
            tx.begin();

            User adminUser;
            String adminUsername = "admin";
            String adminPasswordToSet = "admin";

            try {
                TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class
                );
                query.setParameter("username", adminUsername);
                adminUser = query.getSingleResult();
                System.out.println("Znaleziono użytkownika admin.");


                adminUser.setPassword(adminPasswordToSet); 
                em.merge(adminUser);

            } catch (NoResultException e) {
                System.out.println("Użytkownik admin nie istnieje. Tworzenie nowego użytkownika admin.");
                adminUser = new User();
                adminUser.setUsername(adminUsername);
                adminUser.setPassword(adminPasswordToSet); 
                em.persist(adminUser);
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
            if (em != null && em.isOpen()) {
                em.close();
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

    public static void main(String[] args) {

        ensureAdminUserPassword();


        launch(args);
    }
}