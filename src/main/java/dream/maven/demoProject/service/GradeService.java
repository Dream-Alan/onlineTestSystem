package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.dto.grade.*;
import dream.maven.demoProject.entity.*;
import dream.maven.demoProject.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class GradeService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PageResult<GradeResponse> getGradeList(Long courseId, String studentName, int page, int size) {
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getState, "finished")
                .orderByDesc(ExamRecord::getSubmitTime);

        if (courseId != null) {
            List<Long> examIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                            .eq(Exam::getCourseId, courseId))
                    .stream().map(Exam::getId).toList();
            if (examIds.isEmpty()) {
                return new PageResult<>(List.of(), 0);
            }
            wrapper.in(ExamRecord::getExamId, examIds);
        }

        Page<ExamRecord> result = examRecordMapper.selectPage(new Page<>(page, size), wrapper);

        List<GradeResponse> list = result.getRecords().stream()
                .map(this::toGradeResponse)
                .filter(g -> !StringUtils.hasText(studentName) || g.getStudentName().contains(studentName))
                .toList();

        return new PageResult<>(list, list.size() == result.getRecords().size() ? result.getTotal() : list.size());
    }

    public List<GradeHistoryResponse> getStudentGradeHistory(Long courseId, Long studentId) {
        List<Long> examIds = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getCourseId, courseId))
                .stream().map(Exam::getId).toList();
        if (examIds.isEmpty()) {
            return List.of();
        }

        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .in(ExamRecord::getExamId, examIds)
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getState, "finished")
                .orderByAsc(ExamRecord::getAttemptNo));

        return records.stream().map(r -> {
            GradeHistoryResponse response = new GradeHistoryResponse();
            response.setId(r.getId());
            response.setExamId(r.getExamId());
            Exam exam = examMapper.selectById(r.getExamId());
            response.setExamName(exam != null ? exam.getName() : null);
            response.setAttemptNo(r.getAttemptNo());
            response.setScore(r.getScore());
            response.setExamTime(r.getSubmitTime());
            return response;
        }).toList();
    }

    public PageResult<MyGradeResponse> getMyGrades(int page, int size) {
        Long studentId = UserContext.get().getUserId();
        Page<ExamRecord> result = examRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getStudentId, studentId)
                        .eq(ExamRecord::getState, "finished")
                        .orderByDesc(ExamRecord::getSubmitTime));

        List<MyGradeResponse> list = result.getRecords().stream().map(r -> {
            MyGradeResponse response = new MyGradeResponse();
            response.setId(r.getId());
            Exam exam = examMapper.selectById(r.getExamId());
            if (exam != null) {
                response.setExamId(exam.getId());
                response.setExamName(exam.getName());
                response.setCourseId(exam.getCourseId());
                response.setTotalScore(exam.getTotalScore());
                Course course = courseMapper.selectById(exam.getCourseId());
                response.setCourseName(course != null ? course.getName() : null);
            }
            response.setScore(r.getScore());
            response.setAccuracy(r.getAccuracy());
            response.setSubmitTime(r.getSubmitTime());
            return response;
        }).toList();

        return new PageResult<>(list, result.getTotal());
    }

    public GradeDetailResponse getGradeDetail(Long examRecordId) {
        ExamRecord record = examRecordMapper.selectById(examRecordId);
        if (record == null) {
            throw new BusinessException(404, "考试记录不存在");
        }

        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getExamRecordId, examRecordId));

        List<GradeDetailItem> items = answers.stream().map(a -> {
            GradeDetailItem item = new GradeDetailItem();
            Question question = questionMapper.selectById(a.getQuestionId());
            item.setQuestionId(a.getQuestionId());
            if (question != null) {
                item.setType(question.getType());
                item.setTitle(question.getTitle());
                item.setOptions(readJson(question.getOptions()));
                item.setCorrectAnswer(readJson(question.getAnswer()));
                item.setAnalysis(question.getAnalysis());
            }
            item.setUserAnswer(readJson(a.getUserAnswer()));
            item.setIsCorrect(a.getIsCorrect());
            item.setScore(a.getScore());
            item.setFullScore(a.getFullScore());
            return item;
        }).toList();

        GradeDetailResponse response = new GradeDetailResponse();
        response.setId(record.getId());
        response.setScore(record.getScore());
        Exam exam = examMapper.selectById(record.getExamId());
        response.setTotalScore(exam != null ? exam.getTotalScore() : null);
        response.setAccuracy(record.getAccuracy());
        response.setItems(items);
        return response;
    }

    private GradeResponse toGradeResponse(ExamRecord record) {
        GradeResponse response = new GradeResponse();
        response.setId(record.getId());
        response.setStudentId(record.getStudentId());

        User student = userMapper.selectById(record.getStudentId());
        if (student != null) {
            response.setStudentName(student.getName());
            response.setStudentNo(student.getUsername());
        }

        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null) {
            response.setExamId(exam.getId());
            response.setExamName(exam.getName());
            response.setCourseId(exam.getCourseId());
            Course course = courseMapper.selectById(exam.getCourseId());
            response.setCourseName(course != null ? course.getName() : null);
        }

        response.setScore(record.getScore());
        response.setAccuracy(record.getAccuracy());
        response.setExamTime(record.getSubmitTime());
        return response;
    }

    private Object readJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
