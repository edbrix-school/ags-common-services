package com.asg.common.services.service;

import com.asg.common.lib.service.PrintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicReportPrintService {

    private final PrintService printService;
    private final DataSource dataSource;

    @Value("${db.connection:oracle}")
    private String dbConnection;

    private JasperReport load(String path) throws Exception {
        return printService.load("postgres".equalsIgnoreCase(dbConnection) ? "PG/" + path : path);
    }

    public byte[] generateReportPdf(String docId, Map<String, Object> rptParams) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(null, docId);
        JasperReport mainReport = getReportConfig(docId, params);
        if (rptParams != null) {
            convertCompanyPoidParam(rptParams, docId);
            convertDateParams(rptParams);
            params.putAll(rptParams);
        }
        //params.forEach((k, v) -> log.debug("PARAM [{}] = [{}] type=[{}]", k, v, v != null ? v.getClass().getName() : "null"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    /**
     * Handle COMPANY_POID parameter - create appropriate types for different reports
     */
    private void convertCompanyPoidParam(Map<String, Object> params, String docId) throws Exception {
        Object raw = params.get("COMPANY_POID");
        if (docId.equals("600-201") || docId.equals("600-203")) {
            raw = params.get("COMPANY");
        }
        if (raw instanceof List<?> list) {
            List<Long> companyIds = list.stream()
                    .map(obj -> obj instanceof Number ? ((Number) obj).longValue() : Long.parseLong(obj.toString()))
                    .toList();
            // 999 - All Companies Check
            boolean contains999 = companyIds.contains(999L);
            if (companyIds.size() == 1 || contains999) {
                // Backward Compatibility
                // Every JRXML declares COMPANY_POID/COMPANY_POID_CSV as java.lang.String,
                // so always hand JasperReports a String - a raw Long fails parameter type validation.
                params.put("COMPANY_POID", contains999 ? "999" : String.valueOf(companyIds.getFirst()));
                // Adding CSV as well because we already changed query for Two Reports BillWise Statement and BillWise Statement FC
                params.put("COMPANY_POID_CSV", contains999 ? "999" : String.valueOf(companyIds.getFirst()));
            } else {
                // Multiple values - add as CSV, Add First ID in COMPANY_POID as well to maintain existing logic
                params.put("COMPANY_POID", String.valueOf(companyIds.getFirst()));
                params.put("COMPANY_POID_CSV", companyIds.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));

            }
            // Instead of COMPANY_POID we are getting COMPANY so converting back to Support JRXML
            // 600-201: Asset Wise Depreciation Schedule Report
            // 600-203: Asset Category Wise Depreciation Posting Report
            if (docId.equals("600-201") || docId.equals("600-203")) {
                params.put("COMPANY", params.get("COMPANY_POID"));
                params.put("COMPANY_CSV", params.get("COMPANY_POID_CSV"));
            }
        }
    }
    
    private void convertDateParams(Map<String, Object> params) {
        params.entrySet().forEach(entry -> {
            if ((entry.getKey().contains("DATE") || entry.getKey().contains("PERIOD") || entry.getKey().contains("PERIOD2")) && entry.getValue() instanceof String value) {
                if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if ("postgres".equalsIgnoreCase(dbConnection)) {
                        entry.setValue(java.sql.Date.valueOf(value));
                    } else {
                        entry.setValue(LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")).toUpperCase());
                    }
                }
            }
        });
    }
    
    private JasperReport getReportConfig(String docId, Map<String, Object> params) throws Exception {
        return switch (docId) {
            case "100-292" -> load("DynamicReport/SH/Ship_LineWise_Inventory_Daily.jrxml");
            case "100-293" -> load("DynamicReport/SH/Ship_Linewise_Transhipment.jrxml");
            case "100-360" -> load("DynamicReport/RevenueReports/ShippingRevenueLineContainerComparisonReport.jrxml");
            case "100-361" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonReport.jrxml");
            }
            case "100-396" -> load("DynamicReport/SH/SH_EXPORT_DAYBOOK_MONTHLY.jrxml");
            case "100-463", "100-473" -> {
                params.put("SUBREPORT1", load("DynamicReport/SALES/Shipping_Charges_SubReport.jrxml"));
                params.put("SUBREPORT2", load("DynamicReport/SALES/Shipping_Demurrage_SubReport.jrxml"));
                params.put("SUBREPORT3", load("DynamicReport/SALES/Shipping_Charges_Export_SubReport.jrxml"));
                params.put("SUBREPORT4", load("DynamicReport/SALES/Shipping_Detention_SubReport.jrxml"));
                params.put("SUBREPORT5", load("DynamicReport/SALES/Shipping_Charges_Final_Dummy.jrxml"));
                yield load("DynamicReport/SALES/Shipping_Charges_List.jrxml");
            }
            case "120-521" -> load("DynamicReport/FF/HOUSE_Mnf_Cargo_multiple.jrxml"); // Manifest for Multiple House BLs => Print Manifest After Process
            case "120-529" -> load("DynamicReport/FF/DELIVERY_RECEIPT.jrxml"); // FF Proof of Delivery Receipt Print Report (120-529), the Print Proof of Delivery Receipt
            case "300-203" -> load("DynamicReport/GL/CustomerLedgerStatement_A4.jrxml");
            case "400-201" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/TrialBalanceReportSubreport1.jrxml"));
                yield load("DynamicReport/GL/TrialBalanceReport.jrxml");
            }
            case "400-202" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/ProfitandLossReportSubreport1.jrxml"));
                yield load("DynamicReport/GL/ProfitandLossReport.jrxml");
            }
            case "400-205" -> {
                params.put("SUB_CONTACT", load("DynamicReport/GL/BillwiseLedgerStatement_Contact_subreport1.jrxml"));
                yield load("DynamicReport/GL/BillwiseLedgerStatement_A4.jrxml");
            }
            case "400-219" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/LedgerGroupSummarySubReport1.jrxml"));
                yield load("DynamicReport/GL/LedgerGroupSummary.jrxml");
            }
            case "400-240" -> load("DynamicReport/GL/LedgerStatementFC.jrxml");
            case "400-331" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/DivisionWiseComparisonSubreport.jrxml"));
                yield load("DynamicReport/GL/DivisionWiseComparisonReport.jrxml");
            }
            case "400-332" -> load("DynamicReport/RevenueReports/PropertiesMonthlyRentSummaryReport.jrxml");
            case "400-343" -> load("DynamicReport/RevenueReports/ShippingRevenueYearWiseQtyComparisonReport.jrxml");
            case "400-347" -> {
                params.put("SUB_CONTACT", load("DynamicReport/GL/BillwiseLedgerStatement_Contact_subreport1.jrxml"));
                yield load("DynamicReport/GL/BillwiseLedgerStatementFC_A4.jrxml");
            }
            case "400-356" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/DivisionWiseComparisonSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/DivisionWiseComparisonReport.jrxml");
            }
            case "400-357" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonReport.jrxml");
            }
            case "400-365" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/OverAllGLLedgerDivisionWiseSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/OverAllGLLedgerDivisionWiseReport.jrxml");
            }
            case "400-375", "400-377", "400-378", "400-379", "400-380", "400-381", "400-382", "400-384", "400-385",
                 "400-386", "400-387", "400-388", "400-392" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/TrialBalanceReportSubreport1.jrxml"));
                yield load("DynamicReport/GL/TrialBalanceReport.jrxml");
            }
            case "400-389" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/TRIAL_BALANCE/TrialBalanceReportSubreport1.jrxml"));
                yield load("DynamicReport/GL/TRIAL_BALANCE/TrialBalanceReport.jrxml");
            }
            case "400-396" -> {
                params.put("SUBREPORT1", load("DynamicReport/GL/TradeDebtorsSummarySubReport1.jrxml"));
                yield load("DynamicReport/GL/TradeDebtorsSummary.jrxml");
            }
            case "400-406" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonReport.jrxml");
            }
            case "400-433" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/DivisionWiseComparisonPeriodSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/DivisionWiseComparisonPeriodReport.jrxml");
            }
            case "400-434", "400-453" -> {
                params.put("SUBREPORT1", load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonSubreport.jrxml"));
                yield load("DynamicReport/RevenueReports/ShippingRevenueLineComparisonReport.jrxml");
            }
            case "600-201" -> load("DynamicReport/FA/DepreciationScheduleReport.jrxml");
            case "600-203" -> load("DynamicReport/FA/CategoryWiseDepreciationScheduleReport.jrxml");
             case "600-204" -> {
                params.put("SUBREPORT1", load("DynamicReport/FA/AssetTagReport.jrxml"));
                yield load("DynamicReport/FA/AssetTagReportSubreport.jrxml");
            }
            case "700-216" -> load("DynamicReport/SH/Telephone_Letter.jrxml");
            case "800-204" -> load("DynamicReport/HR/HrPayrollCustomPrint.jrxml");
            case "800-219" -> load("DynamicReport/HR/HrProvisionsTillDateRpt.jrxml");
            default -> load("DynamicReport/DynamicReport_A3.jrxml");
        };
      }
    }