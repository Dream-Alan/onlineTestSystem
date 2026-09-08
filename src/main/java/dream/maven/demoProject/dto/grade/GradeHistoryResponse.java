package dream.maven.demoProject.dto.grade;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GradeHistoryResponse {
    private Long id;
    private Long examId;
    private String examName;
    private Integer attemptNo;
    private Integer score;
    private LocalDateTime examTime;
}
