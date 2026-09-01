/**
 * 溯源模板 - 信息模块字段定义（共享常量）
 * TemplateEditor.vue 和 TraceView.vue 共用，避免重复定义和同步风险
 */

export interface SectionField {
  field: string
  label: string
  type?: string
}

export interface SectionDefinition {
  title: string
  fields: SectionField[]
}

/** 信息模块类型 -> 数据路径前缀 */
export const SECTION_TYPE_MAP: Record<string, string> = {
  'enterprise-info': 'enterprise',
  'product-info': 'goods',
  'cert-info': 'cert',
  'test-info': 'batch',
  'base-info': 'base',
  'video-monitor': 'videoSources',
  'iot-environment': 'iotReadings',
  'vr-panorama': 'vrScenes',
}

/** 信息模块字段定义（编辑器 + 渲染共用） */
export const SECTION_FIELDS: Record<string, SectionDefinition> = {
  'enterprise-info': { title: '企业信息', fields: [
    { field: 'enterprise.name', label: '企业名称' }, { field: 'enterprise.introduction', label: '企业简介', type: 'text' },
    { field: 'enterprise.addressFull', label: '企业地址' }, { field: 'enterprise.contact', label: '联系人' },
    { field: 'enterprise.phone', label: '联系电话' }, { field: 'enterprise.creditCode', label: '信用代码' },
    { field: 'enterprise.licenseImage', label: '营业执照', type: 'image' }, { field: 'enterprise.enterpriseImage', label: '企业图片', type: 'image' },
    { field: 'enterprise.honors', label: '荣誉资质', type: 'rich-text' }, { field: 'enterprise.qualifications', label: '资质证书', type: 'rich-text' },
    { field: 'enterprise.mainType', label: '主营类型' },
    { field: 'enterprise.promoVideo', label: '宣传视频', type: 'video' },
  ]},
  'product-info': { title: '产品信息', fields: [
    { field: 'goods.name', label: '商品名称' }, { field: 'goods.introduction', label: '产品介绍', type: 'text' },
    { field: 'goods.packageSpec', label: '包装规格' }, { field: 'goods.weightSpec', label: '重量规格' },
    { field: 'goods.sampleImage', label: '产品图片', type: 'image' }, { field: 'goods.storageMethod', label: '储存方式' },
    { field: 'goods.eatingMethod', label: '食用方式' }, { field: 'goods.promoImage', label: '宣传图片', type: 'image' },
  ]},
  'cert-info': { title: '认证信息', fields: [
    { field: 'cert.certName', label: '证书名称' }, { field: 'cert.productName', label: '产品名称' },
    { field: 'cert.startDate', label: '开始日期' }, { field: 'cert.endDate', label: '结束日期' },
    { field: 'cert.certImage', label: '证书图片', type: 'image' },
  ]},
  'test-info': { title: '检测信息', fields: [
    { field: 'batch.name', label: '批次名称' }, { field: 'batch.goodsSpec', label: '商品规格' },
    { field: 'batch.testCode', label: '检测编号' }, { field: 'batch.testReport', label: '检测报告', type: 'file' },
    { field: 'batch.testOrg', label: '检测机构' }, { field: 'batch.testTime', label: '检测时间' },
    { field: 'batch.testMethod', label: '检测方法' }, { field: 'batch.testBasis', label: '检测依据' },
    { field: 'batch.testType', label: '检测类型' }, { field: 'batch.testResult', label: '检测结果', type: 'badge' },
  ]},
  'base-info': { title: '基地信息', fields: [
    { field: 'base.name', label: '基地名称' }, { field: 'base.code', label: '基地编码' },
    { field: 'base.areaDisplay', label: '基地面积' }, { field: 'base.manager', label: '负责人' },
    { field: 'base.phone', label: '联系电话' }, { field: 'base.planImage', label: '规划图', type: 'image' },
    { field: 'base.realImage', label: '实景图', type: 'image' }, { field: 'base.certification', label: '基地认证' },
    { field: 'base.envReport', label: '环境报告', type: 'file' }, { field: 'base.testItems', label: '检测项目' },
  ]},
  'video-monitor': { title: '视频监控', fields: [
    { field: 'videoSources.cameras', label: '摄像头列表', type: 'live-video' },
  ]},
  'iot-environment': { title: '环境监测', fields: [
    { field: 'iotReadings.temperature', label: '温度' },
    { field: 'iotReadings.humidity', label: '湿度' },
    { field: 'iotReadings.soilMoisture', label: '土壤湿度' },
    { field: 'iotReadings.ph', label: 'pH值' },
  ]},
  'vr-panorama': { title: 'VR全景导览', fields: [
    { field: 'vrScenes.scenes', label: '全景场景', type: 'vr-panorama' },
  ]},
}

/** 判断是否为信息模块类型 */
export function isInfoSection(type: string): boolean {
  return type in SECTION_FIELDS
}

/** 获取信息模块全部字段 */
export function getSectionAllFields(type: string): SectionField[] {
  return SECTION_FIELDS[type]?.fields || []
}

/** 获取信息模块数据路径前缀 */
export function getSectionDataPrefix(type: string): string {
  return SECTION_TYPE_MAP[type] || ''
}
