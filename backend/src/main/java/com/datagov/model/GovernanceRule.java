package com.datagov.model;

/**
 * 治理规则 - 基于数据元生成的SQL治理规则
 */
public class GovernanceRule {
    private String ruleId;         // 规则ID
    private String ruleName;       // 规则名称
    private String ruleType;       // 规则类型（长度、格式、范围、非空等）
    private String description;    // 规则描述
    private String sqlExpression;  // SQL表达式（OceanBase兼容）
    private String severity;       // 严重程度（ERROR、WARNING、INFO）
    private String dataElementName; // 关联数据元名称

    public GovernanceRule() {}

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSqlExpression() { return sqlExpression; }
    public void setSqlExpression(String sqlExpression) { this.sqlExpression = sqlExpression; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDataElementName() { return dataElementName; }
    public void setDataElementName(String dataElementName) { this.dataElementName = dataElementName; }
}
