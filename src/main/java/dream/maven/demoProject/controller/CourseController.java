package dream.maven.demoProject.controller;

import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.dto.course.CourseRequest;
import dream.maven.demoProject.dto.course.CourseResponse;
import dream.maven.demoProject.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping
    public Result<PageResult<CourseResponse>> getCourseList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(courseService.getCourseList(keyword, page, size));
    }

    @GetMapping("/my")
    public Result<List<CourseResponse>> getMyCourses() {
        return Result.success(courseService.getMyCourses());
    }

    @PostMapping
    public Result<CourseResponse> createCourse(@RequestBody CourseRequest request) {
        return Result.success("创建成功", courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    public Result<Void> updateCourse(@PathVariable Long id, @RequestBody CourseRequest request) {
        courseService.updateCourse(id, request);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/{courseId}/enroll")
    public Result<Void> enrollCourse(@PathVariable Long courseId) {
        courseService.enrollCourse(courseId);
        return Result.success("选课成功", null);
    }
}
