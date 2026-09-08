package dream.maven.demoProject.dto.grade;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long courseId;
    private String courseName;
    private Long examId;
    private String examName;
    private Integer score;
    private BigDecimal accuracy;
    private LocalDateTime examTime;
}
