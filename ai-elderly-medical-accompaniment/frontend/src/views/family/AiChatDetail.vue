<template>
  <div class="chat-detail-page">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="chat-title">{{ sessionTitle || 'AI健康助手' }}</h1>
      <div style="width: 32px;"></div>
    </div>

    <!-- 上下文选择区（仅新建会话时显示） -->
    <div class="context-bar" v-if="isNewSession && !sessionCreated">
      <span class="context-label">选择咨询场景（可选）：</span>
      <div class="context-tags">
        <el-tag
          :type="contextType === 'GENERAL' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'GENERAL'; contextId = null"
        >
          通用咨询
        </el-tag>
        <el-tag
          :type="contextType === 'ELDERLY' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'ELDERLY'"
        >
          老人档案
        </el-tag>
        <el-tag
          :type="contextType === 'RECORD' ? '' : 'info'"
          class="context-tag"
          @click="contextType = 'RECORD'"
        >
          就医记录
        </el-tag>
      </div>
      <div class="context-select" v-if="contextType === 'ELDERLY' || contextType === 'RECORD'">
        <span class="context-label">请选择关联的{{ contextType === 'ELDERLY' ? '老人' : '就医记录' }}：</span>
        <el-select
          v-model="contextId"
          :placeholder="'请选择' + (contextType === 'ELDERLY' ? '老人' : '记录')"
          style="width: 100%; margin-top: 8px;"
          size="large"
        >
          <el-option
            v-for="item in contextOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <!-- 空状态 -->
      <div class="welcome-area" v-if="messages.length === 0 && !isStreaming">
        <el-icon :size="56" color="#2F5AA8"><ChatDotRound /></el-icon>
        <p class="welcome-title">您好，我是AI健康助手</p>
        <p class="welcome-desc">我可以帮您解答就诊流程、检查报告、用药注意事项等问题</p>
        <div class="quick-questions">
          <div class="quick-item" @click="sendQuick('去心内科就诊需要准备什么？')">
            就诊需要准备什么？
          </div>
          <div class="quick-item" @click="sendQuick('血糖偏高平时要注意什么？')">
            血糖偏高要注意什么？
          </div>
          <div class="quick-item" @click="sendQuick('高血压吃药有什么注意事项？')">
            吃药有什么注意事项？
          </div>
        </div>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="(msg, index) in messages"
        :key="msg.id || index"
        :class="['message-row', msg.role === 'USER' ? 'msg-user' : 'msg-assistant']"
      >
        <div class="msg-avatar">
          <el-icon :size="28" v-if="msg.role === 'ASSISTANT'">
            <Service />
          </el-icon>
          <el-icon :size="28" v-else>
            <UserFilled />
          </el-icon>
        </div>
        <div :class="['msg-bubble', msg.role === 'USER' ? 'bubble-user' : 'bubble-assistant']">
          <div class="bubble-text" v-html="formatContent(msg.content)"></div>
          <div class="bubble-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>

      <!-- 流式输出中的AI消息 -->
      <div class="message-row msg-assistant" v-if="isStreaming">
        <div class="msg-avatar">
          <el-icon :size="28"><Service /></el-icon>
        </div>
        <div class="msg-bubble bubble-assistant">
          <div class="bubble-text streaming-text" v-html="formatContent(streamingContent)"></div>
          <span class="typing-cursor">|</span>
        </div>
      </div>

      <!-- 免责声明 -->
      <div class="disclaimer-bar" v-if="messages.length > 0">
        <el-icon :size="18" color="#E6A23C"><WarningFilled /></el-icon>
        <span>AI生成内容仅供参考，不构成任何诊疗建议</span>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="input-bar">
      <div class="input-row">
        <el-input
          v-model="inputText"
          placeholder="输入您的问题..."
          class="msg-input"
          size="large"
          :disabled="isStreaming"
          @keyup.enter="handleSend"
          clearable
        />
        <el-button
          type="primary"
          class="send-btn"
          :disabled="!inputText.trim() || isStreaming"
          @click="handleSend"
        >
          <el-icon :size="22" v-if="!isStreaming"><Promotion /></el-icon>
          <el-icon :size="22" class="is-loading" v-else><Loading /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChatDotRound, Service, UserFilled, Promotion, Loading, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMessages, sendMessageStream } from '@/api/aiChat'
import api from '@/api/request'

const route = useRoute()
const router = useRouter()

const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const sessionId = ref(null)
const sessionTitle = ref('')
const sessionCreated = ref(false)

// 返回AI助手列表：新建会话页使用 replace 避免残留；已存在会话直接 back
function goBack() {
  if (isNewSession.value) {
    router.replace('/family/ai-chat')
  } else {
    router.back()
  }
}
const messageListRef = ref(null)

// 上下文选择
const contextType = ref('GENERAL')
const contextId = ref(null)
const contextOptions = ref([])

// 从query参数获取初始上下文
const queryContextType = route.query.contextType || null
const queryContextId = route.query.contextId || null

const isNewSession = computed(() => !route.params.sessionId || route.params.sessionId === 'new')

let abortController = null

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return h + ':' + m
}

// 格式化内容（换行转<br>）
const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 加载历史消息
const loadHistory = async (sid) => {
  try {
    const res = await getMessages(sid)
    if (res.code === 200 && res.data) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch (e) {
    console.error('Failed to load messages:', e)
  }
}

// 加载上下文选项（老人列表或就医记录列表）
const loadContextOptions = async (type) => {
  try {
    if (type === 'ELDERLY') {
      const res = await api.get('/elderly/page', { params: { current: 1, size: 50 } })
      if (res.code === 200 && res.data?.records) {
        contextOptions.value = res.data.records.map(e => ({ id: e.id, name: e.name + (e.gender === 'MALE' ? ' 男' : ' 女') }))
      }
    } else if (type === 'RECORD') {
      const res = await api.get('/record/page', { params: { current: 1, size: 50 } })
      if (res.code === 200 && res.data?.records) {
        contextOptions.value = res.data.records.map(r => ({ id: r.id, name: (r.department || '') + ' - ' + (r.visitDate || '') }))
      }
    }
  } catch (e) {
    console.error('Failed to load context options:', e)
  }
}

// 监听上下文类型切换
watch(contextType, (val) => {
  if (val === 'ELDERLY' || val === 'RECORD') {
    contextId.value = null
    loadContextOptions(val)
  }
})

// 发送消息
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return
  inputText.value = ''

  // 添加用户消息到列表
  const userMsg = {
    id: Date.now(),
    role: 'USER',
    content: text,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMsg)
  scrollToBottom()

  // 开始流式
  isStreaming.value = true
  streamingContent.value = ''

  const sendData = {
    message: text,
    sessionId: sessionId.value,
    contextType: queryContextType || contextType.value,
    contextId: queryContextId || contextId.value || null
  }

  abortController = sendMessageStream(sendData, {
    onSessionId: (sid) => {
      // 服务端返回了sessionId
      if (!sessionId.value) {
        sessionId.value = sid
        sessionCreated.value = true
        // 更新URL
        router.replace(`/family/ai-chat/${sid}`)
      }
    },
    onChunk: (chunk) => {
      streamingContent.value += chunk
      scrollToBottom()
    },
    onDone: () => {
      // 将流式内容保存为ASSISTANT消息
      if (streamingContent.value) {
        messages.value.push({
          id: Date.now(),
          role: 'ASSISTANT',
          content: streamingContent.value,
          createTime: new Date().toISOString()
        })
        streamingContent.value = ''
      }
      isStreaming.value = false
      scrollToBottom()
    },
    onError: (err) => {
      ElMessage.error('发送失败: ' + err.message)
      if (streamingContent.value) {
        messages.value.push({
          id: Date.now(),
          role: 'ASSISTANT',
          content: streamingContent.value + '\n\n（回复中断）',
          createTime: new Date().toISOString()
        })
        streamingContent.value = ''
      }
      isStreaming.value = false
    }
  })
}

// 快捷问题
const sendQuick = (text) => {
  inputText.value = text
  handleSend()
}

onMounted(() => {
  // 从query参数获取初始上下文
  if (queryContextType) {
    contextType.value = queryContextType
  }
  if (queryContextId) {
    contextId.value = Number(queryContextId)
  }

  if (!isNewSession.value) {
    // 加载已有会话
    sessionId.value = Number(route.params.sessionId)
    sessionCreated.value = true
    loadHistory(sessionId.value)
  } else if (queryContextType) {
    // 带上下文参数的新会话
    loadContextOptions(queryContextType)
  }
})

onUnmounted(() => {
  if (abortController) {
    abortController.abort()
  }
})
</script>

<style scoped>
.chat-detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #2F5AA8;
  color: #fff;
  flex-shrink: 0;
  min-height: 52px;
}

.back-btn {
  color: #fff !important;
}

.chat-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

/* 上下文选择区 */
.context-bar {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.context-label {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
  display: block;
}

.context-tags {
  display: flex;
  gap: 10px;
}

.context-tag {
  font-size: 16px !important;
  padding: 8px 16px !important;
  cursor: pointer;
  border-radius: 20px;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  -webkit-overflow-scrolling: touch;
}

/* 欢迎区 */
.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px 30px;
  text-align: center;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin: 16px 0 8px;
}

.welcome-desc {
  font-size: 16px;
  color: #999;
  margin: 0 0 24px;
  line-height: 1.6;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 320px;
}

.quick-item {
  padding: 12px 18px;
  background: #eef3fb;
  border-radius: 20px;
  font-size: 16px;
  color: #2F5AA8;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:active {
  background: #dce6f8;
  transform: scale(0.97);
}

/* 消息行 */
.message-row {
  display: flex;
  margin-bottom: 18px;
  gap: 10px;
}

.msg-user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #999;
}

.msg-user .msg-avatar {
  background: #2F5AA8;
  color: #fff;
}

/* 消息气泡 */
.msg-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
}

.bubble-assistant {
  background: #fff;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.bubble-user {
  background: #2F5AA8;
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble-text {
  font-size: 18px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-user .bubble-text {
  font-size: 18px;
}

.bubble-text :deep(strong) {
  font-weight: 700;
}

.bubble-time {
  font-size: 12px;
  color: #bbb;
  margin-top: 6px;
  text-align: right;
}

.bubble-user .bubble-time {
  color: rgba(255,255,255,0.7);
}

/* 流式文字 */
.streaming-text {
  display: inline;
}

.typing-cursor {
  display: inline;
  color: #2F5AA8;
  font-weight: 700;
  font-size: 18px;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 免责声明 */
.disclaimer-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  margin: 8px 0 16px;
  font-size: 14px;
  color: #E6A23C;
  background: #FFF8ED;
  border-radius: 8px;
  text-align: center;
}

/* 底部输入区 */
.input-bar {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  flex-shrink: 0;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.msg-input {
  flex: 1;
}

.msg-input :deep(.el-input__inner) {
  height: 48px;
  font-size: 18px;
  border-radius: 24px;
  padding: 0 20px;
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
}

.send-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  font-size: 20px;
  background: #2F5AA8;
  border-color: #2F5AA8;
  flex-shrink: 0;
}

.send-btn:hover {
  background: #254a8a;
}

.send-btn.is-disabled {
  background: #ccc;
  border-color: #ccc;
}

/* 适配选择器 */
.context-select {
  margin-top: 10px;
}

:deep(.el-select) {
  width: 100%;
}
:deep(.el-select .el-input__inner) {
  height: 44px;
  font-size: 18px;
}
:deep(.el-tag--large) {
  font-size: 16px;
}
</style>