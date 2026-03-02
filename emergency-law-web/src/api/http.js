import axios from 'axios'

const http = axios.create({
    // ✅ 关键：不要写 http://localhost:5177，也不要写 8080
    // 直接用相对路径 /api/**，交给 Vite proxy
    baseURL: '',
    timeout: 15000
})

http.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers = config.headers || {}
        config.headers.Authorization = token.startsWith('Bearer ')
            ? token
            : `Bearer ${token}`
    }
    return config
})

http.interceptors.response.use(
    (resp) => resp,
    (err) => Promise.reject(err)
)

export default http
