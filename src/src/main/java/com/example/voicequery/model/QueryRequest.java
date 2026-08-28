package com.example.voicequery.model;

import java.util.List;

public record QueryRequest(String tableName, List<QueryCondition> conditions) {
}
