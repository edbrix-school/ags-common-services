package com.asg.common.services.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AttachmentUploadDto {
    private MultipartFile file;
    private String remarks;
    private String checklistName;
    private boolean attachEDI;
    private Long attachmentEDIJobPoid;
    private Long seqNo; // Auto-generated if null

    public void setJobId(Long jobId) {
        if (jobId != null) this.attachmentEDIJobPoid = jobId;
    }

    public void setJobPoid(Long jobPoid) {
        if (jobPoid != null) this.attachmentEDIJobPoid = jobPoid;
    }
}
