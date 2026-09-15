<template>
  <div class="orders-page">
    <Header />
    <div class="page-container">
      <h2 class="section-title">我的订单</h2>

      <el-tabs v-model="orderType" @tab-change="loadOrders">
        <el-tab-pane label="我购买的" name="buy" />
        <el-tab-pane label="我出售的" name="sell" />
      </el-tabs>

      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="orders.length === 0" class="empty-state">
        <el-icon :size="60"><List /></el-icon>
        <p>暂无订单</p>
      </div>
      <div v-else class="order-list">
        <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
          <div class="order-header">
            <span class="order-no">订单编号：{{ order.orderNo }}</span>
            <el-tag :type="orderStatusTag(order.status)" size="small">
              {{ orderStatusText(order.status) }}
            </el-tag>
          </div>
          <div class="order-body">
            <div class="order-info">
              <p>金额：<span class="order-amount">¥{{ order.amount }}</span></p>
              <p v-if="order.remark">备注：{{ order.remark }}</p>
              <p class="order-time">创建时间：{{ order.createTime }}</p>
            </div>
          </div>
          <div class="order-actions">
            <el-button v-if="order.status === 0 && orderType === 'buy'" size="small" type="danger" @click="handleCancel(order.id)">
              取消订单
            </el-button>
            <el-button v-if="order.status === 1 && orderType === 'buy'" size="small" type="primary" @click="handleConfirm(order.id)">
              确认收货
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List } from '@element-plus/icons-vue'
import { orderAPI } from '@/api/order'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { Order } from '@/api/types'

const orders = ref<Order[]>([])
const loading = ref(true)
const orderType = ref('buy')

onMounted(() => {
  loadOrders()
})

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: 1, pageSize: 20 }
    const api = orderType.value === 'buy' ? orderAPI.getMyBuyOrders(params) : orderAPI.getMySellOrders(params)
    const res = await api
    if (res.data) orders.value = res.data.records || []
  } catch (e) {
    console.error('加载订单失败', e)
  } finally {
    loading.value = false
  }
}

function orderStatusTag(status: number) {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'primary'
    case 2: return 'info'
    case 3: return 'success'
    case 4: return 'danger'
    case 5: return ''
    default: return 'info'
  }
}

function orderStatusText(status: number) {
  switch (status) {
    case 0: return '待付款'
    case 1: return '已付款'
    case 2: return '已发货'
    case 3: return '已收货'
    case 4: return '已取消'
    case 5: return '已完成'
    default: return '未知'
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '提示')
    await orderAPI.cancel(id)
    ElMessage.success('已取消')
    loadOrders()
  } catch (e) {}
}

async function handleConfirm(id: number) {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '提示')
    await orderAPI.confirm(id)
    ElMessage.success('已确认收货')
    loadOrders()
  } catch (e) {}
}
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.order-card {
  margin-bottom: 16px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-no {
  font-size: 13px;
  color: #909399;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info p {
  margin-bottom: 4px;
  font-size: 14px;
}

.order-amount {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.order-time {
  color: #909399;
  font-size: 12px;
}

.order-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
