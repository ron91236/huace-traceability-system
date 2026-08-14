import request from './request'

// ==================== 商品 ====================
export const getDlProducts = (params: any) => request.get('/enterprise/dl/products', { params })
export const createDlProduct = (data: any) => request.post('/enterprise/dl/products', data)

// ==================== 标签版本 ====================
export const getDlVersions = (productId: number, params?: any) => request.get(`/enterprise/dl/products/${productId}/versions`, { params })
export const getDlVersion = (id: number) => request.get(`/enterprise/dl/versions/${id}`)
export const createDlVersion = (productId: number, copyFromId?: number) =>
  request.post(`/enterprise/dl/products/${productId}/versions`, null, { params: copyFromId ? { copyFromId } : {} })
export const updateDlVersion = (id: number, data: any) => request.put(`/enterprise/dl/versions/${id}`, data)
export const deleteDlVersion = (id: number) => request.delete(`/enterprise/dl/versions/${id}`)
export const publishDlVersion = (id: number) => request.put(`/enterprise/dl/versions/${id}/publish`)
export const offlineDlVersion = (id: number) => request.put(`/enterprise/dl/versions/${id}/offline`)

// ==================== 商品同步 ====================
export const manualDlSync = (data: any) => request.post('/enterprise/dl/sync', data)
export const getDlSyncRecords = (params: any) => request.get('/enterprise/dl/sync/records', { params })

// ==================== 数据分析 ====================
export const getDlDashboard = (days = 7) => request.get('/enterprise/dl/dashboard', { params: { days } })
export const getDlScanAnalysis = () => request.get('/enterprise/dl/analysis/scan')
export const getDlScanDetail = (versionId: number) => request.get('/enterprise/dl/analysis/scan/detail', { params: { versionId } })
export const getDlGeoAnalysis = () => request.get('/enterprise/dl/analysis/geo')
export const getDlLabelAnalysis = (days = 30) => request.get('/enterprise/dl/analysis/label', { params: { days } })
export const getDlProductAnalysis = (days = 30) => request.get('/enterprise/dl/analysis/product', { params: { days } })

// ==================== 用户管理 ====================
export const getDlUsers = (params: any) => request.get('/enterprise/dl/users', { params })

// ==================== 日志 ====================
export const getDlOperationLogs = (params: any) => request.get('/enterprise/dl/logs/operation', { params })
export const getDlLoginLogs = (params: any) => request.get('/enterprise/dl/logs/login', { params })
export const recordDlLogin = () => request.post('/enterprise/dl/logs/login')

// ==================== 食品分类 ====================
export const getDlCategories = () => request.get('/enterprise/dl/categories')

// ==================== 消费者扫码（公开） ====================
export const getDlScanData = (barcode: string) => request.get(`/dl/scan/${barcode}`)
