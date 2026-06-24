package com.asg.common.services.service.impl;

import com.asg.common.lib.dto.GLMasterCommonDTO;
import com.asg.common.lib.dto.GLMasterDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.ReconcileResultDto;
import com.asg.common.lib.dto.StockInfoDto;
import com.asg.common.lib.dto.TaxMasterDto;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import com.asg.common.lib.dto.response.StockDetailsResponse;
import com.asg.common.lib.dto.response.TaxCalculationResponseDto;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.services.client.TaxServiceClient;
import com.asg.common.services.client.GLMasterServiceClient;
import com.asg.common.services.client.StockServiceClient;
import com.asg.common.services.repository.GlobalTermsConditionRepository;
import com.asg.common.services.dto.AddressDetailsListResponseDto;
import com.asg.common.services.dto.AddressDetailsResponseDto;
import com.asg.common.services.dto.AddressPoidResponseDto;
import com.asg.common.services.dto.CurrencyRateResponseDto;
import com.asg.common.services.service.CommonDataService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommonDataServiceImpl implements CommonDataService {

    private final GLMasterServiceClient glMasterServiceClient;
    private final TaxServiceClient taxServiceClient;
    private final StockServiceClient stockServiceClient;
    private final GlobalTermsConditionRepository globalTermsConditionRepository;
    private final LovDataService lovService;
    private final LoggingService loggingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public GLMasterCommonDTO getGLMasterDetails(Long glPoid) {
        GLMasterDto gl = glMasterServiceClient.getGLMaster(glPoid);

        GLMasterCommonDTO dto = new GLMasterCommonDTO();
        dto.setGlCode(gl.getGlCode());
        dto.setGlDescription(gl.getGlDescription());
        dto.setCostGroup(gl.getCostGroup());
        dto.setBillwise(gl.getBillwise());
        dto.setPrepaymentLedger(gl.getPrepaymentLedger());
        dto.setInterCompanyAc(gl.getInterCompanyAc());
        dto.setControlAcNature(gl.getControlAcNature());
        dto.setGlAcType(gl.getGlAcType());
        return dto;
    }


    private TaxMasterDto getTaxMaster(Long taxPoid) {
        return taxServiceClient.getTaxMaster(taxPoid);
    }

    public TaxCalculationResponseDto calculateTaxForPettyCash(Long taxPoid, Double drAmt) {

        if (taxPoid == null || drAmt == null) {
            throw new RuntimeException("Tax Poid and Dr Amount are required");
        }

        TaxMasterDto taxMaster = getTaxMaster(taxPoid);

        Double taxPercentage = taxMaster.getPercentage();

        Double taxAmt = BigDecimal.valueOf((drAmt * taxPercentage) / 100.0)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        Double totalAmt = BigDecimal.valueOf(drAmt + taxAmt)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        return TaxCalculationResponseDto.builder()
                .drAmt(drAmt)
                .taxPercentage(taxPercentage)
                .taxAmount(taxAmt)
                .totalAmount(totalAmt)
                .build();
    }

    @Override
    @Transactional
    public void insertGlobalTerms(List<GlobalTermsInsertRequestDto> requestList) {

        if (requestList == null) {
            throw new ValidationException("No Global Terms entries provided");
        }

        // Handle empty list — user deleted all rows (legacy: deleteCustomChanges)
        if (requestList.isEmpty()) {
            return;
        }

        GlobalTermsInsertRequestDto first = requestList.get(0);

        if (first.getTermsPoid() == null || first.getTermsPoid() == 0) {
            throw new ValidationException("No Terms and Conditions Template selected...");
        }

        if (first.getCompanyPoid() == null ||
                first.getDocId() == null ||
                first.getDocKeyPoid() == null ||
                first.getDetRowId() == null) {

            throw new ValidationException(
                    "Missing mandatory fields: groupPoid, companyPoid, docId, docKeyPoid, loginUserPoid, termsPoid"
            );
        }

        globalTermsConditionRepository.insertGlobalTerms(requestList);

        loggingService.createLogSummaryEntry(
                first.getDocId(),
                first.getDocKeyPoid().toString(),
                "Terms and Conditions modified..."
        );
    }

    @Override
    public void deleteGlobalTerms(
            Long groupPoid,
            Long companyPoid,
            String documentId,
            Long docKeyPoid,
            Long userPoid
    ) {
        globalTermsConditionRepository.deleteGlobalTerms(
                groupPoid,
                companyPoid,
                documentId,
                docKeyPoid,
                userPoid
        );
        loggingService.createLogSummaryEntry(documentId, docKeyPoid.toString(),
                "Custom Terms and Conditions deleted...");
    }

    @Override
    public GlobalTermsResponseDto loadGlobalTermsList(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long docKeyPoid,
            Long termsPoid
    ) {
        return globalTermsConditionRepository.loadGlobalTermsList(
                groupPoid,
                companyPoid,
                docId,
                docKeyPoid,
                termsPoid
        );
    }

    @Override
    public ReconcileResultDto fetchReconDate(
            String docId,
            Long docKeyPoid) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_DEBIT_PAYMENT_RECON_DATE");

        // Register IN parameters
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, jakarta.persistence.ParameterMode.IN);

        // Register OUT cursor
        query.registerStoredProcedureParameter("OUTDATA", void.class, jakarta.persistence.ParameterMode.REF_CURSOR);

        // Set input values
        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_DOC_KEY_POID", docKeyPoid);

        // Execute SP
        query.execute();

        Object cursor = query.getOutputParameterValue("OUTDATA");

        return mapCursorToDto(cursor);
    }

    private ReconcileResultDto mapCursorToDto(Object cursor) {

        try {
            ResultSet rs = (ResultSet) cursor;

            if (rs.next()) {
                return new ReconcileResultDto(
                        rs.getString("RECONCILE_DATE"),
                        rs.getString("HOLD")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading cursor", e);
        }

        return new ReconcileResultDto(null, null);
    }

    @Override
    public CurrencyRateResponseDto getCurrencyRate(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String docId,
            Long docKeyPoid,
            String currencyCode,
            String parameters
    ) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_GLOB_CURRENCY_GETRATE_V2");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CURRENCY_CODE", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_PARAMETERS", String.class, jakarta.persistence.ParameterMode.IN);

        query.registerStoredProcedureParameter("P_CURRENCY_RATE", Double.class, jakarta.persistence.ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_CURRENCY_DECIMALS", Integer.class, jakarta.persistence.ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
        query.setParameter("P_LOGIN_USER_POID", userPoid);
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_DOC_KEY_POID", docKeyPoid);
        query.setParameter("P_CURRENCY_CODE", currencyCode);
        query.setParameter("P_PARAMETERS", parameters);
        query.execute();

        Object rate = query.getOutputParameterValue("P_CURRENCY_RATE");
        Object decimals = query.getOutputParameterValue("P_CURRENCY_DECIMALS");
        Double finalRate = (rate != null) ? ((Number) rate).doubleValue() : null;
        Integer finalDecimals = (decimals != null) ? ((Number) decimals).intValue() : null;

        return CurrencyRateResponseDto.builder()
                .currencyCode(currencyCode)
                .rate(finalRate)
                .currencyDecimals(finalDecimals)
                .build();
    }

    @Override
    public StockDetailsResponse getStockDetails(Long stockPoid) {
        StockInfoDto stock = stockServiceClient.getStockInfo(stockPoid);

        LovGetListDto stockLov = lovService.getDetailsByPoidAndLovName(
                stock.getStockPoid(),
                "STOCK_MASTER"
        );

        LovGetListDto unitLov = lovService.getDetailsByPoidAndLovName(
                stock.getStockUnitPoid(),
                "STOCK_UNIT"
        );

        LovGetListDto taxLov = lovService.getDetailsByPoidAndLovName(
                stock.getTaxPoid(),
                "INPUT_TAX_MASTER"
        );

        return StockDetailsResponse.builder()
                .stockPoid(stock.getStockPoid())
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .stockName2(stock.getStockName2())
                .stockDtl(stockLov)
                .stockUnitPoid(stock.getStockUnitPoid())
                .unitDtl(unitLov)
                .inputTaxPoid(stock.getTaxPoid())
                .taxDtl(taxLov)
                .stockCost(stock.getStockCost())
                .remarks(null)
                .build();
    }

    @Override
    public String createPoFromRfq(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            Long poPoid,
            String supplierPoid,
            String rfqPoid
    ) {
        return globalTermsConditionRepository.createPoFromRfq(
                loginGroupPoid,
                loginUserPoid,
                loginCompanyPoid,
                poPoid,
                supplierPoid,
                rfqPoid
        );
    }

    @Override
    public AddressPoidResponseDto fetchAddressPoidByAssociatedData(
            Long associatedAddressPoid,
            String associatedAddressType
    ) {
        // Input validation
        if (associatedAddressPoid == null) {
            throw new ValidationException("Associated Address POID is required");
        }
        
        if (associatedAddressType == null || associatedAddressType.trim().isEmpty()) {
            associatedAddressType = "CUSTOMER"; // Default value
        }

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_ADDRESS_GET_ASSOC_DET");

        query.registerStoredProcedureParameter("P_ASSOCIATED_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ASSOCIATED_TYPE", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ADDRESS_MASTER_POID", Long.class, jakarta.persistence.ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_STATUS", String.class, jakarta.persistence.ParameterMode.OUT);

        query.setParameter("P_ASSOCIATED_POID", associatedAddressPoid);
        query.setParameter("P_ASSOCIATED_TYPE", associatedAddressType.toUpperCase());

        try {
            query.execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute address lookup procedure: " + e.getMessage(), e);
        }

        Object addressMasterPoidObj = query.getOutputParameterValue("P_ADDRESS_MASTER_POID");
        String status = (String) query.getOutputParameterValue("P_STATUS");

        // Handle procedure errors
        if (status != null && status.startsWith("ERROR")) {
            throw new ValidationException("Address lookup failed: " + status);
        }

        // Handle case where no address is found
        if (addressMasterPoidObj == null) {
            if (status != null && status.contains("no address")) {
                // This is expected for entities without addresses
                return new AddressPoidResponseDto(null);
            } else {
                throw new ValidationException("No address found for the given associated address POID: " + associatedAddressPoid);
            }
        }

        Long addressMasterPoid;
        try {
            addressMasterPoid = ((Number) addressMasterPoidObj).longValue();
        } catch (Exception e) {
            throw new RuntimeException("Invalid address master POID format returned from database", e);
        }

        return new AddressPoidResponseDto(addressMasterPoid);
    }

    @Override
    public AddressDetailsListResponseDto fetchAddressByMasterPoid(
            Long addressMasterPoid,
            BigDecimal addressPoid
    ) {
        // At least one identifier must be supplied
        if (addressMasterPoid == null && addressPoid == null) {
            throw new ValidationException("Either Address Master POID or Address POID must be provided");
        }

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_ADDRESS_GET_DETAILS_ALL");

        query.registerStoredProcedureParameter("P_ADDRESS_MASTER_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ADDRESS_POID", BigDecimal.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, jakarta.persistence.ParameterMode.REF_CURSOR);

        // Pass both params as-is — the procedure checks P_ADDRESS_POID first (IF branch),
        // then falls through to P_ADDRESS_MASTER_POID (ELSIF branch).
        query.setParameter("P_ADDRESS_MASTER_POID", addressMasterPoid);
        query.setParameter("P_ADDRESS_POID", addressPoid);

        try {
            query.execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute address details procedure: " + e.getMessage(), e);
        }

        Object cursor = query.getOutputParameterValue("OUTDATA");

        if (cursor == null) {
            throw new RuntimeException("No data returned from address details procedure");
        }

        List<AddressDetailsResponseDto> addressDetailsList = mapAddressCursorToList(cursor);

        // Check if any address details were found
        if (addressDetailsList.isEmpty()) {
            String identifier = addressPoid != null
                    ? "Address POID: " + addressPoid.toPlainString()
                    : "Address Master POID: " + addressMasterPoid;
            throw new ValidationException("No address details found for " + identifier);
        }

        Set<String> allowedTypes = Set.of("MAIN", "OPERATION", "SALES", "CAN", "FIN");
        Map<String, LovGetListDto> typelovMap = Map.of(
                "MAIN",      new LovGetListDto(null, "MAIN",      "Main",                 null, null, 1, null),
                "OPERATION", new LovGetListDto(null, "OPERATION", "Operation",            null, null, 2, null),
                "SALES",     new LovGetListDto(null, "SALES",     "Sales",                null, null, 3, null),
                "CAN",       new LovGetListDto(null, "CAN",       "Cargo Arrival Notice", null, null, 4, null),
                "FIN",       new LovGetListDto(null, "FIN",       "Finance",              null, null, 5, null)
        );

        addressDetailsList = addressDetailsList.stream()
                .filter(a -> a.getAddressType() != null && allowedTypes.contains(a.getAddressType().toUpperCase()))
                .collect(Collectors.toList());

        addressDetailsList.forEach(a -> a.setAddressTypeDet(typelovMap.get(a.getAddressType().toUpperCase())));

        return new AddressDetailsListResponseDto(addressDetailsList);
    }

    private List<AddressDetailsResponseDto> mapAddressCursorToList(Object cursor) {
        List<AddressDetailsResponseDto> addressList = new ArrayList<>();

        try {
            ResultSet rs = (ResultSet) cursor;

            while (rs.next()) {
                AddressDetailsResponseDto address = new AddressDetailsResponseDto();
                
                address.setAddressMasterPoid(rs.getBigDecimal("ADDRESS_MASTER_POID") != null ? rs.getBigDecimal("ADDRESS_MASTER_POID").longValue() : null);
                address.setAddressPoid(rs.getBigDecimal("ADDRESS_POID"));
                address.setAddressName(rs.getString("ADDRESS_NAME"));
                address.setAddressType(rs.getString("ADDRESS_TYPE"));
                address.setOffTel1(rs.getString("OFF_TEL1"));
                address.setOffTel2(rs.getString("OFF_TEL2"));
                address.setContactPerson(rs.getString("CONTACT_PERSON"));
                address.setDesignation(rs.getString("DESIGNATION"));
                address.setMobile(rs.getString("MOBILE"));
                address.setFax(rs.getString("FAX"));
                address.setEmail1(rs.getString("EMAIL1"));
                address.setEmail2(rs.getString("EMAIL2"));
                address.setWebsite(rs.getString("WEBSITE"));
                address.setPoBox(rs.getString("PO_BOX"));
                address.setOffNo(rs.getString("OFF_NO"));
                address.setBldg(rs.getString("BLDG"));
                address.setRoad(rs.getString("ROAD"));
                address.setAreaCity(rs.getString("AREA_CITY"));
                address.setState(rs.getString("STATE"));
                address.setCountryPoid(rs.getBigDecimal("COUNTRY_POID") != null ? rs.getBigDecimal("COUNTRY_POID").longValue() : null);
                address.setLandMark(rs.getString("LAND_MARK"));

                addressList.add(address);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading address cursor", e);
        }

        return addressList;
    }

}
