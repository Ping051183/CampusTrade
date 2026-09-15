package com.campustrade.common;

import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    private Result() {}

    /**
     * 构建成功响应（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "操作成功";
        return result;
    }

    /**
     * 构建成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "操作成功";
        result.data = data;
        return result;
    }

    /**
     * 构建成功响应（带数据和自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = message;
        result.data = data;
        return result;
    }

    /**
     * 构建失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    /**
     * 构建失败响应（默认400）
     */
    public static <T> Result<T> error(String message) {
        return error(400, message);
    }

    /**
     * 构建未授权响应
     */
    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }

    /**
     * 构建无权限响应
     */
    public static <T> Result<T> forbidden(String message) {
        return error(403, message);
    }

    /**
     * 构建服务器错误响应
     */
    public static <T> Result<T> serverError(String message) {
        return error(500, message);
    }
}
