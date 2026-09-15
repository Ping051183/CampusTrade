<template>
  <router-view />
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

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}
</style>
