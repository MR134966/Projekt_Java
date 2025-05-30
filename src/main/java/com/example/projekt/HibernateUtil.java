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

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Tworzenie rejestru z pliku konfiguracyjnego
                registry = new StandardServiceRegistryBuilder()
                        .configure() // załaduj hibernate.cfg.xml
                        .build();

                // Tworzenie źródeł metadanych
                MetadataSources sources = new MetadataSources(registry);
                
                // Dodawanie klas encji
                sources.addAnnotatedClass(Movie.class);
                sources.addAnnotatedClass(User.class);
                sources.addAnnotatedClass(Genre.class);
                sources.addAnnotatedClass(Director.class);

                // Budowanie metadanych
                Metadata metadata = sources.getMetadataBuilder().build();
                
                // Tworzenie fabryki sesji
                sessionFactory = metadata.getSessionFactoryBuilder().build();

            } catch (Exception e) {
                logger.error("Błąd podczas tworzenia SessionFactory: " + e.getMessage(), e);
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
                throw new RuntimeException("Błąd konfiguracji Hibernate: " + e.getMessage(), e);
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}