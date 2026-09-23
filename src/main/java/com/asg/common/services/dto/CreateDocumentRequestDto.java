package com.asg.common.services.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CreateDocumentRequestDto {
    private String docShortName;
    private String docRef;
    private String remarks;
    private String checklistName;
    private Long categoryId;
    private Map<String, Object> parameters;
}
