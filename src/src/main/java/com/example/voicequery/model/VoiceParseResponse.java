package com.example.voicequery.model;

import java.util.List;

public record VoiceParseResponse(String normalizedText, List<QueryCondition> conditions, String wherePreview) {
}
