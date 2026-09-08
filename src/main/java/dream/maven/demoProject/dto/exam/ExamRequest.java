package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamRequest {
    private String name;
    private Long courseId;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String composeType;
    private Boolean allowRetake;
    private List<Long> questionIds;
    private List<RandomRuleItem> composeRule;
}
