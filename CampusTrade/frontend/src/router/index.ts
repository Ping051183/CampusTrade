import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 首页
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue')
    },
    // 登录
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    // 注册
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue')
    },
    // 商品列表
    {
      path: '/goods',
      name: 'GoodsList',
      component: () => import('@/views/GoodsList.vue')
    },
    // 商品详情
    {
      path: '/goods/detail/:id',
      name: 'GoodsDetail',
      component: () => import('@/views/GoodsDetail.vue')
    },
    // 发布商品（需要登录）
    {
      path: '/goods/publish',
      name: 'PublishGoods',
      component: () => import('@/views/PublishGoods.vue'),
      meta: { requiresAuth: true }
    },
    // 编辑商品（需要登录）
    {
      path: '/goods/edit/:id',
      name: 'EditGoods',
      component: () => import('@/views/PublishGoods.vue'),
      meta: { requiresAuth: true }
    },
    // 我的商品
    {
      path: '/my/goods',
      name: 'MyGoods',
      component: () => import('@/views/MyGoods.vue'),
      meta: { requiresAuth: true }
    },
    // 我的订单
    {
      path: '/my/orders',
      name: 'MyOrders',
      component: () => import('@/views/MyOrders.vue'),
      meta: { requiresAuth: true }
    },
    // 我的收藏
    {
      path: '/my/favorites',
      name: 'MyFavorites',
      component: () => import('@/views/MyFavorites.vue'),
      meta: { requiresAuth: true }
    },
    // 个人信息
    {
      path: '/my/profile',
      name: 'MyProfile',
      component: () => import('@/views/MyProfile.vue'),
      meta: { requiresAuth: true }
    },
    // 我的消息
    {
      path: '/my/chats',
      name: 'MyChats',
      component: () => import('@/views/MyChats.vue'),
      meta: { requiresAuth: true }
    },
    // 系统通知
    {
      path: '/my/notifications',
      name: 'Notifications',
      component: () => import('@/views/Notifications.vue'),
      meta: { requiresAuth: true }
    },
    // 聊天详情
    {
      path: '/chat',
      name: 'ChatDetail',
      component: () => import('@/views/ChatDetail.vue'),
      meta: { requiresAuth: true }
    },
    // 修改密码
    {
      path: '/my/password',
      name: 'UpdatePassword',
      component: () => import('@/views/UpdatePassword.vue'),
      meta: { requiresAuth: true }
    },
    // ===== 管理后台 =====
    {
      path: '/admin',
      component: () => import('@/views/admin/Layout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue')
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/UserManage.vue')
        },
        {
          path: 'goods',
          name: 'AdminGoods',
          component: () => import('@/views/admin/GoodsManage.vue')
        },
        {
          path: 'reports',
          name: 'AdminReports',
          component: () => import('@/views/admin/ReportManage.vue')
        },
        {
          path: 'notices',
          name: 'AdminNotices',
          component: () => import('@/views/admin/NoticeManage.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 需要管理员权限的页面
  if (to.meta.requiresAdmin && userStore.role !== 0) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
