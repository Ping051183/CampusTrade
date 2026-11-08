<template>
  <el-card class="goods-card" shadow="hover" @click="goToDetail">
    <div class="goods-image">
      <el-image
        :src="firstImage"
        fit="cover"
        lazy
      >
        <template #error>
          <div class="image-placeholder">
            <el-icon :size="32"><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <div class="goods-status" v-if="goods.status !== undefined && goods.status !== 1">
        <el-tag :type="statusTagType" size="small" effect="dark">
          {{ statusText }}
        </el-tag>
      </div>
    </div>
    <div class="goods-info">
      <h3 class="goods-title">{{ goods.title }}</h3>
      <div class="goods-category" v-if="goods.categoryName">
        <el-tag size="small" type="info">{{ goods.categoryName }}</el-tag>
      </div>
      <div class="goods-price-row">
        <span class="goods-price">¥{{ goods.price }}</span>
        <span class="goods-original-price" v-if="goods.originalPrice">¥{{ goods.originalPrice }}</span>
      </div>
      <div class="goods-meta">
        <span class="goods-seller">
          <el-avatar :size="20" :src="goods.sellerAvatar || undefined" />
          {{ goods.sellerName || '未知卖家' }}
        </span>
        <span class="goods-views">
          <el-icon><View /></el-icon> {{ goods.viewCount || 0 }}
        </span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, View } from '@element-plus/icons-vue'
import type { GoodsVO } from '@/api/types'

const props = defineProps<{
  goods: GoodsVO
}>()

const router = useRouter()

const firstImage = computed(() => {
  if (props.goods.images) {
    return props.goods.images.split(',')[0]
  }
  return ''
})

const statusTagType = computed(() => {
  switch (props.goods.status) {
    case 0: return 'warning'
    case 2: return 'info'
    case 3: return 'success'
    case 4: return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (props.goods.status) {
    case 0: return '待审核'
    case 1: return '在售'
    case 2: return '已下架'
    case 3: return '已售出'
    case 4: return '已驳回'
    default: return '未知'
  }
})

function goToDetail() {
  router.push(`/goods/detail/${props.goods.id}`)
}
</script>

<style scoped>
.goods-card {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  /* 只过渡阴影：hover 不做位移，避免卡片在鼠标停留时持续"上浮" */
  transition: box-shadow 0.2s ease;
}

.goods-image {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f5f7fa;
}

.goods-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f7fa;
}

.goods-status {
  position: absolute;
  top: 8px;
  right: 8px;
}

.goods-info {
  padding: 12px;
}

.goods-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 2.8em;
}

.goods-category {
  margin-bottom: 8px;
}

.goods-price-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.goods-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.goods-original-price {
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.goods-seller {
  display: flex;
  align-items: center;
  gap: 4px;
}

.goods-views {
  display: flex;
  align-items: center;
  gap: 2px;
}
</style>
