<template>
  <div class="favorites-page">
    <Header />
    <div class="page-container">
      <h2 class="section-title">我的收藏</h2>

      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="goodsList.length === 0" class="empty-state">
        <el-icon :size="60"><Star /></el-icon>
        <p>还没有收藏任何商品</p>
        <el-button type="primary" @click="$router.push('/goods')">去逛逛</el-button>
      </div>
      <div v-else class="goods-grid">
        <GoodsCard v-for="item in goodsList" :key="item.id" :goods="item" />
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Star } from '@element-plus/icons-vue'
import { favoriteAPI } from '@/api/favorite'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import GoodsCard from '@/components/GoodsCard.vue'
import type { Goods } from '@/api/types'

const goodsList = ref<Goods[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await favoriteAPI.getMyFavorites()
    if (res.data) goodsList.value = res.data
  } catch (e) {
    console.error('加载收藏失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.favorites-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
