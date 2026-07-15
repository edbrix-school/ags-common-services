package com.asg.common.services.service;

import com.asg.common.lib.dto.GLMasterCommonDTO;
import com.asg.common.lib.dto.ReconcileResultDto;
import com.asg.common.lib.dto.request.GlobalTermsInsertRequestDto;
import com.asg.common.lib.dto.response.GlobalTermsResponseDto;
import com.asg.common.lib.dto.response.StockDetailsResponse;
import com.asg.common.lib.dto.response.TaxCalculationResponseDto;
import com.asg.common.services.dto.AddressDetailsListResponseDto;
import com.asg.common.services.dto.AddressPoidResponseDto;
import com.asg.common.services.dto.CreditDaysReponseDto;
import com.asg.common.services.dto.CurrencyRateResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface CommonDataService {
    GLMasterCommonDTO getGLMasterDetails(Long glPoid);

    TaxCalculationResponseDto calculateTaxForPettyCash(Long taxPoid, Double drAmt);

    void insertGlobalTerms(List<GlobalTermsInsertRequestDto> requestList);

    void deleteGlobalTerms(
            Long groupPoid,
            Long companyPoid,
            String documentId,
            Long docKeyPoid,
            Long userPoid
    );

    GlobalTermsResponseDto loadGlobalTermsList(
            Long groupPoid,
            Long companyPoid,
            String docId,
            Long docKeyPoid,
            Long termsPoid
    );

    ReconcileResultDto fetchReconDate(String docId,
                                      Long docKeyPoid
    );

    CurrencyRateResponseDto getCurrencyRate(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String docId,
            Long docKeyPoid,
            String currencyCode,
            String parameters
    );

    public StockDetailsResponse getStockDetails(Long stockPoid);

    String createPoFromRfq(
            Long loginGroupPoid,
            Long loginUserPoid,
            Long loginCompanyPoid,
            Long poPoid,
            String supplierPoid,
            String rfqPoid
    );

    AddressPoidResponseDto fetchAddressPoidByAssociatedData(
            Long associatedAddressPoid,
            String associatedAddressType
    );

    AddressDetailsListResponseDto fetchAddressByMasterPoid(
            Long addressMasterPoid,
            BigDecimal addressPoid
    );

    CreditDaysReponseDto fetchCreditDaysByCustomerPoid(
            Long customerPoid,
            String blType
    );
}
