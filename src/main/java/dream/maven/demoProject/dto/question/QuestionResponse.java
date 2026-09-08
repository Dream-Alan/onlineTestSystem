package dream.maven.demoProject.dto.question;

import lombok.Data;

@Data
public class QuestionResponse {
    private Long id;
    private Long courseId;
    private String type;
    private String title;
    private Object options;
    private Object answer;
    private String analysis;
    private Integer score;
    private String difficulty;
}
