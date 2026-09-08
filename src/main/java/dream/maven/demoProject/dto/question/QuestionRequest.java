package dream.maven.demoProject.dto.question;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    private Long courseId;
    private String type;
    private String title;
    private List<OptionDto> options;
    private Object answer;
    private String analysis;
    private Integer score;
    private String difficulty;

    @Data
    public static class OptionDto {
        private String content;
    }
}
