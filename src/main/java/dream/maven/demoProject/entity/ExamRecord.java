package dream.maven.demoProject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long studentId;
    private Integer attemptNo;
    private String state;
    private String answersJson;
    private Integer score;
    private BigDecimal accuracy;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
}
