package dream.maven.demoProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dream.maven.demoProject.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
}
