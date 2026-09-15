package com.campustrade.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    /**
     * 根据错误码构造业务异常
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 根据错误码和自定义信息构造业务异常
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 直接构造业务异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
