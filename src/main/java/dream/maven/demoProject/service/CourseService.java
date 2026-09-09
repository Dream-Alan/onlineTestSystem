package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.dto.course.CourseRequest;
import dream.maven.demoProject.dto.course.CourseResponse;
import dream.maven.demoProject.dto.course.CourseStudentResponse;
import dream.maven.demoProject.entity.Course;
import dream.maven.demoProject.entity.Exam;
import dream.maven.demoProject.entity.ExamAnswer;
import dream.maven.demoProject.entity.ExamQuestion;
import dream.maven.demoProject.entity.ExamRecord;
import dream.maven.demoProject.entity.Question;
import dream.maven.demoProject.entity.StudentCourse;
import dream.maven.demoProject.entity.User;
import dream.maven.demoProject.mapper.CourseMapper;
import dream.maven.demoProject.mapper.ExamAnswerMapper;
import dream.maven.demoProject.mapper.ExamMapper;
import dream.maven.demoProject.mapper.ExamQuestionMapper;
import dream.maven.demoProject.mapper.ExamRecordMapper;
import dream.maven.demoProject.mapper.QuestionMapper;
import dream.maven.demoProject.mapper.StudentCourseMapper;
import dream.maven.demoProject.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentCourseMapper studentCourseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

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

    @Transactional
    public void deleteCourse(Long id) {
        getOwnedCourse(id);

        List<Long> examIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getCourseId, id))
                .stream().map(Exam::getId).toList();

        if (!examIds.isEmpty()) {
            List<Long> recordIds = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                            .in(ExamRecord::getExamId, examIds))
                    .stream().map(ExamRecord::getId).toList();

            if (!recordIds.isEmpty()) {
                examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>()
                        .in(ExamAnswer::getExamRecordId, recordIds));
            }
            examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>().in(ExamRecord::getExamId, examIds));
            examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().in(ExamQuestion::getExamId, examIds));
            examMapper.delete(new LambdaQueryWrapper<Exam>().in(Exam::getId, examIds));
        }

        questionMapper.delete(new LambdaQueryWrapper<Question>().eq(Question::getCourseId, id));
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getCourseId, id));
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

    public List<CourseStudentResponse> listStudents(Long courseId) {
        getOwnedCourse(courseId);
        List<Long> studentIds = studentCourseMapper.selectList(new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getCourseId, courseId))
                .stream().map(StudentCourse::getStudentId).distinct().toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(studentIds).stream()
                .sorted(Comparator.comparing(User::getUsername))
                .map(this::toStudentResponse)
                .toList();
    }

    public List<CourseStudentResponse> listAvailableStudents(Long courseId, String keyword) {
        getOwnedCourse(courseId);
        List<Long> enrolledIds = studentCourseMapper.selectList(new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getCourseId, courseId))
                .stream().map(StudentCourse::getStudentId).toList();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getRole, "student");
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw).or().like(User::getName, kw));
        }
        if (!enrolledIds.isEmpty()) {
            wrapper.notIn(User::getId, enrolledIds);
        }
        wrapper.orderByAsc(User::getUsername);

        return userMapper.selectList(wrapper).stream()
                .map(this::toStudentResponse)
                .toList();
    }

    public void addStudents(Long courseId, List<Long> studentIds) {
        getOwnedCourse(courseId);
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的学生");
        }
        List<Long> distinctIds = studentIds.stream().distinct().toList();
        List<User> users = userMapper.selectBatchIds(distinctIds);
        boolean hasInvalid = users.size() != distinctIds.size()
                || users.stream().anyMatch(u -> !"student".equals(u.getRole()));
        if (hasInvalid) {
            throw new BusinessException(400, "存在无效的学生账号");
        }
        for (Long studentId : distinctIds) {
            Long exists = studentCourseMapper.selectCount(new LambdaQueryWrapper<StudentCourse>()
                    .eq(StudentCourse::getStudentId, studentId)
                    .eq(StudentCourse::getCourseId, courseId));
            if (exists > 0) {
                continue;
            }
            StudentCourse enrollment = new StudentCourse();
            enrollment.setStudentId(studentId);
            enrollment.setCourseId(courseId);
            studentCourseMapper.insert(enrollment);
        }
    }

    public void removeStudent(Long courseId, Long studentId) {
        getOwnedCourse(courseId);
        studentCourseMapper.delete(new LambdaQueryWrapper<StudentCourse>()
                .eq(StudentCourse::getCourseId, courseId)
                .eq(StudentCourse::getStudentId, studentId));
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

    private CourseStudentResponse toStudentResponse(User user) {
        CourseStudentResponse response = new CourseStudentResponse();
        response.setUserId(user.getId());
        response.setStudentNo(user.getUsername());
        response.setName(user.getName());
        response.setClassName(user.getClassName());
        response.setMajor(user.getMajor());
        return response;
    }
}
