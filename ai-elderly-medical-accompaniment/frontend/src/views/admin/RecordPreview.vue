<template>
  <div class="admin-record-preview">
    <div class="page-header">
      <el-button text @click="$router.push('/admin/dashboard')">
        <el-icon :size="20"><ArrowLeft /></el-icon>
        <span style="margin-left:4px">返回看板</span>
      </el-button>
      <h2 class="page-title">档案脱敏视图预览</h2>
      <div style="width:80px"></div>
    </div>

    <el-alert
      title="以下数据由后端按管理员角色自动脱敏处理，前端不做额外处理。姓名、诊断结论等敏感字段已自动打码，身份证号密文存储不可逆。"
      type="info" :closable="false" show-icon style="margin:16px 20px 0"
    />

    <el-table
      :data="records"
      v-loading="loading"
      stripe
      style="width:100%;margin-top:16px"
      @row-click="showDetail"
    >
      <el-table-column prop="id" label="档案ID" width="80" />
      <el-table-column prop="elderlyName" label="就诊人" width="120" />
      <el-table-column prop="hospitalName" label="就诊医院" min-width="160" />
      <el-table-column prop="department" label="科室" width="120" />
      <el-table-column prop="visitDate" label="就诊日期" width="120" />
      <el-table-column prop="diagnosis" label="诊断结果" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="showDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

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
    <el-dialog v-model="dialogVisible" title="档案详情（脱敏视图）" width="700px" destroy-on-close>
      <template v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="档案ID">{{ currentRecord.id }}</el-descriptions-item>
          <el-descriptions-item label="就诊人">{{ currentRecord.elderlyName }}</el-descriptions-item>
          <el-descriptions-item label="就诊医院">{{ currentRecord.hospitalName }}</el-descriptions-item>
          <el-descriptions-item label="就诊科室">{{ currentRecord.department }}</el-descriptions-item>
          <el-descriptions-item label="就诊日期">{{ currentRecord.visitDate }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ currentRecord.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主诉" :span="2">{{ currentRecord.chiefComplaint || '-' }}</el-descriptions-item>
          <el-descriptions-item label="诊断结果" :span="2">
            <span :style="{color: currentRecord.diagnosis ? '#303133' : '#999'}">{{ currentRecord.diagnosis || '（已脱敏处理）' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="处方信息" :span="2">{{ currentRecord.prescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentRecord.createTime }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="documents.length > 0" class="documents-section">
          <h4>关联单据（{{ documents.length }}份）</h4>
          <div class="doc-list">
            <div v-for="doc in documents" :key="doc.id" class="doc-item">
              <div class="doc-thumb">
                <el-image
                  v-if="doc.url"
                  :src="doc.url"
                  fit="cover"
                  style="width:80px;height:80px;border-radius:8px"
                  :preview-src-list="[doc.url]"
                  preview-teleported
                />
                <el-icon v-else :size="32"><Picture /></el-icon>
              </div>
              <div class="doc-info">
                <span class="doc-name">{{ doc.name || '单据' }}</span>
                <el-tag :type="doc.ocrStatus === 'SUCCESS' ? 'success' : 'warning'" size="small">
                  {{ doc.ocrStatus === 'SUCCESS' ? '已识别' : doc.ocrStatus === 'RUNNING' ? '识别中' : '未识别' }}
                </el-tag>
                <span class="ocr-result" v-if="doc.ocrResult">{{ doc.ocrResult }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无关联单据" :image-size="60" />
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/request'

const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const currentRecord = ref(null)
const documents = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await api.get('/record/page', {
      params: { current: page.value, size: size.value }
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取档案列表失败')
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  currentRecord.value = row
  dialogVisible.value = true
  try {
    const [detailRes, docRes] = await Promise.all([
      api.get(`/record/${row.id}`),
      api.get(`/record/${row.id}/documents`)
    ])
    if (detailRes.data) {
      currentRecord.value = detailRes.data
    }
    documents.value = Array.isArray(docRes.data) ? docRes.data : (docRes.data?.records || [])
  } catch (e) {
    ElMessage.error('加载档案详情失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.admin-record-preview {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: #333;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 0 20px;
}

.documents-section {
  margin-top: 20px;
}

.documents-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #333;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
}

.doc-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.doc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.ocr-result {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>