package dream.maven.demoProject.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class StartExamResponse {
    private ExamBrief exam;
    private List<StudentQuestionView> questions;
}
