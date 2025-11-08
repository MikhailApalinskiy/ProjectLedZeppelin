package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUserService")
class DefaultUserServiceTest {

    @Mock
    SessionFactory sessionFactory;
    @Mock
    Session session;
    @Mock
    Transaction tx;
    @Mock
    UserRepository users;

    private AutoCloseable staticMock;
    private DefaultUserService sut;

    @BeforeEach
    void init() {
        staticMock = Mockito.mockStatic(HibernateUtil.class);
        lenient().when(HibernateUtil.getSessionFactory()).thenReturn(sessionFactory);
        lenient().when(sessionFactory.getCurrentSession()).thenReturn(session);
        lenient().when(session.getTransaction()).thenReturn(tx);
        sut = new DefaultUserService(users);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (staticMock != null) {
            staticMock.close();
        }
    }

    @Nested
    @DisplayName("findPage(page,size)")
    class FindPage {

        @Test
        @DisplayName("starts local read-only tx, queries total+items, commits and restores RO")
        void startsReadonlyTx_commits() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(users.countAll()).thenReturn(3L);
            var u1 = user("1", "alice", "Alice");
            var u2 = user("2", "bob", "Bob");
            when(users.findPage(1, 2)).thenReturn(List.of(u1, u2));
            // When
            DefaultUserService.PagedResult<User> page = sut.findPage(1, 2);
            // Then
            verify(session).beginTransaction();
            InOrder io = inOrder(session, users, tx);
            io.verify(session).setDefaultReadOnly(true);
            io.verify(users).countAll();
            io.verify(users).findPage(1, 2);
            io.verify(tx).commit();
            io.verify(session).setDefaultReadOnly(false);
            assertEquals(3, page.total());
            assertEquals(List.of(u1, u2), page.items());
            assertEquals(1, page.page());
            assertEquals(2, page.size());
        }

        @Test
        @DisplayName("propagates runtime errors and rolls back when started here")
        void error_rollsBack() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            when(users.countAll()).thenThrow(new RuntimeException("boom"));
            // When / Then
            assertThrows(RuntimeException.class, () -> sut.findPage(1, 10));
            verify(tx).rollback();
            verify(session).setDefaultReadOnly(false);
        }
    }

    @Nested
    @DisplayName("register(role,name,login,password)")
    class Register {

        @Test
        @DisplayName("persists user on first try and commits")
        void firstTry_ok() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            // When
            User created = sut.register(Role.USER, "Alice", "alice", "pwd123");
            // Then
            verify(users).save(argThat(u -> "alice".equals(u.getUserLogin())));
            verify(tx).commit();
            assertNotNull(created.getUserId());
            assertEquals("alice", created.getUserLogin());
            assertEquals("Alice", created.getUserName());
            assertEquals(Role.USER, created.getRole());
        }

        @Test
        @DisplayName("duplicate login -> rolls back and rethrows DuplicateLoginException")
        void duplicateLogin_throws() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            doThrow(new DuplicateLoginException("dup"))
                    .when(users).save(any(User.class));
            // When / Then
            assertThrows(DuplicateLoginException.class,
                    () -> sut.register(Role.USER, "Alice", "alice", "pwd123"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("duplicate id first 3 attempts -> IllegalStateException after retries")
        void duplicateId_retriesAndFails() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            doThrow(new DuplicateIdException("dup id")).when(users).save(any(User.class));
            // When / Then
            assertThrows(IllegalStateException.class,
                    () -> sut.register(Role.USER, "Bob", "bob", "xxyyzz"));
            verify(tx, times(3)).rollback();
        }
    }

    @Nested
    @DisplayName("login(login,password)")
    class Login {

        @Test
        @DisplayName("success -> returns user, commits, restores RO")
        void success() {
            // Given
            var u = user("id-1", "admin", "test");
            u.setPassword("secret");
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(users.findByLogin("admin")).thenReturn(Optional.of(u));
            // When
            var res = sut.login("admin", "secret");
            // Then
            assertTrue(res.isPresent());
            verify(session).setDefaultReadOnly(true);
            verify(tx).commit();
            verify(session).setDefaultReadOnly(false);
        }

        @Test
        @DisplayName("wrong password -> empty, commits, restores RO")
        void wrongPassword() {
            // Given
            var u = user("id-2", "alice", "Alice");
            u.setPassword("p1");
            when(session.beginTransaction()).thenReturn(tx);
            when(session.isDefaultReadOnly()).thenReturn(true);
            when(users.findByLogin("alice")).thenReturn(Optional.of(u));
            // When
            var res = sut.login("alice", "p2");
            // Then
            assertTrue(res.isEmpty());
            verify(tx).commit();
            verify(session, times(2)).setDefaultReadOnly(true);
        }
    }

    @Nested
    @DisplayName("findByLogin(login)")
    class FindByLogin {

        @Test
        @DisplayName("starts local read-only tx when inactive, commits and restores RO")
        void localTxReadonly() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u-1", "john", "John");
            when(users.findByLogin("john")).thenReturn(Optional.of(u));
            // When
            var res = sut.findByLogin("john");
            // Then
            assertTrue(res.isPresent());
            verify(session).beginTransaction();
            verify(session).setDefaultReadOnly(true);
            verify(tx).commit();
            verify(session).setDefaultReadOnly(false);
        }
    }

    @Nested
    @DisplayName("findById(userId)")
    class FindById {

        @Test
        @DisplayName("propagates read-only tx logic similar to findByLogin")
        void localTxReadonly() {
            // Given
            when(tx.isActive()).thenReturn(false);
            when(session.isDefaultReadOnly()).thenReturn(false);
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u-42", "kate", "Kate");
            when(users.findById("u-42")).thenReturn(Optional.of(u));
            // When
            var res = sut.findById("u-42");
            // Then
            assertTrue(res.isPresent());
            verify(session).beginTransaction();
            verify(session).setDefaultReadOnly(true);
            verify(tx).commit();
            verify(session).setDefaultReadOnly(false);
        }
    }

    @Nested
    @DisplayName("updateProfile(userId, newDisplayName)")
    class UpdateProfile {

        @Test
        @DisplayName("blank name -> throws IAE and does not start tx")
        void blankName() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.updateProfile("u", "   "));
            verify(session, never()).beginTransaction();
            verify(users, never()).findById(anyString());
        }

        @Test
        @DisplayName("user not found -> throws NoSuchElementException and rolls back")
        void userMissing_throws() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            when(users.findById("ghost")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.updateProfile("ghost", "New Name"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("same display name -> no-op but commits")
        void sameName_noop() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u-1", "kate", "Kate");
            when(users.findById("u-1")).thenReturn(Optional.of(u));
            // When
            var out = sut.updateProfile("u-1", "Kate");
            // Then
            assertEquals("Kate", out.getUserName());
            verify(users, never()).update(any());
            verify(tx).commit();
        }

        @Test
        @DisplayName("changes name and commits")
        void changesName() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u-2", "david", "David");
            when(users.findById("u-2")).thenReturn(Optional.of(u));
            // When
            var out = sut.updateProfile("u-2", "Dave");
            // Then
            assertEquals("Dave", out.getUserName());
            verify(users).update(out);
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("changePassword(userId, current, new)")
    class ChangePassword {

        @Test
        @DisplayName("invalid new password -> throws IAE, no tx")
        void invalidNew_throws() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.changePassword("u", "old", "123"));
            verify(session, never()).beginTransaction();
        }

        @Test
        @DisplayName("user not found -> throws NoSuchElementException and rolls back")
        void userMissing() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            when(users.findById("ghost")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.changePassword("ghost", "x", "newpass"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("wrong current -> throws SecurityException and rolls back")
        void wrongCurrent() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u", "usr", "User");
            u.setPassword("old");
            when(users.findById("u")).thenReturn(Optional.of(u));
            // When / Then
            assertThrows(SecurityException.class,
                    () -> sut.changePassword("u", "BAD", "newpass"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("same as current -> throws IAE and rolls back")
        void sameAsCurrent() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u","usr","User");
            u.setPassword("samepass");         // длина >= 6
            when(users.findById("u")).thenReturn(Optional.of(u));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.changePassword("u", "samepass", "samepass"));
            verify(tx).rollback(); // теперь вызовется
        }

        @Test
        @DisplayName("changes password and commits")
        void ok() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u", "usr", "User");
            u.setPassword("old");
            when(users.findById("u")).thenReturn(Optional.of(u));
            // When
            sut.changePassword("u", "old", "newpass");
            // Then
            assertEquals("newpass", u.getPassword());
            verify(users).update(u);
            verify(tx).commit();
        }
    }

    @Nested
    @DisplayName("adminUpdate(userId, role, name, login, newPassword)")
    class AdminUpdate {

        @Test
        @DisplayName("blank name/login -> throws IAE and no tx")
        void blank_throws() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("u", Role.ADMIN, "  ", "  ", null));
            verify(session, never()).beginTransaction();
        }

        @Test
        @DisplayName("user not found -> throws NoSuchElementException and rolls back")
        void userMissing() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            when(users.findById("ghost")).thenReturn(Optional.empty());
            // When / Then
            assertThrows(NoSuchElementException.class,
                    () -> sut.adminUpdate("ghost", Role.ADMIN, "X", "x", null));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("too short new password -> IAE and rolls back")
        void shortPwd_throws() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u1", "john", "John");
            u.setPassword("oldpass");
            when(users.findById("u1")).thenReturn(Optional.of(u));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("u1", Role.ADMIN, "John2", "john2", "123"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("same new password -> IAE and rolls back")
        void samePwd_throws() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u1", "john", "John");
            u.setPassword("oldpass");
            when(users.findById("u1")).thenReturn(Optional.of(u));
            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> sut.adminUpdate("u1", Role.ADMIN, "John2", "john2", "oldpass"));
            verify(tx).rollback();
        }

        @Test
        @DisplayName("updates fields and commits")
        void ok() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u1", "john", "John");
            u.setPassword("oldpass");
            when(users.findById("u1")).thenReturn(Optional.of(u));
            // When
            User out = sut.adminUpdate("u1", Role.ADMIN, "Johnny", "johnny", "newpass");
            // Then
            assertEquals(Role.ADMIN, out.getRole());
            assertEquals("Johnny", out.getUserName());
            assertEquals("johnny", out.getUserLogin());
            assertEquals("newpass", out.getPassword());
            verify(users).update(out);
            verify(tx).commit();
        }

        @Test
        @DisplayName("duplicate login during update -> propagates and rolls back")
        void duplicateLogin_propagates() {
            // Given
            when(session.beginTransaction()).thenReturn(tx);
            var u = user("u1", "john", "John");
            when(users.findById("u1")).thenReturn(Optional.of(u));
            doThrow(new DuplicateLoginException("dup"))
                    .when(users).update(any(User.class));
            // When / Then
            assertThrows(DuplicateLoginException.class,
                    () -> sut.adminUpdate("u1", null, "John", "admin", null));
            verify(tx).rollback();
        }
    }

    private static User user(String id, String login, String name) {
        User u = new User();
        u.setUserId(id != null ? id : UUID.randomUUID().toString());
        u.setUserLogin(login);
        u.setUserName(name);
        u.setRole(Role.USER);
        u.setPassword("test");
        return u;
    }
}