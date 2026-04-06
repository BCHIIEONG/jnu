package cn.edu.jnu.labflowreport.statistics.vo;

import java.math.BigDecimal;
import java.util.List;

public record StatisticsChartVO(
        List<String> categories,
        List<Series> series
) {

    public record Series(
            String name,
            String type,
            List<BigDecimal> data
    ) {
    }
}
