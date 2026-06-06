package com.smartmemo.user.domain;

/**
 * 用户状态。
 */
public enum UserStatus {
    /** 正常 */
    active,
    /** 禁用 */
    disabled,
    /** 已注销（软删除） */
    deleted
}
