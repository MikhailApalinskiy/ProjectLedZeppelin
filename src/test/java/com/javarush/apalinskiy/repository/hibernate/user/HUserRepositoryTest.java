package com.javarush.apalinskiy.repository.hibernate.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.utils.HibernateUtil;
import it.AbstractDbTest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("HUserRepository (IT) — seeded by Liquibase testdata")
class HUserRepositoryIT extends AbstractDbTest {

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";

    private SessionFactory sf;
    private Transaction tx;
    private HUserRepository sut;
    private MockedStatic<HibernateUtil> mockedHibernateUtil;

    @BeforeEach
    void initUserRepo() {
        this.sf = super.sessionFactory;
        Session s = sf.getCurrentSession();
        tx = s.beginTransaction();
        mockedHibernateUtil = mockStatic(HibernateUtil.class);
        mockedHibernateUtil.when(HibernateUtil::getSessionFactory).thenReturn(sf);
        sut = new HUserRepository();
    }

    @AfterEach
    void cleanupUserRepo() {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        if (mockedHibernateUtil != null) {
            mockedHibernateUtil.close();
        }
    }

    private Session cur() { return sf.getCurrentSession(); }

    private User persistUser(String id, String login, String name) {
        User u = new User();
        u.setUserId(id);
        u.setUserLogin(login);
        u.setUserName(name);
        u.setRole(Role.USER);
        u.setPassword("test");
        Instant now = Instant.now();
        u.setCreatedAt(now);
        if (u.getFriends() == null)  {
            u.setFriends(new HashSet<>());
        }
        if (u.getFriendOf() == null) {
            u.setFriendOf(new HashSet<>());
        }
        cur().persist(u);
        return u;
    }

    @Nested
    @DisplayName("findByLogin(login)")
    class FindByLogin {

        @Test
        @DisplayName("trims and lowercases — finds seeded 'admin' by '  ADMIN  '")
        void findsCaseInsensitive() {
            // when
            Optional<User> u = sut.findByLogin("  ADMIN  ");
            // then
            assertTrue(u.isPresent());
            assertEquals("admin", u.get().getUserLogin().toLowerCase(Locale.ROOT));
            assertEquals(ADMIN_ID, u.get().getUserId());
        }

        @Test
        @DisplayName("returns empty for unknown login")
        void emptyWhenMissing() {
            assertTrue(sut.findByLogin("no_such_login").isEmpty());
        }
    }

    @Nested
    @DisplayName("findById(id)")
    class FindById {

        @Test
        @DisplayName("returns seeded Admin by id")
        void returnsAdmin() {
            Optional<User> u = sut.findById(ADMIN_ID);
            assertTrue(u.isPresent());
            assertEquals("admin", u.get().getUserLogin().toLowerCase(Locale.ROOT));
        }

        @Test
        @DisplayName("returns empty for unknown id")
        void returnsEmpty() {
            assertTrue(sut.findById("00000000-0000-0000-0000-000000000000").isEmpty());
        }
    }

    @Nested
    @DisplayName("save(user)")
    class SaveUser {

        @Test
        @DisplayName("persists a brand new user")
        void savesNew() {
            // given
            User bob = new User();
            bob.setUserId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            bob.setUserLogin("bob");
            bob.setUserName("Bob");
            bob.setRole(Role.USER);
            bob.setPassword("test");
            Instant now = Instant.now();
            bob.setCreatedAt(now);
            if (bob.getFriends() == null)  bob.setFriends(new java.util.HashSet<>());
            if (bob.getFriendOf() == null) bob.setFriendOf(new java.util.HashSet<>());
            // when
            assertDoesNotThrow(() -> sut.save(bob));
            // then
            assertTrue(sut.findById("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb").isPresent());
        }

        @Test
        @DisplayName("throws DuplicateLoginException when login already exists")
        void duplicateLogin() {
            // given
            User dup = new User();
            dup.setUserId("cccccccc-cccc-cccc-cccc-cccccccccccc");
            dup.setUserLogin("alice");
            dup.setUserName("Clone");
            dup.setRole(Role.USER);
            dup.setPassword("test");
            Instant now = Instant.now();
            dup.setCreatedAt(now);
            // when / then
            assertThrows(DuplicateLoginException.class, () -> sut.save(dup));
        }

        @Test
        @DisplayName("throws DuplicateIdException when id already exists")
        void duplicateId() {
            // given
            User dup = new User();
            dup.setUserId(ADMIN_ID);
            dup.setUserLogin("newlogin");
            dup.setUserName("Someone");
            dup.setRole(Role.USER);
            dup.setPassword("test");
            Instant now = Instant.now();
            dup.setCreatedAt(now);
            // when / then
            assertThrows(DuplicateIdException.class, () -> sut.save(dup));
        }
    }

    @Nested
    @DisplayName("update(user)")
    class UpdateUser {

        @Test
        @DisplayName("updates user fields successfully when no conflict")
        void updatesOk() {
            // given
            User bob = persistUser(
                    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    "bob",
                    "Bob"
            );
            bob.setUserName("Bobby");
            // when
            assertDoesNotThrow(() -> sut.update(bob));
            // then
            Optional<User> got = sut.findById(bob.getUserId());
            assertTrue(got.isPresent());
            assertEquals("Bobby", got.get().getUserName());
        }

        @Test
        @DisplayName("throws DuplicateLoginException when changing login to existing one ('alice')")
        void updateDuplicateLogin() {
            // given
            User bob = persistUser(
                    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    "bob",
                    "Bob"
            );
            bob.setUserLogin("alice");
            // when / then
            assertThrows(DuplicateLoginException.class, () -> sut.update(bob));
        }
    }

    @Nested
    @DisplayName("findPage(page, size)")
    class FindPage {

        @Test
        @DisplayName("returns immutable page ordered by createdAt desc, then login asc")
        void basicPaging() {
            // given
            User u1 = new User();
            u1.setUserId("11111111-aaaa-bbbb-cccc-000000000001");
            u1.setUserLogin("charlie");
            u1.setUserName("Charlie");
            u1.setRole(Role.USER);
            u1.setPassword("test");
            u1.setCreatedAt(Instant.parse("2099-01-01T00:00:00Z"));
            cur().persist(u1);
            User u2 = new User();
            u2.setUserId("11111111-aaaa-bbbb-cccc-000000000002");
            u2.setUserLogin("david");
            u2.setUserName("David");
            u2.setRole(Role.USER);
            u2.setPassword("test");
            u2.setCreatedAt(Instant.parse("2099-02-01T00:00:00Z")); // новее u1
            cur().persist(u2);
            // when
            List<User> page1 = sut.findPage(1, 2);
            // then
            assertEquals(2, page1.size(), "should return 2 users");
            assertEquals("david", page1.get(0).getUserLogin().toLowerCase(Locale.ROOT));
            assertEquals("charlie", page1.get(1).getUserLogin().toLowerCase(Locale.ROOT));
            assertThrows(UnsupportedOperationException.class, () -> page1.add(new User()));
        }
    }

    @Nested
    @DisplayName("countAll()")
    class CountAll {

        @Test
        @DisplayName("returns total number of users")
        void counts() {
            // given
            long before = sut.countAll();
            // when
            persistUser("99999999-aaaa-bbbb-cccc-ffffffffffff", "zz", "Zed");
            // then
            long after = sut.countAll();
            assertEquals(before + 1, after);
        }
    }
}