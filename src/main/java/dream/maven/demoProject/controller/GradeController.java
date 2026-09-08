package dream.maven.demoProject.controller;

import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.dto.grade.*;
import dream.maven.demoProject.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @GetMapping("/grades")
    public Result<PageResult<GradeResponse>> getGradeList(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String studentName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(gradeService.getGradeList(courseId, studentName, page, size));
    }

    @GetMapping("/courses/{courseId}/students/{studentId}/grades")
    public Result<List<GradeHistoryResponse>> getStudentGradeHistory(
            @PathVariable Long courseId, @PathVariable Long studentId) {
        return Result.success(gradeService.getStudentGradeHistory(courseId, studentId));
    }

    @GetMapping("/grades/my")
    public Result<PageResult<MyGradeResponse>> getMyGrades(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(gradeService.getMyGrades(page, size));
    }

    @GetMapping("/grades/{examRecordId}")
    public Result<GradeDetailResponse> getGradeDetail(@PathVariable Long examRecordId) {
        return Result.success(gradeService.getGradeDetail(examRecordId));
    }
}
