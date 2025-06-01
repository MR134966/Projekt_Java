package com.example.projekt;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;


public class ClientController {

    @FXML
    private Label userLabel;
    @FXML
    private TableView<Movie> moviesTable;
    @FXML
    private TableColumn<Movie, String> titleColumn;
    @FXML
    private TableColumn<Movie, Integer> yearColumn;
    @FXML
    private TableColumn<Movie, String> genreColumn;
    @FXML
    private TableColumn<Movie, String> directorColumn;
    @FXML
    private TableColumn<Movie, Float> ratingColumn;
    @FXML
    private TableColumn<Movie, String> notesColumn;
    @FXML
    private TableColumn<Movie, String> availabilityColumn;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterGenreComboBox;

    @FXML
    private TextField titleField;
    @FXML
    private TextField yearField;
    @FXML
    private ComboBox<String> genreComboBox;
    @FXML
    private ComboBox<String> directorComboBox;
    @FXML
    private TextField ratingField;
    @FXML
    private TextArea notesField;

    @FXML
    private Button rentButton;
    @FXML
    private Button returnButton;
    @FXML
    private Label statusLabel;

    @FXML
    private TableView<Rental> rentedMoviesTable;
    @FXML
    private TableColumn<Rental, String> rentedTitleColumn;
    @FXML
    private TableColumn<Rental, LocalDate> rentedReturnDateColumn;
    @FXML
    private Button changePasswordButton;

    private User currentUser;
    private ObservableList<Movie> masterMovieList = FXCollections.observableArrayList();
    private FilteredList<Movie> filteredMovieList;
    private ObservableList<Rental> userRentalsList = FXCollections.observableArrayList();


    @FXML
    public void initialize() {

        titleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        yearColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getYear()));
        genreColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGenre() != null ? cellData.getValue().getGenre().getName() : ""));
        directorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDirector() != null ? cellData.getValue().getDirector().getName() : ""));
        ratingColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getRating()));
        notesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNotes()));
        availabilityColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().isAvailable() ? "Dostępny" : "Wypożyczony"));


        rentedTitleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getMovie() != null ? cellData.getValue().getMovie().getTitle() : ""));
        rentedReturnDateColumn.setCellValueFactory(cellData -> cellData.getValue().expectedReturnDateProperty());



        moviesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showMovieDetails(newValue));


        rentedMoviesTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showRentedMovieDetails(newValue));



        filteredMovieList = new FilteredList<>(masterMovieList, p -> true);
        SortedList<Movie> sortedData = new SortedList<>(filteredMovieList);
        sortedData.comparatorProperty().bind(moviesTable.comparatorProperty());
        moviesTable.setItems(sortedData);


        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMovieList.setPredicate(movie -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return movie.getTitle().toLowerCase().contains(lowerCaseFilter);
            });
        });


        filterGenreComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            filteredMovieList.setPredicate(movie -> {
                if (newValue == null || newValue.equals("Wszystkie gatunki")) {
                    return true;
                }

                return movie.getGenre() != null && movie.getGenre().getName().equals(newValue);
            });
        });


        loadGenresToComboBox();
        loadDirectorsToComboBox();
    }


    public void setCurrentUser(User user) {
        this.currentUser = user;

        if (userLabel != null) {
            userLabel.setText("Zalogowano: " + user.getUsername() + " (Rola: " + user.getRole() + ")");
        }
        loadAllMovies();
        loadUserRentals();
    }


    private void loadAllMovies() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Query<Movie> query = session.createQuery("SELECT m FROM Movie m", Movie.class);
            masterMovieList.setAll(query.getResultList());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania filmów: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    private void loadUserRentals() {
        if (currentUser == null) {
            System.err.println("currentUser is null in loadUserRentals. Cannot load rentals.");
            return;
        }
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            Query<Rental> query = session.createQuery(
                    "SELECT r FROM Rental r JOIN FETCH r.movie WHERE r.user = :user AND r.returnDate IS NULL", Rental.class);
            query.setParameter("user", currentUser);
            userRentalsList.setAll(query.getResultList());
            rentedMoviesTable.setItems(userRentalsList);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania wypożyczonych filmów: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    private void showMovieDetails(Movie movie) {

        rentedMoviesTable.getSelectionModel().clearSelection();

        if (movie != null) {
            titleField.setText(movie.getTitle());
            yearField.setText(String.valueOf(movie.getYear()));
            genreComboBox.getSelectionModel().select(movie.getGenre() != null ? movie.getGenre().getName() : null);
            directorComboBox.getSelectionModel().select(movie.getDirector() != null ? movie.getDirector().getName() : null);
            ratingField.setText(String.valueOf(movie.getRating()));
            notesField.setText(movie.getNotes());


            rentButton.setDisable(!movie.isAvailable());
            returnButton.setDisable(true);
        } else {

            clearMovieDetails();
            rentButton.setDisable(true);
            returnButton.setDisable(true);
        }
    }


    private void showRentedMovieDetails(Rental rental) {

        moviesTable.getSelectionModel().clearSelection();

        if (rental != null && rental.getMovie() != null) {
            Movie movie = rental.getMovie();
            titleField.setText(movie.getTitle());
            yearField.setText(String.valueOf(movie.getYear()));
            genreComboBox.getSelectionModel().select(movie.getGenre() != null ? movie.getGenre().getName() : null);
            directorComboBox.getSelectionModel().select(movie.getDirector() != null ? movie.getDirector().getName() : null);
            ratingField.setText(String.valueOf(movie.getRating()));
            notesField.setText(movie.getNotes());

            rentButton.setDisable(true);
            returnButton.setDisable(false);
        } else {
            clearMovieDetails();
            rentButton.setDisable(true);
            returnButton.setDisable(true);
        }
    }



    private void clearMovieDetails() {
        titleField.clear();
        yearField.clear();
        genreComboBox.getSelectionModel().clearSelection();
        directorComboBox.getSelectionModel().clearSelection();
        ratingField.clear();
        notesField.clear();
        statusLabel.setText("");
    }


    private void loadGenresToComboBox() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            Query<Genre> query = session.createQuery("SELECT g FROM Genre g ORDER BY g.name", Genre.class);
            ObservableList<String> genres = FXCollections.observableArrayList();
            for (Genre g : query.getResultList()) {
                genres.add(g.getName());
            }
            genres.add(0, "Wszystkie gatunki");
            filterGenreComboBox.setItems(genres);

            genreComboBox.setItems(genres.filtered(g -> !g.equals("Wszystkie gatunki")));
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania gatunków: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }


    private void loadDirectorsToComboBox() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            Query<Director> query = session.createQuery("SELECT d FROM Director d ORDER BY d.name", Director.class);
            ObservableList<String> directors = FXCollections.observableArrayList();
            for (Director d : query.getResultList()) {
                directors.add(d.getName());
            }
            directorComboBox.setItems(directors);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania reżyserów: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }



    @FXML
    private void handleRentMovie() {
        Movie selectedMovie = moviesTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            statusLabel.setText("Proszę wybrać film do wypożyczenia.");
            return;
        }

        if (!selectedMovie.isAvailable()) {
            statusLabel.setText("Ten film jest już wypożyczony.");
            return;
        }


        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdź wypożyczenie");
        alert.setHeaderText("Czy na pewno chcesz wypożyczyć film \"" + selectedMovie.getTitle() + "\"?");
        alert.setContentText("Okres wypożyczenia to 14 dni.");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Session session = null;
            Transaction transaction = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();


                Movie managedMovie = session.find(Movie.class, selectedMovie.getId());
                if (managedMovie == null) {
                    throw new RuntimeException("Film nie znaleziony w kontekście Hibernate.");
                }
                managedMovie.setAvailable(false);
                session.merge(managedMovie);


                Rental newRental = new Rental();
                newRental.setMovie(managedMovie);

                newRental.setUser(session.find(User.class, currentUser.getId()));
                newRental.setRentalDate(LocalDate.now());
                newRental.setExpectedReturnDate(LocalDate.now().plusDays(14));

                session.persist(newRental);

                transaction.commit();

                statusLabel.setText("Film \"" + selectedMovie.getTitle() + "\" został wypożyczony pomyślnie!");
                loadAllMovies();
                loadUserRentals();
                clearMovieDetails();

            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                e.printStackTrace();
                statusLabel.setText("Błąd podczas wypożyczania filmu: " + e.getMessage());
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
        }
    }

    @FXML
    private void handleReturnMovie() {
        Rental selectedRental = rentedMoviesTable.getSelectionModel().getSelectedItem();
        if (selectedRental == null) {
            statusLabel.setText("Proszę wybrać film do zwrotu z listy wypożyczonych.");
            return;
        }


        if (!selectedRental.getUser().getId().equals(currentUser.getId()) || selectedRental.getReturnDate() != null) {
            statusLabel.setText("Ten film nie jest wypożyczony przez Ciebie lub już został zwrócony.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdź zwrot");
        alert.setHeaderText("Czy na pewno chcesz zwrócić film \"" + selectedRental.getMovie().getTitle() + "\"?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Session session = null;
            Transaction transaction = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();


                Rental managedRental = session.find(Rental.class, selectedRental.getId());
                if (managedRental == null) {
                    throw new RuntimeException("Wypożyczenie nie znalezione w kontekście Hibernate.");
                }
                Movie managedMovie = managedRental.getMovie();


                managedRental.setReturnDate(LocalDate.now());
                session.merge(managedRental);


                managedMovie.setAvailable(true);
                session.merge(managedMovie);

                transaction.commit();

                statusLabel.setText("Film \"" + managedMovie.getTitle() + "\" został zwrócony pomyślnie!");
                loadAllMovies();
                loadUserRentals();
                clearMovieDetails();

            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                e.printStackTrace();
                statusLabel.setText("Błąd podczas zwracania filmu: " + e.getMessage());
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
        }
    }
    @FXML
    private void handleChangePassword() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Błąd", "Brak zalogowanego użytkownika.");
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/projekt/password-change-view.fxml"));
            Parent parent = fxmlLoader.load();

            PasswordChangeController passwordChangeController = fxmlLoader.getController();

            Stage stage = new Stage();
            stage.setTitle("Zmień Hasło");
            stage.setScene(new Scene(parent));

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(changePasswordButton.getScene().getWindow());

            passwordChangeController.setCurrentUser(currentUser);
            passwordChangeController.setDialogStage(stage);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Błąd podczas ładowania okna zmiany hasła: " + e.getMessage());
        }
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie wylogowania");
        alert.setHeaderText("Czy na pewno chcesz się wylogować?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projekt/login-view.fxml"));
                Parent loginRoot = loader.load();


                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(loginRoot);

                URL cssUrl = getClass().getResource("/com/example/projekt/styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("Nie można znaleźć pliku CSS dla widoku logowania!");
                }

                ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("pl"));
                if (bundle.containsKey("app.title.login")) {
                    stage.setTitle(bundle.getString("app.title.login"));
                } else {
                    stage.setTitle("Logowanie");
                }
                stage.setScene(scene);
                stage.centerOnScreen();
                stage.show();

                currentUser = null;

            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Błąd podczas wylogowywania: " + e.getMessage());
            }
        }
    }
}