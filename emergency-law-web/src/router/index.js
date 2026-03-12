import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Learn from '../views/Learn.vue'
import CourseDetail from '../views/CourseDetail.vue'
import Stats from '../views/Stats.vue'
import Share from '../views/Share.vue'
import Admin from '../views/Admin.vue'

import TestHome from '../views/TestHome.vue'
import TestTake from '../views/TestTake.vue'
import TestHistory from '../views/TestHistory.vue'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: Login },
    { path: '/register', component: Register },

    { path: '/learn', component: Learn },
    { path: '/learn/course/:id', component: CourseDetail },
    { path: '/stats', component: Stats },
    { path: '/share', component: Share },

    { path: '/test', component: TestHome },
    { path: '/test/take/:paperId', component: TestTake },
    { path: '/test/history', component: TestHistory },
    { path: '/admin', component: Admin }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 登录拦截：无 token 只能去 login
router.beforeEach((to, from, next) => {
    if (to.path === '/login' || to.path === '/register') return next()

    const token = localStorage.getItem('token')
    if (!token) return next('/login')

    const raw = localStorage.getItem('user')
    let role = ''
    if (raw) {
        try {
            role = JSON.parse(raw)?.role || ''
        } catch {
            role = ''
        }
    }

    if (to.path.startsWith('/admin')) {
        if (role !== 'ADMIN') return next('/learn')
    }

    // 管理员账号只允许进入管理端页面
    if (role === 'ADMIN' && !to.path.startsWith('/admin')) {
        return next('/admin')
    }

    next()
})

export default router
