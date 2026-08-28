package com.example.voicequery.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.voicequery.model.ColumnInfo;
import com.example.voicequery.model.QueryCondition;
import com.example.voicequery.model.QueryRequest;
import com.example.voicequery.model.QueryResponse;

@Service
public class QueryBuilderService {

    private final JdbcTemplate jdbcTemplate;
    private final TableMetadataService tableMetadataService;

    public QueryBuilderService(JdbcTemplate jdbcTemplate, TableMetadataService tableMetadataService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableMetadataService = tableMetadataService;
    }

    public QueryResponse execute(QueryRequest request) {
        BuiltQuery builtQuery = build(request);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(builtQuery.sql(), builtQuery.params().toArray());
        List<String> headers = tableMetadataService.listColumns(request.tableName()).stream()
                .map(ColumnInfo::name)
                .toList();
        return new QueryResponse(builtQuery.sql(), builtQuery.wherePreview(), headers, rows);
    }

    public String previewWhere(String tableName, List<QueryCondition> conditions) {
        return build(new QueryRequest(tableName, conditions)).wherePreview();
    }

    private BuiltQuery build(QueryRequest request) {
        String tableName = tableMetadataService.requireExistingTable(request.tableName());
        List<QueryCondition> conditions = request.conditions() == null ? List.of() : request.conditions();
        StringBuilder whereSql = new StringBuilder();
        StringJoiner preview = new StringJoiner(" ");
        List<Object> params = new ArrayList<>();

        int validIndex = 0;
        for (QueryCondition condition : conditions) {
            if (condition == null || condition.column() == null || condition.column().isBlank()
                    || condition.operator() == null || condition.operator().isBlank()) {
                continue;
            }

            ColumnInfo column = tableMetadataService.requireExistingColumn(tableName, condition.column());
            String connector = normalizeConnector(condition.connector(), validIndex);
            String operator = normalizeOperator(condition.operator());
            String sqlOperator = toSqlOperator(operator);
            Object param = convertValue(condition.value(), column, operator);

            if (validIndex > 0) {
                whereSql.append(" ").append(connector).append(" ");
                preview.add(connector);
            }
            whereSql.append(column.name()).append(" ").append(sqlOperator);
            if ("Is".equals(operator) || "Is Not".equals(operator)) {
                whereSql.append(" NULL");
            } else {
                whereSql.append(" ?");
                params.add(param);
            }
            preview.add(column.label() + " " + displayOperator(operator) + " " + displayValue(param, operator));
            validIndex++;
        }

        String sql = "SELECT * FROM " + tableName;
        if (whereSql.length() > 0) {
            sql += " WHERE " + whereSql;
        }
        sql += " ORDER BY id";
        return new BuiltQuery(sql, whereSql.length() == 0 ? "无查询条件" : preview.toString(), params);
    }

    private String normalizeConnector(String connector, int validIndex) {
        if (validIndex == 0) {
            return "";
        }
        if (connector == null) {
            return "AND";
        }
        String normalized = connector.trim().toUpperCase(Locale.ROOT);
        if ("OR".equals(normalized) || "或者".equals(connector) || "或".equals(connector)) {
            return "OR";
        }
        return "AND";
    }

    private String normalizeOperator(String operator) {
        return switch (operator.trim().toLowerCase(Locale.ROOT)) {
            case "小于", "低于", "少于", "<" -> "<";
            case "大于", "高于", "超过", ">" -> ">";
            case "不小于", "大于等于", ">=" -> ">=";
            case "不大于", "小于等于", "<=" -> "<=";
            case "包含", "模糊", "like" -> "Like";
            case "不包含", "not like" -> "Not Like";
            case "为空", "is" -> "Is";
            case "不为空", "is not" -> "Is Not";
            default -> "=";
        };
    }

    private String toSqlOperator(String operator) {
        return switch (operator) {
            case "Like" -> "LIKE";
            case "Not Like" -> "NOT LIKE";
            case "Is" -> "IS";
            case "Is Not" -> "IS NOT";
            default -> operator;
        };
    }

    private Object convertValue(String rawValue, ColumnInfo column, String operator) {
        String value = rawValue == null ? "" : rawValue.trim();
        if ("Is".equals(operator) || "Is Not".equals(operator)) {
            return value.isBlank() || "空".equals(value) || "null".equalsIgnoreCase(value) ? null : value;
        }
        if ("Like".equals(operator) || "Not Like".equals(operator)) {
            return "%" + value + "%";
        }

        String dataType = column.dataType().toUpperCase(Locale.ROOT);
        if (dataType.contains("INT")) {
            return Integer.parseInt(onlyNumeric(value));
        }
        if (dataType.contains("DECIMAL") || dataType.contains("NUMERIC")
                || dataType.contains("DOUBLE") || dataType.contains("REAL")) {
            return Double.parseDouble(onlyNumeric(value));
        }
        if (dataType.contains("DATE")) {
            return Date.valueOf(value.replace("年", "-").replace("月", "-").replace("日", ""));
        }
        return value;
    }

    private String onlyNumeric(String value) {
        String cleaned = value.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("数值不能为空");
        }
        return cleaned;
    }

    private String displayOperator(String operator) {
        return switch (operator) {
            case "Like" -> "LIKE";
            case "Not Like" -> "NOT LIKE";
            case "Is" -> "IS";
            case "Is Not" -> "IS NOT";
            default -> operator;
        };
    }

    private String displayValue(Object value, String operator) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        String text = value.toString();
        if ("Like".equals(operator) || "Not Like".equals(operator)) {
            text = text.replace("%", "");
        }
        return "'" + text + "'";
    }

    private record BuiltQuery(String sql, String wherePreview, List<Object> params) {
    }
}
