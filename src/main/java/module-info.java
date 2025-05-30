module com.example.projekt {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires java.naming; // To może być potrzebne dla niektórych funkcji JNDI, ale niekoniecznie dla podstawowego JPA

    opens com.example.projekt to javafx.fxml, org.hibernate.orm.core;
    // Dodaj otwarcie pakietu com.example.projekt.util, jeśli encje są w tym samym pakiecie lub Hibernate tego wymaga
    // Otwierasz com.example.projekt, co powinno objąć encje, jeśli są bezpośrednio w tym pakiecie
    exports com.example.projekt;
}