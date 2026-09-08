package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.util.Map;

@Data
public class AnswerSubmission {
    private Map<String, Object> answers;
}
