package dream.maven.demoProject.dto.course;

import lombok.Data;

@Data
public class CourseRequest {
    private String name;
    private String code;
    private String semester;
}
