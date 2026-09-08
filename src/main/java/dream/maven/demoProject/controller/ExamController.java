package dream.maven.demoProject.controller;

import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.dto.exam.*;
import dream.maven.demoProject.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/exams")
    public Result<PageResult<ExamResponse>> getExamList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(examService.getExamList(courseId, status, page, size));
    }

    @GetMapping("/exams/{id}")
    public Result<ExamDetailResponse> getExamById(@PathVariable Long id) {
        return Result.success(examService.getExamById(id));
    }

    @PostMapping("/exams")
    public Result<ExamResponse> createExam(@RequestBody ExamRequest request) {
        return Result.success("创建成功", examService.createExam(request));
    }

    @PutMapping("/exams/{id}")
    public Result<Void> updateExam(@PathVariable Long id, @RequestBody ExamRequest request) {
        examService.updateExam(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/exams/{id}")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/exams/{id}/publish")
    public Result<Void> publishExam(@PathVariable Long id) {
        examService.publishExam(id);
        return Result.success("发布成功", null);
    }

    @GetMapping("/courses/{courseId}/exams")
    public Result<List<StudentExamResponse>> getStudentExamsByCourse(@PathVariable Long courseId) {
        return Result.success(examService.getStudentExamsByCourse(courseId));
    }

    @PostMapping("/exams/{examId}/start")
    public Result<StartExamResponse> startExam(@PathVariable Long examId) {
        return Result.success("开始考试", examService.startExam(examId));
    }

    @GetMapping("/exams/{examId}/progress")
    public Result<ExamProgressResponse> getExamProgress(@PathVariable Long examId) {
        return Result.success(examService.getExamProgress(examId));
    }

    @PutMapping("/exams/{examId}/answer")
    public Result<Void> saveAnswer(@PathVariable Long examId, @RequestBody AnswerSubmission submission) {
        examService.saveAnswer(examId, submission);
        return Result.success("保存成功", null);
    }

    @PostMapping("/exams/{examId}/submit")
    public Result<SubmitExamResponse> submitExam(@PathVariable Long examId, @RequestBody AnswerSubmission submission) {
        return Result.success("提交成功", examService.submitExam(examId, submission));
    }

    @PostMapping("/exams/{examId}/retake")
    public Result<Void> retakeExam(@PathVariable Long examId) {
        examService.retakeExam(examId);
        return Result.success("重考已开启，请重新开始", null);
    }
}
