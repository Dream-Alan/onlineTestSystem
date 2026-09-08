package dream.maven.demoProject.dto.grade;

import lombok.Data;

@Data
public class GradeDetailItem {
    private Long questionId;
    private String type;
    private String title;
    private Object options;
    private Object userAnswer;
    private Object correctAnswer;
    private Boolean isCorrect;
    private Integer score;
    private Integer fullScore;
    private String analysis;
}
