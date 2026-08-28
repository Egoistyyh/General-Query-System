package com.example.voicequery.model;

import java.util.List;

public record ColumnInfo(String name, String label, String dataType, List<String> operators) {
}
