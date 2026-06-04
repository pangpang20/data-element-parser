package com.datagov.model;

/**
 * 数据元 - 符合江苏省地方标准 DB32/T 4124
 */
public class DataElement {
    private String name;           // 数据元名称
    private String identifier;     // 数据元标识符
    private String definition;     // 数据元定义
    private String dataType;       // 数据类型（字符串、整数、浮点数、日期等）
    private String dataTypeCode;   // 数据类型代码（C-字符型,N-数值型,D-日期型,T-时间型,B-布尔型）
    private int length;            // 数据长度
    private int precision;         // 精度（小数位数）
    private String valueRange;     // 值域
    private String unit;           // 计量单位
    private String constraint;     // 约束条件
    private String remark;         // 备注

    public DataElement() {}

    public DataElement(String name, String identifier, String definition,
                       String dataType, int length, int precision,
                       String valueRange, String unit, String constraint, String remark) {
        this.name = name;
        this.identifier = identifier;
        this.definition = definition;
        this.dataType = dataType;
        this.dataTypeCode = convertToCode(dataType);
        this.length = length;
        this.precision = precision;
        this.valueRange = valueRange;
        this.unit = unit;
        this.constraint = constraint;
        this.remark = remark;
    }

    /**
     * 将中文数据类型转换为标准代码
     * C-字符型 N-数值型 D-日期型 T-时间型 B-布尔型
     */
    private String convertToCode(String dataType) {
        if (dataType == null) return "C";
        return switch (dataType) {
            case "字符串" -> "C";
            case "整数", "浮点数" -> "N";
            case "日期" -> "D";
            case "时间", "时间戳" -> "T";
            case "布尔" -> "B";
            default -> "C";
        };
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) {
        this.dataType = dataType;
        this.dataTypeCode = convertToCode(dataType);
    }

    public String getDataTypeCode() { return dataTypeCode; }
    public void setDataTypeCode(String dataTypeCode) { this.dataTypeCode = dataTypeCode; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = precision; }

    public String getValueRange() { return valueRange; }
    public void setValueRange(String valueRange) { this.valueRange = valueRange; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getConstraint() { return constraint; }
    public void setConstraint(String constraint) { this.constraint = constraint; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
