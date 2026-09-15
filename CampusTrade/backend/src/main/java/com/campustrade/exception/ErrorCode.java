package com.campustrade.exception;

/**
 * 错误码枚举类
 */
public enum ErrorCode {

    // 通用错误码
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块错误码
    USERNAME_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    PASSWORD_ERROR(1004, "密码错误"),
    OLD_PASSWORD_ERROR(1005, "原密码错误"),
    USERNAME_OR_PASSWORD_ERROR(1006, "用户名或密码错误"),

    // 商品模块错误码
    GOODS_NOT_FOUND(2001, "商品不存在"),
    GOODS_OFFLINE(2002, "商品已下架"),
    GOODS_SOLD(2003, "商品已售出"),
    GOODS_NOT_YOURS(2004, "无权操作该商品"),

    // 分类模块
    CATEGORY_NOT_FOUND(3001, "分类不存在"),

    // 收藏模块
    FAVORITE_EXISTS(4001, "已收藏该商品"),
    FAVORITE_NOT_FOUND(4002, "收藏记录不存在"),

    // 订单模块
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_STATUS_ERROR(5002, "订单状态错误"),
    ORDER_CANNOT_CANCEL(5003, "订单无法取消"),
    ORDER_CANNOT_CONFIRM(5004, "订单无法确认收货"),

    // 评论模块
    COMMENT_NOT_FOUND(6001, "评论不存在"),
    COMMENT_NOT_YOURS(6002, "无权删除该评论"),

    // 举报模块
    REPORT_NOT_FOUND(7001, "举报记录不存在"),

    // 文件上传
    FILE_TOO_LARGE(8001, "文件大小超出限制"),
    FILE_TYPE_ERROR(8002, "文件类型不支持"),
    FILE_UPLOAD_ERROR(8003, "文件上传失败"),

    // 聊天模块
    CHAT_SEND_TO_SELF(9001, "不能给自己发送消息");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
