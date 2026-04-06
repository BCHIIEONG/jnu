package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.ReportReviewIssueTagEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportReviewIssueTagMapper extends BaseMapper<ReportReviewIssueTagEntity> {

    @Select("""
            SELECT tag_code
            FROM report_review_issue_tag
            WHERE review_id = #{reviewId}
            ORDER BY id ASC
            """)
    List<String> findTagCodesByReviewId(Long reviewId);

    @Select({
            "<script>",
            "SELECT review_id, tag_code",
            "FROM report_review_issue_tag",
            "WHERE review_id IN",
            "<foreach collection='reviewIds' item='reviewId' open='(' separator=',' close=')'>",
            "#{reviewId}",
            "</foreach>",
            "ORDER BY review_id ASC, id ASC",
            "</script>"
    })
    List<IssueTagRow> findRowsByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);

    record IssueTagRow(Long reviewId, String tagCode) {
    }
}
