package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamResponse {
    private Long id;
    private String name;
    private Long courseId;
    private String courseName;
    private Integer questionCount;
    private Integer totalScore;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean allowRetake;
    private String composeType;
}
