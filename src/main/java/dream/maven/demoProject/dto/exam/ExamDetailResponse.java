package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamDetailResponse {
    private Long id;
    private String name;
    private Long courseId;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowRetake;
    private String composeType;
    private Integer totalScore;
    private List<RandomRuleItem> composeRule;
    private List<QuestionRef> questions;

    @Data
    public static class QuestionRef {
        private Long questionId;
        private Integer order;

        public QuestionRef(Long questionId, Integer order) {
            this.questionId = questionId;
            this.order = order;
        }
    }
}
