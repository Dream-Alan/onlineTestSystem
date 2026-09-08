package dream.maven.demoProject.dto.auth;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String college;
    private String gender;
    private String major;
    private String className;
}
