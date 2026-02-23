package cn.edu.jnu.labflowreport.admin.service;

final class AdminCsv {

    private AdminCsv() {
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}

