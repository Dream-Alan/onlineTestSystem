package dream.maven.demoProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dream.maven.demoProject.entity.StudentCourse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {
}
