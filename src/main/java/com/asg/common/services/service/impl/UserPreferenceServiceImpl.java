package com.asg.common.services.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.services.dto.RecentMenuDto;
import com.asg.common.services.dto.UserPreferenceRequest;
import com.asg.common.services.dto.UserProfileSettingDto;
import com.asg.common.services.service.UserPreferenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    @Autowired
    DataSource dataSource;

    @Override
    public List<UserProfileSettingDto> getUserPreferences() {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            throw new ValidationException("User not authenticated");
        }
        try {
            return getUserPreferences(userPoid);
        } catch (SQLException e) {
            throw new ValidationException("Failed to fetch user preferences");
        }
    }

    @Override
    public void updateUserPreferences(UserPreferenceRequest request) {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            throw new ValidationException("User not authenticated");
        }
        for (UserPreferenceRequest.UserPreferenceItem preference : request.preferences()) {
            String result = updateUserPreferences(userPoid, preference.settingsName(), preference.settingsValue());
            if (!"SUCCESS".equals(result)) {
                throw new ValidationException("Failed to update preference: " + preference.settingsName());
            }
        }
    }

    private List<UserProfileSettingDto> getUserPreferences(Long userPoid) throws SQLException {
        String sql = "{ call PROC_GLOB_USR_PROFILE_LOAD(?, ?) }";
        try (Connection conn = dataSource.getConnection()) {
            // Postgres refcursors only live for the duration of the transaction that opened them
            conn.setAutoCommit(false);

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setLong(1, userPoid);
                cs.registerOutParameter(2, Types.OTHER); // REF_CURSOR
                cs.execute();
                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                    List<UserProfileSettingDto> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(new UserProfileSettingDto(
                                rs.getString("SETTINGS_NAME"),
                                rs.getString("SETTINGS_VALUE")));
                    }
                    conn.commit();
                    return result;
                }
            }
        }
    }

    @Override
    public List<RecentMenuDto> getRecentMenus() {
        String userId = UserContext.getUserId();
        Long userPoid = UserContext.getUserPoid();
        if (userId == null || userPoid == null) {
            throw new ValidationException("User not authenticated");
        }
        try {
            return getRecentMenus(userId, userPoid);
        } catch (SQLException e) {
            throw new ValidationException("Failed to fetch recent menus");
        }
    }

    private List<RecentMenuDto> getRecentMenus(String userId, Long userPoid) throws SQLException {
        String sql = "{ call PROC_GLOB_USR_PROF_RECENT_MENU(?, ?, ?) }";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, userId);
                cs.setLong(2, userPoid);
                cs.registerOutParameter(3, Types.OTHER); // REF_CURSOR
                cs.execute();
                try (ResultSet rs = (ResultSet) cs.getObject(3)) {
                    List<RecentMenuDto> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(new RecentMenuDto(
                                rs.getString("MENU_ID"),
                                rs.getString("MENU_NAME"),
                                rs.getString("MENU_LEVEL"),
                                rs.getString("MENU_GROUP"),
                                rs.getString("TASKFLOW_URL"),
                                rs.getString("ROUTE_NAME"),
                                rs.getString("DOC_TYPE"),
                                rs.getString("MODULE_ID")));
                    }
                    conn.commit();
                    return result;
                }
            }
        }
    }

    @Override
    public void updateRecentMenu(String documentId, Boolean isDocument) {
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            throw new ValidationException("User not authenticated");
        }
        String settingsName = isDocument ? "RecentDocument" : "RecentReport";
        String result = updateUserPreferences(userPoid, settingsName, documentId);
        if (!"SUCCESS".equals(result)) {
            throw new ValidationException("Failed to update recent menu");
        }
    }

    private String updateUserPreferences(Long userPoid, String settingsName, String settingsValue) {
        String sql = "{ call PROC_GLOB_USR_PROFILE_UPDATE(?, ?, ?) }";
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setLong(1, userPoid);
            cs.setString(2, settingsName);
            cs.setString(3, settingsValue);
            cs.execute();
            return "SUCCESS";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }
}