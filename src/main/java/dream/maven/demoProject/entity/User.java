package dream.maven.demoProject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String role;
    private String username;
    private String password;
    private String name;
    private String gender;
    private String phone;
    private String college;
    private String major;
    private String className;
    private String avatar;
    private LocalDateTime createdAt;
}
