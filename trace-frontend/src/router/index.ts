import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 华测管理端路由
const adminRoutes: RouteRecordRaw[] = [
  { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '控制台' } },
  { path: 'cert-type', name: 'AdminCertType', component: () => import('@/views/admin/cert-type/CertTypeList.vue'), meta: { title: '证书类型管理' } },
  { path: 'enterprise', name: 'AdminEnterprise', component: () => import('@/views/admin/enterprise/EnterpriseList.vue'), meta: { title: '企业管理' } },
  { path: 'enterprise-cert', name: 'AdminEnterpriseCert', component: () => import('@/views/admin/enterprise-cert/EnterpriseCertList.vue'), meta: { title: '企业认证' } },
  { path: 'product', name: 'AdminProduct', component: () => import('@/views/admin/product/ProductList.vue'), meta: { title: '产品管理' } },
  { path: 'label-spec', name: 'AdminLabelSpec', component: () => import('@/views/admin/label-spec/LabelSpecList.vue'), meta: { title: '标签规格管理' } },
  { path: 'base-manage', name: 'AdminBaseManage', component: () => import('@/views/admin/base-manage/BaseManageList.vue'), meta: { title: '企业基地管理' } },
  { path: 'goods', name: 'AdminGoods', component: () => import('@/views/admin/goods/GoodsList.vue'), meta: { title: '商品管理' } },
  { path: 'order', name: 'AdminOrder', component: () => import('@/views/admin/order/OrderList.vue'), meta: { title: '订单管理' } },
  { path: 'order/:id', name: 'AdminOrderDetail', component: () => import('@/views/admin/order/OrderDetail.vue'), meta: { title: '订单详情' } },
  { path: 'code-platform', name: 'AdminCodePlatform', component: () => import('@/views/admin/code-platform/CodePlatformView.vue'), meta: { title: '码中台' } },
  { path: 'code-package', name: 'AdminCodePackage', component: () => import('@/views/admin/code-package/CodePackageList.vue'), meta: { title: '码包管理' } },
  { path: 'code-void', name: 'AdminCodeVoid', component: () => import('@/views/admin/code-void/CodeVoidList.vue'), meta: { title: '溯源码作废' } },
  { path: 'code-distribution', name: 'AdminCodeDistribution', component: () => import('@/views/admin/code-distribution/DistributionList.vue'), meta: { title: '发放管理' } },
  { path: 'trace-template', name: 'AdminTraceTemplate', component: () => import('@/views/admin/trace-template/TemplateList.vue'), meta: { title: '溯源模板管理' } },
  { path: 'notice', name: 'AdminNotice', component: () => import('@/views/admin/notice/NoticeList.vue'), meta: { title: '公告管理' } },
  { path: 'system/user', name: 'AdminUser', component: () => import('@/views/admin/system/UserList.vue'), meta: { title: '用户管理' } },
  { path: 'video-source', name: 'AdminVideoSource', component: () => import('@/views/admin/video-source/VideoSourceList.vue'), meta: { title: '视频源总览' } },
  { path: 'iot-device', name: 'AdminIotDevice', component: () => import('@/views/admin/iot-device/IotDeviceList.vue'), meta: { title: 'IoT设备总览' } },
  { path: 'vr-manage', name: 'AdminVrManage', component: () => import('@/views/admin/vr-manage/VrManageList.vue'), meta: { title: 'VR全景管理' } },
  { path: 'poster', name: 'AdminPoster', component: () => import('@/views/admin/poster/PosterList.vue'), meta: { title: '海报管理' } },
]

// 企业端路由
const enterpriseRoutes: RouteRecordRaw[] = [
  { path: 'dashboard', name: 'EntDashboard', component: () => import('@/views/enterprise/DashboardView.vue'), meta: { title: '控制台' } },
  { path: 'cert', name: 'EntCert', component: () => import('@/views/enterprise/cert/CertView.vue'), meta: { title: '企业认证' } },
  { path: 'profile', name: 'EntProfile', component: () => import('@/views/enterprise/profile/ProfileEdit.vue'), meta: { title: '企业信息' } },
  { path: 'base', name: 'EntBase', component: () => import('@/views/enterprise/base/BaseList.vue'), meta: { title: '基地管理' } },
  { path: 'goods', name: 'EntGoods', component: () => import('@/views/enterprise/goods/GoodsList.vue'), meta: { title: '商品管理' } },
  { path: 'address', name: 'EntAddress', component: () => import('@/views/enterprise/address/AddressList.vue'), meta: { title: '收货地址' } },
  { path: 'test-report', name: 'EntTestReport', component: () => import('@/views/enterprise/test-report/TestReportList.vue'), meta: { title: '检测报告' } },
  { path: 'batch', name: 'EntBatch', component: () => import('@/views/enterprise/batch/BatchList.vue'), meta: { title: '批次管理' } },
  { path: 'order', name: 'EntOrder', component: () => import('@/views/enterprise/order/OrderList.vue'), meta: { title: '订单管理' } },
  { path: 'order/:id', name: 'EntOrderDetail', component: () => import('@/views/enterprise/order/OrderDetail.vue'), meta: { title: '订单详情' } },
  { path: 'order-code', name: 'EntOrderCode', component: () => import('@/views/enterprise/order-code/OrderCodeList.vue'), meta: { title: '订单条码' } },
  { path: 'code-usage', name: 'EntCodeUsage', component: () => import('@/views/enterprise/code-usage/CodeUsageList.vue'), meta: { title: '条码使用' } },
  { path: 'notice', name: 'EntNotice', component: () => import('@/views/enterprise/notice/NoticeList.vue'), meta: { title: '公告' } },
  { path: 'video-source', name: 'EntVideoSource', component: () => import('@/views/enterprise/video-source/VideoSourceList.vue'), meta: { title: '视频源管理' } },
  { path: 'iot-device', name: 'EntIotDevice', component: () => import('@/views/enterprise/iot-device/IotDeviceList.vue'), meta: { title: 'IoT设备管理' } },
  { path: 'iot-alert', name: 'EntIotAlert', component: () => import('@/views/enterprise/iot-alert/IotAlertList.vue'), meta: { title: 'IoT告警' } },
]

// 数字标签系统（独立布局，管理员与企业用户均可进入；管理员全局只读查看）
const dlRoutes: RouteRecordRaw[] = [
  { path: 'dashboard', name: 'DlDashboard', component: () => import('@/views/enterprise/dl/DlDashboard.vue'), meta: { title: '数字标签工作台' } },
  { path: 'products', name: 'DlProducts', component: () => import('@/views/enterprise/dl/DlProductList.vue'), meta: { title: '商品管理' } },
  { path: 'products/:id/versions', name: 'DlVersions', component: () => import('@/views/enterprise/dl/DlVersionList.vue'), meta: { title: '标签版本管理' } },
  { path: 'versions/:id/edit', name: 'DlVersionEdit', component: () => import('@/views/enterprise/dl/DlVersionEdit.vue'), meta: { title: '编辑标签版本' } },
  { path: 'sync', name: 'DlSync', component: () => import('@/views/enterprise/dl/DlSync.vue'), meta: { title: '商品同步' } },
  { path: 'analysis/scan', name: 'DlScanAnalysis', component: () => import('@/views/enterprise/dl/DlScanAnalysis.vue'), meta: { title: '扫码分析' } },
  { path: 'analysis/label', name: 'DlLabelAnalysis', component: () => import('@/views/enterprise/dl/DlLabelAnalysis.vue'), meta: { title: '标签分析' } },
  { path: 'analysis/product', name: 'DlProductAnalysis', component: () => import('@/views/enterprise/dl/DlProductAnalysis.vue'), meta: { title: '商品分析' } },
  { path: 'users', name: 'DlUsers', component: () => import('@/views/enterprise/dl/DlUserManage.vue'), meta: { title: '用户管理' } },
  { path: 'logs/operation', name: 'DlOperationLog', component: () => import('@/views/enterprise/dl/DlOperationLog.vue'), meta: { title: '操作日志' } },
  { path: 'logs/login', name: 'DlLoginLog', component: () => import('@/views/enterprise/dl/DlLoginLog.vue'), meta: { title: '登录日志' } },
]

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/LoginView.vue'), meta: { title: '登录', public: true } },
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requireAuth: true, requireAdmin: true },
    children: adminRoutes,
  },
  {
    path: '/enterprise',
    component: () => import('@/components/EnterpriseLayout.vue'),
    redirect: '/enterprise/dashboard',
    meta: { requireAuth: true, requireEnterprise: true },
    children: enterpriseRoutes,
  },
  // 数字标签独立系统
  {
    path: '/dl',
    component: () => import('@/components/DlLayout.vue'),
    redirect: '/dl/dashboard',
    meta: { requireAuth: true },
    children: dlRoutes,
  },
  // 旧路径兼容重定向
  { path: '/enterprise/dl/:rest(.*)*', redirect: (to) => ({ path: '/dl/' + (to.params.rest as string[] || []).join('/') }) },
  // 数据大屏（独立全屏路由，不嵌入Layout）
  { path: '/screen/admin', name: 'AdminScreen', component: () => import('@/views/admin/DataScreenView.vue'), meta: { title: '数据大屏', requireAuth: true, requireAdmin: true } },
  { path: '/screen/enterprise', name: 'EntScreen', component: () => import('@/views/enterprise/DataScreenView.vue'), meta: { title: '数据大屏', requireAuth: true, requireEnterprise: true } },
  { path: '/trace/:serialNo', name: 'Trace', component: () => import('@/views/trace/TraceView.vue'), meta: { title: '溯源查询', public: true } },
  { path: '/trace/batch/:batchId', name: 'BatchTrace', component: () => import('@/views/trace/TraceView.vue'), meta: { title: '批次溯源', public: true } },
  { path: '/cert/:id', name: 'CertPublic', component: () => import('@/views/cert/CertPublicView.vue'), meta: { title: '证书信息', public: true } },
  { path: '/dl/scan/:barcode', name: 'DlScanPage', component: () => import('@/views/dl/DlScanPage.vue'), meta: { title: '食品数字标签', public: true } },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/Forbidden.vue'), meta: { title: '无权限', public: true } },
  { path: '/', name: 'Home', component: () => import('@/views/login/LoginView.vue'), meta: { title: '溯源系统', public: true },
    beforeEnter: (to) => {
      const a = to.query.a as string
      if (a && /^[a-zA-Z0-9_-]{1,64}$/.test(a)) {
        const query: Record<string, string> = {}
        if (to.query.direct) query.direct = '1'
        return { path: '/trace/' + a, query }
      }
      return { path: '/login' }
    }
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue'), meta: { title: '404', public: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 溯源系统` : '溯源系统'

  // 公开页面直接放行
  if (to.meta.public) return next()

  const userStore = useUserStore()

  // 需要认证但未登录
  if (to.meta.requireAuth && !userStore.isLoggedIn) {
    const redirect = to.fullPath
    // 防止开放重定向：只允许站内路径
    const safeRedirect = (redirect.startsWith('/') && !redirect.startsWith('//')) ? redirect : undefined
    return next({ path: '/login', query: safeRedirect ? { redirect: safeRedirect } : {} })
  }

  // 华测管理端 - 仅管理员可访问
  if (to.meta.requireAdmin && !userStore.isAdmin) {
    return next('/403')
  }

  // 企业端 - 仅企业用户可访问
  if (to.meta.requireEnterprise && !userStore.isEnterprise) {
    return next('/403')
  }

  next()
})

export default router
