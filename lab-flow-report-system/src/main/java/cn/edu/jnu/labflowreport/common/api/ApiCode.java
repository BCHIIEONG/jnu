package cn.edu.jnu.labflowreport.common.api;

public final class ApiCode {

    private ApiCode() {
    }

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 40000;
    public static final int VALIDATION_ERROR = 40001;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    public static final int INTERNAL_ERROR = 50000;
}

