package com.asg.common.services.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * What an ISO Documents and Policies Access Log row records. Persisted verbatim into
 * {@code ADMIN_ISO_COMP_POLICY_LOG_DTL.LOG_TYPE}, which has a CHECK constraint on these two values —
 * so the names must not be renamed without changing {@code ADMIN_ISO_COMP_POLICY_LOG_CK1}.
 */
public enum IsoPolicyLogType {

    /** The employee opened the file. */
    Accessed,

    /** The employee acknowledged the file. Only valid when the document requires acknowledgement. */
    Acknowledged;

    @JsonCreator
    public static IsoPolicyLogType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (IsoPolicyLogType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid logType: " + value + " (expected Accessed or Acknowledged)");
    }
}
