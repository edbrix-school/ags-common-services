package com.asg.common.services.dto;

import com.asg.common.services.entity.FavoriteMenuEntity;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class FavoriteMenuReorderRequest {
    private String userId;
    private Long userPoid;
    private Map<String, List<FavoriteMenuEntity>> data = new LinkedHashMap<>();
}
