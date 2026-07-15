package com.asg.common.services.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One ISO document or policy visible to an employee. This is an <b>internal</b> carrier, not a node
 * in the Home page widget: the widget groups by category (folder) -> attachment (file), so a
 * document is only the intermediate that ties its files to a category and a version. Its attachments
 * are lifted out and grouped under their category folder; the document itself is not serialised.
 */
@Data
public class IsoPolicyDocumentDto {

    private Long transactionPoid;
    private String docRef;
    private String docName;
    private String docType;
    private String category;
    private String description;
    private String versionNo;

    private LocalDate expiryDate;

    /** Timestamp of addition, shown against the file in the widget. */
    private LocalDateTime publishedOn;

    private boolean acknowledgementRequired;

    /**
     * True when every file on this document has been acknowledged at the current version.
     * Acknowledgement is recorded per attachment, so the document is only "done" when its files are.
     * A document with no files is never acknowledged — there is nothing to read.
     */
    private boolean acknowledged;

    /**
     * True when an acknowledgement is required and at least one file is still outstanding — never
     * opened, never acknowledged, or acknowledged at an older version than the one now published.
     * This is what the widget badges.
     */
    private boolean acknowledgementPending;

    /** Files on this document the employee still owes an acknowledgement for. */
    private int acknowledgementPendingCount;

    /** The most recent time the employee opened any file on this document. */
    private LocalDateTime lastAccessedTime;

    /** The files attached to this document — what the employee opens. Empty if none were uploaded. */
    private List<IsoPolicyAttachmentDto> attachments;

    private int attachmentCount;
}
