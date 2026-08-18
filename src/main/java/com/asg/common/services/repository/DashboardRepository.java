package com.asg.common.services.repository;


import com.asg.common.services.entity.DashboardEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class DashboardRepository {

    private final DataSource dataSource;

    public DashboardRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<DashboardEntity> getPendingApprovals(Long groupPoid, Long companyPoid, Long userPoid) {
        List<DashboardEntity> list = new ArrayList<>();
        String sql = "{ call PROC_GLOB_APPROVAL_PENDING(?, ?, ?, ?, ?) }";

        try (Connection conn = dataSource.getConnection()) {
            // Postgres refcursors only live for the duration of the transaction that opened them
            conn.setAutoCommit(false);

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setString(3, userPoid != null ? userPoid.toString() : null);

                cs.registerOutParameter(4, Types.INTEGER);
                cs.registerOutParameter(5, Types.OTHER); // REF_CURSOR
                cs.execute();

                int count = cs.getInt(4);
                log.debug("Found {} pending approval records", count);

                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    if (rs != null) {
                        long counter = 1;
                        while (rs.next()) {
                            DashboardEntity entity = mapResultSetToEntity(rs);
                            entity.setId(counter++);
                            list.add(entity);
                        }
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Error while calling stored procedure PROC_GLOB_APPROVAL_PENDING", e);
            throw new RuntimeException("Error fetching pending approvals", e);
        }
        return list;
    }

    private DashboardEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        DashboardEntity entity = new DashboardEntity();
        entity.setCompanyPoid(rs.getLong("GROUP_POID"));
        entity.setGroupPoid(rs.getLong("COMPANY_POID"));
        entity.setDocId(rs.getString("DOC_ID"));
        entity.setDocShortName(rs.getString("DOC_SHORT_NAME"));
        entity.setDocKeyPoid(rs.getLong("DOC_KEY_POID"));
        entity.setDocRef(rs.getString("DOC_REF"));
        entity.setDocDate(rs.getDate("DOC_DATE"));
        entity.setDocSummaryInfo(rs.getString("DOC_SUMMARY_INFO"));
        entity.setUserId(rs.getString("USER_ID"));
        entity.setUserRolePoid(rs.getLong("USER_ROLE_POID"));
        entity.setUserPoid(rs.getLong("USER_POID"));
        entity.setActionStatus(rs.getString("ACTION_STATUS"));
        entity.setActionedBy(rs.getString("ACTIONED_BY"));
        entity.setUserName(rs.getString("USER_NAME"));
        entity.setDatetime(rs.getTimestamp("DATE_TIME"));  // Changed to getTimestamp
        entity.setComments(rs.getString("COMMENTS"));
      //  entity.setTaskflowurl(rs.getString("TASKFLOW_URL"));
        return entity;
    }

}
