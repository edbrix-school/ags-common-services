package com.asg.common.services.repository;

import com.asg.common.lib.dto.GlobalTermsDto;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class GlobalTermsConditionRepositoryImpl implements GlobalTermsConditionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Override
    public void insertGlobalTerms(List<GlobalTermsInsertRequestDto> requestList) {

        if (requestList == null || requestList.isEmpty()) return;

        for (GlobalTermsInsertRequestDto dto : requestList) {
            try {
                StoredProcedureQuery query = entityManager
                        .createStoredProcedureQuery("PROC_GLOB_TERMS_INSERT");

                query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_TERMS_POID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_ROW_SEQ_NO", Long.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_CLAUSE_NO", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("P_CLAUSE_DETAILS", String.class, ParameterMode.IN);

                query.setParameter("P_GROUP_POID", dto.getGroupPoid());
                query.setParameter("P_COMPANY_POID", dto.getCompanyPoid());
                query.setParameter("P_DOC_ID", dto.getDocId());
                query.setParameter("P_DOC_KEY_POID", dto.getDocKeyPoid());
                query.setParameter("P_LOGIN_USER_POID", dto.getLoginUserPoid());
                query.setParameter("P_TERMS_POID", dto.getTermsPoid());
                query.setParameter("P_DET_ROW_ID", dto.getDetRowId());
                query.setParameter("P_ROW_SEQ_NO", dto.getRowSeqNo());
                query.setParameter("P_CLAUSE_NO", dto.getClauseNo());
                query.setParameter("P_CLAUSE_DETAILS", dto.getClauseDetails());

                query.execute();
            } catch (Exception e) {
                log.error("Error inserting global terms for DOC_ID {} and DOC_KEY_POID {}: {}",
                        dto.getDocId(), dto.getDocKeyPoid(), e.getMessage());
                throw new RuntimeException("Failed to insert global terms", e);
            }
        }
    }


    @Override
    public void deleteGlobalTerms(
            Long groupPoid,
            Long companyPoid,
            String documentId,
            Long docKeyPoid,
            Long userPoid
    ) {
        try {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("PROC_GLOB_TERMS_DELETE");

            // Register procedure parameters
            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);

            // Set parameter values
            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_ID", documentId);
            query.setParameter("P_DOC_KEY_POID", docKeyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);

            query.execute();

            log.info("PROC_GLOB_TERMS_DELETE executed successfully for DOC_ID={} and DOC_KEY_POID={}",
                    documentId, docKeyPoid);

        } catch (Exception e) {
            log.error("Error executing PROC_GLOB_TERMS_DELETE: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete Global Terms: " + e.getMessage(), e);
        }
    }

    @Override
    public GlobalTermsResponseDto loadGlobalTermsList(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long docKeyPoid,
            Long termsPoid) {

        // PROC_GLOB_TERMS_LOADLIST's 2nd parameter (company) is numeric[], not a scalar — the JPA
        // StoredProcedureQuery registered it as Long.class, which never matches the real signature.
        // Its refcursor OUT param is also 6th of 7, not first, hitting the same pgjdbc positional
        // restriction as the rest of the REF_CURSOR family in this migration. Raw JDBC with an
        // explicit array bind and a plain CALL fixes both at once.
        List<GlobalTermsDto> termsList;
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                String cursorName;
                try (PreparedStatement ps = con.prepareStatement(
                        "CALL PROC_GLOB_TERMS_LOADLIST(?, ?, ?, ?, ?, NULL, NULL)")) {
                    ps.setLong(1, groupPoid);
                    ps.setArray(2, con.createArrayOf("numeric", new Object[]{companyPoid}));
                    ps.setString(3, docId);
                    // docKeyPoid/termsPoid are both optional at the controller — setLong(index, null)
                    // NPEs unboxing a null Long, so fall back to setNull when either is absent.
                    if (docKeyPoid != null) {
                        ps.setLong(4, docKeyPoid);
                    } else {
                        ps.setNull(4, java.sql.Types.NUMERIC);
                    }
                    if (termsPoid != null) {
                        ps.setLong(5, termsPoid);
                    } else {
                        ps.setNull(5, java.sql.Types.NUMERIC);
                    }
                    try (ResultSet crs = ps.executeQuery()) {
                        cursorName = crs.next() ? crs.getString(1) : null;
                    }
                }

                termsList = new ArrayList<>();
                if (cursorName != null) {
                    try (Statement fetchStmt = con.createStatement();
                         ResultSet rs = fetchStmt.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
                        termsList = mapToGlobalTerms(rs);
                    }
                }
                con.commit();
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Error executing PROC_GLOB_TERMS_LOADLIST: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load global terms list: " + e.getMessage(), e);
        }

        GlobalTermsResponseDto response = new GlobalTermsResponseDto();
        response.setTermsList(termsList);

        return response;
    }

    private List<GlobalTermsDto> mapToGlobalTerms(ResultSet rs) {
        List<GlobalTermsDto> list = new ArrayList<>();

        try {
            while (rs.next()) {
                GlobalTermsDto dto = new GlobalTermsDto();
                
                // Try TERMS_POID first, fallback to REF_TERMS_POID
                try {
                    dto.setRefTermsPoid(rs.getLong("TERMS_POID"));
                } catch (SQLException e) {
                    dto.setRefTermsPoid(rs.getLong("REF_TERMS_POID"));
                }
                
                dto.setDetRowId(rs.getLong("DET_ROW_ID"));
                dto.setClauseNo(rs.getString("CLAUSE_NO"));
                dto.setClauseDetails(rs.getString("CLAUSE_DETAILS"));
                list.add(dto);
            }
        } catch (SQLException e) {
            log.error("Error mapping ResultSet for PROC_GLOB_TERMS_LOADLIST", e);
            throw new RuntimeException(e);
        }

        return list;
    }

    @Override
    public String createPoFromRfq(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            Long poPoid,
            String supplierPoid,
            String rfqPoid) {

        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("PROC_AP_PO_CREATE_FROM_RFQ");

            // Register IN parameters
            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_PO_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_SUPPLIER_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RFQ_POID", String.class, ParameterMode.IN);

            // OUT parameter
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            // Setting parameters
            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_PO_POID", poPoid);
            query.setParameter("P_SUPPLIER_POID", supplierPoid);
            query.setParameter("P_RFQ_POID", rfqPoid);

            // Execute
            query.execute();

            // Read OUT result
            String result = (String) query.getOutputParameterValue("P_RESULT");

            log.info("PROC_AP_PO_CREATE_FROM_RFQ executed successfully. Result: {}", result);

            return result;

        } catch (Exception e) {
            log.error("Error executing PROC_AP_PO_CREATE_FROM_RFQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create PO from RFQ: " + e.getMessage(), e);
        }
    }
}
