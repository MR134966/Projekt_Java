module com.example.projekt {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires java.naming;

    opens com.example.projekt to javafx.fxml, org.hibernate.orm.core;

    exports com.example.projekt;
}