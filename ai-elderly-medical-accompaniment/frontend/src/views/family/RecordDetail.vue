<template>
  <div class="record-detail-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">就医详情</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="detail-card" v-if="record">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">就诊人</span>
          <span class="value">{{ desensitizedName(record.elderlyName) }}</span>
        </div>
        <div class="info-item">
          <span class="label">就诊日期</span>
          <span class="value">{{ record.visitDate }}</span>
        </div>
        <div class="info-item">
          <span class="label">就诊医院</span>
          <span class="value">{{ record.hospitalName }}</span>
        </div>
        <div class="info-item">
          <span class="label">就诊科室</span>
          <span class="value">{{ record.department }}</span>
        </div>
      </div>

      <div class="section-block">
        <h3 class="block-title">诊断结果</h3>
        <p class="block-text">{{ record.diagnosis || '-' }}</p>
      </div>

      <div class="section-block">
        <h3 class="block-title">处方信息</h3>
        <p class="block-text">{{ record.prescription || '-' }}</p>
      </div>
    </div>

    <div class="documents-card" v-if="documents.length > 0">
      <h2 class="section-title">相关单据</h2>
      <div class="doc-list">
        <div v-for="doc in documents" :key="doc.id" class="doc-item">
          <div class="doc-thumb">
            <el-image
              v-if="doc.url"
              :src="doc.url"
              fit="cover"
              class="thumb-img"
              :preview-src-list="[doc.url]"
              preview-teleported
            />
            <el-icon v-else :size="32"><Picture /></el-icon>
          </div>
          <div class="doc-info">
            <span class="doc-name">{{ doc.name || '单据' }}</span>
            <el-tag :type="doc.ocrStatus === 'SUCCESS' ? 'success' : 'warning'" size="large">
              {{ doc.ocrStatus === 'SUCCESS' ? '已识别' : '识别中' }}
            </el-tag>
            <span class="ocr-result" v-if="doc.ocrStatus === 'SUCCESS' && doc.ocrResult">
              {{ doc.ocrResult }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="action-area">
      <el-button
        type="primary"
        class="explain-btn"
        :loading="generating"
        @click="handleGenerateExplain"
      >
        生成医嘱解读
      </el-button>

      <el-button
        class="ask-ai-btn"
        @click="handleAskAi"
      >
        <el-icon :size="20"><ChatDotRound /></el-icon>
        追问AI
      </el-button>

      <div class="ai-explain" v-if="explanationContent">
        <div class="disclaimer-box">
          <el-icon :size="20" color="#E6A23C"><WarningFilled /></el-icon>
          <span>AI生成内容仅供参考，不构成任何诊疗建议</span>
        </div>
        <div class="explain-text" v-html="explanationContent"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Picture, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'
import { useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const record = ref(null)
const documents = ref([])
const generating = ref(false)
const explanationContent = ref('')

const desensitizedName = (name) => {
  if (!name) return '-'
  if (name.length <= 1) return name
  if (name.length === 2) return name.charAt(0) + '*'
  return name.charAt(0) + '*'.repeat(name.length - 2) + name.charAt(name.length - 1)
}

const loadRecord = async () => {
  try {
    const res = await api.get(`/record/${route.params.id}`)
    if (res.code === 200 && res.data) {
      record.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载就医档案失败')
  }
}

const loadDocuments = async () => {
  try {
    const res = await api.get(`/record/${route.params.id}/documents`)
    if (res.code === 200 && res.data) {
      documents.value = Array.isArray(res.data) ? res.data : (res.data.records || [])
    }
  } catch (e) {
    console.error('Failed to load documents:', e)
  }
}

const handleGenerateExplain = async () => {
  generating.value = true
  try {
    const res = await api.post('/ai/explain/generate', {
      recordId: record.value.id
    })
    if (res.code === 200 && res.data) {
      const contentId = res.data.id || res.data
      const contentRes = await api.get(`/ai/content/${contentId}`)
      if (contentRes.code === 200 && contentRes.data) {
        explanationContent.value = contentRes.data.content || contentRes.data
        ElMessage.success('医嘱解读生成成功')
      }
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

const handleAskAi = () => {
  router.push(`/family/ai-chat/new?contextType=RECORD&contextId=${record.value.id}`)
}

onMounted(() => {
  loadRecord()
  loadDocuments()
})
</script>

<style scoped>
.record-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #333;
}

.detail-card {
  margin: 16px 20px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item .label {
  font-size: 18px;
  color: #666;
}

.info-item .value {
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.section-block {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.block-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 10px 0;
  color: #333;
}

.block-text {
  font-size: 18px;
  color: #555;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.documents-card {
  margin: 0 20px 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: #333;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.doc-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.doc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.doc-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.ocr-result {
  font-size: 16px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-area {
  margin: 0 20px;
}

.explain-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  border-radius: 12px;
}

.ask-ai-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  border-radius: 12px;
  background: #fff;
  border: 2px solid #2F5AA8;
  color: #2F5AA8;
  margin-top: 12px;
}

.ask-ai-btn:hover {
  background: #f0f4ff;
  color: #254a8a;
  border-color: #254a8a;
}

.ai-explain {
  margin-top: 20px;
}

.disclaimer-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #FEF0E6;
  border: 1px solid #F5DAB1;
  border-radius: 8px;
  font-size: 16px;
  color: #E6A23C;
  margin-bottom: 16px;
}

.explain-text {
  font-size: 18px;
  line-height: 1.8;
  color: #333;
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
}

:deep(.el-tag--large) {
  font-size: 16px;
}
</style>