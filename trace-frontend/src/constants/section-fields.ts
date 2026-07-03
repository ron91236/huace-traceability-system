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
  'breed-archive': 'breedArchive',
  'farm-info': 'farm',
  'transport-info': 'transport',
  'slaughter-info': 'slaughter',
  'cutting-record': 'cutting',
}

/** 信息模块字段定义（编辑器 + 渲染共用） */
export const SECTION_FIELDS: Record<string, SectionDefinition> = {
  'enterprise-info': { title: '企业信息', fields: [
    { field: 'enterprise.name', label: '企业名称' }, { field: 'enterprise.introduction', label: '企业简介', type: 'text' },
    { field: 'enterprise.addressFull', label: '企业地址' }, { field: 'enterprise.contact', label: '联系人' },
    { field: 'enterprise.phone', label: '联系电话' }, { field: 'enterprise.creditCode', label: '信用代码' },
    { field: 'enterprise.licenseImage', label: '营业执照', type: 'image' }, { field: 'enterprise.enterpriseImage', label: '企业图片', type: 'image' },
    { field: 'enterprise.honors', label: '荣誉资质', type: 'text' }, { field: 'enterprise.mainType', label: '主营类型' },
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
  'breed-archive': { title: '养殖档案', fields: [
    { field: 'breedArchive.batchNo', label: '养殖批次' }, { field: 'breedArchive.earTag', label: '耳标号' },
    { field: 'breedArchive.farmAddress', label: '养殖地址' }, { field: 'breedArchive.motherCode', label: '母猪编号' },
    { field: 'breedArchive.birthDate', label: '出生日期' }, { field: 'breedArchive.feedPeriod', label: '饲养周期' },
  ]},
  'farm-info': { title: '养殖场信息', fields: [
    { field: 'farm.name', label: '养殖场名称' }, { field: 'farm.legalPerson', label: '法人' },
    { field: 'farm.phone', label: '联系电话' }, { field: 'farm.address', label: '养殖场地址' },
    { field: 'farm.introduction', label: '养殖场简介', type: 'text' },
    { field: 'farm.feedingRecords', label: '饲养记录', type: 'text' },
    { field: 'farm.vaccinationRecords', label: '接种记录', type: 'text' },
  ]},
  'transport-info': { title: '调运信息', fields: [
    { field: 'transport.destination', label: '目的地' }, { field: 'transport.loadTime', label: '装车时间' },
    { field: 'transport.duration', label: '运输时长' }, { field: 'transport.vehicle', label: '车辆' },
    { field: 'transport.fromLat', label: '起点纬度' }, { field: 'transport.fromLng', label: '起点经度' },
    { field: 'transport.toLat', label: '终点纬度' }, { field: 'transport.toLng', label: '终点经度' },
  ]},
  'slaughter-info': { title: '屠宰信息', fields: [
    { field: 'slaughter.houseName', label: '屠宰场名称' }, { field: 'slaughter.legalPerson', label: '法人' },
    { field: 'slaughter.phone', label: '联系电话' }, { field: 'slaughter.address', label: '屠宰场地址' },
    { field: 'slaughter.entryTime', label: '入场时间' }, { field: 'slaughter.slaughterTime', label: '屠宰时间' },
    { field: 'slaughter.quarantineImage', label: '检疫证明', type: 'image' },
    { field: 'slaughter.qualityImage', label: '品质合格证', type: 'image' },
  ]},
  'cutting-record': { title: '分割记录', fields: [
    { field: 'cutting.cuttingTime', label: '分割时间' },
    { field: 'cutting.video', label: '分割视频', type: 'video' },
    { field: 'cutting.images', label: '分割照片', type: 'image' },
    { field: 'cutting.slaughterVideo', label: '屠宰视频', type: 'video' },
    { field: 'cutting.slaughterImages', label: '屠宰照片', type: 'image' },
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
