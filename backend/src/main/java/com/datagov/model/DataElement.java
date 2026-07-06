package com.datagov.model;

/**
 * 数据元 - 符合 GB/T 19488.1-2004 电子政务数据元标准
 * 包含标识类、定义类、表示类、关系类、管理类五大属性组
 */
public class DataElement {

    // ========== 标识类属性 ==========
    private String identifier;      // 标识符 (如 DE0010001)
    private String internalId;      // 内部标识符
    private String name;            // 中文名称 (兼容字段，对应 cnName)
    private String cnName;          // 中文名称
    private String enName;          // 英文名称
    private String synonym;         // 同义名称
    private String keywords;        // 关键词

    // ========== 定义类属性 ==========
    private String definition;      // 定义
    private String description;     // 描述

    // ========== 表示类属性 ==========
    private String representation;  // 表示类（代码/文本/数字/日期/布尔/二进制）
    private String dataType;        // 数据类型（字符型/数值型/日期型/布尔型/二进制）
    private String dataTypeCode;    // 数据类型代码（C/N/D/T/B）
    private String format;          // 数据格式（如 AN..100, N..10, YYYYMMDD）
    private int length;             // 数据长度
    private int precision;          // 精度（小数位数）
    private String valueDomain;     // 值域
    private String valueDomainMeaning; // 值域含义
    private String unit;            // 计量单位

    // ========== 关系类属性 ==========
    private String classification;  // 分类方案（人员/机构/地理/时间/证件/经济）
    private String relatedDE;       // 相关数据元

    // ========== 管理类属性 ==========
    private String regAuthority;    // 注册机构
    private String regStatus;       // 注册状态（现行有效/编制中/审核中/废止）
    private String submitOrg;       // 提交机构
    private String version;         // 版本
    private String remarks;         // 备注
    private String regDate;         // 注册日期

    // ========== 兼容字段（用于规则生成） ==========
    private String constraint;      // 约束条件（如"必填"）

    public DataElement() {}

    /**
     * 兼容构造器 - 保持旧接口可用
     */
    public DataElement(String name, String identifier, String definition,
                       String dataType, int length, int precision,
                       String valueRange, String unit, String constraint, String remark) {
        this.name = name;
        this.cnName = name;
        this.identifier = identifier;
        this.definition = definition;
        this.dataType = dataType;
        this.dataTypeCode = convertToCode(dataType);
        this.length = length;
        this.precision = precision;
        this.valueDomain = valueRange;
        this.unit = unit;
        this.constraint = constraint;
        this.remarks = remark;
        this.version = "1.0";
        this.regStatus = "现行有效";
    }

    /**
     * 将中文数据类型转换为标准代码
     * C-字符型 N-数值型 D-日期型 T-时间型 B-布尔型
     */
    private String convertToCode(String dataType) {
        if (dataType == null) return "C";
        return switch (dataType) {
            case "字符型", "字符串" -> "C";
            case "数值型", "整数", "浮点数" -> "N";
            case "日期型", "日期" -> "D";
            case "时间", "时间戳" -> "T";
            case "布尔型", "布尔" -> "B";
            default -> "C";
        };
    }

    // ========== Getters & Setters ==========

    // 标识类
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getInternalId() { return internalId; }
    public void setInternalId(String internalId) { this.internalId = internalId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        if (this.cnName == null) this.cnName = name;
    }

    public String getCnName() { return cnName; }
    public void setCnName(String cnName) {
        this.cnName = cnName;
        if (this.name == null) this.name = cnName;
    }

    public String getEnName() { return enName; }
    public void setEnName(String enName) { this.enName = enName; }

    public String getSynonym() { return synonym; }
    public void setSynonym(String synonym) { this.synonym = synonym; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    // 定义类
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // 表示类
    public String getRepresentation() { return representation; }
    public void setRepresentation(String representation) { this.representation = representation; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) {
        this.dataType = dataType;
        this.dataTypeCode = convertToCode(dataType);
    }

    public String getDataTypeCode() { return dataTypeCode; }
    public void setDataTypeCode(String dataTypeCode) { this.dataTypeCode = dataTypeCode; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = precision; }

    public String getValueDomain() { return valueDomain; }
    public void setValueDomain(String valueDomain) { this.valueDomain = valueDomain; }

    public String getValueDomainMeaning() { return valueDomainMeaning; }
    public void setValueDomainMeaning(String valueDomainMeaning) { this.valueDomainMeaning = valueDomainMeaning; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    // 关系类
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getRelatedDE() { return relatedDE; }
    public void setRelatedDE(String relatedDE) { this.relatedDE = relatedDE; }

    // 管理类
    public String getRegAuthority() { return regAuthority; }
    public void setRegAuthority(String regAuthority) { this.regAuthority = regAuthority; }

    public String getRegStatus() { return regStatus; }
    public void setRegStatus(String regStatus) { this.regStatus = regStatus; }

    public String getSubmitOrg() { return submitOrg; }
    public void setSubmitOrg(String submitOrg) { this.submitOrg = submitOrg; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getRegDate() { return regDate; }
    public void setRegDate(String regDate) { this.regDate = regDate; }

    // 兼容字段
    public String getConstraint() { return constraint; }
    public void setConstraint(String constraint) { this.constraint = constraint; }

    /**
     * 兼容方法 - getValueRange 代理到 valueDomain
     */
    public String getValueRange() { return valueDomain; }
    public void setValueRange(String valueRange) { this.valueDomain = valueRange; }

    /**
     * 兼容方法 - getRemark 代理到 remarks
     */
    public String getRemark() { return remarks; }
    public void setRemark(String remark) { this.remarks = remark; }
}
