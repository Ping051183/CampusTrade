# 校园二手交易平台 - API接口文档

## 1. 通用说明

### 1.1 基础URL
- 开发环境：`http://localhost:8080/api`
- 生产环境：`http://your-domain/api`

### 1.2 统一响应格式
```json
{
  "code": 200,      // 状态码
  "message": "操作成功",  // 提示信息
  "data": {}        // 响应数据
}
```

### 1.3 分页响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [],  // 数据列表
    "total": 100,   // 总记录数
    "page": 1,      // 当前页
    "pageSize": 10, // 每页条数
    "pages": 10     // 总页数
  }
}
```

### 1.4 认证方式
在请求头中添加：`Authorization: Bearer <token>`

## 2. 认证接口

### POST /auth/register
注册新用户
```json
// Request
{"username":"string","password":"string","nickname":"string","phone":"string","email":"string","studentNo":"string","school":"string"}
// Response 200
{"code":200,"message":"注册成功"}
```

### POST /auth/login
用户登录
```json
// Request
{"username":"string","password":"string"}
// Response 200
{"code":200,"message":"登录成功","data":{"token":"jwt...","userId":1,"username":"admin","nickname":"管理员","avatar":"","role":0}}
```

## 3. 用户接口

### GET /user/info
获取当前用户信息（需认证）

### PUT /user/update
修改个人信息（需认证）

### PUT /user/password
修改密码（需认证）

## 4. 商品接口

### POST /goods/publish
发布商品（需认证）

### PUT /goods/update
编辑商品（需认证）

### DELETE /goods/delete/{id}
删除商品（需认证）

### PUT /goods/offline/{id}
下架商品（需认证）

### GET /goods/detail/{id}
获取商品详情

### GET /goods/list
分页查询商品列表
参数：page, pageSize, categoryId, keyword

### GET /goods/my
获取我的商品列表（需认证）
参数：status

## 5. 分类接口

### GET /category/list
获取所有分类

## 6. 收藏接口

### POST /favorite/add/{goodsId}
添加收藏（需认证）

### DELETE /favorite/cancel/{goodsId}
取消收藏（需认证）

### GET /favorite/my
获取我的收藏（需认证）

### GET /favorite/check/{goodsId}
检查是否已收藏（需认证）

## 7. 订单接口

### POST /order/create/{goodsId}
创建订单（需认证）

### GET /order/detail/{id}
订单详情（需认证）

### GET /order/my-buy
我的购买订单（需认证）

### GET /order/my-sell
我的出售订单（需认证）

### PUT /order/cancel/{id}
取消订单（需认证）

### PUT /order/confirm/{id}
确认收货（需认证）

## 8. 评论接口

### GET /comment/list/{goodsId}
获取商品评论

### POST /comment/add
添加评论（需认证）

### DELETE /comment/delete/{id}
删除评论（需认证）

## 9. 举报接口

### POST /report/add
举报商品（需认证）

## 10. 公告接口

### GET /notice/list
获取最新公告

## 11. 管理员接口

### GET /admin/dashboard
数据统计（需管理员）

### GET /admin/user/list
用户列表（需管理员）

### PUT /admin/user/status/{id}
修改用户状态（需管理员）

### GET /admin/goods/list
商品列表（需管理员）

### PUT /admin/goods/audit/{id}
商品审核（需管理员）

### DELETE /admin/goods/delete/{id}
删除商品（需管理员）

### GET /admin/report/list
举报列表（需管理员）

### PUT /admin/report/handle/{id}
处理举报（需管理员）

### POST /admin/notice/add
发布公告（需管理员）

### DELETE /admin/notice/delete/{id}
删除公告（需管理员）

## 12. 文件接口

### POST /file/upload
上传图片（需认证）
参数：file (multipart/form-data)
返回：{"code":200,"data":"/uploads/xxx.jpg"}
