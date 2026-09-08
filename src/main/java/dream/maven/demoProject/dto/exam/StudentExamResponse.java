package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentExamResponse {
    private Long id;
    private String name;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowRetake;
    private String status;
    private Integer score;
    private Integer attemptCount;
}
