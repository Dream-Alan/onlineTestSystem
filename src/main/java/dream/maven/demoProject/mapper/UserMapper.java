package dream.maven.demoProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dream.maven.demoProject.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
