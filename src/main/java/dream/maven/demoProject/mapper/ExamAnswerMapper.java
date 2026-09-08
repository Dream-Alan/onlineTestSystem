package dream.maven.demoProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dream.maven.demoProject.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {
}
