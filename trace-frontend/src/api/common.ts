import request from './request'

// 上传前压缩图片（宽度≤1080px、优先 webp），降低扫码页/海报页大图加载耗时
async function compressImage(file: File): Promise<File> {
  if (!file.type.startsWith('image/') || file.type === 'image/gif' || file.size <= 200 * 1024) return file
  const objectUrl = URL.createObjectURL(file)
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image()
      el.onload = () => resolve(el)
      el.onerror = () => reject(new Error('image decode failed'))
      el.src = objectUrl
    })
    // 手机端按整宽展示、纵向滚动，故约束宽度而非最长边（长图海报需保留高度）
    const maxWidth = 1080
    const scale = Math.min(1, maxWidth / img.width)
    if (scale === 1 && file.size <= 300 * 1024) return file
    const width = Math.round(img.width * scale)
    const height = Math.round(img.height * scale)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    const isPng = file.type === 'image/png'
    if (!isPng) {
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, width, height)
    }
    ctx.drawImage(img, 0, 0, width, height)
    const webp = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.8))
    if (webp && webp.type === 'image/webp' && webp.size < file.size) {
      return new File([webp], file.name.replace(/\.\w+$/, '.webp'), { type: 'image/webp' })
    }
    let jpegSource: HTMLCanvasElement = canvas
    if (isPng) {
      jpegSource = document.createElement('canvas')
      jpegSource.width = width
      jpegSource.height = height
      const jctx = jpegSource.getContext('2d')
      if (!jctx) return file
      jctx.fillStyle = '#fff'
      jctx.fillRect(0, 0, width, height)
      jctx.drawImage(canvas, 0, 0)
    }
    const jpeg = await new Promise<Blob | null>((resolve) => jpegSource.toBlob(resolve, 'image/jpeg', 0.82))
    if (jpeg && jpeg.size < file.size) {
      return new File([jpeg], file.name.replace(/\.\w+$/, '.jpg'), { type: 'image/jpeg' })
    }
    return file
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
