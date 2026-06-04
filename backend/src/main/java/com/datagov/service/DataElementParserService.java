package com.datagov.service;

import com.datagov.model.DataElement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据元解析服务 - 基于江苏省地方标准 DB32/T 4124
 * 覆盖政府全场景：个人身份、组织机构、金融财税、医疗卫生、教育、不动产、车辆交通等
 */
@Service
public class DataElementParserService {

    private static final List<DataElementTemplate> TEMPLATES = List.of(
        // ==================== 一、个人身份类 ====================
        t("身份证号码", "ID_CARD", "居民身份证号码", "字符串", 18, 0,
            "17位数字+1位校验码(0-9或X)", "", "必填", "符合GB 11643"),
        t("护照号码", "PASSPORT", "护照号码", "字符串", 9, 0,
            "字母+数字组合", "", "", "E/G/D/S/P/H开头"),
        t("军官证号", "MILITARY_ID", "军官证号码", "字符串", 18, 0,
            "字母+数字组合", "", "", ""),
        t("港澳通行证", "HK_MACAU_PASS", "港澳居民来往内地通行证号码", "字符串", 11, 0,
            "字母+数字组合", "", "", "H/M开头"),
        t("台湾通行证", "TAIWAN_PASS", "台湾居民来往大陆通行证号码", "字符串", 8, 0,
            "字母+数字组合", "", "", ""),
        t("社保卡号", "SOCIAL_CARD", "社会保障卡号码", "字符串", 18, 0,
            "18位数字", "", "", "符合人社部标准"),
        t("医保卡号", "MEDICAL_CARD", "医疗保险卡号码", "字符串", 18, 0,
            "数字", "", "", ""),
        t("驾驶证号", "DRIVER_LICENSE", "机动车驾驶证号码", "字符串", 18, 0,
            "与身份证号一致", "", "", "符合GA 482"),
        t("居住证号", "RESIDENCE_PERMIT", "居住证号码", "字符串", 18, 0,
            "数字", "", "", ""),
        t("港澳居民身份证", "HK_MACAU_RESIDENT", "港澳居民居住证号码", "字符串", 18, 0,
            "8位数字+10位数字", "", "", "符合公安部标准"),
        t("台湾居民身份证", "TAIWAN_RESIDENT", "台湾居民居住证号码", "字符串", 18, 0,
            "8位数字+10位数字", "", "", ""),

        // ==================== 二、个人信息类 ====================
        t("姓名", "NAME", "个人姓名", "字符串", 50, 0,
            "中文或英文字符", "", "必填", ""),
        t("曾用名", "FORMER_NAME", "曾用名", "字符串", 50, 0,
            "中文字符", "", "", ""),
        t("性别", "GENDER", "性别代码", "字符串", 1, 0,
            "0-未知,1-男,2-女,9-未说明", "", "必填", "符合GB/T 2261.1"),
        t("出生日期", "BIRTH_DATE", "出生日期", "日期", 10, 0,
            "YYYY-MM-DD格式", "", "必填", "符合GB/T 7408"),
        t("年龄", "AGE", "年龄（周岁）", "整数", 3, 0,
            "0-150", "岁", "", ""),
        t("民族", "ETHNICITY", "民族代码", "字符串", 2, 0,
            "01-汉族,02-蒙古族,03-回族...", "", "", "符合GB/T 3304"),
        t("国籍", "NATIONALITY", "国籍代码", "字符串", 3, 0,
            "CHN-中国,USA-美国...", "", "", "符合GB/T 2659"),
        t("籍贯", "NATIVE_PLACE", "籍贯", "字符串", 100, 0,
            "省+市/县", "", "", ""),
        t("出生地", "BIRTH_PLACE", "出生地", "字符串", 100, 0,
            "省+市/县", "", "", ""),
        t("政治面貌", "POLITICAL_STATUS", "政治面貌代码", "字符串", 2, 0,
            "01-中共党员,02-中共预备党员,03-共青团员,04-民革...", "", "", "符合GB/T 4762"),
        t("婚姻状况", "MARITAL_STATUS", "婚姻状况代码", "字符串", 1, 0,
            "1-未婚,2-已婚,3-丧偶,4-离婚,9-未说明", "", "", "符合GB/T 4766"),
        t("健康状况", "HEALTH_STATUS", "健康状况代码", "字符串", 1, 0,
            "1-健康,2-一般,3-较弱,4-有疾病", "", "", "符合GB/T 4767"),
        t("血型", "BLOOD_TYPE", "血型代码", "字符串", 1, 0,
            "A,B,O,AB", "", "", ""),
        t("学历", "EDUCATION", "学历代码", "字符串", 2, 0,
            "10-研究生,20-大学本科,30-大学专科,40-高中,50-初中,60-小学", "", "", "符合GB/T 4658"),
        t("学位", "DEGREE", "学位代码", "字符串", 2, 0,
            "1-博士,2-硕士,3-学士", "", "", "符合GB/T 6864"),
        t("毕业院校", "GRADUATE_SCHOOL", "毕业院校", "字符串", 100, 0,
            "学校全称", "", "", ""),
        t("专业", "MAJOR", "所学专业", "字符串", 100, 0,
            "专业名称", "", "", "符合GB/T 13745"),
        t("职业", "OCCUPATION", "职业代码", "字符串", 4, 0,
            "符合GB/T 6565", "", "", "符合GB/T 6565"),
        t("职务", "POSITION", "职务", "字符串", 50, 0,
            "职务名称", "", "", ""),
        t("职称", "TITLE", "专业技术职称", "字符串", 20, 0,
            "初级/中级/副高/正高", "", "", "符合GB/T 8561"),
        t("工作单位", "WORK_UNIT", "工作单位", "字符串", 200, 0,
            "单位全称", "", "", ""),
        t("工号", "EMPLOYEE_NO", "员工编号", "字符串", 20, 0,
            "字母+数字", "", "", ""),

        // ==================== 三、联系方式类 ====================
        t("手机号码", "MOBILE", "移动电话号码", "字符串", 11, 0,
            "以1开头的11位数字", "", "必填", "符合工信部编号规则"),
        t("固定电话", "PHONE", "固定电话号码", "字符串", 20, 0,
            "区号-号码或手机号", "", "", "如025-88888888"),
        t("电子邮箱", "EMAIL", "电子邮箱地址", "字符串", 100, 0,
            "标准邮箱格式", "", "", "符合RFC 5322"),
        t("邮政编码", "ZIP_CODE", "邮政编码", "字符串", 6, 0,
            "6位数字", "", "", "符合GB/T 2260"),
        t("通讯地址", "MAILING_ADDRESS", "通讯地址", "字符串", 200, 0,
            "省+市+区+街道+门牌号", "", "必填", ""),
        t("户籍地址", "REGISTERED_ADDRESS", "户籍地址", "字符串", 200, 0,
            "省+市+区+街道+门牌号", "", "", ""),
        t("地址", "ADDRESS", "详细地址", "字符串", 200, 0,
            "省+市+区+街道+门牌号", "", "必填", ""),

        // ==================== 四、组织机构类 ====================
        t("统一社会信用代码", "USCC", "统一社会信用代码", "字符串", 18, 0,
            "18位字母数字组合", "", "必填", "符合GB 32100"),
        t("组织机构代码", "ORG_CODE", "组织机构代码", "字符串", 9, 0,
            "8位字符+1位校验码", "", "", "符合GB 11714"),
        t("企业名称", "COMPANY_NAME", "企业（机构）名称", "字符串", 200, 0,
            "工商注册全称", "", "必填", ""),
        t("法定代表人", "LEGAL_PERSON", "法定代表人姓名", "字符串", 50, 0,
            "中文姓名", "", "必填", ""),
        t("注册地址", "REG_ADDRESS", "注册地址", "字符串", 200, 0,
            "工商注册地址", "", "", ""),
        t("注册资本", "REG_CAPITAL", "注册资本", "浮点数", 14, 2,
            "≥0", "万元", "", ""),
        t("成立日期", "ESTABLISH_DATE", "成立日期", "日期", 10, 0,
            "YYYY-MM-DD格式", "", "", ""),
        t("经营期限", "OPERATE_PERIOD", "经营期限", "字符串", 50, 0,
            "起止日期或长期", "", "", ""),
        t("经营范围", "BUSINESS_SCOPE", "经营范围", "字符串", 2000, 0,
            "工商登记经营范围", "", "", ""),
        t("行业分类", "INDUSTRY_CODE", "行业分类代码", "字符串", 4, 0,
            "符合GB/T 4754", "", "", "符合GB/T 4754"),
        t("企业类型", "COMPANY_TYPE", "企业类型代码", "字符串", 2, 0,
            "11-国企,12-集体,13-私营,14-外资...", "", "", ""),
        t("纳税人识别号", "TAX_ID", "纳税人识别号", "字符串", 20, 0,
            "与统一社会信用代码一致", "", "", ""),
        t("医疗机构代码", "MEDICAL_ORG_CODE", "医疗机构代码", "字符串", 22, 0,
            "22位编码", "", "", "符合WS 218"),

        // ==================== 五、金融财税类 ====================
        t("银行卡号", "BANK_CARD", "银行卡号", "字符串", 19, 0,
            "16-19位数字", "", "", "符合Luhn算法校验"),
        t("开户银行", "BANK_NAME", "开户银行名称", "字符串", 100, 0,
            "银行全称", "", "", ""),
        t("银行账号", "BANK_ACCOUNT", "银行账号", "字符串", 30, 0,
            "数字", "", "", ""),
        t("税务登记号", "TAX_REG_NO", "税务登记号", "字符串", 20, 0,
            "数字", "", "", ""),
        t("发票代码", "INVOICE_CODE", "发票代码", "字符串", 12, 0,
            "10-12位数字", "", "", ""),
        t("发票号码", "INVOICE_NO", "发票号码", "字符串", 8, 0,
            "8位数字", "", "", ""),
        t("金额", "AMOUNT", "金额数值", "浮点数", 14, 2,
            "≥0", "元", "", "精确到分"),
        t("税率", "TAX_RATE", "税率", "浮点数", 5, 4,
            "0-1", "", "", ""),
        t("价格", "PRICE", "价格", "浮点数", 14, 2,
            "≥0", "元", "", ""),
        t("数量", "QUANTITY", "数量", "浮点数", 12, 2,
            "≥0", "", "", ""),

        // ==================== 六、政务通用类 ====================
        t("行政区划代码", "AREA_CODE", "行政区划代码", "字符串", 6, 0,
            "6位数字", "", "", "符合GB/T 2260"),
        t("项目编号", "PROJECT_NO", "项目编号", "字符串", 30, 0,
            "字母+数字", "", "", ""),
        t("批文号", "APPROVAL_NO", "批文号/批复文号", "字符串", 50, 0,
            "年份+序号", "", "", ""),
        t("许可证号", "LICENSE_NO", "许可证号码", "字符串", 50, 0,
            "字母+数字", "", "", ""),
        t("案件编号", "CASE_NO", "案件编号", "字符串", 30, 0,
            "年份+类型+序号", "", "", ""),
        t("公文编号", "DOC_NO", "公文编号/文号", "字符串", 50, 0,
            "机关代字+年份+序号", "", "", "如国发〔2024〕1号"),
        t("文号", "DOCUMENT_NO", "文号", "字符串", 50, 0,
            "机关代字+年份+序号", "", "", ""),
        t("流水号", "SERIAL_NO", "流水号", "字符串", 30, 0,
            "递增数字", "", "", ""),
        t("序号", "SEQUENCE_NO", "序号", "整数", 10, 0,
            "≥1", "", "", ""),
        t("编码", "CODE", "通用编码", "字符串", 30, 0,
            "字母+数字", "", "", ""),
        t("状态", "STATUS", "状态码", "字符串", 2, 0,
            "01-有效,02-无效,03-待审核...", "", "", ""),
        t("标志", "FLAG", "标志位", "字符串", 1, 0,
            "0-否,1-是", "", "", ""),
        t("等级", "LEVEL", "等级", "字符串", 2, 0,
            "01-一级,02-二级...", "", "", ""),
        t("分数", "SCORE", "分数", "浮点数", 6, 2,
            "0-100", "分", "", ""),
        t("百分比", "PERCENTAGE", "百分比", "浮点数", 5, 2,
            "0-100", "%", "", ""),

        // ==================== 七、医疗卫生类 ====================
        t("病历号", "MEDICAL_RECORD_NO", "病历号", "字符串", 20, 0,
            "数字或字母+数字", "", "", ""),
        t("诊断编码", "DIAGNOSIS_CODE", "诊断编码（ICD-10）", "字符串", 10, 0,
            "ICD-10编码格式", "", "", "符合ICD-10"),
        t("药品编码", "DRUG_CODE", "药品编码", "字符串", 20, 0,
            "国药准字+字母+数字", "", "", "符合国药准字编码规则"),
        t("处方号", "PRESCRIPTION_NO", "处方号", "字符串", 20, 0,
            "字母+数字", "", "", ""),

        // ==================== 八、教育类 ====================
        t("学籍号", "STUDENT_ID", "学籍号", "字符串", 19, 0,
            "G+身份证号或L+临时学籍号", "", "", "符合教育部标准"),
        t("考生号", "EXAM_NO", "考生号", "字符串", 14, 0,
            "14位数字", "", "", ""),
        t("准考证号", "ADMISSION_TICKET", "准考证号", "字符串", 16, 0,
            "数字", "", "", ""),
        t("毕业证号", "DIPLOMA_NO", "毕业证书编号", "字符串", 18, 0,
            "数字", "", "", ""),
        t("学位证号", "DEGREE_CERT_NO", "学位证书编号", "字符串", 16, 0,
            "数字", "", "", ""),
        t("学校代码", "SCHOOL_CODE", "学校代码", "字符串", 10, 0,
            "数字", "", "", "符合教育部学校代码"),
        t("专业代码", "MAJOR_CODE", "专业代码", "字符串", 6, 0,
            "数字", "", "", "符合GB/T 13745"),

        // ==================== 九、不动产类 ====================
        t("不动产证号", "REALESTATE_CERT", "不动产权证书号", "字符串", 20, 0,
            "字母+数字", "", "", ""),
        t("土地证号", "LAND_CERT", "土地使用权证号", "字符串", 20, 0,
            "字母+数字", "", "", ""),
        t("房产证号", "HOUSE_CERT", "房屋所有权证号", "字符串", 20, 0,
            "字母+数字", "", "", ""),
        t("不动产单元号", "REALESTATE_UNIT", "不动产单元号", "字符串", 28, 0,
            "28位编码", "", "", "符合GB/T 26424"),
        t("宗地号", "PARCEL_NO", "宗地号", "字符串", 20, 0,
            "数字", "", "", ""),

        // ==================== 十、车辆交通类 ====================
        t("车牌号", "PLATE_NUMBER", "机动车号牌号码", "字符串", 8, 0,
            "汉字+字母+数字", "", "", "符合GA 36"),
        t("车架号", "VIN", "车辆识别代号（VIN）", "字符串", 17, 0,
            "17位字母+数字", "", "", "符合GB 16735"),
        t("发动机号", "ENGINE_NO", "发动机号码", "字符串", 20, 0,
            "字母+数字", "", "", ""),
        t("行驶证号", "DRIVING_CERT", "机动车行驶证号", "字符串", 20, 0,
            "数字", "", "", ""),
        t("运输证号", "TRANSPORT_CERT", "道路运输证号", "字符串", 20, 0,
            "字母+数字", "", "", ""),

        // ==================== 十一、时间日期类 ====================
        t("日期", "DATE", "日期", "日期", 10, 0,
            "YYYY-MM-DD格式", "", "", "符合GB/T 7408"),
        t("时间", "TIME", "时间", "时间", 8, 0,
            "HH:mm:ss格式", "", "", "符合GB/T 7408"),
        t("时间戳", "TIMESTAMP", "日期时间", "时间戳", 19, 0,
            "YYYY-MM-DD HH:mm:ss", "", "", "符合GB/T 7408"),
        t("年份", "YEAR", "年份", "字符串", 4, 0,
            "YYYY格式", "", "", ""),
        t("月份", "MONTH", "月份", "字符串", 7, 0,
            "YYYY-MM格式", "", "", ""),

        // ==================== 十二、网络信息类 ====================
        t("IP地址", "IP_ADDRESS", "IP地址", "字符串", 45, 0,
            "IPv4或IPv6格式", "", "", "如192.168.1.1"),
        t("MAC地址", "MAC_ADDRESS", "MAC地址", "字符串", 17, 0,
            "XX:XX:XX:XX:XX:XX", "", "", ""),
        t("URL地址", "URL", "URL地址", "字符串", 2000, 0,
            "http/https格式", "", "", ""),
        t("域名", "DOMAIN", "域名", "字符串", 253, 0,
            "标准域名格式", "", "", "符合RFC 1035"),

        // ==================== 十三、布尔/通用类型 ====================
        t("布尔值", "BOOLEAN", "布尔类型", "布尔", 1, 0,
            "0-否, 1-是", "", "", ""),
        t("整数", "INTEGER", "整数数值", "整数", 10, 0,
            "", "", "", ""),
        t("浮点数", "FLOAT", "浮点数值", "浮点数", 12, 2,
            "", "", "", ""),
        t("备注", "REMARK", "备注", "字符串", 500, 0,
            "文本", "", "", ""),
        t("描述", "DESCRIPTION", "描述", "字符串", 2000, 0,
            "文本", "", "", ""),
        t("名称", "NAME_FIELD", "名称", "字符串", 200, 0,
            "文本", "", "", ""),
        t("编号", "NO", "编号", "字符串", 30, 0,
            "字母+数字", "", "", "")
    );

    public List<DataElement> parse(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        String[] parts = input.split("[,;，；\n、]+");
        List<DataElement> results = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                DataElement element = parseSingle(trimmed);
                if (element != null) {
                    results.add(element);
                }
            }
        }

        if (results.isEmpty()) {
            results.add(parseGeneric(input.trim()));
        }

        return results;
    }

    private DataElement parseSingle(String input) {
        // 提取名称和长度
        String name = null;
        int explicitLength = -1;

        // 按优先级尝试多种模式
        Pattern[] patterns = {
            Pattern.compile("(.+?)[\\s]*[长为有]?[\\s]*(\\d+)[\\s]*位"),   // "xxx N位"
            Pattern.compile("(.+?)长度[\\s:=：]*(\\d+)"),                   // "xxx长度N"
            Pattern.compile("(.+?)[(（](\\d+)[)）位]")                      // "xxx(N)"
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(input);
            if (m.matches()) {
                name = m.group(1).trim();
                explicitLength = Integer.parseInt(m.group(2));
                break;
            }
        }
        if (name == null) {
            name = input.replaceAll("[,;，；\\s]+$", "").trim();
        }

        String dataTypeHint = extractDataType(input);
        int precision = extractPrecision(input);

        // 模糊匹配模板
        DataElementTemplate bestMatch = null;
        int bestScore = 0;
        for (DataElementTemplate template : TEMPLATES) {
            int score = calculateMatchScore(name, template.name);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = template;
            }
        }

        if (bestMatch != null && bestScore >= 2) {
            DataElement de = bestMatch.toDataElement();
            if (explicitLength > 0) {
                de.setLength(explicitLength);
            }
            if (dataTypeHint != null) {
                de.setDataType(dataTypeHint);
            }
            if (precision > 0) {
                de.setPrecision(precision);
            }
            if (!name.equals(bestMatch.name) && name.length() > 1) {
                de.setName(name);
            }
            return de;
        }

        return buildGeneric(name, explicitLength, dataTypeHint, precision);
    }

    private DataElement buildGeneric(String name, int length, String dataType, int precision) {
        if (dataType == null) {
            dataType = "字符串";
        }
        if (length <= 0) {
            length = guessLength(dataType);
        }
        return new DataElement(name, "DE_" + Math.abs(name.hashCode()) % 10000,
            name, dataType, length, precision, "", "", "", "");
    }

    private DataElement parseGeneric(String input) {
        return buildGeneric(input, -1, extractDataType(input), extractPrecision(input));
    }

    private String extractDataType(String input) {
        if (input.contains("整数") || input.matches("(?i).*\\bint(eger)?\\b.*")) return "整数";
        if (input.contains("浮点") || input.contains("小数") || input.matches("(?i).*\\b(float|double|decimal|number)\\b.*")) return "浮点数";
        if (input.contains("日期时间") || input.contains("时间戳") || input.matches("(?i).*\\bdatetime\\b.*")) return "时间戳";
        if (input.contains("日期") || input.matches("(?i).*\\bdate\\b.*")) return "日期";
        if (input.contains("时间") || input.matches("(?i).*\\btime\\b.*")) return "时间";
        if (input.contains("布尔") || input.matches("(?i).*\\bbool(ean)?\\b.*")) return "布尔";
        if (input.contains("字符串") || input.contains("文本") || input.matches("(?i).*\\b(varchar|char|text|string)\\b.*")) return "字符串";
        return null;
    }

    private int extractPrecision(String input) {
        Pattern p = Pattern.compile("(\\d+)[\\s]*位[\\s]*小数");
        Matcher m = p.matcher(input);
        if (m.find()) return Integer.parseInt(m.group(1));
        p = Pattern.compile("精度[\\s:=：]*(\\d+)");
        m = p.matcher(input);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private int guessLength(String dataType) {
        return switch (dataType) {
            case "整数" -> 10;
            case "浮点数" -> 12;
            case "日期" -> 10;
            case "时间" -> 8;
            case "时间戳" -> 19;
            case "布尔" -> 1;
            default -> 255;
        };
    }

    private int calculateMatchScore(String input, String templateName) {
        if (input.equals(templateName)) return 10;
        if (input.contains(templateName) || templateName.contains(input)) return 5;
        int match = 0;
        for (char c : templateName.toCharArray()) {
            if (input.indexOf(c) >= 0) match++;
        }
        return match;
    }

    private static DataElementTemplate t(String name, String id, String def, String type,
                                          int len, int prec, String range, String unit,
                                          String constraint, String remark) {
        return new DataElementTemplate(name, id, def, type, len, prec, range, unit, constraint, remark);
    }

    private static class DataElementTemplate {
        final String name, identifier, definition, dataType, valueRange, unit, constraint, remark;
        final int length, precision;

        DataElementTemplate(String name, String identifier, String definition,
                            String dataType, int length, int precision,
                            String valueRange, String unit, String constraint, String remark) {
            this.name = name; this.identifier = identifier; this.definition = definition;
            this.dataType = dataType; this.length = length; this.precision = precision;
            this.valueRange = valueRange; this.unit = unit; this.constraint = constraint;
            this.remark = remark;
        }

        DataElement toDataElement() {
            return new DataElement(name, identifier, definition, dataType, length, precision,
                valueRange, unit, constraint, remark);
        }
    }
}
