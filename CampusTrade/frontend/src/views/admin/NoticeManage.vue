<template>
  <div class="notice-manage-page">
    <div class="page-header">
      <h2 class="page-title">公告管理</h2>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon> 发布公告
      </el-button>
    </div>

    <el-table :data="notices" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="公告标题" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发布公告对话框 -->
    <el-dialog v-model="showDialog" title="发布公告" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminAPI } from '@/api/admin'
import { noticeAPI } from '@/api/notice'
import type { Notice } from '@/api/types'
import type { FormInstance, FormRules } from 'element-plus'

const notices = ref<Notice[]>([])
const loading = ref(true)
const showDialog = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  content: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

onMounted(() => loadNotices())

async function loadNotices() {
  loading.value = true
  try {
    const res = await noticeAPI.getList()
    if (res.data) notices.value = res.data
  } catch (e) {
    console.error('加载公告失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    await adminAPI.addNotice({ title: form.title, content: form.content })
    ElMessage.success('发布成功')
    showDialog.value = false
    form.title = ''
    form.content = ''
    loadNotices()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '提示')
    await adminAPI.deleteNotice(id)
    ElMessage.success('已删除')
    loadNotices()
  } catch (e) {}
}
</script>

<style scoped>
.notice-manage-page {
  max-width: 1000px;
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
</style>
