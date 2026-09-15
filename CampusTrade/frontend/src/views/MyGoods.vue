<template>
  <div class="my-goods-page">
    <Header />
    <div class="page-container">
      <div class="page-header">
        <h2 class="section-title">我的商品</h2>
        <el-button type="primary" @click="$router.push('/goods/publish')">
          <el-icon><Plus /></el-icon> 发布商品
        </el-button>
      </div>

      <!-- 状态标签 -->
      <el-tabs v-model="activeTab" @tab-change="loadGoods">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="在售" name="onsale" />
        <el-tab-pane label="已下架" name="offline" />
        <el-tab-pane label="已售出" name="sold" />
        <el-tab-pane label="已驳回" name="rejected" />
      </el-tabs>

      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="goodsList.length === 0" class="empty-state">
        <el-icon :size="60"><GoodsFilled /></el-icon>
        <p>暂无商品</p>
        <el-button type="primary" @click="$router.push('/goods/publish')">去发布</el-button>
      </div>
      <div v-else>
        <el-table :data="goodsList" stripe style="width: 100%">
          <el-table-column prop="title" label="商品标题" min-width="200">
            <template #default="{ row }">
              <span class="goods-title-link" @click="$router.push(`/goods/detail/${row.id}`)">
                {{ row.title }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="售价" width="120">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column prop="createTime" label="发布时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="$router.push(`/goods/edit/${row.id}`)">
                编辑
              </el-button>
              <el-button v-if="row.status === 1" text type="warning" size="small" @click="handleOffline(row.id)">
                下架
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row.id)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { goodsAPI } from '@/api/goods'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { Goods } from '@/api/types'

const goodsList = ref<Goods[]>([])
const loading = ref(true)
const activeTab = ref('all')

const statusMap: Record<string, number | undefined> = {
  all: undefined,
  pending: 0,
  onsale: 1,
  offline: 2,
  sold: 3,
  rejected: 4
}

onMounted(() => {
  loadGoods()
})

async function loadGoods() {
  loading.value = true
  try {
    const status = statusMap[activeTab.value]
    const res = await goodsAPI.getMyGoods(status)
    if (res.data) goodsList.value = res.data
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

async function handleOffline(id: number) {
  try {
    await goodsAPI.offline(id)
    ElMessage.success('已下架')
    loadGoods()
  } catch (e) {}
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示')
    await goodsAPI.deleteGoods(id)
    ElMessage.success('已删除')
    loadGoods()
  } catch (e) {}
}
</script>

<style scoped>
.my-goods-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.goods-title-link {
  color: #409eff;
  cursor: pointer;
}

.goods-title-link:hover {
  text-decoration: underline;
}
</style>
