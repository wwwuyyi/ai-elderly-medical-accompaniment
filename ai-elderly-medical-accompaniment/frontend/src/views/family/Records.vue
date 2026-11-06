<template>
  <div class="records-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">就医档案</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="records-list" v-if="records.length > 0">
      <div
        v-for="record in records"
        :key="record.id"
        class="record-card"
        @click="$router.push(`/family/record/${record.id}`)"
      >
        <div class="card-header">
          <span class="hospital-name">{{ record.hospitalName }}</span>
          <span class="visit-date">{{ record.visitDate }}</span>
        </div>
        <div class="card-mid">
          <span class="department">{{ record.department }}</span>
        </div>
        <div class="card-footer">
          <span class="diagnosis">
            {{ truncateText(record.diagnosis, 50) }}
          </span>
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchRecords"
          :pager-count="5"
        />
      </div>
    </div>

    <el-empty v-else description="暂无就医档案" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '@/api/request'

const records = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const truncateText = (text, maxLen) => {
  if (!text) return '-'
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const fetchRecords = async () => {
  try {
    const res = await api.get('/record/page', {
      params: { current: pageNo.value, size: pageSize.value }
    })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load records:', e)
  }
}

onMounted(fetchRecords)
</script>

<style scoped>
.records-page {
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

.records-list {
  padding: 16px 20px;
}

.record-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s;
}

.record-card:active {
  transform: scale(0.98);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hospital-name {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.visit-date {
  font-size: 18px;
  color: #999;
}

.card-mid {
  margin-bottom: 8px;
}

.department {
  font-size: 18px;
  color: #2F5AA8;
  background: rgba(47, 90, 168, 0.08);
  padding: 2px 10px;
  border-radius: 4px;
}

.card-footer {
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.diagnosis {
  font-size: 18px;
  color: #666;
  line-height: 1.5;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>