package com.asg.common.services.repository;

import com.asg.common.services.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long>, AttachmentCustomRepository {

    // -------------------- INSERT --------------------
    @Procedure(procedureName = "PROC_ATTACHMENTS_INSERT")
    void insertAttachment(
            @Param("P_GROUP_POID") Long groupPoid,
            @Param("P_COMPANY_POID") Long companyPoid,
            @Param("P_DOC_ID") String docId,
            @Param("P_DOC_KEY_POID") Long docKeyPoid,
            @Param("P_SEQNO") Long seqNo,
            @Param("P_FILE_NAME") String fileName,
            @Param("P_FILE_REMARKS") String remarks,
            @Param("P_CHECKLIST_NAME") String checklistName,
            @Param("P_CREATED_BY") String createdBy,
            @Param("P_FILE_NAME_MAPPED") String fileNameMapped,
            @Param("P_JOB_POID") Long jobPoid
    );

    // -------------------- UPDATE REMARKS / CHECKLIST --------------------
    @Procedure(procedureName = "PROC_ATTACHMENTS_UPDATE")
    void updateAttachment(
            @Param("P_GROUP_POID") Long groupPoid,
            @Param("P_COMPANY_POID") Long companyPoid,
            @Param("P_DOC_ID") String docId,
            @Param("P_DOC_KEY_POID") Long docKeyPoid,
            @Param("P_SEQNO") Long seqNo,
            @Param("P_FILE_NAME") String fileName,
            @Param("P_FILE_REMARKS") String remarks,
            @Param("P_CHECKLIST_NAME") String checklistName,
            @Param("P_USER_POID") Long updatedBy,
            @Param("P_FILE_NAME_MAPPED") String fileNameMapped
    );

    // -------------------- DELETE --------------------
    @Procedure(procedureName = "PROC_ATTACHMENTS_DELETE")
    void deleteAttachment(
            @Param("P_GROUP_POID") Long groupPoid,
            @Param("P_COMPANY_POID") Long companyPoid,
            @Param("P_DOC_ID") String docId,
            @Param("P_DOC_KEY_POID") Long docKeyPoid,
            @Param("P_FILE_NAME_MAPPED") String fileNameMapped
    );

    // -------------------- ARCHIVE --------------------
    @Procedure(procedureName = "PROC_ATTACHMENTS_ARCHIVE")
    void archiveAttachment(
            @Param("P_GROUP_POID") Long groupPoid,
            @Param("P_COMPANY_POID") Long companyPoid,
            @Param("P_DOC_ID") String docId,
            @Param("P_DOC_KEY_POID") Long docKeyPoid,
            @Param("P_FILE_NAME_MAPPED") String fileNameMapped
    );

    // -------------------- ACTIVATE --------------------
    @Query("UPDATE Attachment a SET a.active = 'Y' WHERE a.docId = :docId AND a.docKeyPoid = :docKeyPoid AND a.fileNameMapped = :fileNameMapped")
    @org.springframework.data.jpa.repository.Modifying
    void activateAttachment(@Param("docId") String docId, @Param("docKeyPoid") Long docKeyPoid, @Param("fileNameMapped") String fileNameMapped);

    // -------------------- CHECKLIST VALUES --------------------
    @Query(value = "SELECT ATTACHMENT_CHECKLIST FROM GLOBAL_DOC_MASTER WHERE DOC_ID = :docId AND ACTIVE = 'Y' AND (DELETED = 'N' OR DELETED IS NULL)", nativeQuery = true)
    String getAttachmentChecklistRaw(@Param("docId") String docId);

    @Query(value = "SELECT DOC_SHORT_NAME FROM GLOBAL_DOC_MASTER WHERE DOC_ID = :docId AND ACTIVE = 'Y' AND (DELETED = 'N' OR DELETED IS NULL)", nativeQuery = true)
    String getDocShortName(@Param("docId") String docId);

    default List<String> getChecklistFromGlobalDocMaster(String docId) {
        String checklistStr = getAttachmentChecklistRaw(docId);
        if (checklistStr == null || checklistStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(checklistStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Query(value = "SELECT VOYAGE_TRANSACTION_POID FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = :blPoid AND ROWNUM = 1", nativeQuery = true)
    Long findVoyagePoidByBlPoid(@Param("blPoid") Long blPoid);

    @Query("SELECT a FROM Attachment a WHERE a.docId = :docId AND a.docKeyPoid = :docKeyPoid AND a.fileNameMapped = :fileNameMapped AND (a.active IS NULL OR a.active = 'Y') AND (a.deleted IS NULL OR a.deleted = 'N')")
    Optional<Attachment> findByDocIdAndDocKeyPoidAndFileNameMapped(@Param("docId") String docId, @Param("docKeyPoid") Long docKeyPoid, @Param("fileNameMapped") String fileNameMapped);

    @Query("SELECT a FROM Attachment a WHERE a.docId = :docId AND a.docKeyPoid = :docKeyPoid AND a.fileNameMapped = :fileNameMapped AND (a.deleted IS NULL OR a.deleted = 'N')")
    Optional<Attachment> findByDocIdAndDocKeyPoidAndFileNameMappedForArchive(@Param("docId") String docId, @Param("docKeyPoid") Long docKeyPoid, @Param("fileNameMapped") String fileNameMapped);
}
