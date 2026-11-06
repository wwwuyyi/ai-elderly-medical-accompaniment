<template>
  <div class="escort-orders">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
    <el-button @click="$router.push('/escort/home')">← 返回首页</el-button>
    <h2 style="margin:0">我的订单</h2>
  </div>

    <!-- Status Filter Tabs -->
    <div class="filter-tabs">
      <el-radio-group v-model="statusFilter" @change="fetchOrders">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING_DISPATCH">待派单</el-radio-button>
        <el-radio-button value="ACCEPTED">已接单</el-radio-button>
        <el-radio-button value="IN_SERVICE">服务中</el-radio-button>
        <el-radio-button value="SERVICE_FINISHED">已完成</el-radio-button>
        <el-radio-button value="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Table -->
    <el-table
      :data="orders"
      v-loading="loading"
      stripe
      style="width: 100%; margin-top: 16px;"
      @row-click="(row) => $router.push(`/escort/order/${row.id}`)"
    >
      <el-table-column prop="orderNo" label="订单编号" width="160" />
      <el-table-column prop="elderlyName" label="就诊人" width="100" />
      <el-table-column prop="appointmentTime" label="预约时间" width="180" />
      <el-table-column prop="department" label="就诊科室" width="120" />
      <el-table-column prop="hospital" label="就诊医院" min-width="150" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACCEPTED'"
            type="primary"
            size="small"
            @click.stop="handleStart(row)"
          >
            开始服务
          </el-button>
          <el-button
            v-if="row.status === 'IN_SERVICE'"
            type="success"
            size="small"
            @click.stop="handleFinish(row)"
          >
            结束服务
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
        @current-change="fetchOrders"
        @size-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/request'

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref('')

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

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await api.get('/order/page', { params })
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取订单列表失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function handleStart(row) {
  try {
    await ElMessageBox.confirm(`确认开始服务订单 "${row.orderNo}" 吗？`, '开始服务', {
      confirmButtonText: '确认开始',
      cancelButtonText: '取消',
      type: 'info'
    })
  } catch {
    return
  }
  try {
    await api.post(`/order/${row.id}/start`)
    ElMessage.success('已开始服务')
    await fetchOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleFinish(row) {
  try {
    await ElMessageBox.confirm(`确认结束服务订单 "${row.orderNo}" 吗？`, '结束服务', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await api.post(`/order/${row.id}/finish`)
    ElMessage.success('服务已结束')
    await fetchOrders()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  fetchOrders()
  poll = setInterval(fetchOrders, 5000)
})
onUnmounted(() => clearInterval(poll))
let poll
</script>

<style scoped>
.escort-orders {
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
</style>