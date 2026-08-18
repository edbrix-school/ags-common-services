package com.asg.common.services.repository;

import com.asg.common.lib.dto.BillwiseBreakupDto;
import com.asg.common.lib.dto.CostBreakupDto;
import com.asg.common.lib.dto.LedgerEntryDto;
import com.asg.common.lib.dto.VatBreakupDto;
import com.asg.common.lib.dto.response.GlPostingViewResponseDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LovDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import com.asg.common.lib.exception.ValidationException;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class GlPostingRepository {

    private final DataSource dataSource;
    private final LovDataService lovDataService;

    public GlPostingRepository(DataSource dataSource, LovDataService lovDataService) {
        this.dataSource = dataSource;
        this.lovDataService = lovDataService;
    }

    public GlPostingViewResponseDto getGlPostings(String docId, Long transactionPoid) throws SQLException {
        Long companyPoid = UserContext.getCompanyPoid();
        Long groupPoid = UserContext.getGroupPoid();
        log.info("companyPoid : {}, groupPoid : {} docId : {}, transactionPoid : {} ", companyPoid, groupPoid, docId, transactionPoid);

        GlPostingViewResponseDto response = new GlPostingViewResponseDto();

        try (Connection connection = dataSource.getConnection()) {
            // Postgres refcursors only live for the duration of the transaction that opened them
            connection.setAutoCommit(false);

            try (CallableStatement cs = connection.prepareCall(
                    "{call PROC_GL_POSTING_VIEW_LOAD_V2(?, ?, ?, ?, ?, ?, ?, ?)}")) {

                // Set input parameters
                cs.setLong(1, groupPoid);        // P_GROUP_POID
                cs.setLong(2, companyPoid);      // P_COMPANY_POID
                cs.setString(3, docId);          // P_DOC_ID
                cs.setLong(4, transactionPoid);  // P_TRANSACTION_POID

                // Register output parameters (cursors)
                cs.registerOutParameter(5, Types.OTHER);  // Ledger Entries
                cs.registerOutParameter(6, Types.OTHER);  // Billwise Breakup
                cs.registerOutParameter(7, Types.OTHER);  // Cost Breakup
                cs.registerOutParameter(8, Types.OTHER);  // VAT Breakup

                cs.execute();

                // Process Ledger Entries (cursor 5)
                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    response.setLedgerEntries(mapToLedgerEntries(rs));
                }

                // Process Billwise Breakup (cursor 6)
                try (ResultSet rs = (ResultSet) cs.getObject(6)) {
                    response.setBillwiseBreakup(mapToBillwiseBreakup(rs));
                }

                // Process Cost Breakup (cursor 7)
                try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                    response.setCostBreakup(mapToCostBreakup(rs));
                }

                // Process VAT Breakup (cursor 8)
                try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                    response.setVatBreakup(mapToVatBreakup(rs));
                }
            }
            connection.commit();

        } catch (SQLException e) {
            log.error(" error : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        return response;
    }


    private List<LedgerEntryDto> mapToLedgerEntries(ResultSet rs) throws SQLException {
        List<LedgerEntryDto> entries = new ArrayList<>();

        while (rs.next()) {
            LedgerEntryDto dto = new LedgerEntryDto();
            dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
            dto.setDocRef(rs.getString("DOC_REF"));
            dto.setNarration(rs.getString("NARRATION"));
            dto.setCompanyCode(rs.getString("COMPANY_CODE"));
            dto.setGlAcType(rs.getString("GL_AC_TYPE"));
            dto.setGlCode(rs.getString("GL_CODE"));
            dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
            dto.setDrAmt(rs.getBigDecimal("DR_AMT"));
            dto.setCrAmt(rs.getBigDecimal("CR_AMT"));
            dto.setPostedBy(rs.getString("POSTED_BY"));
            dto.setPostedDate(rs.getTimestamp("POSTED_DATE").toLocalDateTime());
            entries.add(dto);
        }

        return entries;
    }

    private List<BillwiseBreakupDto> mapToBillwiseBreakup(ResultSet rs) throws SQLException {
        List<BillwiseBreakupDto> entries = new ArrayList<>();

        while (rs.next()) {
            BillwiseBreakupDto dto = new BillwiseBreakupDto();
            dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
            dto.setDocRef(rs.getString("DOC_REF"));
            dto.setGlCode(rs.getString("GL_CODE"));
            dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
            dto.setBillRefType(rs.getString("BILL_REF_TYPE"));
            dto.setBillRef(rs.getString("BILL_REF"));
            dto.setBillDueDate(rs.getDate("BILL_DUE_DATE"));
            dto.setRemarks(rs.getString("REMARKS"));
            dto.setDrAmt(rs.getBigDecimal("DR_AMT"));
            dto.setCrAmt(rs.getBigDecimal("CR_AMT"));
            dto.setGlCompany(rs.getString("GL_COMPANY"));
            entries.add(dto);
        }

        return entries;
    }

    private List<CostBreakupDto> mapToCostBreakup(ResultSet rs) throws SQLException {
        List<CostBreakupDto> entries = new ArrayList<>();

        while (rs.next()) {
            CostBreakupDto dto = new CostBreakupDto();
            dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
            dto.setDocRef(rs.getString("DOC_REF"));
            dto.setGlCode(rs.getString("GL_CODE"));
            dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
            dto.setCostGroup(rs.getString("COST_GROUP"));
            String costPoidStr = rs.getString("COST_POID");
            dto.setCostPoid(StringUtils.isNotBlank(costPoidStr) ? costPoidStr.trim() : null);
            dto.setAmt(rs.getBigDecimal("AMT"));
            dto.setGlCompany(rs.getString("GL_COMPANY"));
            entries.add(dto);
        }

        return entries;
    }

    private List<VatBreakupDto> mapToVatBreakup(ResultSet rs) throws SQLException {
        List<VatBreakupDto> entries = new ArrayList<>();

        while (rs.next()) {
            VatBreakupDto dto = new VatBreakupDto();
            dto.setTransactionDate(convertToLocalDate(rs.getDate("TRANSACTION_DATE")));
            dto.setDocRef(rs.getString("DOC_REF"));
            dto.setGlCode(rs.getString("GL_CODE"));
            dto.setGlDescription(rs.getString("GL_DESCRIPTION"));
            dto.setTaxName(rs.getString("TAX_NAME"));
            dto.setTaxBaseAmount(rs.getBigDecimal("TAX_BASE_AMOUNT"));
            dto.setTaxAmount(rs.getBigDecimal("TAX_AMOUNT"));
            dto.setAmt(rs.getBigDecimal("AMT"));
            entries.add(dto);
        }

        return entries;
    }

    private java.time.LocalDate convertToLocalDate(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    public String glreposting(int loginGroupPoid, int loginCompanyPoid, int loginUserPoid, String docId, int transactionPoid, String docRef) {
        String outPutMessage = StringUtils.EMPTY;
        try (Connection connection = dataSource.getConnection();
             CallableStatement cs = connection.prepareCall(
                     " { call PROC_GL_LEDGER_POSTING_MAIN(?, ?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setInt(1, loginGroupPoid);
            cs.setInt(2, loginCompanyPoid);
            cs.setInt(3, loginUserPoid);
            cs.setString(4, docId);
            cs.setInt(5, transactionPoid);
            cs.setString(6, docRef);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.setString(8, "N");

            cs.execute();
            outPutMessage = cs.getString(7);

            if (outPutMessage != null && outPutMessage.contains("ERROR")) {
                log.error("GL posting failed: {}", outPutMessage);
                throw new ValidationException("GL Posting failed: " + outPutMessage);
            }
            log.info("GL posting completed successfully: {}", outPutMessage);
        } catch (SQLException e) {
            log.error(" error : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        return outPutMessage;
    }

}
