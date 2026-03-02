import http from './http'

export function apiAdminCourses() {
  return http.get('/api/admin/courses')
}

export function apiAdminCreateCourse(payload) {
  return http.post('/api/admin/courses', payload)
}

export function apiAdminUpdateCourse(id, payload) {
  return http.put(`/api/admin/courses/${id}`, payload)
}

export function apiAdminPublishCourse(id) {
  return http.post(`/api/admin/courses/${id}/publish`)
}

export function apiAdminArchiveCourse(id) {
  return http.post(`/api/admin/courses/${id}/archive`)
}

export function apiAdminLessons(courseId) {
  return http.get(`/api/admin/courses/${courseId}/lessons`)
}

export function apiAdminCreateLesson(courseId, payload) {
  return http.post(`/api/admin/courses/${courseId}/lessons`, payload)
}

export function apiAdminUpdateLesson(courseId, id, payload) {
  return http.put(`/api/admin/courses/${courseId}/lessons/${id}`, payload)
}

export function apiAdminDeleteLesson(courseId, id) {
  return http.delete(`/api/admin/courses/${courseId}/lessons/${id}`)
}

// --- 题库 / 试卷 ---
export function apiAdminTestPapers() {
  return http.get('/api/admin/test/papers')
}

export function apiAdminCreateTestPaper(payload) {
  return http.post('/api/admin/test/papers', payload)
}

export function apiAdminUpdateTestPaper(id, payload) {
  return http.put(`/api/admin/test/papers/${id}`, payload)
}

export function apiAdminPublishTestPaper(id) {
  return http.post(`/api/admin/test/papers/${id}/publish`)
}

export function apiAdminArchiveTestPaper(id) {
  return http.post(`/api/admin/test/papers/${id}/archive`)
}

export function apiAdminTestQuestions(paperId) {
  return http.get(`/api/admin/test/papers/${paperId}/questions`)
}

export function apiAdminCreateTestQuestion(paperId, payload) {
  return http.post(`/api/admin/test/papers/${paperId}/questions`, payload)
}

export function apiAdminUpdateTestQuestion(paperId, id, payload) {
  return http.put(`/api/admin/test/papers/${paperId}/questions/${id}`, payload)
}

export function apiAdminDeleteTestQuestion(paperId, id) {
  return http.delete(`/api/admin/test/papers/${paperId}/questions/${id}`)
}

// --- 试卷规则 ---
export function apiAdminGetTestRules() {
  return http.get('/api/admin/test/rules')
}

export function apiAdminSaveTestRules(payload) {
  return http.put('/api/admin/test/rules', payload)
}

// --- 帖子审核 ---
export function apiAdminSharePosts() {
  return http.get('/api/admin/share/posts')
}

export function apiAdminApproveSharePost(id) {
  return http.post(`/api/admin/share/post/${id}/approve`)
}

export function apiAdminRejectSharePost(id) {
  return http.post(`/api/admin/share/post/${id}/reject`)
}

// --- 用户管理 ---
export function apiAdminUsers() {
  return http.get('/api/admin/users')
}

export function apiAdminUpdateUserRole(id, role) {
  return http.post(`/api/admin/users/${id}/role`, { role })
}

export function apiAdminResetUserPassword(id, password) {
  return http.post(`/api/admin/users/${id}/reset-password`, { password })
}

export function apiAdminDisableUser(id) {
  return http.post(`/api/admin/users/${id}/disable`)
}

export function apiAdminEnableUser(id) {
  return http.post(`/api/admin/users/${id}/enable`)
}

// --- 报表 ---
export function apiAdminOverviewReport() {
  return http.get('/api/admin/reports/overview')
}

export function apiAdminMonthlyReport() {
  return http.get('/api/admin/reports/monthly')
}

export function apiAdminQuarterlyReport() {
  return http.get('/api/admin/reports/quarterly')
}

export function apiAdminSeedTestBank() {
  return http.post('/api/admin/test/seed')
}
