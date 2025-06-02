package com.example.projekt;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Alert.AlertType;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class MoviesController {

    @FXML
    private TableView<Movie> moviesTable;
    private ObservableList<Movie> moviesList = FXCollections.observableArrayList();
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
    private TableColumn<Movie, String> rentedByUserColumn;

    @FXML
    private TextField titleField, yearField, ratingField, searchField;
    @FXML
    private ComboBox<Genre> genreComboBox, filterGenreComboBox;
    @FXML
    private ComboBox<Director> directorComboBox;
    @FXML
    private TextArea notesField;
    @FXML
    private Button editButton, deleteButton, logoutButton;
    @FXML
    private Label userLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> passwordColumn;
    @FXML private Button addUserButton;
    @FXML private Button deleteUserButton;
    private ObservableList<User> users = FXCollections.observableArrayList();

    private ObservableList<Movie> movies = FXCollections.observableArrayList();
    private FilteredList<Movie> filteredMovies;
    private SortedList<Movie> sortedMovies;
    private ObservableList<Genre> genres = FXCollections.observableArrayList();
    private ObservableList<Director> directors = FXCollections.observableArrayList();

    private User currentUser;

    @FXML
    public void initialize() {
        directorComboBox.setConverter(new StringConverter<Director>() {
            @Override
            public String toString(Director director) {
                return director == null ? null : director.getName();
            }

            @Override
            public Director fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                return directors.stream()
                        .filter(d -> d.getName().equalsIgnoreCase(string.trim()))
                        .findFirst()
                        .orElse(null);
            }
        });
        directorComboBox.setEditable(true);

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));

        genreColumn.setCellValueFactory(cellData -> {
            Movie movie = cellData.getValue();
            if (movie != null && movie.getGenre() != null) {
                return new SimpleStringProperty(movie.getGenre().getName());
            }
            return new SimpleStringProperty("N/A");
        });
        directorColumn.setCellValueFactory(cellData -> {
            Movie movie = cellData.getValue();
            if (movie != null && movie.getDirector() != null) {
                return new SimpleStringProperty(movie.getDirector().getName());
            }
            return new SimpleStringProperty("N/A");
        });
        if (notesColumn != null) {
            notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        }

        availabilityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isAvailable() ? "Dostępny" : "Wypożyczony"));
        rentedByUserColumn.setCellValueFactory(cellData -> {
            Movie movie = cellData.getValue();
            if (movie != null && !movie.isAvailable()) {
                Session session = null;
                try {
                    session = HibernateUtil.getSessionFactory().openSession();

                    Query<Rental> query = session.createQuery(
                            "SELECT r FROM Rental r WHERE r.movie = :movie AND r.returnDate IS NULL", Rental.class);
                    query.setParameter("movie", movie);

                    List<Rental> rentals = query.getResultList();
                    if (!rentals.isEmpty()) {
                        User user = rentals.get(0).getUser();
                        return new SimpleStringProperty(user != null ? user.getUsername() : "Nieznany");
                    }
                } catch (Exception e) {
                    System.err.println("Błąd pobierania użytkownika dla filmu " + movie.getTitle() + ": " + e.getMessage());
                } finally {
                    if (session != null && session.isOpen()) {
                        session.close();
                    }
                }
            }
            return new SimpleStringProperty("-");
        });


        filteredMovies = new FilteredList<>(movies, p -> true);
        sortedMovies = new SortedList<>(filteredMovies);
        sortedMovies.comparatorProperty().bind(moviesTable.comparatorProperty());
        moviesTable.setItems(sortedMovies);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
        filterGenreComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
        loadGenres();
        loadDirectors();

        //wpisywanie wartosci do textfiledów
        moviesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
                editButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                clearFormFields();
                editButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });
        genreComboBox.setItems(genres);
        //Nie można tworzyć nowych gatunków
        genreComboBox.setConverter(new StringConverter<Genre>() {
            @Override
            public String toString(Genre genre) {
                return genre == null ? "" : genre.getName();
            }
            @Override
            public Genre fromString(String string) { return null; }
        });

        filterGenreComboBox.setItems(genres);
        filterGenreComboBox.setConverter(new StringConverter<Genre>() {
            @Override
            public String toString(Genre genre) {
                return genre == null ? "" : genre.getName();
            }
            @Override
            public Genre fromString(String string) { return null;}
        });
        directorComboBox.setItems(directors);
        //zabezpieczenie gdy nie było użytkowników
        if (currentUser == null) {
            loadMovies();
        }
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("passwordHash"));

        usersTable.setItems(users);

        loadUsers();
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteUserButton.setDisable(newSelection == null);
        });
    }

    private void loadUsers() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        //czyszczenie pól users
        try {
            usersTable.getSelectionModel().clearSelection();

            users.clear();
            users.addAll(session.createQuery("FROM User", User.class).list());
        } finally {
            session.close();
        }

    }
    private void updateFilterPredicate() {
        String searchText = searchField.getText() == null ?
                "" : searchField.getText().toLowerCase().trim();
        Genre selectedGenre = filterGenreComboBox.getValue();

        filteredMovies.setPredicate(movie -> {
            if (searchText.isEmpty() && selectedGenre == null) {
                return true; //wszystkie filmy są widoczne, jeśli brak filtra
            }

            boolean matchesSearchText = true;
            if (!searchText.isEmpty()) {
                matchesSearchText = movie.getTitle().toLowerCase().contains(searchText); //sprawdzenie tytułu
            }

            boolean matchesGenre = true;
            if (selectedGenre != null) {
                matchesGenre = movie.getGenre() != null && movie.getGenre().equals(selectedGenre);
            }

            return matchesSearchText && matchesGenre; //film musi spełniać oba kryteria
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (userLabel != null) {
            userLabel.setText("Zalogowano: " + (user != null ? user.getUsername() : "Brak"));
        }
        loadMovies(); //Ładuje filmy po ustawieniu użytkownika
    }

    private void loadMovies() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<Movie> movieList;
            //Pobieranie filmów z wypożyczeniami i użytikami
            movieList = session.createQuery(
                            "SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.rentals r LEFT JOIN FETCH r.user ORDER BY m.title", Movie.class)
                    .getResultList();
            moviesTable.getSelectionModel().clearSelection();

            movies.setAll(movieList);//aktualizacja listy filmów
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd ładowania filmów", "Nie udało się załadować listy filmów: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private void loadGenres() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<Genre> genreList = session.createQuery("SELECT g FROM Genre g ORDER BY g.name", Genre.class).getResultList();
            genres.setAll(genreList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd ładowania gatunków", "Nie udało się załadować listy gatunków: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private void loadDirectors() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<Director> directorList = session.createQuery("SELECT d FROM Director d ORDER BY d.name", Director.class).getResultList();
            directors.setAll(directorList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd ładowania reżyserów", "Nie udało się załadować listy reżyserów: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    private boolean validateYearInput(String yearText) {
        if (yearText == null || yearText.trim().isEmpty()) {
            showAlert("Błąd walidacji", "Pole 'Rok' nie może być puste.");
            return false;
        }

        int year;
        try {
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            showAlert("Błąd walidacji", "Pole 'Rok' musi być liczbą całkowitą.");
            return false;
        }

        if (year <= 0) {
            showAlert("Błąd walidacji", "Rok musi być liczbą dodatnią.");
            return false;
        }

        int currentYear = java.time.Year.now().getValue();
        if (year > currentYear) {
            showAlert("Błąd walidacji", "Rok filmu nie może być większy niż rok bieżący (" + currentYear + ").");
            return false;
        }

        return true;
    }

    @FXML
    private void addFilm() {
        //Pobiera danych z formularza
        String title = titleField.getText().trim();
        String yearStr = yearField.getText().trim();
        String ratingStr = ratingField.getText().trim().replace(',', '.');
        Genre selectedGenre = genreComboBox.getValue();
        String directorInput = directorComboBox.getEditor().getText().trim();
        Director selectedDirectorObject = directorComboBox.getValue();
        String notesText = notesField.getText().trim();

        if (title.isEmpty()) {
            showAlert("Błąd walidacji", "Pole 'Tytuł' musi być wypełnione.");
            return;
        }
        if (!validateYearInput(yearStr)) {
            return;
        }
        int year = Integer.parseInt(yearStr);
        if (ratingStr.isEmpty()) {
            showAlert("Błąd walidacji", "Pole 'Ocena' musi być wypełnione.");
            return;
        }
        float rating;
        try {
            rating = Float.parseFloat(ratingStr);
            if (rating < 0.0f || rating > 10.0f) {
                showAlert("Błąd walidacji", "Ocena musi być w zakresie od 0.0 do 10.0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Błąd formatu", "Pole 'Ocena' musi być liczbą (np. 7.5 lub 7,5).");
            return;
        }

        if (selectedGenre == null) {
            showAlert("Błąd walidacji", "Gatunek musi być wybrany z listy.");
            return;
        }

        if (selectedDirectorObject == null && directorInput.isEmpty()) {
            showAlert("Błąd walidacji", "Reżyser musi być wybrany z listy lub wpisany.");
            return;
        }

        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);
        movie.setRating(rating);
        movie.setNotes(notesText.isEmpty() ? null : notesText);
        movie.setAvailable(true);

        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
//sprawdzanie reżysera jeśli dodanie nowego
            Director directorEntity = null;
            if (!directorInput.isEmpty()) {
                Query<Director> query = session.createQuery("SELECT d FROM Director d WHERE d.name = :name", Director.class);
                query.setParameter("name", directorInput);
                List<Director> existingDirectors = query.getResultList();
                if (existingDirectors.isEmpty()) {
                    Director newDirector = new Director(directorInput);
                    session.persist(newDirector);
                    directorEntity = newDirector;
                    if (!directors.contains(newDirector)) {
                        directors.add(newDirector);
                    }
                } else {
                    directorEntity = existingDirectors.get(0);
                }
            } else if (selectedDirectorObject != null) {
                directorEntity = (Director) session.merge(selectedDirectorObject);
            }

            if (directorEntity == null) {
                showAlert("Błąd krytyczny", "Nie udało się przetworzyć informacji o reżyserze.");
                if (tx.isActive()) tx.rollback();
                return;
            }
            movie.setDirector(directorEntity);
            movie.setGenre((Genre) session.merge(selectedGenre));
            movie.setUser((User) session.merge(currentUser));

            session.persist(movie); //zapis filmu do bazy danych
            tx.commit();

            movies.add(movie);
            clearFormFields();
            showAlert("Sukces", "Film \"" + movie.getTitle() + "\" został pomyślnie dodany.");

            loadMovies();
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            ex.printStackTrace();
            showAlert("Błąd dodawania filmu", "Wystąpił błąd: " + ex.getMessage() + (ex.getCause() != null ? " Przyczyna: " + ex.getCause().getMessage() : ""));
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @FXML
    private void handleEdit() {
        Movie selectedMovie = moviesTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Błąd", "Wybierz film do edycji.");
            return;
        }
        if (currentUser == null || selectedMovie.getUser() == null || !selectedMovie.getUser().getId().equals(currentUser.getId())) {
            showAlert("Błąd", "Nie możesz edytować tego filmu, ponieważ nie jesteś jego właścicielem lub nie jesteś zalogowany.");
            return;
        }

        String title = titleField.getText().trim();
        String yearStr = yearField.getText().trim();
        String ratingStr = ratingField.getText().trim().replace(',', '.');
        Genre selectedGenreObject = genreComboBox.getValue();
        String directorInput = directorComboBox.getEditor().getText().trim();
        Director selectedDirectorObject = directorComboBox.getValue();
        String notesText = notesField.getText().trim();

        if (title.isEmpty()) {
            showAlert("Błąd walidacji", "Pole 'Tytuł' musi być wypełnione.");
            return;
        }
        if (!validateYearInput(yearStr)) {
            return;
        }
        int year = Integer.parseInt(yearStr);
        if (ratingStr.isEmpty()) {
            showAlert("Błąd walidacji", "Pole 'Ocena' musi być wypełnione.");
            return;
        }
        float rating;
        try {
            rating = Float.parseFloat(ratingStr);
            if (rating < 0.0f || rating > 10.0f) {
                showAlert("Błąd walidacji", "Ocena musi być w zakresie od 0.0 do 10.0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Błąd formatu", "Pole 'Ocena' musi być liczbą (np. 7.5 lub 7,5).");
            return;
        }

        if (selectedGenreObject == null) {
            showAlert("Błąd walidacji", "Gatunek musi być wybrany z listy.");
            return;
        }

        if (selectedDirectorObject == null && directorInput.isEmpty()) {
            showAlert("Błąd walidacji", "Reżyser musi być wybrany z listy lub wpisany.");
            return;
        }

        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Movie movieToUpdate = session.find(Movie.class, selectedMovie.getId());
            if (movieToUpdate == null) {
                showAlert("Błąd", "Nie znaleziono filmu do edycji. Mógł zostać usunięty.");
                if (tx.isActive()) tx.rollback();
                loadMovies();
                return;
            }
            if (movieToUpdate.getUser() == null || !movieToUpdate.getUser().getId().equals(currentUser.getId())) {
                showAlert("Błąd", "Nie możesz edytować tego filmu, ponieważ nie jesteś jego właścicielem (sprawdzenie po odświeżeniu).");
                if (tx.isActive()) tx.rollback();
                return;
            }

            movieToUpdate.setTitle(title);
            movieToUpdate.setYear(year);
            movieToUpdate.setRating(rating);
            movieToUpdate.setNotes(notesText.isEmpty() ? null : notesText);

            Director directorEntity = null;
            if (!directorInput.isEmpty()) {
                if (selectedDirectorObject != null && directorInput.equals(selectedDirectorObject.getName())) {
                    directorEntity = (Director) session.merge(selectedDirectorObject);
                } else {
                    Query<Director> query = session.createQuery("SELECT d FROM Director d WHERE d.name = :name", Director.class);
                    query.setParameter("name", directorInput);
                    Optional<Director> existingDirectorOpt = query.getResultList().stream().findFirst();

                    if (existingDirectorOpt.isPresent()) {
                        directorEntity = existingDirectorOpt.get();
                    } else {
                        Director newDirector = new Director(directorInput);
                        session.persist(newDirector);
                        directorEntity = newDirector;
                        if (!directors.contains(newDirector)) {
                            directors.add(newDirector);
                        }
                    }
                }
            } else if (selectedDirectorObject != null) {
                directorEntity = (Director) session.merge(selectedDirectorObject);
            }

            if (directorEntity == null) {
                showAlert("Błąd krytyczny", "Nie udało się przetworzyć informacji o reżyserze podczas edycji.");
                if (tx.isActive()) tx.rollback();
                return;
            }
            movieToUpdate.setDirector(directorEntity);
            movieToUpdate.setGenre((Genre) session.merge(selectedGenreObject));

            session.merge(movieToUpdate);
            tx.commit();
            int index = movies.indexOf(selectedMovie);
            if (index != -1) {
                movies.set(index, movieToUpdate);
            } else {
                loadMovies();
            }
            clearFormFields();
            showAlert("Sukces", "Film \"" + movieToUpdate.getTitle() + "\" został pomyślnie zaktualizowany.");

            loadMovies();
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            ex.printStackTrace();
            showAlert("Błąd edycji filmu", "Wystąpił błąd: " + ex.getMessage() + (ex.getCause() != null ? " Przyczyna: " + ex.getCause().getMessage() : ""));
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @FXML
    private void handleDelete() {
        Movie selectedMovie = moviesTable.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showAlert("Błąd", "Wybierz film do usunięcia.");
            return;
        }
        if (currentUser == null || selectedMovie.getUser() == null || !selectedMovie.getUser().getId().equals(currentUser.getId())) {
            showAlert("Błąd", "Nie możesz usunąć tego filmu, ponieważ nie jesteś jego właścicielem lub nie jesteś zalogowany.");
            return;
        }

        Alert confirmationAlert = new Alert(AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć film \"" + selectedMovie.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        confirmationAlert.setHeaderText("Potwierdzenie usunięcia");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.NO) {
            return;
        }

        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Movie movieToRemove = session.find(Movie.class, selectedMovie.getId());

            if (movieToRemove != null) {
                if (movieToRemove.getUser() == null || !movieToRemove.getUser().getId().equals(currentUser.getId())) {
                    showAlert("Błąd", "Nie możesz usunąć tego filmu (sprawdzenie po odświeżeniu).");
                    if(tx.isActive()) tx.rollback();
                    return;
                }
                session.remove(movieToRemove);
                tx.commit();

                movies.remove(selectedMovie);
                clearFormFields();
                showAlert("Sukces", "Film \"" + selectedMovie.getTitle() + "\" został usunięty.");

                loadMovies();
            } else {
                showAlert("Błąd", "Nie znaleziono filmu do usunięcia. Mógł już zostać usunięty.");
                if(tx.isActive()) tx.rollback();
                movies.remove(selectedMovie);
                loadMovies();
            }
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            ex.printStackTrace();
            showAlert("Błąd usuwania filmu", "Wystąpił błąd: " + ex.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
//wypełnieie formularza danymi Movie
    private void populateForm(Movie movie) {
        if (movie == null) {
            clearFormFields();
            return;
        }
        titleField.setText(movie.getTitle());
        yearField.setText(movie.getYear() != null ? movie.getYear().toString() : "");
        ratingField.setText(movie.getRating() != null ? String.format(Locale.US, "%.1f", movie.getRating()) : "");
        notesField.setText(movie.getNotes() != null ? movie.getNotes() : "");

        genreComboBox.setValue(movie.getGenre());
        directorComboBox.setValue(movie.getDirector());
        if (movie.getDirector() != null) {
            directorComboBox.getEditor().setText(movie.getDirector().getName());
        } else {
            directorComboBox.getEditor().clear();
        }
    }
//czyści pola formularza i wyłącza przyciski edit oraz delete
    private void clearFormFields() {
        titleField.clear();
        yearField.clear();
        ratingField.clear();
        notesField.clear();
        genreComboBox.getSelectionModel().clearSelection();
        genreComboBox.setValue(null);
        directorComboBox.getSelectionModel().clearSelection();
        directorComboBox.setValue(null);
        directorComboBox.getEditor().clear();
        moviesTable.getSelectionModel().clearSelection();
        editButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    //ustawia currentUser na null
    @FXML
    private void handleLogout() {
        this.currentUser = null;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", java.util.Locale.forLanguageTag("pl"));
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/projekt/login-view.fxml"), bundle); //ładuje ponownie login-view
            javafx.scene.Parent loginRoot = loader.load();
//scena na stage
            javafx.stage.Stage stage = (javafx.stage.Stage) logoutButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(loginRoot);
            java.net.URL cssUrl = getClass().getResource("/com/example/projekt/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            if (bundle.containsKey("app.title")) {
                stage.setTitle(bundle.getString("app.title"));
            } else {
                stage.setTitle("Logowanie");
            }
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Błąd wylogowania", "Nie udało się wrócić do ekranu logowania: " + ex.getMessage());
        }
    }
//czyści pola i filtry
    @FXML
    private void handleReset() {
        clearFormFields();
        searchField.clear();
        filterGenreComboBox.getSelectionModel().clearSelection();
        filterGenreComboBox.setValue(null);
    }

    @FXML
    private void handleAddUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Dodaj nowego użytkownika");
        dialog.setHeaderText("Wprowadź dane nowego użytkownika");

        ButtonType addButtonType = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nazwa użytkownika");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Hasło");

        grid.add(new Label("Nazwa użytkownika:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Hasło:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    showAlert("Błąd walidacji", "Nazwa użytkownika i hasło nie mogą być puste.");
                    return null;
                }
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setPassword(password);
                return newUser;
            }
            return null;
        });
        Optional<User> result = dialog.showAndWait();

        result.ifPresent(newUser -> {
            Session session = HibernateUtil.getSessionFactory().openSession();
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();

                Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
                query.setParameter("username", newUser.getUsername());
                if (!query.getResultList().isEmpty()) {
                    showAlert("Błąd dodawania", "Użytkownik o nazwie '" + newUser.getUsername() + "' już istnieje.");
                    if (transaction != null) transaction.rollback();
                    return;
                }

                session.persist(newUser);//odświeżenie
                transaction.commit();
                users.add(newUser);
                showAlert("Sukces", "Użytkownik '" + newUser.getUsername() + "' został pomyślnie dodany.");
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                e.printStackTrace();
                showAlert("Błąd dodawania użytkownika", "Wystąpił błąd podczas dodawania użytkownika: " + e.getMessage());
            } finally {
                session.close();
            }
        });
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Błąd", "Wybierz użytkownika do usunięcia.");
            return;
        }

        if (currentUser != null && selectedUser.getId().equals(currentUser.getId())) {
            showAlert("Błąd", "Nie możesz usunąć aktualnie zalogowanego użytkownika.");
            return;
        }

        if ("admin".equalsIgnoreCase(selectedUser.getUsername())) {
            showAlert("Błąd", "Nie można usunąć konta administratora.");
            return;
        }

        Alert confirmationAlert = new Alert(AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć użytkownika '" + selectedUser.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
        confirmationAlert.setHeaderText("Potwierdzenie usunięcia użytkownika");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            Session session = null;
            Transaction transaction = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                transaction = session.beginTransaction();

                User userToDelete = session.find(User.class, selectedUser.getId());
                if (userToDelete != null) {
                    Query<Rental> activeRentalsQuery = session.createQuery(
                            "SELECT r FROM Rental r WHERE r.user = :user AND r.returnDate IS NULL", Rental.class);
                    activeRentalsQuery.setParameter("user", userToDelete);
                    List<Rental> activeRentals = activeRentalsQuery.getResultList();

                    for (Rental rental : activeRentals) {
                        Movie rentedMovie = rental.getMovie();
                        if (rentedMovie != null) {
                            rentedMovie.setAvailable(true);
                            session.merge(rentedMovie);
                            System.out.println("Film \"" + rentedMovie.getTitle() + "\" zmieniono na dostępny.");
                        }
                    }

                    session.remove(userToDelete);
                    transaction.commit();

                    showAlert("Sukces", "Użytkownik '" + selectedUser.getUsername() + "' został pomyślnie usunięty.");

                    loadUsers();
                    loadMovies();
                } else {
                    showAlert("Błąd", "Nie znaleziono użytkownika do usunięcia. Mógł już zostać usunięty.");
                    if (transaction != null) transaction.rollback();
                }
            } catch (Exception e) {
                if (transaction != null) {
                    transaction.rollback();
                }
                e.printStackTrace();
                showAlert("Błąd usuwania użytkownika", "Wystąpił błąd podczas usuwania użytkownika: " + e.getMessage());
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
        }
    }
//wyświetla okno dialogowe Alert ERROR i INFORMATION
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        if (title.equalsIgnoreCase("Sukces")) {
            alert.setAlertType(AlertType.INFORMATION);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}