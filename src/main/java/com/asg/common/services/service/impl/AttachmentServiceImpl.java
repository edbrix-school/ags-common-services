package com.asg.common.services.service.impl;

import com.asg.common.lib.enums.AttachmentFilterType;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.AsgException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.services.dto.AttachmentDto;
import com.asg.common.services.dto.AttachmentUploadDto;
import com.asg.common.services.dto.UpdateRemarksRequest;
import com.asg.common.services.dto.UploadResponse;
import com.asg.common.services.entity.Attachment;
import com.asg.common.services.client.DmsClient;
import com.asg.common.services.repository.AttachmentRepository;
import com.asg.common.services.service.AttachmentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final DmsClient dmsClient;
    private final HttpServletRequest httpServletRequest;

    @Autowired
    private LoggingService loggingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${attachments.base-path:/opt/app/attachments/}")
    private String basePath;

    @Value("${attachments.allowed-extensions:pdf,txt,csv,jpg,jpeg,png,bmp,gif,doc,docx,xls,xlsx,ppt,pptx}")
    private String allowedExtensions;

    @Override
    public String getBasePath() {
        return basePath;
    }

    // private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // Not used in attachment functionality

    // UPLOAD FILES
    @Override
    @Transactional
    public UploadResponse uploadFiles(String docId, Long docKeyPoid, MultipartFile[] files,
                                      String remarks, String checklistName, Long createdBy,
                                      boolean attachEDI, Long attachmentEDIJobPoid) {

        validateDoc(docId, docKeyPoid);
        if (files == null || files.length == 0) throw new IllegalArgumentException("No files provided");

        List<AttachmentDto> existing = getActiveAttachments(docId, docKeyPoid);
        Set<String> existingFileNames = existing.stream()
                .map(AttachmentDto::getOriginalFileName)
                .collect(Collectors.toSet());

        List<AttachmentDto> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalName = sanitizeFilename(file.getOriginalFilename());

            if (originalName == null || originalName.isEmpty()) {
                errors.add("Invalid file name");
                continue;
            }

            if (!isAllowedExtension(originalName)) {
                errors.add("Invalid file type: " + originalName);
                continue;
            }

            if (existingFileNames.contains(originalName)) {
                errors.add("File already exists: " + originalName);
                continue;
            }

            try {
                // Upload to DMS — returns stored key: DMS_{documentId}_{documentFileId}
                String authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                String storedName = dmsClient.uploadToDms(file, docId, docKeyPoid, authToken);

                Long groupPoid = getGroupPoid();
                Long companyPoid = 1L;

                Long maxSeq = existing.stream()
                        .map(AttachmentDto::getSeqNo)
                        .filter(Objects::nonNull)
                        .max(Long::compareTo)
                        .orElse(0L);

                Long newSeq = maxSeq + 1L;

                attachmentRepository.insertAttachment(
                        groupPoid, companyPoid, docId, docKeyPoid, newSeq,
                        originalName, remarks, checklistName, createdBy,
                        storedName, attachEDI ? attachmentEDIJobPoid : 0L
                );

                if (attachEDI) {
                    String loginUser = UserContext.getUserId() != null ? UserContext.getUserId() : "82";
                    callEdiProc(groupPoid, companyPoid, docId, docKeyPoid, attachmentEDIJobPoid, loginUser);
                }

                uploaded.add(buildDto(docKeyPoid, originalName, storedName, remarks, checklistName,String.valueOf(createdBy != null ? createdBy : getUserPoid()), new Date(), true));
                existingFileNames.add(originalName);
                
                // Log attachment upload
                String logDetail = String.format("%s File: %s", LogDetailsEnum.ATTACHMENTS_UPLOADED.getDescription(), originalName);
                loggingService.createLogSummaryEntry(docId, docKeyPoid.toString(), logDetail);

            } catch (Exception ex) {
                log.error("Upload failed for {}: {}", file.getOriginalFilename(), ex.getMessage(), ex);
                errors.add("Upload error for " + file.getOriginalFilename() + ": " + ex.getMessage());
            }
        }

        return new UploadResponse(uploaded, errors);
    }


    // GET ATTACHMENTS (Paginated) - Backward Compatible
    @Override
    @Transactional(readOnly = true)
    public Page<AttachmentDto> getAttachments(String docId, Long docKeyPoid, boolean includeArchived, Pageable pageable) {
        AttachmentFilterType filterType = includeArchived ? AttachmentFilterType.ALL : AttachmentFilterType.ACTIVE;
        return getAttachmentsByFilter(docId, docKeyPoid, filterType, pageable);
    }

    // ENHANCED GET ATTACHMENTS BY FILTER
    @Override
    @Transactional(readOnly = true)
    public Page<AttachmentDto> getAttachmentsByFilter(String docId, Long docKeyPoid, AttachmentFilterType filterType, Pageable pageable) {
        validateDoc(docId, docKeyPoid);

        List<Object[]> rows = attachmentRepository.fetchAttachmentsByFilter(getGroupPoid(), 1L, docId, docKeyPoid, filterType);

        List<AttachmentDto> dtos = rows.stream()
                .map(this::mapRowToDto)
                .filter(dto -> applyClientSideFilter(dto, filterType))
                .sorted(Comparator.comparing(AttachmentDto::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());

        return new PageImpl<>(start > end ? Collections.emptyList() : dtos.subList(start, end), pageable, dtos.size());
    }

    // GET DELETED ATTACHMENTS
    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getDeletedAttachments(String docId, Long docKeyPoid) {
        validateDoc(docId, docKeyPoid);

        return attachmentRepository.fetchDeletedAttachments(getGroupPoid(), 1L, docId, docKeyPoid)
                .stream()
                .map(this::mapRowToDto)
                .filter(dto -> dto.isDeleted())
                .sorted(Comparator.comparing(AttachmentDto::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // GET ACTIVE ATTACHMENTS
    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getActiveAttachments(String docId, Long docKeyPoid) {
        validateDoc(docId, docKeyPoid);

        return attachmentRepository.fetchAllAttachments(getGroupPoid(), 1L, docId, docKeyPoid)
                .stream()
                .map(this::mapRowToDto)
                .sorted(Comparator.comparing(AttachmentDto::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getAllAttachments(String docId, Long docKeyPoid) {
        validateDoc(docId, docKeyPoid);

        return attachmentRepository.fetchAllAttachments(getGroupPoid(), UserContext.getCompanyPoid(), docId, docKeyPoid)
                .stream()
                .map(this::mapRowToDto)
                .sorted(Comparator.comparing(AttachmentDto::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // GET ACTIVE ATTACHMENTS (Paginated)
    @Override
    @Transactional(readOnly = true)
    public Page<AttachmentDto> getActiveAttachments(String docId, Long docKeyPoid, Pageable pageable) {
        validateDoc(docId, docKeyPoid);

        List<AttachmentDto> dtos = attachmentRepository.fetchActiveAttachments(getGroupPoid(), 1L, docId, docKeyPoid)
                .stream()
                .map(this::mapRowToDto)
                .sorted(Comparator.comparing(AttachmentDto::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());

        return new PageImpl<>(start > end ? Collections.emptyList() : dtos.subList(start, end), pageable, dtos.size());
    }

    // DELETE & ARCHIVE
    @Override
    @Transactional
    public void deleteAttachment(String docId, Long docKeyPoid, String fileNameMapped) {
        validateDoc(docId, docKeyPoid);
        
        if (fileNameMapped == null || fileNameMapped.trim().isEmpty()) {
            throw new IllegalArgumentException("File Name Mapped is required for deletion");
        }
        
        // Check if attachment exists before deletion
        Optional<Attachment> attachment = attachmentRepository.findByDocIdAndDocKeyPoidAndFileNameMapped(docId, docKeyPoid, fileNameMapped);
        if (attachment.isEmpty()) {
            throw new ResourceNotFoundException("Attachment", "parameters", "docId=" + docId + ", docKeyPoid=" + docKeyPoid + ", fileNameMapped=" + fileNameMapped);
        }

        // Get filename before deletion
        String originalFileName = attachment.get().getFileName();
        
        attachmentRepository.deleteAttachment(getGroupPoid(), 1L, docId, docKeyPoid, fileNameMapped);
        
        // Log attachment deletion
        String logDetail = String.format("%s File: %s", LogDetailsEnum.ATTACHMENT_DELETED.getDescription(), originalFileName);
        loggingService.createLogSummaryEntry(docId, docKeyPoid.toString(), logDetail);
    }

    @Override
    @Transactional
    public void deleteAllAttachments(String docId, Long docKeyPoid) {
        validateDoc(docId, docKeyPoid);
        
        // Check if any attachments exist for the given docId and docKeyPoid
        List<AttachmentDto> existingAttachments = getAllAttachments(docId, docKeyPoid);
        if (existingAttachments.isEmpty()) {
            throw new ResourceNotFoundException("Attachments", "parameters", "docId=" + docId + ", docKeyPoid=" + docKeyPoid);
        }
        attachmentRepository.deleteAttachment(getGroupPoid(), 1L, docId, docKeyPoid, "(ALL)");
        String logDetail = LogDetailsEnum.ATTACHMENTS_DELETED.getDescription();
        loggingService.createLogSummaryEntry(docId, docKeyPoid.toString(), logDetail);
    }

    @Override
    @Transactional
    public String archiveAttachment(String docId, Long docKeyPoid, String fileNameMapped, Long userPoid) {
        validateDoc(docId, docKeyPoid);
        
        // Get original filename before archiving
        AttachmentDto attachmentInfo = getAttachmentInfoForArchive(docId, docKeyPoid, fileNameMapped);
        String originalFileName = stripTimestampPrefixes(attachmentInfo.getOriginalFileName());
        
        // Call existing archive procedure
        attachmentRepository.archiveAttachment(getGroupPoid(), 1L, docId, docKeyPoid, fileNameMapped);
        
        // Log attachment archiving
        String logDetail = String.format("%s File: %s", LogDetailsEnum.ATTACHMENT_ARCHIVED.getDescription(), originalFileName);
        loggingService.createLogSummaryEntry(docId, docKeyPoid.toString(), logDetail);
        
        // Generate archived filename with timestamp (format: ddMMyyyyHHmm_originalname)
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyyHHmm"));
        String archivedFileName = timestamp + "_" + originalFileName;
        
        return archivedFileName;
    }

    @Override
    @Transactional
    public void activateAttachment(String docId, Long docKeyPoid, String fileNameMapped, Long userPoid) {
        validateDoc(docId, docKeyPoid);
        
        // Check if attachment exists
        Attachment attachment = attachmentRepository
                .findByDocIdAndDocKeyPoidAndFileNameMappedForArchive(docId, docKeyPoid, fileNameMapped)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "fileNameMapped", fileNameMapped));
        
        // Validate that attachment is currently archived (active = 'N')
        if ("Y".equalsIgnoreCase(attachment.getActive())) {
            throw new IllegalArgumentException("Attachment is already active");
        }
        // Update active flag from N to Y
        attachmentRepository.activateAttachment(docId, docKeyPoid, fileNameMapped);

        // Log attachment unarchive/activation
        String logDetail = String.format("%s File: %s", LogDetailsEnum.ATTACHMENT_UNARCHIVED.getDescription(), stripTimestampPrefixes(attachment.getFileName()));
        loggingService.createLogSummaryEntry(docId, docKeyPoid.toString(), logDetail);
    }

    // BULK UPDATE
    @Override
    @Transactional
    public void updateRemarksAndChecklist(String docId, List<UpdateRemarksRequest> updates) {
        validateDocId(docId);
        
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("Updates list cannot be null or empty");
        }

        for (UpdateRemarksRequest u : updates) {
            if (u.getDocKeyPoid() == null || u.getDocKeyPoid() <= 0) {
                throw new IllegalArgumentException("Doc Key Poid is required for update");
               // continue;
            }
            if (u.getSeqNo() == null || u.getSeqNo() <= 0) {
                throw new IllegalArgumentException("Seq No is required for update");
            }
            
            String actualDocId = (u.getDocId() != null && !u.getDocId().trim().isEmpty()) ? u.getDocId() : docId;
            String fileNameMapped = u.getFileNameMapped();
            
            // If fileNameMapped is null, empty, or a placeholder, find it using seqNo
            if (fileNameMapped == null || fileNameMapped.trim().isEmpty() || fileNameMapped.startsWith("PLACEHOLDER_")) {
                // Find the attachment by docId, docKeyPoid, and seqNo to get the actual fileNameMapped
                List<AttachmentDto> attachments = getActiveAttachments(actualDocId, u.getDocKeyPoid());
                Optional<AttachmentDto> targetAttachment = attachments.stream()
                    .filter(att -> att.getSeqNo() != null && att.getSeqNo().equals(u.getSeqNo()))
                    .findFirst();
                    
                if (targetAttachment.isPresent()) {
                    fileNameMapped = targetAttachment.get().getStoredFileName();
                } else {
                    throw new ResourceNotFoundException("Attachment", "seqNo", u.getSeqNo().toString());
                }
            }
            // -------- FIX : Resolve filename by seqNo only --------
            AttachmentDto existingAttachment = getAttachmentBySeqNo(
                    actualDocId,
                    u.getDocKeyPoid(),
                    u.getSeqNo()
            );
            boolean remarksChanged =
                    !Objects.equals(existingAttachment.getRemarks(), u.getRemarks());

            boolean checklistChanged =
                    !Objects.equals(existingAttachment.getChecklistName(), u.getChecklistName());

            if (!remarksChanged && !checklistChanged) {
                log.info("No changes detected for attachment seqNo={}, skipping update & log", u.getSeqNo());
                continue;
            }

            String resolvedFileName =
                    (u.getOriginalFileName() != null && !u.getOriginalFileName().isBlank())
                            ? u.getOriginalFileName()
                            : existingAttachment.getOriginalFileName();

            attachmentRepository.updateAttachment(
                    getGroupPoid(), 1L, actualDocId, u.getDocKeyPoid(), u.getSeqNo(),
                    existingAttachment.getOriginalFileName(),
                    u.getRemarks(), u.getChecklistName(),
                    getUserPoid(), fileNameMapped
            );

            // Log attachment update
            StringBuilder logMsg = new StringBuilder(
                    "Attachment comments updated, File Name : " + resolvedFileName
            );

            if (remarksChanged) {
                logMsg.append(", Remarks : ").append(u.getRemarks());
            }
            if (checklistChanged) {
                logMsg.append(", Check List Name : ").append(u.getChecklistName());
            }

            loggingService.createLogSummaryEntry(
                    actualDocId,
                    u.getDocKeyPoid().toString(),
                    logMsg.toString()
            );


        }
        
        entityManager.flush();
    }

    /*@Override
    @Transactional
    public void updateSeqNo(String docId, Long docKeyPoid, List<UpdateSeqRequest> updates) {
        validateDoc(docId, docKeyPoid);

        for (UpdateSeqRequest u : updates) {
            attachmentRepository.updateSeqNo(getGroupPoid(), 1L, docId, docKeyPoid,
                    u.getFileNameMapped(), u.getNewSeqNo());
        }
    }*/
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getChecklistFromGlobalDocMaster(String docId) {
        validateDocId(docId);
        return attachmentRepository.getChecklistFromGlobalDocMaster(docId);
    }

    // DOWNLOAD FILE
    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(String docId, Long docKeyPoid, String fileNameMapped) {
        validateDoc(docId, docKeyPoid);

        Attachment attachment = attachmentRepository
                .findByDocIdAndDocKeyPoidAndFileNameMappedForArchive(docId, docKeyPoid, fileNameMapped)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "fileNameMapped", fileNameMapped));

        String mappedFileName = attachment.getFileNameMapped();

        // DMS stored files have key format: DMS_{documentId}_{documentFileId}
        if (mappedFileName != null && mappedFileName.startsWith("DMS_")) {
            Long documentId = Long.valueOf(mappedFileName.substring(4)); // DMS_{documentId}
            String authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            return dmsClient.downloadFromDms(documentId, attachment.getFileName(), authToken);
        }

        // Fallback: legacy local file system
        String attachmentsPath = resolveAttachmentsPath(docId);
        if (attachmentsPath == null || attachmentsPath.trim().isEmpty()) {
            throw new AsgException("AttachmentsPath is missing for login company", 500);
        }
        File file;
        if (mappedFileName.contains(".")) {
            file = new File(attachmentsPath, mappedFileName);
        } else {
            String extension = FilenameUtils.getExtension(attachment.getFileName());
            file = new File(attachmentsPath, mappedFileName + "." + extension);
        }
        if (!file.exists() || !file.canRead()) {
            throw new ResourceNotFoundException("File not found on server", "path", file.getAbsolutePath());
        }
        return new FileSystemResource(file);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentInfo(String docId, Long docKeyPoid, String fileNameMapped) {
        Attachment a = attachmentRepository
                .findByDocIdAndDocKeyPoidAndFileNameMappedForArchive(docId, docKeyPoid, fileNameMapped)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "fileNameMapped", fileNameMapped));
        String logDetail = String.format("%s File: %s", LogDetailsEnum.ATTACHMENT_VIEWED.getDescription(), a.getFileName());

        // Log attachment viewed/downloaded
        loggingService.createLogSummaryEntry( docId, docKeyPoid.toString(), logDetail);

        return mapRowToDto(new Object[]{
                a.getGroupPoid(), a.getCompanyPoid(), a.getDocId(),
                a.getDocKeyPoid(), a.getSeqNo(), a.getFileName(),
                a.getFileRemarks(), a.getChecklistName(), a.getCreatedBy(),
                a.getCreatedDate() != null ? a.getCreatedDate().toString() : null,
                a.getFileNameMapped()
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentInfoForArchive(String docId, Long docKeyPoid, String fileNameMapped) {
        Attachment a = attachmentRepository
                .findByDocIdAndDocKeyPoidAndFileNameMappedForArchive(docId, docKeyPoid, fileNameMapped)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "fileNameMapped", fileNameMapped));

        return mapRowToDto(new Object[]{
                a.getGroupPoid(), a.getCompanyPoid(), a.getDocId(),
                a.getDocKeyPoid(), a.getSeqNo(), a.getFileName(),
                a.getFileRemarks(), a.getChecklistName(), a.getCreatedBy(),
                a.getCreatedDate() != null ? a.getCreatedDate().toString() : null,
                a.getFileNameMapped()
        });
    }

    // EDI TRIGGER
    @Override
    @Transactional
    public void triggerEdi(Long docKeyPoid) {
        if (docKeyPoid == null || docKeyPoid <= 0) return;

        try {
            Session session = entityManager.unwrap(Session.class);
            Connection conn = session.doReturningWork(c -> c);

            try (CallableStatement cs = conn.prepareCall("{call PROC_ATTACHMENTS_EDI_PROC_NEW(?, ?, ?, ?, ?, ?, ?)}")) {
                Long groupPoid = UserContext.getCurrentUser() != null ? UserContext.getCurrentUser().getUserPoid() : 1L;
                Long companyPoid = 1L;
                String loginUser = UserContext.getUserId() != null ? UserContext.getUserId() : "82";

                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setString(3, "100-101");
                cs.setLong(4, docKeyPoid);
                cs.setLong(5, docKeyPoid);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.setString(7, loginUser);

                cs.execute();

                String status = cs.getString(6);
                if (status != null && status.contains("ERROR")) {
                    throw new AsgException("EDI failed: " + status, 500);
                }
            }
        } catch (Exception e) {
            throw new AsgException("EDI error: " + e.getMessage(), 500, e);
        }
    }

    @Override
    public String resolveContentType(String fileName) {
        if (fileName == null || fileName.isBlank()) return "application/octet-stream";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "bmp" -> "image/bmp";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }



    private String resolveAttachmentsPath(String docId) {
        if ("Masters".equals(docId)) {
            return basePath;
        } else {
            // For other document types, use basePath as fallback
            // In production, this would query parameter table for company-specific path
            return basePath;
        }
    }

    // ================== Helper Methods ==================
    private Long getGroupPoid() {
        return UserContext.getCurrentUser() != null
                ? UserContext.getCurrentUser().getUserPoid()
                : 1L;
    }

    private Long getUserPoid() {
        return UserContext.getUserPoid() != null ? UserContext.getUserPoid() : 1L;
    }

    private String getUserId() {
        return UserContext.getUserId() != null ? UserContext.getUserId() : "82";
    }

    private void validateDocId(String docId) {
        if (docId == null || docId.trim().isEmpty())
            throw new IllegalArgumentException("Doc Id is required");
    }

    private void validateDoc(String docId, Long docKeyPoid) {
        validateDocId(docId);
        if (docKeyPoid == null || docKeyPoid <= 0)
            throw new IllegalArgumentException("Invalid Doc Key Poid");
    }

    private String sanitizeFilename(String filename) {
        return filename == null ? null : filename.replaceAll("[&%@!*+<>:/\\\\|?#]", "_");
    }

    private boolean isAllowedExtension(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        Set<String> allowed = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return allowed.contains(ext);
    }

    private AttachmentDto buildDto(Long docKeyPoid, String orig, String mapped, String remarks, String checklist,
                                   String createdBy, Date createdDate, boolean active) {
        AttachmentDto d = new AttachmentDto();
        d.setGroupPoid(getGroupPoid());
        d.setCompanyPoid(1L);
        d.setDocKeyPoid(docKeyPoid);
        d.setOriginalFileName(orig);
        d.setStoredFileName(mapped);
        d.setRemarks(remarks);
        d.setChecklistName(checklist);
        d.setUploadedBy(createdBy);
        d.setCreatedDate(createdDate != null ? createdDate.toString() : null);
        d.setActive(active);
        return d;
    }

    private AttachmentDto mapRowToDto(Object[] row) {
        AttachmentDto d = new AttachmentDto();
        d.setGroupPoid(row[0] != null ? ((Number) row[0]).longValue() : null);
        d.setCompanyPoid(row[1] != null ? ((Number) row[1]).longValue() : null);
        d.setDocId(row[2] != null ? row[2].toString() : null);
        d.setDocKeyPoid(row[3] != null ? ((Number) row[3]).longValue() : null);
        d.setSeqNo(row[4] != null ? ((Number) row[4]).longValue() : null);
        d.setOriginalFileName(row[5] != null ? row[5].toString() : null);
        d.setRemarks(row[6] != null ? row[6].toString() : null);
        d.setChecklistName(row[7] != null ? row[7].toString() : null);
        d.setUploadedBy(row[8] != null ? row[8].toString() : null);
        d.setCreatedDate(row[9] != null ? row[9].toString() : null);
        d.setStoredFileName(row[10] != null ? row[10].toString() : null);
        
        // Map ACTIVE and DELETED columns (now available from native query)
        String activeValue = row.length > 11 && row[11] != null ? row[11].toString().trim() : null;
        String deletedValue = row.length > 12 && row[12] != null ? row[12].toString().trim() : null;

        // ACTIVE: 'Y' = true, 'N' = false, null = true (default active)
        d.setActive(activeValue == null || "Y".equalsIgnoreCase(activeValue));
        
        // DELETED: 'Y' = true, 'N' or null = false
        d.setDeleted("Y".equalsIgnoreCase(deletedValue));

        return d;
    }

    private boolean applyClientSideFilter(AttachmentDto dto, AttachmentFilterType filterType) {
        return switch (filterType) {
            case ACTIVE -> dto.isActive() && !dto.isDeleted();
            case DELETED -> dto.isDeleted();
            case ALL -> true;
        };
    }

    private String callEdiProc(Long groupPoid, Long companyPoid, String docId, Long docKeyPoid, Long ediJobPoid, String loginUser) {
        // Placeholder for actual logic
        return "SUCCESS";
    }

    /** Strips all leading ddMMyyyyHHmm_ timestamp prefixes from a filename. */
    private String stripTimestampPrefixes(String fileName) {
        if (fileName == null) return null;
        // Pattern: 12 digits followed by underscore (ddMMyyyyHHmm_)
        while (fileName.matches("^\\d{12}_.*")) {
            fileName = fileName.substring(13);
        }
        return fileName;
    }

    @Override
    @Transactional
    public UploadResponse uploadFilesWithMetadata(String docId, Long docKeyPoid,
                                                  List<AttachmentUploadDto> attachments,
                                                  Long createdBy) {
        validateDoc(docId, docKeyPoid);

        List<AttachmentDto> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (AttachmentUploadDto req : attachments) {
            MultipartFile file = req.getFile();
            if (file == null || file.isEmpty()) {
                errors.add("Missing file in request");
                continue;
            }

            try {
                UploadResponse singleResp = uploadFiles(
                        docId, docKeyPoid,
                        new MultipartFile[]{file},
                        req.getRemarks(),
                        req.getChecklistName(),
                        createdBy,
                        req.isAttachEDI(),
                        req.getAttachmentEDIJobPoid()
                );

                uploaded.addAll(singleResp.getUploadedFiles());
                errors.addAll(singleResp.getErrors());

            } catch (Exception e) {
                errors.add("Upload failed for " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        return new UploadResponse(uploaded, errors);
    }
    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentBySeqNo(String docId, Long docKeyPoid, Long seqNo) {
        validateDoc(docId, docKeyPoid);

        List<AttachmentDto> attachments = getActiveAttachments(docId, docKeyPoid);

        return attachments.stream()
                .filter(a -> a.getSeqNo() != null && a.getSeqNo().equals(seqNo))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment",
                        "seqNo",
                        String.valueOf(seqNo)
                ));
    }

}
