<template>
  <div class="report-manage-page">
    <div class="page-header">
      <h2 class="page-title">举报管理</h2>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width: 150px" @change="loadReports">
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
    </div>

    <el-table :data="reports" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="goodsId" label="商品ID" width="100" />
      <el-table-column prop="reason" label="举报原因" min-width="300" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="举报时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="handleHandle(row.id, 1)">
              通过
            </el-button>
            <el-button size="small" type="warning" @click="handleHandle(row.id, 2)">
              驳回
            </el-button>
          </template>
          <span v-else style="color: #909399; font-size: 13px">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadReports"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI } from '@/api/admin'
import type { Report } from '@/api/types'

const reports = ref<Report[]>([])
const loading = ref(true)
const statusFilter = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

onMounted(() => loadReports())

async function loadReports() {
  loading.value = true
  try {
    const res = await adminAPI.getReportList({
      page: currentPage.value,
      pageSize,
      status: statusFilter.value
    })
    if (res.data) {
      reports.value = res.data.records || []
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载举报失败', e)
  } finally {
    loading.value = false
  }
}

function statusTagType(status?: number) {
  switch (status) {
    case 0: return 'danger'
    case 1: return 'success'
    case 2: return 'info'
    default: return 'info'
  }
}

function statusText(status?: number) {
  switch (status) {
    case 0: return '待处理'
    case 1: return '已处理'
    case 2: return '已驳回'
    default: return '未知'
  }
}

async function handleHandle(id: number, status: number) {
  try {
    const action = status === 1 ? '处理' : '驳回'
    const { value } = await ElMessageBox.prompt(`请输入${action}结果`, `${action}举报`)
    await adminAPI.handleReport(id, status, value)
    ElMessage.success(`${action}成功`)
    loadReports()
  } catch (e) {}
}
</script>

<style scoped>
.report-manage-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
