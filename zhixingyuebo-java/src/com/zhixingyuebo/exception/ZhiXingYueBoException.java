package com.zhixingyuebo.exception;

/**
 * 系统业务根受检异常基类
 */
public class ZhiXingYueBoException extends Exception {
    private static final long serialVersionUID = 1L;

    public ZhiXingYueBoException(String message) {
        super(message);
    }

    public ZhiXingYueBoException(String message, Throwable cause) {
        super(message, cause);
    }
}
