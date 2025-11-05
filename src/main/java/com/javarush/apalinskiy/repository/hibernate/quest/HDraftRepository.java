package com.javarush.apalinskiy.repository.hibernate.quest;

import com.javarush.apalinskiy.domain.quest.QuestNode;
import com.javarush.apalinskiy.domain.quest.custom.DraftRow;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Hibernate-backed repository for managing quest drafts.
 *
 * <p>Provides upsert, lookup, pagination, rename and delete operations for {@link DraftRow}
 * entities that hold serialized quest graphs (see {@code nodes_json}). Each method
 * manages its own Hibernate transaction boundary and writes diagnostic logs.</p>
 *
 * <p>Read paths may set the session to read-only where appropriate; write paths
 * commit or roll back on failure.</p>
 */
@RequiredArgsConstructor
public class HDraftRepository {

    private static final Logger log = LoggerFactory.getLogger(HDraftRepository.class);

    /**
     * Session factory supplied by the application (e.g., via {@code HibernateUtil}).
     */
    private final SessionFactory sessionFactory;

    /**
     * Creates or updates the latest draft for the given owner and target quest.
     *
     * <p>If a draft exists for the (owner, targetQuestId) pair, it is updated;
     * otherwise a new draft is created. The nodes list is serialized to JSON.</p>
     *
     * @param ownerId       draft owner user ID (required)
     * @param targetQuestId optional quest ID this draft targets; {@code null} for new quest
     * @param name          draft display name; defaults to "Untitled Draft" if blank
     * @param startId       proposed start node ID
     * @param nodes         quest nodes to serialize and store
     * @param versionNote   optional version/comment label (may be {@code null})
     * @throws RuntimeException if the transaction fails
     */
    public void upsertDraft(String ownerId, String targetQuestId, String name,
                            int startId, List<QuestNode> nodes, String versionNote) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("upsertDraft: ownerId={} targetQuestId={} name='{}' nodes={}",
                    ownerId, targetQuestId, name, nodes.size());
            DraftRow row = session.createQuery("""
                            select d from DraftRow d
                            where d.ownerId = :o and ((:t is null and d.targetQuestId is null) or d.targetQuestId = :t)
                            order by d.updatedAt desc
                            """, DraftRow.class)
                    .setParameter("o", ownerId)
                    .setParameter("t", targetQuestId)
                    .setMaxResults(1)
                    .uniqueResult();
            if (row == null) {
                log.debug("upsertDraft: creating new draft for ownerId={} targetQuestId={}", ownerId, targetQuestId);
                row = new DraftRow();
                row.setDraftId(UUID.randomUUID().toString());
                row.setOwnerId(ownerId);
                row.setTargetQuestId(targetQuestId);
            } else {
                log.debug("upsertDraft: updating existing draftId={}", row.getDraftId());
            }
            row.setName((name == null || name.isBlank()) ? "Untitled Draft" : name.trim());
            row.setStartId(startId);
            row.setNodesJson(GraphJsonMapper.toJson(nodes));
            row.setVersionNote(versionNote == null ? "" : versionNote);
            row.setUpdatedAt(Instant.now());
            session.merge(row);
            tx.commit();
            log.info("upsertDraft: success ownerId={} draftId={}", ownerId, row.getDraftId());
        } catch (RuntimeException e) {
            log.error("upsertDraft: failed ownerId={} targetQuestId={}", ownerId, targetQuestId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Finds the most recently updated draft for the (owner, targetQuestId) key.
     *
     * @param ownerId       owner user ID
     * @param targetQuestId optional quest ID; {@code null} to search drafts for new quests
     * @return optional latest {@link DraftRow}
     */
    public Optional<DraftRow> findLatest(String ownerId, String targetQuestId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("findLatest: ownerId={} targetQuestId={}", ownerId, targetQuestId);
            DraftRow row = session.createQuery("""
                            select d from DraftRow d
                            where d.ownerId = :o and ((:t is null and d.targetQuestId is null) or d.targetQuestId = :t)
                            order by d.updatedAt desc
                            """, DraftRow.class)
                    .setParameter("o", ownerId)
                    .setParameter("t", targetQuestId)
                    .setMaxResults(1)
                    .uniqueResult();
            tx.commit();
            log.debug("findLatest: found={}", row != null);
            return Optional.ofNullable(row);
        } catch (RuntimeException e) {
            log.error("findLatest: failed ownerId={} targetQuestId={}", ownerId, targetQuestId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Alias for {@link #findById(String)}.
     *
     * @param draftId draft identifier
     * @return optional {@link DraftRow}
     */
    public Optional<DraftRow> get(String draftId) {
        return findById(draftId);
    }

    /**
     * Deletes a draft when it exists and belongs to the specified owner.
     *
     * @param draftId draft identifier
     * @param ownerId expected owner user ID
     * @throws RuntimeException if the transaction fails
     */
    public void delete(String draftId, String ownerId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("delete: draftId={} ownerId={}", draftId, ownerId);
            DraftRow row = session.get(DraftRow.class, draftId);
            if (row != null && row.getOwnerId().equals(ownerId)) {
                session.remove(row);
                log.debug("delete: removed draftId={}", draftId);
            } else {
                log.warn("delete: denied or not found draftId={} ownerId={}", draftId, ownerId);
            }
            tx.commit();
        } catch (RuntimeException e) {
            log.error("delete: failed draftId={} ownerId={}", draftId, ownerId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Loads a draft by its identifier.
     *
     * @param draftId draft identifier
     * @return optional {@link DraftRow}
     */
    public Optional<DraftRow> findById(String draftId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.debug("findById: draftId={}", draftId);
            DraftRow row = session.get(DraftRow.class, draftId);
            tx.commit();
            log.debug("findById: found={}", row != null);
            return Optional.ofNullable(row);
        } catch (RuntimeException e) {
            log.error("findById: failed draftId={}", draftId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Counts drafts owned by a user, optionally filtering by a case-insensitive name pattern.
     *
     * @param ownerId owner user ID
     * @param q       optional search text (draft name contains), ignored if blank
     * @return number of matching drafts
     */
    public int countByOwner(String ownerId, String q) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        session.setDefaultReadOnly(true);
        try {
            String jq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
            log.debug("countByOwner: ownerId={} qPresent={}", ownerId, jq != null);
            final String hql = (jq == null)
                    ? """
                     select count(d.id)
                     from DraftRow d
                     where d.ownerId = :uid
                    """
                    : """
                     select count(d.id)
                     from DraftRow d
                     where d.ownerId = :uid
                       and lower(d.name) like :pat
                    """;
            Query<Long> st = session.createQuery(hql, Long.class)
                    .setParameter("uid", ownerId);
            if (jq != null) {
                st.setParameter("pat", "%" + jq + "%");
            }
            Long cnt = st.uniqueResult();
            tx.commit();
            int result = (cnt == null ? 0 : cnt.intValue());
            log.debug("countByOwner: result={}", result);
            return result;
        } catch (RuntimeException e) {
            log.error("countByOwner: failed ownerId={} q='{}'", ownerId, q, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Retrieves a page of drafts for an owner ordered by {@code updatedAt} (descending),
     * optionally filtered by a case-insensitive name substring.
     *
     * @param ownerId owner user ID
     * @param page    page number (1-based; clamped to ≥1)
     * @param size    page size (clamped to ≥1)
     * @param q       optional search text; ignored if blank
     * @return immutable list of drafts for the page
     */
    public List<DraftRow> findByOwnerPaged(String ownerId, int page, int size, String q) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        session.setDefaultReadOnly(true);
        try {
            int safePage = Math.max(1, page);
            int safeSize = Math.max(1, size);
            int first = (safePage - 1) * safeSize;
            String jq = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
            log.debug("findByOwnerPaged: ownerId={} page={} size={} qPresent={}", ownerId, safePage, safeSize, jq != null);
            final String hql = (jq == null)
                    ? """
                     select d
                     from DraftRow d
                     where d.ownerId = :uid
                     order by d.updatedAt desc
                    """
                    : """
                     select d
                     from DraftRow d
                     where d.ownerId = :uid
                       and lower(d.name) like :pat
                     order by d.updatedAt desc
                    """;
            Query<DraftRow> st = session.createQuery(hql, DraftRow.class)
                    .setParameter("uid", ownerId)
                    .setFirstResult(first)
                    .setMaxResults(safeSize);
            if (jq != null) {
                st.setParameter("pat", "%" + jq + "%");
            }
            List<DraftRow> list = st.getResultList();
            tx.commit();
            log.debug("findByOwnerPaged: result size={}", list.size());
            return List.copyOf(list);
        } catch (RuntimeException e) {
            log.error("findByOwnerPaged: failed ownerId={} page={} size={} q='{}'", ownerId, page, size, q, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Creates a minimal empty draft for the given owner/target with a generated ID.
     *
     * <p>Initializes an empty graph ({@code []}), startId = {@code 0},
     * and an empty version note.</p>
     *
     * @param ownerId       owner user ID
     * @param targetQuestId optional quest ID this draft targets; {@code null} for new quest
     * @param name          draft name; defaults to "Untitled Draft" if blank
     */
    public void createEmpty(String ownerId, String targetQuestId, String name) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("createEmpty: ownerId={} targetQuestId={} name='{}'", ownerId, targetQuestId, name);
            DraftRow row = new DraftRow();
            String id = UUID.randomUUID().toString();
            row.setDraftId(id);
            row.setOwnerId(ownerId);
            row.setTargetQuestId(targetQuestId);
            row.setName((name == null || name.isBlank()) ? "Untitled Draft" : name.trim());
            if (row.getName().length() >= 50) {
                throw new IllegalArgumentException("Name too long, maximum length is 50 characters");
            }
            row.setStartId(0);
            row.setNodesJson("[]");
            row.setVersionNote("");
            row.setUpdatedAt(Instant.now());
            session.persist(row);
            tx.commit();
            log.info("createEmpty: success draftId={} ownerId={}", id, ownerId);
        } catch (RuntimeException e) {
            log.error("createEmpty: failed ownerId={} targetQuestId={}", ownerId, targetQuestId, e);
            tx.rollback();
            throw e;
        }
    }

    /**
     * Renames a draft, if it exists and belongs to the specified owner.
     *
     * @param draftId draft identifier
     * @param newName new draft name (must be non-null; trimmed)
     * @param ownerId expected owner user ID
     */
    public void rename(String draftId, String newName, String ownerId) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            log.info("rename: draftId={} ownerId={} newName='{}'", draftId, ownerId, newName);
            DraftRow row = session.get(DraftRow.class, draftId);
            if (row != null && row.getOwnerId().equals(ownerId)) {
                row.setName(Objects.requireNonNull(newName, "name").trim());
                row.setUpdatedAt(Instant.now());
                log.debug("rename: success draftId={} ownerId={}", draftId, ownerId);
            } else {
                log.warn("rename: denied or not found draftId={} ownerId={}", draftId, ownerId);
            }
            tx.commit();
        } catch (RuntimeException e) {
            log.error("rename: failed draftId={} ownerId={}", draftId, ownerId, e);
            tx.rollback();
            throw e;
        }
    }
}
