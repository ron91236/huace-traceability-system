<template>
  <div class="landing" :style="pageStyle">
    <template v-if="loaded">
      <h1 v-if="showTitle && payload.templateName" class="ld-title">{{ payload.templateName }}</h1>

      <div class="ld-body">
        <section v-for="b in blocks" :key="b.key" :class="['ld-block', 'ld-block-' + b.key]">
          <!-- 富文本由企业填写，与 TraceView 同一处置：过 DOMPurify 再进 DOM -->
          <div v-if="b.key === 'richtext'" class="ld-richtext" v-html="DOMPurify.sanitize(b.content || '')"></div>

          <video v-else-if="b.key === 'video' && b.content" :src="b.content" controls preload="metadata" class="ld-video" />

          <div v-else-if="b.key === 'promoImages'" class="ld-promos">
            <img v-for="(u, i) in promoList(b)" :key="u + i" :src="u" class="ld-promo" loading="lazy" :alt="`宣传图片 ${i + 1}`" />
          </div>

          <div v-else-if="b.key === 'antiFake'" class="ld-antifake">
            <div class="ld-af-title">防伪查询</div>
            <div v-if="fwResult" :class="['ld-af-result', fwResult.verified ? 'ok' : 'fail']">
              {{ fwResult.verified ? '✅ 验证通过' : '❌ 验证未通过' }}
              <span v-if="fwResult.verified && fwResult.queryTime" class="ld-af-time">（{{ fwResult.queryTime }}）</span>
            </div>
            <button v-else class="ld-af-btn" :disabled="fwLoading" @click="doVerify">
              {{ fwLoading ? '查询中…' : '立即查询防伪' }}
            </button>
          </div>
        </section>
      </div>

      <!-- 底部大按钮：移动端拇指热区，与侧栏的「溯源详情」是两个入口、同一个目标（spec §9.1） -->
      <a v-if="showBottomEntry" class="ld-bottom" @click="goTrace">查看溯源详情 ›</a>

      <div v-if="menuVisible" :class="['ld-float', { collapsed: isFloat && collapsed }]">
        <div v-if="isFloat" class="ld-float-trigger" @click="toggle">{{ collapsed ? '‹' : '›' }}</div>
        <div class="ld-btn-group">
          <button v-if="showTraceDetail" class="ld-btn ld-btn-trace" @click="goTrace">
            <span class="ld-btn-name">溯源详情</span>
          </button>
          <button v-for="l in links" :key="l.id" class="ld-btn" @click="onLink(l)">
            <img v-if="isIconImage(l.icon)" :src="l.icon" class="ld-btn-ico" :alt="l.name" />
            <span class="ld-btn-name">{{ l.name }}</span>
          </button>
        </div>
      </div>
    </template>

    <!-- 资料查看器：单张二维码与多页报告走同一条路径，竖着滑比左右翻更适合手机上扫大图 -->
    <el-dialog v-model="mediaVisible" :title="mediaTitle" width="92vw" top="5vh">
      <div class="ld-media-scroll">
        <img v-for="(p, i) in mediaPages" :key="p + i" :src="p" class="ld-media-img" loading="lazy"
          :alt="`${mediaTitle} 第 ${i + 1} 页`" />
        <div v-if="mediaPages.length === 0" class="ld-media-empty">资料暂时无法显示</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { getLanding, directVerify } from '@/api/common'
import { isIconImage } from '@/utils/constants'

const route = useRoute()
const router = useRouter()

const sn = computed(() => String(route.params.serialNo || ''))
const payload = ref<any>({})
const loaded = ref(false)
const collapsed = ref(true)
const fwResult = ref<any>(null)
const fwLoading = ref(false)
const mediaVisible = ref(false)
const mediaPages = ref<string[]>([])
const mediaTitle = ref('')

const blocks = computed(() => payload.value.blocks || [])
const links = computed(() => payload.value.sideLinks || [])
const menu = computed(() => payload.value.sideMenu || {})
// 生效后的展示开关：模板允许的与企业没关的已经在后端取过交集了，前端只看这一份。
// 载荷里没有这块（旧缓存）时全部按"展示"处理，不给消费者一个凭空少东西的页面。
const display = computed<any>(() => payload.value.display || {})
const isFloat = computed(() => menu.value.display !== 'inline')
const showTitle = computed(() => display.value.title !== false)
const showTraceDetail = computed(() => display.value.traceDetail !== false)
const showBottomEntry = computed(() => display.value.bottomEntry !== false)
// 无外链且不显示溯源详情时整个侧边栏不出现（照参考页 hasMenuItems）；底部入口独立，不受此约束
const menuVisible = computed(() => !!menu.value.enabled && (links.value.length > 0 || showTraceDetail.value))

const pageStyle = computed(() => (payload.value.backgroundImage
  ? { backgroundImage: `url(${payload.value.backgroundImage})` } : {}))

function promoList(b: any): string[] {
  try {
    const v = JSON.parse(b.content || '[]')
    return Array.isArray(v) ? v.filter(Boolean) : []
  } catch { return [] }
}

function toggle() { collapsed.value = !collapsed.value }

/**
 * 闸门：模板要求先做防伪查询时，未验证过就不许跳。
 * 挡的是"跳转"这个动作，查询本身永远开放。
 */
function gateBlocks(): boolean {
  if (!payload.value.gates?.requireAntiFakeBeforeJump) return false
  if (fwResult.value) return false
  ElMessage.warning('请先完成防伪查询')
  return true
}

function goTrace() {
  if (gateBlocks()) return
  // detail=1 让 TraceView 别再转回落地页：它自己就是落地页「溯源详情」按钮的目标
  router.push({ path: `/trace/${sn.value}`, query: { detail: '1' } })
}

function onLink(l: any) {
  if (gateBlocks()) return
  // 三分支，与参考页一致：有链接就跳，只有资料就弹查看器，两者都空是死按钮不响应。
  // url 的协议在 SideLinkService.validate 里已经限死 http/https，这里不再重复判一遍
  if (l.url) { window.location.href = l.url; return }
  // 判"有没有配资料"看 image 而不是 pages：pages 为空只说明 PDF 展开失败，
  // 那要给一句「资料暂时无法显示」，而不是让按钮点了没反应
  if (!l.image) return
  mediaPages.value = pagesOf(l)
  mediaTitle.value = l.name || '资料'
  mediaVisible.value = true
}

/** 后端把 PDF 展开成页图放进 pages；后端回滚时回落到 image 本身，单份图片资料照样能看 */
function pagesOf(l: any): string[] {
  const raw = Array.isArray(l.pages)
    ? l.pages
    : (typeof l.image === 'string' && l.image.startsWith('/uploads/') ? [l.image] : [])
  return raw.filter((p: any) => typeof p === 'string' && p !== '')
}

async function doVerify() {
  fwLoading.value = true
  try {
    const res: any = await directVerify(sn.value)
    fwResult.value = res.data || { verified: false }
  } catch {
    // 查询失败也要给个结论，否则用户会一直停在"可以点查询"的状态里反复试
    fwResult.value = { verified: false }
  } finally { fwLoading.value = false }
}

onMounted(async () => {
  try {
    const res: any = await getLanding(sn.value)
    payload.value = res.data || {}
  } catch {
    // 取不到载荷就按"不是落地页"处理，交给详情页去显示真正的错误
    payload.value = { isLanding: false }
  }
  // 两种"该看详情页"的情形统一跳：非落地页模板、以及企业三项内容全空（spec §9.2/§12）
  if (!payload.value.isLanding || payload.value.contentEmpty) {
    router.replace(`/trace/${sn.value}`)
    return
  }
  loaded.value = true
  collapsed.value = isFloat.value
  // 闸门开着时替消费者先查一次：只给一句"请先完成防伪查询"却不给查询入口，用户会卡在原地
  if (payload.value.gates?.requireAntiFakeBeforeJump && !fwResult.value) doVerify()
})
</script>

<style scoped lang="scss">
.landing { min-height: 100vh; background: #f7f8fa center/cover no-repeat; padding-bottom: 88px; }
.ld-title { font-size: 18px; font-weight: 600; text-align: center; padding: 16px 12px 4px; margin: 0; }
.ld-body { max-width: 720px; margin: 0 auto; }
.ld-block { background: #fff; border-radius: 12px; margin: 12px; padding: 14px 16px; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
.ld-richtext { font-size: 14px; line-height: 1.8; word-break: break-word; }
.ld-richtext :deep(img) { max-width: 100%; height: auto; }
.ld-video { width: 100%; border-radius: 8px; background: #000; }
.ld-promos { display: flex; flex-direction: column; gap: 8px; }
.ld-promo { width: 100%; border-radius: 8px; }
.ld-af-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.ld-af-result { font-size: 14px; font-weight: 600; }
.ld-af-result.ok { color: #059669; }
.ld-af-result.fail { color: #dc2626; }
.ld-af-time { font-weight: 400; color: #909399; font-size: 13px; }
.ld-af-btn { width: 100%; padding: 10px; border: none; border-radius: 8px; background: var(--trace-accent, #059669); color: #fff; font-size: 14px; }
.ld-af-btn:disabled { opacity: .6; }
.ld-bottom {
  position: fixed; left: 12px; right: 12px; bottom: calc(12px + env(safe-area-inset-bottom, 0px)); z-index: 15;
  display: block; text-align: center; padding: 13px; border-radius: 999px;
  background: var(--trace-accent, #059669); color: #fff; font-size: 15px; text-decoration: none;
}
.ld-float {
  position: fixed; right: 0; top: 45%; z-index: 20;
  display: flex; align-items: center; gap: 6px; transition: transform .25s ease;
}
/* 悬浮态收起时整体右移，只留把手；直显态不加这个类，也就没有收起一说 */
.ld-float.collapsed { transform: translateX(calc(100% - 22px)); }
.ld-float-trigger {
  width: 22px; height: 54px; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.45); color: #fff; border-radius: 8px 0 0 8px; font-size: 14px; cursor: pointer;
}
.ld-btn-group {
  display: flex; flex-direction: column; align-items: flex-end; gap: 6px;
  /* 进场时往屏幕内探两次：静态贴在一旁的东西很容易被当成页面装饰 */
  animation: ld-nudge .9s ease-out .5s 2;
}
@keyframes ld-nudge { 0%, 100% { transform: translateX(0); } 45% { transform: translateX(-10px); } }
@media (prefers-reduced-motion: reduce) { .ld-btn-group { animation: none; } }
/* 窄栏竖排：图标在上、文案在下，长名字换行而不是把侧栏撑宽。
   66px 是实测出来的：文字区在 62~70px 之间都会断成「低GI产品 / 证书」「朝日唯品 / Morning / Club」，
   到 72px 就变成「低GI产品证 / 书」——留 6px 余量是因为各机型字体度量不完全一样。 */
.ld-btn {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  width: 66px; max-width: 22vw; padding: 8px 5px 9px; border: none; border-radius: 10px 0 0 10px;
  background: rgba(255, 255, 255, .97); color: var(--trace-accent, #059669); font-size: 12px;
  line-height: 1.3; text-align: center; overflow-wrap: break-word; cursor: pointer;
  box-shadow: -1px 2px 8px rgba(0, 0, 0, .18); -webkit-tap-highlight-color: transparent;
  transition: transform .12s ease, background .12s ease;
}
/* 按下去往屏幕里缩一点：触屏没有 hover，只能靠按压反馈说明它是个按钮 */
.ld-btn:active { transform: translateX(3px); background: #e9fbf0; }
.ld-btn-trace { font-weight: 600; }
.ld-btn-ico { width: 30px; height: 30px; object-fit: contain; }
/* 内容区自己滚，弹层整体不跟着页面滚：多页报告滑到底时标题还得留在屏幕上 */
.ld-media-scroll { max-height: 74vh; overflow-y: auto; -webkit-overflow-scrolling: touch; }
.ld-media-img { display: block; width: 100%; border-radius: 6px; }
.ld-media-img + .ld-media-img { margin-top: 8px; }
.ld-media-empty { padding: 48px 0; text-align: center; color: #909399; font-size: 14px; }
</style>
