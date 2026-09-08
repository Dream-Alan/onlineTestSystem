package dream.maven.demoProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dream.maven.demoProject.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
