<template>
  <div class="publish-page">
    <Header />
    <div class="page-container">
      <div class="publish-card">
        <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
        <p class="page-desc">{{ isEdit ? '修改商品信息' : '发布闲置物品，等待管理员审核' }}</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="publish-form">
          <el-form-item label="商品标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="售价" prop="price">
            <el-input-number v-model="form.price" :precision="2" :min="0.01" :max="99999.99" style="width: 200px" />
          </el-form-item>

          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :precision="2" :min="0" :max="99999.99" style="width: 200px" />
          </el-form-item>

          <el-form-item label="商品图片">
            <el-upload
              :action="'/api/file/upload'"
              list-type="picture-card"
              :limit="6"
              :on-success="handleImageSuccess"
              :before-upload="handleBeforeUpload"
              :headers="{ Authorization: 'Bearer ' + getToken() }"
              name="file"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">建议上传1-6张图片，支持JPG/PNG格式</div>
          </el-form-item>

          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              placeholder="描述商品的成色、使用情况、购买时间等信息..."
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '提交审核' }}
            </el-button>
            <el-button size="large" @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { goodsAPI } from '@/api/goods'
import { categoryAPI } from '@/api/category'
import { compressImage } from '@/utils/imageCompress'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Category, Goods } from '@/api/types'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const submitting = ref(false)
const fileList = ref<any[]>([])
const uploadedImages = ref<string[]>([])

const isEdit = computed(() => !!route.params.id)
const goodsId = computed(() => Number(route.params.id))

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  price: undefined as number | undefined,
  originalPrice: undefined as number | undefined,
  description: ''
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在2-100之间', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' },
    { min: 10, message: '描述至少10个字', trigger: 'blur' }
  ]
}

onMounted(async () => {
  // 加载分类
  const catRes = await categoryAPI.getList()
  if (catRes.data) categories.value = catRes.data

  // 如果是编辑，加载商品信息
  if (isEdit.value) {
    const res = await goodsAPI.getDetail(goodsId.value)
    if (res.data) {
      form.title = res.data.title
      form.categoryId = res.data.categoryId
      form.price = res.data.price
      form.originalPrice = res.data.originalPrice
      form.description = res.data.description
    }
  }
})

async function handleImageSuccess(response: any, _file: any, fileList: any[]) {
  if (response.code === 200 && response.data) {
    uploadedImages.value.push(response.data)
  }
}

/**
 * 图片上传前的压缩处理
 */
async function handleBeforeUpload(rawFile: File): Promise<File | Blob> {
  return compressImage(rawFile)
}

function getToken(): string {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).token || ''
    } catch (e) {}
  }
  return ''
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const goodsData: any = {
      title: form.title,
      categoryId: form.categoryId!,
      price: form.price!,
      originalPrice: form.originalPrice,
      description: form.description,
      images: uploadedImages.value.join(',')
    }

    if (isEdit.value) {
      goodsData.id = goodsId.value
      await goodsAPI.update(goodsData)
      ElMessage.success('修改成功，需重新审核')
    } else {
      await goodsAPI.publish(goodsData)
      ElMessage.success('发布成功，等待审核')
    }
    router.push('/my/goods')
  } catch (e) {
    // Error handled in interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.publish-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  margin-top: 20px;
  margin-bottom: 40px;
}

.page-title {
  font-size: 22px;
  margin-bottom: 8px;
}

.page-desc {
  color: #909399;
  margin-bottom: 32px;
}

.publish-form {
  max-width: 700px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
