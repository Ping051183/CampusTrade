// ========== 通用类型 ==========

/** 统一响应结果 */
export interface ApiResult<T> {
  code: number
  message: string
  data?: T
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  pages: number
}

// ========== 用户相关 ==========

/** 登录请求 */
export interface LoginDTO {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterDTO {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
  studentNo: string
  school: string
}

/** 登录返回 */
export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
  avatar: string
  role: number
}

/** 用户信息 */
export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  studentNo: string
  school: string
  role: number
  status: number
  createTime: string
}

/** 用户更新 */
export interface UserUpdateDTO {
  nickname?: string
  phone?: string
  email?: string
  studentNo?: string
  school?: string
}

/** 修改密码 */
export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

// ========== 商品相关 ==========

/** 商品 */
export interface Goods {
  id?: number
  title: string
  description: string
  categoryId: number
  price: number
  originalPrice?: number
  sellerId?: number
  images?: string
  status?: number
  viewCount?: number
  createTime?: string
  updateTime?: string
}

/** 商品VO */
export interface GoodsVO {
  id: number
  title: string
  description: string
  categoryId: number
  categoryName: string
  price: number
  originalPrice: number
  sellerId: number
  sellerName: string
  sellerAvatar: string
  sellerSchool: string
  images: string
  status: number
  viewCount: number
  isFavorited: boolean
  favoriteCount: number
  createTime: string
}

// ========== 分类 ==========

export interface Category {
  id: number
  categoryName: string
  sort: number
}

// ========== 订单 ==========

export interface Order {
  id: number
  orderNo: string
  buyerId: number
  sellerId: number
  goodsId: number
  amount: number
  status: number
  remark: string
  isRated: number
  sellerRated: number
  refundStatus: number
  refundReason?: string
  createTime: string
  updateTime: string
}

// ========== 交易评价 ==========

/** 交易评价 */
export interface Review {
  id: number
  orderId: number
  goodsId: number
  goodsTitle?: string
  goodsImages?: string
  raterId: number
  raterName: string
  raterAvatar?: string
  rateeId: number
  score: number
  content?: string
  createTime: string
}

/** 用户信用汇总 */
export interface ReviewSummary {
  avgScore: number
  count: number
  goodCount: number
  goodRate: number
}

// ========== 系统通知 ==========

/** 系统通知 */
export interface Notification {
  id: number
  userId: number
  type: string
  title: string
  content?: string
  relatedId?: number
  isRead: number
  createTime: string
}

// ========== 评论 ==========

export interface Comment {
  id?: number
  goodsId: number
  userId?: number
  content: string
  createTime?: string
}

// ========== 举报 ==========

export interface Report {
  id?: number
  goodsId: number
  reporterId?: number
  reason: string
  status?: number
  handleResult?: string
  createTime?: string
}

// ========== 聊天 ==========

/** 发送消息请求 */
export interface SendMessageDTO {
  receiverId: number
  goodsId?: number
  content?: string
  imageUrl?: string
}

/** 聊天消息 */
export interface ChatMessageVO {
  id: number
  senderId: number
  senderName: string
  senderAvatar: string
  receiverId: number
  content: string
  imageUrl?: string
  isRead: number
  createTime: string
}

/** 会话信息 */
export interface ConversationVO {
  otherUserId: number
  otherUserName: string
  otherUserAvatar: string
  goodsId: number
  goodsTitle: string
  goodsImage: string
  goodsPrice: number
  lastMessage: string
  lastMessageImageUrl?: string
  lastMessageTime: string
  unreadCount: number
}

// ========== 公告 ==========

export interface Notice {
  id?: number
  title: string
  content: string
  createTime?: string
}
