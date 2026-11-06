<template>
  <div class="notice-view-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">陪诊告知书</h1>
      <div style="width: 32px;"></div>
    </div>

    <div class="notice-content" v-if="content">
      <div class="warning-box">
        <div class="warning-icon-row">
          <el-icon :size="24" color="#E6A23C"><WarningFilled /></el-icon>
          <span class="warning-title">重要提示</span>
        </div>
        <p class="warning-text">AI生成内容仅供参考，不构成任何诊疗建议</p>
      </div>

      <div class="content-card">
        <div class="content-body" v-html="content"></div>
      </div>
    </div>

    <div class="loading-area" v-else-if="loading">
      <el-skeleton :rows="10" animated />
    </div>

    <div class="bottom-bar">
      <el-button
        class="back-btn"
        @click="$router.back()"
      >
        返回
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const route = useRoute()
const content = ref('')
const loading = ref(true)

const loadContent = async () => {
  try {
    const res = await api.get(`/ai/content/${route.params.id}`)
    if (res.code === 200 && res.data) {
      content.value = res.data.content || res.data
    } else {
      ElMessage.error(res.message || '加载告知书失败')
    }
  } catch (e) {
    ElMessage.error('加载告知书失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadContent)
</script>

<style scoped>
.notice-view-page {
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

.notice-content {
  padding: 16px 20px;
}

.warning-box {
  background: linear-gradient(135deg, #FFF5E6 0%, #FDECCC 100%);
  border: 2px solid #E6A23C;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 20px;
}

.warning-icon-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.warning-title {
  font-size: 22px;
  font-weight: 700;
  color: #E6A23C;
}

.warning-text {
  font-size: 18px;
  color: #E6A23C;
  margin: 0;
  padding-left: 34px;
  line-height: 1.6;
}

.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.content-body {
  font-size: 18px;
  line-height: 2;
  color: #333;
}

.content-body :deep(h2) {
  font-size: 22px;
  margin: 20px 0 12px;
}

.content-body :deep(h3) {
  font-size: 20px;
  margin: 16px 0 10px;
}

.content-body :deep(p) {
  margin: 8px 0;
}

.content-body :deep(strong) {
  font-weight: 700;
  color: #222;
}

.content-body :deep(ul), .content-body :deep(ol) {
  padding-left: 24px;
}

.content-body :deep(li) {
  margin: 6px 0;
}

.loading-area {
  padding: 20px;
}

.bottom-bar {
  padding: 16px 20px;
}

.back-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  border-radius: 12px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  color: #fff;
}

.back-btn:hover {
  background: #254a8a;
}
</style>