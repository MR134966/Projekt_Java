package com.example.projekt;

import jakarta.persistence.*; // Upewnij się, że importujesz właściwe pakiety JPA
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;

@Entity // <--- TO JEST KLUCZOWE! MUSI BYĆ TA ADNOTACJA!
@Table(name = "rentals") // Opcjonalnie, jeśli nazwa tabeli jest inna niż 'rentals'
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Jeśli w bazie masz 'serial', Long jest zazwyczaj dobrym typem w Javie

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "rental_date")
    private LocalDate rentalDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "return_date")
    private LocalDate returnDate; // Może być NULL

    // Konstruktory (jeśli masz inne, zostaw je)
    public Rental() {}

    // Gettery i Settery (upewnij się, że masz wszystkie, w tym dla movie i user)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDate rentalDate) { this.rentalDate = rentalDate; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public ObjectProperty<LocalDate> expectedReturnDateProperty() {
        return new SimpleObjectProperty<>(this.expectedReturnDate);
    }


    @Override
    public String toString() {
        return "Rental{" +
                "id=" + id +
                ", rentalDate=" + rentalDate +
                '}';
    }
}