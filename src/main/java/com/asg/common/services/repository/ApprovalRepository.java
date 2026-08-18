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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
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
        String sql = "{ call PROC_GLOB_APPROVAL_ACTION(?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
        
        try (Connection connection = DataSourceUtils.getConnection(dataSource);
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, request.getLoginGroupPoid());
            cs.setLong(2, request.getCompanyPoid());
            cs.setLong(3, request.getUserPoid());
            cs.setString(4, request.getDocId());
            cs.setString(5, String.valueOf(request.getDocKeyPoid()));
            cs.setString(6, request.getApprovalAction().getCode());
            cs.setString(7, request.getComments() != null ? request.getComments() : "");
            cs.setString(8, request.getDocName() != null ? request.getDocName() : "");
            cs.setString(9, request.getDocRef());
            cs.setObject(10, request.getDocDate() != null ? java.sql.Timestamp.valueOf(request.getDocDate()) : null);
            cs.setObject(11, request.getUserRolePoid());
            cs.registerOutParameter(12, Types.VARCHAR);
            cs.registerOutParameter(13, Types.VARCHAR);
            cs.registerOutParameter(14, Types.NUMERIC);

            cs.execute();

            String result = cs.getString(12);
            String approverUserPoid = cs.getString(13);
            Long approvalPoid = cs.getLong(14);

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
        String sql = "{ call PROC_GLOB_APPROVAL_ACTION(?,?,?,?,?,?,?,?,?,?,?,?,?,?) }";
        
        try (Connection connection = DataSourceUtils.getConnection(dataSource);
             CallableStatement cs = connection.prepareCall(sql)) {

            cs.setLong(1, loginGroupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, userPoid);
            cs.setString(4, docId);
            cs.setString(5, String.valueOf(docKeyPoid));
            cs.setString(6, "STATUS_CHECK");
            cs.setString(7, "");
            cs.setString(8, "");
            cs.setString(9, "");
            cs.setObject(10, null);
            cs.setObject(11, null);
            cs.registerOutParameter(12, Types.VARCHAR);
            cs.registerOutParameter(13, Types.VARCHAR);
            cs.registerOutParameter(14, Types.NUMERIC);

            cs.execute();

            String result = cs.getString(12);
            String approverUserRoles = cs.getString(13);

            log.info("Approval status result: {}, approverUserRoles: {}", result, approverUserRoles);

            return parseApprovalStatus(result, approverUserRoles, userPoid.toString());

        } catch (SQLException e) {
            log.error("Error getting approval status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get approval status: " + e.getMessage());
        }
    }

    public ApprovalLogResponse getApprovalLog(Long loginGroupPoid, Long companyPoid, 
                                               String docId, Long docKeyPoid) {
        String sql = "{ call PROC_GLOB_APPROVAL_LOG(?,?,?,?,?) }";

        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            // Postgres refcursors only live within their transaction. If this connection is already
            // bound to a Spring-managed transaction, autocommit is already off and committing here
            // ourselves would end that surrounding transaction early — so only manage it locally when
            // this call isn't already running inside one.
            boolean manageTx = !DataSourceUtils.isConnectionTransactional(connection, dataSource);
            if (manageTx) {
                connection.setAutoCommit(false);
            }

            ApprovalLogResponse response;
            try (CallableStatement cs = connection.prepareCall(sql)) {
                cs.setLong(1, loginGroupPoid);
                cs.setLong(2, companyPoid);
                cs.setString(3, docId);
                cs.setString(4, String.valueOf(docKeyPoid));
                cs.registerOutParameter(5, Types.OTHER); // REF_CURSOR

                cs.execute();

                try (java.sql.ResultSet rs = (java.sql.ResultSet) cs.getObject(5)) {
                    if (rs == null) {
                        response = ApprovalLogResponse.builder()
                                .logs(new java.util.ArrayList<>())
                                .columns(new java.util.ArrayList<>())
                                .build();
                    } else {
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
