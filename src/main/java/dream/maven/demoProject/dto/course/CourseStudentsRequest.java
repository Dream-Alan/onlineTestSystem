package dream.maven.demoProject.dto.course;

import lombok.Data;

import java.util.List;

@Data
public class CourseStudentsRequest {
    private List<Long> studentIds;
}
