package dream.maven.demoProject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import dream.maven.demoProject.common.BusinessException;
import dream.maven.demoProject.common.PageResult;
import dream.maven.demoProject.dto.question.BatchDeleteRequest;
import dream.maven.demoProject.dto.question.QuestionRequest;
import dream.maven.demoProject.dto.question.QuestionResponse;
import dream.maven.demoProject.entity.Question;
import dream.maven.demoProject.mapper.QuestionMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PageResult<QuestionResponse> getQuestionList(Long courseId, String type, String difficulty, int page, int size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(StringUtils.hasText(type), Question::getType, type)
                .eq(StringUtils.hasText(difficulty), Question::getDifficulty, difficulty)
                .orderByDesc(Question::getCreatedAt);

        Page<Question> result = questionMapper.selectPage(new Page<>(page, size), wrapper);
        List<QuestionResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResult<>(list, result.getTotal());
    }

    public QuestionResponse getQuestionById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "试题不存在");
        }
        return toResponse(question);
    }

    public QuestionResponse createQuestion(QuestionRequest request) {
        validateAnswerType(request.getType(), request.getAnswer());
        Question question = toEntity(request);
        questionMapper.insert(question);
        return toResponse(question);
    }

    public void updateQuestion(Long id, QuestionRequest request) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "试题不存在");
        }
        String type = request.getType() != null ? request.getType() : question.getType();
        Object answer = request.getAnswer() != null ? request.getAnswer() : readJson(question.getAnswer());
        validateAnswerType(type, answer);
        if (request.getType() != null) question.setType(request.getType());
        if (request.getTitle() != null) question.setTitle(request.getTitle());
        if (request.getOptions() != null) question.setOptions(writeJson(request.getOptions()));
        if (request.getAnswer() != null) question.setAnswer(writeJson(request.getAnswer()));
        if (request.getAnalysis() != null) question.setAnalysis(request.getAnalysis());
        if (request.getScore() != null) question.setScore(request.getScore());
        if (request.getDifficulty() != null) question.setDifficulty(request.getDifficulty());
        questionMapper.updateById(question);
    }

    private void validateAnswerType(String type, Object answer) {
        if (type == null || answer == null) return;
        boolean isCollection = answer instanceof java.util.Collection || answer.getClass().isArray();
        switch (type) {
            case "single", "judge" -> {
                if (isCollection) throw new BusinessException(400, type.equals("single") ? "单选题答案不能为多个选项" : "判断题答案不能为多个选项");
            }
            case "multiple" -> {
                if (!isCollection) throw new BusinessException(400, "多选题答案应为数组格式");
            }
        }
    }

    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    public void batchDeleteQuestions(BatchDeleteRequest request) {
        if (request.getIds() != null && !request.getIds().isEmpty()) {
            questionMapper.deleteBatchIds(request.getIds());
        }
    }

    public Map<String, Integer> importQuestions(Long courseId, MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    Question question = new Question();
                    question.setCourseId(courseId);
                    question.setType(cellString(row, 0));
                    question.setTitle(cellString(row, 1));
                    question.setOptions(cellString(row, 2));
                    question.setAnswer(cellString(row, 3));
                    question.setAnalysis(cellString(row, 4));
                    question.setScore((int) row.getCell(5).getNumericCellValue());
                    question.setDifficulty(cellString(row, 6));
                    questionMapper.insert(question);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                }
            }
        } catch (IOException e) {
            throw new BusinessException(400, "文件解析失败");
        }
        return Map.of("successCount", successCount, "failCount", failCount);
    }

    public byte[] exportQuestions(Long courseId, String type) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(courseId != null, Question::getCourseId, courseId)
                .eq(StringUtils.hasText(type), Question::getType, type);
        List<Question> questions = questionMapper.selectList(wrapper);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("题库");
            Row header = sheet.createRow(0);
            String[] headers = {"题型", "题干", "选项", "答案", "解析", "分值", "难度"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Question q : questions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(q.getType());
                row.createCell(1).setCellValue(q.getTitle());
                row.createCell(2).setCellValue(q.getOptions() == null ? "" : q.getOptions());
                row.createCell(3).setCellValue(q.getAnswer() == null ? "" : q.getAnswer());
                row.createCell(4).setCellValue(q.getAnalysis() == null ? "" : q.getAnalysis());
                row.createCell(5).setCellValue(q.getScore());
                row.createCell(6).setCellValue(q.getDifficulty());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "导出失败");
        }
    }

    private String cellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    private Question toEntity(QuestionRequest request) {
        Question question = new Question();
        question.setCourseId(request.getCourseId());
        question.setType(request.getType());
        question.setTitle(request.getTitle());
        question.setOptions(writeJson(request.getOptions()));
        question.setAnswer(writeJson(request.getAnswer()));
        question.setAnalysis(request.getAnalysis());
        question.setScore(request.getScore());
        question.setDifficulty(request.getDifficulty());
        return question;
    }

    private QuestionResponse toResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setCourseId(question.getCourseId());
        response.setType(question.getType());
        response.setTitle(question.getTitle());
        response.setOptions(readJson(question.getOptions()));
        response.setAnswer(readJson(question.getAnswer()));
        response.setAnalysis(question.getAnalysis());
        response.setScore(question.getScore());
        response.setDifficulty(question.getDifficulty());
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
}
