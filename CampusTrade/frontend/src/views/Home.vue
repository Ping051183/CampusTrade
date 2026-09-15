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

      <!-- 公告 -->
      <section v-if="notices.length > 0" class="section">
        <h2 class="section-title">系统公告</h2>
        <el-card v-for="notice in notices" :key="notice.id" class="notice-card" shadow="hover">
          <div class="notice-header">
            <el-tag size="small" type="info">公告</el-tag>
            <span class="notice-title">{{ notice.title }}</span>
            <span class="notice-time">{{ notice.createTime }}</span>
          </div>
          <p class="notice-content">{{ notice.content }}</p>
        </el-card>
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

onMounted(async () => {
  try {
    const [goodsRes, catRes, noticeRes] = await Promise.all([
      goodsAPI.getList({ page: 1, pageSize: 8 }),
      categoryAPI.getList(),
      noticeAPI.getList()
    ])
    if (goodsRes.data) goodsList.value = goodsRes.data.records || []
    if (catRes.data) categories.value = catRes.data
    if (noticeRes.data) notices.value = noticeRes.data
  } catch (e) {
    console.error('加载首页数据失败', e)
  } finally {
    loading.value = false
  }
})

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
  transition: transform 0.2s;
}

.category-card:hover {
  transform: translateY(-3px);
}

.notice-card {
  margin-bottom: 12px;
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.notice-title {
  font-weight: 600;
  flex: 1;
}

.notice-time {
  color: #909399;
  font-size: 12px;
}

.notice-content {
  color: #606266;
  line-height: 1.6;
}
</style>
