package com.example.voicequery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.voicequery.service.TableMetadataService;

@Controller
public class HomeController {

    private final TableMetadataService tableMetadataService;

    public HomeController(TableMetadataService tableMetadataService) {
        this.tableMetadataService = tableMetadataService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tables", tableMetadataService.listTables());
        return "index";
    }
}
