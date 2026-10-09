import request from './request'

// 上传前压缩图片（最长边≤2000px、质量0.85），降低扫码页/报告页大图加载耗时
async function compressImage(file: File): Promise<File> {
  if (!file.type.startsWith('image/') || file.type === 'image/gif' || file.size <= 300 * 1024) return file
  const objectUrl = URL.createObjectURL(file)
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image()
      el.onload = () => resolve(el)
      el.onerror = () => reject(new Error('image decode failed'))
      el.src = objectUrl
    })
    const maxSide = 2000
    if (Math.max(img.width, img.height) <= maxSide) return file
    const scale = maxSide / Math.max(img.width, img.height)
    const width = Math.round(img.width * scale)
    const height = Math.round(img.height * scale)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(img, 0, 0, width, height)
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85))
    if (!blob || blob.size >= file.size) return file
    return new File([blob], file.name.replace(/\.\w+$/, '.jpg'), { type: 'image/jpeg' })
  } catch {
    return file
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

// 通用文件上传
export const uploadFile = async (file: File) => {
  const formData = new FormData()
  formData.append('file', await compressImage(file))
  return request.post('/upload', formData)
}

// 下拉数据
export const getProductOptions = () => request.get('/common/products')
export const getCertTypeOptions = () => request.get('/common/cert-types')
export const getTraceTemplateOptions = (enterpriseId?: number) => request.get('/common/trace-templates', { params: { enterpriseId } })

// 溯源查询（公开）
export const getTraceInfo = (serialNo: string) => request.get(`/trace/${serialNo}`)
export const getBatchTraceInfo = (batchId: number) => request.get(`/trace/batch/${batchId}`)
export const getTraceTemplate = (templateKey: string) => request.get(`/trace/template/${templateKey}`)
export const verifyAntiFake = (data: { serialNo: string; antiFakeCode: string }) => request.post('/trace/verify', data)
export const directVerify = (serialNo: string) => request.get(`/trace/direct-verify/${serialNo}`)
export const getCertPublicInfo = (id: number) => request.get(`/trace/cert/${id}`)

// 承诺达标合格证（公开）
export const getHgzPublic = (code: string) => request.get(`/hgz/${code}`)

// 溯源扩展数据（视频/IoT - 公开）
export const getTraceVideos = (serialNo: string) => request.get(`/trace/${serialNo}/videos`)
export const getTraceIotLatest = (serialNo: string) => request.get(`/trace/${serialNo}/iot/latest`)
export const getTraceTemperature = (serialNo: string) => request.get(`/trace/${serialNo}/iot/temperature`)
export const getTraceGpsTrack = (serialNo: string) => request.get(`/trace/${serialNo}/iot/gps-track`)
export const getBatchVideos = (batchId: number) => request.get(`/trace/batch/${batchId}/videos`)
export const getBatchIotLatest = (batchId: number) => request.get(`/trace/batch/${batchId}/iot/latest`)

// PDF转图片
export const convertPdfToImages = (url: string) => request.get(`/trace/pdf-images`, { params: { url } })
