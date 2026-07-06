package com.asg.common.services.service;

import com.asg.common.lib.enums.AttachmentFilterType;
import com.asg.common.services.dto.AttachmentDto;
import com.asg.common.services.dto.AttachmentUploadDto;
import com.asg.common.services.dto.UpdateRemarksRequest;
import com.asg.common.services.dto.UploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    // -------------------- UPLOAD --------------------
    UploadResponse uploadFiles(String docId, Long docKeyPoid, MultipartFile[] files,
                               String remarks, String checklistName, Long createdBy,
                               boolean attachEDI, Long attachmentEDIJobPoid,
                               String docShortName, String docRef);

    // -------------------- LIST --------------------
    Page<AttachmentDto> getAttachments(String docId, Long docKeyPoid, boolean includeArchived, Pageable pageable);

    List<AttachmentDto> getActiveAttachments(String docId, Long docKeyPoid);

    Page<AttachmentDto> getActiveAttachments(String docId, Long docKeyPoid, Pageable pageable);

    // -------------------- ENHANCED LIST WITH FILTER --------------------
    Page<AttachmentDto> getAttachmentsByFilter(String docId, Long docKeyPoid, AttachmentFilterType filterType, Pageable pageable);

    List<AttachmentDto> getDeletedAttachments(String docId, Long docKeyPoid);

    // -------------------- DELETE & ARCHIVE --------------------
    void deleteAttachment(String docId, Long docKeyPoid, String fileNameMapped);

    void deleteAllAttachments(String docId, Long docKeyPoid);

    String archiveAttachment(String docId, Long docKeyPoid, String fileNameMapped, Long userPoid);

    void activateAttachment(String docId, Long docKeyPoid, String fileNameMapped, Long userPoid);

    // -------------------- BULK UPDATE --------------------
    void updateRemarksAndChecklist(String docId, List<UpdateRemarksRequest> updates);

    // -------------------- CHECKLIST --------------------
    // List<String> getChecklist(String docId); // Not used - both controller endpoints use getChecklistFromGlobalDocMaster

    List<String> getChecklistFromGlobalDocMaster(String docId);

    // -------------------- DOWNLOAD --------------------
    Resource downloadAttachment(String docId, Long docKeyPoid, String fileNameMapped);

    AttachmentDto getAttachmentInfo(String docId, Long docKeyPoid, String fileNameMapped);

    AttachmentDto getAttachmentInfoForArchive(String docId, Long docKeyPoid, String fileNameMapped);

    // -------------------- UTILITY --------------------
    String resolveContentType(String filename);

    // -------------------- EDI --------------------
    void triggerEdi(Long docKeyPoid);

    String getBasePath();

    UploadResponse uploadFilesWithMetadata(String docId, Long docKeyPoid,
                                           List<AttachmentUploadDto> attachments,
                                           Long createdBy, String docShortName, String docRef);
    AttachmentDto getAttachmentBySeqNo(String docId, Long docKeyPoid, Long seqNo);

    List<AttachmentDto> getAllAttachments(String docId, Long docKeyPoid);

}
