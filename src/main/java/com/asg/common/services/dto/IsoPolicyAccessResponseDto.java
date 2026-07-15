package com.asg.common.services.dto;

import com.asg.common.services.enums.IsoPolicyLogType;
import lombok.Data;

import java.time.LocalDateTime;

/** The Access Log event that was just written. */
@Data
public class IsoPolicyAccessResponseDto {

    private Long transactionPoid;
    private Long detRowId;
    private Long employeePoid;

    /** The file this event is about — GLOBAL_ATTACHMENTS.SEQNO. */
    private Long attachmentId;

    /** Its name at the time of the event, snapshotted onto the row for display. */
    private String attachmentName;

    private IsoPolicyLogType logType;

    /** The document's version when the event happened. */
    private String docVersion;

    /** When the event happened — the row's CREATED_DATE. */
    private LocalDateTime loggedOn;

    private String remarks;
}
