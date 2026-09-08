package dream.maven.demoProject.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SubmitExamResponse {
    private Integer score;
    private BigDecimal accuracy;
    private Long examRecordId;
}
