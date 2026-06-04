package com.datagov.controller;

import com.datagov.model.*;
import com.datagov.service.DataElementParserService;
import com.datagov.service.RuleGeneratorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataElementController {

    private final DataElementParserService parserService;
    private final RuleGeneratorService ruleGeneratorService;

    public DataElementController(DataElementParserService parserService,
                                  RuleGeneratorService ruleGeneratorService) {
        this.parserService = parserService;
        this.ruleGeneratorService = ruleGeneratorService;
    }

    /**
     * 解析自然语言输入为数据元
     */
    @PostMapping("/parse")
    public Map<String, Object> parse(@RequestBody ParseRequest request) {
        List<DataElement> elements = parserService.parse(request.getInput());
        return Map.of(
            "success", true,
            "data", elements,
            "count", elements.size()
        );
    }

    /**
     * 根据数据元生成治理规则
     */
    @PostMapping("/generate-rules")
    public Map<String, Object> generateRules(@RequestBody RuleRequest request) {
        List<GovernanceRule> rules = ruleGeneratorService.generate(request.getDataElements());
        return Map.of(
            "success", true,
            "data", rules,
            "count", rules.size()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "service", "数据元解析服务");
    }
}
