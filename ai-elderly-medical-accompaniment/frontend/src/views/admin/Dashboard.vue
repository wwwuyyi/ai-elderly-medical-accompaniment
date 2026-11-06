<template>
  <div class="admin-dashboard">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:24px">
    <h2 style="margin:0">管理后台</h2>
    <el-button @click="() => { const s = useUserStore(); s.logout(); $router.push('/login') }" type="danger" plain size="default">退出登录</el-button>
  </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- Top Stat Cards -->
      <el-row :gutter="20" class="stat-row">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #ecf5ff;">
                <el-icon :size="36" color="#409EFF"><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ dashboard.todayOrders || 0 }}</div>
                <div class="stat-label">今日订单</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #fef0f0;">
                <el-icon :size="36" color="#E6A23C"><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ dashboard.pendingAudit || 0 }}</div>
                <div class="stat-label">待审核</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #e1f3d8;">
                <el-icon :size="36" color="#67C23A"><UserFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ dashboard.totalUsers || 0 }}</div>
                <div class="stat-label">累计用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 7-Day Trend Bar Chart -->
      <el-card shadow="never" class="chart-card">
        <template #header><span>近7天订单趋势</span></template>
        <div class="bar-chart" v-if="dashboard.trend7Days && dashboard.trend7Days.length > 0">
          <div
            v-for="(item, index) in dashboard.trend7Days"
            :key="index"
            class="bar-column"
          >
            <div class="bar-wrapper">
              <div
                class="bar"
                :style="{
                  height: barHeight(item.count),
                  background: barColor(index)
                }"
                :title="`${item.date}: ${item.count}单`"
              ></div>
            </div>
            <div class="bar-label">{{ item.date }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无趋势数据" :image-size="80" />
      </el-card>

      <!-- Quick Links -->
      <el-card shadow="never" class="links-card">
        <template #header><span>快捷操作</span></template>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="quick-link" @click="$router.push('/admin/audit')">
              <el-icon :size="28" color="#409EFF"><Checked /></el-icon>
              <span>资质审核</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="quick-link" @click="$router.push('/admin/users')">
              <el-icon :size="28" color="#67C23A"><User /></el-icon>
              <span>用户管理</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="quick-link" @click="$router.push('/admin/orders')">
              <el-icon :size="28" color="#E6A23C"><List /></el-icon>
              <span>订单管理</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="quick-link" @click="$router.push('/admin/evaluations')">
              <el-icon :size="28" color="#F56C6C"><Star /></el-icon>
              <span>评价管理</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="quick-link" @click="$router.push('/admin/record-preview')">
              <el-icon :size="28" color="#9254DE"><View /></el-icon>
              <span>脱敏预览</span>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Document, Clock, UserFilled, Checked, User, List, Star, View
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import api from '@/api/request'

const loading = ref(true)
const dashboard = ref({})

function barHeight(count) {
  const maxCount = Math.max(...(dashboard.value.trend7Days || []).map(d => d.count), 1)
  return Math.max((count / maxCount) * 180, 8) + 'px'
}

function barColor(index) {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#36CFC9', '#9254DE']
  return colors[index % colors.length]
}

onMounted(async () => {
  try {
    const res = await api.get('/admin/dashboard')
    dashboard.value = res.data || {}
  } catch (e) {
    dashboard.value = {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}
.loading-container {
  padding: 20px;
}
.stat-row {
  margin-bottom: 20px;
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}
.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 2px;
}
.chart-card {
  margin-bottom: 20px;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 220px;
  padding: 16px 0;
}
.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.bar-wrapper {
  height: 180px;
  display: flex;
  align-items: flex-end;
}
.bar {
  width: 40px;
  border-radius: 4px 4px 0 0;
  transition: opacity 0.2s;
  min-width: 24px;
}
.bar:hover {
  opacity: 0.8;
}
.bar-label {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
.links-card {
  margin-bottom: 20px;
}
.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.quick-link:hover {
  background: #f5f7fa;
}
.quick-link span {
  font-size: 14px;
  color: #333;
}
</style>