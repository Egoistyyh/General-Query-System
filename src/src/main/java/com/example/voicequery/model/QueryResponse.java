package com.example.voicequery.model;

import java.util.List;
import java.util.Map;

public record QueryResponse(String sql, String whereClause, List<String> headers, List<Map<String, Object>> rows) {
}
