<template>
  <div class="admin-orders">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
    <el-button @click="$router.push('/admin/dashboard')">← 返回看板</el-button>
    <h2 style="margin:0">订单管理</h2>
  </div>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select
        v-model="statusFilter"
        placeholder="订单状态"
        clearable
        style="width: 180px;"
        @change="handleSearch"
      >
        <el-option label="全部" value="" />
        <el-option label="待派单" value="PENDING_DISPATCH" />
        <el-option label="已接单" value="ACCEPTED" />
        <el-option label="服务中" value="IN_SERVICE" />
        <el-option label="已完成" value="SERVICE_FINISHED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索订单号/家属/陪诊员"
        clearable
        style="width: 280px; margin-left: 12px;"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch" style="margin-left: 12px;">
        搜索
      </el-button>
    </div>

    <!-- Table -->
    <el-table
      :data="records"
      v-loading="loading"
      stripe
      style="width: 100%; margin-top: 16px;"
    >
      <el-table-column prop="orderNo" label="订单编号" width="160" />
      <el-table-column prop="elderlyName" label="就诊人" width="100" />
      <el-table-column label="家属ID" width="100">
        <template #default="{ row }">{{ row.familyId || '-' }}</template>
      </el-table-column>
      <el-table-column label="陪诊员ID" width="100">
        <template #default="{ row }">{{ row.escortId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="department" label="科室" width="100" />
      <el-table-column prop="hospital" label="医院" min-width="140" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" width="100">
        <template #default="{ row }">
          {{ row.amount ? '¥' + row.amount : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING_DISPATCH'"
            type="primary"
            size="small"
            @click="showDispatchDialog(row)"
          >
            派单
          </el-button>
          <el-button
            v-if="row.status === 'SERVICE_FINISHED'"
            type="success"
            size="small"
            @click="handleSettle(row)"
          >
            结算
          </el-button>
        </template>
      </el-table-column>
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

    <!-- Dispatch Dialog -->
    <el-dialog v-model="dispatchVisible" title="派单" width="400px" destroy-on-close>
      <el-form :model="dispatchForm" label-width="100px">
        <el-form-item label="订单编号">
          <span>{{ dispatchTarget?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="选择陪诊师" required>
          <el-select v-model="dispatchForm.escortId" placeholder="请选择已审核通过的陪诊师" style="width:100%" @visible-change="v => v && loadEscorts()">
            <el-option v-for="e in escortList" :key="e.userId" :label="`${e.realName} (ID:${e.userId})`" :value="e.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="dispatching"
          :disabled="!dispatchForm.escortId"
          @click="handleDispatch"
        >
          确认派单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(false)
const dispatching = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')
const keyword = ref('')

// Dispatch dialog
const dispatchVisible = ref(false)
const dispatchTarget = ref(null)
const escortList = ref([])
const dispatchForm = ref({ escortId: '' })

async function loadEscorts() {
  try {
    const r = await api.get('/admin/escort/audit/page', { params: { auditStatus: 'APPROVED', size: 50 } })
    escortList.value = r.data?.records || []
  } catch { escortList.value = [] }
}

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

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    if (keyword.value) params.keyword = keyword.value
    const res = await api.get('/admin/order/page', { params })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function showDispatchDialog(row) {
  dispatchTarget.value = row
  dispatchForm.value = { escortId: '' }
  dispatchVisible.value = true
}

async function handleDispatch() {
  if (!dispatchForm.value.escortId) {
    ElMessage.warning('请输入陪诊员ID')
    return
  }
  dispatching.value = true
  try {
    await api.post(`/order/${dispatchTarget.value.id}/dispatch`, null, {
      params: { escortId: dispatchForm.value.escortId }
    })
    ElMessage.success('派单成功')
    dispatchVisible.value = false
    await fetchData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '派单失败')
  } finally {
    dispatching.value = false
  }
}

async function handleSettle(row) {
  try {
    await ElMessageBox.confirm(
      `确认对订单 "${row.orderNo}" 进行结算吗？`,
      '订单结算',
      {
        confirmButtonText: '确认结算',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
  } catch {
    return
  }
  try {
    await api.post(`/order/${row.id}/settle`)
    ElMessage.success('结算成功')
    await fetchData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '结算失败')
  }
}

onMounted(() => {
  fetchData()
  poll = setInterval(fetchData, 5000)
})
onUnmounted(() => clearInterval(poll))
let poll
</script>

<style scoped>
.admin-orders {
  padding: 20px;
}
.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>