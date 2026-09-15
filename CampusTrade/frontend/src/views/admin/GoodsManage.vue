<template>
  <div class="goods-manage-page">
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width: 150px" @change="loadGoods">
        <el-option label="待审核" :value="0" />
        <el-option label="在售" :value="1" />
        <el-option label="已下架" :value="2" />
        <el-option label="已售出" :value="3" />
        <el-option label="已驳回" :value="4" />
      </el-select>
    </div>

    <el-table :data="goodsList" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="商品标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="price" label="售价" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="handleAudit(row.id, 1)">
              通过
            </el-button>
            <el-button size="small" type="danger" @click="handleReject(row.id)">
              驳回
            </el-button>
          </template>
          <el-button size="small" type="danger" plain @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadGoods"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI } from '@/api/admin'
import type { Goods } from '@/api/types'

const goodsList = ref<Goods[]>([])
const loading = ref(true)
const statusFilter = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

onMounted(() => loadGoods())

async function loadGoods() {
  loading.value = true
  try {
    const res = await adminAPI.getGoodsList({
      page: currentPage.value,
      pageSize,
      status: statusFilter.value
    })
    if (res.data) {
      goodsList.value = res.data.records || []
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载商品失败', e)
  } finally {
    loading.value = false
  }
}

function statusTagType(status?: number) {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'info'
    case 3: return ''
    case 4: return 'danger'
    default: return 'info'
  }
}

function statusText(status?: number) {
  switch (status) {
    case 0: return '待审核'
    case 1: return '在售'
    case 2: return '已下架'
    case 3: return '已售出'
    case 4: return '已驳回'
    default: return '未知'
  }
}

async function handleAudit(id: number, status: number) {
  try {
    await adminAPI.auditGoods(id, status)
    ElMessage.success('审核通过')
    loadGoods()
  } catch (e) {}
}

async function handleReject(id: number) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回商品')
    await adminAPI.auditGoods(id, 4, value)
    ElMessage.success('已驳回')
    loadGoods()
  } catch (e) {}
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示')
    await adminAPI.deleteGoods(id)
    ElMessage.success('已删除')
    loadGoods()
  } catch (e) {}
}
</script>

<style scoped>
.goods-manage-page {
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
