import http from './http'

/**
 * 在线测试 API（对接后端 Spring Boot）
 * BaseURL 在 http.js 里配置（一般是 http://localhost:8080）
 */

// 1) 试卷列表
export function apiTestPapers() {
    return http.get('/api/test/papers')
}

// 2) 试卷详情（含题目）
export function apiTestPaperDetail(paperId) {
    return http.get(`/api/test/paper/${paperId}`)
}

// 3) 交卷
// payload: { paperId: number, answers: number[], usedMin: number }
export function apiTestSubmit(payload) {
    return http.post('/api/test/submit', payload)
}

// 4) 我的成绩
export function apiTestMyHistory() {
    return http.get('/api/test/my/history')
}

// 5) 试卷规则（全局）
export function apiTestRules() {
    return http.get('/api/test/rules')
}

export default {
    apiTestPapers,
    apiTestPaperDetail,
    apiTestSubmit,
    apiTestMyHistory,
    apiTestRules
}
