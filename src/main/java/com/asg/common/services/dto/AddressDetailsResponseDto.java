package com.asg.common.services.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDetailsResponseDto {
    private Long addressMasterPoid;
    private BigDecimal addressPoid;
    private String addressName;
    private String addressType;
    private LovGetListDto addressTypeDet;
    private String offTel1;
    private String offTel2;
    private String contactPerson;
    private String designation;
    private String mobile;
    private String fax;
    private String email1;
    private String email2;
    private String website;
    private String poBox;
    private String offNo;
    private String bldg;
    private String road;
    private String areaCity;
    private String state;
    private Long countryPoid;
    private String landMark;
}