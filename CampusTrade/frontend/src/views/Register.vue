<template>
  <div class="register-page">
    <Header />
    <div class="page-container">
      <div class="form-container">
        <h2 class="form-title">创建账号</h2>
        <p class="form-subtitle">加入校园二手交易平台</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="3-50个字符" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="6-50个字符" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="选填" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="选填" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="选填" />
          </el-form-item>
          <el-form-item label="学号" prop="studentNo">
            <el-input v-model="form.studentNo" placeholder="选填" />
          </el-form-item>
          <el-form-item label="学校" prop="school">
            <el-input v-model="form.school" placeholder="选填" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleRegister">
              注册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
  studentNo: '',
  school: ''
})

const validatePass = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度应在3-50之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度应在6-50之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const success = await userStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      studentNo: form.studentNo || undefined,
      school: form.school || undefined
    })
    if (success) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }
  } catch (e) {
    // Error handled in interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.form-container {
  margin-top: 40px;
  margin-bottom: 40px;
  max-width: 480px;
}

.form-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 8px;
}

.form-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
}

.form-footer {
  text-align: center;
  color: #909399;
  margin-top: 16px;
}

.form-footer a {
  color: #409eff;
  text-decoration: none;
}
</style>
