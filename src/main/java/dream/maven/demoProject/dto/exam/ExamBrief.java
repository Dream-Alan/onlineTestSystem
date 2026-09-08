package dream.maven.demoProject.dto.exam;

import lombok.Data;

@Data
public class ExamBrief {
    private Long id;
    private String name;
    private Integer duration;

    public ExamBrief(Long id, String name, Integer duration) {
        this.id = id;
        this.name = name;
        this.duration = duration;
    }
}
