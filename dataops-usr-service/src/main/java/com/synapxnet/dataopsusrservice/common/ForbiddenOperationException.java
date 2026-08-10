package com.synapxnet.dataopsusrservice.common;

/**
 * 表示当前登录身份已通过认证，但没有执行管理操作的权限。
 */
public class ForbiddenOperationException extends RuntimeException {

    /**
     * 使用指定提示创建禁止访问异常。
     *
     * @param message 面向调用方的拒绝原因
     */
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
