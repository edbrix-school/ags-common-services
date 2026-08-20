package com.asg.common.services.repository;

import com.asg.common.services.entity.FavoriteMenuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Repository
public class FavoriteMenuRepository {

    @Autowired
    private DataSource dataSource;

    public List<FavoriteMenuEntity> getUnassignedFavList(String userId, Long userPoid, String search) throws SQLException {
        String sql = "{ call PROC_GLOB_FAV_MENU_LIST_FULL(?, ?, ?, ?) }";
        List<FavoriteMenuEntity> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            // Postgres refcursors only live for the duration of the transaction that opened them
            conn.setAutoCommit(false);

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, userId);
                cs.setLong(2, userPoid);
                cs.setString(3, search != null ? search : "");
                cs.registerOutParameter(4, Types.OTHER); // REF_CURSOR

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
            conn.commit();
        }

        return results;
    }


    public List<FavoriteMenuEntity> getFavoriteMenuList(@Param("userPoid") Long userPoid,
                                                        @Param("userId") String userId) throws SQLException {
        // PROC_GLOB_FAV_MENU_LIST's refcursor OUT param is 3rd of 3, not first — pgjdbc's
        // CallableStatement.registerOutParameter only binds a REF_CURSOR correctly in the first
        // position, so it was silently dropped. A plain CALL query sidesteps that entirely:
        // Postgres returns the cursor's name as an ordinary one-row ResultSet, then a separate
        // FETCH ALL FROM "<name>" reads the actual rows.
        List<FavoriteMenuEntity> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String cursorName;
                try (PreparedStatement ps = conn.prepareStatement("CALL PROC_GLOB_FAV_MENU_LIST(?, ?, NULL)")) {
                    ps.setString(1, userId);
                    ps.setLong(2, userPoid);
                    try (ResultSet crs = ps.executeQuery()) {
                        cursorName = crs.next() ? crs.getString(1) : null;
                    }
                }

                if (cursorName != null) {
                    try (Statement fetchStmt = conn.createStatement();
                         ResultSet rs = fetchStmt.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
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
                conn.commit();
            } finally {
                conn.setAutoCommit(true);
            }
        }

        enrichWithIdAuto(userPoid, results);
        return results;
    }

    private void enrichWithIdAuto(Long userPoid, List<FavoriteMenuEntity> menus) throws SQLException {
        if (menus.isEmpty()) {
            return;
        }

        String sql = """
                SELECT ID_AUTO, DOC_ID, FAVORITE_CATEGORY, DOC_SEQ_NO, CAT_SEQ_NO
                FROM GLOBAL_FAVORITE_MENU
                WHERE USER_POID = ?
                """;

        Map<String, Long> idByKey = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userPoid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long docSeqNo = readLongOrZero(rs, "DOC_SEQ_NO");
                    long catSeqNo = readLongOrZero(rs, "CAT_SEQ_NO");
                    String key = buildFavoriteKey(
                            rs.getString("DOC_ID"),
                            rs.getString("FAVORITE_CATEGORY"),
                            docSeqNo,
                            catSeqNo
                    );
                    idByKey.put(key, rs.getLong("ID_AUTO"));
                }
            }
        }

        for (FavoriteMenuEntity menu : menus) {
            if (menu.getId() != null) {
                continue;
            }
            String key = buildFavoriteKey(
                    menu.getMenuId(),
                    menu.getMenuGroup(),
                    menu.getDocSeqNo() != null ? menu.getDocSeqNo() : 0L,
                    menu.getCatSeqNo() != null ? menu.getCatSeqNo() : 0L
            );
            Long idAuto = idByKey.get(key);
            if (idAuto != null) {
                menu.setId(idAuto);
            }
        }
    }

    private long readLongOrZero(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? 0L : value;
    }

    private String buildFavoriteKey(String docId, String category, long docSeqNo, long catSeqNo) {
        return String.join("|",
                docId != null ? docId : "",
                category != null ? category : "",
                String.valueOf(docSeqNo),
                String.valueOf(catSeqNo)
        );
    }

    private void mapSeqColumns(ResultSet rs, FavoriteMenuEntity menu) throws SQLException {
        if (hasColumn(rs, "ID_AUTO")) {
            Long idAuto = rs.getLong("ID_AUTO");
            if (!rs.wasNull()) {
                menu.setId(idAuto);
            }
        }
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

        String sql = "{ call PROC_GLOB_FAV_MENU_LIST_ADD(?, ?, ?, ?, ?) }";
        String status;

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.setString(3, categoryValue);
            cs.setString(4, selectedDocIds);
            cs.registerOutParameter(5, Types.VARCHAR);

            cs.execute();
            status = cs.getString(5);
        }
        return status;
    }

    public String removeFavoriteMenuList(String userId, Long userPoid, String categoryValue, String selectedDocIdList) throws SQLException {
        String sql = "{ call PROC_GLOB_FAV_MENU_LIST_REMOVE(?, ?, ?, ?, ?) }";
        String status;

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, userId);
            cs.setLong(2, userPoid);
            cs.setString(3, categoryValue);
            cs.setString(4, selectedDocIdList);
            cs.registerOutParameter(5, Types.VARCHAR);

            cs.execute();

            status = cs.getString(5);
        }

        return status;
    }

    public int updateFavoriteMenuOrder(Long userPoid, String menuGroup, String menuId,
                                     long catSeqNo, long docSeqNo) throws SQLException {
        String sql = """
                UPDATE GLOBAL_FAVORITE_MENU
                SET CAT_SEQ_NO = ?, DOC_SEQ_NO = ?
                WHERE USER_POID = ? AND DOC_ID = ? AND FAVORITE_CATEGORY = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, catSeqNo);
            ps.setLong(2, docSeqNo);
            ps.setLong(3, userPoid);
            ps.setString(4, menuId);
            ps.setString(5, menuGroup);

            return ps.executeUpdate();
        }
    }

    public int updateFavoriteMenuOrderById(Long userPoid, Long idAuto,
                                           long catSeqNo, long docSeqNo) throws SQLException {
        String sql = """
                UPDATE GLOBAL_FAVORITE_MENU
                SET CAT_SEQ_NO = ?, DOC_SEQ_NO = ?
                WHERE ID_AUTO = ? AND USER_POID = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, catSeqNo);
            ps.setLong(2, docSeqNo);
            ps.setLong(3, idAuto);
            ps.setLong(4, userPoid);

            return ps.executeUpdate();
        }
    }
}
