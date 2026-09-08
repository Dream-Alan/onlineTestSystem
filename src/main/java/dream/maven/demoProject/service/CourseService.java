package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.dto.course.CourseRequest;
import dream.maven.demoProject.dto.course.CourseResponse;
import dream.maven.demoProject.entity.Course;
import dream.maven.demoProject.entity.StudentCourse;
import dream.maven.demoProject.mapper.CourseMapper;
import dream.maven.demoProject.mapper.StudentCourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    public PageResult<CourseResponse> getCourseList(String keyword, int page, int size) {
        Long teacherId = UserContext.get().getUserId();
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId)
                .like(StringUtils.hasText(keyword), Course::getName, keyword)
                .orderByDesc(Course::getCreatedAt);

        Page<Course> result = courseMapper.selectPage(new Page<>(page, size), wrapper);
        List<CourseResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(list, result.getTotal());
    }

    public List<CourseResponse> getMyCourses() {
        UserContext ctx = UserContext.get();
        List<Course> courses;
        if ("teacher".equals(ctx.getRole())) {
            courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, ctx.getUserId()));
        } else {
            List<StudentCourse> enrollments = studentCourseMapper.selectList(
                    new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, ctx.getUserId()));
            List<Long> courseIds = enrollments.stream().map(StudentCourse::getCourseId).toList();
            if (courseIds.isEmpty()) {
                return List.of();
            }
            courses = courseMapper.selectBatchIds(courseIds);
        }
        return courses.stream().map(this::toResponse).toList();
    }

    public CourseResponse createCourse(CourseRequest request) {
        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setSemester(request.getSemester());
        course.setTeacherId(UserContext.get().getUserId());
        courseMapper.insert(course);
        return toResponse(course);
    }

    public void updateCourse(Long id, CourseRequest request) {
        Course course = getOwnedCourse(id);
        if (request.getName() != null) course.setName(request.getName());
        if (request.getCode() != null) course.setCode(request.getCode());
        if (request.getSemester() != null) course.setSemester(request.getSemester());
        courseMapper.updateById(course);
    }

    public void deleteCourse(Long id) {
        getOwnedCourse(id);
        courseMapper.deleteById(id);
    }

    public void enrollCourse(Long courseId) {
        Long studentId = UserContext.get().getUserId();
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        Long exists = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId));
        if (exists > 0) {
            throw new BusinessException(400, "已选过该课程");
        }
        StudentCourse enrollment = new StudentCourse();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        studentCourseMapper.insert(enrollment);
    }

    private Course getOwnedCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!course.getTeacherId().equals(UserContext.get().getUserId())) {
            throw new BusinessException(403, "无权限操作该课程");
        }
        return course;
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setName(course.getName());
        response.setCode(course.getCode());
        response.setSemester(course.getSemester());
        response.setCreatedAt(course.getCreatedAt());
        Long count = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getCourseId, course.getId()));
        response.setStudentCount(count.intValue());
        return response;
    }
}
