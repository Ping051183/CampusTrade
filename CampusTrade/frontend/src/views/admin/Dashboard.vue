<template>
  <div class="dashboard-page">
    <h2 class="page-title">数据概览</h2>

    <div class="stat-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-value">{{ stats.userCount || 0 }}</div>
        <div class="stat-label">用户总数</div>
        <el-icon class="stat-icon" :size="40" color="#409eff"><User /></el-icon>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-value">{{ stats.goodsOnSale || 0 }}</div>
        <div class="stat-label">在售商品</div>
        <el-icon class="stat-icon" :size="40" color="#67c23a"><Goods /></el-icon>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-value">{{ stats.goodsPending || 0 }}</div>
        <div class="stat-label">待审核商品</div>
        <el-icon class="stat-icon" :size="40" color="#e6a23c"><Clock /></el-icon>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-value">¥{{ stats.amountTotal || 0 }}</div>
        <div class="stat-label">总交易额</div>
        <el-icon class="stat-icon" :size="40" color="#f56c6c"><Money /></el-icon>
      </el-card>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="detail-card" shadow="hover">
          <h3>订单统计</h3>
          <div class="detail-row">
            <span>今日订单数</span>
            <span class="detail-value">{{ stats.orderToday || 0 }}</span>
          </div>
          <div class="detail-row">
            <span>今日交易额</span>
            <span class="detail-value">¥{{ stats.amountToday || 0 }}</span>
          </div>
          <div class="detail-row">
            <span>总订单数</span>
            <span class="detail-value">{{ stats.orderTotal || 0 }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="detail-card" shadow="hover">
          <h3>快速操作</h3>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/admin/goods')">商品审核</el-button>
            <el-button type="warning" @click="$router.push('/admin/reports')">举报处理</el-button>
            <el-button type="success" @click="$router.push('/admin/notices')">发布公告</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { User, Goods, Clock, Money } from '@element-plus/icons-vue'
import { adminAPI } from '@/api/admin'

interface DashboardStats {
  userCount?: number
  goodsOnSale?: number
  goodsPending?: number
  orderToday?: number
  amountToday?: number
  orderTotal?: number
  amountTotal?: number
}

const stats = ref<DashboardStats>({})

onMounted(async () => {
  try {
    const res = await adminAPI.getDashboard()
    if (res.data) stats.value = res.data
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1000px;
}

.page-title {
  font-size: 20px;
  margin-bottom: 20px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-icon {
  position: absolute;
  right: 20px;
  top: 20px;
  opacity: 0.6;
}

.detail-card {
  margin-bottom: 20px;
}

.detail-card h3 {
  font-size: 16px;
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-value {
  font-weight: 600;
  color: #409eff;
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
