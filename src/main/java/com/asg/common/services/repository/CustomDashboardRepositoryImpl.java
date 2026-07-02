package com.asg.common.services.repository;

import com.asg.common.services.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
public class CustomDashboardRepositoryImpl implements CustomDashboardRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserApprovalSubmitStatusDto> getUserApprovalSubmitStatus(
            String userPoid,
            String status,
            Date fromDate,
            Date toDate) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GET_USR_APPR_SUBMIT_STAT");

        query.registerStoredProcedureParameter("P_USER_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FROM_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TO_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_USER_POID", userPoid);
        query.setParameter("P_STATUS", status);
        query.setParameter("P_FROM_DATE", fromDate);
        query.setParameter("P_TO_DATE", toDate);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
        return mapResultSetForSubmit(rs);
    }

    private List<UserApprovalSubmitStatusDto> mapResultSetForSubmit(ResultSet rs) {
        List<UserApprovalSubmitStatusDto> resultList = new ArrayList<>();

        try {
            while (rs.next()) {
                UserApprovalSubmitStatusDto dto = new UserApprovalSubmitStatusDto();

                dto.setSubmittedBy(rs.getString("SUBMITTED_BY"));
                dto.setSubmittedByName(rs.getString("SUBMITTED_BY_NAME"));
                dto.setDocName(rs.getString("DOC_NAME"));
                dto.setDocShortName(getStringIfPresent(rs, "DOC_SHORT_NAME"));
                dto.setRouteName(getStringIfPresent(rs, "ROUTE_NAME"));
                dto.setDocRef(getStringIfPresent(rs, "DOC_REF"));
                dto.setDocId(rs.getString("DOC_ID"));
                dto.setDocKeyPoid(getDocKeyPoidAsString(rs));
                dto.setCurrentDocStatus(rs.getString("CURRENT_DOC_STATUS"));
                dto.setNextApproverName(rs.getString("NEXT_APPROVER_NAME"));
                dto.setSubmitDate(rs.getTimestamp("SUBMIT_DATE"));
                dto.setStatus(rs.getString("STATUS"));

                resultList.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping user approval status result", e);
        }

        return resultList;
    }

    @Override
    public List<RecentDocumentDto> getRecentDocumentList(String userId, Long userPoid) {
        log.info("Calling PROC_GLOB_RECENT_DOC_LIST for userPoid: {}", userPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_RECENT_DOC_LIST");

        query.registerStoredProcedureParameter("P_USER_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_REC_DOC_OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_USER_ID", userId);
        query.setParameter("P_USER_POID", userPoid);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("P_REC_DOC_OUTDATA");
        List<RecentDocumentDto> result = mapResultSetForRecentDocument(rs);
        log.info("PROC_GLOB_RECENT_DOC_LIST returned {} records", result.size());
        return result;
    }

    private List<RecentDocumentDto> mapResultSetForRecentDocument(ResultSet rs) {
        List<RecentDocumentDto> list = new ArrayList<>();

        try {
            while (rs.next()) {
                RecentDocumentDto dto = new RecentDocumentDto();

                dto.setDocType(rs.getString("DOC_TYPE"));
                String shortName = getStringIfPresent(rs, "DOC_SHORT_NAME");
                if (shortName == null || shortName.isBlank()) {
                    shortName = rs.getString("DOC_TYPE");
                }
                dto.setDocShortName(shortName);
                dto.setDocName(getStringIfPresent(rs, "DOC_NAME"));
                dto.setRouteName(getStringIfPresent(rs, "ROUTE_NAME"));
                dto.setDocId(getStringIfPresent(rs, "DOC_ID"));
                dto.setDocKeyPoid(getDocKeyPoidAsString(rs));
                dto.setDocDate(rs.getString("DOC_DATE"));
                dto.setDocRef(rs.getString("DOC_REF"));

                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping recent document list result", e);
        }

        return list;
    }


    @Override
    public List<FavoriteMenuDto> getFavoriteMenuList(String userId, Long userPoid) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_FAV_MENU_LIST");

        query.registerStoredProcedureParameter("P_USER_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_USER_ID", userId);
        query.setParameter("P_USER_POID", userPoid);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

        return mapResultSetFavList(rs);
    }

    private List<FavoriteMenuDto> mapResultSetFavList(ResultSet rs) {
        List<FavoriteMenuDto> list = new ArrayList<>();

        try {
            while (rs.next()) {
                FavoriteMenuDto dto = new FavoriteMenuDto();

                dto.setMenuId(rs.getLong("MENU_ID"));
                dto.setMenuName(rs.getString("MENU_NAME"));
                dto.setMenuLevel(rs.getLong("MENU_LEVEL"));
                dto.setMenuGroup(rs.getString("MENU_GROUP"));
                dto.setTaskflowUrl(rs.getString("TASKFLOW_URL"));
                dto.setDocType(rs.getString("DOC_TYPE"));
                dto.setModuleId(rs.getLong("MODULE_ID"));
                dto.setCatSeqNo(rs.getLong("CAT_SEQ_NO"));
                dto.setDocSeqNo(rs.getLong("DOC_SEQ_NO"));

                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping favorite menu", e);
        }

        return list;
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (column.equalsIgnoreCase(meta.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ApprovalPendingDto> getApprovalPendingList(
            String userPoid,
            String status,
            Date fromDate,
            Date toDate
    ) {
        log.info("Calling PROC_GLOB_APPROVAL_PENDING_V2 with params - userPoid: {}, status: {}, fromDate: {}, toDate: {}", 
                userPoid, status, fromDate, toDate);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_APPROVAL_PENDING_V2");

        query.registerStoredProcedureParameter("P_USER_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FROM_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TO_DATE", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_USER_POID", userPoid);
        query.setParameter("P_STATUS", status);
        query.setParameter("P_FROM_DATE", fromDate);
        query.setParameter("P_TO_DATE", toDate);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

        List<ApprovalPendingDto> result = mapResultSetForApproval(rs);
        log.info("PROC_GLOB_APPROVAL_PENDING_V2 returned {} records", result.size());
        
        return result;
    }

    private List<ApprovalPendingDto> mapResultSetForApproval(ResultSet rs) {
        List<ApprovalPendingDto> list = new ArrayList<>();

        try {
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                ApprovalPendingDto dto = new ApprovalPendingDto();

                long docKey = rs.getLong("DOC_KEY_POID");
                dto.setDocKeyPoid(rs.wasNull() ? null : docKey);
                dto.setDocId(rs.getString("DOC_ID"));
                dto.setDocName(rs.getString("DOC_NAME"));
                dto.setDocShortName(getStringIfPresent(rs, "DOC_SHORT_NAME"));
                dto.setRouteName(getStringIfPresent(rs, "ROUTE_NAME"));
                dto.setDocRef(getStringIfPresent(rs, "DOC_REF"));
                dto.setActionType(rs.getString("ACTION_TYPE"));
                dto.setDocDate(rs.getDate("DOC_DATE"));
                
                java.sql.Timestamp timestamp = rs.getTimestamp("ACTIONED_DATETIME");
                if (timestamp != null) {
                    dto.setActionedDatetime(timestamp.toLocalDateTime());
                }

                list.add(dto);
                
                if (rowCount <= 5) {
                    log.debug("Row {}: docKeyPoid={}, docId={}, docName={}, actionType={}, actionedDatetime={}",
                            rowCount, dto.getDocKeyPoid(), dto.getDocId(), dto.getDocName(), 
                            dto.getActionType(), dto.getActionedDatetime());
                }
            }
            log.info("Total rows mapped from ResultSet: {}", rowCount);

        } catch (SQLException e) {
            log.error("Error mapping approval pending list", e);
            throw new RuntimeException("Error mapping approval pending list", e);
        }

        return list;
    }

    private String getStringIfPresent(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            // Column may not exist yet in the stored procedure output (backward compatible fallback).
            return null;
        }
    }

    /**
     * Reads DOC_KEY_POID whether the driver returns NUMBER or STRING (Oracle / ref cursor).
     */
    private String getDocKeyPoidAsString(ResultSet rs) throws SQLException {
        try {
            Object v = rs.getObject("DOC_KEY_POID");
            if (v == null) {
                return null;
            }
            return v.toString();
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public List<WeeklyTransactionDto> getWeeklyTransactions(
            String loginUserPoid,
            String periodFrom,
            String periodTo
    ) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DASH_USER_WEEKLY_TRANS");

        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PERIOD_FROM", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PERIOD_TO", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
        query.setParameter("P_PERIOD_FROM", periodFrom);
        query.setParameter("P_PERIOD_TO", periodTo);

        query.execute();

        ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");

        return mapResultSet(rs);
    }

    private List<WeeklyTransactionDto> mapResultSet(ResultSet rs) {
        List<WeeklyTransactionDto> list = new ArrayList<>();
        if (rs == null) return list;

        try {
            while (rs.next()) {

                WeeklyTransactionDto dto = new WeeklyTransactionDto();

                dto.setDocShortName(rs.getString("DOC_SHORT_NAME"));
                dto.setDocActionType(rs.getString("DOC_ACTION_TYPE"));
                dto.setDocCount(rs.getLong("DOC_COUNT"));

                list.add(dto);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error mapping weekly transaction results", e);
        }

        return list;
    }
}
