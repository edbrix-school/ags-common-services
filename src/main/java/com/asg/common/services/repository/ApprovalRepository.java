package com.asg.common.services.repository;

import com.asg.common.services.dto.ApprovalActionRequest;
import com.asg.common.services.dto.ApprovalActionResponse;
import com.asg.common.services.dto.ApprovalLogResponse;
import com.asg.common.services.dto.ApprovalStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ApprovalRepository {

    private final DataSource dataSource;

    public ApprovalActionResponse executeApprovalAction(ApprovalActionRequest request) {
        // PROC_GLOB_APPROVAL_ACTION declares parameters 12-14 as INOUT, not pure OUT — pgjdbc's
        // CallableStatement silently drops an INOUT parameter from the actual call sent to Postgres
        // unless it's both set (setXxx) and registered (registerOutParameter); a bare setObject(x,
        // null) for the always-null positions also binds an untyped NULL that Postgres can't resolve
        // against p_doc_date (timestamp) / p_submit_to_user_poid (bigint). A plain CALL through
        // PreparedStatement.executeQuery() sidesteps CallableStatement's OUT/INOUT machinery
        // entirely — Postgres returns all 3 INOUT values as an ordinary one-row, 3-column ResultSet.
        // p_login_company_poid is bigint[] (confirmed via pg_get_function_arguments), not a scalar —
        // and the 3 trailing INOUT params are text/text/bigint, so bare NULL is untyped/unresolvable.
        try (Connection connection = DataSourceUtils.getConnection(dataSource);
             PreparedStatement ps = connection.prepareStatement(
                     "CALL PROC_GLOB_APPROVAL_ACTION(?,?,?,?,?,?,?,?,?,?,?,NULL::text,NULL::text,NULL::bigint)")) {

            ps.setLong(1, request.getLoginGroupPoid());
            ps.setArray(2, connection.createArrayOf("bigint", new Object[]{request.getCompanyPoid()}));
            ps.setLong(3, request.getUserPoid());
            ps.setString(4, request.getDocId());
            ps.setString(5, String.valueOf(request.getDocKeyPoid()));
            ps.setString(6, request.getApprovalAction().getCode());
            ps.setString(7, request.getComments() != null ? request.getComments() : "");
            ps.setString(8, request.getDocName() != null ? request.getDocName() : "");
            ps.setString(9, request.getDocRef());
            ps.setObject(10, request.getDocDate() != null ? java.sql.Timestamp.valueOf(request.getDocDate()) : null, Types.TIMESTAMP);
            ps.setObject(11, request.getUserRolePoid(), Types.BIGINT);

            String result;
            String approverUserPoid;
            Long approvalPoid;
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                result = rs.getString(1);
                approverUserPoid = rs.getString(2);
                approvalPoid = rs.getLong(3);
            }

            log.info("Approval action result: {}, approverUserPoid: {}, approvalPoid: {}",
                    result, approverUserPoid, approvalPoid);

            return ApprovalActionResponse.builder()
                    .status(result)
                    .message(result)
                    .approverUserPoid(approverUserPoid)
                    .approvalPoid(approvalPoid)
                    .success(result != null && result.contains("SUCCESS"))
                    .build();

        } catch (SQLException e) {
            log.error("Error executing approval action: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute approval action: " + e.getMessage());
        }
    }

    public ApprovalStatusResponse getApprovalStatus(Long loginGroupPoid, Long companyPoid, 
                                                     Long userPoid, String docId, Long docKeyPoid) {
        // Same fix as executeApprovalAction() above: plain CALL through PreparedStatement.executeQuery()
        // instead of CallableStatement's OUT/INOUT machinery. p_doc_date/p_submit_to_user_poid are
        // always null for a status check, so they're cast literals in the SQL text rather than bound
        // placeholders — no ambiguous untyped NULL to resolve. p_login_company_poid is bigint[], and
        // the 3 trailing INOUT params are text/text/bigint (also cast, not bare NULL).
        try (Connection connection = DataSourceUtils.getConnection(dataSource);
             PreparedStatement ps = connection.prepareStatement(
                     "CALL PROC_GLOB_APPROVAL_ACTION(?,?,?,?,?,?,?,?,?,NULL::timestamp,NULL::bigint,NULL::text,NULL::text,NULL::bigint)")) {

            ps.setLong(1, loginGroupPoid);
            ps.setArray(2, connection.createArrayOf("bigint", new Object[]{companyPoid}));
            ps.setLong(3, userPoid);
            ps.setString(4, docId);
            ps.setString(5, String.valueOf(docKeyPoid));
            ps.setString(6, "STATUS_CHECK");
            ps.setString(7, "");
            ps.setString(8, "");
            ps.setString(9, "");

            String result;
            String approverUserRoles;
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                result = rs.getString(1);
                approverUserRoles = rs.getString(2);
            }

            log.info("Approval status result: {}, approverUserRoles: {}", result, approverUserRoles);

            return parseApprovalStatus(result, approverUserRoles, userPoid.toString());

        } catch (SQLException e) {
            log.error("Error getting approval status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get approval status: " + e.getMessage());
        }
    }

    public ApprovalLogResponse getApprovalLog(Long loginGroupPoid, Long companyPoid,
                                               String docId, Long docKeyPoid) {
        // PROC_GLOB_APPROVAL_LOG's refcursor is an INOUT param at position 5 of 5 (last), not
        // first — same pgjdbc positional restriction as the rest of the REF_CURSOR family in this
        // migration (registerOutParameter only binds a REF_CURSOR correctly in the first position).
        // Calling it as a plain CALL query instead of CallableStatement sidesteps that entirely:
        // Postgres returns the cursor's name as an ordinary one-row ResultSet, then a separate
        // FETCH ALL FROM "<name>" reads the actual rows.
        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            // Postgres refcursors only live within their transaction. If this connection is already
            // bound to a Spring-managed transaction, autocommit is already off and committing here
            // ourselves would end that surrounding transaction early — so only manage it locally when
            // this call isn't already running inside one.
            boolean manageTx = !DataSourceUtils.isConnectionTransactional(connection, dataSource);
            if (manageTx) {
                connection.setAutoCommit(false);
            }

            String cursorName;
            // PROC_GLOB_APPROVAL_LOG has two overloads (company_poid as numeric vs numeric[]) —
            // a bare literal NULL for the refcursor position is untyped ("unknown"), and with two
            // otherwise-compatible candidates Postgres can't settle on one, so the whole call fails
            // to resolve. Casting it to the real type removes that ambiguity; the scalar bigint bound
            // to P_COMPANY_POID below still selects the scalar-numeric overload on its own.
            try (PreparedStatement ps = connection.prepareStatement("CALL PROC_GLOB_APPROVAL_LOG(?, ?, ?, ?, NULL::refcursor)")) {
                ps.setLong(1, loginGroupPoid);
                ps.setLong(2, companyPoid);
                ps.setString(3, docId);
                // p_doc_key_poid is numeric, not varchar — String.valueOf(...) here sent it as text
                // and broke overload resolution for both candidates.
                ps.setLong(4, docKeyPoid);
                try (java.sql.ResultSet crs = ps.executeQuery()) {
                    cursorName = crs.next() ? crs.getString(1) : null;
                }
            }

            ApprovalLogResponse response;
            if (cursorName == null) {
                response = ApprovalLogResponse.builder()
                        .logs(new java.util.ArrayList<>())
                        .columns(new java.util.ArrayList<>())
                        .build();
            } else {
                try (Statement fetchStmt = connection.createStatement();
                     java.sql.ResultSet rs = fetchStmt.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
                    java.sql.ResultSetMetaData metadata = rs.getMetaData();
                    int columnCount = metadata.getColumnCount();

                    List<Map<String, String>> logs = new java.util.ArrayList<>();
                    List<ApprovalLogResponse.ColumnInfo> columns = new java.util.ArrayList<>();

                    // Build columns
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = metadata.getColumnName(i);
                        String colWidth = null;

                        if (i < columnCount) {
                            int displaySize = metadata.getColumnDisplaySize(i);
                            colWidth = (displaySize < 80 ? 100 : displaySize + 30) + "px";
                        }

                        columns.add(ApprovalLogResponse.ColumnInfo.builder()
                                .columnName(colName)
                                .columnWidth(colWidth)
                                .build());
                    }

                    // Build rows
                    while (rs.next()) {
                        Map<String, String> row = new java.util.LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metadata.getColumnName(i), rs.getString(i));
                        }
                        logs.add(row);
                    }

                    response = ApprovalLogResponse.builder()
                            .logs(logs)
                            .columns(columns)
                            .build();
                }
            }

            if (manageTx) {
                connection.commit();
            }
            return response;

        } catch (SQLException e) {
            log.error("Error getting approval log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get approval log: " + e.getMessage());
        }
    }

    private ApprovalStatusResponse parseApprovalStatus(String result, String approverUserRoles, String loginUserPoid) {
        ApprovalStatusResponse.ApprovalStatusResponseBuilder builder = ApprovalStatusResponse.builder();
        
        builder.approverUsers(approverUserRoles != null ? approverUserRoles : "");
        builder.approvalMenuVisible(true);

        if (result == null || result.isEmpty()) {
            return builder.statusCode("ERROR").statusDetails("Approval status returned null").build();
        }

        String statusCode = "";
        String statusDetails = "";
        
        if (result.contains(":")) {
            String[] parts = result.split(":", 2);
            statusCode = parts[0].trim();
            statusDetails = parts.length > 1 ? parts[1].trim() : "";
        } else {
            statusCode = result.trim();
        }

        builder.statusCode(statusCode);
        builder.statusDetails(statusDetails);

        boolean isApprover = false;
        if (approverUserRoles != null && !approverUserRoles.isEmpty()) {
            List<String> approverList = Arrays.asList(approverUserRoles.split(","));
            isApprover = approverList.contains(loginUserPoid);
        }

        switch (statusCode) {
            case "APPROVAL_NOT_APPLICABLE":
                builder.color("Black").approvalMenuVisible(false);
                break;
            case "NOT_SUBMITTED":
                builder.color("Black").submitForApprovalEnabled(true);
                break;
            case "SUBMIT_FOR_APPROVAL":
                builder.color("Orange")
                       .recallForChangeEnabled(true)
                       .returnForCorrectionEnabled(isApprover)
                       .approveEnabled(isApprover)
                       .approveWithCommentsEnabled(isApprover)
                       .rejectEnabled(isApprover);
                break;
            case "SUBMIT_FOR_SPECIAL_APPROVAL":
                builder.color("Orange")
                       .recallForChangeEnabled(true)
                       .returnForCorrectionEnabled(isApprover)
                       .specialApproveEnabled(isApprover)
                       .rejectEnabled(isApprover);
                break;
            case "RECALL_FOR_CHANGE":
            case "RETURN_FOR_CORRECTION":
                builder.color("Grey").submitForApprovalEnabled(true);
                break;
            case "FINAL_APPROVAL_COMPLETED":
                builder.color("Green");
                break;
            case "REJECT":
                builder.color("Red");
                break;
            case "APPROVAL_CANCELLED":
                builder.color("Black").submitForApprovalEnabled(true);
                break;
            case "ERROR":
                builder.color("Red");
                break;
            default:
                builder.statusCode("UNKNOWN_STATUS");
        }

        return builder.build();
    }
}
