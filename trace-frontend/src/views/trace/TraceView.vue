<template>
  <div class="trace-page" :data-theme="themeKey" v-loading="loading"
    :style="pageBackgroundStyle">
    <div v-if="error" class="error-container">
      <el-result icon="error" title="查询失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="loadTrace">重新查询</el-button>
          <p style="font-size:12px;color:#999;margin-top:12px">流水号：{{ isBatchMode ? 'batch-' + batchId : serialNo }}</p>
        </template>
      </el-result>
    </div>

    <div v-else-if="traceData" class="trace-container">
      <!-- 溯源码角标 -->
      <div class="serial-badge">{{ isBatchMode ? '批次：' + (traceData.batch?.name || '') : '溯源码：' + serialNo }}</div>

      <!-- 扫码即防伪结果 -->
      <div v-if="isDirectMode && directVerifyResult" class="direct-verify-card">
        <div :class="['dv-status', directVerifyResult.verified ? 'success' : 'fail']">
          <span class="dv-icon">{{ directVerifyResult.verified ? '✅' : '❌' }}</span>
          <span class="dv-msg">{{ directVerifyResult.message }}</span>
        </div>
        <div class="dv-details">
          <p v-if="directVerifyResult.enterpriseName">企业：{{ directVerifyResult.enterpriseName }}</p>
          <p v-if="directVerifyResult.productName">产品：{{ directVerifyResult.productName }}</p>
          <p v-if="directVerifyResult.certName">认证机构：{{ directVerifyResult.certName }}</p>
          <p>查询次数：第 <strong>{{ directVerifyResult.scanCount }}</strong> 次</p>
          <p style="font-size:12px;color:#999">查询时间：{{ directVerifyResult.queryTime || '' }}</p>
        </div>
        <div v-if="qrCodeDataUrl" class="dv-qrcode">
          <img :src="qrCodeDataUrl" alt="防伪二维码" />
          <p>扫码分享验证</p>
        </div>
      </div>

      <!-- 动态渲染溯源信息模块 -->
      <Transition :name="pageTransition || 'none'" mode="out-in">
      <div :key="currentPageIdx" class="trace-body" :class="'layout-' + currentLayout" :style="hasPageElements ? { display: 'flex', flexWrap: 'wrap', gap: '4px' } : {}">
        <!-- 新版：页面元素模式 -->
        <template v-if="hasPageElements">
          <div v-for="el in currentPageElements" :key="el.id" class="page-element" :class="'el-type-' + el.type" :style="el.type === 'button' ? { flex: '0 0 ' + (el.style?.width || '100%'), boxSizing: 'border-box' } : { flex: '0 0 100%' }">
            <!-- 文本 -->
            <div v-if="el.type === 'text'" class="el-text" :style="el.style || {}">{{ el.content }}</div>
            <!-- 富文本 -->
            <div v-else-if="el.type === 'rich-text'" class="el-rich-text" :style="el.style || {}" v-html="DOMPurify.sanitize(el.content || '')"></div>
            <!-- 图片 -->
            <el-image v-else-if="el.type === 'image' && el.src" :src="el.src" fit="cover" :style="{ width: el.style?.width || '100%', height: el.style?.height || 'auto', borderRadius: (el.style?.borderRadius || 0) + 'px', margin: el.style?.margin || '' }" :preview-src-list="[el.src]" />
            <!-- 视频 -->
            <video v-else-if="el.type === 'video' && el.src" :src="el.src" :poster="el.poster" controls style="width:100%;border-radius:8px" />
            <!-- 分割线 -->
            <el-divider v-else-if="el.type === 'divider'" :style="el.style || {}" />
            <!-- 按钮 -->
            <div v-else-if="el.type === 'button'" class="el-button-wrap" :style="{ width: '100%' }">
              <div v-if="el.bgImage && el.label" class="btn-label-above" :style="{ fontSize: (el.style?.fontSize || 14) + 'px', color: el.style?.color || '#fff' }">{{ el.label }}</div>
              <div v-if="el.bgImage" :class="['custom-btn', 'bg-image-btn', el.animation ? 'btn-anim-' + el.animation : '']" :style="btnBgImageStyle(el)" @click="handlePageButton(el)"></div>
              <el-button v-else round :class="el.animation ? 'btn-anim-' + el.animation : ''" :style="{ ...(el.style || {}), width: '100%' }" @click="handlePageButton(el)">
                <el-icon v-if="el.icon" :size="16" style="margin-right:4px"><component :is="el.icon" /></el-icon>
                {{ el.label || '按钮' }}
              </el-button>
            </div>
            <!-- 实时视频监控 -->
            <LiveVideoElement v-else-if="el.type === 'live-video'" :label="el.label" :sources="videoSourcesData" :gridColumns="el.gridColumns || 2" />
            <!-- IoT传感器读数 -->
            <IotSensorElement v-else-if="el.type === 'iot-sensor'" :label="el.label" :data="iotLatestData" :updatedAt="iotUpdatedAt" />
            <!-- 温湿度曲线 -->
            <IotChartElement v-else-if="el.type === 'iot-chart'" :label="el.label" :series="temperatureSeries" />
            <!-- 运输车辆轨迹 -->
            <VehicleTrackElement v-else-if="el.type === 'vehicle-track'" :label="el.label" :points="gpsTrackPoints" :trackInfo="gpsTrackInfo" />
            <!-- VR全景导览 -->
            <VrPanoramaElement v-else-if="el.type === 'vr-panorama'" :config="el" :trace-data="traceData" />
            <!-- 信息模块 -->
            <div v-else-if="isInfoSection(el.type)" :class="['trace-section', 'modern-section', el.style?.cardStyle ? 'card-' + el.style.cardStyle : '']" :style="{ backgroundColor: el.style?.backgroundColor || '', borderRadius: (el.style?.borderRadius || 0) + 'px', padding: el.style?.padding || '', boxShadow: el.style?.boxShadow || '' }">
              <div class="section-header">
                <div class="section-icon"><el-icon :size="20"><component :is="SECTION_ICONS[el.type] || 'Reading'" /></el-icon></div>
                <div class="section-title-text">{{ el.label }}</div>
              </div>
              <div class="section-content field-grid">
                <template v-for="field in getElVisibleFields(el)" :key="field.field">
                  <div v-if="field.type === 'image'" class="field-item field-item-block field-media">
                    <div class="field-label">{{ field.label }}</div>
                    <template v-if="getImageList(field.field).length > 1">
                      <el-carousel :interval="4000" indicator-position="outside" height="200px" arrow="always">
                        <el-carousel-item v-for="(img, idx) in getImageList(field.field)" :key="idx">
                          <el-image :src="img" fit="cover" style="width:100%;height:100%" :preview-src-list="getImageList(field.field)" :initial-index="idx" />
                        </el-carousel-item>
                      </el-carousel>
                    </template>
                    <el-image v-else-if="getImageList(field.field).length === 1" :src="getImageList(field.field)[0]" fit="cover" :preview-src-list="getImageList(field.field)" />
                  </div>
                  <div v-else-if="field.type === 'video'" class="field-item field-item-block field-media">
                    <div class="field-label">{{ field.label }}</div>
                    <video v-if="getFieldValue(field.field)" :src="getFieldValue(field.field)" controls />
                  </div>
                  <div v-else-if="field.type === 'file'" class="field-item">
                    <span class="field-label">{{ field.label }}</span>
                    <el-link v-if="getFieldValue(field.field)" :href="getFieldValue(field.field)" target="_blank" type="primary">查看文件</el-link>
                  </div>
                  <div v-else-if="field.type === 'badge'" class="field-item">
                    <span class="field-label">{{ field.label }}</span>
                    <el-tag :type="getFieldValue(field.field) === '合格' ? 'success' : 'danger'" size="large" effect="dark" round>{{ getFieldValue(field.field) }}</el-tag>
                  </div>
                  <div v-else-if="field.type === 'text'" class="field-item field-item-block">
                    <div class="field-label">{{ field.label }}</div>
                    <div class="field-value field-text">{{ getFieldValue(field.field) }}</div>
                  </div>
                  <div v-else class="field-item">
                    <span class="field-label">{{ field.label }}</span>
                    <span class="field-value">{{ getFieldValue(field.field) }}</span>
                  </div>
                </template>
              </div>
            </div>
            <!-- 自定义字段 -->
            <div v-else-if="el.type === 'custom-field' && getCustomFieldValue(el.fieldKey)" class="field-item" style="background:var(--trace-section-bg);border-radius:8px;padding:12px;margin-bottom:8px">
              <span class="field-label">{{ el.label }}：</span>
              <template v-if="el.fieldType === 'image'">
                <el-image :src="getCustomFieldValue(el.fieldKey)" fit="cover" style="width:100%;max-width:300px;height:180px;border-radius:8px" />
              </template>
              <template v-else>
                <span class="field-value">{{ getCustomFieldValue(el.fieldKey) }}</span>
              </template>
            </div>
            <!-- 防伪验证 -->
            <div v-else-if="el.type === 'anti-counterfeit'" class="anti-fake-module">
              <div class="af-header">
                <el-icon :size="18"><Lock /></el-icon>
                <span>防伪验证</span>
              </div>
              <div class="af-input-bar">
                <input v-model="antiFakeCodeInput" class="af-code-input" placeholder="请输入完整防伪码或后4-6位" />
                <button class="af-verify-btn" :disabled="verifying" @click="handleVerifyAntiFake">{{ verifying ? '验证中...' : '验证' }}</button>
              </div>
              <div v-if="verifyResult" :class="['af-result', verifyResult.verified ? 'success' : 'fail']">
                <div class="af-result-icon">{{ verifyResult.verified ? '✅' : '❌' }}</div>
                <div class="af-result-info">
                  <p class="af-result-msg">{{ verifyResult.message }}</p>
                  <p v-if="verifyResult.enterpriseName" class="af-result-detail">生产企业：{{ verifyResult.enterpriseName }}</p>
                  <p v-if="verifyResult.productName" class="af-result-detail">产品名称：{{ verifyResult.productName }}</p>
                  <p v-if="verifyResult.certName" class="af-result-detail">认证机构：{{ verifyResult.certName }}</p>
                  <p class="af-result-detail">扫码次数：第 <strong>{{ verifyResult.scanCount || 1 }}</strong> 次</p>
                  <p class="af-result-detail af-time">查询时间：{{ verifyResult.queryTime || '' }}</p>
                </div>
              </div>
            </div>
            <!-- 地图 -->
            <div v-else-if="el.type === 'map'" class="trace-map-wrap" :style="{ width: el.style?.width || '100%', height: el.style?.height || '200px', borderRadius: (el.style?.borderRadius || 0) + 'px' }">
              <div :ref="(dom: any) => setMapRef(el.id, dom)" class="trace-map-container" :id="'map-' + el.id"></div>
            </div>
          </div>
          <!-- 子页面返回按钮 -->
          <div v-if="currentPageIdx > 0" class="back-to-home">
            <el-button round @click="currentPageIdx = 0">
              <el-icon><Back /></el-icon> 返回首页
            </el-button>
          </div>
          <!-- 页面导航按钮 -->
          <div v-if="templateConfig?.pages?.length > 1" class="page-nav">
            <el-button v-for="(p, i) in templateConfig.pages" :key="p.id" :type="currentPageIdx === i ? 'primary' : 'default'" size="small" round @click="currentPageIdx = i">{{ p.name }}</el-button>
          </div>
          <!-- 页面分页指示器 -->
          <div v-if="templateConfig?.pages?.length > 1" class="page-dots">
            <span v-for="(p, i) in templateConfig.pages" :key="p.id" :class="['dot', { active: currentPageIdx === i }]" @click="currentPageIdx = i" />
          </div>
        </template>

        <!-- 旧版：sections模式（兼容） -->
        <template v-else>
        <div v-for="section in filteredSections" :key="section.key" class="trace-section">
          <div class="section-title">
            <el-icon><component :is="section.icon" /></el-icon>
            <span>{{ section.title }}</span>
          </div>
          <div class="section-content">
            <div v-for="field in section.visibleFields" :key="field.field" class="field-item">
              <span class="field-label">{{ field.label }}：</span>
              <template v-if="field.type === 'image'">
                <el-image :src="getFieldValue(field.field)" fit="cover" style="width:100%;max-width:300px;height:180px;border-radius:8px" :preview-src-list="[getFieldValue(field.field)]" />
              </template>
              <template v-else-if="field.type === 'file'">
                <el-link v-if="getFieldValue(field.field)" :href="getFieldValue(field.field)" target="_blank" type="primary">查看文件</el-link>
              </template>
              <template v-else-if="field.type === 'badge'">
                <el-tag :type="getFieldValue(field.field) === '合格' ? 'success' : 'danger'">{{ getFieldValue(field.field) }}</el-tag>
              </template>
              <template v-else-if="field.type === 'video'">
                <video v-if="getFieldValue(field.field)" :src="getFieldValue(field.field)"
                  controls style="width:100%;max-width:400px;border-radius:8px" />
              </template>
              <template v-else>
                <span class="field-value">{{ getFieldValue(field.field) }}</span>
              </template>
            </div>
          </div>
        </div>

        <!-- 检测报告 -->
        <div v-if="testReports.length > 0" class="trace-section test-report-section">
          <div class="section-title">
            <span class="test-report-icon">🧪</span>
            <span>检测报告</span>
          </div>
          <div class="test-report-summary">
            检测概况：本产品共检测 <strong>{{ testReports.length }}</strong> 次
          </div>
          <!-- 按月份分组 -->
          <div v-for="group in groupedReports" :key="group.month" class="month-group">
            <div class="month-label">{{ group.month }}</div>
            <div v-for="report in group.reports" :key="report.id" class="report-card">
              <div class="report-card-header">
                <span class="report-name">{{ report.reportName }}</span>
                <el-tag v-if="report.testResult" :type="report.testResult === '合格' ? 'success' : 'danger'" size="small">{{ report.testResult }}</el-tag>
              </div>
              <div class="report-card-body">
                <div v-if="report.testTime" class="report-field"><span class="label">检测时间：</span>{{ report.testTime }}</div>
                <div v-if="report.testMethod" class="report-field"><span class="label">检测方式：</span>{{ report.testMethod }}</div>
                <div v-if="report.testOrg" class="report-field"><span class="label">检测机构：</span>{{ report.testOrg }}</div>
              </div>
              <div class="report-card-footer">
                <a class="view-detail-link" @click="openReportDetail(report)">查看详情</a>
              </div>
            </div>
          </div>
        </div>

        <!-- 防伪验证（仅流水号模式） -->
        <div v-if="hasAntiCounterfeit && !isBatchMode" class="anti-fake-section">
          <div class="section-title"><el-icon><Lock /></el-icon><span>防伪验证</span></div>
          <div class="af-input-bar" style="margin-bottom:12px">
            <input v-model="antiFakeCodeInput" class="af-code-input" placeholder="请输入防伪码" />
            <button class="af-verify-btn" :disabled="verifying" @click="handleVerifyAntiFake">{{ verifying ? '验证中...' : '验证' }}</button>
          </div>
          <div v-if="verifyResult" :class="['af-result', verifyResult.verified ? 'success' : 'fail']">
            <div class="af-result-icon">{{ verifyResult.verified ? '✅' : '❌' }}</div>
            <div class="af-result-info">
              <p class="af-result-msg">{{ verifyResult.message }}</p>
              <p v-if="verifyResult.enterpriseName" class="af-result-detail">生产企业：{{ verifyResult.enterpriseName }}</p>
              <p v-if="verifyResult.productName" class="af-result-detail">产品名称：{{ verifyResult.productName }}</p>
              <p v-if="verifyResult.certName" class="af-result-detail">认证机构：{{ verifyResult.certName }}</p>
              <p class="af-result-detail">扫码次数：第 <strong>{{ verifyResult.scanCount || 1 }}</strong> 次</p>
              <p class="af-result-detail af-time">查询时间：{{ verifyResult.queryTime || '' }}</p>
            </div>
          </div>
          <div v-else class="section-content" style="text-align:center;color:#999;font-size:13px;padding:8px 0">
            请输入防伪码进行验证
          </div>
        </div>

        <!-- 企业自定义字段 -->
        <div v-if="customFields.length > 0" class="trace-section">
          <div class="section-title"><el-icon><List /></el-icon><span>补充信息</span></div>
          <div class="section-content">
            <div v-for="cf in customFields" :key="cf.fieldKey" class="field-item">
              <span class="field-label">{{ cf.fieldLabel || cf.fieldKey }}：</span>
              <template v-if="cf.fieldType === 'image'">
                <el-image v-if="cf.fieldValue" :src="cf.fieldValue" fit="cover" style="width:100%;max-width:300px;height:180px;border-radius:8px" :preview-src-list="[cf.fieldValue]" />
              </template>
              <template v-else-if="cf.fieldType === 'file'">
                <el-link v-if="cf.fieldValue" :href="cf.fieldValue" target="_blank" type="primary">查看文件</el-link>
              </template>
              <template v-else>
                <span class="field-value">{{ cf.fieldValue }}</span>
              </template>
            </div>
          </div>
        </div>

        <!-- 按钮组件 -->
        <div v-if="configButtons.length > 0" class="trace-buttons-section">
          <div v-for="btn in configButtons" :key="btn.id" class="trace-btn-item" @click="openButtonLink(btn)">
            <template v-if="btn.type === 'icon'">
              <el-button :style="btn.style || {}" round>
                <el-icon><component :is="btn.icon || 'Link'" /></el-icon>
                {{ btn.label }}
              </el-button>
            </template>
            <template v-else-if="btn.type === 'image'">
              <div class="image-btn" :style="btn.style || {}">
                <img v-if="btn.image" :src="btn.image" :alt="btn.label" />
                <span>{{ btn.label }}</span>
              </div>
            </template>
            <template v-else>
              <el-link :type="btn.style?.color ? '' : 'primary'" :style="btn.style || {}">{{ btn.label }}</el-link>
            </template>
          </div>
        </div>
        </template>
      </div>
      </Transition>

      <!-- 底部信息 -->
      <div class="trace-footer">
        <p>本信息由产品溯源系统提供 | 查询时间：{{ queryTime }}</p>
      </div>
    </div>
  </div>

  <!-- 检测报告详情弹窗 -->
  <el-dialog v-model="reportDetailVisible" :title="currentReport?.reportName || '检测详情'" width="92%" top="3vh" :close-on-click-modal="true" class="report-detail-dialog" destroy-on-close>
    <div v-if="reportImagesLoading" style="text-align:center;padding:40px 0">
      <el-icon class="is-loading" :size="32" color="#1890ff"><Loading /></el-icon>
      <p style="color:#999;margin-top:12px">正在加载报告图片...</p>
    </div>
    <div v-else-if="currentReportImages.length > 0" class="image-gallery-wrap">
      <div v-for="(img, idx) in currentReportImages" :key="idx" class="gallery-item">
        <el-image :src="img" fit="contain" style="width:100%;border-radius:8px;" :preview-src-list="currentReportImages" :initial-index="idx" />
      </div>
    </div>
    <div v-else-if="currentReport?.reportPdf" class="pdf-viewer-wrap">
      <iframe :src="currentReport.reportPdf" style="width:100%;height:75vh;border:none;border-radius:8px;" />
    </div>
    <div v-else class="no-file">暂无报告文件</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTraceInfo, getBatchTraceInfo, getTraceTemplate, verifyAntiFake, directVerify, getTraceVideos, getTraceIotLatest, getTraceTemperature, getTraceGpsTrack, getBatchVideos, getBatchIotLatest, convertPdfToImages } from '@/api/common'
import LiveVideoElement from '@/components/trace/LiveVideoElement.vue'
import IotSensorElement from '@/components/trace/IotSensorElement.vue'
import IotChartElement from '@/components/trace/IotChartElement.vue'
import VehicleTrackElement from '@/components/trace/VehicleTrackElement.vue'
import VrPanoramaElement from '@/components/trace/VrPanoramaElement.vue'
import QRCode from 'qrcode'
import DOMPurify from 'dompurify'
import { SECTION_FIELDS, SECTION_TYPE_MAP, isInfoSection, getSectionAllFields, getSectionDataPrefix } from '@/constants/section-fields'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const traceData = ref<any>(null)
const templateConfig = ref<any>(null)
const queryTime = ref('')

// 防伪验证
const antiFakeCodeInput = ref('')
const verifyResult = ref<any>(null)
const verifying = ref(false)

// 扫码即防伪
const isDirectMode = computed(() => route.query.direct === '1')
const directVerifyResult = ref<any>(null)
const qrCodeDataUrl = ref('')

const isBatchMode = computed(() => route.name === 'BatchTrace')
const serialNo = computed(() => route.params.serialNo as string)
const batchId = computed(() => Number(route.params.batchId))

const SECTION_ICONS: Record<string, string> = {
  'enterprise-info': 'OfficeBuilding',
  'product-info': 'Goods',
  'cert-info': 'Medal',
  'test-info': 'FirstAidKit',
  'base-info': 'House',
  'breed-archive': 'Notebook',
  'farm-info': 'Van',
  'transport-info': 'MapLocation',
  'slaughter-info': 'KnifeFork',
  'cutting-record': 'Crop',
  'video-monitor': 'VideoCamera',
  'iot-environment': 'Monitor',
}

// 视频/IoT 扩展数据
const videoSourcesData = ref<any[]>([])
const iotLatestData = ref<Record<string, number | null>>({})
const iotUpdatedAt = ref('')
const temperatureSeries = ref<any[]>([])
const gpsTrackPoints = ref<any[]>([])
const gpsTrackInfo = ref<any>({})

async function loadExtendedData() {
  const id = isBatchMode.value ? batchId.value : serialNo.value
  if (!id) return
  try {
    const [vRes, iRes, tRes, gRes] = await Promise.allSettled([
      isBatchMode.value ? getBatchVideos(batchId.value) : getTraceVideos(serialNo.value),
      isBatchMode.value ? getBatchIotLatest(batchId.value) : getTraceIotLatest(serialNo.value),
      getTraceTemperature(serialNo.value),
      getTraceGpsTrack(serialNo.value),
    ])
    if (vRes.status === 'fulfilled') videoSourcesData.value = vRes.value?.data || []
    if (iRes.status === 'fulfilled') {
      const d = iRes.value?.data
      if (d) {
        iotLatestData.value = d.metrics || d
        iotUpdatedAt.value = d.updatedAt ? new Date(d.updatedAt).toLocaleString('zh-CN') : ''
      }
    }
    if (tRes.status === 'fulfilled') {
      const d = tRes.value?.data
      if (Array.isArray(d) && d.length) {
        temperatureSeries.value = [{ name: '温度', data: d.map((p: any) => ({ time: p.time || p.timestamp, value: p.value || p.temperature })) }]
      }
    }
    if (gRes.status === 'fulfilled') {
      const d = gRes.value?.data
      if (Array.isArray(d)) {
        gpsTrackPoints.value = d.map((p: any) => ({ lng: p.lng || p.longitude || p.coordinates?.[0], lat: p.lat || p.latitude || p.coordinates?.[1], time: p.time || p.timestamp, speed: p.speed, temperature: p.temperature }))
      }
      gpsTrackInfo.value = { status: '运输中', ...(traceData.value?._vehiclePlate ? { vehiclePlate: traceData.value._vehiclePlate } : {}) }
    }
  } catch (e) {
    console.warn('扩展数据加载失败', e)
  }
}

const defaultSections = [
  { key: 'enterprise', title: '企业信息', icon: 'OfficeBuilding', fields: [
    { field: 'enterprise.name', label: '企业名称' },
    { field: 'enterprise.introduction', label: '企业简介', type: 'text' },
    { field: 'enterprise.addressFull', label: '企业地址' },
  ]},
  { key: 'product', title: '产品信息', icon: 'Goods', fields: [
    { field: 'goods.name', label: '商品名称' },
    { field: 'goods.introduction', label: '产品介绍', type: 'text' },
    { field: 'goods.storageMethod', label: '储存方式' },
  ]},
  { key: 'batch', title: '批次信息', icon: 'Tickets', fields: [
    { field: 'batch.name', label: '批次名称' },
    { field: 'batch.testOrg', label: '检测机构' },
    { field: 'batch.testResult', label: '检测结果', type: 'badge' },
    { field: 'batch.testTime', label: '检测时间' },
    { field: 'batch.testMethod', label: '检测方法' },
    { field: 'batch.testBasis', label: '检测依据' },
  ]},
  { key: 'base', title: '基地信息', icon: 'Location', fields: [
    { field: 'base.name', label: '基地名称' },
    { field: 'base.areaDisplay', label: '基地面积' },
    { field: 'base.manager', label: '负责人' },
    { field: 'base.certification', label: '基地认证' },
  ]},
]

const themeKey = computed(() => templateConfig.value?.theme?.key || 'standard-green')
const currentLayout = computed(() => templateConfig.value?.layout || 'free')
const pageTransition = computed(() => templateConfig.value?.pageTransition || '')

const backgroundImage = computed(() => templateConfig.value?.backgroundImage || '')
const pageBackgroundColor = computed(() => templateConfig.value?.pageBackgroundColor || '')

const pageBackgroundStyle = computed(() => {
  const style: any = {}
  if (backgroundImage.value) {
    // 背景图叠加渐变遮罩，保证文字可读性
    style.backgroundImage = `linear-gradient(to bottom, rgba(255,255,255,0.78), rgba(255,255,255,0.92)), url(${backgroundImage.value})`
    style.backgroundSize = 'cover'
    style.backgroundAttachment = 'fixed'
    style.backgroundPosition = 'center'
  }
  if (pageBackgroundColor.value) {
    style.backgroundColor = pageBackgroundColor.value
  }
  return style
})

const configButtons = computed(() => templateConfig.value?.buttons || [])

// 检测报告（兼容新旧数据结构）
const testReports = computed(() => {
  if (traceData.value?.testReports) return traceData.value.testReports
  if (traceData.value?.testReportInfo) return [traceData.value.testReportInfo]
  if (traceData.value?.testReport && typeof traceData.value.testReport === 'object') return [traceData.value.testReport]
  return []
})

// 按月份分组
const groupedReports = computed(() => {
  const groups: Record<string, any[]> = {}
  for (const report of testReports.value) {
    const month = report.testTime ? report.testTime.substring(0, 7) : '未知月份'
    if (!groups[month]) groups[month] = []
    groups[month].push(report)
  }
  return Object.entries(groups)
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([month, reports]) => ({ month, reports }))
})

// 报告详情弹窗
const reportDetailVisible = ref(false)
const currentReport = ref<any>(null)
const currentReportImages = ref<string[]>([])
const reportImagesLoading = ref(false)

const existingReportImages = computed(() => {
  if (!currentReport.value?.reportImages) return []
  if (Array.isArray(currentReport.value.reportImages)) return currentReport.value.reportImages
  return String(currentReport.value.reportImages).split(',').filter(Boolean)
})

async function openReportDetail(report: any) {
  currentReport.value = report
  reportDetailVisible.value = true
  // 如果已有图片直接使用
  if (existingReportImages.value.length > 0) {
    currentReportImages.value = existingReportImages.value
    return
  }
  // 如果有PDF，转换为图片
  if (report.reportPdf) {
    reportImagesLoading.value = true
    currentReportImages.value = []
    try {
      const res = await convertPdfToImages(report.reportPdf)
      currentReportImages.value = res.data || []
    } catch (e) {
      console.warn('PDF转图片失败，回退iframe模式')
      currentReportImages.value = []
    } finally {
      reportImagesLoading.value = false
    }
  }
}

const customFields = computed(() => {
  // 新版格式：customFields 是 object { key: { label, value, type } }
  // 旧版格式：customFields 是 array
  const cf = traceData.value?.customFields
  if (!cf) return []
  if (Array.isArray(cf)) return cf
  return Object.entries(cf).map(([key, val]: [string, any]) => ({
    fieldKey: key,
    fieldLabel: val.label || key,
    fieldValue: val.value || '',
    fieldType: val.type || 'text',
  }))
})

// 新版页面元素支持
const currentPageIdx = ref(0)

const hasPageElements = computed(() => {
  const pages = templateConfig.value?.pages
  return pages && pages.length > 0 && pages.some((p: any) => p.elements?.length > 0)
})

const currentPageElements = computed(() => {
  const pages = templateConfig.value?.pages
  if (!pages || !pages[currentPageIdx.value]) return []
  return pages[currentPageIdx.value].elements || []
})

function getElVisibleFields(el: any) {
  const all = getSectionAllFields(el.type)
  const selected = el.selectedFields || []
  return all
    .filter((f: any) => selected.includes(f.field))
    .filter((f: any) => {
      const val = getFieldValue(f.field)
      return val !== '' && val !== null && val !== undefined
    })
}

function getCustomFieldValue(key: string): string {
  const cf = traceData.value?.customFields
  if (!cf) return ''
  if (Array.isArray(cf)) {
    const found = cf.find((f: any) => f.fieldKey === key)
    return found?.fieldValue || ''
  }
  return cf[key]?.value || ''
}

function btnBgImageStyle(el: any) {
  const s: any = {
    backgroundImage: `url(${el.bgImage})`,
    height: el.style?.height || '60px',
    borderRadius: (el.style?.borderRadius || 0) + 'px',
    margin: el.style?.margin || '',
    fontSize: (el.style?.fontSize || 14) + 'px',
    color: el.style?.color || '#fff',
    backgroundColor: el.style?.backgroundColor || '',
  }
  if (el.imageFit === 'contain') {
    s.backgroundSize = 'contain'
    s.backgroundPosition = 'center'
  } else if (el.imageFit === 'custom') {
    s.backgroundSize = `${el.imageWidth || '100%'} ${el.imageHeight || '100%'}`
    s.backgroundPosition = 'center'
  } else {
    s.backgroundSize = 'cover'
    s.backgroundPosition = 'center'
  }
  return s
}

function handlePageButton(btn: any) {
  if (btn.buttonType === 'link' && btn.link) {
    window.open(btn.link, '_blank')
  } else if (btn.buttonType === 'page' && btn.targetPageId) {
    const pages = templateConfig.value?.pages || []
    const idx = pages.findIndex((p: any) => p.id === btn.targetPageId)
    if (idx >= 0) currentPageIdx.value = idx
  } else if (btn.buttonType === 'phone' && btn.phone) {
    window.location.href = 'tel:' + btn.phone
  }
}

// ==================== 腾讯地图集成 ====================
const mapDomRefs: Record<string, HTMLElement | null> = {}
const mapInstances: Record<string, any> = {}
let tencentMapLoaded = false
let tencentMapLoading: Promise<void> | null = null

// 组件卸载时清理地图实例，防止内存泄漏
onUnmounted(() => {
  for (const id of Object.keys(mapInstances)) {
    try { mapInstances[id]?.destroy?.() } catch {}
    delete mapInstances[id]
  }
  for (const id of Object.keys(mapDomRefs)) {
    mapDomRefs[id] = null
  }
})

function loadTencentMap(key: string): Promise<void> {
  if (tencentMapLoaded) return Promise.resolve()
  if (tencentMapLoading) return tencentMapLoading
  tencentMapLoading = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://map.qq.com/api/js?v=2.exp&key=${key}`
    script.onload = () => { tencentMapLoaded = true; resolve() }
    script.onerror = () => reject(new Error('腾讯地图加载失败'))
    document.head.appendChild(script)
  })
  return tencentMapLoading
}

function setMapRef(elId: string, dom: HTMLElement | null) {
  mapDomRefs[elId] = dom
  if (dom) {
    nextTick(() => initMapForElement(elId))
  }
}

function initMapForElement(elId: string) {
  const dom = mapDomRefs[elId]
  if (!dom || mapInstances[elId]) return
  // 查找当前页面中的 map 元素配置
  const pages = templateConfig.value?.pages || []
  let mapEl: any = null
  for (const p of pages) {
    mapEl = (p.elements || []).find((e: any) => e.id === elId && e.type === 'map')
    if (mapEl) break
  }
  if (!mapEl) return
  const apiKey = mapEl.mapKey
  if (!apiKey) {
    dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#999;font-size:13px;background:#f5f5f5;border-radius:8px">地图未配置API Key</div>'
    return
  }
  loadTencentMap(apiKey).then(() => {
    const T = (window as any).T
    if (!T || !T.Map) return
    const centerParts = (mapEl.center || '104.0657,30.6595').split(',')
    const center = new T.LngLat(parseFloat(centerParts[1]) || 30.6595, parseFloat(centerParts[0]) || 104.0657)
    const map = new T.Map(dom)
    map.centerAndZoom(center, mapEl.zoom || 10)
    // 添加标记点
    const markers = mapEl.markers || []
    for (const m of markers) {
      if (m.lat && m.lng) {
        const marker = new T.Marker(new T.LngLat(m.lng, m.lat))
        map.addOverLay(marker)
        if (m.label) {
          const label = new T.Label({ text: m.label, offset: new T.Point(10, -20) })
          marker.bindLabel(label)
        }
      }
    }
    mapInstances[elId] = map
  }).catch(() => {
    dom.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#f56c6c;font-size:13px">地图加载失败</div>'
  })
}

// 切换页面时重新初始化地图
watch(currentPageIdx, () => {
  nextTick(() => {
    const pages = templateConfig.value?.pages || []
    const page = pages[currentPageIdx.value]
    if (!page) return
    for (const el of (page.elements || [])) {
      if (el.type === 'map' && mapDomRefs[el.id]) {
        delete mapInstances[el.id]
        initMapForElement(el.id)
      }
    }
  })
})

// 防伪模块是否展示
const hasAntiCounterfeit = computed(() => {
  const pages = templateConfig.value?.pages
  if (pages) {
    return pages.some((p: any) => p.elements?.some((e: any) => e.type === 'anti-counterfeit'))
  }
  return !isBatchMode.value // 旧版模式默认展示
})

async function handleVerifyAntiFake() {
  if (!antiFakeCodeInput.value.trim()) return
  verifying.value = true
  try {
    const res = await verifyAntiFake({ serialNo: serialNo.value, antiFakeCode: antiFakeCodeInput.value.trim() })
    verifyResult.value = res.data
  } catch (e: any) {
    verifyResult.value = { verified: false, message: '验证请求失败' }
  } finally {
    verifying.value = false
  }
}

async function handleDirectVerify() {
  if (!serialNo.value) return
  try {
    const res = await directVerify(serialNo.value)
    directVerifyResult.value = res.data
  } catch (e: any) {
    directVerifyResult.value = { verified: false, message: '验证请求失败' }
  }
  // 生成二维码
  try {
    const url = window.location.href.replace(/[?&]direct=1/, '')
    qrCodeDataUrl.value = await QRCode.toDataURL(url, { width: 200, margin: 2 })
  } catch (e) { console.warn('QR生成失败') }
}

const filteredSections = computed(() => {
  const sections = templateConfig.value?.sections || defaultSections
  return sections
    .map((section: any) => ({
      ...section,
      visibleFields: section.fields.filter((f: any) => {
        const val = getFieldValue(f.field)
        return val !== '' && val !== null && val !== undefined
      })
    }))
    .filter((section: any) => section.visibleFields.length > 0)
})

function getFieldValue(path: string): any {
  if (!traceData.value) return ''
  const parts = path.split('.')
  let val = traceData.value
  for (const part of parts) {
    val = val?.[part]
    if (val === undefined || val === null) return ''
  }
  return val
}

function getImageList(path: string): string[] {
  const val = getFieldValue(path)
  if (!val) return []
  if (typeof val === 'string') {
    return val.split(',').map(s => s.trim()).filter(Boolean)
  }
  if (Array.isArray(val)) return val
  return []
}

function openButtonLink(btn: any) {
  if (btn.link) window.open(btn.link, '_blank')
}

async function loadTrace() {
  const id = isBatchMode.value ? batchId.value : serialNo.value
  if (!id) return
  loading.value = true
  error.value = ''
  try {
    const res = isBatchMode.value
      ? await getBatchTraceInfo(batchId.value)
      : await getTraceInfo(serialNo.value)
    traceData.value = res.data
    queryTime.value = new Date().toLocaleString('zh-CN')

    // 优先使用响应中已解析的 templateConfig
    if (res.data?.templateConfig) {
      templateConfig.value = res.data.templateConfig
    } else if (res.data?.traceTemplate) {
      try {
        const tplRes = await getTraceTemplate(res.data.traceTemplate)
        templateConfig.value = tplRes.data
          } catch (e) { templateConfig.value = null; console.warn('模板加载失败') }
    }
    // 加载视频/IoT扩展数据（并行，不阻塞主查询）
    loadExtendedData()
  } catch (e: any) {
    error.value = e.message || '未找到溯源信息，请确认流水号是否正确'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadTrace()
  if (isDirectMode.value && !isBatchMode.value) {
    await handleDirectVerify()
  }
})
</script>

<style scoped lang="scss">
.trace-page {
  min-height: 100vh;
  background: var(--trace-bg);
  background-repeat: no-repeat;
  background-size: cover;
  background-attachment: fixed;
  background-position: top center;
  position: relative;
}

.error-container {
  padding-top: 80px;
}

.trace-container {
  position: relative;
  min-height: 100vh;
}

.serial-badge {
  position: fixed; top: 12px; right: 12px; z-index: 100;
  background: rgba(0,0,0,0.5); color: #fff; font-size: 12px;
  padding: 4px 12px; border-radius: 14px; white-space: nowrap;
  backdrop-filter: blur(4px);
}

// 扫码即防伪卡片
.direct-verify-card {
  max-width: 680px;
  margin: 56px auto 0;
  background: var(--trace-section-bg);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--trace-section-shadow);
  text-align: center;

  .dv-status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 16px;
    border-radius: 12px;
    margin-bottom: 16px;
    font-size: 18px;
    font-weight: 600;

    &.success { background: var(--trace-success-bg); color: var(--trace-success-text); }
    &.fail { background: var(--trace-error-bg); color: var(--trace-error-text); }
    .dv-icon { font-size: 28px; }
  }

  .dv-details {
    text-align: left;
    padding: 0 8px;
    p { margin: 6px 0; font-size: 14px; color: var(--trace-text-secondary); }
    strong { color: var(--trace-accent); font-size: 16px; }
  }

  .dv-qrcode {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid var(--trace-border-color);
    img { width: 160px; height: 160px; border-radius: 8px; }
    p { font-size: 12px; color: var(--trace-text-muted); margin-top: 8px; }
  }
}

.trace-body {
  padding: 12px 12px 20px;
  position: relative;
  z-index: 1;
}

.trace-section {
  background: var(--trace-section-bg);
  border: var(--trace-section-border);
  border-left: var(--trace-section-border-left);
  border-radius: var(--trace-section-radius);
  padding: 20px;
  margin-bottom: 12px;
  box-shadow: var(--trace-section-shadow);
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &.card-elevated {
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.1);
    &:hover { transform: translateY(-3px); box-shadow: 0 14px 36px rgba(0, 0, 0, 0.13); }
  }
  &.card-glass {
    background: rgba(255, 255, 255, 0.72);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.35);
  }
  &.card-bordered {
    border: 1px solid var(--trace-border-color);
    box-shadow: none;
  }
  &.card-flat {
    box-shadow: none;
    border: none;
    background: var(--trace-section-bg);
  }
}

.anti-fake-section {
  background: var(--trace-section-bg);
  border: var(--trace-section-border);
  border-left: var(--trace-section-border-left);
  border-radius: var(--trace-section-radius);
  padding: 20px;
  margin-bottom: 12px;
  box-shadow: var(--trace-section-shadow);
}

.modern-section {
  overflow: hidden;

  .section-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--trace-border-color);
  }
  .section-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--trace-accent), var(--trace-accent-light, var(--trace-accent)));
    color: #fff;
    box-shadow: 0 4px 10px rgba(5, 150, 105, 0.18);
  }
  .section-title-text {
    font-size: 17px;
    font-weight: 700;
    color: var(--trace-text-primary);
    letter-spacing: 0.3px;
  }
}

.section-content {
  &.field-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 10px 16px;
  }

  .field-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 8px 10px;
    background: var(--trace-report-bg, rgba(240, 253, 244, 0.4));
    border-radius: 8px;
    line-height: 1.5;
    min-width: 0;
    overflow: hidden;

    &.field-item-block {
      grid-column: 1 / -1;
    }

    &.field-media {
      padding: 10px;
      background: transparent;
      border: 1px dashed var(--trace-border-color);

      .field-label {
        margin-bottom: 6px;
      }
      .el-image, video {
        width: 100%;
        max-width: 100%;
        border-radius: 10px;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(0,0,0,0.06);
      }
      video { background: #000; }
    }
  }

  .field-label {
    color: var(--trace-text-muted);
    font-size: 12px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.4px;
  }

  .field-value {
    color: var(--trace-text-secondary);
    font-size: 14px;
    font-weight: 500;

    &.field-text {
      line-height: 1.7;
      white-space: pre-wrap;
      word-break: break-word;
      overflow-wrap: break-word;
    }
  }

  .anti-fake-code {
    font-family: monospace;
    font-size: 16px;
    font-weight: bold;
    color: var(--trace-anti-code);
    letter-spacing: 2px;
  }
}

.trace-footer {
  text-align: center;
  padding: 20px;
  color: var(--trace-text-muted);
  font-size: 12px;
}

.trace-buttons-section {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  padding: 16px 0;

  .trace-btn-item {
    cursor: pointer;
  }

  .image-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    background: var(--trace-section-bg);
    border-radius: 12px;
    box-shadow: var(--trace-section-shadow);

    img {
      width: 36px;
      height: 36px;
      border-radius: 8px;
    }

    span {
      font-size: 12px;
      color: var(--trace-text-secondary);
    }
  }
}

// Page element styles
.page-element {
  margin-bottom: 12px;
  animation: elFadeInUp 0.45s cubic-bezier(0.25, 0.8, 0.25, 1) both;
}
.page-element:nth-child(1) { animation-delay: 0.03s; }
.page-element:nth-child(2) { animation-delay: 0.06s; }
.page-element:nth-child(3) { animation-delay: 0.09s; }
.page-element:nth-child(4) { animation-delay: 0.12s; }
.page-element:nth-child(5) { animation-delay: 0.15s; }
.page-element:nth-child(6) { animation-delay: 0.18s; }
.page-element:nth-child(7) { animation-delay: 0.21s; }
.page-element:nth-child(8) { animation-delay: 0.24s; }
.page-element:nth-child(9) { animation-delay: 0.27s; }
.page-element:nth-child(10) { animation-delay: 0.30s; }

@keyframes elFadeInUp {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

.el-text {
  line-height: 1.6;
  word-break: break-all;
}

.el-rich-text {
  line-height: 1.6;
  word-break: break-all;
  :deep(p) { margin: 4px 0; }
  :deep(img) { max-width: 100%; height: auto; border-radius: 4px; display: block; }
  :deep(a) { color: var(--trace-link-color); }
}

.el-button-wrap {
  text-align: center;
  padding: 0;
  margin: 0;
  width: 100%;
}

.btn-label-above {
  text-align: center;
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.3;
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

.btn-anim-pulse { animation: btnPulse 1.5s infinite; }
.btn-anim-shine { position: relative; overflow: hidden; }
.btn-anim-shine::after {
  content: '';
  position: absolute;
  top: 0; left: -100%;
  width: 50%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: btnShine 2s infinite;
}
.btn-anim-bounce { animation: btnBounce 1.2s infinite; }

@keyframes btnPulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(var(--trace-accent-rgb), 0.4); }
  50% { transform: scale(1.02); box-shadow: 0 0 0 8px rgba(var(--trace-accent-rgb), 0); }
}
@keyframes btnShine {
  0% { left: -100%; }
  100% { left: 200%; }
}
@keyframes btnBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

// 自定义图片按钮
.custom-btn.bg-image-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  cursor: pointer;
  overflow: hidden;
  border: none;
  transition: opacity 0.2s;
  width: 100%;
  box-sizing: border-box;
  &:hover { opacity: 0.9; }
  &:active { opacity: 0.8; }
}

// 返回按钮
.back-to-home {
  text-align: center;
  padding: 12px 0;
  width: 100%;
}

// 防伪验证模块
.anti-fake-module {
  background: var(--trace-section-bg);
  border-radius: 12px;
  padding: 16px;
  box-shadow: var(--trace-section-shadow);
}

.af-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--trace-text-primary);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--trace-border-color);
}

.af-input-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.af-code-input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  border: 1px solid var(--trace-input-border);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: var(--trace-input-bg);
  color: var(--trace-input-text);
  &:focus {
    border-color: var(--trace-input-focus-border);
    box-shadow: var(--trace-input-focus-shadow);
  }
}

.af-verify-btn {
  height: 40px;
  padding: 0 20px;
  background: var(--trace-btn-bg);
  color: var(--trace-btn-text);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  &:hover { background: var(--trace-btn-hover); }
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.af-result {
  margin-top: 12px;
  padding: 14px;
  border-radius: 10px;
  display: flex;
  gap: 12px;
  align-items: flex-start;

  &.success {
    background: var(--trace-success-bg);
    border: 1px solid var(--trace-success-border);
  }

  &.fail {
    background: var(--trace-error-bg);
    border: 1px solid var(--trace-error-border);
  }

  .af-result-icon {
    font-size: 24px;
    flex-shrink: 0;
  }

  .af-result-info {
    flex: 1;
  }

  .af-result-msg {
    font-size: 15px;
    font-weight: 600;
    margin: 0 0 6px;
    color: var(--trace-text-primary);
  }

  .af-result-detail {
    font-size: 13px;
    color: var(--trace-text-secondary);
    margin: 2px 0;
  }
}

.page-nav {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 16px 0 6px;
  flex-wrap: wrap;
}

.page-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 8px 0 16px;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--trace-text-muted);
    opacity: 0.35;
    cursor: pointer;
    transition: all 0.25s ease;

    &.active {
      width: 20px;
      border-radius: 4px;
      opacity: 1;
      background: var(--trace-accent);
    }
  }
}

// 检测报告卡片
.test-report-section {
  .test-report-icon { margin-right: 6px; color: var(--trace-accent); }
  .test-report-summary {
    font-size: 14px; color: var(--trace-text-secondary); margin-bottom: 16px;
    padding: 8px 12px; background: var(--trace-report-bg); border-radius: 8px;
    strong { color: var(--trace-accent); }
  }
  .month-label {
    font-size: 13px; color: var(--trace-text-muted); margin: 12px 0 8px; padding-left: 4px;
  }
  .report-card {
    border-left: 3px solid var(--trace-report-border); background: var(--trace-section-bg-alt);
    border-radius: 0 8px 8px 0; padding: 12px 16px; margin-bottom: 10px;
  }
  .report-card-header {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;
    .report-name { font-weight: 600; font-size: 15px; color: var(--trace-text-primary); }
  }
  .report-card-body {
    .report-field { font-size: 13px; color: var(--trace-text-secondary); line-height: 1.8; .label { color: var(--trace-text-muted); } }
  }
  .report-card-footer { margin-top: 8px; text-align: right; }
  .view-detail-link {
    color: var(--trace-link-color); font-weight: 600; cursor: pointer; text-decoration: underline;
    &:hover { color: var(--trace-link-hover); }
  }
}

// 报告详情弹窗
:deep(.report-detail-dialog) {
  .pdf-viewer-wrap { padding: 0; }
  .image-gallery-wrap {
    max-height: 80vh; overflow-y: auto;
    .gallery-item { margin-bottom: 12px; }
  }
  .no-file { text-align: center; padding: 40px; color: var(--trace-text-muted); }
}

// 地图元素
.trace-map-wrap {
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid var(--trace-divider-color);
  margin-bottom: 8px;
}

.trace-map-container {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

/* ==================== 布局预设样式 ==================== */
// 页面切换动画
.v-enter-active,
.v-leave-active { transition: opacity 0.3s ease; }
.v-enter-from,
.v-leave-to { opacity: 0; }

.fade-enter-active,
.fade-leave-active { transition: opacity 0.35s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }

.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active,
.slide-up-enter-active,
.slide-up-leave-active,
.slide-down-enter-active,
.slide-down-leave-active { transition: all 0.45s cubic-bezier(0.25, 0.8, 0.25, 1); }

.slide-left-enter-from { opacity: 0; transform: translateX(40px); }
.slide-left-leave-to { opacity: 0; transform: translateX(-40px); }
.slide-right-enter-from { opacity: 0; transform: translateX(-40px); }
.slide-right-leave-to { opacity: 0; transform: translateX(40px); }
.slide-up-enter-from { opacity: 0; transform: translateY(40px); }
.slide-up-leave-to { opacity: 0; transform: translateY(-40px); }
.slide-down-enter-from { opacity: 0; transform: translateY(-40px); }
.slide-down-leave-to { opacity: 0; transform: translateY(40px); }

.zoom-enter-active,
.zoom-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.zoom-enter-from { opacity: 0; transform: scale(0.92); }
.zoom-leave-to { opacity: 0; transform: scale(1.06); }

.flip-enter-active,
.flip-leave-active { transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1); backface-visibility: hidden; }
.flip-enter-from { opacity: 0; transform: perspective(600px) rotateY(-15deg) translateX(20px); }
.flip-leave-to { opacity: 0; transform: perspective(600px) rotateY(15deg) translateX(-20px); }

.trace-body {
  max-width: 720px;
  margin: 0 auto;

  &.layout-one-screen {
    padding: 0;
    max-width: 100%;
  }

  &.layout-modular-cards {
    gap: 12px !important;
    .page-element.el-type-enterprise-info,
    .page-element.el-type-product-info,
    .page-element.el-type-cert-info,
    .page-element.el-type-test-info,
    .page-element.el-type-base-info,
    .page-element.el-type-breed-archive,
    .page-element.el-type-farm-info,
    .page-element.el-type-transport-info,
    .page-element.el-type-slaughter-info,
    .page-element.el-type-cutting-record {
      flex: 0 0 calc(50% - 6px) !important;
      .trace-section {
        height: 100%;
        margin-bottom: 0;
      }
    }
  }

  &.layout-split-left-right {
    align-items: flex-start;
    .page-element.el-type-image:first-child {
      flex: 0 0 45% !important;
      order: -1;
      position: sticky;
      top: 12px;
    }
    .page-element.el-type-text {
      flex: 1 1 calc(55% - 12px) !important;
    }
  }

  &.layout-split-top-bottom {
    padding: 0;
    max-width: 100%;
    .page-element.el-type-image:first-child {
      flex: 0 0 100% !important;
      margin-bottom: 0;
      .el-image { border-radius: 0; }
    }
    .page-element.el-type-text {
      text-align: center;
      width: 100%;
      padding: 0 16px;
      box-sizing: border-box;
    }
  }

  &.layout-tabs {
    .page-nav {
      position: sticky;
      top: 0;
      z-index: 10;
      background: rgba(255,255,255,0.85);
      backdrop-filter: blur(8px);
      padding: 10px 0;
      margin: 0 -12px 12px;
      border-bottom: 1px solid var(--trace-border-color);
    }
  }

  &.layout-timeline {
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      top: 0;
      bottom: 0;
      width: 2px;
      background: var(--trace-divider-color);
      transform: translateX(-50%);
    }
    .page-element.el-type-enterprise-info,
    .page-element.el-type-product-info,
    .page-element.el-type-cert-info,
    .page-element.el-type-test-info,
    .page-element.el-type-base-info,
    .page-element.el-type-breed-archive,
    .page-element.el-type-farm-info,
    .page-element.el-type-transport-info,
    .page-element.el-type-slaughter-info,
    .page-element.el-type-cutting-record {
      position: relative;
      z-index: 1;
      width: calc(50% - 24px);
      &:nth-child(odd) { margin-right: auto; }
      &:nth-child(even) { margin-left: auto; }
      &::before {
        content: '';
        position: absolute;
        top: 20px;
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: var(--trace-accent);
      }
      &:nth-child(odd)::before { right: -30px; }
      &:nth-child(even)::before { left: -30px; }
    }
  }
}

/* 响应式排版 */
@media (max-width: 640px) {
  .trace-body {
    padding: 10px 10px 16px;
    &.layout-modular-cards {
      .page-element.el-type-enterprise-info,
      .page-element.el-type-product-info,
      .page-element.el-type-cert-info,
      .page-element.el-type-test-info,
      .page-element.el-type-base-info,
      .page-element.el-type-breed-archive,
      .page-element.el-type-farm-info,
      .page-element.el-type-transport-info,
      .page-element.el-type-slaughter-info,
      .page-element.el-type-cutting-record {
        flex: 0 0 100% !important;
      }
    }
    &.layout-split-left-right {
      .page-element.el-type-image:first-child {
        flex: 0 0 100% !important;
        position: relative;
        top: auto;
      }
      .page-element.el-type-text {
        flex: 0 0 100% !important;
      }
    }
    &.layout-timeline {
      &::before { left: 16px; }
      .page-element.el-type-enterprise-info,
      .page-element.el-type-product-info,
      .page-element.el-type-cert-info,
      .page-element.el-type-test-info,
      .page-element.el-type-base-info,
      .page-element.el-type-breed-archive,
      .page-element.el-type-farm-info,
      .page-element.el-type-transport-info,
      .page-element.el-type-slaughter-info,
      .page-element.el-type-cutting-record {
        width: calc(100% - 36px);
        margin-left: 36px !important;
        margin-right: 0 !important;
        &::before { left: -26px !important; right: auto !important; }
      }
    }
  }
  .trace-section {
    padding: 16px;
  }
  .modern-section .section-title-text {
    font-size: 15px;
  }
}
</style>
