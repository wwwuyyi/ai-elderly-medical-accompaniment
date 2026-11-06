<template>
  <div class="admin-audit">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
    <el-button @click="$router.push('/admin/dashboard')">← 返回看板</el-button>
    <h2 style="margin:0">资质审核</h2>
  </div>

    <!-- Filter Tabs -->
    <div class="filter-tabs">
      <el-radio-group v-model="statusFilter" @change="fetchData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Table -->
    <el-table
      :data="records"
      v-loading="loading"
      stripe
      style="width: 100%; margin-top: 16px;"
      @row-click="showDetail"
    >
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="serviceCity" label="服务城市" width="120" />
      <el-table-column label="审核状态" width="120">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)">
            {{ auditLabel(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="intro" label="个人介绍" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="180" />
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="dialogVisible" title="审核详情" width="600px" destroy-on-close>
      <template v-if="currentItem">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="真实姓名">{{ currentItem.realName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentItem.phone }}</el-descriptions-item>
          <el-descriptions-item label="服务城市">{{ currentItem.serviceCity }}</el-descriptions-item>
          <el-descriptions-item label="服务区域">{{ currentItem.serviceArea }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentItem.idCard }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ currentItem.birthDate }}</el-descriptions-item>
          <el-descriptions-item label="个人介绍">{{ currentItem.intro }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="auditTagType(currentItem.auditStatus)">
              {{ auditLabel(currentItem.auditStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentItem.auditRemark" label="审核备注">
            <span style="color: #F56C6C;">{{ currentItem.auditRemark }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- Materials Images -->
        <div v-if="currentItem.materials" class="materials-section">
          <h4>资质材料</h4>
          <div class="material-images">
            <el-image
              v-for="(url, idx) in materialUrls"
              :key="idx"
              :src="url"
              fit="cover"
              style="width: 120px; height: 120px; margin-right: 8px; border-radius: 4px;"
              :preview-src-list="materialUrls"
              :initial-index="idx"
            />
            <span v-if="materialUrls.length === 0" class="no-material">未上传材料</span>
          </div>
        </div>

        <!-- Reject Remark -->
        <div v-if="showRejectInput" class="reject-section">
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer" v-if="currentItem?.auditStatus === 'PENDING'">
          <el-button @click="handleReject" :disabled="!showRejectInput">
            驳回（需填写原因）
          </el-button>
          <el-button v-if="showRejectInput" type="danger" :loading="auditing" @click="submitReject">
            确认驳回
          </el-button>
          <el-button v-else type="danger" @click="showRejectInput = true">
            驳回
          </el-button>
          <el-button type="primary" :loading="auditing" @click="submitApprove">
            通过
          </el-button>
        </div>
        <div v-else>
          <el-button @click="dialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const loading = ref(false)
const auditing = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')
const dialogVisible = ref(false)
const currentItem = ref(null)
const showRejectInput = ref(false)
const rejectRemark = ref('')

function auditLabel(status) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[status] || status
}

function auditTagType(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const materialUrls = computed(() => {
  if (!currentItem.value?.materials) return []
  return currentItem.value.materials.split(',').filter(Boolean)
})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.auditStatus = statusFilter.value
    const res = await api.get('/admin/escort/audit/page', { params })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取审核列表失败')
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  currentItem.value = row
  showRejectInput.value = false
  rejectRemark.value = ''
  dialogVisible.value = true
}

async function submitApprove() {
  auditing.value = true
  try {
    await api.post(`/admin/escort/audit/${currentItem.value.userId || currentItem.value.id}`, null, {
      params: { status: 'APPROVED' }
    })
    ElMessage.success('已通过审核')
    dialogVisible.value = false
    await fetchData()
  } catch (e) {
    if (e?.response?.status === 409) {
      ElMessage.warning('不是待审核状态，请刷新列表')
    } else {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  } finally {
    auditing.value = false
  }
}

function handleReject() {
  showRejectInput.value = true
  rejectRemark.value = ''
}

async function submitReject() {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  auditing.value = true
  try {
    await api.post(`/admin/escort/audit/${currentItem.value.userId || currentItem.value.id}`, null, {
      params: { status: 'REJECTED', remark: rejectRemark.value }
    })
    ElMessage.success('已驳回')
    dialogVisible.value = false
    await fetchData()
  } catch (e) {
    if (e?.response?.status === 409) {
      ElMessage.warning('不是待审核状态，请刷新列表')
    } else {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  } finally {
    auditing.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-audit {
  padding: 20px;
}
.filter-tabs {
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.materials-section {
  margin-top: 16px;
}
.materials-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
}
.material-images {
  display: flex;
  flex-wrap: wrap;
}
.no-material {
  color: #999;
  font-size: 14px;
}
.reject-section {
  margin-top: 16px;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>