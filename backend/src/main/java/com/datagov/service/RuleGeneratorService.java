package com.datagov.service;

import com.datagov.model.DataElement;
import com.datagov.model.GovernanceRule;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 治理规则生成服务
 * 生成包含六性标签的治理规则，支持 OceanBase SQL、正则表达式、Java、Python 多种实现
 * 统一使用 {0} 作为列名占位符
 */
@Service
public class RuleGeneratorService {

    private final AtomicInteger ruleCounter = new AtomicInteger(1);

    /**
     * 根据数据元生成治理规则
     */
    public List<GovernanceRule> generate(List<DataElement> dataElements) {
        ruleCounter.set(1);
        List<GovernanceRule> rules = new ArrayList<>();

        for (DataElement de : dataElements) {
            // 完整性 - 非空校验
            if ("必填".equals(de.getConstraint())) {
                rules.add(buildRule(de, "非空校验", "完整性",
                    "检查" + de.getName() + "不为空",
                    "LENGTH(TRIM({0})) > 0", null,
                    buildRegexNotNull(de), buildJavaRegex(de, true), buildPythonRegex(de, true),
                    "ERROR"));
            }

            // 准确性 - 长度校验
            if (de.getLength() > 0) {
                String regex = buildLengthRegex(de);
                rules.add(buildRule(de, "长度校验", "准确性",
                    "检查" + de.getName() + "长度为" + de.getLength() + "位",
                    buildLengthSql(de), null,
                    regex, buildJavaRegex(de, regex), buildPythonRegex(de, regex),
                    "ERROR"));
            }

            // 准确性 - 精度校验（浮点数小数位）
            if ("数值型".equals(de.getDataType()) && de.getPrecision() > 0) {
                rules.add(buildRule(de, "精度校验", "准确性",
                    "检查" + de.getName() + "小数位数不超过" + de.getPrecision() + "位",
                    "LENGTH(SUBSTRING_INDEX({0}, '.', -1)) <= " + de.getPrecision(), null,
                    "^-?\\d+(\\.\\d{1," + de.getPrecision() + "})?$",
                    buildJavaPattern("^-?\\d+(\\.\\d{1," + de.getPrecision() + "})?$"),
                    buildPythonPattern("^-?\\d+(\\.\\d{1," + de.getPrecision() + "})?$"),
                    "WARNING"));
            }

            // 规范性 - 值域校验
            if (de.getValueDomain() != null && !de.getValueDomain().isEmpty()) {
                String rangeSql = buildRangeSql(de.getValueDomain(), de.getDataType());
                String rangeRegex = buildRangeRegex(de.getValueDomain(), de.getDataType());
                if (rangeSql != null) {
                    rules.add(buildRule(de, "值域校验", "规范性",
                        "检查" + de.getName() + "值在允许范围内",
                        rangeSql, null,
                        rangeRegex,
                        rangeRegex != null ? buildJavaPattern(rangeRegex) : null,
                        rangeRegex != null ? buildPythonPattern(rangeRegex) : null,
                        "ERROR"));
                }
            }

            // 准确性 - 格式校验
            FormatResult formatResult = buildFormatResult(de);
            if (formatResult.sql != null) {
                rules.add(buildRule(de, "格式校验", "准确性",
                    "检查" + de.getName() + "格式正确",
                    formatResult.sql, null,
                    formatResult.regex,
                    formatResult.regex != null ? buildJavaPattern(formatResult.regex) : null,
                    formatResult.regex != null ? buildPythonPattern(formatResult.regex) : null,
                    "ERROR"));
            }

            // 时效性 - 日期有效性校验
            if ("日期型".equals(de.getDataType())) {
                rules.add(buildRule(de, "日期有效性", "时效性",
                    "检查" + de.getName() + "日期在合理范围内",
                    "{0} >= '1900-01-01' AND {0} <= '2099-12-31'", null,
                    "^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
                    buildJavaPattern("^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"),
                    buildPythonPattern("^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"),
                    "WARNING"));
            }
        }

        return rules;
    }

    // ========== 规则构建 ==========

    private GovernanceRule buildRule(DataElement de, String ruleType, String qualityDimension,
                                     String description, String sql, String extraSql,
                                     String regex, String javaCode, String pythonCode,
                                     String severity) {
        GovernanceRule rule = new GovernanceRule();
        rule.setRuleId("RULE_" + String.format("%04d", ruleCounter.getAndIncrement()));
        rule.setRuleName(de.getName() + " - " + ruleType);
        rule.setRuleType(ruleType);
        rule.setQualityDimension(qualityDimension);
        rule.setDescription(description);
        rule.setSqlExpression(sql);
        rule.setRegexExpression(regex);
        rule.setJavaCode(javaCode);
        rule.setPythonCode(pythonCode);
        rule.setSeverity(severity);
        rule.setDataElementName(de.getName());
        return rule;
    }

    // ========== SQL 构建 ==========

    private String buildLengthSql(DataElement de) {
        String type = de.getDataType();
        int len = de.getLength();

        if ("字符型".equals(type) || "日期型".equals(type)) {
            return "LENGTH({0}) = " + len;
        }
        if ("数值型".equals(type)) {
            long maxVal = (long) Math.pow(10, len) - 1;
            return "{0} >= -" + maxVal + " AND {0} <= " + maxVal;
        }
        if ("布尔型".equals(type)) {
            return "{0} IN (0, 1)";
        }
        return "LENGTH({0}) <= " + len;
    }

    private String buildRangeSql(String valueDomain, String dataType) {
        if (valueDomain == null || valueDomain.isEmpty()) return null;

        // 枚举格式: "1-男,2-女" 或 "A/B/O/AB"
        if (valueDomain.contains("/")) {
            String[] values = valueDomain.split("/");
            return "{0} IN (" + String.join(", ",
                Arrays.stream(values).map(v -> "'" + v.split("-")[0].trim() + "'").toArray(String[]::new)) + ")";
        }

        // 范围格式: "≥0"
        if (valueDomain.startsWith("≥") || valueDomain.startsWith(">=")) {
            return "{0} >= " + valueDomain.replaceAll("[≥>= ]+", "");
        }
        if (valueDomain.startsWith("≤") || valueDomain.startsWith("<=")) {
            return "{0} <= " + valueDomain.replaceAll("[≤<= ]+", "");
        }

        // 区间格式: "0-150"
        if (valueDomain.matches("\\d+\\s*[-~]\\s*\\d+")) {
            String[] parts = valueDomain.split("[-~]");
            return "{0} >= " + parts[0].trim() + " AND {0} <= " + parts[1].trim();
        }

        // 枚举键值对: "10-研究生,20-本科,30-专科" 或 "01-汉族,02-蒙古族..."
        if (valueDomain.contains(",") || valueDomain.contains("，")) {
            String[] pairs = valueDomain.split("[,，]");
            List<String> codes = new ArrayList<>();
            for (String pair : pairs) {
                String code = pair.trim().split("[-—]")[0].trim();
                if (!code.isEmpty()) codes.add("'" + code + "'");
            }
            if (!codes.isEmpty()) return "{0} IN (" + String.join(", ", codes) + ")";
        }

        return null;
    }

    // ========== 正则表达式构建 ==========

    private String buildLengthRegex(DataElement de) {
        String type = de.getDataType();
        int len = de.getLength();

        if ("布尔型".equals(type)) return "^[01]$";
        if ("日期型".equals(type)) return "^\\d{" + len + "}$";
        if ("字符型".equals(type)) return "^.{" + len + "}$";
        if ("数值型".equals(type)) return "^-?\\d{1," + len + "}$";
        return null;
    }

    private String buildRegexNotNull(DataElement de) {
        return "^.+$";
    }

    private String buildRangeRegex(String valueDomain, String dataType) {
        if (valueDomain == null || valueDomain.isEmpty()) return null;

        // 枚举格式: "A/B/O/AB"
        if (valueDomain.contains("/") && !valueDomain.contains("-")) {
            String[] values = valueDomain.split("/");
            return "^(" + String.join("|", Arrays.stream(values).map(String::trim).toArray(String[]::new)) + ")$";
        }

        // 枚举键值对: "01-汉族,02-蒙古族..."
        if ((valueDomain.contains(",") || valueDomain.contains("，")) && valueDomain.contains("-")) {
            String[] pairs = valueDomain.split("[,，]");
            List<String> codes = new ArrayList<>();
            for (String pair : pairs) {
                String code = pair.trim().split("[-—]")[0].trim();
                if (!code.isEmpty()) codes.add(code);
            }
            if (!codes.isEmpty()) return "^(" + String.join("|", codes) + ")$";
        }

        return null;
    }

    // ========== 格式校验 ==========

    private record FormatResult(String sql, String regex) {}

    private FormatResult buildFormatResult(DataElement de) {
        String name = de.getName();
        String id = de.getIdentifier();
        String type = de.getDataType();

        // 个人身份类
        if (name.contains("身份证") || "ID_CARD".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$'",
                "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
        }
        if (name.contains("护照") || "PASSPORT".equals(id)) {
            return new FormatResult("{0} REGEXP '^[EeGgDdSsPpHh][0-9]{8}$'", "^[EeGgDdSsPpHh]\\d{8}$");
        }
        if (name.contains("军官证") || "MILITARY_ID".equals(id)) {
            return new FormatResult("{0} REGEXP '^[A-Za-z0-9]{5,18}$'", "^[A-Za-z0-9]{5,18}$");
        }
        if (name.contains("港澳通行证") || "HK_MACAU_PASS".equals(id)) {
            return new FormatResult("{0} REGEXP '^[HhMm][0-9]{8,10}$'", "^[HhMm]\\d{8,10}$");
        }
        if (name.contains("台湾通行证") || "TAIWAN_PASS".equals(id)) {
            return new FormatResult("{0} REGEXP '^[A-Za-z0-9]{8}$'", "^[A-Za-z0-9]{8}$");
        }
        if (name.contains("社保卡") || "SOCIAL_CARD".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{18}$'", "^\\d{18}$");
        }
        if (name.contains("驾驶证") || "DRIVER_LICENSE".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$'",
                "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
        }
        if (name.contains("港澳居民居住证") || "HK_MACAU_RESIDENT".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{18}$'", "^\\d{18}$");
        }
        if (name.contains("台湾居民居住证") || "TAIWAN_RESIDENT".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{18}$'", "^\\d{18}$");
        }

        // 联系方式类
        if (name.contains("手机") || "MOBILE".equals(id)) {
            return new FormatResult("{0} REGEXP '^1[3-9][0-9]{9}$'", "^1[3-9]\\d{9}$");
        }
        if (name.contains("邮箱") || name.contains("电子邮件") || "EMAIL".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        }
        if (name.contains("邮政编码") || "ZIP_CODE".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{6}$'", "^\\d{6}$");
        }
        if ((name.contains("固定电话") || "PHONE".equals(id)) && de.getLength() == 20) {
            return new FormatResult(
                "{0} REGEXP '^(0[0-9]{2,3}-[0-9]{7,8}|1[3-9][0-9]{9})$'",
                "^(0\\d{2,3}-\\d{7,8}|1[3-9]\\d{9})$");
        }

        // 组织机构类
        if (name.contains("统一社会信用") || "USCC".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^[0-9A-HJ-NPQRTUWXY]{2}[0-9]{6}[0-9A-HJ-NPQRTUWXY]{10}$'",
                "^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$");
        }
        if (name.contains("组织机构代码") || "ORG_CODE".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9A-Z]{8}[0-9X]$'", "^[0-9A-Z]{8}[\\dX]$");
        }
        if (name.contains("医疗机构代码") || "MEDICAL_ORG_CODE".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{22}$'", "^\\d{22}$");
        }

        // 金融财税类
        if (name.contains("银行卡") || "BANK_CARD".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{16,19}$'", "^\\d{16,19}$");
        }
        if (name.contains("发票代码") || "INVOICE_CODE".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{10,12}$'", "^\\d{10,12}$");
        }
        if (name.contains("发票号码") || "INVOICE_NO".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{8}$'", "^\\d{8}$");
        }

        // 教育类
        if (name.contains("学籍号") || "STUDENT_ID".equals(id)) {
            return new FormatResult("{0} REGEXP '^[GL][0-9]{17,18}$'", "^[GL]\\d{17,18}$");
        }
        if (name.contains("考生号") || "EXAM_NO".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{14}$'", "^\\d{14}$");
        }
        if (name.contains("准考证") || "ADMISSION_TICKET".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{10,16}$'", "^\\d{10,16}$");
        }
        if (name.contains("毕业证") || "DIPLOMA_NO".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{18}$'", "^\\d{18}$");
        }
        if (name.contains("学位证") || "DEGREE_CERT_NO".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{16}$'", "^\\d{16}$");
        }

        // 不动产类
        if (name.contains("不动产单元") || "REALESTATE_UNIT".equals(id)) {
            return new FormatResult("{0} REGEXP '^[0-9]{28}$'", "^\\d{28}$");
        }

        // 车辆交通类
        if (name.contains("车牌") || "PLATE_NUMBER".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$'",
                "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$");
        }
        if (name.contains("车架号") || "VIN".equals(id)) {
            return new FormatResult("{0} REGEXP '^[A-HJ-NPR-Z0-9]{17}$'", "^[A-HJ-NPR-Z0-9]{17}$");
        }

        // 网络信息类
        if (name.contains("IP地址") || "IP_ADDRESS".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$'",
                "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
        }
        if (name.contains("MAC地址") || "MAC_ADDRESS".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$'",
                "^([\\da-fA-F]{2}:){5}[\\da-fA-F]{2}$");
        }
        if (name.contains("URL") || "URL".equals(id)) {
            return new FormatResult(
                "{0} REGEXP '^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$'",
                "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
        }

        // 通用类型
        if ("日期型".equals(type)) {
            return new FormatResult(
                "{0} REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$'",
                "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");
        }
        if ("布尔型".equals(type)) {
            return new FormatResult("{0} IN (0, 1)", "^[01]$");
        }

        return new FormatResult(null, null);
    }

    // ========== 多语言代码生成 ==========

    private String buildJavaRegex(DataElement de, boolean notNull) {
        String regex = "^.+$";
        return buildJavaPattern(regex);
    }

    private String buildJavaRegex(DataElement de, String regex) {
        if (regex == null) return null;
        return buildJavaPattern(regex);
    }

    private String buildJavaPattern(String regex) {
        if (regex == null) return null;
        // 转义 Java 字符串中的反斜杠和引号
        String escaped = regex.replace("\\", "\\\\").replace("\"", "\\\"");
        return "Pattern.matches(\"" + escaped + "\", value)";
    }

    private String buildPythonRegex(DataElement de, boolean notNull) {
        return buildPythonPattern("^.+$");
    }

    private String buildPythonRegex(DataElement de, String regex) {
        if (regex == null) return null;
        return buildPythonPattern(regex);
    }

    private String buildPythonPattern(String regex) {
        if (regex == null) return null;
        return "re.match(r'" + regex + "', value)";
    }
}
