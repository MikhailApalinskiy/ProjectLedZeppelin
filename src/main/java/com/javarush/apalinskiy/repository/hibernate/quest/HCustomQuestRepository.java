package com.javarush.apalinskiy.repository.hibernate.quest;

import com.javarush.apalinskiy.utils.HibernateUtil;
import com.javarush.apalinskiy.domain.quest.custom.PendingEditRow;
import com.javarush.apalinskiy.domain.quest.custom.PendingNewRow;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.domain.quest.custom.CustomQuest;
import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.repository.quest.CustomQuestRepository;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Hibernate-backed implementation of {@link CustomQuestRepository}.
 *
 * <p>Provides creation, retrieval, update, deletion, and moderation staging/approval
 * for {@link CustomQuest} entities. Uses explicit transaction boundaries per method
 * and HQL for pagination and counting. Graph (nodes/options) serialization is handled
 * by {@link GraphJsonMapper}.</p>
 *
 * <p><strong>Transactions:</strong> read methods open short transactions for
 * initialization of lazy associations; write methods commit or roll back on failure.</p>
 */
public class HCustomQuestRepository implements CustomQuestRepository {

    private static final Logger log = LoggerFactory.getLogger(HCustomQuestRepository.class);

    /**
     * Shared Hibernate {@link SessionFactory} used to manage persistence sessions.
     */
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    /**
     * Creates a new quest owned by the given user and persists its graph.
     *
     * <p>Derives {@link CustomQuest.ModerationStatus} from {@code published} flag,
     * attaches nodes via {@link GraphJsonMapper#attachGraphToQuest(CustomQuest, List)} and
     * persists the entity.</p>
     *
     * @param ownerId     owner user ID
     * @param name        quest name
     * @param startId     start node ID
     * @param nodes       quest nodes
     * @param published   whether the quest should be immediately live
     * @param versionNote version/comment label
     * @throws IllegalArgumentException if owner user is not found
     */
    @Override
    public void create(String ownerId, String name, int startId, List<QuestNode> nodes,
                       boolean published, String versionNote) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("create: ownerId={}, name='{}', startId={}, published={}, nodes={}",
                    ownerId, name, startId, published, nodes.size());
            User owner = session.get(User.class, ownerId);
            if (owner == null) {
                log.warn("create: owner not found ownerId={}", ownerId);
                throw new IllegalArgumentException("Owner user not found: " + ownerId);
            }
            CustomQuest.ModerationStatus status = published ? CustomQuest.ModerationStatus.LIVE : CustomQuest.ModerationStatus.PENDING_NEW;
            CustomQuest quest = CustomQuest.create(owner, name, startId, nodes, versionNote, published);
            quest.setModerationStatus(status);
            GraphJsonMapper.attachGraphToQuest(quest, nodes);
            session.persist(quest);
            tx.commit();
            log.info("create: success id={} status={} nodes={}", quest.getId(), status, quest.getNodes().size());
        } catch (RuntimeException e) {
            log.error("create: failed ownerId={} name='{}'", ownerId, name, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Retrieves a quest by ID and initializes owner and node graph.
     *
     * <p>Initializes {@code user}, {@code nodes}, and node {@code options}
     * to make the graph accessible outside the transaction.</p>
     *
     * @param id quest ID
     * @return optional quest or empty if not found
     */
    @Override
    public Optional<CustomQuest> get(String id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("get: id={}", id);
            CustomQuest q = session.get(CustomQuest.class, id);
            if (q != null) {
                Hibernate.initialize(q.getUser());
                Hibernate.initialize(q.getNodes());
                q.getNodes().forEach(n -> Hibernate.initialize(n.getOptions()));
                log.debug("get: found id={} nodes={}", id, q.getNodes() == null ? 0 : q.getNodes().size());
            } else {
                log.debug("get: not found id={}", id);
            }
            tx.commit();
            return Optional.ofNullable(q);
        } catch (RuntimeException e) {
            log.error("get: failed id={}", id, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Updates a quest’s start node, version note, moderation status, and graph.
     *
     * <p>Sets {@link CustomQuest.ModerationStatus} to {@code LIVE} or {@code PENDING_EDIT}
     * depending on {@code published} flag; replaces graph via
     * {@link GraphJsonMapper#attachGraphToQuest(CustomQuest, List)}.</p>
     *
     * @param id          quest ID
     * @param startId     new start node ID
     * @param nodes       replacement node list
     * @param published   whether the updated quest should be live
     * @param versionNote version/comment label
     */
    @Override
    public void update(String id, int startId, List<QuestNode> nodes, boolean published, String versionNote) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("update: id={} startId={} published={} nodes={}",
                    id, startId, published, nodes.size());
            CustomQuest q = session.get(CustomQuest.class, id);
            if (q == null) {
                log.warn("update: quest not found id={}", id);
                tx.commit();
                return;
            }
            q.setStartId(startId);
            q.setVersion(versionNote);
            q.setModerationStatus(published ? CustomQuest.ModerationStatus.LIVE : CustomQuest.ModerationStatus.PENDING_EDIT);
            q.setUpdatedAt(Instant.now());
            GraphJsonMapper.attachGraphToQuest(q, nodes);
            session.merge(q);
            tx.commit();
            log.info("update: success id={} status={} nodes={}", id, q.getModerationStatus(), q.getNodes().size());
        } catch (RuntimeException e) {
            log.error("update: failed id={}", id, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Deletes a quest by ID and removes a pending edit row if present.
     *
     * @param id quest ID
     * @return {@code true} if the quest existed and was deleted; {@code false} otherwise
     */
    @Override
    public boolean delete(String id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("delete: id={}", id);
            CustomQuest q = session.get(CustomQuest.class, id);
            if (q == null) {
                tx.commit();
                log.warn("delete: not found id={}", id);
                return false;
            }
            session.remove(q);
            PendingEditRow pe = session.get(PendingEditRow.class, id);
            if (pe != null) {
                session.remove(pe);
                log.debug("delete: removed pendingEdit questId={}", id);
            }
            tx.commit();
            log.info("delete: success id={}", id);
            return true;
        } catch (RuntimeException e) {
            log.error("delete: failed id={}", id, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Deletes a quest if the caller is the owner and removes a pending edit row if present.
     *
     * @param id      quest ID
     * @param ownerId expected owner user ID
     * @return {@code true} if deleted; {@code false} if access denied or not found
     */
    @Override
    public boolean deleteIfOwner(String id, String ownerId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("deleteIfOwner: id={} ownerId={}", id, ownerId);
            CustomQuest q = session.get(CustomQuest.class, id);
            if (q == null || q.getUser() == null || !ownerId.equals(q.getUser().getUserId())) {
                tx.commit();
                log.warn("deleteIfOwner: denied id={} ownerId={}", id, ownerId);
                return false;
            }
            session.remove(q);
            PendingEditRow pe = session.get(PendingEditRow.class, id);
            if (pe != null) {
                session.remove(pe);
                log.debug("deleteIfOwner: removed pendingEdit questId={}", id);
            }
            tx.commit();
            log.info("deleteIfOwner: success id={} ownerId={}", id, ownerId);
            return true;
        } catch (RuntimeException e) {
            log.error("deleteIfOwner: failed id={} ownerId={}", id, ownerId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Stages a new quest submission for moderation (PENDING_NEW).
     *
     * <p>Serializes the provided graph to JSON and stores it in {@link PendingNewRow}.</p>
     *
     * @param ownerId     owner user ID
     * @param name        quest name
     * @param startId     start node ID
     * @param nodes       quest nodes
     * @param versionNote version/comment label
     */
    @Override
    public void stageCreate(String ownerId, String name, int startId, List<QuestNode> nodes, String versionNote) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("stageCreate: ownerId={} name='{}' startId={} nodes={}",
                    ownerId, name, startId, nodes.size());
            String pendingId = "new-" + UUID.randomUUID();
            PendingNewRow row = new PendingNewRow();
            row.setPendingId(pendingId);
            row.setOwnerId(ownerId);
            row.setName(name);
            row.setStartId(startId);
            row.setNodesJson(GraphJsonMapper.toJson(nodes));
            row.setVersionNote(versionNote);
            row.setSubmittedAt(Instant.now());
            session.persist(row);
            tx.commit();
            log.info("stageCreate: queued pendingId={}", pendingId);
        } catch (RuntimeException e) {
            log.error("stageCreate: failed ownerId={} name='{}'", ownerId, name, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Lists pending new quest submissions ordered by submit time (desc).
     *
     * @return list of lightweight DTOs representing pending new quests
     */
    @Override
    public List<PendingNew> listPendingNew() {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("listPendingNew");
            List<PendingNewRow> rows = session.createQuery(
                            "select p from PendingNewRow p order by p.submittedAt desc", PendingNewRow.class)
                    .getResultList();
            List<PendingNew> out = rows.stream()
                    .map(r -> new PendingNew(
                            r.getPendingId(), r.getOwnerId(), r.getName(),
                            r.getStartId(), GraphJsonMapper.fromJson(r.getNodesJson()),
                            r.getVersionNote(), r.getSubmittedAt()))
                    .toList();
            tx.commit();
            log.debug("listPendingNew: size={}", out.size());
            return out;
        } catch (RuntimeException e) {
            log.error("listPendingNew: failed", e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Approves a pending new quest: materializes a {@link CustomQuest} and removes the pending row.
     *
     * @param pendingId pending submission ID
     * @return created quest ID
     * @throws NoSuchElementException if pending submission not found
     * @throws IllegalStateException  if the owner cannot be resolved
     */
    @Override
    public String approveCreate(String pendingId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("approveCreate: pendingId={}", pendingId);
            PendingNewRow row = session.get(PendingNewRow.class, pendingId);
            if (row == null) {
                log.warn("approveCreate: not found pendingId={}", pendingId);
                tx.rollback();
                throw new NoSuchElementException("Pending new quest not found: " + pendingId);
            }
            User owner = session.get(User.class, row.getOwnerId());
            if (owner == null) {
                log.warn("approveCreate: owner not found ownerId={}", row.getOwnerId());
                tx.rollback();
                throw new IllegalStateException("Owner not found: " + row.getOwnerId());
            }
            List<QuestNode> nodes = GraphJsonMapper.fromJson(row.getNodesJson());
            CustomQuest q = CustomQuest.create(owner, row.getName(), row.getStartId(), nodes, row.getVersionNote(), true);
            q.setModerationStatus(CustomQuest.ModerationStatus.LIVE);
            GraphJsonMapper.attachGraphToQuest(q, nodes);
            session.persist(q);
            session.remove(row);
            tx.commit();
            log.info("approveCreate: success pendingId={} -> questId={}", pendingId, q.getId());
            return q.getId();
        } catch (RuntimeException e) {
            log.error("approveCreate: failed pendingId={}", pendingId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Rejects a pending new quest by removing its pending row.
     *
     * @param pendingId pending submission ID
     * @throws NoSuchElementException if pending submission not found
     */
    @Override
    public void rejectCreate(String pendingId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("rejectCreate: pendingId={}", pendingId);
            PendingNewRow row = session.get(PendingNewRow.class, pendingId);
            if (row == null) {
                log.warn("rejectCreate: not found pendingId={}", pendingId);
                tx.rollback();
                throw new NoSuchElementException("Pending new quest not found: " + pendingId);
            }
            session.remove(row);
            tx.commit();
            log.info("rejectCreate: success pendingId={}", pendingId);
        } catch (RuntimeException e) {
            log.error("rejectCreate: failed pendingId={}", pendingId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Stages an edit for an existing quest (PENDING_EDIT) and stores the new graph JSON.
     *
     * <p>Marks the live quest as {@code PENDING_EDIT} and upserts {@link PendingEditRow}.</p>
     *
     * @param questId     quest ID being edited
     * @param startId     proposed start node ID
     * @param nodes       proposed node list
     * @param versionNote version/comment label
     * @throws NoSuchElementException if the quest is not found
     */
    @Override
    public void stageEdit(String questId, int startId, List<QuestNode> nodes, String versionNote) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("stageEdit: questId={} startId={} nodes={}", questId, startId, nodes == null ? 0 : nodes.size());
            CustomQuest live = session.get(CustomQuest.class, questId);
            if (live == null) {
                log.warn("stageEdit: quest not found questId={}", questId);
                tx.rollback();
                throw new NoSuchElementException("Quest not found: " + questId);
            }
            live.setModerationStatus(CustomQuest.ModerationStatus.PENDING_EDIT);
            live.setUpdatedAt(Instant.now());
            session.merge(live);
            PendingEditRow row = session.get(PendingEditRow.class, questId);
            if (row == null) {
                row = new PendingEditRow();
                row.setQuestId(questId);
            }
            row.setOwnerId(live.getUser().getUserId());
            row.setName(live.getName());
            row.setStartId(startId);
            row.setNodesJson(GraphJsonMapper.toJson(nodes));
            row.setVersionNote(versionNote);
            row.setSubmittedAt(Instant.now());
            session.merge(row);
            tx.commit();
            log.info("stageEdit: queued questId={} status=PENDING_EDIT", questId);
        } catch (RuntimeException e) {
            log.error("stageEdit: failed questId={}", questId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Lists pending quest edits ordered by submit time (desc).
     *
     * @return list of lightweight DTOs representing pending edits
     */
    @Override
    public List<PendingEdit> listPendingEdits() {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("listPendingEdits");
            List<PendingEditRow> rows = session.createQuery(
                            "select p from PendingEditRow p order by p.submittedAt desc", PendingEditRow.class)
                    .getResultList();
            List<PendingEdit> out = rows.stream()
                    .map(r -> new PendingEdit(
                            r.getQuestId(), r.getOwnerId(), r.getName(),
                            r.getStartId(), GraphJsonMapper.fromJson(r.getNodesJson()),
                            r.getVersionNote(), r.getSubmittedAt()))
                    .sorted(Comparator.comparing(PendingEdit::getSubmittedAt).reversed())
                    .toList();
            tx.commit();
            log.debug("listPendingEdits: size={}", out.size());
            return out;
        } catch (RuntimeException e) {
            log.error("listPendingEdits: failed", e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Approves a pending edit: replaces the live quest graph and removes the pending row.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if pending edit or quest is not found
     */
    @Override
    public void approveEdit(String questId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("approveEdit: questId={}", questId);
            PendingEditRow row = session.get(PendingEditRow.class, questId);
            if (row == null) {
                log.warn("approveEdit: pending edit not found questId={}", questId);
                tx.rollback();
                throw new NoSuchElementException("Pending edit not found: " + questId);
            }
            CustomQuest q = session.get(CustomQuest.class, questId);
            if (q == null) {
                log.warn("approveEdit: quest not found questId={}", questId);
                tx.rollback();
                throw new NoSuchElementException("Quest not found: " + questId);
            }
            List<QuestNode> nodes = GraphJsonMapper.fromJson(row.getNodesJson());
            q.setStartId(row.getStartId());
            q.setVersion(row.getVersionNote());
            q.setModerationStatus(CustomQuest.ModerationStatus.LIVE);
            q.setUpdatedAt(Instant.now());
            q.getNodes().clear();
            GraphJsonMapper.attachGraphToQuest(q, nodes);
            session.merge(q);
            session.remove(row);
            tx.commit();
            log.info("approveEdit: success questId={} nodes={}", questId, nodes.size());
        } catch (RuntimeException e) {
            log.error("approveEdit: failed questId={}", questId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Rejects a pending edit: removes the pending row and restores quest status to LIVE.
     *
     * @param questId quest ID
     * @throws NoSuchElementException if pending edit is not found
     */
    @Override
    public void rejectEdit(String questId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("rejectEdit: questId={}", questId);
            PendingEditRow row = session.get(PendingEditRow.class, questId);
            if (row == null) {
                log.warn("rejectEdit: pending edit not found questId={}", questId);
                tx.rollback();
                throw new NoSuchElementException("Pending edit not found: " + questId);
            }
            session.remove(row);
            CustomQuest q = session.get(CustomQuest.class, questId);
            if (q != null) {
                q.setModerationStatus(CustomQuest.ModerationStatus.LIVE);
                q.setUpdatedAt(Instant.now());
                session.merge(q);
            }
            tx.commit();
            log.info("rejectEdit: success questId={} (graph unchanged)", questId);
        } catch (RuntimeException e) {
            log.error("rejectEdit: failed questId={}", questId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Counts live quests, optionally filtered by a case-insensitive name substring.
     *
     * @param q optional search text; ignored if blank
     * @return total number of matching live quests
     */
    @Override
    public int countAllLive(String q) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            boolean hasQ = q != null && !q.isBlank();
            log.debug("countAllLive: qPresent={}", hasQ);
            final String hql = hasQ
                    ? """
                     select count(q.id)
                     from CustomQuest q
                     where q.moderationStatus = :live
                       and lower(q.name) like :pat
                    """
                    : """
                     select count(q.id)
                     from CustomQuest q
                     where q.moderationStatus = :live
                    """;
            Query<Long> qh = session.createQuery(hql, Long.class)
                    .setParameter("live", CustomQuest.ModerationStatus.LIVE);
            if (hasQ) {
                String pat = "%" + q.toLowerCase(Locale.ROOT).trim() + "%";
                qh.setParameter("pat", pat);
                log.trace("countAllLive: HQL with filter pat='{}'", pat);
            } else {
                log.trace("countAllLive: HQL without filter");
            }
            Long cnt = qh.uniqueResult();
            tx.commit();
            int out = (cnt == null ? 0 : cnt.intValue());
            log.debug("countAllLive: result={}", out);
            return out;
        } catch (RuntimeException e) {
            log.error("countAllLive: failed q='{}'", q, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Retrieves a page of live quests (with authors) ordered by update time (desc),
     * optionally filtered by case-insensitive name substring.
     *
     * @param page page number (1-based)
     * @param size page size
     * @param q    optional search text; ignored if blank
     * @return list of live quests for the page
     */
    @Override
    public List<CustomQuest> findAllLivePaged(int page, int size, String q) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            boolean hasQ = q != null && !q.isBlank();
            log.debug("findAllLivePaged: page={} size={} qPresent={}", page, size, hasQ);
            final String hql = hasQ
                    ? """
                     select q
                     from CustomQuest q
                     join fetch q.user
                     where q.moderationStatus = :live
                       and lower(q.name) like :pat
                     order by q.updatedAt desc
                    """
                    : """
                     select q
                     from CustomQuest q
                     join fetch q.user
                     where q.moderationStatus = :live
                     order by q.updatedAt desc
                    """;
            Query<CustomQuest> qh = session.createQuery(hql, CustomQuest.class)
                    .setParameter("live", CustomQuest.ModerationStatus.LIVE)
                    .setFirstResult(Math.max(0, (page - 1) * size))
                    .setMaxResults(size);
            if (hasQ) {
                String pat = "%" + q.toLowerCase(Locale.ROOT).trim() + "%";
                qh.setParameter("pat", pat);
                log.trace("findAllLivePaged: pat='{}'", pat);
            }
            List<CustomQuest> list = qh.getResultList();
            list.forEach(qst -> Hibernate.initialize(qst.getNodes()));
            tx.commit();
            log.debug("findAllLivePaged: result size={}", list.size());
            return list;
        } catch (RuntimeException e) {
            log.error("findAllLivePaged: failed page={} size={} q='{}'", page, size, q, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Counts quests by owner, optionally filtering by name and/or restricting status to LIVE.
     *
     * @param ownerId  owner user ID
     * @param q        optional search text; ignored if blank
     * @param onlyLive whether to count only LIVE quests
     * @return total number of matching quests
     */
    @Override
    public int countByOwner(String ownerId, String q, boolean onlyLive) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            boolean hasQ = (q != null && !q.isBlank());
            log.debug("countByOwner: ownerId={} qPresent={} onlyLive={}", ownerId, hasQ, onlyLive);
            final String hql = hasQ
                    ? """
                    select count(q.id)
                    from CustomQuest q
                    where q.user.userId = :uid
                      and q.moderationStatus in (:st)
                      and lower(q.name) like :pat
                    """
                    : """
                    select count(q.id)
                    from CustomQuest q
                    where q.user.userId = :uid
                      and q.moderationStatus in (:st)
                    """;
            Query<Long> query = session.createQuery(hql, Long.class)
                    .setParameter("uid", ownerId)
                    .setParameter("st", onlyLive
                                    ? List.of(CustomQuest.ModerationStatus.LIVE)
                                    : List.of(
                                    CustomQuest.ModerationStatus.LIVE,
                                    CustomQuest.ModerationStatus.PENDING_NEW,
                                    CustomQuest.ModerationStatus.PENDING_EDIT,
                                    CustomQuest.ModerationStatus.REJECTED,
                                    CustomQuest.ModerationStatus.ARCHIVED
                            )
                    );
            if (hasQ) {
                query.setParameter("pat", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
            }
            Long cnt = query.uniqueResult();
            tx.commit();
            int out = (cnt == null ? 0 : cnt.intValue());
            log.debug("countByOwner: result={}", out);
            return out;
        } catch (RuntimeException e) {
            log.error("countByOwner: failed ownerId={} q='{}' onlyLive={}", ownerId, q, onlyLive, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Retrieves a page of quests by owner (with authors) ordered by update time (desc),
     * optionally filtered by name and/or restricted to LIVE status.
     *
     * @param ownerId  owner user ID
     * @param page     page number (1-based)
     * @param size     page size
     * @param q        optional search text; ignored if blank
     * @param onlyLive whether to include only LIVE quests
     * @return list of owner’s quests for the page
     */
    @Override
    public List<CustomQuest> findByOwnerPaged(String ownerId, int page, int size, String q, boolean onlyLive) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            boolean hasQ = (q != null && !q.isBlank());
            log.debug("findByOwnerPaged: ownerId={} page={} size={} qPresent={} onlyLive={}",
                    ownerId, page, size, hasQ, onlyLive);
            final String hql = hasQ
                    ? """
                    select q
                    from CustomQuest q
                    join fetch q.user
                    where q.user.userId = :uid
                      and q.moderationStatus in (:st)
                      and lower(q.name) like :pat
                    order by q.updatedAt desc
                    """
                    : """
                    select q
                    from CustomQuest q
                    join fetch q.user
                    where q.user.userId = :uid
                      and q.moderationStatus in (:st)
                    order by q.updatedAt desc
                    """;
            Query<CustomQuest> query = session.createQuery(hql, CustomQuest.class)
                    .setParameter("uid", ownerId)
                    .setParameter("st", onlyLive
                                    ? List.of(CustomQuest.ModerationStatus.LIVE)
                                    : List.of(
                                    CustomQuest.ModerationStatus.LIVE,
                                    CustomQuest.ModerationStatus.PENDING_NEW,
                                    CustomQuest.ModerationStatus.PENDING_EDIT,
                                    CustomQuest.ModerationStatus.REJECTED,
                                    CustomQuest.ModerationStatus.ARCHIVED
                            )
                    )
                    .setFirstResult(Math.max(0, (page - 1) * size))
                    .setMaxResults(size);
            if (hasQ) {
                query.setParameter("pat", "%" + q.trim().toLowerCase(Locale.ROOT) + "%");
            }
            List<CustomQuest> list = query.getResultList();
            list.forEach(qst -> Hibernate.initialize(qst.getNodes()));
            tx.commit();
            log.debug("findByOwnerPaged: result size={}", list.size());
            return list;
        } catch (RuntimeException e) {
            log.error("findByOwnerPaged: failed ownerId={} page={} size={} q='{}' onlyLive={}",
                    ownerId, page, size, q, onlyLive, e);
            tx.rollback();
            throw e;
        }
    }
}