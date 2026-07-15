package com.asg.common.services.controller;

import com.asg.common.services.dto.IsoPolicyAccessRequestDto;
import com.asg.common.services.dto.IsoPolicyAccessResponseDto;
import com.asg.common.services.dto.IsoPolicyDocumentDto;
import com.asg.common.services.service.IsoPolicyAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.asg.common.lib.dto.response.ApiResponse.success;

/**
 * Employee-facing API for ISO documents and policies (asg-admin-iso, DocId 700-101): the Home page
 * Documents widget and the Access Log it writes.
 * <p>
 * Deliberately not guarded by {@code @AllowedAction}: the caller is an employee opening a published
 * document, not an admin working the ISO screen, so they hold no rights on document 700-101 and a
 * rights check would reject every legitimate call. Authentication still applies, and the employee is
 * resolved from the token via {@code HR_EMPLOYEE_MASTER.LOGIN_USER_POID} — never from a
 * caller-supplied id — so a caller can only read their own documents and write their own Access Log
 * row. The ISO admin screen itself stays in asg-admin-iso.
 */
@RestController
@RequestMapping("/v1/iso-policy-access")
@RequiredArgsConstructor
@Slf4j
public class IsoPolicyAccessController {

    private final IsoPolicyAccessService isoPolicyAccessService;

    @Operation(summary = "List the ISO documents and policies published to the logged-in employee",
            description = "Backs the Home page Documents widget. Returns the documents visible to the employee "
                    + "(published to All Employees, to their department, or to them by name), each carrying its "
                    + "attachments — the document is the folder, its files are the content — plus whether the "
                    + "employee still owes an acknowledgement. Expired documents are excluded unless "
                    + "includeExpired=true.")
    @GetMapping("/my-documents")
    public ResponseEntity<?> getMyDocuments(
            @Parameter(description = "Include documents past their expiry date")
            @RequestParam(required = false, defaultValue = "false") boolean includeExpired) {
        List<IsoPolicyDocumentDto> documents = isoPolicyAccessService.getMyDocuments(includeExpired);
        return success("Documents retrieved successfully", documents);
    }

    @Operation(summary = "Record the logged-in employee's access / acknowledgement of a file",
            description = "Appends one row to the Access Log — an open writes an 'Accessed' event, an "
                    + "acknowledgement writes an 'Acknowledged' event — for a single file, stamped with the "
                    + "document's current version. Nothing is updated in place, so the full history survives. "
                    + "The document must be one published to this employee, and the file must be live on it.")
    @PostMapping("/{transactionPoid}")
    public ResponseEntity<?> recordAccess(
            @Parameter(description = "ISO document/policy Transaction POID", required = true)
            @PathVariable Long transactionPoid,
            @Valid @RequestBody IsoPolicyAccessRequestDto request) {
        IsoPolicyAccessResponseDto response = isoPolicyAccessService.recordAccess(transactionPoid, request);
        return success("Access recorded successfully", response);
    }
}
