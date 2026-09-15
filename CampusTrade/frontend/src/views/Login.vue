<template>
  <div class="login-page">
    <Header />
    <div class="page-container">
      <div class="form-container">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">登录校园二手交易平台</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="login-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleLogin">
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const success = await userStore.login({ username: form.username, password: form.password })
    if (success) {
      ElMessage.success('登录成功')
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    }
  } catch (e) {
    // 错误已在请求拦截中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.form-container {
  margin-top: 60px;
  max-width: 420px;
}

.form-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 8px;
}

.form-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}

.login-form {
  margin-top: 20px;
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
