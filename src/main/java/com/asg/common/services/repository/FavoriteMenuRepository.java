package com.asg.common.services.repository;

import com.asg.common.services.entity.FavoriteMenuEntity;
import oracle.jdbc.internal.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@Repository
public class FavoriteMenuRepository {

    @Autowired
    private DataSource dataSource;

    public List<FavoriteMenuEntity> getUnassignedFavList(String userId, Long userPoid, String search) throws SQLException {
        String sql = "BEGIN PROC_GLOB_FAV_MENU_LIST_FULL(?, ?, ?, ?); END;";
        List<FavoriteMenuEntity> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.setString(3, search != null ? search : "");
            cs.registerOutParameter(4, OracleTypes.CURSOR); // REF_CURSOR

            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(4)) {
                while (rs.next()) {
                    FavoriteMenuEntity menu = new FavoriteMenuEntity();
                    menu.setMenuId(rs.getString("MENU_ID"));
                    menu.setMenuName(rs.getString("MENU_NAME"));
                    menu.setMenuLevel(rs.getLong("MENU_LEVEL"));
                    menu.setMenuGroup(rs.getString("MENU_GROUP"));
                    menu.setTaskflowUrl(rs.getString("TASKFLOW_URL"));
                    menu.setDocType(rs.getString("DOC_TYPE"));
                    menu.setModuleId(rs.getString("MODULE_ID"));
                    mapSeqColumns(rs, menu);
                    results.add(menu);
                }
            }
        }

        return results;
    }


    public List<FavoriteMenuEntity> getFavoriteMenuList(@Param("userPoid") Long userPoid,
                                                        @Param("userId") String userId) throws SQLException {
        String sql = "BEGIN PROC_GLOB_FAV_MENU_LIST(?, ?, ?); END;";
        List<FavoriteMenuEntity> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.registerOutParameter(3, OracleTypes.CURSOR); // REF_CURSOR

            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(3)) {
                while (rs.next()) {
                    FavoriteMenuEntity menu = new FavoriteMenuEntity();
                    menu.setMenuId(rs.getString("MENU_ID"));
                    menu.setMenuName(rs.getString("MENU_NAME"));
                    menu.setMenuLevel(rs.getLong("MENU_LEVEL"));
                    menu.setMenuGroup(rs.getString("MENU_GROUP"));
                    menu.setTaskflowUrl(rs.getString("TASKFLOW_URL"));
                    menu.setDocType(rs.getString("DOC_TYPE"));
                    menu.setModuleId(rs.getString("MODULE_ID"));
                    mapSeqColumns(rs, menu);
                    results.add(menu);
                }
            }
        }

        return results;
    }

    private void mapSeqColumns(ResultSet rs, FavoriteMenuEntity menu) throws SQLException {
        if (hasColumn(rs, "CAT_SEQ_NO")) {
            Long catSeqNo = rs.getLong("CAT_SEQ_NO");
            if (!rs.wasNull()) {
                menu.setCatSeqNo(catSeqNo);
            }
        }
        if (hasColumn(rs, "DOC_SEQ_NO")) {
            Long docSeqNo = rs.getLong("DOC_SEQ_NO");
            if (!rs.wasNull()) {
                menu.setDocSeqNo(docSeqNo);
            }
        }
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

    public String addFavoriteMenu(String userId, Long userPoid, String categoryValue, String selectedDocIds) throws SQLException {

        String sql = "BEGIN PROC_GLOB_FAV_MENU_LIST_ADD(?, ?, ?, ?, ?); END;";
        String status;

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.setString(3, categoryValue);
            cs.setString(4, selectedDocIds);
            cs.registerOutParameter(5, OracleTypes.VARCHAR);

            cs.execute();
            status = cs.getString(5);
        }
        return status;
    }

    public String removeFavoriteMenuList(String userId, Long userPoid, String categoryValue, String selectedDocIdList) throws SQLException {
        String sql = "BEGIN PROC_GLOB_FAV_MENU_LIST_REMOVE(?, ?, ?, ?, ?); END;";
        String status;

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.setString(3, categoryValue);
            cs.setString(4, selectedDocIdList);
            cs.registerOutParameter(5, OracleTypes.VARCHAR);

            cs.execute();

            status = cs.getString(5);
        }

        return status;
    }
}
