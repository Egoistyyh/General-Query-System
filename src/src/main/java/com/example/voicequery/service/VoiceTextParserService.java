package com.example.voicequery.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.voicequery.model.ColumnInfo;
import com.example.voicequery.model.QueryCondition;
import com.example.voicequery.model.VoiceParseResponse;

@Service
public class VoiceTextParserService {

    private final TableMetadataService tableMetadataService;
    private final QueryBuilderService queryBuilderService;

    public VoiceTextParserService(TableMetadataService tableMetadataService, QueryBuilderService queryBuilderService) {
        this.tableMetadataService = tableMetadataService;
        this.queryBuilderService = queryBuilderService;
    }

    public VoiceParseResponse parse(String tableName, String text) {
        String normalized = normalize(text);
        List<QueryCondition> conditions = new ArrayList<>();

        detectGender(tableName, normalized).forEach(conditions::add);
        detectNumber(tableName, normalized, "age", List.of("年龄", "岁数", "岁")).forEach(conditions::add);
        detectNumber(tableName, normalized, "score", List.of("成绩", "分数", "得分")).forEach(conditions::add);
        detectText(tableName, normalized, "name", List.of("姓名", "名字", "叫")).forEach(conditions::add);
        detectText(tableName, normalized, "major", List.of("专业")).forEach(conditions::add);
        detectText(tableName, normalized, "class_name", List.of("班级", "班")).forEach(conditions::add);
        detectText(tableName, normalized, "city", List.of("城市", "地区", "来自", "地址")).forEach(conditions::add);

        List<QueryCondition> withConnectors = applyConnectors(normalized, conditions);
        String wherePreview = queryBuilderService.previewWhere(tableName, withConnectors);
        return new VoiceParseResponse(normalized, withConnectors, wherePreview);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace("，", ",")
                .replace("。", ",")
                .replace("、", ",")
                .replace("并且", " and ")
                .replace("而且", " and ")
                .replace("以及", " and ")
                .replace("或者", " or ")
                .replace("或", " or ")
                .replace("等于", "=")
                .replace("为", "=")
                .replace("是", "=")
                .replace("小于等于", "<=")
                .replace("不大于", "<=")
                .replace("大于等于", ">=")
                .replace("不小于", ">=")
                .replace("小于", "<")
                .replace("低于", "<")
                .replace("大于", ">")
                .replace("高于", ">")
                .replace("超过", ">")
                .replace("包含", " like ");
    }

    private List<QueryCondition> detectGender(String tableName, String text) {
        if (tableMetadataService.findColumnByKeyword(tableName, "性别").isEmpty()) {
            return List.of();
        }
        if (text.contains("男")) {
            return List.of(new QueryCondition("gender", "=", "男", "AND"));
        }
        if (text.contains("女")) {
            return List.of(new QueryCondition("gender", "=", "女", "AND"));
        }
        return List.of();
    }

    private List<QueryCondition> detectNumber(String tableName, String text, String columnName, List<String> keywords) {
        if (tableMetadataService.findColumnByKeyword(tableName, columnName).isEmpty()) {
            return List.of();
        }
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile(keyword + "\\s*(<=|>=|<|>|=)\\s*([0-9]+(?:\\.[0-9]+)?)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return List.of(new QueryCondition(columnName, matcher.group(1), matcher.group(2), "AND"));
            }

            Pattern beforePattern = Pattern.compile("(<=|>=|<|>|=)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*" + keyword);
            Matcher beforeMatcher = beforePattern.matcher(text);
            if (beforeMatcher.find()) {
                return List.of(new QueryCondition(columnName, beforeMatcher.group(1), beforeMatcher.group(2), "AND"));
            }
        }
        return List.of();
    }

    private List<QueryCondition> detectText(String tableName, String text, String columnName, List<String> keywords) {
        if (tableMetadataService.findColumnByKeyword(tableName, columnName).isEmpty()) {
            return List.of();
        }
        for (String keyword : keywords) {
            Pattern equalsPattern = Pattern.compile(keyword + "\\s*=\\s*([\\u4e00-\\u9fa5A-Za-z0-9_\\-]+)");
            Matcher equalsMatcher = equalsPattern.matcher(text);
            if (equalsMatcher.find()) {
                return List.of(new QueryCondition(columnName, "=", cleanupValue(equalsMatcher.group(1)), "AND"));
            }

            Pattern likePattern = Pattern.compile(keyword + "\\s+like\\s+([\\u4e00-\\u9fa5A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
            Matcher likeMatcher = likePattern.matcher(text);
            if (likeMatcher.find()) {
                return List.of(new QueryCondition(columnName, "Like", cleanupValue(likeMatcher.group(1)), "AND"));
            }
        }
        return List.of();
    }

    private List<QueryCondition> applyConnectors(String text, List<QueryCondition> conditions) {
        if (conditions.size() < 2) {
            return conditions;
        }
        String connector = text.toLowerCase(Locale.ROOT).contains(" or ") ? "OR" : "AND";
        List<QueryCondition> result = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            QueryCondition condition = conditions.get(i);
            result.add(new QueryCondition(condition.column(), condition.operator(), condition.value(), i == 0 ? "AND" : connector));
        }
        return result;
    }

    private String cleanupValue(String value) {
        return value.replaceAll("(的|学生|信息|所有)$", "");
    }
}
