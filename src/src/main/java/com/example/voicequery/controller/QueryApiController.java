package com.example.voicequery.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.voicequery.model.ColumnInfo;
import com.example.voicequery.model.QueryRequest;
import com.example.voicequery.model.VoiceParseRequest;
import com.example.voicequery.service.QueryBuilderService;
import com.example.voicequery.service.TableMetadataService;
import com.example.voicequery.service.VoiceTextParserService;

@RestController
@RequestMapping("/api")
public class QueryApiController {

    private final TableMetadataService tableMetadataService;
    private final QueryBuilderService queryBuilderService;
    private final VoiceTextParserService voiceTextParserService;

    public QueryApiController(TableMetadataService tableMetadataService,
                              QueryBuilderService queryBuilderService,
                              VoiceTextParserService voiceTextParserService) {
        this.tableMetadataService = tableMetadataService;
        this.queryBuilderService = queryBuilderService;
        this.voiceTextParserService = voiceTextParserService;
    }

    @GetMapping("/tables")
    public List<String> tables() {
        return tableMetadataService.listTables();
    }

    @GetMapping("/tables/{tableName}/columns")
    public List<ColumnInfo> columns(@PathVariable String tableName) {
        return tableMetadataService.listColumns(tableName);
    }

    @PostMapping("/parse-voice")
    public ResponseEntity<?> parseVoice(@RequestBody VoiceParseRequest request) {
        try {
            return ResponseEntity.ok(voiceTextParserService.parse(request.tableName(), request.text()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody QueryRequest request) {
        try {
            return ResponseEntity.ok(queryBuilderService.execute(request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
