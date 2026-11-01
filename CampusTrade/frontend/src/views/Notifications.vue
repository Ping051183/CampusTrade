<template>
  <div class="notice-page">
    <Header />
    <div class="page-container">
      <div class="notice-header">
        <h2 class="section-title">系统通知</h2>
        <el-button size="small" :disabled="notices.length === 0" @click="handleMarkAll">全部标记已读</el-button>
      </div>

      <div v-if="loading" class="empty-state">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="notices.length === 0" class="empty-state">
        <el-icon :size="60"><Bell /></el-icon>
        <p>暂无通知</p>
      </div>
      <div v-else class="notice-list">
        <div
          v-for="n in notices"
          :key="n.id"
          class="notice-item"
          :class="{ unread: n.isRead === 0 }"
          @click="handleRead(n)"
        >
          <div class="notice-dot" v-if="n.isRead === 0"></div>
          <div class="notice-main">
            <div class="notice-title-row">
              <el-tag size="small" :type="typeTag(n.type)" effect="plain">{{ typeText(n.type) }}</el-tag>
              <span class="notice-title">{{ n.title }}</span>
              <span class="notice-time">{{ n.createTime }}</span>
            </div>
            <p class="notice-content">{{ n.content }}</p>
          </div>
        </div>
      </div>

      <div class="pager" v-if="total > pageSize">
        <el-pagination
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { notificationAPI } from '@/api/notification'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { Notification as Notice } from '@/api/types'

const notices = ref<Notice[]>([])
const loading = ref(true)
const page = ref(1)
const pageSize = 20
const total = ref(0)

onMounted(loadNotices)

async function loadNotices() {
  loading.value = true
  try {
    const res = await notificationAPI.list({ page: page.value, pageSize })
    if (res.data) {
      notices.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    // 静默处理
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadNotices()
}

async function handleRead(n: Notice) {
  if (n.isRead === 1) return
  try {
    await notificationAPI.markRead(n.id)
    n.isRead = 1
  } catch (e) {}
}

async function handleMarkAll() {
  try {
    await notificationAPI.markAllRead()
    notices.value.forEach((n) => (n.isRead = 1))
    ElMessage.success('已全部标记为已读')
  } catch (e) {}
}

function typeText(type: string) {
  const map: Record<string, string> = {
    ORDER_CREATED: '新订单',
    ORDER_PAID: '付款',
    ORDER_SHIPPED: '发货',
    ORDER_RECEIVED: '收货',
    ORDER_CANCELLED: '取消',
    REFUND_APPLY: '退款申请',
    REFUND_AGREED: '退款完成',
    REFUND_REJECTED: '退款拒绝',
    REVIEW_RECEIVED: '评价'
  }
  return map[type] || '通知'
}

function typeTag(type: string) {
  if (type.startsWith('REFUND')) return 'warning'
  if (type === 'REVIEW_RECEIVED') return 'success'
  if (type === 'ORDER_CANCELLED') return 'info'
  return 'primary'
}
</script>

<style scoped>
.notice-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notice-list {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.notice-item {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
  transition: background 0.2s;
}

.notice-item:hover {
  background: #f7f9fc;
}

.notice-item.unread {
  background: #f0f7ff;
}

.notice-dot {
  width: 8px;
  height: 8px;
  margin-top: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.notice-time {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
}

.notice-content {
  margin: 6px 0 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>