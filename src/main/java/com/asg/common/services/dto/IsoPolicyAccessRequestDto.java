package com.asg.common.services.dto;

import com.asg.common.services.enums.IsoPolicyLogType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Records one Access Log event: the logged-in employee opened, or acknowledged, a file attached to
 * an ISO document/policy.
 * <p>
 * The log appends — each call inserts a row — so this is an event, not a state change. Access is
 * tracked per file, not per document.
 * <p>
 * There is deliberately no {@code employeePoid} field. The employee is resolved server-side from the
 * token, via {@code HR_EMPLOYEE_MASTER.LOGIN_USER_POID}. Taking it from the request body would let
 * any authenticated caller record or acknowledge on behalf of any employee — in the table an ISO
 * auditor reads.
 */
@Data
public class IsoPolicyAccessRequestDto {

    /**
     * The attachment id ({@code GLOBAL_ATTACHMENTS.SEQNO}), as returned by my-documents. Must be a
     * live file on this document. The id, not the name: file names are neither unique nor stable.
     */
    @NotNull(message = "Attachment id is required")
    private Long attachmentId;

    /** Accessed or Acknowledged. Acknowledged is rejected if the document does not require acknowledgement. */
    @NotNull(message = "Log type is required")
    private IsoPolicyLogType logType;

    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    private String remarks;
}
