package com.example.projekt;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                registry = new StandardServiceRegistryBuilder()
                        .configure()
                        .build();

                MetadataSources sources = new MetadataSources(registry);

                sources.addAnnotatedClass(Movie.class);
                sources.addAnnotatedClass(User.class);
                sources.addAnnotatedClass(Genre.class);
                sources.addAnnotatedClass(Director.class);
                sources.addAnnotatedClass(Rental.class);

                Metadata metadata = sources.getMetadataBuilder().build();

                sessionFactory = metadata.getSessionFactoryBuilder().build();

                logger.info("Hibernate SessionFactory została pomyślnie zainicjalizowana.");

            } catch (Exception e) {
                logger.error("Błąd podczas tworzenia SessionFactory: " + e.getMessage(), e);
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                    logger.warn("Rejestr usług Hibernate został zniszczony z powodu błędu.");
                }
                throw new RuntimeException("Błąd konfiguracji Hibernate: " + e.getMessage(), e);
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            sessionFactory = null;
            logger.info("Hibernate SessionFactory została zamknięta i zasoby zwolnione.");
        }
    }
}