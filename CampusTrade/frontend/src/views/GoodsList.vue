<template>
  <div class="goods-list-page">
    <Header />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-inner">
        <el-input
          v-model="keyword"
          placeholder="搜索商品名称、描述..."
          size="large"
          clearable
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div class="page-container">
      <!-- 分类筛选 -->
      <div class="filter-bar">
        <span class="filter-label">商品分类：</span>
        <el-radio-group v-model="selectedCategory" @change="handleCategoryChange">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.categoryName }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 商品列表 -->
      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="goodsList.length === 0" class="empty-state">
        <el-icon :size="60"><GoodsFilled /></el-icon>
        <p>暂无相关商品</p>
      </div>
      <div v-else class="goods-grid">
        <GoodsCard v-for="item in goodsList" :key="item.id" :goods="item" />
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { goodsAPI } from '@/api/goods'
import { categoryAPI } from '@/api/category'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import type { Category, GoodsVO } from '@/api/types'

const route = useRoute()
const router = useRouter()

const goodsList = ref<GoodsVO[]>([])
const categories = ref<Category[]>([])
const loading = ref(true)
const keyword = ref('')
const selectedCategory = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)

onMounted(async () => {
  // 从路由参数恢复搜索条件
  if (route.query.keyword) keyword.value = route.query.keyword as string
  if (route.query.categoryId) selectedCategory.value = Number(route.query.categoryId)

  // 加载分类
  const catRes = await categoryAPI.getList()
  if (catRes.data) categories.value = catRes.data

  // 加载商品
  await loadGoods()
})

async function loadGoods() {
  loading.value = true
  try {
    const res = await goodsAPI.getList({
      page: currentPage.value,
      pageSize,
      categoryId: selectedCategory.value,
      keyword: keyword.value || undefined
    })
    if (res.data) {
      goodsList.value = res.data.records || []
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载商品列表失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadGoods()
}

function handleCategoryChange() {
  currentPage.value = 1
  loadGoods()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadGoods()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.goods-list-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.search-bar {
  background: #fff;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
}

.search-inner {
  max-width: 600px;
  margin: 0 auto;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  margin-bottom: 40px;
}
</style>
