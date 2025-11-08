package com.javarush.apalinskiy.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HibernateUtil (unit)")
@ExtendWith(MockitoExtension.class)
class HibernateUtilTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        var f = HibernateUtil.class.getDeclaredField("sessionFactory");
        f.setAccessible(true);
        f.set(null, null);
    }

    @AfterEach
    void cleanup() throws Exception {
        var f = HibernateUtil.class.getDeclaredField("sessionFactory");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    @DisplayName("Given valid config — When getSessionFactory twice — Then builds once and reuses instance")
    void buildsOnceAndReuses() {
        // Given
        try (MockedConstruction<StandardServiceRegistryBuilder> ignored1 =
                     mockConstruction(StandardServiceRegistryBuilder.class, (builder, ctx) -> {
                         when(builder.configure()).thenReturn(builder);
                         StandardServiceRegistry registry = mock(StandardServiceRegistry.class);
                         when(builder.build()).thenReturn(registry);
                     });
             MockedConstruction<MetadataSources> ignored =
                     mockConstruction(MetadataSources.class, (sources, ctx) -> {
                         MetadataBuilder metadataBuilder = mock(MetadataBuilder.class);
                         when(sources.getMetadataBuilder()).thenReturn(metadataBuilder);
                         Metadata metadata = mock(Metadata.class);
                         when(metadataBuilder.build()).thenReturn(metadata);
                         SessionFactoryBuilder sfb = mock(SessionFactoryBuilder.class);
                         when(metadata.getSessionFactoryBuilder()).thenReturn(sfb);
                         SessionFactory sf = mock(SessionFactory.class);
                         when(sfb.build()).thenReturn(sf);
                     })) {
            // When
            SessionFactory sf1 = HibernateUtil.getSessionFactory();
            SessionFactory sf2 = HibernateUtil.getSessionFactory();
            // Then
            assertNotNull(sf1);
            assertSame(sf1, sf2, "must reuse same singleton instance");
        }
    }

    @Test
    @DisplayName("Given builder throws — When getSessionFactory — Then wraps into ExceptionInInitializerError")
    void buildFailsThrowsInitializerError() {
        // Given
        try (MockedConstruction<StandardServiceRegistryBuilder> ignored =
                     mockConstruction(StandardServiceRegistryBuilder.class, (builder, ctx) ->
                             when(builder.configure()).thenThrow(new RuntimeException("boom")))) {
            // When / Then
            ExceptionInInitializerError err =
                    assertThrows(ExceptionInInitializerError.class, HibernateUtil::getSessionFactory);
            assertTrue(err.getCause().getMessage().contains("boom"));
        }
    }

    @Test
    @DisplayName("Given sessionFactory initialized — When shutdown() — Then closes factory")
    void shutdownClosesFactory() throws Exception {
        // Given
        SessionFactory sf = mock(SessionFactory.class);
        var f = HibernateUtil.class.getDeclaredField("sessionFactory");
        f.setAccessible(true);
        f.set(null, sf);
        // When
        HibernateUtil.shutdown();
        // Then
        verify(sf).close();
    }

    @Test
    @DisplayName("Given no sessionFactory — When shutdown() — Then no exception")
    void shutdownNoFactoryDoesNothing() {
        // Given
        // When / Then
        assertDoesNotThrow(HibernateUtil::shutdown);
    }
}