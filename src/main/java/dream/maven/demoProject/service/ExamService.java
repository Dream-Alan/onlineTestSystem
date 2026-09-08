package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.dto.exam.*;
import dream.maven.demoProject.entity.*;
import dream.maven.demoProject.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ================= 教师端 =================

    public PageResult<ExamResponse> getExamList(Long courseId, String status, int page, int size) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .eq(courseId != null, Exam::getCourseId, courseId)
                .eq(StringUtils.hasText(status), Exam::getStatus, status)
                .orderByDesc(Exam::getCreatedAt);

        Page<Exam> result = examMapper.selectPage(new Page<>(page, size), wrapper);
        List<ExamResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(list, result.getTotal());
    }

    public ExamDetailResponse getExamById(Long id) {
        Exam exam = requireExam(id);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id)
                .orderByAsc(ExamQuestion::getOrderNum));

        ExamDetailResponse response = new ExamDetailResponse();
        response.setId(exam.getId());
        response.setName(exam.getName());
        response.setCourseId(exam.getCourseId());
        response.setDuration(exam.getDuration());
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setAllowRetake(exam.getAllowRetake());
        response.setQuestions(examQuestions.stream()
                .map(eq -> new ExamDetailResponse.QuestionRef(eq.getQuestionId(), eq.getOrderNum()))
                .toList());
        return response;
    }

    public ExamResponse createExam(ExamRequest request) {
        Exam exam = new Exam();
        applyRequest(exam, request);
        exam.setStatus("draft");
        examMapper.insert(exam);
        attachQuestions(exam, request);
        return toResponse(exam);
    }

    public void updateExam(Long id, ExamRequest request) {
        Exam exam = requireExam(id);
        applyRequest(exam, request);
        examMapper.updateById(exam);
        if (request.getQuestionIds() != null) {
            examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
            attachQuestions(exam, request);
        }
    }

    public void deleteExam(Long id) {
        requireExam(id);
        examMapper.deleteById(id);
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
    }

    public void publishExam(Long id) {
        Exam exam = requireExam(id);
        exam.setStatus("published");
        examMapper.updateById(exam);
    }

    // ================= 学生端 =================

    public List<StudentExamResponse> getStudentExamsByCourse(Long courseId) {
        Long studentId = UserContext.get().getUserId();
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getCourseId, courseId)
                .eq(Exam::getStatus, "published")
                .orderByDesc(Exam::getCreatedAt));

        List<StudentExamResponse> result = new ArrayList<>();
        for (Exam exam : exams) {
            List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                    .eq(ExamRecord::getExamId, exam.getId())
                    .eq(ExamRecord::getStudentId, studentId)
                    .orderByDesc(ExamRecord::getAttemptNo));

            StudentExamResponse response = new StudentExamResponse();
            response.setId(exam.getId());
            response.setName(exam.getName());
            response.setDuration(exam.getDuration());
            response.setStartTime(exam.getStartTime());
            response.setEndTime(exam.getEndTime());
            response.setAllowRetake(exam.getAllowRetake());
            response.setAttemptCount((int) records.stream().filter(r -> "finished".equals(r.getState())).count());

            ExamRecord ongoing = records.stream().filter(r -> "ongoing".equals(r.getState())).findFirst().orElse(null);
            ExamRecord latestFinished = records.stream().filter(r -> "finished".equals(r.getState())).findFirst().orElse(null);

            if (ongoing != null) {
                response.setStatus("ongoing");
            } else if (latestFinished != null) {
                response.setStatus("finished");
                response.setScore(latestFinished.getScore());
            } else if (LocalDateTime.now().isAfter(exam.getEndTime())) {
                response.setStatus("expired");
            } else {
                response.setStatus("not_started");
            }
            result.add(response);
        }
        return result;
    }

    public StartExamResponse startExam(Long examId) {
        Exam exam = requireExam(examId);
        Long studentId = UserContext.get().getUserId();

        if (!"published".equals(exam.getStatus())) {
            throw new BusinessException(400, "考试未发布");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new BusinessException(400, "不在考试时间范围内");
        }

        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId));

        boolean hasOngoing = records.stream().anyMatch(r -> "ongoing".equals(r.getState()));
        if (hasOngoing) {
            throw new BusinessException(400, "考试进行中，请勿重复开始");
        }
        boolean hasFinished = records.stream().anyMatch(r -> "finished".equals(r.getState()));
        if (hasFinished && !Boolean.TRUE.equals(exam.getAllowRetake())) {
            throw new BusinessException(400, "该考试不允许重考");
        }

        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentId(studentId);
        record.setAttemptNo(records.size() + 1);
        record.setState("ongoing");
        record.setAnswersJson("{}");
        record.setStartTime(now);
        examRecordMapper.insert(record);

        ExamBrief brief = new ExamBrief(exam.getId(), exam.getName(), exam.getDuration());
        StartExamResponse response = new StartExamResponse();
        response.setExam(brief);
        response.setQuestions(getStudentQuestionViews(examId));
        return response;
    }

    public ExamProgressResponse getExamProgress(Long examId) {
        Exam exam = requireExam(examId);
        Long studentId = UserContext.get().getUserId();

        ExamRecord ongoing = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getState, "ongoing"));

        ExamProgressResponse response = new ExamProgressResponse();
        if (ongoing == null) {
            response.setStarted(false);
            return response;
        }

        long elapsedSeconds = java.time.Duration.between(ongoing.getStartTime(), LocalDateTime.now()).getSeconds();
        long remaining = Math.max(0, exam.getDuration() * 60L - elapsedSeconds);

        response.setStarted(true);
        response.setExam(new ExamBrief(exam.getId(), exam.getName(), exam.getDuration()));
        response.setQuestions(getStudentQuestionViews(examId));
        response.setAnswers(parseAnswers(ongoing.getAnswersJson()));
        response.setRemainingSeconds(remaining);
        return response;
    }

    public void saveAnswer(Long examId, AnswerSubmission submission) {
        ExamRecord record = requireOngoingRecord(examId);
        record.setAnswersJson(writeJson(submission.getAnswers()));
        examRecordMapper.updateById(record);
    }

    public SubmitExamResponse submitExam(Long examId, AnswerSubmission submission) {
        Exam exam = requireExam(examId);
        ExamRecord record = requireOngoingRecord(examId);

        Map<String, Object> answers = submission.getAnswers() != null ? submission.getAnswers() : Map.of();

        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getOrderNum));

        int totalScore = 0;
        int obtainedScore = 0;

        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question == null) continue;

            Object userAnswer = answers.get(String.valueOf(question.getId()));
            Object correctAnswer = readJson(question.getAnswer());

            boolean correct;
            if (isSubjective(question.getType())) {
                correct = userAnswer != null && StringUtils.hasText(userAnswer.toString());
            } else {
                correct = answerEquals(question.getType(), userAnswer, correctAnswer);
            }
            int score = correct ? question.getScore() : 0;

            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setExamRecordId(record.getId());
            examAnswer.setQuestionId(question.getId());
            examAnswer.setUserAnswer(writeJson(userAnswer));
            examAnswer.setIsCorrect(isSubjective(question.getType()) ? null : correct);
            examAnswer.setScore(score);
            examAnswer.setFullScore(question.getScore());
            examAnswerMapper.insert(examAnswer);

            totalScore += question.getScore();
            obtainedScore += score;
        }

        BigDecimal accuracy = totalScore == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(obtainedScore).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalScore), 2, RoundingMode.HALF_UP);

        record.setState("finished");
        record.setScore(obtainedScore);
        record.setAccuracy(accuracy);
        record.setSubmitTime(LocalDateTime.now());
        record.setAnswersJson(writeJson(answers));
        examRecordMapper.updateById(record);

        return new SubmitExamResponse(obtainedScore, accuracy, record.getId());
    }

    public void retakeExam(Long examId) {
        Exam exam = requireExam(examId);
        if (!Boolean.TRUE.equals(exam.getAllowRetake())) {
            throw new BusinessException(400, "该考试不允许重考");
        }
        Long studentId = UserContext.get().getUserId();
        boolean hasOngoing = examRecordMapper.selectCount(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getState, "ongoing")) > 0;
        if (hasOngoing) {
            throw new BusinessException(400, "当前考试进行中，无法重考");
        }
    }

    // ================= 私有方法 =================

    private boolean isSubjective(String type) {
        return "fill".equals(type) || "essay".equals(type);
    }

    @SuppressWarnings("unchecked")
    private boolean answerEquals(String type, Object userAnswer, Object correctAnswer) {
        if (userAnswer == null || correctAnswer == null) return false;
        switch (type) {
            case "single":
                return String.valueOf(userAnswer).equals(String.valueOf(correctAnswer));
            case "judge":
                return String.valueOf(userAnswer).equalsIgnoreCase(String.valueOf(correctAnswer));
            case "multiple":
                List<Object> user = normalizeList(userAnswer);
                List<Object> correct = normalizeList(correctAnswer);
                Set<String> userSet = new TreeSet<>();
                user.forEach(o -> userSet.add(String.valueOf(o)));
                Set<String> correctSet = new TreeSet<>();
                correct.forEach(o -> correctSet.add(String.valueOf(o)));
                return userSet.equals(correctSet);
            default:
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> normalizeList(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return List.of(value);
    }

    private List<StudentQuestionView> getStudentQuestionViews(Long examId) {
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getOrderNum));

        List<StudentQuestionView> views = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (question == null) continue;
            StudentQuestionView view = new StudentQuestionView();
            view.setId(question.getId());
            view.setType(question.getType());
            view.setTitle(question.getTitle());
            view.setOptions(readJson(question.getOptions()));
            view.setScore(question.getScore());
            views.add(view);
        }
        return views;
    }

    private ExamRecord requireOngoingRecord(Long examId) {
        Long studentId = UserContext.get().getUserId();
        ExamRecord record = examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .eq(ExamRecord::getState, "ongoing"));
        if (record == null) {
            throw new BusinessException(400, "当前没有进行中的考试");
        }
        return record;
    }

    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        return exam;
    }

    private void applyRequest(Exam exam, ExamRequest request) {
        if (request.getName() != null) exam.setName(request.getName());
        if (request.getCourseId() != null) exam.setCourseId(request.getCourseId());
        if (request.getDuration() != null) exam.setDuration(request.getDuration());
        if (request.getStartTime() != null) exam.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) exam.setEndTime(request.getEndTime());
        if (request.getComposeType() != null) exam.setComposeType(request.getComposeType());
        if (request.getAllowRetake() != null) exam.setAllowRetake(request.getAllowRetake());
    }

    private void attachQuestions(Exam exam, ExamRequest request) {
        List<Long> questionIds = request.getQuestionIds();
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        int order = 1;
        int totalScore = 0;
        for (Long qid : questionIds) {
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(exam.getId());
            eq.setQuestionId(qid);
            eq.setOrderNum(order++);
            examQuestionMapper.insert(eq);

            Question question = questionMapper.selectById(qid);
            if (question != null) {
                totalScore += question.getScore();
            }
        }
        exam.setTotalScore(totalScore);
        examMapper.updateById(exam);
    }

    private ExamResponse toResponse(Exam exam) {
        ExamResponse response = new ExamResponse();
        response.setId(exam.getId());
        response.setName(exam.getName());
        response.setCourseId(exam.getCourseId());
        Course course = courseMapper.selectById(exam.getCourseId());
        response.setCourseName(course != null ? course.getName() : null);
        Long questionCount = examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, exam.getId()));
        response.setQuestionCount(questionCount.intValue());
        response.setTotalScore(exam.getTotalScore());
        response.setDuration(exam.getDuration());
        response.setStartTime(exam.getStartTime());
        response.setEndTime(exam.getEndTime());
        response.setStatus(exam.getStatus());
        response.setAllowRetake(exam.getAllowRetake());
        response.setComposeType(exam.getComposeType());
        return response;
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(400, "数据格式错误");
        }
    }

    private Object readJson(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnswers(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
