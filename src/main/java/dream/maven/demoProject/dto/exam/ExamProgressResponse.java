package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExamProgressResponse {
    private boolean started;
    private ExamBrief exam;
    private List<StudentQuestionView> questions;
    private Map<String, Object> answers;
    private long remainingSeconds;
}
