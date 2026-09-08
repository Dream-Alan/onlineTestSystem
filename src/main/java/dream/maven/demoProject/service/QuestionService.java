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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

    /**
     * 批量导入。Excel 每行一题，第 1 行表头，第 2 行起为数据。
     * 支持「友好格式」与「旧机器格式」两种（某行选项单元格以 [ 开头时视为旧格式，选项/答案原文入库）。
     */
    public Map<String, Integer> importQuestions(Long courseId, MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowBlank(row)) continue;
                try {
                    questionMapper.insert(parseRow(courseId, row));
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                }
            }
        } catch (IOException e) {
            throw new BusinessException(400, "文件解析失败，请使用模板或导出的 xlsx 文件");
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
            writeHeader(sheet.createRow(0));
            int rowIdx = 1;
            for (Question q : questions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(typeName(q.getType()));
                row.createCell(1).setCellValue(q.getTitle() == null ? "" : q.getTitle());
                row.createCell(2).setCellValue(friendlyOptions(q));
                row.createCell(3).setCellValue(friendlyAnswer(q));
                row.createCell(4).setCellValue(q.getAnalysis() == null ? "" : q.getAnalysis());
                row.createCell(5).setCellValue(q.getScore() == null ? 0 : q.getScore());
                row.createCell(6).setCellValue(difficultyName(q.getDifficulty()));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "导出失败");
        }
    }

    /** 下载模板：仅表头，不含示例数据，避免误导入。 */
    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("题库导入模板");
            writeHeader(sheet.createRow(0));
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "模板生成失败");
        }
    }

    private void writeHeader(Row header) {
        String[] headers = {"题型", "题干", "选项", "答案", "解析", "分值", "难度"};
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    // ==================== 导入：行解析 ====================

    private Question parseRow(Long courseId, Row row) {
        String type = normalizeType(cellString(row, 0));
        String title = cellString(row, 1);
        if (type == null) throw new BusinessException(400, "题型无法识别（应为 单选/多选/判断/填空/简答 或枚举值）");
        if (!StringUtils.hasText(title)) throw new BusinessException(400, "题干为空");

        Question question = new Question();
        question.setCourseId(courseId);
        question.setType(type);
        question.setTitle(title.trim());
        question.setAnalysis(textOrNull(cellString(row, 4)));
        question.setScore(parseScore(row));
        question.setDifficulty(normalizeDifficulty(cellString(row, 6)));

        String optionsCell = cellString(row, 2);
        String answerCell = cellString(row, 3);
        if (optionsCell != null && optionsCell.trim().startsWith("[")) {
            // 旧机器格式：选项/答案单元格里已是 JSON 原文，直接入库
            question.setOptions(optionsCell.trim());
            question.setAnswer(answerCell == null ? null : answerCell.trim());
        } else {
            fillOptions(question, type, optionsCell);
            fillAnswer(question, type, answerCell);
        }
        return question;
    }

    private void fillOptions(Question question, String type, String optionsCell) {
        boolean needsOptions = "single".equals(type) || "multiple".equals(type) || "judge".equals(type);
        if (!needsOptions) return;
        List<String> tokens = splitOptions(optionsCell);
        if ("judge".equals(type)) {
            // 判断题选项可留空，展示端自动生成「对/错」
            question.setOptions(null);
            return;
        }
        if (tokens.isEmpty()) throw new BusinessException(400, "缺少选项");
        if ("single".equals(type) && tokens.size() < 2) throw new BusinessException(400, "单选题至少需要 2 个选项");
        List<Map<String, Object>> options = new ArrayList<>();
        for (String token : tokens) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("content", token);
            options.add(m);
        }
        question.setOptions(writeJson(options));
    }

    private void fillAnswer(Question question, String type, String answerCell) {
        String raw = answerCell == null ? "" : answerCell.trim();
        switch (type) {
            case "single" -> {
                Integer idx = singleAnswerIndex(raw);
                if (idx == null) throw new BusinessException(400, "单选题答案格式错误（应为 A、B 或数字下标）");
                question.setAnswer(String.valueOf(idx));
            }
            case "multiple" -> {
                List<Integer> indexes = multiAnswerIndexes(raw);
                if (indexes.isEmpty()) throw new BusinessException(400, "多选题答案格式错误（如 ACD 或 A、C、D）");
                question.setAnswer(writeJson(indexes));
            }
            case "judge" -> {
                Boolean b = judgeAnswer(raw);
                if (b == null) throw new BusinessException(400, "判断题答案格式错误（应为 对/错/√/× 等）");
                question.setAnswer(b ? "true" : "false");
            }
            default -> { // fill / essay
                if (!StringUtils.hasText(raw)) throw new BusinessException(400, "参考答案为空");
                question.setAnswer(writeJson(raw));
            }
        }
    }

    /** 选项分隔：每行一个或 | 分隔，可带 A. / B、 / C) 前缀，自动剥离。 */
    private List<String> splitOptions(String optionsCell) {
        List<String> result = new ArrayList<>();
        if (optionsCell == null) return result;
        String unified = optionsCell.replace('|', '\n');
        for (String line : unified.split("\n")) {
            String token = stripOptionPrefix(line.trim());
            if (!token.isEmpty()) result.add(token);
        }
        return result;
    }

    private String stripOptionPrefix(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            if ((first >= 'A' && first <= 'H') || (first >= 'a' && first <= 'h')) {
                char sep = s.charAt(1);
                if (sep == '.' || sep == '、' || sep == '．' || sep == ')' || sep == '）' || sep == ':'
                        || sep == '：' || sep == '-' || sep == ' ' || sep == '　') {
                    return s.substring(2).trim();
                }
            }
        }
        return s;
    }

    private Integer singleAnswerIndex(String raw) {
        if (raw.isEmpty()) return null;
        if (raw.length() == 1) {
            char c = raw.toUpperCase().charAt(0);
            if (c >= 'A' && c <= 'Z') return c - 'A';
        }
        try {
            int v = Integer.parseInt(raw);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Integer> multiAnswerIndexes(String raw) {
        TreeSet<Integer> set = new TreeSet<>();
        String upper = raw.toUpperCase();
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                set.add(c - 'A');
            } else if (c >= '0' && c <= '9') {
                set.add(c - '0');
            } else if (c == ',' || c == '，' || c == '、' || c == ';' || c == '；' || c == ' ' || c == '[' || c == ']') {
                // 分隔符，忽略
            } else {
                throw new BusinessException(400, "多选题答案含无法识别的字符：" + c);
            }
        }
        return new ArrayList<>(set);
    }

    private Boolean judgeAnswer(String raw) {
        if (raw.isEmpty()) return null;
        String s = raw.toLowerCase();
        if (s.equals("对") || s.equals("正确") || s.equals("√") || s.equals("是")
                || s.equals("t") || s.equals("true") || s.equals("y") || s.equals("yes") || s.equals("1")) {
            return Boolean.TRUE;
        }
        if (s.equals("错") || s.equals("错误") || s.equals("×") || s.equals("否")
                || s.equals("f") || s.equals("false") || s.equals("n") || s.equals("no") || s.equals("0")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private int parseScore(Row row) {
        Cell cell = row.getCell(5);
        if (cell == null) return 5;
        String value;
        try {
            value = new DataFormatter().formatCellValue(cell).trim();
        } catch (Exception e) {
            throw new BusinessException(400, "分值格式错误");
        }
        if (value.isEmpty()) return 5;
        try {
            int score = (int) Math.round(Double.parseDouble(value));
            return score > 0 ? score : 5;
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "分值格式错误：" + value);
        }
    }

    private String normalizeType(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String t = raw.trim().toLowerCase();
        switch (t) {
            case "single", "单选", "单选题": return "single";
            case "multiple", "多选", "多选题": return "multiple";
            case "judge", "判断", "判断题": return "judge";
            case "fill", "填空", "填空题": return "fill";
            case "essay", "简答", "简答题", "问答": return "essay";
            default: return null;
        }
    }

    private String normalizeDifficulty(String raw) {
        if (!StringUtils.hasText(raw)) return "medium";
        String d = raw.trim().toLowerCase();
        switch (d) {
            case "easy", "简单", "易": return "easy";
            case "medium", "中等", "中": return "medium";
            case "hard", "困难", "难": return "hard";
            default: throw new BusinessException(400, "难度无法识别（应为 简单/中等/困难）");
        }
    }

    private boolean isRowBlank(Row row) {
        for (int i = 0; i < 7; i++) {
            String v = cellString(row, i);
            if (StringUtils.hasText(v)) return false;
        }
        return true;
    }

    private String textOrNull(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }

    // ==================== 导出：库 JSON → 友好文本 ====================

    private String friendlyOptions(Question q) {
        Object parsed = readJson(q.getOptions());
        if (!(parsed instanceof List<?> list) || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String content = "";
            Object item = list.get(i);
            if (item instanceof Map<?, ?> map && map.get("content") != null) {
                content = map.get("content").toString();
            } else if (item != null) {
                content = item.toString();
            }
            if (i > 0) sb.append('\n');
            sb.append((char) ('A' + i)).append('.').append(content);
        }
        return sb.toString();
    }

    private String friendlyAnswer(Question q) {
        Object answer = readJson(q.getAnswer());
        if (answer == null) return "";
        String type = q.getType();
        switch (type) {
            case "single": {
                int idx = answer instanceof Number n ? n.intValue() : -1;
                if (idx >= 0 && idx < 26) return String.valueOf((char) ('A' + idx));
                return answer.toString();
            }
            case "multiple": {
                if (answer instanceof List<?> list) {
                    TreeSet<Integer> sorted = new TreeSet<>();
                    for (Object o : list) {
                        if (o instanceof Number n) sorted.add(n.intValue());
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Integer i : sorted) sb.append((char) ('A' + i));
                    return sb.toString();
                }
                return answer.toString();
            }
            case "judge": {
                boolean b = answer instanceof Boolean b1 ? b1 : "true".equalsIgnoreCase(answer.toString());
                return b ? "对" : "错";
            }
            default: // fill / essay
                return answer.toString();
        }
    }

    private String typeName(String type) {
        switch (type) {
            case "single": return "单选";
            case "multiple": return "多选";
            case "judge": return "判断";
            case "fill": return "填空";
            case "essay": return "简答";
            default: return type == null ? "" : type;
        }
    }

    private String difficultyName(String difficulty) {
        switch (difficulty) {
            case "easy": return "简单";
            case "medium": return "中等";
            case "hard": return "困难";
            default: return difficulty == null ? "" : difficulty;
        }
    }

    // ==================== 通用工具 ====================

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
