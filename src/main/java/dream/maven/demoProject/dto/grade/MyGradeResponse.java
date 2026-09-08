package dream.maven.demoProject.dto.grade;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MyGradeResponse {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long examId;
    private String examName;
    private Integer score;
    private Integer totalScore;
    private BigDecimal accuracy;
    private LocalDateTime submitTime;
}
