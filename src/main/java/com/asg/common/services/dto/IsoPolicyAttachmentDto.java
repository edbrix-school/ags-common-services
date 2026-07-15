package com.asg.common.services.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * One file attached to an ISO document or policy — the content the employee actually opens, and the
 * unit that access and acknowledgement are recorded against.
 * <p>
 * Sourced from {@code GLOBAL_ATTACHMENTS} (keyed by DOC_ID + DOC_KEY_POID), left-joined to this
 * employee's own Access Log row for this file.
 */
@Data
public class IsoPolicyAttachmentDto {

    /** Discriminator for the widget's tree: this node renders as a file inside its document folder. */
    private final String type = "file";

    /**
     * The attachment id ({@code GLOBAL_ATTACHMENTS.SEQNO}). Pass this back to the event-log endpoint
     * when recording an access or acknowledgement.
     */
    private Long attachmentId;

    /** Original file name, e.g. "Information Security Policy.pdf". Display only. */
    private String fileName;

    /**
     * {@code GLOBAL_ATTACHMENTS.FILE_NAME_MAPPED} — the name the file is actually stored under on
     * disk, and what the download endpoint takes. Named to match the platform's own
     * {@code AttachmentDto.storedFileName}, which maps the same column.
     */
    private String storedFileName;

    private String fileRemarks;
    private String checklistName;

    private String uploadedBy;
    private LocalDateTime uploadedOn;

    /**
     * Path to stream the file. Built here so the UI does not have to assemble it. Requires only
     * authentication — see {@code AttachmentController#hasDownloadPermission}.
     */
    private String downloadPath;

    // ---- this employee's history with this file, aggregated from the event log ----

    /** True once the employee has acknowledged this file at any version. */
    private boolean acknowledged;

    /** When they most recently acknowledged it. */
    private LocalDateTime acknowledgedTime;

    /** Document version at their most recent acknowledgement. Older than the current one = stale. */
    private String acknowledgedVersion;

    /**
     * True when the document requires acknowledgement and the employee either has never acknowledged
     * this file, or acknowledged it at an older version than the one now published.
     */
    private boolean acknowledgementPending;

    /** Most recent event of any type — MAX(CREATED_DATE) over this employee's rows for the file. */
    private LocalDateTime lastAccessedTime;

    /** How many times this employee has opened this file — the count of 'Accessed' events. */
    private long accessCount;
}
