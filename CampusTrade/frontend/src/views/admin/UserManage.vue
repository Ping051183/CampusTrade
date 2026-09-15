<template>
  <div class="user-manage-page">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称/学校"
        clearable
        style="width: 300px"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
    </div>

    <el-table :data="users" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="school" label="学校" width="150" />
      <el-table-column prop="role" label="角色" width="80">
        <template #default="{ row }">
          <el-tag :type="row.role === 0 ? 'danger' : 'info'" size="small">
            {{ row.role === 0 ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            :type="row.status === 1 ? 'danger' : 'success'"
            size="small"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadUsers"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminAPI } from '@/api/admin'
import type { UserVO } from '@/api/types'

const users = ref<UserVO[]>([])
const loading = ref(true)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

onMounted(() => loadUsers())

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminAPI.getUserList({
      page: currentPage.value,
      pageSize,
      keyword: keyword.value || undefined
    })
    if (res.data) {
      users.value = res.data.records || []
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载用户失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

async function handleToggleStatus(user: UserVO) {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${user.username}」吗？`, '提示')
    await adminAPI.updateUserStatus(user.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (e) {}
}
</script>

<style scoped>
.user-manage-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
