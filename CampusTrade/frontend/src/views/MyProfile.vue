<template>
  <div class="profile-page">
    <Header />
    <div class="page-container">
      <div class="profile-card">
        <h2 class="section-title">个人信息</h2>

        <div class="avatar-section">
          <el-avatar :size="80" :src="userInfo?.avatar || undefined">
            {{ displayName.charAt(0).toUpperCase() }}
          </el-avatar>
          <el-button size="small" @click="handleUploadAvatar">更换头像</el-button>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
        </div>

        <!-- 信用档案 -->
        <div class="credit-card">
          <div class="credit-item">
            <span class="credit-value">{{ summary.avgScore.toFixed(1) }}</span>
            <span class="credit-label">信用分（5分制）</span>
          </div>
          <div class="credit-item">
            <span class="credit-value">{{ summary.count }}</span>
            <span class="credit-label">收到评价</span>
          </div>
          <div class="credit-item">
            <span class="credit-value">{{ summary.goodRate }}%</span>
            <span class="credit-label">好评率</span>
          </div>
          <div class="credit-item school">
            <el-icon><School /></el-icon>
            <span>{{ userInfo?.school || '广州工商学院' }}</span>
            <span class="credit-label">学号 {{ userInfo?.studentNo || '未填写' }}</span>
          </div>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="profile-form">
          <el-form-item label="用户名">
            <el-input :model-value="userInfo?.username" disabled />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item label="学号" prop="studentNo">
            <el-input v-model="form.studentNo" maxlength="12" placeholder="12位学号，如 202310610320" />
          </el-form-item>
          <el-form-item label="学校" prop="school">
            <el-input v-model="form.school" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleUpdate">保存修改</el-button>
          </el-form-item>
        </el-form>

        <!-- 收到的评价 -->
        <div class="review-section">
          <h3 class="review-title">我收到的评价</h3>
          <div v-if="reviews.length === 0" class="review-empty">暂无评价，完成交易后可获得买家/卖家的评价</div>
          <div v-else class="review-list">
            <div v-for="r in reviews" :key="r.id" class="review-item">
              <div class="review-head">
                <span class="review-user">{{ r.raterName || '匿名用户' }}</span>
                <el-rate :model-value="r.score" disabled size="small" />
                <span class="review-time">{{ r.createTime }}</span>
              </div>
              <p class="review-content">{{ r.content || '（未填写评价内容）' }}</p>
              <p class="review-goods" v-if="r.goodsTitle">商品：{{ r.goodsTitle }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { School } from '@element-plus/icons-vue'
import { userAPI } from '@/api/user'
import { reviewAPI } from '@/api/review'
import { useUserStore } from '@/stores/user'
import { compressImage } from '@/utils/imageCompress'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { Review, ReviewSummary, UserVO } from '@/api/types'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement>()
const userInfo = ref<UserVO | null>(null)
const submitting = ref(false)

const summary = ref<ReviewSummary>({ avgScore: 0, count: 0, goodCount: 0, goodRate: 0 })
const reviews = ref<Review[]>([])

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

const form = reactive({
  nickname: '',
  phone: '',
  email: '',
  studentNo: '',
  school: '广州工商学院'
})

/** 学号规则：12 位数字 = 4 位入学年份 + 6 位专业班级 + 2 位班内学号 */
const validateStudentNo = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback()
    return
  }
  if (!/^20\d{10}$/.test(value)) {
    callback(new Error('学号应为12位数字，如 202310610320'))
    return
  }
  const year = Number(value.substring(0, 4))
  const now = new Date().getFullYear()
  if (year < 2010 || year > now + 1) {
    callback(new Error('学号中的入学年份不合理'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  studentNo: [{ validator: validateStudentNo, trigger: 'blur' }]
}

onMounted(async () => {
  const res = await userAPI.getUserInfo()
  if (res.data) {
    userInfo.value = res.data
    form.nickname = res.data.nickname || ''
    form.phone = res.data.phone || ''
    form.email = res.data.email || ''
    form.studentNo = res.data.studentNo || ''
    form.school = res.data.school || '广州工商学院'
    loadCredit()
  }
})

/** 加载信用分和收到的评价 */
async function loadCredit() {
  if (!userInfo.value) return
  try {
    const [sumRes, listRes] = await Promise.all([
      reviewAPI.summary(userInfo.value.id),
      reviewAPI.pageByUser(userInfo.value.id, { page: 1, pageSize: 20 })
    ])
    if (sumRes.data) summary.value = sumRes.data
    if (listRes.data) reviews.value = listRes.data.records || []
  } catch (e) {
    // 静默失败，不影响资料编辑
  }
}

function handleUploadAvatar() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    // 先压缩再上传
    const compressedFile = await compressImage(file, { maxWidth: 400, maxHeight: 400, quality: 0.8 })
    const uploadRes = await userAPI.uploadAvatar(compressedFile)
    if (uploadRes.data) {
      // 再将头像URL保存到用户记录
      await userAPI.updateAvatar(uploadRes.data)
      userInfo.value!.avatar = uploadRes.data
      userStore.avatar = uploadRes.data
      ElMessage.success('头像更新成功')
    }
  } catch (e) {}
}

async function handleUpdate() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  submitting.value = true
  try {
    await userAPI.updateUserInfo({
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      studentNo: form.studentNo || undefined,
      school: form.school || undefined
    })
    ElMessage.success('修改成功')
    userStore.fetchUserInfo()
    const res = await userAPI.getUserInfo()
    if (res.data) userInfo.value = res.data
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  margin-top: 20px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.credit-card {
  display: flex;
  gap: 32px;
  align-items: center;
  flex-wrap: wrap;
  background: linear-gradient(135deg, #ecf5ff 0%, #f6f8fb 100%);
  border-radius: 10px;
  padding: 16px 24px;
  margin-bottom: 24px;
}

.credit-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 90px;
}

.credit-item.school {
  flex-direction: row;
  gap: 6px;
  color: #409eff;
  border-left: 1px solid #d9ecff;
  padding-left: 24px;
}

.credit-value {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.credit-label {
  font-size: 12px;
  color: #909399;
}

.profile-form {
  max-width: 500px;
}

.review-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.review-title {
  font-size: 18px;
  margin-bottom: 16px;
}

.review-empty {
  color: #909399;
  font-size: 14px;
}

.review-item {
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-user {
  font-weight: 600;
  font-size: 14px;
}

.review-time {
  margin-left: auto;
  color: #c0c4cc;
  font-size: 12px;
}

.review-content {
  margin: 6px 0 0;
  font-size: 14px;
  color: #606266;
}

.review-goods {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>