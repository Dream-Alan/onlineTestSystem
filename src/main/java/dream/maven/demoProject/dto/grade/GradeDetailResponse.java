package dream.maven.demoProject.dto.grade;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GradeDetailResponse {
    private Long id;
    private Integer score;
    private Integer totalScore;
    private BigDecimal accuracy;
    private List<GradeDetailItem> items;
}
