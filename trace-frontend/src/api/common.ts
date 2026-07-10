import request from './request'

// 通用文件上传
export const uploadFile = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload', formData)
}

// 下拉数据
export const getProductOptions = () => request.get('/common/products')
export const getLabelSpecOptions = () => request.get('/common/label-specs')
export const getCertTypeOptions = () => request.get('/common/cert-types')
export const getTraceTemplateOptions = () => request.get('/common/trace-templates')

// 溯源查询（公开）
export const getTraceInfo = (serialNo: string) => request.get(`/trace/${serialNo}`)
export const getBatchTraceInfo = (batchId: number) => request.get(`/trace/batch/${batchId}`)
export const getTraceTemplate = (templateKey: string) => request.get(`/trace/template/${templateKey}`)
export const verifyAntiFake = (data: { serialNo: string; antiFakeCode: string }) => request.post('/trace/verify', data)
export const directVerify = (serialNo: string) => request.get(`/trace/direct-verify/${serialNo}`)
export const getCertPublicInfo = (id: number) => request.get(`/trace/cert/${id}`)

// 溯源扩展数据（视频/IoT - 公开）
export const getTraceVideos = (serialNo: string) => request.get(`/trace/${serialNo}/videos`)
export const getTraceIotLatest = (serialNo: string) => request.get(`/trace/${serialNo}/iot/latest`)
export const getTraceTemperature = (serialNo: string) => request.get(`/trace/${serialNo}/iot/temperature`)
export const getTraceGpsTrack = (serialNo: string) => request.get(`/trace/${serialNo}/iot/gps-track`)
export const getBatchVideos = (batchId: number) => request.get(`/trace/batch/${batchId}/videos`)
export const getBatchIotLatest = (batchId: number) => request.get(`/trace/batch/${batchId}/iot/latest`)

// PDF转图片
export const convertPdfToImages = (url: string) => request.get(`/trace/pdf-images`, { params: { url } })
