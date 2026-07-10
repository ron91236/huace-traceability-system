import request from './request'
import { useUserStore } from '@/stores/user'

// 证书类型
export const getCertTypes = (params: any) => request.get('/admin/cert-types', { params })
export const createCertType = (data: any) => request.post('/admin/cert-types', data)
export const updateCertType = (id: number, data: any) => request.put(`/admin/cert-types/${id}`, data)
export const deleteCertType = (id: number) => request.delete(`/admin/cert-types/${id}`)

// 企业管理
export const getEnterprises = (params: any) => request.get('/admin/enterprises', { params })
export const createEnterprise = (data: any) => request.post('/admin/enterprises', data)
export const updateEnterprise = (id: number, data: any) => request.put(`/admin/enterprises/${id}`, data)
export const deleteEnterprise = (id: number) => request.delete(`/admin/enterprises/${id}`)
export const getAllEnterprises = () => request.get('/admin/enterprises/all')

// 企业认证
export const getEnterpriseCerts = (params: any) => request.get('/admin/enterprise-certs', { params })
export const createEnterpriseCert = (data: any) => request.post('/admin/enterprise-certs', data)
export const updateEnterpriseCert = (id: number, data: any) => request.put(`/admin/enterprise-certs/${id}`, data)
export const deleteEnterpriseCert = (id: number) => request.delete(`/admin/enterprise-certs/${id}`)
export const getCertQrcode = (id: number) => request.get(`/admin/enterprise-certs/${id}/qrcode`)

// 产品管理
export const getProducts = (params: any) => request.get('/admin/products', { params })
export const createProduct = (data: any) => request.post('/admin/products', data)
export const updateProduct = (id: number, data: any) => request.put(`/admin/products/${id}`, data)
export const deleteProduct = (id: number) => request.delete(`/admin/products/${id}`)

// 标签规格
export const getLabelSpecs = (params: any) => request.get('/admin/label-specs', { params })
export const createLabelSpec = (data: any) => request.post('/admin/label-specs', data)
export const updateLabelSpec = (id: number, data: any) => request.put(`/admin/label-specs/${id}`, data)
export const deleteLabelSpec = (id: number) => request.delete(`/admin/label-specs/${id}`)

// 企业基地
export const getBases = (params: any) => request.get('/admin/bases', { params })
export const createBase = (data: any) => request.post('/admin/bases', data)
export const updateBase = (id: number, data: any) => request.put(`/admin/bases/${id}`, data)
export const deleteBase = (id: number) => request.delete(`/admin/bases/${id}`)

// 商品管理（只读）
export const getAdminGoods = (params: any) => request.get('/admin/goods', { params })

// 订单管理
export const getAdminOrders = (params: any) => request.get('/admin/orders', { params })
export const getAdminOrderDetail = (id: number) => request.get(`/admin/orders/${id}`)
export const approveOrder = (id: number, data?: any) => request.post(`/admin/orders/${id}/approve`, data)
export const rejectOrder = (id: number, data: any) => request.post(`/admin/orders/${id}/reject`, data)
export const exportOrders = async (params: any) => {
  const userStore = useUserStore()
  const query = new URLSearchParams(params).toString()
  const res = await fetch(`/api/admin/orders/export?${query}`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  if (!res.ok) throw new Error('导出失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '订单数据.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

// 订单条码绑定
export const getOrderCodes = (orderId: number) => request.get(`/admin/orders/${orderId}/codes`)
export const bindOrderCode = (orderId: number, data: any) => request.post(`/admin/orders/${orderId}/codes`, data)
export const deleteOrderCode = (id: number) => request.delete(`/admin/order-codes/${id}`)
export const getLastSerial = (orderId: number) => request.get(`/admin/orders/${orderId}/last-serial`)

// 预览码
export const previewOrderCodeQrcode = (id: number) => request.get(`/admin/order-codes/${id}/preview-qrcode`)

// 码包管理
export const getCodePackages = (params: any) => request.get('/admin/code-packages', { params })
export const getAllCodePackages = () => request.get('/admin/code-packages/all')
export const getCodeDistribution = (params: any) => request.get('/admin/code-distribution', { params })
export const importCodePackage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/code-packages/import', formData)
}
export const getCodePackageDetail = (id: number) => request.get(`/admin/code-packages/${id}`)
export const deleteCodePackage = (id: number) => request.delete(`/admin/code-packages/${id}`)
export const generateCodePackage = (data: any) => request.post('/admin/code-packages/generate', data)
export const getLastSerialByRule = (serialDigits: number) => request.get('/admin/code-packages/last-serial', { params: { serialDigits } })
export const exportCodePackage = async (id: number) => {
  const userStore = useUserStore()
  const res = await fetch(`/api/admin/code-packages/${id}/export`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  if (!res.ok) throw new Error('导出失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `code_package_${id}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// 作废码计数
export const getVoidedCount = (serialStart: string, serialEnd: string) =>
  request.get('/admin/voided-count', { params: { serialStart, serialEnd } })

// 公告管理
export const getAdminNotices = (params: any) => request.get('/admin/notices', { params })
export const createNotice = (data: any) => request.post('/admin/notices', data)
export const deleteNotice = (id: number) => request.delete(`/admin/notices/${id}`)

// 控制台统计
export const getAdminDashboard = () => request.get('/admin/dashboard/stats')

// 溯源码作废管理
export const getVoidedCodeRanges = (params: any) => request.get('/admin/voided-code-ranges', { params })
export const batchImportVoidedCodeRanges = (data: any[]) => request.post('/admin/voided-code-ranges/batch', data)
export const deleteVoidedCodeRange = (id: number) => request.delete(`/admin/voided-code-ranges/${id}`)

// 证书产品管理
export const getCertProducts = (certId: number) => request.get(`/admin/enterprise-certs/${certId}/products`)
export const addCertProduct = (certId: number, data: any) => request.post(`/admin/enterprise-certs/${certId}/products`, data)
export const removeCertProduct = (id: number) => request.delete(`/admin/cert-products/${id}`)

// 溯源模板管理
export const getTraceTemplates = (params: any) => request.get('/admin/trace-templates', { params })
export const createTraceTemplate = (data: any) => request.post('/admin/trace-templates', data)
export const updateTraceTemplate = (id: number, data: any) => request.put(`/admin/trace-templates/${id}`, data)
export const deleteTraceTemplate = (id: number) => request.delete(`/admin/trace-templates/${id}`)

// 企业集团管理
export const createMasterEnterprise = (data: any) => request.post('/admin/enterprises/master', data)
export const createChildEnterprise = (parentId: number, data: any) => request.post(`/admin/enterprises/${parentId}/children`, data)
export const getChildEnterprises = (parentId: number) => request.get(`/admin/enterprises/${parentId}/children`)
export const removeChildEnterprise = (parentId: number, childId: number) => request.delete(`/admin/enterprises/${parentId}/children/${childId}`)

// 数据大屏
export const getAdminDataScreen = () => request.get('/admin/data-screen/all')

// 视频源总览
export const getAdminVideoSources = (params: any) => request.get('/admin/video-sources', { params })

// IoT设备总览
export const getAdminIotDevices = (params: any) => request.get('/admin/iot-devices', { params })

// IoT告警总览
export const getAdminIotAlerts = (params: any) => request.get('/admin/iot-alerts', { params })
