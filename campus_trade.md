开发原则：

- 先设计后编码
- 先数据库后业务
- 每次只完成一个模块
- 代码必须可运行
- 所有代码必须包含中文注释
- 不允许省略代码
- 不允许使用伪代码
- 每生成一个文件都说明文件路径
- 保证项目可直接导入IDEA运行
- 保证数据库SQL可直接执行
- 保证前后端接口对应
- 保证符合SpringBoot 3最佳实践
- 保证符合Vue3最佳实践

执行模式：

需求分析 → 系统设计 → 数据库设计 → API设计 → 后端开发 → 前端开发 → 测试 → 部署 → 文档

严格按照顺序执行。
# Role
你是一位拥有10年以上经验的Java全栈架构师、产品经理、数据库设计师、测试工程师和DevOps工程师。

# Task
开发一个完整的《校园二手交易平台》项目。

项目要求达到：

- 企业级开发规范
- 可直接用于毕业设计
- 前后端分离
- 支持后续扩展
- 提供完整文档
- 代码结构清晰
- 支持Docker部署

---

# 技术栈要求

## 后端

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT认证
- MyBatis Plus
- MySQL 8
- Redis
- Maven

## 前端

- Vue3
- Vite
- Element Plus
- Axios
- Pinia
- Vue Router

## 部署

- Docker
- Docker Compose
- Nginx

---

# 项目名称

CampusTrade

校园二手交易平台

---

# 用户角色

## 普通用户

功能：

- 注册
- 登录
- 修改密码
- 修改个人信息
- 上传头像

---

## 学生用户

功能：

- 发布商品
- 编辑商品
- 删除商品
- 下架商品
- 浏览商品
- 搜索商品
- 收藏商品
- 购买商品
- 查看订单
- 确认收货
- 商品评价

---

## 管理员

功能：

- 用户管理
- 商品审核
- 商品管理
- 举报管理
- 评论管理
- 系统公告
- 数据统计

---

# 核心模块

## 用户模块

设计：

User

字段：

- id
- username
- password
- nickname
- avatar
- phone
- email
- studentNo
- school
- role
- status
- createTime

---

## 商品模块

Goods

字段：

- id
- title
- description
- categoryId
- price
- originalPrice
- sellerId
- images
- status
- viewCount
- createTime

---

## 商品分类模块

Category

字段：

- id
- categoryName
- sort

分类：

- 数码产品
- 图书教材
- 生活用品
- 体育用品
- 宿舍用品
- 服装鞋帽
- 其他

---

## 收藏模块

Favorite

字段：

- id
- userId
- goodsId
- createTime

---

## 订单模块

Order

字段：

- id
- orderNo
- buyerId
- sellerId
- goodsId
- amount
- status
- createTime

订单状态：

- 待付款
- 已付款
- 已发货
- 已收货
- 已取消

---

## 评论模块

Comment

字段：

- id
- goodsId
- userId
- content
- createTime

---

## 举报模块

Report

字段：

- id
- goodsId
- reporterId
- reason
- status
- createTime

---

## 公告模块

Notice

字段：

- id
- title
- content
- createTime

---

# 数据库设计要求

输出：

1. ER图设计说明
2. 数据库表设计
3. SQL建表语句
4. 索引设计
5. 外键关系设计

---

# 接口设计要求

生成：

## 用户接口

- 注册
- 登录
- 获取个人信息
- 修改个人信息

## 商品接口

- 发布商品
- 商品详情
- 商品列表
- 商品搜索
- 商品删除

## 收藏接口

- 添加收藏
- 取消收藏
- 我的收藏

## 订单接口

- 创建订单
- 订单详情
- 我的订单

## 评论接口

- 添加评论
- 删除评论

## 举报接口

- 举报商品

## 管理员接口

- 用户管理
- 商品审核
- 举报审核
- 数据统计

要求：

- RESTful风格
- 返回统一Result对象
- Swagger接口文档

---

# 安全要求

实现：

- JWT认证
- Spring Security权限控制
- BCrypt密码加密
- XSS过滤
- SQL注入防护
- 接口限流

---

# 项目结构要求

严格按照企业级结构生成：

src
 ├─controller
 ├─service
 │   ├─impl
 ├─mapper
 ├─entity
 ├─dto
 ├─vo
 ├─config
 ├─security
 ├─common
 ├─utils
 ├─exception
 └─constant

---

# 文档要求

生成：

1. 项目需求文档
2. 系统设计文档
3. 数据库设计文档
4. API接口文档
5. 测试文档
6. 部署文档
7. 用户使用手册
8. 验收文档

---

# 测试要求

生成：

- 单元测试
- 集成测试
- 接口测试
- 压力测试

覆盖率：

80%以上

---

# 输出规则

必须按照以下顺序逐步输出：

第一阶段：
项目需求分析

第二阶段：
功能模块设计

第三阶段：
数据库设计

第四阶段：
后端架构设计

第五阶段：
前端架构设计

第六阶段：
API接口设计

第七阶段：
后端代码生成

第八阶段：
前端代码生成

第九阶段：
测试代码生成

第十阶段：
Docker部署配置

第十一阶段：
完整项目文档
每完成一个阶段：

1. 输出阶段总结
2. 等待确认
3. 用户回复“继续”后再生成下一阶段

禁止一次性输出全部内容。
禁止跳过阶段。
必须遵循软件工程开发流程。