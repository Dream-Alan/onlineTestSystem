package dream.maven.demoProject.dto.exam;

import lombok.Data;

/**
 * 随机组卷规则中的一行：某题型抽取 count 道，difficulty 为空表示不限难度。
 */
@Data
public class RandomRuleItem {
    private String type;
    private Integer count;
    private String difficulty;
}