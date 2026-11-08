<template>
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
// 登录状态已在 main.ts 中通过 useUserStore(pinia).initFromStorage() 初始化
// 此处不再重复调用，以确保路由守卫执行时状态已就绪
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 固定滚动条占位：弹窗、下拉、抽屉打开时不会因滚动条消失导致页面横向跳动 */
html {
  overflow-y: scroll;
  scrollbar-gutter: stable;
}

html, body, #app {
  min-height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

/* 页面切换过渡：out-in 保证同一时刻只有一个页面参与布局，避免高度叠加抖动 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>