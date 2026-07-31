package com.asg.common.services.service.impl;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.services.dto.IsoPolicyAccessRequestDto;
import com.asg.common.services.dto.IsoPolicyAccessResponseDto;
import com.asg.common.services.dto.IsoPolicyAttachmentDto;
import com.asg.common.services.dto.IsoPolicyCategoryFolderDto;
import com.asg.common.services.dto.IsoPolicyDocumentDto;
import com.asg.common.services.enums.IsoPolicyLogType;
import com.asg.common.services.service.IsoPolicyAccessService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Employee-facing side of ISO Documents and Policies (asg-admin-iso, DocId 700-101).
 * <p>
 * The Access Log is an <b>append-only event log</b>: every open inserts an {@code Accessed} row and
 * every acknowledgement inserts an {@code Acknowledged} row, keyed on the file's ATTACHMENT_ID
 * ({@code GLOBAL_ATTACHMENTS.SEQNO}) — never on its name, which is neither unique nor stable.
 * Nothing is updated in place, so the full history of who read what, and when, survives. Current
 * state ("has this employee acknowledged the current version of this file?") is an aggregate over
 * those rows, computed in {@link #SQL_ATTACHMENTS}.
 * <p>
 * The ATTACHMENT_ID alone does not identify a file for all time — SEQNO is assigned MAX+1 per
 * document over a hard-deleted table, so ids are recycled — and every match on it is therefore
 * qualified by the attachment's CREATED_DATE. See {@link #SQL_ATTACHMENTS}.
 * <p>
 * The tables involved — ADMIN_ISO_COMP_POLICY_* , HR_EMPLOYEE_MASTER and GLOBAL_ATTACHMENTS — are
 * owned by other services, so this deliberately maps no JPA entities for them: mirroring another
 * service's table as an entity means two definitions of one table that can silently drift apart.
 */
@Service
@Slf4j
public class IsoPolicyAccessServiceImpl implements IsoPolicyAccessService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * DOC_ID this screen's attachments were filed under in GLOBAL_ATTACHMENTS. The SRS gives the
     * transaction as 700-101; overridable because a wrong value here returns no files rather than
     * an error.
     */
    @Value("${iso.policy.attachment-doc-id:700-101}")
    private String attachmentDocId;

    private static final String RESOURCE = "ISO Document / Policy";
    private static final String ATTACHMENT = "Attachment";
    private static final String YES = "Y";
    private static final String UNCATEGORISED = "Uncategorised";

    /**
     * Who may see a document, as one predicate over {@code h} — bound to :employeePoid and
     * :departmentPoid, plus the tenant.
     * <p>
     * Shared by the read path ({@link #SQL_MY_DOCUMENTS}) and the write path
     * ({@link #SQL_VISIBLE_HEADER}) so the two cannot drift: a document an employee may not see is
     * also a document they may not log an access or an acknowledgement against.
     * <p>
     * The tenant columns are not optional. Without them a document published to 'All Employees' by
     * <em>any</em> company would be visible to <em>every</em> employee in the schema.
     * <p>
     * The three PUBLISH_OPTION literals are the values documented in the column comment — if the
     * PUBLISH LOV stores codes rather than labels, these are what change.
     */
    private static final String VISIBLE_TO_EMPLOYEE = """
                   h.GROUP_POID   = :groupPoid
               AND h.COMPANY_POID = :companyPoid
               AND NVL(h.DELETED, 'N') <> 'Y'
               AND UPPER(h.PUBLISH_TO_EMPLOYEES) = 'Y'
               AND (
                     UPPER(h.PUBLISH_OPTION) = 'ALL EMPLOYEES'
                  OR (UPPER(h.PUBLISH_OPTION) = 'SELECTED EMPLOYEES'
                      AND EXISTS (SELECT 1
                                    FROM ADMIN_ISO_COMP_POLICY_EMP_DTL e
                                   WHERE e.TRANSACTION_POID = h.TRANSACTION_POID
                                     AND e.EMPLOYEE_POID    = :employeePoid))
                  OR (UPPER(h.PUBLISH_OPTION) = 'SELECTED DEPARTMENT'
                      AND EXISTS (SELECT 1
                                    FROM ADMIN_ISO_COMP_POLICY_DEP_DTL d
                                   WHERE d.TRANSACTION_POID = h.TRANSACTION_POID
                                     AND d.DEPT_POID        = :departmentPoid))
                   )
            """;

    /**
     * Every published ISO document visible to the logged-in employee, newest first within a category.
     * <p>
     * The Access Log is not joined here: it holds many rows per file, so joining it at document level
     * would multiply each document by its file count. Acknowledgement state is assembled from the
     * attachments instead.
     */
    private static final String SQL_MY_DOCUMENTS = """
            SELECT h.TRANSACTION_POID, h.DOC_REF, h.DOC_NAME, h.DOC_TYPE, h.CATEGORY, h.DESCRIPTION,
                   h.VERSION_NO, h.EXPIRY_DATE, h.CREATED_DATE, h.ACKNOWLEDGEMENT
              FROM ADMIN_ISO_COMP_POLICY_HDR h
             WHERE (:includeExpired = 1
                    OR h.EXPIRY_DATE IS NULL
                    OR h.EXPIRY_DATE >= TRUNC(SYSDATE))
               AND
            """ + VISIBLE_TO_EMPLOYEE + """
             ORDER BY h.CATEGORY, h.CREATED_DATE DESC
            """;

    /**
     * The document the employee is about to log an event against, if they are allowed to see it at
     * all. Returns the fields the event needs — whether acknowledgement is required, and the version
     * to stamp on the row.
     * <p>
     * Empty result means: no such document, or deleted, or another company's, or not published to
     * this employee. All four are a 404 — the caller learns nothing about documents they cannot see.
     */
    private static final String SQL_VISIBLE_HEADER = """
            SELECT NVL(h.ACKNOWLEDGEMENT, 'N'), h.VERSION_NO
              FROM ADMIN_ISO_COMP_POLICY_HDR h
             WHERE h.TRANSACTION_POID = :transactionPoid
               AND
            """ + VISIBLE_TO_EMPLOYEE;

    /**
     * The files for a set of documents, each left-joined to this employee's aggregated event history
     * for that file — one batched query rather than one per document.
     * <p>
     * The {@code hist} subquery collapses the append-only log back to current state per file: last
     * event time, how many times it was opened, and the time and document version of the most recent
     * acknowledgement. {@code KEEP (DENSE_RANK LAST ORDER BY ...)} picks the DOC_VERSION of the
     * latest Acknowledged row — a plain MAX would compare version strings, and 'V10.0' sorts before
     * 'V2.0'. The CASE inside the aggregate matters as much as the one in the ORDER BY: without it,
     * a file with only Accessed rows has every row tied at the last rank and reports the version off
     * an <em>access</em> row as if it had been acknowledged.
     * <p>
     * The join key is ATTACHMENT_ID (= GLOBAL_ATTACHMENTS.SEQNO), never the file name. Names are
     * neither unique nor stable: replacing Policy.pdf with a corrected Policy.pdf would otherwise
     * carry the old acknowledgement onto the new file.
     * <p>
     * The id is not stable either, so it is qualified by CREATED_DATE — which is why the attachments
     * are joined inside the CTE rather than only outside it: the filter has to apply before the
     * aggregation. GLOBAL_ATTACHMENTS is hard-deleted (PROC_ATTACHMENTS_DELETE) and SEQNO is assigned
     * MAX+1 per document, so deleting the last file and uploading another hands out the same id. An
     * event can only be logged while its file is live, so any log row older than the attachment row's
     * CREATED_DATE belongs to a previous occupant of that id. Without the check the new file inherits
     * the dead one's acknowledgements — and the employee is then locked out of acknowledging it for
     * real, because SQL_ACK_EXISTS matches the stale row.
     */
    private static final String SQL_ATTACHMENTS = """
            WITH hist AS (
                SELECT a.DOC_KEY_POID AS TRANSACTION_POID,
                       a.SEQNO        AS ATTACHMENT_ID,
                       MAX(l.CREATED_DATE) AS LAST_EVENT_TIME,
                       COUNT(CASE WHEN UPPER(l.LOG_TYPE) = 'ACCESSED' THEN 1 END) AS ACCESS_COUNT,
                       MAX(CASE WHEN UPPER(l.LOG_TYPE) = 'ACKNOWLEDGED' THEN l.CREATED_DATE END) AS ACK_TIME,
                       MAX(CASE WHEN UPPER(l.LOG_TYPE) = 'ACKNOWLEDGED' THEN l.DOC_VERSION END)
                           KEEP (DENSE_RANK LAST ORDER BY
                                 CASE WHEN UPPER(l.LOG_TYPE) = 'ACKNOWLEDGED' THEN l.CREATED_DATE END
                                 NULLS FIRST) AS ACK_VERSION
                  FROM GLOBAL_ATTACHMENTS a
                  JOIN ADMIN_ISO_COMP_POLICY_LOG_DTL l
                         ON l.TRANSACTION_POID = a.DOC_KEY_POID
                        AND l.ATTACHMENT_ID    = a.SEQNO
                        AND l.CREATED_DATE    >= a.CREATED_DATE
                 WHERE l.EMPLOYEE_POID = :employeePoid
                   AND a.DOC_ID        = :docId
                   AND a.DOC_KEY_POID IN (:docKeyPoids)
                 GROUP BY a.DOC_KEY_POID, a.SEQNO
            )
            SELECT a.DOC_KEY_POID, a.SEQNO, a.FILE_NAME, a.FILE_NAME_MAPPED, a.FILE_REMARKS,
                   a.CHECKLIST_NAME, a.CREATED_BY, a.CREATED_DATE,
                   hist.LAST_EVENT_TIME, hist.ACCESS_COUNT, hist.ACK_TIME, hist.ACK_VERSION
              FROM GLOBAL_ATTACHMENTS a
              LEFT JOIN hist
                     ON hist.TRANSACTION_POID = a.DOC_KEY_POID
                    AND hist.ATTACHMENT_ID    = a.SEQNO
             WHERE a.DOC_ID       = :docId
               AND a.DOC_KEY_POID IN (:docKeyPoids)
               AND NVL(a.DELETED, 'N') <> 'Y'
               AND NVL(a.ACTIVE, 'Y')  <> 'N'
             ORDER BY a.DOC_KEY_POID, a.SEQNO
            """;

    /**
     * Resolves the caller straight from the token's userPoid — the employee is never taken from the
     * request, so a caller can only ever read their own documents and write their own log rows.
     * Scoped to the token's tenant as well: a login is only meaningful within its own company.
     */
    private static final String SQL_EMPLOYEE = """
            SELECT EMPLOYEE_POID, DEPARTMENT_POID
              FROM HR_EMPLOYEE_MASTER
             WHERE LOGIN_USER_POID = :loginUserPoid
               AND GROUP_POID      = :groupPoid
               AND COMPANY_POID    = :companyPoid
               AND NVL(DELETED, 'N')      <> 'Y'
               AND NVL(DISCONTINUED, 'N') <> 'Y'
            """;

    /**
     * Confirms the attachment is a live file <b>on this document</b>, and returns the name to
     * snapshot onto the event plus the upload timestamp that identifies this particular file behind
     * a recycled SEQNO. Without the DOC_KEY_POID check a caller could log an access against any file
     * in the system by passing its id.
     */
    private static final String SQL_ATTACHMENT = """
            SELECT FILE_NAME, CREATED_DATE
              FROM GLOBAL_ATTACHMENTS
             WHERE DOC_ID       = :docId
               AND DOC_KEY_POID = :transactionPoid
               AND SEQNO        = :attachmentId
               AND NVL(DELETED, 'N') <> 'Y'
               AND NVL(ACTIVE, 'Y')  <> 'N'
            """;

    /**
     * Whether this employee already has an {@code Acknowledged} row for <b>this</b> file at this
     * exact version. Guards against a second acknowledgement of the same version — {@code
     * DOC_VERSION} is part of the match so a new version still lets a fresh acknowledgement through.
     * <p>
     * {@code CREATED_DATE >= :attachmentUploadedOn} keeps the guard from firing on a previous
     * occupant of a recycled SEQNO — see {@link #SQL_ATTACHMENTS}. Without it, replacing a file
     * leaves the dead file's acknowledgement blocking the live one.
     */
    private static final String SQL_ACK_EXISTS = """
            SELECT 1
              FROM ADMIN_ISO_COMP_POLICY_LOG_DTL
             WHERE TRANSACTION_POID = :transactionPoid
               AND EMPLOYEE_POID    = :employeePoid
               AND ATTACHMENT_ID    = :attachmentId
               AND UPPER(LOG_TYPE)  = 'ACKNOWLEDGED'
               AND DOC_VERSION      = :docVersion
               AND CREATED_DATE    >= :attachmentUploadedOn
             FETCH FIRST 1 ROWS ONLY
            """;

    /**
     * Appends one event. DET_ROW_ID comes from a sequence, not MAX+1: this table is written by every
     * employee opening every file, and two concurrent opens computing MAX+1 would collide on the
     * primary key. The values need not be contiguous — it is a log.
     */
    private static final String SQL_INSERT_EVENT = """
            INSERT INTO ADMIN_ISO_COMP_POLICY_LOG_DTL
                   (TRANSACTION_POID, DET_ROW_ID, EMPLOYEE_POID,
                    ATTACHMENT_ID, ATTACHMENT_NAME, LOG_TYPE, DOC_VERSION, REMARKS,
                    CREATED_BY, CREATED_DATE, LASTMODIFIED_BY, LASTMODIFIED_DATE)
            VALUES (:transactionPoid, ADMIN_ISO_COMP_POLICY_LOG_SEQ.NEXTVAL, :employeePoid,
                    :attachmentId, :attachmentName, :logType, :docVersion, :remarks,
                    :actor, SYSTIMESTAMP, :actor, SYSTIMESTAMP)
            """;

    /** Reads back the event just written — DET_ROW_ID is monotonic, so the newest row is this one. */
    private static final String SQL_LAST_EVENT = """
            SELECT DET_ROW_ID, CREATED_DATE
              FROM ADMIN_ISO_COMP_POLICY_LOG_DTL
             WHERE TRANSACTION_POID = :transactionPoid
               AND EMPLOYEE_POID    = :employeePoid
               AND ATTACHMENT_ID    = :attachmentId
             ORDER BY DET_ROW_ID DESC
             FETCH FIRST 1 ROWS ONLY
            """;

    /** The logged-in caller, resolved server-side. Never built from anything in the request. */
    private record Employee(Long employeePoid, Long departmentPoid) {
    }

    /**
     * A live file on the document being logged against. {@code uploadedOn} is
     * {@code GLOBAL_ATTACHMENTS.CREATED_DATE} — set by PROC_ATTACHMENTS_INSERT to SYSTIMESTAMP on
     * every insert and never updated afterwards, so it distinguishes this file from an earlier one
     * that held the same recycled SEQNO.
     */
    private record LiveAttachment(String fileName, Timestamp uploadedOn) {
    }

    /**
     * The Home page Documents widget: category folders, each holding every file from the documents in
     * that category. The tree is two levels — category (folder) -> attachment (file); the documents
     * are collapsed away, their files lifted directly under the category.
     * <p>
     * Each file carries this employee's own access and acknowledgement state, aggregated from the
     * event log. Expired documents are excluded unless {@code includeExpired}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<IsoPolicyCategoryFolderDto> getMyDocuments(boolean includeExpired) {
        // A login with no employee record — an admin, say — simply owns no documents. The Home
        // widget renders empty rather than erroring.
        Optional<Employee> caller = findEmployee();
        if (caller.isEmpty()) {
            return List.of();
        }
        Employee me = caller.get();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = bindVisibility(entityManager.createNativeQuery(SQL_MY_DOCUMENTS), me)
                .setParameter("includeExpired", includeExpired ? 1 : 0)
                .getResultList();

        List<IsoPolicyDocumentDto> documents = rows.stream().map(this::toDocumentDto).toList();
        attachFiles(documents, me);

        // Lift every document's files up under its category. The document query is already ordered by
        // CATEGORY then CREATED_DATE DESC, so a LinkedHashMap keeps the folders and their files in that
        // order. Per-file acknowledgement state was set in toAttachmentDto against the file's own
        // document version, so it survives the flattening.
        Map<String, List<IsoPolicyAttachmentDto>> byCategory = new LinkedHashMap<>();
        for (IsoPolicyDocumentDto document : documents) {
            String category = document.getCategory() == null ? UNCATEGORISED : document.getCategory();
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).addAll(document.getAttachments());
        }

        return byCategory.entrySet().stream().map(entry -> {
            List<IsoPolicyAttachmentDto> files = entry.getValue();
            IsoPolicyCategoryFolderDto folder = new IsoPolicyCategoryFolderDto();
            folder.setCategoryCode(entry.getKey());
            folder.setCategory(entry.getKey());
            folder.setAttachments(files);
            folder.setAttachmentCount(files.size());
            folder.setAcknowledgementPendingCount(
                    (int) files.stream().filter(IsoPolicyAttachmentDto::isAcknowledgementPending).count());
            return folder;
        }).toList();
    }

    @Override
    @Transactional
    public IsoPolicyAccessResponseDto recordAccess(Long transactionPoid, IsoPolicyAccessRequestDto request) {
        Employee me = findEmployee()
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "loginUserPoid",
                        UserContext.getUserPoid()));

        Long attachmentId = request.getAttachmentId();
        IsoPolicyLogType logType = request.getLogType();

        // Enforces the same visibility rules as my-documents: an employee cannot log an access, and
        // above all cannot acknowledge, against a document that was never published to them — those
        // rows are what an ISO auditor acts on.
        Object[] header = visibleHeader(transactionPoid, me);
        String acknowledgementRequired = toStr(header[0]);
        String docVersion = toStr(header[1]);

        // Proves the file is live and belongs to this document, and gives us the name to snapshot.
        // Runs before the acknowledgement gates because they need its upload timestamp to tell this
        // file apart from a previous occupant of the same recycled SEQNO.
        LiveAttachment file = liveAttachment(transactionPoid, attachmentId);

        if (logType == IsoPolicyLogType.Acknowledged) {
            if (!YES.equalsIgnoreCase(acknowledgementRequired)) {
                // IllegalArgumentException, not the lib's ValidationException: this service's
                // GlobalExceptionHandler maps jakarta.xml.bind.ValidationException, so the lib one
                // would fall through to the 500 handler instead of returning 400.
                throw new IllegalArgumentException("This document does not require acknowledgement");
            }
            // A file is acknowledged once per version. A second acknowledgement of the same version is
            // rejected rather than appended, so the log cannot show one employee acknowledging one file
            // twice at the same version. The version qualifier is deliberate: after the admin publishes
            // a new version the employee owes a fresh acknowledgement, and that one must go through.
            if (alreadyAcknowledged(transactionPoid, me.employeePoid(), attachmentId, docVersion,
                    file.uploadedOn())) {
                throw new IllegalArgumentException("This file has already been acknowledged at version "
                        + docVersion);
            }
        }

        String attachmentName = file.fileName();

        entityManager.createNativeQuery(SQL_INSERT_EVENT)
                .setParameter("transactionPoid", transactionPoid)
                .setParameter("employeePoid", me.employeePoid())
                .setParameter("attachmentId", attachmentId)
                .setParameter("attachmentName", attachmentName)
                .setParameter("logType", logType.name())
                .setParameter("docVersion", docVersion)
                .setParameter("remarks", request.getRemarks())
                .setParameter("actor", UserContext.getUserId())
                .executeUpdate();

        log.info("ISO policy log event transactionPoid={} employeePoid={} attachmentId={} logType={}",
                transactionPoid, me.employeePoid(), attachmentId, logType);

        Object[] written = (Object[]) entityManager.createNativeQuery(SQL_LAST_EVENT)
                .setParameter("transactionPoid", transactionPoid)
                .setParameter("employeePoid", me.employeePoid())
                .setParameter("attachmentId", attachmentId)
                .getSingleResult();

        IsoPolicyAccessResponseDto dto = new IsoPolicyAccessResponseDto();
        dto.setTransactionPoid(transactionPoid);
        dto.setEmployeePoid(me.employeePoid());
        dto.setAttachmentId(attachmentId);
        dto.setAttachmentName(attachmentName);
        dto.setLogType(logType);
        dto.setDocVersion(docVersion);
        dto.setRemarks(request.getRemarks());
        dto.setDetRowId(toLong(written[0]));
        dto.setLoggedOn(toDateTime(written[1]));
        return dto;
    }

    /**
     * Hangs each document's files off it, with this employee's aggregated history per file. One
     * batched query for every document rather than one query per document.
     */
    private void attachFiles(List<IsoPolicyDocumentDto> documents, Employee me) {
        if (documents.isEmpty()) {
            return; // Oracle rejects an empty IN list.
        }

        List<Long> docKeyPoids = documents.stream().map(IsoPolicyDocumentDto::getTransactionPoid).toList();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SQL_ATTACHMENTS)
                .setParameter("employeePoid", me.employeePoid())
                .setParameter("docId", attachmentDocId)
                .setParameter("docKeyPoids", docKeyPoids)
                .getResultList();

        Map<Long, List<Object[]>> byDocument = rows.stream()
                .collect(Collectors.groupingBy(row -> toLong(row[0])));

        documents.forEach(document -> {
            List<IsoPolicyAttachmentDto> files =
                    byDocument.getOrDefault(document.getTransactionPoid(), List.of()).stream()
                            .map(row -> toAttachmentDto(row, document))
                            .toList();
            document.setAttachments(files);
            document.setAttachmentCount(files.size());
        });
    }

    /**
     * Resolves the logged-in application user to their employee within the token's tenant, so an
     * event is always recorded against the caller and never against a caller-supplied employee id.
     */
    private Optional<Employee> findEmployee() {
        Long userPoid = UserContext.getUserPoid();
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        if (userPoid == null || groupPoid == null || companyPoid == null) {
            // Fail loud. Left to run, a null tenant silently matches no rows, and the employee sees
            // an empty document list rather than an error.
            throw new IllegalArgumentException("Authenticated user has no user/company/group context");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> matches = entityManager.createNativeQuery(SQL_EMPLOYEE)
                .setParameter("loginUserPoid", userPoid)
                .setParameter("groupPoid", groupPoid)
                .setParameter("companyPoid", companyPoid)
                .getResultList();

        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            // LOGIN_USER_POID has no unique constraint, so this is data the schema permits. Picking
            // one arbitrarily would attribute the acknowledgement to the wrong person.
            throw new IllegalArgumentException(
                    "More than one employee is linked to this login (LOGIN_USER_POID = " + userPoid + ")");
        }
        Object[] row = matches.get(0);
        return Optional.of(new Employee(toLong(row[0]), toLong(row[1])));
    }

    /** Binds the tenant and the caller that {@link #VISIBLE_TO_EMPLOYEE} reads. */
    private Query bindVisibility(Query query, Employee me) {
        return query
                .setParameter("groupPoid", UserContext.getGroupPoid())
                .setParameter("companyPoid", UserContext.getCompanyPoid())
                .setParameter("employeePoid", me.employeePoid())
                .setParameter("departmentPoid", me.departmentPoid());
    }

    private Object[] visibleHeader(Long transactionPoid, Employee me) {
        List<?> rows = bindVisibility(entityManager.createNativeQuery(SQL_VISIBLE_HEADER), me)
                .setParameter("transactionPoid", transactionPoid)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(RESOURCE, "transactionPoid", transactionPoid);
        }
        return (Object[]) rows.get(0);
    }

    private LiveAttachment liveAttachment(Long transactionPoid, Long attachmentId) {
        List<?> rows = entityManager.createNativeQuery(SQL_ATTACHMENT)
                .setParameter("docId", attachmentDocId)
                .setParameter("transactionPoid", transactionPoid)
                .setParameter("attachmentId", attachmentId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(ATTACHMENT, "attachmentId", attachmentId);
        }
        // Two columns, so Hibernate hands back an Object[] — a single-column select would have been
        // a bare scalar.
        Object[] row = (Object[]) rows.get(0);
        return new LiveAttachment(toStr(row[0]), (Timestamp) row[1]);
    }

    private boolean alreadyAcknowledged(Long transactionPoid, Long employeePoid, Long attachmentId,
                                        String docVersion, Timestamp attachmentUploadedOn) {
        return !entityManager.createNativeQuery(SQL_ACK_EXISTS)
                .setParameter("transactionPoid", transactionPoid)
                .setParameter("employeePoid", employeePoid)
                .setParameter("attachmentId", attachmentId)
                .setParameter("docVersion", docVersion)
                .setParameter("attachmentUploadedOn", attachmentUploadedOn)
                .getResultList()
                .isEmpty();
    }

    private IsoPolicyDocumentDto toDocumentDto(Object[] row) {
        IsoPolicyDocumentDto dto = new IsoPolicyDocumentDto();
        dto.setTransactionPoid(toLong(row[0]));
        dto.setDocRef(toStr(row[1]));
        dto.setDocName(toStr(row[2]));
        dto.setDocType(toStr(row[3]));
        dto.setCategory(toStr(row[4]));
        dto.setDescription(toStr(row[5]));
        dto.setVersionNo(toStr(row[6]));
        dto.setExpiryDate(toDate(row[7]));
        dto.setPublishedOn(toDateTime(row[8]));
        dto.setAcknowledgementRequired(YES.equalsIgnoreCase(toStr(row[9])));
        return dto;
    }

    private IsoPolicyAttachmentDto toAttachmentDto(Object[] row, IsoPolicyDocumentDto document) {
        IsoPolicyAttachmentDto dto = new IsoPolicyAttachmentDto();
        // The document is collapsed away by the category grouping, so its key rides on the file:
        // without it the client has no transactionPoid to POST the access / acknowledgement against.
        dto.setTransactionPoid(document.getTransactionPoid());
        dto.setAttachmentId(toLong(row[1]));
        dto.setFileName(toStr(row[2]));
        dto.setStoredFileName(toStr(row[3]));
        dto.setFileRemarks(toStr(row[4]));
        dto.setChecklistName(toStr(row[5]));
        dto.setUploadedBy(toStr(row[6]));
        dto.setUploadedOn(toDateTime(row[7]));
        dto.setDownloadPath(String.format("/v1/attachments/%s/%d/%s/download",
                attachmentDocId, toLong(row[0]), dto.getStoredFileName()));

        dto.setLastAccessedTime(toDateTime(row[8]));
        Long accessCount = toLong(row[9]);
        dto.setAccessCount(accessCount == null ? 0L : accessCount);
        dto.setAcknowledgedTime(toDateTime(row[10]));
        String acknowledgedVersion = toStr(row[11]);
        dto.setAcknowledgedVersion(acknowledgedVersion);

        boolean acknowledged = dto.getAcknowledgedTime() != null;
        dto.setAcknowledged(acknowledged);

        // Pending when never acknowledged, or acknowledged against an older version than the one now
        // published — a revision makes the previous acknowledgement stale rather than current.
        boolean staleVersion = acknowledged && !Objects.equals(acknowledgedVersion, document.getVersionNo());
        dto.setAcknowledgementPending(
                document.isAcknowledgementRequired() && (!acknowledged || staleVersion));
        return dto;
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    /**
     * Reads a text column out of an {@code Object[]} row.
     * <p>
     * Not a plain {@code (String)} cast: Hibernate hands a single-character column back as a
     * {@link Character}, not a String, so casting blows up with a ClassCastException on every Y/N
     * flag here — ACKNOWLEDGEMENT, and anything else declared VARCHAR2(1).
     */
    private String toStr(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof String s ? s : value.toString();
    }

    private LocalDateTime toDateTime(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) value;
    }

    /** EXPIRY_DATE is an Oracle DATE, which the driver hands back as a Timestamp. */
    private LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return (LocalDate) value;
    }
}
