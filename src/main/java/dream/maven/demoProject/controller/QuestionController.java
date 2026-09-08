package dream.maven.demoProject.controller;

import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.dto.question.BatchDeleteRequest;
import dream.maven.demoProject.dto.question.QuestionRequest;
import dream.maven.demoProject.dto.question.QuestionResponse;
import dream.maven.demoProject.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping
    public Result<PageResult<QuestionResponse>> getQuestionList(
            @RequestParam Long courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(questionService.getQuestionList(courseId, type, difficulty, page, size));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> getQuestionTemplate() {
        byte[] content = questionService.buildTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"question_template.xlsx\"")
                .body(content);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportQuestions(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String type) {
        byte[] content = questionService.exportQuestions(courseId, type);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"questions.xlsx\"")
                .body(content);
    }

    @GetMapping("/{id}")
    public Result<QuestionResponse> getQuestionById(@PathVariable Long id) {
        return Result.success(questionService.getQuestionById(id));
    }

    @PostMapping
    public Result<QuestionResponse> createQuestion(@RequestBody QuestionRequest request) {
        return Result.success("创建成功", questionService.createQuestion(request));
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuestion(@PathVariable Long id, @RequestBody QuestionRequest request) {
        questionService.updateQuestion(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDeleteQuestions(@RequestBody BatchDeleteRequest request) {
        questionService.batchDeleteQuestions(request);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/import")
    public Result<Map<String, Integer>> importQuestions(
            @RequestParam Long courseId,
            @RequestParam("file") MultipartFile file) {
        return Result.success("导入成功", questionService.importQuestions(courseId, file));
    }
}
