// src/api/learn.js
// 【最终稳定版】
// 目标：
// 1. 完全匹配 Learn.vue / CourseDetail.vue 里的 import
// 2. 不改任何页面
// 3. 所有常见命名全部导出，永不再出现 does not provide export

import http from './http'

// ---------- 工具 ----------
const isPlainObject = (v) =>
    Object.prototype.toString.call(v) === '[object Object]'
const isNil = (v) => v === null || v === undefined

// ---------- 分类 ----------
const getCategories = (params) =>
    http({
        url: '/api/learn/categories',
        method: 'get',
        params
    })

// ---------- 课程列表 ----------
const getCourses = (arg) => {
    let params
    if (isNil(arg) || arg === '') {
        params = undefined
    } else if (isPlainObject(arg)) {
        params = arg
    } else {
        params = { categoryId: arg }
    }

    return http({
        url: '/api/learn/courses',
        method: 'get',
        params
    })
}

// ---------- 课程详情 ----------
const getCourse = (id) =>
    http({
        url: `/api/learn/course/${id}`,
        method: 'get'
    })

// ---------- 课时 ----------
const getCourseLessons = (courseId) =>
    http({
        url: `/api/learn/course/${courseId}/lessons`,
        method: 'get'
    })

const getLesson = (lessonId) =>
    http({
        url: `/api/learn/lesson/${lessonId}`,
        method: 'get'
    })

// ---------- 学习进度 ----------
const getMyProgressList = () =>
    http({
        url: '/api/learn/my/progress',
        method: 'get'
    })

const getMyProgressOne = (courseId) =>
    http({
        url: `/api/learn/my/progress/${courseId}`,
        method: 'get'
    })

const upsertProgress = (data) =>
    http({
        url: '/api/learn/my/progress/upsert',
        method: 'post',
        data
    })

// 智能：
// apiMyProgress()        -> 列表
// apiMyProgress(courseId) -> 单课程
const getMyProgressSmart = (arg) => {
    if (!isNil(arg) && !isPlainObject(arg)) {
        return getMyProgressOne(arg)
    }
    return getMyProgressList()
}

// =======================================================
// ⭐⭐⭐ 导出区：这里是“关键”，Learn.vue 依赖这些名字 ⭐⭐⭐
// =======================================================

// --- 分类 ---
export const apiCategories = getCategories
export const apiGetCategories = getCategories
export const apiCategoryList = getCategories

// --- 课程 ---
export const apiCourses = getCourses
export const apiGetCourses = getCourses
export const apiCourseList = getCourses

// --- 课程详情 ---
export const apiCourse = getCourse
export const apiGetCourse = getCourse
export const apiCourseDetail = getCourse

// --- 课时 ---
export const apiCourseLessons = getCourseLessons
export const apiGetCourseLessons = getCourseLessons

export const apiLesson = getLesson
export const apiGetLesson = getLesson

// --- 进度 ---
export const apiMyProgressList = getMyProgressList
export const apiMyProgressOne = getMyProgressOne
export const apiMyProgress = getMyProgressSmart

export const apiProgressUpsert = upsertProgress
export const apiUpsertProgress = upsertProgress

// --- 默认导出（防御性保留，哪怕暂时不用）---
export default {
    apiCategories,
    apiCourses,
    apiCourse,
    apiCourseLessons,
    apiLesson,
    apiMyProgress,
    apiUpsertProgress
}
