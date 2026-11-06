<template>
  <div class="ocr-confirm">
    <div class="ocr-header">
      <el-button @click="$router.back()">返回</el-button>
      <h2>OCR识别结果确认</h2>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p>识别中，请稍候...</p>
      <el-progress :percentage="progressPercent" :stroke-width="12" />
    </div>

    <!-- Success State -->
    <template v-else-if="task?.status === 'SUCCESS'">
      <div class="ocr-workspace">
        <el-row :gutter="20">
          <!-- Left: OCR Result JSON Editor -->
          <el-col :span="12">
            <el-card shadow="never" class="result-card">
              <template #header>
                <span>识别结果</span>
                <span
                  :class="['confidence-badge', confidence >= 0.85 ? 'conf-high' : 'conf-low']"
                >
                  置信度: {{ (confidence * 100).toFixed(1) }}%
                </span>
              </template>
              <div v-if="confidence < 0.85" class="confirm-warning">
                <el-icon color="#E6A23C"><WarningFilled /></el-icon>
                <span>识别置信度较低，需人工确认</span>
              </div>
              <el-form label-width="100px">
                <el-form-item
                  v-for="(value, key) in editableResult"
                  :key="key"
                  :label="key"
                >
                  <el-input v-model="editableResult[key]" />
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <!-- Right: Original Image -->
          <el-col :span="12">
            <el-card shadow="never" class="image-card">
              <template #header><span>原始图片</span></template>
              <el-image
                v-if="task.imageUrl"
                :src="task.imageUrl"
                fit="contain"
                style="width: 100%;"
                :preview-src-list="[task.imageUrl]"
              />
              <el-empty v-else description="暂无图片" />
            </el-card>
          </el-col>
        </el-row>

        <!-- Confirm Button -->
        <div class="action-bar">
          <el-button type="primary" :loading="confirming" @click="handleConfirm">
            确认入库
          </el-button>
          <el-button @click="$router.back()">返回修改</el-button>
        </div>
      </div>
    </template>

    <!-- Failed/Degraded State -->
    <template v-else-if="task?.status === 'FAILED' || task?.status === 'DEGRADED'">
      <el-result icon="error" title="识别失败" :sub-title="task.errorMessage || 'OCR识别未能完成，请尝试手动录入。'">
        <template #extra>
          <el-button type="primary" @click="handleManual">手动录入</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </template>
      </el-result>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, WarningFilled } from '@element-plus/icons-vue'
import api from '@/api/request'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const confirming = ref(false)
const task = ref(null)
const editableResult = reactive({})
const progressPercent = ref(0)
let pollTimer = null

const confidence = computed(() => {
  return task.value?.confidence ?? 0
})

async function fetchTask() {
  try {
    const res = await api.get(`/ai/ocr/task/${route.params.taskId}`)
    task.value = res.data
    if (res.data?.status === 'SUCCESS') {
      const result = res.data.result || {}
      Object.keys(editableResult).forEach(k => delete editableResult[k])
      Object.entries(result).forEach(([k, v]) => {
        editableResult[k] = v
      })
      loading.value = false
      stopPolling()
    } else if (res.data?.status === 'FAILED' || res.data?.status === 'DEGRADED') {
      loading.value = false
      stopPolling()
    } else {
      // Still processing - keep polling
      progressPercent.value = Math.min(progressPercent.value + 5, 90)
    }
  } catch (e) {
    loading.value = false
    stopPolling()
    ElMessage.error('获取OCR任务失败')
  }
}

function startPolling() {
  fetchTask()
  pollTimer = setInterval(() => {
    fetchTask()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleConfirm() {
  confirming.value = true
  try {
    await api.post(`/ai/ocr/task/${route.params.taskId}/confirm`, {
      correctedResult: { ...editableResult }
    })
    ElMessage.success('入库成功')
    router.back()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '入库失败')
  } finally {
    confirming.value = false
  }
}

function handleManual() {
  ElMessage.info('跳转到手动录入页面')
  // router.push(`/escort/manual/${route.params.taskId}`)
}

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.ocr-confirm {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.ocr-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.ocr-header h2 {
  margin: 0;
  font-size: 20px;
}
.loading-state {
  text-align: center;
  padding: 80px 20px;
}
.loading-state p {
  margin: 16px 0;
  font-size: 16px;
  color: #666;
}
.ocr-workspace {
  margin-top: 16px;
}
.result-card,
.image-card {
  height: 100%;
}
.confidence-badge {
  margin-left: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
}
.conf-high {
  background: #e1f3d8;
  color: #67C23A;
}
.conf-low {
  background: #fef0f0;
  color: #E6A23C;
}
.confirm-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  margin-bottom: 12px;
  color: #E6A23C;
  font-size: 14px;
}
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>