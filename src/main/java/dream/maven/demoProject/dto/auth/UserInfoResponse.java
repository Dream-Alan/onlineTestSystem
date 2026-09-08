package dream.maven.demoProject.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoResponse {
    private Long id;
    private String name;
    private String gender;
    private String phone;
    private String college;
    private String avatar;

    // 学生专属
    private String studentNo;
    private String major;
    private String className;

    // 教师专属
    private String teacherNo;
    private List<CourseBrief> courses;

    @Data
    public static class CourseBrief {
        private Long id;
        private String name;

        public CourseBrief(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
