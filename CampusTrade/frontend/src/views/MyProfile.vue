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

        <el-form :model="form" ref="formRef" label-width="100px" class="profile-form">
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
            <el-input v-model="form.studentNo" />
          </el-form-item>
          <el-form-item label="学校" prop="school">
            <el-input v-model="form.school" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleUpdate">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userAPI } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { compressImage } from '@/utils/imageCompress'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { UserVO } from '@/api/types'
import type { FormInstance } from 'element-plus'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement>()
const userInfo = ref<UserVO | null>(null)
const submitting = ref(false)

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

const form = reactive({
  nickname: '',
  phone: '',
  email: '',
  studentNo: '',
  school: ''
})

onMounted(async () => {
  const res = await userAPI.getUserInfo()
  if (res.data) {
    userInfo.value = res.data
    form.nickname = res.data.nickname || ''
    form.phone = res.data.phone || ''
    form.email = res.data.email || ''
    form.studentNo = res.data.studentNo || ''
    form.school = res.data.school || ''
  }
})

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
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.profile-form {
  max-width: 500px;
}
</style>
