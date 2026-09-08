package dream.maven.demoProject.dto.course;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseResponse {
    private Long id;
    private String name;
    private String code;
    private String semester;
    private Integer studentCount;
    private LocalDateTime createdAt;
}
