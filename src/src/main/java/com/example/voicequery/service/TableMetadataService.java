package com.example.voicequery.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.voicequery.model.ColumnInfo;

@Service
public class TableMetadataService {

    private final JdbcTemplate jdbcTemplate;

    public TableMetadataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> listTables() {
        return jdbcTemplate.queryForList("""
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'
                        ORDER BY TABLE_NAME
                        """, String.class)
                .stream()
                .filter(table -> !table.startsWith("flyway_"))
                .toList();
    }

    public List<ColumnInfo> listColumns(String tableName) {
        String safeTable = requireExistingTable(tableName);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME, DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """, safeTable);

        List<ColumnInfo> columns = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String columnName = row.get("COLUMN_NAME").toString();
            String dataType = row.get("DATA_TYPE").toString();
            columns.add(new ColumnInfo(columnName, toLabel(columnName), dataType, operatorsFor(dataType)));
        }
        return columns;
    }

    public String requireExistingTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        return listTables().stream()
                .filter(table -> table.equalsIgnoreCase(tableName.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("表不存在：" + tableName));
    }

    public ColumnInfo requireExistingColumn(String tableName, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("列名不能为空");
        }
        return listColumns(tableName).stream()
                .filter(column -> column.name().equalsIgnoreCase(columnName.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("列不存在：" + columnName));
    }

    public Optional<ColumnInfo> findColumnByKeyword(String tableName, String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return listColumns(tableName).stream()
                .filter(column -> column.name().equalsIgnoreCase(normalizedKeyword)
                        || column.label().equalsIgnoreCase(keyword)
                        || aliases(column.name()).stream().anyMatch(alias -> normalizedKeyword.contains(alias)))
                .findFirst();
    }

    public String toLabel(String columnName) {
        return switch (columnName) {
            case "id" -> "编号";
            case "student_no" -> "学号";
            case "name" -> "姓名";
            case "gender" -> "性别";
            case "age" -> "年龄";
            case "major" -> "专业";
            case "class_name" -> "班级";
            case "phone" -> "电话";
            case "email" -> "邮箱";
            case "city" -> "城市";
            case "score" -> "成绩";
            case "enrollment_date" -> "入学日期";
            default -> columnName;
        };
    }

    public List<String> operatorsFor(String dataType) {
        String type = dataType.toUpperCase(Locale.ROOT);
        if (type.contains("INT") || type.contains("DECIMAL") || type.contains("NUMERIC")
                || type.contains("DOUBLE") || type.contains("REAL") || type.contains("DATE")) {
            return List.of("<", ">", ">=", "<=", "=", "Is", "Is Not");
        }
        return List.of("=", "Like", "Not Like", "Is", "Is Not");
    }

    private List<String> aliases(String columnName) {
        return switch (columnName) {
            case "student_no" -> List.of("学号", "学生编号", "studentno", "student_no");
            case "name" -> List.of("姓名", "名字", "学生姓名", "name");
            case "gender" -> List.of("性别", "男生", "女生", "男女", "gender");
            case "age" -> List.of("年龄", "岁数", "age");
            case "major" -> List.of("专业", "院系", "major");
            case "class_name" -> List.of("班级", "班", "class");
            case "phone" -> List.of("电话", "手机", "联系方式", "phone");
            case "email" -> List.of("邮箱", "邮件", "email");
            case "city" -> List.of("城市", "地区", "地址", "city");
            case "score" -> List.of("成绩", "分数", "score");
            case "enrollment_date" -> List.of("入学日期", "入学时间", "enrollment");
            default -> List.of(columnName);
        };
    }
}
