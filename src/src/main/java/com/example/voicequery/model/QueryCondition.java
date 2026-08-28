package com.example.voicequery.model;

public record QueryCondition(String column, String operator, String value, String connector) {
}
