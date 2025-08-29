package com.javarush.apalinskiy.web.listener;


import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repositories.InMemoryUserRepository;
import com.javarush.apalinskiy.service.DefaultUserService;
import com.javarush.apalinskiy.service.UserService;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppBootstrapTest {

    @Mock
    private ServletContext context;
    @Mock
    private ServletContextEvent event;

    @Test
    void contextInitializedSetsUserServiceAttributeAndAdminCanLoginTest() {
        // given
        when(event.getServletContext()).thenReturn(context);
        // when
        new AppBootstrap().contextInitialized(event);
        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(1)).setAttribute(eq("userService"), captor.capture());
        Object stored = captor.getValue();
        assertNotNull(stored, "userService attribute must be set");
        assertInstanceOf(UserService.class, stored, "attribute must be a UserService");
        UserService svc = (UserService) stored;
        assertTrue(svc.login("admin", "admin").isPresent(), "admin/admin should authenticate");
    }

    @Test
    void contextInitializedTwicePutsNewServiceEachTimeBothUsableTest() {
        // given
        when(event.getServletContext()).thenReturn(context);
        List<UserService> services = new ArrayList<>(2);
        doAnswer(inv -> { services.add((UserService) inv.getArgument(1)); return null; })
                .when(context).setAttribute(eq("userService"), any());
        AppBootstrap bootstrap = new AppBootstrap();
        // when
        bootstrap.contextInitialized(event);
        bootstrap.contextInitialized(event);
        // then
        assertEquals(2, services.size(), "attribute should be set twice on two inits");
        assertNotSame(services.get(0), services.get(1), "each init provides a new service instance");
        assertTrue(services.get(0).login("admin", "admin").isPresent());
        assertTrue(services.get(1).login("admin", "admin").isPresent());
    }

    @Test
    void contextInitializedAttributeKeyIsExactlyUserServiceTest() {
        // given
        when(event.getServletContext()).thenReturn(context);
        // when
        new AppBootstrap().contextInitialized(event);
        // then
        verify(context, atLeastOnce()).setAttribute(eq("userService"), any());
    }

    @Test
    void contextInitializedAdminUserHasAdminRoleAndNormalizedFieldsTest() {
        // given
        when(event.getServletContext()).thenReturn(context);
        // when
        new AppBootstrap().contextInitialized(event);
        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(context).setAttribute(eq("userService"), captor.capture());
        UserService svc = (UserService) captor.getValue();
        User admin = svc.login("admin", "admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals("Admin", admin.getUserName());
        assertEquals("admin", admin.getUserLogin());
    }

    @Test
    void duplicateLoginIsSwallowedAndAttributeIsSetTest() {
        // given
        ServletContext ctx = mock(ServletContext.class);
        ServletContextEvent ev = mock(ServletContextEvent.class);
        when(ev.getServletContext()).thenReturn(ctx);
        try (MockedConstruction<InMemoryUserRepository> repoCons =
                     mockConstruction(InMemoryUserRepository.class);
             MockedConstruction<DefaultUserService> svcCons =
                     mockConstruction(DefaultUserService.class, (mock, c) -> {
                         doThrow(new DuplicateLoginException("dup"))
                                 .when(mock).register(Role.ADMIN, "Admin", "admin", "admin");
                     })) {
            // when
            assertDoesNotThrow(() -> new AppBootstrap().contextInitialized(ev));
            // then
            DefaultUserService service = svcCons.constructed().getFirst();
            verify(service, times(1)).register(Role.ADMIN, "Admin", "admin", "admin");
            verify(ctx, times(1)).setAttribute(eq("userService"), same(service));
            verifyNoMoreInteractions(ctx);
        }
    }

    @Test
    void otherRuntimeIsPropagatedAndContextNotTouchedTest() {
        // given
        ServletContext ctx = mock(ServletContext.class);
        ServletContextEvent ev = mock(ServletContextEvent.class);
        try (MockedConstruction<InMemoryUserRepository> repoCons =
                     mockConstruction(InMemoryUserRepository.class);
             MockedConstruction<DefaultUserService> svcCons =
                     mockConstruction(DefaultUserService.class, (mock, c) -> {
                         doThrow(new IllegalStateException("boom"))
                                 .when(mock).register(Role.ADMIN, "Admin", "admin", "admin");
                     })) {
            // when / then
            assertThrows(IllegalStateException.class,
                    () -> new AppBootstrap().contextInitialized(ev));
            // then
            verifyNoInteractions(ctx);
        }
    }
}