<template>
  <div class="elderly-list-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">老人档案</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="add-btn-wrap">
      <el-button
        type="primary"
        class="add-btn"
        @click="$router.push('/family/elderly/add')"
      >
        新增老人档案
      </el-button>
    </div>

    <div class="elderly-cards" v-if="elderlyList.length > 0">
      <div
        v-for="elderly in elderlyList"
        :key="elderly.id"
        class="elderly-card"
      >
        <div class="card-body">
          <div class="avatar">{{ elderly.name?.charAt(0) || '老' }}</div>
          <div class="info">
            <div class="name-row">
              <span class="name">{{ elderly.name }}</span>
              <el-tag :type="elderly.gender === 'MALE' ? '' : 'danger'" size="large">
                {{ elderly.gender === 'MALE' ? '男' : '女' }}
              </el-tag>
              <span class="age">{{ computedAge(elderly.birthDate) }}岁</span>
            </div>
            <div class="meta">
              <span class="mobility">{{ getMobilityLabel(elderly.mobility) }}</span>
            </div>
            <div class="phone" v-if="elderly.phone">{{ elderly.phone }}</div>
          </div>
        </div>
        <div class="card-actions">
          <el-button
            type="primary"
            plain
            class="action-btn"
            @click="$router.push(`/family/elderly/${elderly.id}`)"
          >
            编辑
          </el-button>
          <el-button
            type="danger"
            plain
            class="action-btn"
            @click="handleDelete(elderly)"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无老人档案，请添加" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/request'

const elderlyList = ref([])

const computedAge = (birthDate) => {
  if (!birthDate) return '-'
  return new Date().getFullYear() - new Date(birthDate).getFullYear()
}

const getMobilityLabel = (mobility) => {
  const map = {
    WALK: '自主行走',
    ASSIST: '需搀扶',
    WHEELCHAIR: '轮椅'
  }
  return map[mobility] || mobility || '-'
}

const fetchList = async () => {
  try {
    const res = await api.get('/elderly/page')
    if (res.code === 200 && res.data) {
      elderlyList.value = res.data.records || []
    }
  } catch (e) {
    ElMessage.error('加载老人档案失败')
  }
}

const handleDelete = async (elderly) => {
  try {
    await ElMessageBox.confirm(
      `确定删除"${elderly.name}"的档案吗？删除后无法恢复。`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await api.delete(`/elderly/${elderly.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      if (e?.response?.status === 409 || e?.code === 409) {
        ElMessage.error('存在进行中的陪诊订单，无法删除')
      } else if (e !== 'cancel') {
        ElMessage.error('删除失败')
      }
    }
  }
}

onMounted(fetchList)
</script>

<style scoped>
.elderly-list-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 20px;
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

.add-btn-wrap {
  padding: 20px;
  text-align: center;
}

.add-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  border-radius: 12px;
}

.elderly-cards {
  padding: 0 20px;
}

.elderly-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #2F5AA8;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.name {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.age {
  font-size: 18px;
  color: #666;
}

.meta {
  margin-bottom: 4px;
}

.mobility {
  font-size: 18px;
  color: #2F5AA8;
  background: rgba(47, 90, 168, 0.08);
  padding: 2px 10px;
  border-radius: 4px;
}

.phone {
  font-size: 18px;
  color: #999;
}

.card-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 20px;
  border-radius: 8px;
}

.el-tag--large {
  font-size: 16px;
}
</style>