package com.dongchedi.car.support;

/**
 * Session 与请求属性键。
 */
public final class SessionKeys {

    /**
     * 登录用户 id，存放在 HttpSession 中，实现「服务端会话」而不是前端保存 Token。
     */
    public static final String LOGIN_USER_ID = "DONGCHEDI_LOGIN_USER_ID";

    /**
     * 当前登录用户对象在本次请求中的属性名，控制器可用 @RequestAttribute 直接注入。
     */
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private SessionKeys() {
    }
}
