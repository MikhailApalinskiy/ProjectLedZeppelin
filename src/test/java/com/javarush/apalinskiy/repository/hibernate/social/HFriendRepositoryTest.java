package com.javarush.apalinskiy.repository.hibernate.social;

import com.javarush.apalinskiy.domain.social.FriendRequest;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
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

@DisplayName("HFriendRepository (IT) — seeded by Liquibase testdata")
class HFriendRepositoryIT extends AbstractDbTest {

    private static final String ADMIN_ID = "11111111-2222-3333-4444-555555555555";
    private static final String ALICE_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private SessionFactory sf;
    private Transaction tx;
    private HFriendRepository sut;
    private MockedStatic<HibernateUtil> mockedHibernateUtil;

    @BeforeEach
    void initFriendRepo() {
        this.sf = super.sessionFactory;
        Session s = sf.getCurrentSession();
        tx = s.beginTransaction();
        mockedHibernateUtil = mockStatic(HibernateUtil.class);
        mockedHibernateUtil.when(HibernateUtil::getSessionFactory).thenReturn(sf);
        sut = new HFriendRepository();
    }

    @AfterEach
    void cleanupFriendRepo() {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        if (mockedHibernateUtil != null) {
            mockedHibernateUtil.close();
        }
    }

    private Session cur() {
        return sf.getCurrentSession();
    }

    private User persistUser() {
        User u = new User();
        u.setUserId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        u.setUserLogin("bob");
        u.setUserName("Bob");
        u.setRole(Role.USER);
        u.setPassword("test");
        Instant now = Instant.now();
        u.setCreatedAt(now);
        if (u.getFriends() == null) {
            u.setFriends(new HashSet<>());
        }
        if (u.getFriendOf() == null) {
            u.setFriendOf(new HashSet<>());
        }
        cur().persist(u);
        return u;
    }

    private FriendRequest mkReq(User from, User to) {
        FriendRequest fr = new FriendRequest();
        fr.setId(UUID.randomUUID().toString());
        fr.setFromUser(from);
        fr.setToUser(to);
        fr.setStatus(FriendRequest.Status.PENDING);
        fr.setCreatedAt(Instant.now());
        return fr;
    }

    @Nested
    @DisplayName("areFriends(a, b)")
    class AreFriends {

        @Test
        @DisplayName("returns true for seeded Admin ↔ Alice friendship")
        void seededTrue() {
            // when
            boolean ok1 = sut.areFriends(ADMIN_ID, ALICE_ID);
            boolean ok2 = sut.areFriends(ALICE_ID, ADMIN_ID);
            // then
            assertTrue(ok1);
            assertTrue(ok2, "Query is symmetric via OR in HQL");
        }

        @Test
        @DisplayName("returns false for unrelated users / self")
        void falseCases() {
            // given
            String ghost = "00000000-0000-0000-0000-000000000000";
            // when / then
            assertFalse(sut.areFriends(ALICE_ID, ghost));
            assertFalse(sut.areFriends(ALICE_ID, ALICE_ID), "No self-friendship expected");
        }
    }

    @Nested
    @DisplayName("addFriendship(a, b)")
    class AddFriendship {

        @Test
        @DisplayName("links two existing users that were not linked before")
        void linksNewUsers() {
            // given
            User bob = persistUser();
            assertFalse(sut.areFriends(ALICE_ID, bob.getUserId()));
            // when
            sut.addFriendship(ALICE_ID, bob.getUserId());
            // then
            assertTrue(sut.areFriends(ALICE_ID, bob.getUserId()));
            Set<String> fAlice = sut.friendsOf(ALICE_ID);
            Set<String> fBob = sut.friendsOf(bob.getUserId());
            assertTrue(fAlice.contains(bob.getUserId()));
            assertTrue(fBob.contains(ALICE_ID));
        }

        @Test
        @DisplayName("no-op when a == b")
        void noOpSameIds() {
            // given
            Set<String> before = sut.friendsOf(ALICE_ID);
            // when
            sut.addFriendship(ALICE_ID, ALICE_ID);
            // then
            Set<String> after = sut.friendsOf(ALICE_ID);
            assertEquals(before, after);
        }

        @Test
        @DisplayName("no-op when one of users is missing")
        void noOpMissingUser() {
            // when / then
            assertDoesNotThrow(() -> sut.addFriendship(ALICE_ID, "ffffffff-ffff-ffff-ffff-ffffffffffff"));
        }
    }

    @Nested
    @DisplayName("removeFriendship(a, b)")
    class RemoveFriendship {

        @Test
        @DisplayName("removes existing friendship (Admin ↔ Alice)")
        void removesSeeded() {
            assertTrue(sut.areFriends(ADMIN_ID, ALICE_ID));
            // when
            sut.removeFriendship(ADMIN_ID, ALICE_ID);
            // then
            assertFalse(sut.areFriends(ADMIN_ID, ALICE_ID));
            assertFalse(sut.friendsOf(ADMIN_ID).contains(ALICE_ID));
            assertFalse(sut.friendsOf(ALICE_ID).contains(ADMIN_ID));
        }

        @Test
        @DisplayName("no-op when users are not linked or missing")
        void noOpForNonLinked() {
            // given
            User bob = persistUser();
            assertFalse(sut.areFriends(ALICE_ID, bob.getUserId()));
            // when / then
            assertDoesNotThrow(() -> sut.removeFriendship(ALICE_ID, bob.getUserId()));
            assertDoesNotThrow(() -> sut.removeFriendship(ALICE_ID, "ffffffff-ffff-ffff-ffff-ffffffffffff"));
        }
    }

    @Nested
    @DisplayName("friendsOf(userId)")
    class FriendsOf {

        @Test
        @DisplayName("returns unique set of friends (seeded Admin has Alice)")
        void seededSet() {
            // when
            Set<String> adminFriends = sut.friendsOf(ADMIN_ID);
            // then
            assertTrue(adminFriends.contains(ALICE_ID));
        }
    }

    @Nested
    @DisplayName("friend requests API")
    class RequestsApi {

        @Test
        @DisplayName("findPending() finds seeded Alice→Admin")
        void findSeededPending() {
            // when
            Optional<FriendRequest> req = sut.findPending(ALICE_ID, ADMIN_ID);
            // then
            assertTrue(req.isPresent());
            assertEquals(FriendRequest.Status.PENDING, req.get().getStatus());
            assertEquals(ALICE_ID, req.get().getFromUser().getUserId());
            assertEquals(ADMIN_ID, req.get().getToUser().getUserId());
        }

        @Test
        @DisplayName("saveRequest() persists new Admin→Alice request; removeRequest() deletes it")
        void saveAndRemove() {
            // given
            User admin = cur().get(User.class, ADMIN_ID);
            User alice = cur().get(User.class, ALICE_ID);
            FriendRequest fr = mkReq(admin, alice);
            // when
            sut.saveRequest(fr);
            // then
            assertTrue(sut.findPending(ADMIN_ID, ALICE_ID).isPresent());
            // when
            sut.removeRequest(ADMIN_ID, ALICE_ID);
            // then
            assertTrue(sut.findPending(ADMIN_ID, ALICE_ID).isEmpty());
        }
    }

    @Nested
    @DisplayName("countFriends / pageFriends")
    class CountingPaging {

        @Test
        @DisplayName("countFriends() returns total; filter by login/name is case-insensitive; UUID matches id")
        void countVariants() {
            // given
            // when / then
            assertEquals(1, sut.countFriends(ADMIN_ID, null));
            assertEquals(1, sut.countFriends(ADMIN_ID, "ali"));  // login/name partial
            assertEquals(1, sut.countFriends(ADMIN_ID, ALICE_ID)); // UUID direct match
            assertEquals(0, sut.countFriends(ADMIN_ID, "zzz"));
        }

        @Test
        @DisplayName("pageFriends() returns ordered page; filter aligns with countFriends()")
        void paging() {
            // when
            List<User> page1 = sut.pageFriends(ADMIN_ID, 1, 1, null);
            List<User> page2 = sut.pageFriends(ADMIN_ID, 2, 1, null);
            // then
            assertEquals(1, page1.size());
            assertEquals("alice", page1.getFirst().getUserLogin().toLowerCase());
            assertTrue(page2.isEmpty());
            List<User> filtered = sut.pageFriends(ADMIN_ID, 1, 10, "ali");
            assertEquals(1, filtered.size());
            assertEquals(ALICE_ID, filtered.getFirst().getUserId());
        }
    }
}
