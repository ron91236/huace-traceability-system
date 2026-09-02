import request from './request'

// 企业信息
export const getEnterpriseProfile = () => request.get('/enterprise/profile')
export const updateEnterpriseProfile = (data: any) => request.put('/enterprise/profile', data)

// 企业认证（只读）
export const getEnterpriseCerts = (params: any) => request.get('/enterprise/certs', { params })

// 基地管理
export const getBases = (params: any) => request.get('/enterprise/bases', { params })
export const updateBase = (id: number, data: any) => request.put(`/enterprise/bases/${id}`, data)

// 商品管理
export const getGoods = (params: any) => request.get('/enterprise/goods', { params })
export const createGoods = (data: any) => request.post('/enterprise/goods', data)
export const updateGoods = (id: number, data: any) => request.put(`/enterprise/goods/${id}`, data)
export const deleteGoods = (id: number) => request.delete(`/enterprise/goods/${id}`)
export const copyGoods = (id: number) => request.post(`/enterprise/goods/${id}/copy`)

// 收货地址
export const getAddresses = (params: any) => request.get('/enterprise/addresses', { params })
export const createAddress = (data: any) => request.post('/enterprise/addresses', data)
export const updateAddress = (id: number, data: any) => request.put(`/enterprise/addresses/${id}`, data)
export const deleteAddress = (id: number) => request.delete(`/enterprise/addresses/${id}`)

// 批次管理
export const getBatches = (params: any) => request.get('/enterprise/batches', { params })
export const createBatch = (data: any) => request.post('/enterprise/batches', data)
export const updateBatch = (id: number, data: any) => request.put(`/enterprise/batches/${id}`, data)
export const getBatchQrcode = (id: number) => request.get(`/enterprise/batches/${id}/qrcode`)
export const copyBatch = (id: number) => request.post(`/enterprise/batches/${id}/copy`)

// 订单管理
export const getOrders = (params: any) => request.get('/enterprise/orders', { params })
export const createOrder = (data: any) => request.post('/enterprise/orders', data)
export const deleteOrder = (id: number) => request.delete(`/enterprise/orders/${id}`)
export const submitOrder = (id: number) => request.post(`/enterprise/orders/${id}/submit`)
export const getOrderDetail = (id: number) => request.get(`/enterprise/orders/${id}`)
export const getAuditHistory = (id: number) => request.get(`/enterprise/orders/${id}/audit-history`)

// 订单明细
export const addOrderItem = (data: any) => request.post('/enterprise/order-items', data)
export const updateOrderItem = (id: number, data: any) => request.put(`/enterprise/order-items/${id}`, data)
export const deleteOrderItem = (id: number) => request.delete(`/enterprise/order-items/${id}`)

// 订单条码
export const getOrderCodes = (params: any) => request.get('/enterprise/order-codes', { params })
export const updateOrderCode = (id: number, data: any) => request.put(`/enterprise/order-codes/${id}`, data)
export const previewOrderCode = (id: number) => request.get(`/enterprise/order-codes/${id}/preview`)

// 条码使用
export const getCodeUsages = (params: any) => request.get('/enterprise/code-usages', { params })
export const createCodeUsage = (data: any) => request.post('/enterprise/code-usages', data)
export const deleteCodeUsage = (id: number) => request.delete(`/enterprise/code-usages/${id}`)

// 公告（只读）
export const getNotices = (params: any) => request.get('/enterprise/notices', { params })

// 控制台
export const getDashboard = () => request.get('/enterprise/dashboard/stats')

// 检测报告管理
export const getTestReports = (params: any) => request.get('/enterprise/test-reports', { params })
export const getAllTestReports = () => request.get('/enterprise/test-reports/all')
export const createTestReport = (data: any) => request.post('/enterprise/test-reports', data)
export const updateTestReport = (id: number, data: any) => request.put(`/enterprise/test-reports/${id}`, data)
export const deleteTestReport = (id: number) => request.delete(`/enterprise/test-reports/${id}`)

// 企业集团
export const getGroupChildren = () => request.get('/enterprise/group/children')

// 数据大屏
export const getEntDataScreen = () => request.get('/enterprise/data-screen/all')

// 视频源管理
export const getVideoSources = (params: any) => request.get('/enterprise/video-sources', { params })
export const createVideoSource = (data: any) => request.post('/enterprise/video-sources', data)
export const updateVideoSource = (id: number, data: any) => request.put(`/enterprise/video-sources/${id}`, data)
export const deleteVideoSource = (id: number) => request.delete(`/enterprise/video-sources/${id}`)

// IoT 设备管理
export const getIotDevices = (params: any) => request.get('/enterprise/iot-devices', { params })
export const createIotDevice = (data: any) => request.post('/enterprise/iot-devices', data)
export const updateIotDevice = (id: number, data: any) => request.put(`/enterprise/iot-devices/${id}`, data)
export const deleteIotDevice = (id: number) => request.delete(`/enterprise/iot-devices/${id}`)
export const getDeviceLatest = (id: number) => request.get(`/enterprise/iot-devices/${id}/latest`)
export const getDeviceHistory = (id: number, params: any) => request.get(`/enterprise/iot-devices/${id}/history`, { params })

// IoT 告警
export const getIotAlerts = (params: any) => request.get('/enterprise/iot-alerts', { params })
export const handleIotAlert = (id: number, data: any) => request.put(`/enterprise/iot-alerts/${id}/handle`, data)
export const getIotAlertRules = (params: any) => request.get('/enterprise/iot-alert-rules', { params })
export const createIotAlertRule = (data: any) => request.post('/enterprise/iot-alert-rules', data)

// 企业分配的溯源模板
export const getAssignedTemplates = () => request.get('/enterprise/assigned-templates')

// 企业可用标签规格（按证书类型过滤）
export const getEnterpriseLabelSpecs = () => request.get('/enterprise/label-specs')

// 承诺达标合格证
export const getHgzList = (params: any) => request.get('/enterprise/hgz', { params })
export const getHgzDefaults = () => request.get('/enterprise/hgz/defaults')
export const createHgz = (data: any) => request.post('/enterprise/hgz', data)
export const getHgzDetail = (id: number) => request.get(`/enterprise/hgz/${id}`)
export const updateHgz = (id: number, data: any) => request.put(`/enterprise/hgz/${id}`, data)
export const voidHgz = (id: number) => request.post(`/enterprise/hgz/${id}/void`)
export const getHgzQrcode = (id: number) => request.get(`/enterprise/hgz/${id}/qrcode`)
