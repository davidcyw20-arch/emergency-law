import http from './http'

/**
 * 经验分享（后端如果尚未实现接口，页面会自动兜底用 localStorage 演示数据）
 * 建议后端未来提供：
 * GET  /api/share/posts
 * POST /api/share/posts
 * GET  /api/share/posts/{id}
 */

export function apiShareList() {
    return http.get('/api/share/posts')
}

export function apiShareCreate(payload) {
    return http.post('/api/share/posts', payload)
}

export function apiShareLike(id) {
    return http.post(`/api/share/post/${id}/like`)
}

export function apiShareComments(id) {
    return http.get(`/api/share/post/${id}/comments`)
}

export function apiShareComment(id, payload) {
    return http.post(`/api/share/post/${id}/comment`, payload)
}

const LS_KEY = 'share_posts_demo'

export function demoLoad() {
    try {
        const raw = localStorage.getItem(LS_KEY)
        if (raw) return JSON.parse(raw)

        // 初始演示数据
        const seed = [
            {
                id: 1,
                title: '地震避险：室内“三角区”要点',
                content:
                    '地震发生时优先就近躲避，选择承重墙角、坚固家具旁形成的“三角区”，远离玻璃、吊灯；地震结束后再有序撤离。',
                tags: ['地震', '避险'],
                region: '江苏',
                author: '测试用户',
                createdAt: new Date().toISOString(),
                likes: 12
            },
            {
                id: 2,
                title: '火灾逃生：湿毛巾不是万能，关键是低姿前进',
                content:
                    '烟气比火更致命。逃生时尽量贴近地面，沿疏散指示方向撤离，不可乘坐电梯；门把手发烫不要贸然开门。',
                tags: ['火灾', '逃生'],
                region: '浙江',
                author: '测试用户',
                createdAt: new Date().toISOString(),
                likes: 8
            }
        ]
        localStorage.setItem(LS_KEY, JSON.stringify(seed))
        return seed
    } catch {
        return []
    }
}

export function demoSave(list) {
    try {
        localStorage.setItem(LS_KEY, JSON.stringify(list))
    } catch {
        // ignore
    }
}
