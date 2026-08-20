package com.asg.common.services.controller;

import com.asg.common.lib.dto.LovGetListDto;

import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationProperties;
import com.asg.common.services.entity.State;
import com.asg.common.services.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.dto.response.ApiResponse.*;

@RestController
@RequestMapping("/v1/lovs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "List of Values (LOV)", description = "APIs for managing and retrieving various list of values used across the application")
public class LovController {

    @Autowired
    private LovDataService lovService;

    @Autowired
    private PaginationProperties paginationProperties;

    @Autowired
    private StateService stateService;

    @Operation(summary = "Get currencies list", description = "Retrieve a paginated list of currencies with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Currencies retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/currencies")
    public ResponseEntity<?> getCurrencies(
            @Parameter(description = "Filter text for searching currencies") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CURRENCY", "Currencies", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get IMCO pending receipts", description = "Retrieve pending IMCO deposit receipts with BL Number and Paying To details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IMCO receipts retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/imco/receipts")
    public ResponseEntity<?> getImcoReceipts(
            @Parameter(description = "Filter text for searching receipt numbers") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (receipt_no, bl_no, paying_to)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "IMCO_REFUND_PENDING_RECEIPTS", "IMCO Receipts",                  // <-- Description
                    pageNumber,
                    pageSize,
                    sortBy,
                    sortDir
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get Bank list", description = "Retrieve a paginated list of banks from CUSTOMER_BANK_MASTER")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Banks retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/banks")
    public ResponseEntity<?> getBanks(
            @Parameter(description = "Filter text for searching bank names") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (bank_code, bank_name)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CUSTOMER_BANK_MASTER", "Banks", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get countries list", description = "Retrieve a paginated list of countries with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Countries retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/countries")
    public ResponseEntity<?> getCountries(
            @Parameter(description = "Filter text for searching countries") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "COUNTRY", "Countries", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Property list", description = "Retrieve a paginated list of active properties for property insurance from GL_COST_CENTER_MASTER")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Properties retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/properties")
    public ResponseEntity<?> getProperties(
            @Parameter(description = "Filter text for searching properties") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PROPERTIES", "Properties", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Insurance Contact Types", description = "Retrieve a paginated list of contact types for insurance (e.g., Primary, Secondary) from LOV_INSURANCE_CONTACT_TYPE")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contact types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/insurance-contact-types")
    public ResponseEntity<?> getInsuranceContactTypes(
            @Parameter(description = "Filter text for searching contact types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "INSURANCE_CONTACT_TYPE", "Insurance Contact Types", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Insurance Categories", description = "Retrieve a paginated list of insurance categories (e.g., Individual, Group, Company) from LOV_INSURANCE_CATEGORY")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Insurance categories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/insurance-categories")
    public ResponseEntity<?> getInsuranceCategories(
            @Parameter(description = "Filter text for searching insurance categories") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "INSURANCE_CATEGORY", "Insurance Categories", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get Insurance Types", description = "Retrieve a paginated list of insurance types (e.g., Vehicle, Medical, Property, Travel, Project, etc.) from LOV_INSURANCE_TYPE")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Insurance types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/insurance-types")
    public ResponseEntity<?> getInsuranceTypes(
            @Parameter(description = "Filter text for searching insurance types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "INSURANCE_TYPE", "Insurance Types", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get PIC Person List", description = "Retrieve a paginated list of active employees eligible as Person In Charge (PIC) for insurance from HR_EMPLOYEE_MASTER")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PIC Person list retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pic-person-list")
    public ResponseEntity<?> getPicPersonList(
            @Parameter(description = "Filter text for searching PIC persons") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "INSURANCE_PROPERTY_PIC", "PIC Person List", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get Charge list", description = "Retrieve a paginated list of Charges with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Charges retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/charge")

    public ResponseEntity<?> getCharges(

            @Parameter(description = "Filter text for searching charges") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "TAX_PERIOD_CHARGE_MASTER", "Charges", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }


    @Operation(summary = "Get Charge Category list", description = "Retrieve a paginated list of Charge Categories with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Charge Categories retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/charge-category")

    public ResponseEntity<?> getChargeCategories(

            @Parameter(description = "Filter text for searching charge categories") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "CHARGE_GROUP_MASTER", "Charge Categories", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }


    @Operation(summary = "Get Input Tax list", description = "Retrieve a paginated list of Input Taxes with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Input Taxes retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/input-tax")

    public ResponseEntity<?> getInputTaxes(

            @Parameter(description = "Filter text for searching input taxes") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "INPUT_TAX_MASTER", "Input Taxes", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }


    @Operation(summary = "Get Output Tax list", description = "Retrieve a paginated list of Output Taxes with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Output Taxes retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/output-tax")

    public ResponseEntity<?> getOutputTaxes(

            @Parameter(description = "Filter text for searching output taxes") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "OUTPUT_TAX_MASTER", "Output Taxes", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }

    @Operation(summary = "Get Stock list", description = "Retrieve a paginated list of Stocks with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stocks retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/stock")

    public ResponseEntity<?> getStocks(

            @Parameter(description = "Filter text for searching stocks") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "TAX_PERIOD_STOCK_MASTER", "Stocks", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }


    @Operation(summary = "Get Stock Category list", description = "Retrieve a paginated list of Stock Categories with optional filtering and sorting")

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock Categories retrieved successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")

    })

    @GetMapping("/stock-category")

    public ResponseEntity<?> getStockCategories(

            @Parameter(description = "Filter text for searching stock categories") @RequestParam(required = false) String filter,

            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,

            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,

            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,

            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,

            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,

            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {

        try {

            validateSortParameters(sortBy, sortDir);

            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,

                    "TAX_PERIOD_STOCK_CATEGORY_MASTER", "Stock Categories", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {

            return badRequest(e.getMessage());

        }

    }


    @Operation(summary = "Get employees list", description = "Retrieve a paginated list of employees with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Employees retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees(
            @Parameter(description = "Filter text for searching employees") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "EMPLOYEE_NAME", "Employees", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get bank master list", description = "Retrieve a paginated list of banks with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank master retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/bank-master")
    public ResponseEntity<?> getBankMaster(
            @Parameter(description = "Filter text for searching banks") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "1") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "BANK_MASTER", "Bank master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    private void validateSortParameters(String sortBy, String sortDir) {
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            List<String> validSortFields = Arrays.asList("poid", "code", "label", "description", "value");
            if (!validSortFields.contains(sortBy.toLowerCase())) {
                throw new IllegalArgumentException("Invalid sortBy field. Allowed values: " + validSortFields);
            }
        }
        if (sortDir != null && !sortDir.trim().isEmpty()) {
            List<String> validSortDirections = Arrays.asList("asc", "desc");
            if (!validSortDirections.contains(sortDir.toLowerCase())) {
                throw new IllegalArgumentException("Invalid sortDir value. Allowed values: asc, desc");
            }
        }
    }

    @Operation(summary = "Get companies list", description = "Retrieve a paginated list of companies with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/companies")
    public ResponseEntity<?> getCompanies(
            @Parameter(description = "Filter text for searching companies") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "COMPANY", "Companies", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Petty Cash GL list", description = "Retrieve a paginated list of Petty Cash GL mappings with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/petty-cash-gl")
    public ResponseEntity<?> getPettyCashGl(
            @Parameter(description = "Filter text for searching Petty Cash GL") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "1") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            // Validate sorting parameters
            validateSortParameters(sortBy, sortDir);

            // Handle the LOV request using a generic handler (similar to bank master)
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PETTY_CASH_GL", "Petty Cash GL Mapping", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get locations list", description = "Retrieve a paginated list of locations with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Locations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/locations")
    public ResponseEntity<?> getLocations(
            @Parameter(description = "Filter text for searching locations") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "LOCATION", "Locations", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get petty cash user role ref types list", description = "Retrieve a paginated list of petty cash user role reference types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Petty cash user role ref types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/petty-cash-user-role-ref-types")
    public ResponseEntity<?> getPettyCashUserRoleRefTypes(
            @Parameter(description = "Filter text for searching petty cash user role ref types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PETTY_CASH_USER_ROLE_REF_TYPE", "Ref Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

    }

    @Operation(summary = "Get user roles list", description = "Retrieve a paginated list of user roles with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User roles retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })

    @GetMapping("/user-roles")
    public ResponseEntity<?> getUserRoles(
            @Parameter(description = "Filter text for searching user roles") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "USER_ROLES", "User Roles", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(
            summary = "Get GL Accounts list",
            description = "Retrieve a list of GL Accounts (code and description) from GL_MASTER_LEDGERS table",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "GL Accounts retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/gl-accounts")
    public ResponseEntity<?> getGLAccounts(
            @Parameter(description = "Filter text for searching GL Accounts") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_MASTER_LEDGERS", "Fa GL Accounts", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(
            summary = "Get Cost Center list",
            description = "Retrieve a list of Cost Centers (code and description) from GL_COST_CENTRE table",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cost Centers retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/cost-centers")
    public ResponseEntity<?> getCostCenters(
            @Parameter(description = "Filter text for searching Cost Centers") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_COST_CENTRE", "Cost Center", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(
            summary = "Get Property Groups list",
            description = "Retrieve a list of top-level parent properties (PROPERTY_GROUPS)",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property Groups retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/property-groups")
    public ResponseEntity<?> getPropertyGroups(
            @Parameter(description = "Filter text for searching Property Groups") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PROPERTY_GROUPS", "Property Groups", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(
            summary = "Get Property Companies list",
            description = "Retrieve a list of Property Companies (PROPERTY_COMPANY)",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Property Companies retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/property-companies")
    public ResponseEntity<?> getPropertyCompanies(
            @Parameter(description = "Filter text for searching Property Companies") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PROPERTY_COMPANY", "Property Companies", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get divisions list", description = "Retrieve a paginated list of divisions with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Divisions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/divisions")
    public ResponseEntity<?> getDivisions(
            @Parameter(description = "Filter text for searching divisions") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "DIVISION", "Divisions", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get preferred communication list", description = "Retrieve a paginated list of preferred communication methods with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferred communication methods retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/preferred-communication")
    public ResponseEntity<?> getPreferredCommunication(
            @Parameter(description = "Filter text for searching communication methods") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PREFERRED_COMMUNICATION", "Preferred Communication",
                    pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get party types list", description = "Retrieve a paginated list of party types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Party types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/party-type")
    public ResponseEntity<?> getPartyType(
            @Parameter(description = "Filter text for searching party types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "ADDRESS_PARTY_TYPE", "Party Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get cities list", description = "Retrieve a paginated list of cities for a specific state with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cities retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cities")
    public ResponseEntity<?> getCities(
            @Parameter(description = "Filter text for searching cities") @RequestParam(required = false) String filter,
            @Parameter(description = "State identifier to filter cities", required = true) @RequestParam Long statePoid,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(String.valueOf(statePoid), groupPoid, companyPoid, userPoid,
                    "CITY", "Cities", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

    }

    @Operation(summary = "Get task categories list", description = "Retrieve a paginated list of task categories with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task categories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/task-category")
    public ResponseEntity<?> getTaskCategories(
            @Parameter(description = "Filter text for searching task categories") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TASK_CATEGORY", "Task Categories", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get task sub-categories list", description = "Retrieve a paginated list of task sub-categories with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task sub-categories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/task-sub-category")
    public ResponseEntity<?> getTaskSubCategories(
            @Parameter(description = "Filter text for searching task sub-categories") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TASK_SUB_CATEGORY", "Task Sub Categories", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get task priority list", description = "Retrieve a static list of task priorities (Low, Medium, Normal, High).Filters and sorting not supported.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task priorities retrieved successfully")
    })
    @GetMapping("/task-priority")
    public ResponseEntity<?> getTaskPriority() {
        List<LovGetListDto> priorities = Arrays.asList(
                createStaticLov(1L, "LOW", "Low", 1),
                createStaticLov(2L, "MEDIUM", "Medium", 2),
                createStaticLov(3L, "NORMAL", "Normal", 3),
                createStaticLov(4L, "HIGH", "High", 4)
        );
        return staticLovResponse("Task Priority", priorities);
    }

    @Operation(summary = "Get task types list", description = "Retrieve a paginated list of task types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/task-type")
    public ResponseEntity<?> getTaskType(
            @Parameter(description = "Filter text for searching task types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TASK_TYPE", "Task Types", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get task status list", description = "Retrieve a static list of task statuses (Pending, In Progress, Completed, Cancelled, Closed).Filters and sorting not supported.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Task statuses retrieved successfully")
    })
    @GetMapping("/task-status")
    public ResponseEntity<?> getTaskStatus() {
        List<LovGetListDto> statuses = Arrays.asList(
                createStaticLov(1L, "PENDING", "Pending", 1),
                createStaticLov(2L, "IN_PROGRESS", "In Progress", 2),
                createStaticLov(3L, "COMPLETED", "Completed", 3),
                createStaticLov(4L, "CANCELLED", "Cancelled", 4),
                createStaticLov(5L, "CLOSED", "Closed", 5)
        );
        return staticLovResponse("Task Status", statuses);
    }

    @Operation(summary = "Get user master list", description = "Retrieve a paginated list of users with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user-master")
    public ResponseEntity<?> getUserMaster(
            @Parameter(description = "Filter text for searching users") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "USER_MASTER", "Users", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get modules list", description = "Retrieve a paginated list of modules with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/modules")
    public ResponseEntity<?> getModules(
            @Parameter(description = "Filter text for searching modules") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "MODULE", "Modules", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get timezones list", description = "Retrieve a paginated list of timezones with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Timezones retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/timezones")
    public ResponseEntity<?> getTimezones(
            @Parameter(description = "Filter text for searching timezones") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            int page = pageNumber != null ? pageNumber : paginationProperties.getPageNumber();
            int size = pageSize != null ? pageSize : paginationProperties.getPageSize();
            String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : paginationProperties.getSortBy();
            String sortOrder = (sortDir != null && !sortDir.isEmpty()) ? sortDir : paginationProperties.getSortDir();

            Map<String, Object> result = lovService.getTimezoneList(filter, page, size, sortField, sortOrder);
            return success("Timezones fetched successfully", result);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception ex) {
            return internalServerError("Error fetching Timezones: " + ex.getMessage());
        }
    }

    @Operation(summary = "Get data entry periods list", description = "Retrieve a static list of data entry periods (Financial Period, Transaction Period).Filters and sorting not supported.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data entry periods retrieved successfully")
    })
    @GetMapping("/data-entry-periods")
    public ResponseEntity<?> getDataEntryPeriods() {
        List<LovGetListDto> list = Arrays.asList(
                createStaticLov(1L, "FINANCIAL", "Financial Period", 1),
                createStaticLov(2L, "TRANSACTION", "Transaction Period", 2)
        );
        return staticLovResponse("Data Entry Periods", list);
    }

    @Operation(summary = "Get approval levels list", description = "Retrieve a static list of approval levels (Level 1, Level 2, Level 3).Filters and sorting not supported.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Approval levels retrieved successfully")
    })
    @GetMapping("/approval-levels")
    public ResponseEntity<?> getApprovalLevels() {
        List<LovGetListDto> list = Arrays.asList(
                createStaticLov(1L, "LEVEL1", "Level 1", 1),
                createStaticLov(2L, "LEVEL2", "Level 2", 2),
                createStaticLov(3L, "LEVEL3", "Level 3", 3)
        );
        return staticLovResponse("Approval Levels", list);
    }

    @Operation(
            summary = "Get States by CountryId",
            description = """
                    Returns the list of states for the given country.
                    
                    ### Path Parameters
                    - **countryId:** Country's Primary Key (Path Variable)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "States retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Country not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/countries/{countryId}/states")
    public ResponseEntity<?> getStatesByCountry(@PathVariable Long countryId) {

        List<State> states = stateService.getStatesByCountry(countryId);
        return success("States fetched successfully", states);

    }

    @Operation(summary = "Get documents list", description = "Retrieve a paginated list of documents with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/documents")
    public ResponseEntity<?> getDocuments(
            @Parameter(description = "Filter text for searching documents") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "DOCUMENTS", "Documents", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get terms template list", description = "Retrieve a list of terms templates with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Terms template retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/terms-template")
    public ResponseEntity<?> getTermsTemplate(
            @Parameter(description = "Filter text for searching terms templates") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TERMS_TEMPLATE_MASTER", "Terms Template", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get IT Fixed Assets List", description = "Retrieve a paginated list of IT Fixed Assets")
    @GetMapping("/it-fixed-assets")
    public ResponseEntity<?> getITFixedAssets(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "IT_FIXED_ASSET", "IT Fixed Assets", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== Bank Master =====

    @Operation(summary = "Get Bank Account Types", description = "Retrieve a paginated list of Bank Account Types.Filters and sorting not supported.")
    @GetMapping("/bank-account-types")
    public ResponseEntity<?> getBankAccountTypes(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "BANK_ACCOUNT_TYPE", "Bank Account Types", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get GL Ledgers for Banks", description = "Retrieve a paginated list of GL Master Ledgers for Banks")
    @GetMapping("/gl-master-ledgers-bank")
    public ResponseEntity<?> getGLMasterLedgersBank(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_MASTER_LEDGERS_BANK", "GL Master Ledgers Bank", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Bank Prefix list", description = "Retrieve a paginated list of Bank Prefix values.Filters not supported.")
    @GetMapping("/bank-prefix")
    public ResponseEntity<?> getBankPrefix(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "ARCUSTBANKRCPT", "Bank Prefix", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Bank Currencies list", description = "Retrieve a paginated list of Bank Currencies")
    @GetMapping("/bank-currencies")
    public ResponseEntity<?> getBankCurrencies(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CURRENCY", "Bank Currencies", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Cheque Sign Types", description = "Retrieve a paginated list of Cheque Sign Types.Filters and sorting not supported.")
    @GetMapping("/cheque-sign-type")
    public ResponseEntity<?> getChequeSignType(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "CHEQUE_SIGN_TYPE", "Cheque Sign Types", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

    }

    @Operation(summary = "Get Printers list", description = "Retrieve a paginated list of Printers")
    @GetMapping("/printers")
    public ResponseEntity<?> getPrinters(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "PRINTERS", "Printers", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Bank Master Stock Finished list", description = "Retrieve a paginated list of Bank Master Stock Finished values.Filters and sorting not supported.")
    @GetMapping("/bank-master-stock-finished")
    public ResponseEntity<?> getBankMasterStockFinished(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "BANK_MASTER_STOCK_FINISHED", "Bank Master Stock Finished", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Bank Tax Master list", description = "Retrieve a paginated list of Bank Tax Master values")
    @GetMapping("/bank-tax-master")
    public ResponseEntity<?> getBankTaxMaster(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "BANK_TAX_MASTER", "Bank Tax Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Bank Card Types", description = "Retrieve a paginated list of Bank Card Types")
    @GetMapping("/bank-card-type")
    public ResponseEntity<?> getBankCardType(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "BANK_CARD_TYPE", "Bank Card Types", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Cost Group Types List", description = "Retrieve a paginated list of cost group types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cost Group Types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cost-group-types")
    public ResponseEntity<?> getCostGroupTypes(
            @Parameter(description = "Filter text for searching cost group types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_COST_GROUPS", "Cost Group Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Cost Center Type List", description = "Retrieve a paginated list of cost center types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cost Center Types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cost-center-type")
    public ResponseEntity<?> getCostCenterTypes(
            @Parameter(description = "Filter text for searching cost center type") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "COST_CENTER_TYPE", "Cost Center Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Cost Center Group List", description = "Retrieve a paginated list of cost center groups with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cost Center Types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cost-center-groups")
    public ResponseEntity<?> getCostCenterGroups(
            @Parameter(description = "Filter text for searching cost center type") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "COST_CENTER_GROUPS", "Cost Center Groups", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }
    // ================== Supplier Master LOV Endpoints ==================

    @Operation(summary = "Get Supplier Type List", description = "Retrieve a paginated list of supplier types.Filters not supported.")
    @GetMapping("/supplier-type")
    public ResponseEntity<?> getSupplierType(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "SUPPLIER_TYPE", "Supplier Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Supplier Category List", description = "Retrieve a paginated list of supplier categories")
    @GetMapping("/supplier-category")
    public ResponseEntity<?> getSupplierCategory(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "SUPPLIER_CATEGORY", "Supplier Category", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Purchaser List", description = "Retrieve a paginated list of purchasers for suppliers.Filters not supported.")
    @GetMapping("/purchasers")
    public ResponseEntity<?> getPurchasers(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "PURCHASER_FOR_SUPPLIER", "Purchasers", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Customer Tax Slab List", description = "Retrieve a paginated list of customer tax slabs.Filters not supported.")
    @GetMapping("/customer-tax-slab")
    public ResponseEntity<?> getCustomerTaxSlab(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "CUSTOMER_TAX_SLAB", "Customer Tax Slab", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get GL Master Ledgers List", description = "Retrieve a paginated list of GL Master Ledgers")
    @GetMapping("/gl-master-ledgers")
    public ResponseEntity<?> getGLMasterLedgers(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_MASTER_LEDGERS", "GL Master Ledgers", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Customer Master List", description = "Retrieve a paginated list of customers")
    @GetMapping("/customer-master")
    public ResponseEntity<?> getCustomerMaster(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CUSTOMER_MASTER", "Customer Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    //No more required
//    @Operation(summary = "Get GRN Credit GL List", description = "Retrieve a paginated list of GRN Credit GL")
//    @GetMapping("/grn-credit-gl")
//    public ResponseEntity<?> getGRNCreditGL(
//            @RequestParam(required = false) String filter,
//            @RequestParam(defaultValue = "1") Long groupPoid,
//            @RequestParam(defaultValue = "0") Long companyPoid,
//            @RequestParam(defaultValue = "0") Long userPoid,
//            @RequestParam(required = false) Integer pageNumber,
//            @RequestParam(required = false) Integer pageSize,
//            @RequestParam(required = false) String sortBy,
//            @RequestParam(required = false) String sortDir) {
//        try {
//            validateSortParameters(sortBy,sortDir);
//            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
//                    "GRN_CREDIT_GL", "GRN Credit GL", pageNumber, pageSize, sortBy, sortDir);
//        } catch (IllegalArgumentException e) {
//            return badRequest(e.getMessage());
//        }
//    }

    @Operation(summary = "Get Payment Type List", description = "Retrieve a paginated list of Payment Types.Filters not supported.")
    @GetMapping("/payment-type")
    public ResponseEntity<?> getPaymentType(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "PAYMENT_TYPE", "Payment Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Supplier Services List", description = "Retrieve a paginated list of Supplier Services")
    @GetMapping("/supplier-services")
    public ResponseEntity<?> getSupplierServices(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "AP_SUPPLIER_MASTER_SERVICES", "Supplier Services", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

    }

    @Operation(summary = "Get Questionnaire List", description = "Retrieve a paginated list of Questionnaire Master")
    @GetMapping("/questionnaire")
    public ResponseEntity<?> getQuestionnaire(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "QUESTIONNAIRE_MASTER", "Questionnaire Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get address master list", description = "Retrieve a paginated list of address master entries with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address master retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/address-master")
    public ResponseEntity<?> getAddressMaster(
            @Parameter(description = "Filter text for searching addresses") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "ADDRESS_MASTER", "Address Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // Tax Master LOV Endpoints
    @Operation(summary = "Get Tax Type List", description = "Retrieve a paginated list of Tax Types with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tax Type for Tax Master retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tax-type")
    public ResponseEntity<?> getTaxTypesForTaxMaster(
            @Parameter(description = "Filter text for searching tax type") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TAX_IN_OUT", "Tax Type", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Tax Category List", description = "Retrieve a paginated list of Tax Categories with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tax Categories retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tax-category")
    public ResponseEntity<?> getTaxCategories(
            @Parameter(description = "Filter text for searching tax category") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TAX_CATEGORY", "Tax Category", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get GL Type List", description = "Retrieve a paginated list of GL Type with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "GL Types retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/gl-type")
    public ResponseEntity<?> getGlType(
            @Parameter(description = "Filter text for searching gl type") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TAX_GL_TYPE", "GL Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get GL Ledger List", description = "Retrieve a paginated list of GL Ledger with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "GL Ledger for Tax Master retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/gl-ledger")
    public ResponseEntity<?> getGlLedger(
            @Parameter(description = "Filter text for searching gl ledger types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_FOR_TAX_MASTER", "GL Ledger", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    // ===== Common handler for dynamic LOVs =====
    private ResponseEntity<?> handleLovRequest(String filter, Long groupPoid, Long companyPoid, Long userPoid,
                                               String lovType, String lovName,
                                               Integer pageNumber, Integer pageSize,
                                               String sortBy, String sortDir) {
        return handleLovRequestWithDefaults(filter, groupPoid, companyPoid, userPoid, lovType, lovName, pageNumber, pageSize, sortBy, sortDir, null, null, null);
    }

    // ===== Common handler for dynamic LOVs with default parameters =====
    private ResponseEntity<?> handleLovRequestWithDefaults(String filter, Long groupPoid, Long companyPoid, Long userPoid,
                                                          String lovType, String lovName,
                                                          Integer pageNumber, Integer pageSize,
                                                          String sortBy, String sortDir,
                                                          List<String> defaultCode, List<Long> defaultPoid,
                                                          String filterField) {
        try {
            // apply defaults from PaginationProperties if null
            int page = pageNumber != null ? pageNumber : paginationProperties.getPageNumber();
            int size = pageSize != null ? pageSize : paginationProperties.getPageSize();
            String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : paginationProperties.getSortBy();
            String sortOrder = (sortDir != null && !sortDir.isEmpty()) ? sortDir : paginationProperties.getSortDir();

            Map<String, Object> result;

            if ("BANK_MASTER".equals(lovType)) {
                result = lovService.getBankMasterLov(filter, groupPoid, companyPoid, userPoid, page, size, sortField, sortOrder, filterField);
            } else {
                result = lovService.getLovList(filter, groupPoid, companyPoid, userPoid, lovType, page, size, sortField, sortOrder, defaultCode, defaultPoid, filterField);
            }
            return success(lovName + " fetched successfully", result);
        } catch (Exception ex) {
            return internalServerError("Error fetching " + lovName + ": " + ex.getMessage());
        }
    }

    // ===== Helpers for static LOVs =====
    private LovGetListDto createStaticLov(Long poid, String code, String description, int seqNo) {
        LovGetListDto dto = new LovGetListDto();
        dto.setPoid(poid);
        dto.setCode(code);
        dto.setDescription(description);
        dto.setLabel(description);
        dto.setValue(poid);
        dto.setSeqNo(seqNo);
        return dto;
    }

    private ResponseEntity<?> staticLovResponse(String lovName, List<LovGetListDto> list) {
        Map<String, Object> response = new HashMap<>();
        response.put("totalRecords", list.size());
        response.put("data", list);
        return success(lovName + " fetched successfully", response);
    }

    //ageing Master LOV's
    @Operation(
            summary = "Fetch Ageing Breakup Types",
            description = "Returns a list of predefined Ageing Breakup Types from LOV 'GL_AGEING_TYPES' for dropdown selection"
    )
    @GetMapping("/ageing-breakup-types")
    public ResponseEntity<List<LovGetListDto>> getAgeingBreakupTypes(
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid) {

        List<LovGetListDto> result = lovService.getAgeingBreakupTypes(groupPoid, companyPoid, userPoid);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get Favourite Account Groups List",
            description = "Retrieve a paginated list of Favorite Account Groups based on filters, sorting, and pagination parameters."
    )
    @GetMapping("/favorite-account-groups")
    public ResponseEntity<?> getFavoriteAccountGroups(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "FAV_VIEW_CATEGORY", "Favorite Account Groups", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get FA Types", description = "Retrieve list of FA Types from FA_TYPE table.Filters not supported.")
    @GetMapping("/fa-types")
    public ResponseEntity<?> getFaTypes(
//            @Parameter(description = "Filter text for searching FA Types") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, description)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "FA_TYPE", "FA Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Fixed Asset Categories", description = "Retrieve list of categories from FIXED_ASSET_CATEGORY table.Filters not supported.")
    @GetMapping("/fixed-asset-category")
    public ResponseEntity<?> getFixedAssetCategories(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "FIXED_ASSET_CATEGORY", "Fixed Asset Category", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Asset Types", description = "Retrieve list of Asset Types from ASSET_TYPE table")
    @GetMapping("/asset-types")
    public ResponseEntity<?> getAssetTypes(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "ASSET_TYPE", "Asset Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Parent Assets", description = "Retrieve list of Parent Assets from FIXED_ASSET_PARENT table")
    @GetMapping("/fixed-asset-parent")
    public ResponseEntity<?> getFixedAssetParent(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "FIXED_ASSET_PARENT", "Fixed Asset Parent", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get Asset Locations", description = "Retrieve list of Asset Locations from ASSET_LOCATION table")
    @GetMapping("/asset-locations")
    public ResponseEntity<?> getAssetLocations(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "ASSET_LOCATION", "Asset Location", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- GENERAL INFORMATION TAB ---

    @Operation(summary = "Get Supplier Master", description = "Retrieve list of Suppliers from SUPPLIER_MASTER table")
    @GetMapping("/supplier-master")
    public ResponseEntity<?> getSupplierMaster(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "SUPPLIER_MASTER", "Supplier Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // --- INFORMATION ASSET DETAILS TAB ---

    @Operation(summary = "Get Type of Information Assets", description = "Retrieve list of Information Asset Types from TYPE_OF_INFORMATION_ASSET table.Filters not supported.")
    @GetMapping("/type-of-information-asset")
    public ResponseEntity<?> getTypeOfInformationAsset(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TYPE_OF_INFORMATION_ASSET", "Type of Information Asset", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get FA Classification", description = "Retrieve list of FA Classification values from FA_CLASSIFICATION table.Filters not supported.")
    @GetMapping("/fa-classification")
    public ResponseEntity<?> getFAClassification(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "FA_CLASSIFICATION", "FA Classification", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get FA Integrity", description = "Retrieve list of FA Integrity values from FA_INTEGRITY table.Filters not supported.")
    @GetMapping("/fa-integrity")
    public ResponseEntity<?> getFAIntegrity(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "FA_INTEGRITY", "FA Integrity", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get FA Availability", description = "Retrieve list of FA Availability values from FA_AVAILABILITY table")
    @GetMapping("/fa-availability")
    public ResponseEntity<?> getFAAvailability(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "FA_AVAILABILITY", "FA Availability", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get FA Data Retention Periods", description = "Retrieve list of FA Data Retention Periods from FA_DATA_RETENTION_PERIOD table")
    @GetMapping("/fa-data-retention-period")
    public ResponseEntity<?> getFADataRetentionPeriods(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "FA_DATA_RETENTION_PERIOD", "FA Data Retention Period", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get GL Types", description = "Retrieve list of GL Types from GL_TYPE LOV. Requires Authorization header with Bearer token.")
    @GetMapping("/gl-types")
    public ResponseEntity<?> getGlTypes(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_TYPE", "GL Type", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== GL MASTER GROUPS =====
    @Operation(summary = "Get GL Master Groups", description = "Retrieve list of GL Master Groups. Requires Authorization header with Bearer token.")
    @GetMapping("/gl-master-groups")
    public ResponseEntity<?> getGlMasterGroups(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_MASTER_GROUPS", "GL Master Groups", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== GL ACCOUNT TYPES =====
    @Operation(summary = "Get GL Account Types", description = "Retrieve list of GL Account Types. Requires Authorization header with Bearer token.")
    @GetMapping("/gl-account-types")
    public ResponseEntity<?> getGlAccountTypes(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_AC_TYPES", "GL Account Types", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== GL CONTROL ACCOUNT TYPES =====
    @Operation(summary = "Get GL Control Account Types", description = "Retrieve list of GL Control Account Types. Requires Authorization header with Bearer token.")
    @GetMapping("/gl-control-account-types")
    public ResponseEntity<?> getGlControlAccountTypes(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "GL_CONTROL_AC_TYPES", "GL Control Account Types", pageNumber, pageSize, sortBy, sortDir);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ===== GL COST GROUPS =====
    @Operation(summary = "Get GL Cost Groups", description = "Retrieve list of GL Cost Groups. Requires Authorization header with Bearer token.Filters not supported.")
    @GetMapping("/gl-cost-groups")
    public ResponseEntity<?> getGlCostGroups(
//            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(null, groupPoid, companyPoid, userPoid,
                    "GL_COST_GROUPS", "GL Cost Groups", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(
            summary = "Get IA Operating Units",
            description = "Retrieve list of IA Operating Units. Requires Authorization header with Bearer token."
    )
    @GetMapping("/ia-operating-units")
    public ResponseEntity<?> getIaOperatingUnits(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        try {
            // Optional: Validate sorting params like in GL Cost Groups
            validateSortParameters(sortBy, sortDir);

            // Call generic LOV handler but pass the IA_OPERATING_UNIT type
            return handleLovRequest(
                    filter,
                    groupPoid,
                    companyPoid,
                    userPoid,
                    "IA_OPERATING_UNIT",   // type of LOV
                    "IA Operating Units",  // display name
                    pageNumber,
                    pageSize,
                    sortBy,
                    sortDir
            );

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

    }

    @Operation(
            summary = "Get IA Operating Units by Asset Type",
            description = "Retrieve list of IA Operating Units filtered by TYPE_OF_INFORMATION_ASSET. Requires Authorization header with Bearer token."
    )
    @GetMapping("/ia-operating-units-assets")
    public ResponseEntity<?> getIaOperatingUnitsByAssetType(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "1") Long groupPoid,
            @RequestParam(defaultValue = "0") Long companyPoid,
            @RequestParam(defaultValue = "0") Long userPoid,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        try {
            // Validate sorting parameters
            validateSortParameters(sortBy, sortDir);

            // Call the generic LOV handler with TYPE_OF_INFORMATION_ASSET
            return handleLovRequest(
                    filter,
                    groupPoid,
                    companyPoid,
                    userPoid,
                    "TYPE_OF_INFORMATION_ASSET", // LOV type for asset-related info
                    "IA Operating Units - Asset Type", // Display name
                    pageNumber,
                    pageSize,
                    sortBy,
                    sortDir
            );
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get bank commission GL list", description = "Retrieve a paginated list of bank commission GL accounts with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank commission GL retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/bank-commission-gl")
    public ResponseEntity<?> getBankCommissionGL(
            @Parameter(description = "Filter text for searching GL accounts") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "BANK_COMMISSION_GL", "Bank commission GL", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ================================
// CHEQUE RETURN STATUS LOV
// ================================
    @Operation(summary = "Get Cheque Return Status LOV", description = "Retrieve a paginated list of cheque return statuses with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cheque return statuses retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/cheque-return-status")
    public ResponseEntity<?> getChequeReturnStatus(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CHEQUE_RETURN_STATUS", "Cheque Return Status", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    // ================================
// CUSTOMER BANK MASTER LOV
// ================================
    @Operation(summary = "Get Customer Bank Master LOV", description = "Retrieve a paginated list of customer bank master entries with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer bank master retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/customer-bank-master")
    public ResponseEntity<?> getCustomerBankMaster(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CUSTOMER_BANK_MASTER", "Customer Bank Master", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ================================
// ACCOUNT TYPE SHORT LOV
// ================================
    @Operation(summary = "Get Account Type Short LOV", description = "Retrieve a paginated list of account type short entries with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account type short retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/account-type-short")
    public ResponseEntity<?> getAccountTypeShort(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "ACC_TYPE_SHORT", "Account Type Short", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }


    @Operation(summary = "Get terms category list", description = "Retrieve a list of terms categories with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Terms category retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/terms-category")
    public ResponseEntity<?> getTermsCategory(
            @Parameter(description = "Filter text for searching terms categories") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "TERMS_CATEGORY", "Terms Category", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // Generic API to load any LOV by name and Label
    @Operation(summary = "Get LOV List by LOV_NAME", description = "Retrieve a list of LOVs by LOV_NAME with optional filtering and sorting")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "LOVs retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{lovName}")
    public ResponseEntity<?> getLovList(
            @PathVariable String lovName,
            @Parameter(description = "Filter text for searching lov list") @RequestParam(required = false) String filter,
            @Parameter(description = "Filter field name (maps to P_LOV_FILTER_FIELD)") @RequestParam(required = false) String filterField,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir,
            @Parameter(description = "Default codes for selection") @RequestParam(required = false) List<String> defaultCode,
            @Parameter(description = "Default POIDs for selection") @RequestParam(required = false) List<Long> defaultPoid) {
        try {
            validateSortParameters(sortBy, sortDir);
            if ("EMPLOYEE_NAME_WITH_SHORT".equalsIgnoreCase(lovName) && filter != null && !filter.isBlank()) {
                filter = filter.replace("%", "").trim();
            }
            return handleLovRequestWithDefaults(filter, groupPoid, companyPoid, userPoid,
                    lovName, lovName, pageNumber, pageSize, sortBy, sortDir, defaultCode, defaultPoid, filterField);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/gl-bdv-voucher-type")
    public ResponseEntity<?> getGlBdvVoucherType(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "GL_BDV_VOUCHER_TYPE", "GL BDV Voucher Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/deposit-line-type")
    public ResponseEntity<?> getDepositLineType(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "DEPOSIT_LINE_TYPE", "Deposit Line Type", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/cheque-cash-conversion")
    public ResponseEntity<?> getChequeCashConversion(
            @Parameter(description = "Filter text for searching") @RequestParam(required = false) String filter,
            @Parameter(description = "Group identifier") @RequestParam(defaultValue = "1") Long groupPoid,
            @Parameter(description = "Company identifier") @RequestParam(defaultValue = "0") Long companyPoid,
            @Parameter(description = "User identifier") @RequestParam(defaultValue = "0") Long userPoid,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer pageNumber,
            @Parameter(description = "Page size for pagination (1-1000)") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Field to sort by (code, label, description, value)") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (asc, desc)") @RequestParam(required = false) String sortDir) {
        try {
            validateSortParameters(sortBy, sortDir);
            return handleLovRequest(filter, groupPoid, companyPoid, userPoid,
                    "CHEQUE_CASH_CONVERSION", "Cheque Cash Conversion", pageNumber, pageSize, sortBy, sortDir);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

}