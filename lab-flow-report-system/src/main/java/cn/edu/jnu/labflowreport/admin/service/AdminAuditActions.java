package cn.edu.jnu.labflowreport.admin.service;

public final class AdminAuditActions {

    private AdminAuditActions() {
    }

    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String USER_RESET_PASSWORD = "USER_RESET_PASSWORD";
    public static final String USER_SET_ROLES = "USER_SET_ROLES";
    public static final String USER_IMPORT = "USER_IMPORT";
    public static final String USER_EXPORT = "USER_EXPORT";

    public static final String DEPARTMENT_CREATE = "DEPARTMENT_CREATE";
    public static final String DEPARTMENT_UPDATE = "DEPARTMENT_UPDATE";
    public static final String DEPARTMENT_DELETE = "DEPARTMENT_DELETE";
    public static final String DEPARTMENT_EXPORT = "DEPARTMENT_EXPORT";

    public static final String CLASS_CREATE = "CLASS_CREATE";
    public static final String CLASS_UPDATE = "CLASS_UPDATE";
    public static final String CLASS_DELETE = "CLASS_DELETE";
    public static final String CLASS_EXPORT = "CLASS_EXPORT";

    public static final String LAB_ROOM_CREATE = "LAB_ROOM_CREATE";
    public static final String LAB_ROOM_UPDATE = "LAB_ROOM_UPDATE";
    public static final String LAB_ROOM_DELETE = "LAB_ROOM_DELETE";
    public static final String LAB_ROOM_EXPORT = "LAB_ROOM_EXPORT";

    public static final String DEVICE_CREATE = "DEVICE_CREATE";
    public static final String DEVICE_UPDATE = "DEVICE_UPDATE";
    public static final String DEVICE_DELETE = "DEVICE_DELETE";
    public static final String DEVICE_EXPORT = "DEVICE_EXPORT";

    public static final String SEMESTER_CREATE = "SEMESTER_CREATE";
    public static final String SEMESTER_UPDATE = "SEMESTER_UPDATE";
    public static final String SEMESTER_DELETE = "SEMESTER_DELETE";
    public static final String SEMESTER_EXPORT = "SEMESTER_EXPORT";

    public static final String AUDIT_EXPORT = "AUDIT_EXPORT";
}
