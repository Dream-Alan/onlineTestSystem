package dream.maven.demoProject.dto.auth;

import lombok.Data;

@Data
public class LoginUserInfo {
    private Long id;
    private String name;
    private String username;
    private String avatar;
}
