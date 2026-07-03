import { SECTION_FIELDS } from './section-fields'

export type LayoutType = 'free' | 'one-screen' | 'modular-cards' | 'split-left-right' | 'split-top-bottom' | 'tabs' | 'timeline'

export interface LayoutPreset {
  key: LayoutType
  label: string
  description: string
  icon: string
  thumbnail?: string
}

export const LAYOUT_PRESETS: LayoutPreset[] = [
  { key: 'free', label: '自由布局', description: '完全自由拖拽组合元素', icon: 'Edit' },
  { key: 'one-screen', label: '一屏到底', description: '单页纵向滚动，信息连贯', icon: 'ArrowDown' },
  { key: 'modular-cards', label: '模块化卡片', description: '每个信息模块为独立卡片', icon: 'Grid' },
  { key: 'split-left-right', label: '左图右文', description: '左侧主图，右侧信息', icon: 'Picture' },
  { key: 'split-top-bottom', label: '上图下文', description: '顶部大图，底部信息', icon: 'Top' },
  { key: 'tabs', label: '标签页导航', description: '顶部Tab切换不同模块', icon: 'DocumentCopy' },
  { key: 'timeline', label: '时间轴', description: '纵向时间线串联各阶段', icon: 'Clock' },
]

export function getDefaultLayout(): LayoutType {
  return 'free'
}

export function getLayoutLabel(key: LayoutType): string {
  return LAYOUT_PRESETS.find(p => p.key === key)?.label || key
}

// 生成通用页面元素结构
export interface PageElement {
  id: string
  type: string
  label: string
  [key: string]: any
}

export interface PageData {
  id: string
  name: string
  elements: PageElement[]
}

export function createBlankPages(genId: (prefix: string) => string): PageData[] {
  return [{ id: genId('page'), name: '首页', elements: [] }]
}

// 生猪溯源默认信息模块
function pigInfoSections(genId: (prefix: string) => string): PageElement[] {
  const sectionTypes = ['enterprise-info', 'product-info', 'base-info', 'breed-archive', 'farm-info', 'transport-info', 'slaughter-info', 'cutting-record', 'test-info', 'cert-info']
  return sectionTypes.map(type => {
    const def = SECTION_FIELDS[type]
    return {
      id: genId('el'),
      type,
      label: def?.title || type,
      selectedFields: def?.fields.slice(0, 6).map((f: any) => f.field) || [],
      style: {},
    }
  })
}

// 分屏布局：左图右文（首页）
export function createSplitLeftRightPages(genId: (prefix: string) => string): PageData[] {
  return [{
    id: genId('page'),
    name: '首页',
    elements: [
      { id: genId('el'), type: 'image', label: '主图', src: '', style: { width: '48%', height: 'auto', borderRadius: 12, float: 'left' as const, margin: '0 12px 12px 0' } },
      { id: genId('el'), type: 'text', label: '产品标题', content: '产品名称', style: { fontSize: 20, fontWeight: 'bold', margin: '0 0 8px' } },
      { id: genId('el'), type: 'text', label: '副标题', content: '安全溯源 · 品质保障', style: { fontSize: 14, color: '#666', margin: '0 0 12px' } },
      ...pigInfoSections(genId).slice(0, 4),
      { id: genId('el'), type: 'button', label: '查看完整信息', buttonType: 'page', targetPageId: '', style: { width: '100%', backgroundColor: '#059669', color: '#fff' } },
    ],
  }, {
    id: genId('page'),
    name: '详情',
    elements: [
      ...pigInfoSections(genId).slice(4),
      { id: genId('el'), type: 'anti-counterfeit', label: '防伪验证' },
    ],
  }]
}

// 上图下文布局
export function createSplitTopBottomPages(genId: (prefix: string) => string): PageData[] {
  return [{
    id: genId('page'),
    name: '首页',
    elements: [
      { id: genId('el'), type: 'image', label: '顶部大图', src: '', style: { width: '100%', height: '220px', borderRadius: 0 } },
      { id: genId('el'), type: 'text', label: '标题', content: '产品溯源档案', style: { fontSize: 22, fontWeight: 'bold', textAlign: 'center', margin: '16px 0' } },
      ...pigInfoSections(genId),
      { id: genId('el'), type: 'anti-counterfeit', label: '防伪验证' },
    ],
  }]
}

// 一屏到底布局：单页纵向滚动
export function createOneScreenPages(genId: (prefix: string) => string): PageData[] {
  return [{
    id: genId('page'),
    name: '首页',
    elements: [
      { id: genId('el'), type: 'image', label: '头图', src: '', style: { width: '100%', height: '200px', borderRadius: 0 } },
      { id: genId('el'), type: 'text', label: '品牌宣言', content: '全程可追溯，安心看得见', style: { fontSize: 18, fontWeight: 'bold', textAlign: 'center', margin: '12px 0' } },
      { id: genId('el'), type: 'divider', label: '分割线' },
      ...pigInfoSections(genId),
      { id: genId('el'), type: 'anti-counterfeit', label: '防伪验证' },
      { id: genId('el'), type: 'text', label: '页脚', content: '本页面由溯源中台提供技术支持', style: { fontSize: 12, color: '#999', textAlign: 'center', margin: '20px 0' } },
    ],
  }]
}

// 模块化卡片布局：信息模块独立卡片
export function createModularCardsPages(genId: (prefix: string) => string): PageData[] {
  return [{
    id: genId('page'),
    name: '首页',
    elements: [
      { id: genId('el'), type: 'image', label: '头图', src: '', style: { width: '100%', height: '160px', borderRadius: 12 } },
      ...pigInfoSections(genId).map(el => ({ ...el, style: { ...el.style, width: '48%' } })),
      { id: genId('el'), type: 'anti-counterfeit', label: '防伪验证', style: { width: '100%' } },
    ],
  }]
}

// 标签页导航布局：每个模块一个页面
export function createTabsPages(genId: (prefix: string) => string): PageData[] {
  const sections = pigInfoSections(genId)
  return [
    {
      id: genId('page'),
      name: '概览',
      elements: [
        { id: genId('el'), type: 'image', label: '产品主图', src: '', style: { width: '100%', height: '180px', borderRadius: 12 } },
        sections[0],
        sections[1],
        { id: genId('el'), type: 'button', label: '开始溯源', buttonType: 'page', targetPageId: '', style: { width: '100%' } },
      ],
    },
    ...sections.slice(2).map(sec => ({
      id: genId('page'),
      name: sec.label,
      elements: [sec],
    })),
    {
      id: genId('page'),
      name: '防伪',
      elements: [{ id: genId('el'), type: 'anti-counterfeit', label: '防伪验证' }],
    },
  ]
}

// 时间轴布局：按阶段排列
export function createTimelinePages(genId: (prefix: string) => string): PageData[] {
  const sections = pigInfoSections(genId)
  return [{
    id: genId('page'),
    name: '首页',
    elements: [
      { id: genId('el'), type: 'text', label: '时间轴标题', content: '产品全生命周期追溯', style: { fontSize: 20, fontWeight: 'bold', textAlign: 'center', margin: '12px 0' } },
      ...sections.map((sec, idx) => ({
        ...sec,
        style: { ...sec.style, marginLeft: idx % 2 === 0 ? '0' : '20%' },
      })),
      { id: genId('el'), type: 'anti-counterfeit', label: '防伪验证' },
    ],
  }]
}

export function createPagesByLayout(layout: LayoutType, genId: (prefix: string) => string): PageData[] {
  switch (layout) {
    case 'one-screen': return createOneScreenPages(genId)
    case 'modular-cards': return createModularCardsPages(genId)
    case 'split-left-right': return createSplitLeftRightPages(genId)
    case 'split-top-bottom': return createSplitTopBottomPages(genId)
    case 'tabs': return createTabsPages(genId)
    case 'timeline': return createTimelinePages(genId)
    default: return createBlankPages(genId)
  }
}
