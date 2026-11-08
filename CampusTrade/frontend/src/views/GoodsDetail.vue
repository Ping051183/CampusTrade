<template>
  <div class="detail-page">
    <Header />

    <div class="page-container" v-loading="loading">
      <el-button text @click="$router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>

      <div v-if="goods" class="detail-content">
        <div class="detail-main">
          <!-- 图片展示 -->
          <div class="image-section">
            <el-image
              :src="currentImage"
              fit="contain"
              class="main-image"
              :preview-src-list="imageList"
              preview-teleported
            />
            <div class="image-thumbs" v-if="imageList.length > 1">
              <el-image
                v-for="(img, idx) in imageList"
                :key="idx"
                :src="img"
                fit="cover"
                class="thumb"
                :class="{ active: currentIdx === idx }"
                @click="currentIdx = idx"
              />
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="info-section">
            <h1 class="goods-title">{{ goods.title }}</h1>
            <div class="tags-row">
              <el-tag>{{ goods.categoryName }}</el-tag>
              <el-tag v-if="goods.status === 1" type="success">在售</el-tag>
            </div>
            <div class="price-row">
              <span class="price">¥{{ goods.price }}</span>
              <span class="original-price" v-if="goods.originalPrice">¥{{ goods.originalPrice }}</span>
            </div>
            <div class="meta-row">
              <span><el-icon><View /></el-icon> {{ goods.viewCount }} 次浏览</span>
              <span><el-icon><Star /></el-icon> {{ goods.favoriteCount }} 人收藏</span>
              <span><el-icon><Clock /></el-icon> {{ goods.createTime }}</span>
            </div>
            <div class="seller-info">
              <el-avatar :size="40" :src="goods.sellerAvatar || undefined" />
              <div class="seller-detail">
                <span class="seller-name">{{ goods.sellerName }}</span>
                <span class="seller-school" v-if="goods.sellerSchool">{{ goods.sellerSchool }}</span>
                <span class="seller-credit" v-if="sellerCredit && sellerCredit.count > 0">
                  <el-icon><Star /></el-icon>
                  信用 {{ sellerCredit.avgScore.toFixed(1) }} 分 · 好评率 {{ sellerCredit.goodRate }}% · {{ sellerCredit.count }} 条评价
                </span>
              </div>
            </div>
            <el-divider />
            <div class="description">
              <h3>商品描述</h3>
              <p>{{ goods.description || '暂无描述' }}</p>
            </div>
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                :disabled="goods.status !== 1 || isOwner"
                @click="handleBuy"
              >
                <el-icon><ShoppingCart /></el-icon> 立即购买
              </el-button>
              <el-button
                v-if="!isOwner && userStore.isLoggedIn"
                size="large"
                type="success"
                @click="handleContact"
              >
                <el-icon><ChatDotSquare /></el-icon> 联系卖家
              </el-button>
              <el-button
                size="large"
                :type="goods.isFavorited ? 'warning' : 'default'"
                @click="handleFavorite"
              >
                <el-icon><Star /></el-icon>
                {{ goods.isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>

        <!-- 评论区 -->
        <el-divider />
        <div class="comments-section">
          <h3>商品评论</h3>
          <div class="comment-input">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="说点什么..."
              maxlength="500"
              show-word-limit
            />
            <el-button type="primary" style="margin-top: 12px" @click="handleComment">
              发表评论
            </el-button>
          </div>
          <div class="comment-list" v-if="comments.length > 0">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="comment-header">
                <el-avatar :size="24">{{ c.userId }}</el-avatar>
                <span class="comment-user">用户{{ c.userId }}</span>
                <span class="comment-time">{{ c.createTime }}</span>
              </div>
              <p class="comment-content">{{ c.content }}</p>
            </div>
          </div>
          <div v-else class="empty-state" style="padding: 20px">
            <p>暂无评论</p>
          </div>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, View, Star, Clock, ShoppingCart, ChatDotSquare } from '@element-plus/icons-vue'
import { goodsAPI } from '@/api/goods'
import { favoriteAPI } from '@/api/favorite'
import { commentAPI } from '@/api/comment'
import { orderAPI } from '@/api/order'
import { reviewAPI } from '@/api/review'
import { useUserStore } from '@/stores/user'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { GoodsVO, Comment, ReviewSummary } from '@/api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const goods = ref<GoodsVO | null>(null)
const comments = ref<Comment[]>([])
const sellerCredit = ref<ReviewSummary | null>(null)
const loading = ref(true)
const commentContent = ref('')
const currentIdx = ref(0)

const goodsId = computed(() => Number(route.params.id))

const imageList = computed(() => {
  if (!goods.value?.images) return []
  return goods.value.images.split(',').filter(Boolean)
})

const currentImage = computed(() => {
  return imageList.value[currentIdx.value] || ''
})

const isOwner = computed(() => {
  return goods.value?.sellerId === userStore.userId
})

onMounted(async () => {
  await loadGoods()
})

async function loadGoods() {
  loading.value = true
  try {
    const goodsRes = await goodsAPI.getDetail(goodsId.value)
    if (goodsRes.data) {
      goods.value = goodsRes.data
      loadSellerCredit(goodsRes.data.sellerId)
    }
  } catch (e) {
    console.error('加载商品详情失败', e)
  } finally {
    loading.value = false
  }
  loadComments()
}

/** 评论加载与详情解耦：失败不影响商品信息展示 */
async function loadComments() {
  try {
    const commentRes = await commentAPI.getList(goodsId.value)
    if (commentRes.data) comments.value = commentRes.data
  } catch (e) {
    // 评论加载失败静默
  }
}

/** 加载卖家信用分（不影响详情主流程，失败静默） */
async function loadSellerCredit(sellerId?: number) {
  if (!sellerId) return
  try {
    const res = await reviewAPI.summary(sellerId)
    if (res.data) sellerCredit.value = res.data
  } catch (e) {
    // 静默
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    if (goods.value?.isFavorited) {
      await favoriteAPI.cancel(goodsId.value)
      goods.value!.isFavorited = false
      goods.value!.favoriteCount--
      ElMessage.success('已取消收藏')
    } else {
      await favoriteAPI.add(goodsId.value)
      goods.value!.isFavorited = true
      goods.value!.favoriteCount++
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    // Error handled in interceptor
  }
}

async function handleBuy() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    ElMessageBox.confirm('确认购买该商品？下单后请在我的订单中完成支付。', '购买确认').then(async () => {
      const res = await orderAPI.create(goodsId.value)
      if (res.data) {
        ElMessage.success('下单成功，请尽快完成支付')
        router.push(`/my/orders`)
      }
    }).catch(() => {})
  } catch (e) {
    // Error handled
  }
}

function handleContact() {
  router.push({
    path: '/chat',
    query: {
      userId: goods.value?.sellerId,
      userName: goods.value?.sellerName,
      goodsId: goods.value?.id,
      goodsTitle: goods.value?.title
    }
  })
}

async function handleComment() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await commentAPI.add({
      goodsId: goodsId.value,
      content: commentContent.value
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    // 重新加载评论
    const res = await commentAPI.getList(goodsId.value)
    if (res.data) comments.value = res.data
  } catch (e) {
    // Error handled
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.back-btn {
  margin-bottom: 16px;
}

.detail-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
}

.image-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-image {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  background: #f5f7fa;
}

.image-thumbs {
  display: flex;
  gap: 8px;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.thumb.active {
  border-color: #409eff;
}

.goods-title {
  font-size: 22px;
  margin-bottom: 12px;
}

.tags-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.price-row {
  margin-bottom: 16px;
}

.price {
  font-size: 28px;
  font-weight: 700;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: #c0c4cc;
  text-decoration: line-through;
  margin-left: 12px;
}

.meta-row {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 20px;
}

.meta-row span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.seller-detail {
  display: flex;
  flex-direction: column;
}

.seller-name {
  font-weight: 600;
}

.seller-school {
  font-size: 12px;
  color: #909399;
}

.seller-credit {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
}

.description h3 {
  margin-bottom: 8px;
  font-size: 16px;
}

.description p {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.comments-section {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  margin-top: 20px;
}

.comments-section h3 {
  margin-bottom: 16px;
  font-size: 16px;
}

.comment-input {
  margin-bottom: 24px;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-user {
  font-weight: 500;
  font-size: 13px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.comment-content {
  color: #606266;
  line-height: 1.6;
}
</style>
