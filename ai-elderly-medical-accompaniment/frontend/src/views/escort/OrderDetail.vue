<template>
  <div class="order-detail">
    <div class="detail-header">
      <el-button @click="$router.back()" :icon="ArrowLeft">返回</el-button>
      <h2>订单详情</h2>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="order">
      <!-- Order Info -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span>订单信息</span>
          <el-tag :type="statusTagType(order.status)" style="margin-left: 12px;">
            {{ statusLabel(order.status) }}
          </el-tag>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="就诊人">{{ order.elderlyName }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ order.appointmentTime }}</el-descriptions-item>
          <el-descriptions-item label="就诊科室">{{ order.department }}</el-descriptions-item>
          <el-descriptions-item label="就诊医院">{{ order.hospital }}</el-descriptions-item>
          <el-descriptions-item label="服务城市">{{ order.serviceCity }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Status Timeline -->
      <el-card shadow="never" class="timeline-card">
        <template #header><span>状态时间线</span></template>
        <el-timeline>
          <el-timeline-item
            v-for="item in timeline"
            :key="item.status"
            :timestamp="item.time"
            :type="item.type"
          >
            {{ item.label }}
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- Action Buttons -->
      <div class="action-bar" v-if="order.status === 'ACCEPTED' || order.status === 'IN_SERVICE'">
        <el-button
          v-if="order.status === 'ACCEPTED'"
          type="primary"
          :loading="actioning"
          @click="handleStart"
        >
          开始服务
        </el-button>
        <el-button
          v-if="order.status === 'IN_SERVICE'"
          type="success"
          :loading="actioning"
          @click="handleFinish"
        >
          结束服务
        </el-button>
      </div>

      <!-- Document Upload (IN_SERVICE) -->
      <el-card shadow="never" class="upload-card" v-if="order.status === 'IN_SERVICE'">
        <template #header><span>上传医疗单据</span></template>
        <el-form label-width="100px">
          <el-form-item label="单据类型">
            <el-select v-model="docType" placeholder="请选择单据类型">
              <el-option label="检验单" value="LAB_REPORT" />
              <el-option label="处方" value="PRESCRIPTION" />
              <el-option label="病历" value="MEDICAL_RECORD" />
            </el-select>
          </el-form-item>
          <el-form-item label="上传图片">
            <el-upload
              :action="uploadUrl"
              list-type="picture-card"
              :limit="1"
              :on-success="onDocUploadSuccess"
              :before-upload="beforeDocUpload"
              v-model:file-list="docFileList"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item v-if="uploadedDocUrl">
            <el-button type="primary" :loading="creating" @click="handleCreateRecord">
              创建记录并准备OCR
            </el-button>
          </el-form-item>
          <el-form-item v-if="documentId">
            <el-button type="success" :loading="ocrStarting" @click="handleStartOcr">
              发起OCR识别
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import api from '@/api/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const actioning = ref(false)
const docType = ref('LAB_REPORT')
const docFileList = ref([])
const uploadedDocUrl = ref('')
const documentId = ref(null)
const creating = ref(false)
const ocrStarting = ref(false)
const uploadUrl = '/file/upload'

function statusLabel(status) {
  const map = {
    PENDING_DISPATCH: '待派单',
    ACCEPTED: '已接单',
    IN_SERVICE: '服务中',
    SERVICE_FINISHED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

function statusTagType(status) {
  const map = {
    PENDING_DISPATCH: 'warning',
    ACCEPTED: 'primary',
    IN_SERVICE: '',
    SERVICE_FINISHED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

const timeline = computed(() => {
  if (!order.value) return []
  const items = []
  if (order.value.createTime) {
    items.push({ status: 'CREATED', label: '订单创建', time: order.value.createTime, type: 'primary' })
  }
  if (order.value.acceptTime) {
    items.push({ status: 'ACCEPTED', label: '陪诊员接单', time: order.value.acceptTime, type: 'success' })
  }
  if (order.value.startTime) {
    items.push({ status: 'IN_SERVICE', label: '开始服务', time: order.value.startTime, type: 'warning' })
  }
  if (order.value.finishTime) {
    items.push({ status: 'SERVICE_FINISHED', label: '服务结束', time: order.value.finishTime, type: 'success' })
  }
  return items
})

async function fetchOrder() {
  loading.value = true
  try {
    const res = await api.get(`/order/${route.params.id}`)
    order.value = res.data
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

async function handleStart() {
  try {
    await ElMessageBox.confirm('确认开始服务吗？', '开始服务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  actioning.value = true
  try {
    await api.post(`/order/${order.value.id}/start`)
    ElMessage.success('已开始服务')
    await fetchOrder()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

async function handleFinish() {
  try {
    await ElMessageBox.confirm('确认结束服务吗？', '结束服务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  actioning.value = true
  try {
    await api.post(`/order/${order.value.id}/finish`)
    ElMessage.success('服务已结束')
    await fetchOrder()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actioning.value = false
  }
}

function onDocUploadSuccess(response) {
  if (response?.data?.url) {
    uploadedDocUrl.value = response.data.url
  }
}

function beforeDocUpload(file) {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传 JPG/PNG 格式的图片')
  if (!isLt5M) ElMessage.error('图片大小不能超过 5MB')
  return isImage && isLt5M
}

async function handleCreateRecord() {
  if (!docType.value || !uploadedDocUrl.value) {
    ElMessage.warning('请选择单据类型并上传图片')
    return
  }
  creating.value = true
  try {
    const res = await api.post('/record/new', {
      orderId: order.value.id,
      docType: docType.value,
      imageUrl: uploadedDocUrl.value
    })
    documentId.value = res.data?.id || res.data
    ElMessage.success('记录创建成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '创建记录失败')
  } finally {
    creating.value = false
  }
}

async function handleStartOcr() {
  if (!documentId.value) {
    ElMessage.warning('请先创建记录')
    return
  }
  ocrStarting.value = true
  try {
    const res = await api.post('/ai/ocr/recognize', { documentId: documentId.value })
    const taskId = res.data?.taskId || res.data?.id
    ElMessage.success('OCR识别任务已启动')
    router.push(`/escort/ocr/${taskId}`)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || 'OCR启动失败')
  } finally {
    ocrStarting.value = false
  }
}

onMounted(() => {
  fetchOrder()
  poll = setInterval(fetchOrder, 5000)
})
onUnmounted(() => clearInterval(poll))
let poll
</script>

<style scoped>
.order-detail {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.detail-header h2 {
  margin: 0;
  font-size: 20px;
}
.loading-container {
  padding: 20px;
}
.info-card,
.timeline-card,
.upload-card {
  margin-bottom: 16px;
}
.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
}
</style>