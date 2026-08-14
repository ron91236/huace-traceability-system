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
const entParam = (enterpriseId?: number) => (enterpriseId ? { enterpriseId } : {})
export const getDlDashboard = (days = 7, enterpriseId?: number) =>
  request.get('/enterprise/dl/dashboard', { params: { days, ...entParam(enterpriseId) } })
export const getDlScanAnalysis = (enterpriseId?: number) =>
  request.get('/enterprise/dl/analysis/scan', { params: entParam(enterpriseId) })
export const getDlScanDetail = (versionId: number) => request.get('/enterprise/dl/analysis/scan/detail', { params: { versionId } })
export const getDlGeoAnalysis = (enterpriseId?: number) =>
  request.get('/enterprise/dl/analysis/geo', { params: entParam(enterpriseId) })
export const getDlLabelAnalysis = (days = 30, enterpriseId?: number) =>
  request.get('/enterprise/dl/analysis/label', { params: { days, ...entParam(enterpriseId) } })
export const getDlProductAnalysis = (days = 30, enterpriseId?: number) =>
  request.get('/enterprise/dl/analysis/product', { params: { days, ...entParam(enterpriseId) } })

// ==================== 用户管理 ====================
export const getDlUsers = (params: any) => request.get('/enterprise/dl/users', { params })

// ==================== 日志 ====================
export const getDlOperationLogs = (params: any) => request.get('/enterprise/dl/logs/operation', { params })
export const getDlLoginLogs = (params: any) => request.get('/enterprise/dl/logs/login', { params })
export const recordDlLogin = () => request.post('/enterprise/dl/logs/login')

// ==================== 食品分类 / 企业列表 ====================
export const getDlCategories = () => request.get('/enterprise/dl/categories')
/** 已创建数字标签的企业列表（管理员企业筛选用） */
export const getDlEnterprises = () => request.get('/enterprise/dl/enterprises')

// ==================== 消费者扫码（公开） ====================
export const getDlScanData = (barcode: string) => request.get(`/dl/scan/${barcode}`)
