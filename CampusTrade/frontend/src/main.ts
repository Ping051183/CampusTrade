import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import App from './App.vue'
import router from './router'
import './style.css'
import { useUserStore } from '@/stores/user'

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 先初始化 Pinia 状态，再注册路由
// 确保路由守卫执行时能读到正确的角色信息（如管理员身份）
const pinia = createPinia()
app.use(pinia)
useUserStore(pinia).initFromStorage()

app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
