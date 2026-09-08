package dream.maven.demoProject.dto.question;

import lombok.Data;

@Data
public class BatchDeleteRequest {
    private java.util.List<Long> ids;
}
