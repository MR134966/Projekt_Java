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
import javafx.stage.Stage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.event.ActionEvent;

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
        EntityManager em = null;
        try {
            em = JPAUtil.createEntityManager();
            TypedQuery<Movie> query = em.createQuery("SELECT m FROM Movie m", Movie.class);
            masterMovieList.setAll(query.getResultList());
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania filmów: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }


    private void loadUserRentals() {
        if (currentUser == null) {
            System.err.println("currentUser is null in loadUserRentals. Cannot load rentals.");
            return;
        }
        EntityManager em = null;
        try {
            em = JPAUtil.createEntityManager();

            TypedQuery<Rental> query = em.createQuery(
                    "SELECT r FROM Rental r JOIN FETCH r.movie WHERE r.user = :user AND r.returnDate IS NULL", Rental.class);
            query.setParameter("user", currentUser);
            userRentalsList.setAll(query.getResultList());
            rentedMoviesTable.setItems(userRentalsList);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania wypożyczonych filmów: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
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
        EntityManager em = null;
        try {
            em = JPAUtil.createEntityManager();

            TypedQuery<Genre> query = em.createQuery("SELECT g FROM Genre g ORDER BY g.name", Genre.class);
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
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }


    private void loadDirectorsToComboBox() {
        EntityManager em = null;
        try {
            em = JPAUtil.createEntityManager();

            TypedQuery<Director> query = em.createQuery("SELECT d FROM Director d ORDER BY d.name", Director.class);
            ObservableList<String> directors = FXCollections.observableArrayList();
            for (Director d : query.getResultList()) {
                directors.add(d.getName());
            }
            directorComboBox.setItems(directors);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania reżyserów: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
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
            EntityManager em = null;
            try {
                em = JPAUtil.createEntityManager();
                em.getTransaction().begin();


                Movie managedMovie = em.find(Movie.class, selectedMovie.getId());
                if (managedMovie == null) {
                    throw new RuntimeException("Film nie znaleziony w kontekście JPA.");
                }
                managedMovie.setAvailable(false);
                em.merge(managedMovie);


                Rental newRental = new Rental();
                newRental.setMovie(managedMovie);

                newRental.setUser(em.find(User.class, currentUser.getId()));
                newRental.setRentalDate(LocalDate.now());
                newRental.setExpectedReturnDate(LocalDate.now().plusDays(14));

                em.persist(newRental);

                em.getTransaction().commit();

                statusLabel.setText("Film \"" + selectedMovie.getTitle() + "\" został wypożyczony pomyślnie!");
                loadAllMovies();
                loadUserRentals();
                clearMovieDetails();

            } catch (Exception e) {
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
                statusLabel.setText("Błąd podczas wypożyczania filmu: " + e.getMessage());
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
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
            EntityManager em = null;
            try {
                em = JPAUtil.createEntityManager();
                em.getTransaction().begin();


                Rental managedRental = em.find(Rental.class, selectedRental.getId());
                if (managedRental == null) {
                    throw new RuntimeException("Wypożyczenie nie znalezione w kontekście JPA.");
                }
                Movie managedMovie = managedRental.getMovie();


                managedRental.setReturnDate(LocalDate.now());
                em.merge(managedRental);


                managedMovie.setAvailable(true);
                em.merge(managedMovie);

                em.getTransaction().commit();

                statusLabel.setText("Film \"" + managedMovie.getTitle() + "\" został zwrócony pomyślnie!");
                loadAllMovies();
                loadUserRentals();
                clearMovieDetails();

            } catch (Exception e) {
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
                statusLabel.setText("Błąd podczas zwracania filmu: " + e.getMessage());
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        }
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


    @FXML
    private void handleReset() {
        clearMovieDetails();
        moviesTable.getSelectionModel().clearSelection();
        rentedMoviesTable.getSelectionModel().clearSelection();
        statusLabel.setText("");
    }
}