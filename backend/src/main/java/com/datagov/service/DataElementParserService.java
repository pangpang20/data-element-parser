package com.datagov.service;

import com.datagov.model.DataElement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据元解析服务 - 基于 GB/T 19488.1-2004 电子政务数据元标准
 * 覆盖政府全场景：个人身份、组织机构、金融财税、医疗卫生、教育、不动产、车辆交通等
 */
@Service
public class DataElementParserService {

    private static final List<DataElementTemplate> TEMPLATES = List.of(
        // ==================== 一、个人身份类 ====================
        t("身份证号码", "ID_CARD", "ID Number", "公安机关为公民编制的唯一、终身不变的编码",
            "字符型", "AN18", 18, 0, "17位数字+1位校验码(0-9或X)", "", "必填", "证件", "符合GB 11643"),
        t("护照号码", "PASSPORT", "Passport Number", "护照证件号码",
            "字符型", "AN..20", 9, 0, "字母+数字组合", "", "", "证件", "E/G/D/S/P/H开头"),
        t("军官证号", "MILITARY_ID", "Military ID Number", "军官证号码",
            "字符型", "AN..18", 18, 0, "字母+数字组合", "", "", "证件", ""),
        t("港澳通行证", "HK_MACAU_PASS", "HK/Macau Pass", "港澳居民来往内地通行证号码",
            "字符型", "AN..11", 11, 0, "字母+数字组合", "", "", "证件", "H/M开头"),
        t("台湾通行证", "TAIWAN_PASS", "Taiwan Pass", "台湾居民来往大陆通行证号码",
            "字符型", "AN..8", 8, 0, "字母+数字组合", "", "", "证件", ""),
        t("社保卡号", "SOCIAL_CARD", "Social Security Card No.", "社会保障卡号码",
            "字符型", "AN18", 18, 0, "18位数字", "", "", "证件", "符合人社部标准"),
        t("医保卡号", "MEDICAL_CARD", "Medical Insurance Card No.", "医疗保险卡号码",
            "字符型", "AN18", 18, 0, "数字", "", "", "证件", ""),
        t("驾驶证号", "DRIVER_LICENSE", "Driver License No.", "机动车驾驶证号码",
            "字符型", "AN18", 18, 0, "与身份证号一致", "", "", "证件", "符合GA 482"),
        t("居住证号", "RESIDENCE_PERMIT", "Residence Permit No.", "居住证号码",
            "字符型", "AN18", 18, 0, "数字", "", "", "证件", ""),
        t("港澳居民身份证", "HK_MACAU_RESIDENT", "HK/Macau Resident ID", "港澳居民居住证号码",
            "字符型", "AN18", 18, 0, "8位数字+10位数字", "", "", "证件", "符合公安部标准"),
        t("台湾居民身份证", "TAIWAN_RESIDENT", "Taiwan Resident ID", "台湾居民居住证号码",
            "字符型", "AN18", 18, 0, "8位数字+10位数字", "", "", "证件", ""),

        // ==================== 二、个人信息类 ====================
        t("姓名", "NAME", "Name", "在户籍管理部门正式登记注册的本人姓氏和名称",
            "字符型", "AN..100", 50, 0, "中文或英文字符", "", "必填", "人员", ""),
        t("曾用名", "FORMER_NAME", "Former Name", "曾用名",
            "字符型", "AN..100", 50, 0, "中文字符", "", "", "人员", ""),
        t("性别", "GENDER", "Gender Code", "人的生理性别的代码表示",
            "字符型", "AN1", 1, 0, "0-未知,1-男,2-女,9-未说明", "", "必填", "人员", "符合GB/T 2261.1"),
        t("出生日期", "BIRTH_DATE", "Date of Birth", "出生证签署的出生年月日",
            "日期型", "YYYYMMDD", 10, 0, "YYYY-MM-DD格式", "", "必填", "人员", "符合GB/T 7408"),
        t("年龄", "AGE", "Age", "年龄（周岁）",
            "数值型", "N..3", 3, 0, "0-150", "岁", "", "人员", ""),
        t("民族", "ETHNICITY", "Ethnicity Code", "标识个人所属民族的代码",
            "字符型", "AN2", 2, 0, "01-汉族,02-蒙古族,03-回族...", "", "", "人员", "符合GB/T 3304"),
        t("国籍", "NATIONALITY", "Nationality Code", "标识个人所属国籍的代码",
            "字符型", "AN3", 3, 0, "CHN-中国,USA-美国...", "", "", "人员", "符合GB/T 2659"),
        t("籍贯", "NATIVE_PLACE", "Native Place", "籍贯",
            "字符型", "AN..100", 100, 0, "省+市/县", "", "", "人员", ""),
        t("出生地", "BIRTH_PLACE", "Birth Place", "出生地",
            "字符型", "AN..100", 100, 0, "省+市/县", "", "", "人员", ""),
        t("政治面貌", "POLITICAL_STATUS", "Political Status", "政治面貌代码",
            "字符型", "AN2", 2, 0, "01-中共党员,02-中共预备党员,03-共青团员...", "", "", "人员", "符合GB/T 4762"),
        t("婚姻状况", "MARITAL_STATUS", "Marital Status Code", "个人当前婚姻状况的代码",
            "字符型", "AN1", 1, 0, "1-未婚,2-已婚,3-丧偶,4-离婚,9-其他", "", "", "人员", "符合GB/T 4766"),
        t("健康状况", "HEALTH_STATUS", "Health Status", "健康状况代码",
            "字符型", "AN1", 1, 0, "1-健康,2-一般,3-较弱,4-有疾病", "", "", "人员", "符合GB/T 4767"),
        t("血型", "BLOOD_TYPE", "Blood Type", "血型代码",
            "字符型", "AN1", 1, 0, "A,B,O,AB", "", "", "人员", ""),
        t("学历", "EDUCATION", "Education Code", "个人所受的最高学历教育的代码",
            "字符型", "AN2", 2, 0, "10-研究生,20-大学本科,30-大学专科...", "", "", "人员", "符合GB/T 4658"),
        t("学位", "DEGREE", "Degree Code", "学位代码",
            "字符型", "AN2", 2, 0, "1-博士,2-硕士,3-学士", "", "", "人员", "符合GB/T 6864"),
        t("毕业院校", "GRADUATE_SCHOOL", "Graduate School", "毕业院校",
            "字符型", "AN..200", 100, 0, "学校全称", "", "", "人员", ""),
        t("专业", "MAJOR", "Major", "所学专业",
            "字符型", "AN..200", 100, 0, "专业名称", "", "", "人员", "符合GB/T 13745"),
        t("职业", "OCCUPATION", "Occupation Code", "职业代码",
            "字符型", "AN4", 4, 0, "符合GB/T 6565", "", "", "人员", "符合GB/T 6565"),
        t("职务", "POSITION", "Position", "职务",
            "字符型", "AN..100", 50, 0, "职务名称", "", "", "人员", ""),
        t("职称", "TITLE", "Professional Title", "专业技术职称",
            "字符型", "AN..40", 20, 0, "初级/中级/副高/正高", "", "", "人员", "符合GB/T 8561"),
        t("工作单位", "WORK_UNIT", "Work Unit", "工作单位",
            "字符型", "AN..200", 200, 0, "单位全称", "", "", "人员", ""),
        t("工号", "EMPLOYEE_NO", "Employee No.", "员工编号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "人员", ""),

        // ==================== 三、联系方式类 ====================
        t("手机号码", "MOBILE", "Mobile Number", "移动电话号码",
            "字符型", "AN11", 11, 0, "以1开头的11位数字", "", "必填", "人员", "符合工信部编号规则"),
        t("固定电话", "PHONE", "Telephone", "固定电话号码",
            "字符型", "AN..20", 20, 0, "区号-号码或手机号", "", "", "人员", "如025-88888888"),
        t("电子邮箱", "EMAIL", "Email Address", "用于电子邮件通信的地址",
            "字符型", "AN..100", 100, 0, "标准邮箱格式", "", "", "人员", "符合RFC 5322"),
        t("邮政编码", "ZIP_CODE", "Postal Code", "邮政部门编制的投递邮件的区域编码",
            "字符型", "AN6", 6, 0, "6位数字", "", "", "地理", "符合GB/T 2260"),
        t("通讯地址", "MAILING_ADDRESS", "Mailing Address", "通讯地址",
            "字符型", "AN..500", 200, 0, "省+市+区+街道+门牌号", "", "必填", "地理", ""),
        t("户籍地址", "REGISTERED_ADDRESS", "Registered Address", "户籍地址",
            "字符型", "AN..500", 200, 0, "省+市+区+街道+门牌号", "", "", "地理", ""),
        t("地址", "ADDRESS", "Detailed Address", "详细地址",
            "字符型", "AN..500", 200, 0, "省+市+区+街道+门牌号", "", "必填", "地理", ""),

        // ==================== 四、组织机构类 ====================
        t("统一社会信用代码", "USCC", "Unified Social Credit Code", "法人和其他组织的统一社会信用代码",
            "字符型", "AN18", 18, 0, "18位字母数字组合", "", "必填", "机构", "符合GB 32100"),
        t("组织机构代码", "ORG_CODE", "Organization Code", "组织机构代码",
            "字符型", "AN9", 9, 0, "8位字符+1位校验码", "", "", "机构", "符合GB 11714"),
        t("企业名称", "COMPANY_NAME", "Enterprise Name", "企业在工商行政管理部门登记注册的名称",
            "字符型", "AN..300", 200, 0, "工商注册全称", "", "必填", "机构", ""),
        t("法定代表人", "LEGAL_PERSON", "Legal Representative", "法定代表人姓名",
            "字符型", "AN..100", 50, 0, "中文姓名", "", "必填", "机构", ""),
        t("注册地址", "REG_ADDRESS", "Registered Address", "工商注册地址",
            "字符型", "AN..500", 200, 0, "工商注册地址", "", "", "机构", ""),
        t("注册资本", "REG_CAPITAL", "Registered Capital", "企业注册登记的资本金额",
            "数值型", "N..16,2", 14, 2, "≥0", "万元", "", "机构", ""),
        t("成立日期", "ESTABLISH_DATE", "Establishment Date", "成立日期",
            "日期型", "YYYYMMDD", 10, 0, "YYYY-MM-DD格式", "", "", "机构", ""),
        t("经营期限", "OPERATE_PERIOD", "Operating Period", "经营期限",
            "字符型", "AN..100", 50, 0, "起止日期或长期", "", "", "机构", ""),
        t("经营范围", "BUSINESS_SCOPE", "Business Scope", "经营范围",
            "字符型", "AN..2000", 2000, 0, "工商登记经营范围", "", "", "机构", ""),
        t("行业分类", "INDUSTRY_CODE", "Industry Code", "国民经济行业分类的代码标识",
            "字符型", "AN4", 4, 0, "符合GB/T 4754", "", "", "机构", "符合GB/T 4754"),
        t("企业类型", "COMPANY_TYPE", "Company Type Code", "企业类型代码",
            "字符型", "AN2", 2, 0, "11-国企,12-集体,13-私营,14-外资...", "", "", "机构", ""),
        t("纳税人识别号", "TAX_ID", "Tax Identification No.", "纳税人识别号",
            "字符型", "AN..20", 20, 0, "与统一社会信用代码一致", "", "", "机构", ""),
        t("医疗机构代码", "MEDICAL_ORG_CODE", "Medical Institution Code", "医疗机构代码",
            "字符型", "AN22", 22, 0, "22位编码", "", "", "机构", "符合WS 218"),

        // ==================== 五、金融财税类 ====================
        t("银行卡号", "BANK_CARD", "Bank Card No.", "银行卡号",
            "字符型", "AN..19", 19, 0, "16-19位数字", "", "", "经济", "符合Luhn算法校验"),
        t("开户银行", "BANK_NAME", "Bank Name", "开户银行名称",
            "字符型", "AN..200", 100, 0, "银行全称", "", "", "经济", ""),
        t("银行账号", "BANK_ACCOUNT", "Bank Account", "在银行开立的账户号码",
            "字符型", "AN..30", 30, 0, "数字", "", "", "经济", ""),
        t("税务登记号", "TAX_REG_NO", "Tax Registration No.", "税务登记号",
            "字符型", "AN..20", 20, 0, "数字", "", "", "经济", ""),
        t("发票代码", "INVOICE_CODE", "Invoice Code", "发票代码",
            "字符型", "AN12", 12, 0, "10-12位数字", "", "", "经济", ""),
        t("发票号码", "INVOICE_NO", "Invoice No.", "发票号码",
            "字符型", "AN8", 8, 0, "8位数字", "", "", "经济", ""),
        t("金额", "AMOUNT", "Amount", "以人民币元为单位的金额数值",
            "数值型", "N..16,2", 14, 2, "≥0", "元", "", "经济", "精确到分"),
        t("税率", "TAX_RATE", "Tax Rate", "应纳税额占征税对象金额的比率",
            "数值型", "N..5,4", 5, 4, "0-1", "", "", "经济", ""),
        t("价格", "PRICE", "Price", "价格",
            "数值型", "N..16,2", 14, 2, "≥0", "元", "", "经济", ""),
        t("数量", "QUANTITY", "Quantity", "数量",
            "数值型", "N..12,2", 12, 2, "≥0", "", "", "经济", ""),

        // ==================== 六、政务通用类 ====================
        t("行政区划代码", "AREA_CODE", "Administrative Division Code", "表示县级及县级以上行政区划的代码",
            "字符型", "AN6", 6, 0, "6位数字", "", "", "地理", "符合GB/T 2260"),
        t("项目编号", "PROJECT_NO", "Project No.", "项目编号",
            "字符型", "AN..30", 30, 0, "字母+数字", "", "", "人员", ""),
        t("批文号", "APPROVAL_NO", "Approval No.", "批文号/批复文号",
            "字符型", "AN..50", 50, 0, "年份+序号", "", "", "人员", ""),
        t("许可证号", "LICENSE_NO", "License No.", "许可证号码",
            "字符型", "AN..50", 50, 0, "字母+数字", "", "", "证件", ""),
        t("案件编号", "CASE_NO", "Case No.", "案件编号",
            "字符型", "AN..30", 30, 0, "年份+类型+序号", "", "", "人员", ""),
        t("公文编号", "DOC_NO", "Document No.", "公文编号/文号",
            "字符型", "AN..50", 50, 0, "机关代字+年份+序号", "", "", "人员", "如国发〔2024〕1号"),
        t("文号", "DOCUMENT_NO", "Document Reference", "文号",
            "字符型", "AN..50", 50, 0, "机关代字+年份+序号", "", "", "人员", ""),
        t("流水号", "SERIAL_NO", "Serial No.", "流水号",
            "字符型", "AN..30", 30, 0, "递增数字", "", "", "人员", ""),
        t("序号", "SEQUENCE_NO", "Sequence No.", "序号",
            "数值型", "N..10", 10, 0, "≥1", "", "", "人员", ""),
        t("编码", "CODE", "Code", "通用编码",
            "字符型", "AN..30", 30, 0, "字母+数字", "", "", "人员", ""),
        t("状态", "STATUS", "Status Code", "状态码",
            "字符型", "AN2", 2, 0, "01-有效,02-无效,03-待审核...", "", "", "人员", ""),
        t("标志", "FLAG", "Flag", "标志位",
            "字符型", "AN1", 1, 0, "0-否,1-是", "", "", "人员", ""),
        t("等级", "LEVEL", "Level", "等级",
            "字符型", "AN2", 2, 0, "01-一级,02-二级...", "", "", "人员", ""),
        t("分数", "SCORE", "Score", "分数",
            "数值型", "N..6,2", 6, 2, "0-100", "分", "", "人员", ""),
        t("百分比", "PERCENTAGE", "Percentage", "百分比",
            "数值型", "N..5,2", 5, 2, "0-100", "%", "", "人员", ""),

        // ==================== 七、医疗卫生类 ====================
        t("病历号", "MEDICAL_RECORD_NO", "Medical Record No.", "病历号",
            "字符型", "AN..20", 20, 0, "数字或字母+数字", "", "", "人员", ""),
        t("诊断编码", "DIAGNOSIS_CODE", "Diagnosis Code (ICD-10)", "诊断编码（ICD-10）",
            "字符型", "AN..10", 10, 0, "ICD-10编码格式", "", "", "人员", "符合ICD-10"),
        t("药品编码", "DRUG_CODE", "Drug Code", "药品编码",
            "字符型", "AN..20", 20, 0, "国药准字+字母+数字", "", "", "人员", "符合国药准字编码规则"),
        t("处方号", "PRESCRIPTION_NO", "Prescription No.", "处方号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "人员", ""),

        // ==================== 八、教育类 ====================
        t("学籍号", "STUDENT_ID", "Student ID", "学籍号",
            "字符型", "AN19", 19, 0, "G+身份证号或L+临时学籍号", "", "", "人员", "符合教育部标准"),
        t("考生号", "EXAM_NO", "Exam No.", "考生号",
            "字符型", "AN14", 14, 0, "14位数字", "", "", "人员", ""),
        t("准考证号", "ADMISSION_TICKET", "Admission Ticket No.", "准考证号",
            "字符型", "AN..16", 16, 0, "数字", "", "", "证件", ""),
        t("毕业证号", "DIPLOMA_NO", "Diploma No.", "毕业证书编号",
            "字符型", "AN18", 18, 0, "数字", "", "", "证件", ""),
        t("学位证号", "DEGREE_CERT_NO", "Degree Certificate No.", "学位证书编号",
            "字符型", "AN16", 16, 0, "数字", "", "", "证件", ""),
        t("学校代码", "SCHOOL_CODE", "School Code", "学校代码",
            "字符型", "AN10", 10, 0, "数字", "", "", "机构", "符合教育部学校代码"),
        t("专业代码", "MAJOR_CODE", "Major Code", "专业代码",
            "字符型", "AN6", 6, 0, "数字", "", "", "人员", "符合GB/T 13745"),

        // ==================== 九、不动产类 ====================
        t("不动产证号", "REALESTATE_CERT", "Real Estate Certificate No.", "不动产权证书号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "证件", ""),
        t("土地证号", "LAND_CERT", "Land Certificate No.", "土地使用权证号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "证件", ""),
        t("房产证号", "HOUSE_CERT", "House Certificate No.", "房屋所有权证号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "证件", ""),
        t("不动产单元号", "REALESTATE_UNIT", "Real Estate Unit No.", "不动产单元号",
            "字符型", "AN28", 28, 0, "28位编码", "", "", "证件", "符合GB/T 26424"),
        t("宗地号", "PARCEL_NO", "Parcel No.", "宗地号",
            "字符型", "AN..20", 20, 0, "数字", "", "", "地理", ""),

        // ==================== 十、车辆交通类 ====================
        t("车牌号", "PLATE_NUMBER", "License Plate No.", "机动车号牌号码",
            "字符型", "AN..8", 8, 0, "汉字+字母+数字", "", "", "人员", "符合GA 36"),
        t("车架号", "VIN", "VIN", "车辆识别代号（VIN）",
            "字符型", "AN17", 17, 0, "17位字母+数字", "", "", "人员", "符合GB 16735"),
        t("发动机号", "ENGINE_NO", "Engine No.", "发动机号码",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "人员", ""),
        t("行驶证号", "DRIVING_CERT", "Driving Certificate No.", "机动车行驶证号",
            "字符型", "AN..20", 20, 0, "数字", "", "", "证件", ""),
        t("运输证号", "TRANSPORT_CERT", "Transport Certificate No.", "道路运输证号",
            "字符型", "AN..20", 20, 0, "字母+数字", "", "", "证件", ""),

        // ==================== 十一、时间日期类 ====================
        t("日期", "DATE", "Date", "日期",
            "日期型", "YYYYMMDD", 10, 0, "YYYY-MM-DD格式", "", "", "时间", "符合GB/T 7408"),
        t("时间", "TIME", "Time", "时间",
            "字符型", "HHmmss", 8, 0, "HH:mm:ss格式", "", "", "时间", "符合GB/T 7408"),
        t("时间戳", "TIMESTAMP", "DateTime", "日期时间",
            "字符型", "YYYYMMDDHHmmss", 19, 0, "YYYY-MM-DD HH:mm:ss", "", "", "时间", "符合GB/T 7408"),
        t("年份", "YEAR", "Year", "年份",
            "字符型", "AN4", 4, 0, "YYYY格式", "", "", "时间", ""),
        t("月份", "MONTH", "Month", "月份",
            "字符型", "AN7", 7, 0, "YYYY-MM格式", "", "", "时间", ""),

        // ==================== 十二、网络信息类 ====================
        t("IP地址", "IP_ADDRESS", "IP Address", "IP地址",
            "字符型", "AN..45", 45, 0, "IPv4或IPv6格式", "", "", "人员", "如192.168.1.1"),
        t("MAC地址", "MAC_ADDRESS", "MAC Address", "MAC地址",
            "字符型", "AN17", 17, 0, "XX:XX:XX:XX:XX:XX", "", "", "人员", ""),
        t("URL地址", "URL", "URL", "URL地址",
            "字符型", "AN..2000", 2000, 0, "http/https格式", "", "", "人员", ""),
        t("域名", "DOMAIN", "Domain Name", "域名",
            "字符型", "AN..253", 253, 0, "标准域名格式", "", "", "人员", "符合RFC 1035"),

        // ==================== 十三、布尔/通用类型 ====================
        t("布尔值", "BOOLEAN", "Boolean", "布尔类型",
            "布尔型", "AN1", 1, 0, "0-否, 1-是", "", "", "人员", ""),
        t("整数", "INTEGER", "Integer", "整数数值",
            "数值型", "N..10", 10, 0, "", "", "", "人员", ""),
        t("浮点数", "FLOAT", "Float", "浮点数值",
            "数值型", "N..12,2", 12, 2, "", "", "", "人员", ""),
        t("备注", "REMARK", "Remark", "备注",
            "字符型", "AN..500", 500, 0, "文本", "", "", "人员", ""),
        t("描述", "DESCRIPTION", "Description", "描述",
            "字符型", "AN..2000", 2000, 0, "文本", "", "", "人员", ""),
        t("名称", "NAME_FIELD", "Name", "名称",
            "字符型", "AN..200", 200, 0, "文本", "", "", "人员", ""),
        t("编号", "NO", "Number", "编号",
            "字符型", "AN..30", 30, 0, "字母+数字", "", "", "人员", "")
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
                de.setCnName(name);
            }
            return de;
        }

        return buildGeneric(name, explicitLength, dataTypeHint, precision);
    }

    private DataElement buildGeneric(String name, int length, String dataType, int precision) {
        if (dataType == null) {
            dataType = "字符型";
        }
        if (length <= 0) {
            length = guessLength(dataType);
        }
        DataElement de = new DataElement(name, "DE_" + Math.abs(name.hashCode()) % 10000,
            name, dataType, length, precision, "", "", "", "");
        de.setEnName(name);
        de.setClassification("人员");
        de.setRepresentation(switch (dataType) {
            case "字符型" -> "文本";
            case "数值型" -> "数字";
            case "日期型" -> "日期";
            case "布尔型" -> "布尔";
            default -> "文本";
        });
        return de;
    }

    private DataElement parseGeneric(String input) {
        return buildGeneric(input, -1, extractDataType(input), extractPrecision(input));
    }

    private String extractDataType(String input) {
        if (input.contains("整数") || input.matches("(?i).*\\bint(eger)?\\b.*")) return "数值型";
        if (input.contains("浮点") || input.contains("小数") || input.matches("(?i).*\\b(float|double|decimal|number)\\b.*")) return "数值型";
        if (input.contains("日期时间") || input.contains("时间戳") || input.matches("(?i).*\\bdatetime\\b.*")) return "日期型";
        if (input.contains("日期") || input.matches("(?i).*\\bdate\\b.*")) return "日期型";
        if (input.contains("时间") || input.matches("(?i).*\\btime\\b.*")) return "字符型";
        if (input.contains("布尔") || input.matches("(?i).*\\bbool(ean)?\\b.*")) return "布尔型";
        if (input.contains("字符串") || input.contains("文本") || input.matches("(?i).*\\b(varchar|char|text|string)\\b.*")) return "字符型";
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
            case "数值型" -> 12;
            case "日期型" -> 10;
            case "布尔型" -> 1;
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

    private static DataElementTemplate t(String name, String id, String enName, String def,
                                          String dataType, String format, int len, int prec,
                                          String range, String unit, String constraint,
                                          String classification, String remark) {
        return new DataElementTemplate(name, id, enName, def, dataType, format, len, prec,
            range, unit, constraint, classification, remark);
    }

    private static class DataElementTemplate {
        final String name, identifier, enName, definition, dataType, format;
        final String valueRange, unit, constraint, classification, remark;
        final int length, precision;

        DataElementTemplate(String name, String identifier, String enName, String definition,
                            String dataType, String format, int length, int precision,
                            String valueRange, String unit, String constraint,
                            String classification, String remark) {
            this.name = name; this.identifier = identifier; this.enName = enName;
            this.definition = definition; this.dataType = dataType; this.format = format;
            this.length = length; this.precision = precision; this.valueRange = valueRange;
            this.unit = unit; this.constraint = constraint; this.classification = classification;
            this.remark = remark;
        }

        DataElement toDataElement() {
            DataElement de = new DataElement(name, identifier, definition, dataType, length,
                precision, valueRange, unit, constraint, remark);
            de.setCnName(name);
            de.setEnName(enName);
            de.setFormat(format);
            de.setClassification(classification);
            de.setRepresentation(switch (dataType) {
                case "字符型" -> "文本";
                case "数值型" -> "数字";
                case "日期型" -> "日期";
                case "布尔型" -> "布尔";
                default -> "文本";
            });
            de.setRegAuthority("国务院电子政务办公室");
            de.setRegStatus("现行有效");
            de.setVersion("1.0");
            return de;
        }
    }
}
