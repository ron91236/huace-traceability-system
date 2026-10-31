// 订单状态映射
export const orderStatusMap: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

// 码包状态映射
export const codePackageStatusMap: Record<string, { label: string; type: string }> = {
  UNBOUND: { label: '未绑定', type: 'info' },
  PARTIAL: { label: '部分绑定', type: 'warning' },
  BOUND: { label: '已绑定', type: 'success' },
}

/**
 * 侧边栏按钮图标只在"看起来是个图片地址"时才渲染。企业端预览与消费者端必须用同一个判断，
 * 否则企业在自己后台看到图标、消费者那边却是空的。
 */
export function isIconImage(v?: string | null) {
  return !!v && /\.(png|jpe?g|gif|webp|svg)(\?|$)/i.test(v)
}
