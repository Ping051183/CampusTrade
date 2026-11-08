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
            <div class="order-tags">
              <el-tag v-if="order.refundStatus === 1" type="warning" size="small">退款中</el-tag>
              <el-tag v-else-if="order.refundStatus === 2" type="danger" size="small">已退款</el-tag>
              <el-tag v-else-if="order.refundStatus === 3" type="info" size="small">退款被拒</el-tag>
              <el-tag :type="orderStatusTag(order.status)" size="small">
                {{ orderStatusText(order.status) }}
              </el-tag>
            </div>
          </div>
          <div class="order-body">
            <div class="order-info">
              <p>金额：<span class="order-amount">¥{{ order.amount }}</span></p>
              <p v-if="order.remark">备注：{{ order.remark }}</p>
              <p v-if="order.refundReason" class="refund-reason">退款原因：{{ order.refundReason }}</p>
              <p class="order-time">创建时间：{{ order.createTime }}</p>
            </div>
          </div>
          <div class="order-actions">
            <template v-if="orderType === 'buy'">
              <el-button v-if="order.status === 0" size="small" type="primary" @click="openPay(order)">
                去支付
              </el-button>
              <el-button v-if="order.status === 0" size="small" type="danger" @click="handleCancel(order.id)">
                取消订单
              </el-button>
              <span v-if="order.status === 0" class="order-tip">30 分钟内未支付将自动取消</span>
              <span v-if="order.status === 1 && order.refundStatus === 0" class="order-tip">等待卖家发货</span>
              <el-button v-if="order.status === 2" size="small" type="primary" @click="handleConfirm(order.id)">
                确认收货
              </el-button>
              <el-button
                v-if="(order.status === 1 || order.status === 2) && order.refundStatus === 0"
                size="small"
                @click="openRefund(order)"
              >
                申请退款
              </el-button>
              <span v-if="order.refundStatus === 1" class="order-tip warning">退款申请处理中，请等待卖家处理</span>
              <el-button
                v-if="order.status === 3 && order.isRated === 0"
                size="small"
                type="warning"
                @click="openReview(order)"
              >
                评价
              </el-button>
              <el-tag v-if="order.status === 3 && order.isRated === 1" type="success" size="small">已评价</el-tag>
            </template>
            <template v-else>
              <span v-if="order.status === 0" class="order-tip">等待买家付款</span>
              <el-button
                v-if="order.status === 1 && order.refundStatus === 0"
                size="small"
                type="primary"
                @click="handleShip(order.id)"
              >
                发货
              </el-button>
              <span v-if="order.status === 2 && order.refundStatus === 0" class="order-tip">等待买家确认收货</span>
              <template v-if="order.refundStatus === 1">
                <el-button size="small" type="danger" @click="handleAgreeRefund(order.id)">同意退款</el-button>
                <el-button size="small" @click="handleRejectRefund(order.id)">拒绝退款</el-button>
              </template>
              <el-button
                v-if="order.status === 3 && order.sellerRated === 0"
                size="small"
                type="warning"
                @click="openReview(order)"
              >
                评价买家
              </el-button>
            </template>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 模拟支付弹窗 -->
    <el-dialog v-model="payVisible" title="订单支付" width="420px" :close-on-click-modal="false">
      <div class="pay-box" v-if="payOrder">
        <p>订单编号：{{ payOrder.orderNo }}</p>
        <p class="pay-amount">应付金额：<span>¥{{ payOrder.amount }}</span></p>
        <p class="pay-label">选择支付方式</p>
        <el-radio-group v-model="payMethod">
          <el-radio label="balance">校园钱包（模拟）</el-radio>
          <el-radio label="wechat">微信支付（模拟）</el-radio>
          <el-radio label="alipay">支付宝（模拟）</el-radio>
        </el-radio-group>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="演示环境为模拟支付，点击「确认支付」即视为支付成功，不会产生真实扣款。"
          style="margin-top: 16px"
        />
      </div>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">确认支付 ¥{{ payOrder?.amount }}</el-button>
      </template>
    </el-dialog>

    <!-- 申请退款弹窗 -->
    <el-dialog v-model="refundVisible" title="申请退款" width="460px" :close-on-click-modal="false">
      <div v-if="refundOrder">
        <p>订单编号：{{ refundOrder.orderNo }}</p>
        <p class="pay-amount">退款金额：<span>¥{{ refundOrder.amount }}</span></p>
        <el-input
          v-model="refundReason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请填写退款原因（选填），如：商品与描述不符"
          style="margin-top: 12px"
        />
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refunding" @click="handleApplyRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="交易评价" width="460px" :close-on-click-modal="false">
      <div v-if="reviewOrder">
        <p class="review-label">本次交易体验如何？</p>
        <el-rate v-model="reviewScore" :texts="['很差', '较差', '一般', '满意', '非常满意']" show-text />
        <el-input
          v-model="reviewContent"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="说说你的交易感受（选填）"
          style="margin-top: 12px"
        />
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview">提交评价</el-button>
      </template>
    </el-dialog>

    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List } from '@element-plus/icons-vue'
import { orderAPI } from '@/api/order'
import { reviewAPI } from '@/api/review'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { Order } from '@/api/types'

const orders = ref<Order[]>([])
const loading = ref(true)
const orderType = ref('buy')

/** 支付弹窗状态 */
const payVisible = ref(false)
const payOrder = ref<Order | null>(null)
const payMethod = ref('balance')
const paying = ref(false)

/** 退款弹窗状态 */
const refundVisible = ref(false)
const refundOrder = ref<Order | null>(null)
const refundReason = ref('')
const refunding = ref(false)

/** 评价弹窗状态 */
const reviewVisible = ref(false)
const reviewOrder = ref<Order | null>(null)
const reviewScore = ref(5)
const reviewContent = ref('')
const reviewing = ref(false)

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
    ElMessage.success('已确认收货，可以评价本次交易了')
    loadOrders()
  } catch (e) {}
}

/** 打开支付弹窗 */
function openPay(order: Order) {
  payOrder.value = order
  payMethod.value = 'balance'
  payVisible.value = true
}

/** 模拟支付 */
async function handlePay() {
  if (!payOrder.value) return
  paying.value = true
  try {
    await orderAPI.pay(payOrder.value.id)
    ElMessage.success('支付成功，等待卖家发货')
    payVisible.value = false
    loadOrders()
  } catch (e) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    paying.value = false
  }
}

/** 卖家发货 */
async function handleShip(id: number) {
  try {
    await ElMessageBox.confirm('确认已经发出商品吗？', '提示')
    await orderAPI.ship(id)
    ElMessage.success('发货成功')
    loadOrders()
  } catch (e) {}
}

/** 打开退款弹窗 */
function openRefund(order: Order) {
  refundOrder.value = order
  refundReason.value = ''
  refundVisible.value = true
}

/** 买家申请退款 */
async function handleApplyRefund() {
  if (!refundOrder.value) return
  refunding.value = true
  try {
    await orderAPI.applyRefund(refundOrder.value.id, refundReason.value || undefined)
    ElMessage.success('退款申请已提交，等待卖家处理')
    refundVisible.value = false
    loadOrders()
  } catch (e) {
  } finally {
    refunding.value = false
  }
}

/** 卖家同意退款 */
async function handleAgreeRefund(id: number) {
  try {
    await ElMessageBox.confirm('同意退款后订单将取消，商品恢复在售，确定吗？', '提示')
    await orderAPI.agreeRefund(id)
    ElMessage.success('已同意退款')
    loadOrders()
  } catch (e) {}
}

/** 卖家拒绝退款 */
async function handleRejectRefund(id: number) {
  try {
    await ElMessageBox.confirm('确定拒绝该退款申请吗？', '提示')
    await orderAPI.rejectRefund(id)
    ElMessage.success('已拒绝退款')
    loadOrders()
  } catch (e) {}
}

/** 打开评价弹窗 */
function openReview(order: Order) {
  reviewOrder.value = order
  reviewScore.value = 5
  reviewContent.value = ''
  reviewVisible.value = true
}

/** 提交评价 */
async function handleReview() {
  if (!reviewOrder.value) return
  reviewing.value = true
  try {
    await reviewAPI.create({
      orderId: reviewOrder.value.id,
      score: reviewScore.value,
      content: reviewContent.value || undefined
    })
    ElMessage.success('评价成功，感谢你的反馈')
    reviewVisible.value = false
    loadOrders()
  } catch (e) {
  } finally {
    reviewing.value = false
  }
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

.order-tags {
  display: flex;
  gap: 6px;
  align-items: center;
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

.refund-reason {
  color: #e6a23c;
  font-size: 13px;
}

.order-time {
  color: #909399;
  font-size: 12px;
}

.order-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.order-tip {
  font-size: 13px;
  color: #909399;
}

.order-tip.warning {
  color: #e6a23c;
}

.pay-box p {
  margin-bottom: 8px;
  font-size: 14px;
}

.pay-amount span {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.pay-label {
  margin-top: 16px;
  color: #606266;
}

.review-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

/* 列表区域保持最小高度，避免切换标签或加载完成时页面高度突变导致跳动 */
.order-list,
.empty-state {
  min-height: 360px;
}
</style>