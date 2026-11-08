<template>
  <div class="home-page">
    <Header />

    <!-- Banner -->
    <section class="banner">
      <div class="banner-content">
        <h1>校园二手交易平台</h1>
        <p>绿色环保，物尽其用 —— 让闲置物品找到新主人</p>
        <div class="banner-actions">
          <el-button type="primary" size="large" @click="$router.push('/goods')">
            <el-icon><Search /></el-icon>逛逛商品市场
          </el-button>
          <el-button size="large" @click="$router.push('/goods/publish')">
            <el-icon><Plus /></el-icon>发布闲置物品
          </el-button>
        </div>
      </div>
    </section>

    <div class="page-container">
      <!-- 系统公告：置顶横条，发布后立刻可见 -->
      <section v-if="notices.length > 0" class="notice-bar" aria-label="系统公告">
        <div class="notice-bar-head">
          <el-icon class="notice-bar-icon"><BellFilled /></el-icon>
          <span>系统公告</span>
        </div>
        <ul class="notice-bar-list">
          <li v-for="notice in notices.slice(0, 3)" :key="notice.id" class="notice-bar-item">
            <span class="notice-bar-item-title" :title="notice.title">{{ notice.title }}</span>
            <span class="notice-bar-item-content" :title="notice.content">{{ notice.content }}</span>
            <span class="notice-bar-item-time">{{ notice.createTime }}</span>
          </li>
        </ul>
      </section>

      <!-- 分类导航 -->
      <section class="section">
        <h2 class="section-title">商品分类</h2>
        <div class="category-grid">
          <el-card
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            shadow="hover"
            @click="goToCategory(cat.id)"
          >
            <el-icon :size="32" :color="getCategoryColor(cat.id)"><Goods /></el-icon>
            <span>{{ cat.categoryName }}</span>
          </el-card>
        </div>
      </section>

      <!-- 最新商品 -->
      <section class="section">
        <div class="section-header">
          <h2 class="section-title">最新商品</h2>
          <el-button text type="primary" @click="$router.push('/goods')">
            查看更多 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="loading" class="empty-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="goodsList.length === 0" class="empty-state">
          <el-icon><GoodsFilled /></el-icon>
          <p>暂无商品</p>
        </div>
        <div v-else class="goods-grid">
          <GoodsCard v-for="item in goodsList" :key="item.id" :goods="item" />
        </div>
      </section>
    </div>

    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { goodsAPI } from '@/api/goods'
import { categoryAPI } from '@/api/category'
import { noticeAPI } from '@/api/notice'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import type { Category, GoodsVO, Notice } from '@/api/types'

const router = useRouter()
const goodsList = ref<GoodsVO[]>([])
const categories = ref<Category[]>([])
const notices = ref<Notice[]>([])
const loading = ref(true)

onMounted(() => {
  loadGoods()
  loadCategories()
  loadNotices()
})

async function loadGoods() {
  try {
    const res = await goodsAPI.getList({ page: 1, pageSize: 8 })
    if (res.data) goodsList.value = res.data.records || []
  } catch (e) {
    console.error('加载商品失败', e)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await categoryAPI.getList()
    if (res.data) categories.value = res.data
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

async function loadNotices() {
  try {
    const res = await noticeAPI.getList()
    notices.value = res.data || []
  } catch (e) {
    console.error('加载公告失败', e)
  }
}

function goToCategory(id: number) {
  router.push({ path: '/goods', query: { categoryId: id.toString() } })
}

function getCategoryColor(_id: number): string {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9']
  return colors[(_id - 1) % colors.length]
}
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 60px 20px;
  text-align: center;
}

.banner-content h1 {
  font-size: 36px;
  margin-bottom: 12px;
}

.banner-content p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 24px;
}

.banner-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.category-card {
  cursor: pointer;
  text-align: center;
  padding: 20px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  transition: box-shadow 0.2s ease;
}

.category-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.notice-bar {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
  padding: 14px 18px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-left: 4px solid #e6a23c;
  border-radius: 8px;
}

.notice-bar-head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  font-weight: 600;
  color: #b88230;
  white-space: nowrap;
  line-height: 24px;
}

.notice-bar-icon {
  color: #e6a23c;
}

.notice-bar-list {
  flex: 1;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notice-bar-item {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
  font-size: 14px;
  line-height: 24px;
}

.notice-bar-item-title {
  flex: 0 0 auto;
  font-weight: 600;
  color: #303133;
}

.notice-bar-item-content {
  flex: 1;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-bar-item-time {
  flex: 0 0 auto;
  font-size: 12px;
  color: #909399;
}
</style>
