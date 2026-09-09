package dream.maven.demoProject.dto.course;

import lombok.Data;

@Data
public class CourseStudentResponse {
    private Long userId;
    private String studentNo;
    private String name;
    private String className;
    private String major;
}
