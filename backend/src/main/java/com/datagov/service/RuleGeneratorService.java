package com.datagov.service;

import com.datagov.model.DataElement;
import com.datagov.model.GovernanceRule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 治理规则生成服务 - 生成 OceanBase 兼容的 SQL 治理规则
 * 统一使用 {0} 作为列名占位符
 */
@Service
public class RuleGeneratorService {

    private final AtomicInteger ruleCounter = new AtomicInteger(1);

    /**
     * 根据数据元生成治理规则
     */
    public List<GovernanceRule> generate(List<DataElement> dataElements) {
        // 每次生成前重置计数器
        ruleCounter.set(1);
        List<GovernanceRule> rules = new ArrayList<>();

        for (DataElement de : dataElements) {
            // 非空校验
            if ("必填".equals(de.getConstraint())) {
                rules.add(rule(de, "非空校验",
                    "检查" + de.getName() + "不为空",
                    "LENGTH(TRIM({0})) > 0", "ERROR"));
            }

            // 长度校验
            if (de.getLength() > 0) {
                rules.add(rule(de, "长度校验",
                    "检查" + de.getName() + "长度为" + de.getLength() + "位",
                    buildLengthSql(de), "ERROR"));
            }

            // 精度校验（浮点数小数位）
            if ("浮点数".equals(de.getDataType()) && de.getPrecision() > 0) {
                rules.add(rule(de, "精度校验",
                    "检查" + de.getName() + "小数位数不超过" + de.getPrecision() + "位",
                    "LENGTH(SUBSTRING_INDEX({0}, '.', -1)) <= " + de.getPrecision(), "WARNING"));
            }

            // 值域校验
            if (de.getValueRange() != null && !de.getValueRange().isEmpty()) {
                String rangeSql = buildRangeSql(de.getValueRange(), de.getDataType());
                if (rangeSql != null) {
                    rules.add(rule(de, "值域校验",
                        "检查" + de.getName() + "值在允许范围内",
                        rangeSql, "ERROR"));
                }
            }

            // 格式校验
            String formatSql = buildFormatSql(de);
            if (formatSql != null) {
                rules.add(rule(de, "格式校验",
                    "检查" + de.getName() + "格式正确",
                    formatSql, "ERROR"));
            }
        }

        return rules;
    }

    private GovernanceRule createRule(DataElement de, String ruleType, String description,
                                      String sql, String severity) {
        GovernanceRule rule = new GovernanceRule();
        rule.setRuleId("RULE_" + String.format("%04d", ruleCounter.getAndIncrement()));
        rule.setRuleName(de.getName() + " - " + ruleType);
        rule.setRuleType(ruleType);
        rule.setDescription(description);
        rule.setSqlExpression(sql);
        rule.setSeverity(severity);
        rule.setDataElementName(de.getName());
        return rule;
    }

    // alias for brevity
    private GovernanceRule rule(DataElement de, String type, String desc, String sql, String sev) {
        return createRule(de, type, desc, sql, sev);
    }

    private String buildLengthSql(DataElement de) {
        String type = de.getDataType();
        int len = de.getLength();

        if ("字符串".equals(type) || "日期".equals(type) || "时间".equals(type)
            || "时间戳".equals(type) || "布尔".equals(type)) {
            return "LENGTH({0}) = " + len;
        }
        if ("整数".equals(type)) {
            long maxVal = (long) Math.pow(10, len) - 1;
            return "{0} >= -" + maxVal + " AND {0} <= " + maxVal;
        }
        if ("浮点数".equals(type)) {
            int intPart = len - de.getPrecision();
            return "LENGTH(CAST(ABS(FLOOR({0})) AS CHAR)) <= " + intPart;
        }
        return "LENGTH({0}) <= " + len;
    }

    private String buildRangeSql(String valueRange, String dataType) {
        if (valueRange == null || valueRange.isEmpty()) return null;

        // 枚举格式: "1-男,2-女" 或 "A/B/O/AB"
        if (valueRange.contains("/")) {
            String[] values = valueRange.split("/");
            return "{0} IN (" + String.join(", ",
                Arrays.stream(values).map(v -> "'" + v.split("-")[0].trim() + "'").toArray(String[]::new)) + ")";
        }

        // 范围格式: "≥0"
        if (valueRange.startsWith("≥") || valueRange.startsWith(">=")) {
            return "{0} >= " + valueRange.replaceAll("[≥>= ]+", "");
        }
        if (valueRange.startsWith("≤") || valueRange.startsWith("<=")) {
            return "{0} <= " + valueRange.replaceAll("[≤<= ]+", "");
        }

        // 区间格式: "0-150"
        if (valueRange.matches("\\d+\\s*[-~]\\s*\\d+")) {
            String[] parts = valueRange.split("[-~]");
            return "{0} >= " + parts[0].trim() + " AND {0} <= " + parts[1].trim();
        }

        // 枚举键值对: "10-研究生,20-本科,30-专科" 或 "01-汉族,02-蒙古族..."
        if (valueRange.contains(",") || valueRange.contains("，")) {
            String[] pairs = valueRange.split("[,，]");
            List<String> codes = new ArrayList<>();
            for (String pair : pairs) {
                String code = pair.trim().split("[-—]")[0].trim();
                if (!code.isEmpty()) codes.add("'" + code + "'");
            }
            if (!codes.isEmpty()) return "{0} IN (" + String.join(", ", codes) + ")";
        }

        return null;
    }

    /**
     * 根据数据元特征生成格式校验 SQL
     * 使用 {0} 作为列名占位符
     */
    private String buildFormatSql(DataElement de) {
        String name = de.getName();
        String id = de.getIdentifier();
        String type = de.getDataType();

        // ==================== 个人身份类 ====================
        if (name.contains("身份证") || "ID_CARD".equals(id)) {
            return "{0} REGEXP '^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$'";
        }
        if (name.contains("护照") || "PASSPORT".equals(id)) {
            return "{0} REGEXP '^[EeGgDdSsPpHh][0-9]{8}$'";
        }
        if (name.contains("军官证") || "MILITARY_ID".equals(id)) {
            return "{0} REGEXP '^[A-Za-z0-9]{5,18}$'";
        }
        if (name.contains("港澳通行证") || "HK_MACAU_PASS".equals(id)) {
            return "{0} REGEXP '^[HhMm][0-9]{8,10}$'";
        }
        if (name.contains("台湾通行证") || "TAIWAN_PASS".equals(id)) {
            return "{0} REGEXP '^[A-Za-z0-9]{8}$'";
        }
        if (name.contains("社保卡") || "SOCIAL_CARD".equals(id)) {
            return "{0} REGEXP '^[0-9]{18}$'";
        }
        if (name.contains("驾驶证") || "DRIVER_LICENSE".equals(id)) {
            return "{0} REGEXP '^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$'";
        }
        if (name.contains("港澳居民居住证") || "HK_MACAU_RESIDENT".equals(id)) {
            return "{0} REGEXP '^[0-9]{18}$'";
        }
        if (name.contains("台湾居民居住证") || "TAIWAN_RESIDENT".equals(id)) {
            return "{0} REGEXP '^[0-9]{18}$'";
        }

        // ==================== 联系方式类 ====================
        if (name.contains("手机") || "MOBILE".equals(id)) {
            return "{0} REGEXP '^1[3-9][0-9]{9}$'";
        }
        if (name.contains("邮箱") || name.contains("电子邮件") || "EMAIL".equals(id)) {
            return "{0} REGEXP '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'";
        }
        if (name.contains("邮政编码") || "ZIP_CODE".equals(id)) {
            return "{0} REGEXP '^[0-9]{6}$'";
        }
        if ((name.contains("固定电话") || "PHONE".equals(id)) && de.getLength() == 20) {
            return "{0} REGEXP '^(0[0-9]{2,3}-[0-9]{7,8}|1[3-9][0-9]{9})$'";
        }

        // ==================== 组织机构类 ====================
        if (name.contains("统一社会信用") || "USCC".equals(id)) {
            return "{0} REGEXP '^[0-9A-HJ-NPQRTUWXY]{2}[0-9]{6}[0-9A-HJ-NPQRTUWXY]{10}$'";
        }
        if (name.contains("组织机构代码") || "ORG_CODE".equals(id)) {
            return "{0} REGEXP '^[0-9A-Z]{8}[0-9X]$'";
        }
        if (name.contains("医疗机构代码") || "MEDICAL_ORG_CODE".equals(id)) {
            return "{0} REGEXP '^[0-9]{22}$'";
        }

        // ==================== 金融财税类 ====================
        if (name.contains("银行卡") || "BANK_CARD".equals(id)) {
            return "{0} REGEXP '^[0-9]{16,19}$'";
        }
        if (name.contains("发票代码") || "INVOICE_CODE".equals(id)) {
            return "{0} REGEXP '^[0-9]{10,12}$'";
        }
        if (name.contains("发票号码") || "INVOICE_NO".equals(id)) {
            return "{0} REGEXP '^[0-9]{8}$'";
        }

        // ==================== 教育类 ====================
        if (name.contains("学籍号") || "STUDENT_ID".equals(id)) {
            return "{0} REGEXP '^[GL][0-9]{17,18}$'";
        }
        if (name.contains("考生号") || "EXAM_NO".equals(id)) {
            return "{0} REGEXP '^[0-9]{14}$'";
        }
        if (name.contains("准考证") || "ADMISSION_TICKET".equals(id)) {
            return "{0} REGEXP '^[0-9]{10,16}$'";
        }
        if (name.contains("毕业证") || "DIPLOMA_NO".equals(id)) {
            return "{0} REGEXP '^[0-9]{18}$'";
        }
        if (name.contains("学位证") || "DEGREE_CERT_NO".equals(id)) {
            return "{0} REGEXP '^[0-9]{16}$'";
        }

        // ==================== 不动产类 ====================
        if (name.contains("不动产单元") || "REALESTATE_UNIT".equals(id)) {
            return "{0} REGEXP '^[0-9]{28}$'";
        }

        // ==================== 车辆交通类 ====================
        if (name.contains("车牌") || "PLATE_NUMBER".equals(id)) {
            return "{0} REGEXP '^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$'";
        }
        if (name.contains("车架号") || "VIN".equals(id)) {
            return "{0} REGEXP '^[A-HJ-NPR-Z0-9]{17}$'";
        }

        // ==================== 网络信息类 ====================
        if (name.contains("IP地址") || "IP_ADDRESS".equals(id)) {
            return "{0} REGEXP '^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$'";
        }
        if (name.contains("MAC地址") || "MAC_ADDRESS".equals(id)) {
            return "{0} REGEXP '^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$'";
        }
        if (name.contains("URL") || "URL".equals(id)) {
            return "{0} REGEXP '^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$'";
        }

        // ==================== 通用类型 ====================
        if ("日期".equals(type)) {
            return "{0} REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$'";
        }
        if ("时间".equals(type)) {
            return "{0} REGEXP '^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$'";
        }
        if ("时间戳".equals(type)) {
            return "{0} REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]) ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$'";
        }
        if ("整数".equals(type)) {
            return "{0} REGEXP '^-?[0-9]+$'";
        }
        if ("浮点数".equals(type)) {
            return "{0} REGEXP '^-?[0-9]+(\\.[0-9]+)?$'";
        }
        if ("布尔".equals(type)) {
            return "{0} IN (0, 1)";
        }

        return null;
    }
}
