<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-info">
        <h3>校园二手交易平台</h3>
        <p>绿色环保，物尽其用</p>
      </div>
      <div class="footer-links">
        <span>关于我们</span>
        <span>帮助中心</span>
        <span>用户协议</span>
        <span>隐私政策</span>
      </div>
      <div class="footer-copyright">
        &copy; 2024 CampusTrade. All rights reserved.
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  background: #303133;
  color: #c0c4cc;
  padding: 40px 20px 20px;
  margin-top: auto;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.footer-info h3 {
  color: #fff;
  font-size: 18px;
  margin-bottom: 8px;
}

.footer-info p {
  font-size: 13px;
  margin-bottom: 20px;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 20px;
  font-size: 13px;
}

.footer-links span {
  cursor: pointer;
  transition: color 0.2s;
}

.footer-links span:hover {
  color: #409eff;
}

.footer-copyright {
  font-size: 12px;
  border-top: 1px solid #4a4a4a;
  padding-top: 16px;
}
</style>
