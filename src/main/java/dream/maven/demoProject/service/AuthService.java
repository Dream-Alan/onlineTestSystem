package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.dto.auth.*;
import dream.maven.demoProject.entity.Course;
import dream.maven.demoProject.entity.User;
import dream.maven.demoProject.mapper.CourseMapper;
import dream.maven.demoProject.mapper.UserMapper;
import dream.maven.demoProject.util.JwtUtil;
import dream.maven.demoProject.util.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getRole, request.getRole()));

        if (user == null || !user.getPassword().equals(Md5Util.md5(request.getPassword()))) {
            throw new BusinessException(400, "账号或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        LoginUserInfo userInfo = new LoginUserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setUsername(user.getUsername());
        userInfo.setAvatar(user.getAvatar());

        return new LoginResponse(token, user.getRole(), userInfo);
    }

    public UserInfoResponse getUserInfo() {
        UserContext ctx = UserContext.get();
        User user = userMapper.selectById(ctx.getUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setGender(user.getGender());
        response.setPhone(user.getPhone());
        response.setCollege(user.getCollege());
        response.setAvatar(user.getAvatar());

        if ("teacher".equals(user.getRole())) {
            response.setTeacherNo(user.getUsername());
            List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, user.getId()));
            response.setCourses(courses.stream()
                    .map(c -> new UserInfoResponse.CourseBrief(c.getId(), c.getName()))
                    .toList());
        } else {
            response.setStudentNo(user.getUsername());
            response.setMajor(user.getMajor());
            response.setClassName(user.getClassName());
        }

        return response;
    }

    public UserInfoResponse updateProfile(UpdateProfileRequest request) {
        UserContext ctx = UserContext.get();
        User user = userMapper.selectById(ctx.getUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getCollege() != null) user.setCollege(request.getCollege());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getMajor() != null) user.setMajor(request.getMajor());
        if (request.getClassName() != null) user.setClassName(request.getClassName());

        userMapper.updateById(user);
        return getUserInfo();
    }

    public void changePassword(ChangePasswordRequest request) {
        UserContext ctx = UserContext.get();
        User user = userMapper.selectById(ctx.getUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!user.getPassword().equals(Md5Util.md5(request.getOldPassword()))) {
            throw new BusinessException(400, "原密码错误");
        }

        user.setPassword(Md5Util.md5(request.getNewPassword()));
        userMapper.updateById(user);
    }
}
