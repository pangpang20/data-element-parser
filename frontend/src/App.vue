<template>
  <div class="app-container">
    <div class="header">
      <h1>📊 数据元解析与治理规则生成</h1>
      <p>基于江苏省地方标准 DB32/T 4124 · 支持 OceanBase 数据库</p>
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
        <div class="card-title">数据元信息（{{ dataElements.length }}项）</div>
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
            <th>名称</th>
            <th>标识符</th>
            <th>定义</th>
            <th>数据类型</th>
            <th>长度</th>
            <th>精度</th>
            <th>值域</th>
            <th>单位</th>
            <th>约束</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(de, index) in dataElements" :key="index">
            <td><strong>{{ de.name }}</strong></td>
            <td><code>{{ de.identifier }}</code></td>
            <td>{{ de.definition }}</td>
            <td>
              <span class="tag" :class="getDataTypeClass(de.dataType)" :title="de.dataType">
                {{ de.dataTypeCode || de.dataType }}
              </span>
            </td>
            <td>{{ de.length }}</td>
            <td>{{ de.precision || '-' }}</td>
            <td>{{ de.valueRange || '-' }}</td>
            <td>{{ de.unit || '-' }}</td>
            <td>{{ de.constraint || '-' }}</td>
            <td>{{ de.remark || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 治理规则结果 -->
    <div class="card" v-if="governanceRules.length > 0">
      <div class="card-title">治理规则（{{ governanceRules.length }}项）- OceanBase SQL</div>

      <div class="rule-card" v-for="(rule, index) in governanceRules" :key="index">
        <div class="rule-header">
          <span class="rule-name">{{ rule.ruleName }}</span>
          <span class="tag" :class="getSeverityClass(rule.severity)">
            {{ rule.severity }}
          </span>
        </div>
        <div class="rule-desc">{{ rule.description }}</div>
        <div class="sql-box" v-html="highlightSql(rule.sqlExpression)"></div>
      </div>

      <!-- 复制全部 SQL -->
      <div style="margin-top: 16px; text-align: right;">
        <button class="btn btn-primary" @click="copyAllSql" style="font-size: 12px; height: 32px; padding: 4px 16px;">
          {{ copied ? '✓ 已复制' : '复制全部 SQL' }}
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="card" v-if="!loading && !ruleLoading && dataElements.length === 0 && hasSearched">
      <div class="empty-state">
        <div class="icon">🔍</div>
        <p>未找到匹配的数据元，请尝试其他描述</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const inputText = ref('')
const dataElements = ref([])
const governanceRules = ref([])
const loading = ref(false)
const ruleLoading = ref(false)
const hasSearched = ref(false)
const copied = ref(false)

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

function getDataTypeClass(dataType) {
  if (!dataType) return ''
  // 支持标准代码和中文
  if (dataType === 'C' || dataType.includes('字符') || dataType.includes('字符串')) return 'tag-string'
  if (dataType === 'N' || dataType.includes('整数') || dataType.includes('浮点') || dataType.includes('小数')) return 'tag-number'
  if (dataType === 'D' || dataType.includes('日期')) return 'tag-date'
  if (dataType === 'T' || dataType.includes('时间')) return 'tag-time'
  if (dataType === 'B' || dataType.includes('布尔')) return 'tag-bool'
  return ''
}

function getSeverityClass(severity) {
  if (severity === 'ERROR') return 'tag-error'
  if (severity === 'WARNING') return 'tag-warning'
  return 'tag-info'
}

function highlightSql(sql) {
  if (!sql) return ''
  // 简单的 SQL 语法高亮
  let highlighted = sql
    .replace(/\b(LENGTH|TRIM|ABS|FLOOR|CAST|AS|CHAR|SUBSTRING_INDEX|REGEXP|IN|AND|OR|NOT)\b/gi,
      '<span class="sql-keyword">$1</span>')
    .replace(/\b(\d+)\b/g, '<span class="sql-number">$1</span>')
    .replace(/'([^']*)'/g, '<span class="sql-string">\'$1\'</span>')

  // 将 {COLUMN} 占位符高亮
  highlighted = highlighted.replace(/\{(\w+)\}/g, '<span style="color:#9cdcfe;font-weight:bold">{$1}</span>')

  return highlighted
}

function copyAllSql() {
  if (governanceRules.value.length === 0) return

  const allSql = governanceRules.value
    .map(r => `-- ${r.ruleName}\n-- ${r.description}\n${r.sqlExpression};\n`)
    .join('\n')

  // 优先使用 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(allSql).then(() => {
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    }).catch(() => {
      fallbackCopy(allSql)
    })
  } else {
    fallbackCopy(allSql)
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
