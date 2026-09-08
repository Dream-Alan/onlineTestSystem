package dream.maven.demoProject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String type;
    private String title;
    private String options;
    private String answer;
    private String analysis;
    private Integer score;
    private String difficulty;
    private LocalDateTime createdAt;
}
