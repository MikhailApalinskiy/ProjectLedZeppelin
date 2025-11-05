package com.javarush.apalinskiy.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Utility class for initializing and providing access to the Hibernate {@link SessionFactory}.
 * <p>
 * Implements the double-checked locking pattern for lazy, thread-safe initialization.
 * The configuration is loaded from the default {@code hibernate.cfg.xml} file.
 * <p>
 * Typical usage:
 * <pre>{@code
 * SessionFactory sf = HibernateUtil.getSessionFactory();
 * try (Session session = sf.openSession()) {
 *     // work with Hibernate session
 * }
 * }</pre>
 * Always call {@link #shutdown()} on application shutdown to release resources.
 */
public final class HibernateUtil {

    /**
     * Lazily initialized singleton {@link SessionFactory}.
     */
    private static volatile SessionFactory sessionFactory;

    /**
     * Private constructor to prevent instantiation.
     */
    private HibernateUtil() {
    }

    /**
     * Returns the singleton {@link SessionFactory}, creating it if necessary.
     * <p>
     * Thread-safe and lazily initialized using double-checked locking.
     *
     * @return initialized Hibernate {@link SessionFactory}
     * @throws ExceptionInInitializerError if factory initialization fails
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                    sessionFactory = buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    /**
     * Builds a new {@link SessionFactory} from the Hibernate configuration.
     *
     * @return newly built {@link SessionFactory}
     * @throws ExceptionInInitializerError if initialization fails
     */
    private static SessionFactory buildSessionFactory() {
        StandardServiceRegistry registry;
        try {
            registry = new StandardServiceRegistryBuilder()
                    .configure()
                    .build();
            MetadataSources sources = new MetadataSources(registry);
            Metadata metadata = sources.getMetadataBuilder().build();
            return metadata.getSessionFactoryBuilder().build();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Closes the {@link SessionFactory} if it is initialized.
     * <p>
     * Should be invoked on application shutdown to properly release
     * all cached resources and database connections.
     */
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
