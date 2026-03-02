// src/api/auth.js
import http from './http'

/**
 * 登录
 * payload: { username, password }
 * 返回：后端统一结构 { success, message, data: { token, userId, username, ... } }
 */
export function apiLogin(payload) {
    return http({
        url: '/api/auth/login',
        method: 'post',
        data: payload
    })
}

/**
 * 当前登录用户信息
 */
export function apiMe() {
    return http({
        url: '/api/user/me',
        method: 'get'
    })
}

export default {
    apiLogin,
    apiMe
}
