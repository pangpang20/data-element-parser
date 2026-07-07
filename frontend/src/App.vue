<template>
  <div class="app-container">
    <div class="header">
      <h1>📊 数据元解析与治理规则生成</h1>
      <p>基于 GB/T 19488.1-2004 电子政务数据元标准 · 六性治理 · 多语言规则生成</p>
    </div>

    <!-- 输入区域 -->
    <div class="card">
      <div class="card-title">自然语言输入</div>
      <div class="input-group">
        <div class="input-field">
          <textarea
            v-model="inputText"
            placeholder="请输入数据元描述，例如：身份证号码18位"
            @keydown.ctrl.enter="handleParse"
          ></textarea>
          <div class="examples">
            示例：
            <span @click="fillExample('身份证号码18位')">身份证号码18位</span>
            <span @click="fillExample('手机号码11位')">手机号码11位</span>
            <span @click="fillExample('统一社会信用代码')">统一社会信用代码</span>
            <span @click="fillExample('车牌号')">车牌号</span>
            <span @click="fillExample('银行卡号')">银行卡号</span>
            <span @click="fillExample('学籍号')">学籍号</span>
            <span @click="fillExample('不动产单元号')">不动产单元号</span>
            <span @click="fillExample('金额浮点数12位2位小数')">金额浮点数12位2位小数</span>
            <span @click="fillExample('邮箱,手机号码11位,身份证号码18位')">多数据元(逗号分隔)</span>
          </div>
        </div>
        <button
          class="btn btn-primary"
          @click="handleParse"
          :disabled="loading || !inputText.trim()"
        >
          <span v-if="loading" class="loading"></span>
          数据元解析
        </button>
      </div>
    </div>

    <!-- 数据元结果 -->
    <div class="card" v-if="dataElements.length > 0">
      <div class="section-header">
        <div class="card-title">数据元信息（{{ dataElements.length }}项）- GB/T 19488.1-2004</div>
        <button
          class="btn btn-success"
          @click="handleGenerateRules"
          :disabled="ruleLoading"
        >
          <span v-if="ruleLoading" class="loading"></span>
          生成治理规则
        </button>
      </div>

      <table>
        <thead>
          <tr>
            <th>标识符</th>
            <th>中文名称</th>
            <th>英文名称</th>
            <th>数据类型</th>
            <th>数据格式</th>
            <th>分类</th>
            <th>注册状态</th>
            <th>值域</th>
            <th style="width:60px">详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(de, index) in dataElements" :key="index">
            <td><code>{{ de.identifier }}</code></td>
            <td><strong>{{ de.cnName || de.name }}</strong></td>
            <td style="font-size:12px;color:#999;font-family:monospace">{{ de.enName || '-' }}</td>
            <td>
              <span class="tag" :class="getDataTypeClass(de.dataType)" :title="de.dataType">
                {{ de.dataTypeCode || de.dataType }}
              </span>
            </td>
            <td><code style="font-size:12px">{{ de.format || '-' }}</code></td>
            <td>{{ de.classification || '-' }}</td>
            <td>
              <span class="tag" :class="getStatusClass(de.regStatus)">
                {{ de.regStatus || '-' }}
              </span>
            </td>
            <td style="max-width:200px;word-break:break-all;font-size:12px">{{ de.valueDomain || '-' }}</td>
            <td>
              <button class="btn btn-detail" @click="showDetail(de)">查看</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 治理规则结果 -->
    <div class="card" v-if="governanceRules.length > 0">
      <div class="card-title">治理规则（{{ governanceRules.length }}项）- 数据质量六性</div>

      <div class="rule-layout">
        <!-- 左侧：六性列表 -->
        <div class="dim-sidebar">
          <div
            v-for="dim in dimensions"
            :key="dim.key"
            class="dim-item"
            :class="{ active: activeDimension === dim.key, 'has-rules': getDimCount(dim.key) > 0 }"
            @click="activeDimension = dim.key"
          >
            <span class="dim-dot" :class="getDimensionClass(dim.key)"></span>
            <span class="dim-name">{{ dim.label }}</span>
            <span class="dim-count">{{ getDimCount(dim.key) }}</span>
          </div>
        </div>

        <!-- 右侧：规则表格 -->
        <div class="rule-detail">
          <template v-if="filteredRules.length > 0">
            <div class="rule-table-wrap">
              <table class="rule-table">
                <thead>
                  <tr>
                    <th style="width:180px">规则名称</th>
                    <th style="width:200px">正则表达式</th>
                    <th>OceanBase SQL</th>
                    <th style="width:200px">Java</th>
                    <th style="width:200px">Python</th>
                    <th style="width:70px">级别</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(rule, index) in filteredRules" :key="index">
                    <td>
                      <div class="rule-cell-name">{{ rule.ruleName }}</div>
                      <div class="rule-cell-desc">{{ rule.description }}</div>
                    </td>
                    <td><div class="sql-box" v-html="highlightRegex(rule.regexExpression)"></div></td>
                    <td><div class="sql-box" v-html="highlightSql(rule.sqlExpression)"></div></td>
                    <td><div class="sql-box" v-html="highlightJava(rule.javaCode)"></div></td>
                    <td><div class="sql-box" v-html="highlightPython(rule.pythonCode)"></div></td>
                    <td>
                      <span class="tag" :class="getSeverityClass(rule.severity)">{{ rule.severity }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <!-- 空状态 -->
          <div v-else class="empty-dim">
            <div class="empty-dim-icon">{{ getDimensionIcon(activeDimension) }}</div>
            <p>{{ activeDimension }} 暂无规则</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="card" v-if="!loading && !ruleLoading && dataElements.length === 0 && hasSearched">
      <div class="empty-state">
        <div class="icon">🔍</div>
        <p>未找到匹配的数据元，请尝试其他描述</p>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="detail-overlay" v-if="detailElement" @click.self="detailElement = null">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">数据元详情 - {{ detailElement.cnName || detailElement.name }}</div>
          <button class="detail-close" @click="detailElement = null">&times;</button>
        </div>
        <div class="detail-body">
          <div class="detail-grid">
            <template v-for="field in detailFields" :key="field.key">
              <div class="detail-label">{{ field.label }}</div>
              <div class="detail-value">{{ getDetailValue(field.key) || '—' }}</div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const inputText = ref('')
const dataElements = ref([])
const governanceRules = ref([])
const loading = ref(false)
const ruleLoading = ref(false)
const hasSearched = ref(false)
const copied = ref(false)
const detailElement = ref(null)

// 六性筛选
const activeDimension = ref('all')
const dimensions = [
  { key: 'all', label: '全部' },
  { key: '完整性', label: '完整性' },
  { key: '准确性', label: '准确性' },
  { key: '规范性', label: '规范性' },
  { key: '唯一性', label: '唯一性' },
  { key: '一致性', label: '一致性' },
  { key: '时效性', label: '时效性' },
]

// 详情字段
const detailFields = [
  { key: 'identifier', label: '标识符' },
  { key: 'internalId', label: '内部标识符' },
  { key: 'cnName', label: '中文名称' },
  { key: 'enName', label: '英文名称' },
  { key: 'synonym', label: '同义名称' },
  { key: 'keywords', label: '关键词' },
  { key: 'definition', label: '定义' },
  { key: 'description', label: '描述' },
  { key: 'representation', label: '表示类' },
  { key: 'dataType', label: '数据类型' },
  { key: 'format', label: '数据格式' },
  { key: 'unit', label: '计量单位' },
  { key: 'valueDomain', label: '值域' },
  { key: 'valueDomainMeaning', label: '值域含义' },
  { key: 'classification', label: '分类方案' },
  { key: 'relatedDE', label: '相关数据元' },
  { key: 'regAuthority', label: '注册机构' },
  { key: 'regStatus', label: '注册状态' },
  { key: 'submitOrg', label: '提交机构' },
  { key: 'version', label: '版本' },
  { key: 'remarks', label: '备注' },
]

const filteredRules = computed(() => {
  if (activeDimension.value === 'all') return governanceRules.value
  return governanceRules.value.filter(r => r.qualityDimension === activeDimension.value)
})

function getDimCount(dim) {
  if (dim === 'all') return governanceRules.value.length
  return governanceRules.value.filter(r => r.qualityDimension === dim).length
}

function getDetailValue(key) {
  if (!detailElement.value) return ''
  return detailElement.value[key] || ''
}

function fillExample(text) {
  inputText.value = text
}

async function handleParse() {
  if (!inputText.value.trim()) return

  loading.value = true
  governanceRules.value = []
  hasSearched.value = true

  try {
    const response = await fetch('/api/parse', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ input: inputText.value })
    })
    const result = await response.json()
    if (result.success) {
      dataElements.value = result.data
    } else {
      alert('解析失败：' + (result.message || '未知错误'))
    }
  } catch (err) {
    alert('请求失败，请确保后端服务已启动 (localhost:8081)\n' + err.message)
  } finally {
    loading.value = false
  }
}

async function handleGenerateRules() {
  if (dataElements.value.length === 0) return

  ruleLoading.value = true

  try {
    const response = await fetch('/api/generate-rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ dataElements: dataElements.value })
    })
    const result = await response.json()
    if (result.success) {
      governanceRules.value = result.data
    } else {
      alert('规则生成失败：' + (result.message || '未知错误'))
    }
  } catch (err) {
    alert('请求失败：' + err.message)
  } finally {
    ruleLoading.value = false
  }
}

function showDetail(de) {
  detailElement.value = de
}

// ========== 样式类 ==========

function getDataTypeClass(dataType) {
  if (!dataType) return ''
  if (dataType === 'C' || dataType.includes('字符')) return 'tag-string'
  if (dataType === 'N' || dataType.includes('数值') || dataType.includes('整数') || dataType.includes('浮点')) return 'tag-number'
  if (dataType === 'D' || dataType.includes('日期')) return 'tag-date'
  if (dataType === 'T' || dataType.includes('时间')) return 'tag-time'
  if (dataType === 'B' || dataType.includes('布尔')) return 'tag-bool'
  return ''
}

function getStatusClass(status) {
  if (status === '现行有效') return 'status-active'
  if (status === '编制中') return 'status-draft'
  if (status === '审核中') return 'status-review'
  if (status === '废止') return 'status-retired'
  return ''
}

function getDimensionClass(dim) {
  const map = {
    'all': '',
    '唯一性': 'dim-uniqueness',
    '完整性': 'dim-completeness',
    '准确性': 'dim-accuracy',
    '一致性': 'dim-consistency',
    '时效性': 'dim-timeliness',
    '规范性': 'dim-compliance',
  }
  return map[dim] || ''
}

function getDimensionIcon(dim) {
  const map = {
    '完整性': '🔗',
    '准确性': '🎯',
    '规范性': '📐',
    '唯一性': '🔑',
    '一致性': '🔄',
    '时效性': '⏰',
  }
  return map[dim] || '📋'
}

function getSeverityClass(severity) {
  if (severity === 'ERROR') return 'tag-error'
  if (severity === 'WARNING') return 'tag-warning'
  return 'tag-info'
}

// ========== 语法高亮 ==========

function highlightSql(sql) {
  if (!sql) return '<span style="color:#999">无</span>'
  let h = sql
    .replace(/\b(LENGTH|TRIM|ABS|FLOOR|CAST|AS|CHAR|SUBSTRING_INDEX|REGEXP|IN|AND|OR|NOT)\b/gi,
      '<span class="sql-keyword">$1</span>')
    .replace(/\b(\d+)\b/g, '<span class="sql-number">$1</span>')
    .replace(/'([^']*)'/g, '<span class="sql-string">\'$1\'</span>')
  h = h.replace(/\{(\w+)\}/g, '<span style="color:#9cdcfe;font-weight:bold">{$1}</span>')
  return h
}

function highlightRegex(regex) {
  if (!regex) return '<span style="color:#999">无</span>'
  return '<span class="sql-string">' + escapeHtml(regex) + '</span>'
}

function highlightJava(code) {
  if (!code) return '<span style="color:#999">无</span>'
  let h = escapeHtml(code)
    .replace(/\b(Pattern|matches|String|boolean)\b/g, '<span class="sql-keyword">$1</span>')
    .replace(/"([^"]*)"/g, '<span class="sql-string">"$1"</span>')
  return h
}

function highlightPython(code) {
  if (!code) return '<span style="color:#999">无</span>'
  let h = escapeHtml(code)
    .replace(/\b(re|match)\b/g, '<span class="sql-keyword">$1</span>')
    .replace(/(r'[^']*')/g, '<span class="sql-string">$1</span>')
  return h
}

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

// ========== 复制功能 ==========

function copyAll() {
  if (filteredRules.value.length === 0) return

  const text = filteredRules.value.map(r => {
    const parts = [`-- ${r.ruleName} - ${r.description}`]
    if (r.sqlExpression) parts.push(`[SQL] ${r.sqlExpression}`)
    if (r.regexExpression) parts.push(`[Regex] ${r.regexExpression}`)
    if (r.javaCode) parts.push(`[Java] ${r.javaCode}`)
    if (r.pythonCode) parts.push(`[Python] ${r.pythonCode}`)
    return parts.join('\n')
  }).join('\n\n')

  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    }).catch(() => fallbackCopy(text))
  } else {
    fallbackCopy(text)
  }
}

function fallbackCopy(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch (err) {
    alert('复制失败，请手动复制')
  } finally {
    document.body.removeChild(textarea)
  }
}
</script>
