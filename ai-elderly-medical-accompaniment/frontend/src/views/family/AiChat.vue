<template>
  <div class="ai-chat-page">
    <div class="page-header">
      <el-button icon="ArrowLeft" text @click="$router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">AI健康助手</h1>
      <div style="width: 32px;"></div>
    </div>

    <!-- 会话列表 -->
    <div class="session-list" v-if="sessions.length > 0">
      <div
        v-for="session in sessions"
        :key="session.id"
        class="session-item"
        @click="$router.push(`/family/ai-chat/${session.id}`)"
      >
        <div class="session-info">
          <span class="session-title">{{ session.title }}</span>
          <span class="session-preview">{{ session.lastMessage || '点击开始对话' }}</span>
        </div>
        <div class="session-meta">
          <span class="session-time">{{ formatTime(session.updateTime) }}</span>
          <el-button
            class="delete-btn"
            :icon="Delete"
            text
            size="small"
            @click.stop="handleDelete(session.id)"
          />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else-if="!loading">
      <el-icon :size="64" color="#ccc"><ChatDotRound /></el-icon>
      <p class="empty-title">还未开始对话</p>
      <p class="empty-desc">点击下方按钮，向AI健康助手咨询就诊相关问题</p>
    </div>

    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 底部新建按钮 -->
    <div class="bottom-bar">
      <el-button
        type="primary"
        class="new-chat-btn"
        @click="$router.push('/family/ai-chat/new')"
      >
        <el-icon :size="22"><Plus /></el-icon>
        新建咨询
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, ChatDotRound, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSessions, deleteSession } from '@/api/aiChat'

const sessions = ref([])
const loading = ref(true)

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return month + '/' + day
}

const loadSessions = async () => {
  loading.value = true
  try {
    const res = await listSessions()
    if (res.code === 200 && res.data) {
      sessions.value = res.data
    }
  } catch (e) {
    console.error('Failed to load sessions:', e)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.ai-chat-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 90px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #2F5AA8;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #fff;
}

.page-header .el-button {
  color: #fff;
}

.session-list {
  padding: 12px 16px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  transition: all 0.2s;
}

.session-item:active {
  background: #f0f4ff;
  transform: scale(0.98);
}

.session-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-preview {
  font-size: 16px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

.session-time {
  font-size: 14px;
  color: #bbb;
}

.delete-btn {
  padding: 4px;
  color: #ccc;
}

.delete-btn:hover {
  color: #f56c6c;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  text-align: center;
}

.empty-title {
  font-size: 20px;
  color: #999;
  margin: 16px 0 8px;
}

.empty-desc {
  font-size: 16px;
  color: #bbb;
  margin: 0;
}

.loading-state {
  padding: 20px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 20px calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 100;
}

.new-chat-btn {
  width: 100%;
  height: 52px;
  font-size: 22px;
  border-radius: 12px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  color: #fff;
  font-weight: 600;
}

.new-chat-btn:hover {
  background: #254a8a;
}
</style>