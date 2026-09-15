<template>
  <div class="admin-layout">
    <!-- 顶部导航 -->
    <el-header class="admin-header">
      <div class="header-left">
        <el-icon :size="24"><School /></el-icon>
        <span class="header-title">校园二手交易平台 - 管理后台</span>
      </div>
      <div class="header-right">
        <el-button text @click="$router.push('/')">返回前台</el-button>
        <el-button text @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>

    <el-container class="admin-container">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          class="admin-menu"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/goods">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/reports">
            <el-icon><Warning /></el-icon>
            <span>举报管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notices">
            <el-icon><Notification /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容 -->
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { School, DataAnalysis, User, Goods, Warning, Notification } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示').then(() => {
    userStore.logout()
    router.push('/')
  }).catch(() => {})
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #409eff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  gap: 8px;
}

.admin-container {
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  background: #fff;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
}

.admin-menu {
  border-right: none;
}

.admin-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
