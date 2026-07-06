package com.asg.common.services.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.AttachmentFilterType;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.services.dto.*;
import com.asg.common.services.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.asg.common.lib.dto.response.ApiResponse.*;


@RestController
@RequestMapping("/v1/attachments")

public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Operation(summary = "Upload files and/or update remarks - supports structured and array formats")
    @PostMapping(value = "/{docId}/{docKeyPoid}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFilesWithMetadata(
            @PathVariable String docId,
            @PathVariable Long docKeyPoid,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "remarks", required = false) String[] remarks,
            @RequestParam(value = "checklistName", required = false) String[] checklistNames,
            @RequestParam(value = "seqNo", required = false) Long[] seqNos,
            @RequestParam(value = "docShortName", required = false) String docShortName,
            @RequestParam(value = "docRef", required = false) String docRef,
            @ModelAttribute UploadRequestWrapper wrapper
    ) {
        try {
            Long currentUserPoid = UserContext.getUserPoid();
            if ((files == null || files.length == 0) &&
                    (wrapper.getRequest() == null || wrapper.getRequest().isEmpty())) {
                return badRequest("No files or remarks provided for update");
            }
            UploadResponse uploadResponse = null;

            List<AttachmentUploadDto> structuredRequests = wrapper.getRequest();
            List<AttachmentUploadDto> structuredUploads = new ArrayList<>();
            List<AttachmentUploadDto> structuredUpdates = new ArrayList<>();

            if (structuredRequests != null && !structuredRequests.isEmpty()) {
                for (AttachmentUploadDto dto : structuredRequests) {

                    // NEW file (seqNo null) OR file replacement (seqNo present + file present)
                    if (dto.getSeqNo() == null || dto.getFile() != null) {
                        structuredUploads.add(dto);
                        continue;
                    }

                    // EXISTING file metadata update only (no file upload)
                    AttachmentDto old = attachmentService.getAttachmentBySeqNo(docId, docKeyPoid, dto.getSeqNo());

                    boolean remarksChanged =
                            dto.getRemarks() != null &&
                                    !dto.getRemarks().equals(old.getRemarks());

                    boolean checklistChanged =
                            dto.getChecklistName() != null &&
                                    !dto.getChecklistName().equals(old.getChecklistName());

                    // If nothing changed → skip
                    if (!remarksChanged && !checklistChanged) {
                        continue;
                    }

                    structuredUpdates.add(dto);
                }
            }
            List<AttachmentUploadDto> arrayUploads = new ArrayList<>();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    AttachmentUploadDto dto = new AttachmentUploadDto();
                    dto.setFile(files[i]);
                    dto.setRemarks(remarks != null && i < remarks.length ? remarks[i] : null);
                    dto.setChecklistName(checklistNames != null && i < checklistNames.length ? checklistNames[i] : null);
                    dto.setSeqNo(seqNos != null && i < seqNos.length ? seqNos[i] : null);
                    arrayUploads.add(dto);
                }
            }
            List<AttachmentUploadDto> allUploads = new ArrayList<>();
            allUploads.addAll(structuredUploads);
            allUploads.addAll(arrayUploads);

            boolean remarksPresentInUploads = allUploads.stream()
                    .anyMatch(dto -> dto.getRemarks() != null && !dto.getRemarks().isBlank());

            if (!allUploads.isEmpty()) {
                uploadResponse = attachmentService.uploadFilesWithMetadata(docId, docKeyPoid, allUploads, currentUserPoid, docShortName, docRef);
            }

            if (!structuredUpdates.isEmpty()) {
                List<UpdateRemarksRequest> updates = structuredUpdates.stream()
                        .map(dto -> {
                            UpdateRemarksRequest ur = new UpdateRemarksRequest();
                            ur.setDocId(docId);
                            ur.setDocKeyPoid(docKeyPoid);
                            ur.setSeqNo(dto.getSeqNo());
                            ur.setRemarks(dto.getRemarks());
                            ur.setChecklistName(dto.getChecklistName());
                            ur.setOriginalFileName(dto.getFile() != null ? dto.getFile().getOriginalFilename() : null);
                            // FileNameMapped will be resolved by service using seqNo if not provided
                            ur.setFileNameMapped(null);
                            ur.setUpdatedBy(currentUserPoid.toString());
                            return ur;
                        })
                        .collect(Collectors.toList());

                attachmentService.updateRemarksAndChecklist(docId, updates);
            }

            // CASE 1 → Both upload + update
            if (uploadResponse != null && !structuredUpdates.isEmpty()) {
                String message = uploadResponse.isHasErrors() ?
                        "Files uploaded with some errors, remarks updated successfully" :
                        "Files uploaded and remarks updated successfully";
                return success(message, uploadResponse);


                // CASE 2 → Upload only
            }  else if (uploadResponse != null) {
                String message;
                if (uploadResponse.isHasErrors()) {
                    List<String> duplicateFiles = uploadResponse.getErrors().stream()
                            .filter(err -> err.contains("already exists"))
                            .map(err -> err.replace("File already exists: ", ""))
                            .distinct()  // Remove duplicate filenames
                            .collect(Collectors.toList());
                    
                    if (!duplicateFiles.isEmpty()) {
                        message = duplicateFiles.size() + " duplicate file(s) found and skipped...Files :[" + 
                                String.join(", ", duplicateFiles) + "]";
                    } else {
                        message = "Files uploaded with some errors";
                    }
                } else {
                    message = "File uploaded successfully";
                }
                return success(message, uploadResponse);

                // CASE 3 → Only update (no upload)
            }else if (!structuredUpdates.isEmpty()) {
                return success("Remarks updated successfully", null);
            } else {
                return badRequest("No upload or update requests provided");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            return internalServerError("Operation failed: " + ex.getMessage());
        }
    }

    // ------------------- List All Attachments -------------------
    @Operation(
            summary = "List attachments with enhanced filtering",
            description = """
    Fetch attachments for given document with pagination and filtering support.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier

    ### Request Parameters
        - **includeArchived:** Include archived attachments (default: false) - for backward compatibility
        - **filterType:** Filter type (ACTIVE, DELETED, ALL) - overrides includeArchived if provided
        - **page:** Page number for pagination (default: 0)
        - **size:** Number of items per page (default: 20)

    ### Filter Types
        - **ACTIVE:** Returns only active attachments (ACTIVE='Y' AND DELETED='N')
        - **DELETED:** Returns only deleted attachments (DELETED='Y')
        - **ALL:** Returns all attachments regardless of status
    """
    )

    @GetMapping("/{docId}/{docKeyPoid}/list")
    public ResponseEntity<?> getAttachments(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Include archived attachments (legacy parameter)")
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @Parameter(description = "Filter type: ACTIVE, DELETED, or ALL (overrides includeArchived)")
            @RequestParam(required = false) AttachmentFilterType filterType,
            @Parameter(description = "Page number for pagination")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AttachmentDto> result;

            if (filterType != null) {
                result = attachmentService.getAttachmentsByFilter(docId, docKeyPoid, filterType, pageable);
            } else {
                result = attachmentService.getAttachments(docId, docKeyPoid, includeArchived, pageable);
            }

            return success("Attachments fetched successfully", PaginationUtil.wrapPage(result,null));
        } catch (Exception ex) {
            return internalServerError("Fetch failed: " + ex.getMessage());
        }
    }

    // ------------------- List Active Attachments -------------------
    @Operation(
            summary = "List active attachments",
            description = """
    Fetch only active (non-archived) attachments for given document with pagination support.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier

    ### Request Parameters
        - **page:** Page number for pagination (default: 0)
        - **size:** Number of items per page (default: 20)
        - **sort:** Sort field and direction (e.g., "fileName,asc" or "createdDate,desc")

    ### Authorization Parameters
    """
    )
    @GetMapping("/{docId}/{docKeyPoid}/list-active")
    public ResponseEntity<?> getActiveAttachments(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Page number for pagination")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., fileName,asc)")
            @RequestParam(defaultValue = "createdDate,desc") String sort
    ) {
        try {
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

            Page<AttachmentDto> result = attachmentService.getActiveAttachments(docId, docKeyPoid, pageable);
            return success("Active attachments fetched successfully", PaginationUtil.wrapPage(result,null));
        } catch (Exception ex) {
            return internalServerError("Fetch active failed: " + ex.getMessage());
        }
    }

    // ------------------- Enhanced List Attachments by Filter -------------------
    @Operation(
            summary = "List attachments by filter type",
            description = """
    Fetch attachments based on filter type with pagination support.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier

    ### Request Parameters
        - **filterType:** Filter type (ACTIVE, DELETED, ALL) - default: ACTIVE
        - **page:** Page number for pagination (default: 0)
        - **size:** Number of items per page (default: 20)
        - **sort:** Sort field and direction (e.g., "seqNo,asc" or "createdDate,desc")

    ### Filter Types
        - **ACTIVE:** Returns only active attachments (ACTIVE='Y' AND DELETED='N')
        - **DELETED:** Returns only deleted attachments (DELETED='Y')
        - **ALL:** Returns all attachments regardless of status
    """
    )
    @GetMapping("/{docId}/{docKeyPoid}/filter")
    public ResponseEntity<?> getAttachmentsByFilter(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Filter type: ACTIVE, DELETED, or ALL")
            @RequestParam(defaultValue = "ACTIVE") AttachmentFilterType filterType,
            @Parameter(description = "Page number for pagination")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (e.g., seqNo,asc)")
            @RequestParam(defaultValue = "seqNo,asc") String sort
    ) {
        try {
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

            Page<AttachmentDto> result = attachmentService.getAttachmentsByFilter(docId, docKeyPoid, filterType, pageable);
            String message = String.format("%s attachments fetched successfully", filterType.name().toLowerCase());
            return success(message, PaginationUtil.wrapPage(result, null));
        } catch (Exception ex) {
            return internalServerError("Fetch by filter failed: " + ex.getMessage());
        }
    }

    // ------------------- List Deleted Attachments -------------------
  /*  @Operation(
            summary = "List deleted attachments",
            description = """
    Fetch only deleted attachments for given document (non-paginated).

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier
    """
    )
    @GetMapping("/{docId}/{docKeyPoid}/list-deleted")
    public ResponseEntity<?> getDeletedAttachments(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid
    ) {
        try {
            List<AttachmentDto> result = attachmentService.getDeletedAttachments(docId, docKeyPoid);
            return success("Deleted attachments fetched successfully", result);
        } catch (Exception ex) {
            return internalServerError("Fetch deleted failed: " + ex.getMessage());
        }
    }*/

    // ------------------- Get Checklist -------------------
    @Operation(
            summary = "Get attachment checklist items for a document",
            description = """
    Fetch valid checklist options for categorizing attachments for a specific document.

    ### Path Parameters
        - **docId:** Document identifier
    """
    )
    @GetMapping("/documents/{docId}/attachments/checklist")
    public ResponseEntity<?> getAttachmentChecklist(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId) {
        try {
            List<String> checklist = attachmentService.getChecklistFromGlobalDocMaster(docId);
            return success("Checklist loaded successfully", checklist);
        } catch (Exception ex) {
            return internalServerError("Checklist fetch failed: " + ex.getMessage());
        }
    }

    @Operation(
            summary = "Get checklist values (Legacy)",
            description = """
    Legacy endpoint - Fetch checklist options available for a given document type.

    ### Path Parameters
        - **docId:** Document identifier

    ### Authorization Parameters
    """
    )
    @GetMapping("/{docId}/checklist")
    public ResponseEntity<?> getChecklist(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId
    ) {
        try {
            List<String> checklist = attachmentService.getChecklistFromGlobalDocMaster(docId);
            return success("Checklist loaded successfully", checklist);
        } catch (Exception ex) {
            return internalServerError("Checklist fetch failed: " + ex.getMessage());
        }
    }

    // ------------------- Delete Single Attachment -------------------
    @Operation(
            summary = "Delete a single attachment",
            description = """
    Delete a specific attachment by its mapped filename.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier
        - **fileNameMapped:** Mapped filename of the attachment to delete

    ### Authorization Parameters
    """
    )
    @DeleteMapping("/{docId}/{docKeyPoid}/{fileNameMapped}")
    public ResponseEntity<?> deleteAttachment(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Mapped filename of attachment to delete", required = true)
            @PathVariable String fileNameMapped
    ) {
        try {
            attachmentService.deleteAttachment(docId, docKeyPoid, fileNameMapped);
            return success("Attachment deleted successfully", null);
        } catch (ResourceNotFoundException ex) {
            return badRequest(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Delete failed: " + ex.getMessage());
        }
    }

    // ------------------- Delete All Attachments -------------------
    @Operation(
            summary = "Delete all attachments",
            description = """
    Delete all attachments associated with a document.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier

    ### Authorization Parameters
    """
    )
    @DeleteMapping("/{docId}/{docKeyPoid}/delete-all")
    public ResponseEntity<?> deleteAllAttachments(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid
    ) {
        try {
            attachmentService.deleteAllAttachments(docId, docKeyPoid);
            return success("All attachments deleted successfully", null);
        } catch (ResourceNotFoundException ex) {
            return badRequest(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception ex) {
            return internalServerError("Delete all failed: " + ex.getMessage());
        }
    }

    // ------------------- Archive Attachment -------------------
    @Operation(
            summary = "Archive an attachment",
            description = """
    Mark an attachment as archived while retaining it for audit purposes.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier
        - **fileNameMapped:** Mapped filename of attachment to archive

    ### Authorization Parameters
    """
    )
    @PutMapping("/{docId}/{docKeyPoid}/{fileNameMapped}/archive")
    public ResponseEntity<?> archiveAttachment(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Mapped filename of attachment to archive", required = true)
            @PathVariable String fileNameMapped

    ) {
        try {
            Long currentUserPoid = UserContext.getUserPoid();
            attachmentService.archiveAttachment(docId, docKeyPoid, fileNameMapped, currentUserPoid);
            return success("Attachment archived successfully", null);
        } catch (Exception ex) {
            return internalServerError("Archive failed: " + ex.getMessage());
        }
    }

    // ------------------- Activate Attachment -------------------
    @Operation(
            summary = "Activate an attachment",
            description = """
    Mark an attachment as active by setting the active flag from N to Y.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier
        - **fileNameMapped:** Mapped filename of attachment to activate
    """
    )
    @PutMapping("/{docId}/{docKeyPoid}/{fileNameMapped}/active")
    public ResponseEntity<?> activeAttachment(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Mapped filename of attachment to activate", required = true)
            @PathVariable String fileNameMapped
    ) {
        try {
            Long currentUserPoid = UserContext.getUserPoid();
            attachmentService.activateAttachment(docId, docKeyPoid, fileNameMapped, currentUserPoid);
            return success("Attachment activated successfully", null);
        } catch (Exception ex) {
            return internalServerError("Activate failed: " + ex.getMessage());
        }
    }

    // ------------------- Download Attachment -------------------
    @Operation(
            summary = "Download an attachment",
            description = """
    Stream attachment file for download.

    ### Path Parameters
        - **docId:** Document identifier
        - **docKeyPoid:** Document key primary object identifier
        - **fileNameMapped:** Mapped filename of attachment to download

    ### Authorization Parameters
    """
    )
    @GetMapping("/{docId}/{docKeyPoid}/{fileNameMapped}/download")
    public ResponseEntity<?> downloadAttachment(
            @Parameter(description = "Document identifier", required = true)
            @PathVariable String docId,
            @Parameter(description = "Document key primary object identifier", required = true)
            @PathVariable Long docKeyPoid,
            @Parameter(description = "Mapped filename of attachment to download", required = true)
            @PathVariable String fileNameMapped
    ){

        if (!hasDownloadPermission(docId, docKeyPoid)) {
            return error("Access denied", 403);
        }

        // Get attachment info (validates + logs)
        AttachmentDto attachmentInfo = attachmentService.getAttachmentInfo(docId, docKeyPoid, fileNameMapped);

        // Download file
        Resource resource = attachmentService.downloadAttachment(docId, docKeyPoid, fileNameMapped);

        String contentType = attachmentService.resolveContentType(attachmentInfo.getOriginalFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachmentInfo.getOriginalFileName()).build().toString())
                .body(resource);
    }

    private boolean hasDownloadPermission(String docId, Long docKeyPoid) {
        return UserContext.getCurrentUser() != null;
    }
}