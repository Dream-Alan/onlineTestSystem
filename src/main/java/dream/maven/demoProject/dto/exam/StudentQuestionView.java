package dream.maven.demoProject.dto.exam;

import lombok.Data;

@Data
public class StudentQuestionView {
    private Long id;
    private String type;
    private String title;
    private Object options;
    private Integer score;
}
