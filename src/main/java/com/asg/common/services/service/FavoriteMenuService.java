package com.asg.common.services.service;

import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.services.dto.FavoriteMenuReorderRequest;
import com.asg.common.services.dto.FavoriteMenuRequest;
import com.asg.common.services.entity.FavoriteMenuEntity;
import com.asg.common.services.repository.FavoriteMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteMenuService {

    @Autowired
    private FavoriteMenuRepository favoriteMenuRepository;

    public List<FavoriteMenuEntity> getFavoriteList(Long userPoid, String userId) throws SQLException {
        return favoriteMenuRepository.getFavoriteMenuList(userPoid, userId);
    }

    public Map<String, List<FavoriteMenuEntity>> getGroupedFavoriteList(Long userPoid, String userId) throws SQLException {
        List<FavoriteMenuEntity> favorites = favoriteMenuRepository.getFavoriteMenuList(userPoid, userId);
        return favorites.stream()
                .collect(Collectors.groupingBy(
                        menu -> menu.getMenuGroup() != null ? menu.getMenuGroup() : "",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public Map<String, Object> getUnassignedFavList(Long userPoid, String userId, String search, Pageable pageable) throws SQLException {
        List<FavoriteMenuEntity> allResults = favoriteMenuRepository.getUnassignedFavList(userId, userPoid, search);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        List<FavoriteMenuEntity> pagedList = allResults.subList(start, end);

        Page<FavoriteMenuEntity> page = new PageImpl<>(pagedList, pageable, allResults.size());

        return PaginationUtil.wrapPage(page,null);
    }


    public String addFavoriteMenu(FavoriteMenuRequest request) throws SQLException {
        return favoriteMenuRepository.addFavoriteMenu(
                request.getUserId(),
                request.getUserPoid(),
                request.getMenuGroup(),
                request.getSelectedDocIds()
        );
    }

    public String removeFavoriteMenus(String userId, Long userPoid, String categoryValue, String selectedDocIdList) throws SQLException {
        return favoriteMenuRepository.removeFavoriteMenuList(userId, userPoid, categoryValue, selectedDocIdList);
    }

    public String reorderFavoriteMenus(FavoriteMenuReorderRequest request) throws SQLException {
        if (request.getData() == null || request.getData().isEmpty()) {
            throw new RuntimeException("data is required");
        }

        int updatedCount = 0;
        int catSeqNo = 0;

        for (Map.Entry<String, List<FavoriteMenuEntity>> entry : request.getData().entrySet()) {
            String menuGroup = entry.getKey();
            List<FavoriteMenuEntity> items = entry.getValue();
            if (items == null || items.isEmpty()) {
                catSeqNo++;
                continue;
            }

            int docSeqNo = 0;
            for (FavoriteMenuEntity item : items) {
                int rows;
                if (item.getId() != null) {
                    rows = favoriteMenuRepository.updateFavoriteMenuOrderById(
                            request.getUserPoid(), item.getId(), catSeqNo, docSeqNo);
                } else {
                    String menuId = item.getMenuId();
                    String group = item.getMenuGroup() != null ? item.getMenuGroup() : menuGroup;
                    rows = favoriteMenuRepository.updateFavoriteMenuOrder(
                            request.getUserPoid(), group, menuId, catSeqNo, docSeqNo);
                }
                if (rows == 0) {
                    throw new RuntimeException(
                            "Favorite menu not found for menuGroup=" + menuGroup
                                    + ", menuId=" + item.getMenuId() + ", id=" + item.getId()
                    );
                }
                updatedCount += rows;
                docSeqNo++;
            }
            catSeqNo++;
        }

        return "SUCCESS: Updated " + updatedCount + " favorite menu record(s)";
    }
}
