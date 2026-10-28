<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>溯源长图模块</span>
          <el-button type="primary" :disabled="!enabled" @click="openCreate">新增模块</el-button>
        </div>
      </template>

      <el-alert v-if="!enabled" type="warning" :closable="false" style="margin-bottom:16px"
        title="尚未开通长图模板，请联系平台管理员在「企业管理」中分配后再配置" />

      <div class="lm-layout">
        <div class="lm-list" ref="listRef">
          <div v-for="m in modules" :key="m.id" class="lm-item" :class="{ active: m.id === activeId }" @click="select(m)">
            <span class="lm-name">{{ m.name }}</span>
            <el-tag size="small" :type="m.goodsId ? 'success' : 'info'">
              {{ m.goodsId ? goodsName(m.goodsId) : '全部商品' }}
            </el-tag>
            <el-tag v-if="m.status !== 1" size="small" type="danger">停用</el-tag>
          </div>
          <div v-if="!modules.length" class="lm-empty">暂无模块，点击右上角新增</div>
        </div>

        <div class="lm-detail" v-if="active">
          <el-form label-width="96px">
            <el-form-item label="模块名称" required>
              <el-input v-model="active.name" maxlength="50" show-word-limit placeholder="如：有机养土" />
            </el-form-item>
            <el-form-item label="适用商品">
              <el-select v-model="active.goodsId" clearable placeholder="不选=该企业全部商品" style="width:100%">
                <el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="启用状态">
              <el-switch v-model="active.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="长图内容">
              <div class="lm-pages">
                <div class="lm-pages-grid" ref="pagesRef">
                  <div v-for="(p, i) in pageList" :key="p + i" class="lm-page">
                    <img :src="p" />
                    <span class="lm-page-del" @click="removePage(i)">×</span>
                  </div>
                </div>
                <div class="lm-pages-actions">
                  <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onImagePick" multiple>
                    <el-button size="small">添加长图</el-button>
                  </el-upload>
                  <el-upload :auto-upload="false" :show-file-list="false" accept="application/pdf" :on-change="onPdfPick">
                    <el-button size="small" :loading="converting">上传 PDF 转整页</el-button>
                  </el-upload>
                  <span class="lm-tip">单个模块最多 {{ MAX_PAGES }} 页</span>
                </div>
              </div>
            </el-form-item>
          </el-form>
          <div class="lm-detail-footer">
            <el-button type="primary" :disabled="!enabled" :loading="saving" @click="handleSave">保存</el-button>
            <el-button @click="handleRevert">还原</el-button>
            <el-popconfirm title="确认删除该模块？" @confirm="handleDelete">
              <template #reference><el-button type="danger" plain>删除模块</el-button></template>
            </el-popconfirm>
          </div>
        </div>
        <div v-else class="lm-blank">选择左侧模块进行编辑</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Sortable from 'sortablejs'
import { getLongModules, createLongModule, updateLongModule, deleteLongModule, sortLongModules, convertModulePdf } from '@/api/enterprise'
import { getGoods, getAssignedTemplates } from '@/api/enterprise'
import { uploadFile } from '@/api/common'

const MAX_PAGES = 30

const modules = ref<any[]>([])
const goodsList = ref<any[]>([])
const enabled = ref(true)
const activeId = ref<number | null>(null)
const active = ref<any>(null)
const pageList = ref<string[]>([])
const saving = ref(false)
const converting = ref(false)
const listRef = ref<HTMLElement>()
const pagesRef = ref<HTMLElement>()
let listSortable: Sortable | null = null
let pagesSortable: Sortable | null = null

onMounted(async () => {
  try { const g = await getGoods({ page: 1, size: 200 }); goodsList.value = g.data?.list || [] } catch (e) {}
  try {
    // 按渲染模式判定而非 templateKey：P2 起长图模板的 key 是 tpl_<时间戳>，判 key 会静默失效
    const t = await getAssignedTemplates()
    enabled.value = (t.data || []).some((x: any) => x.renderMode === 'long-image')
  } catch (e) {
    // fail-closed：原先 catch 里置 true，接口一抖就把未开通企业放开
    enabled.value = false
  }
  await loadData()
  initSortable()
})

async function loadData() {
  const res = await getLongModules()
  modules.value = res.data || []
  if (activeId.value && !modules.value.some(m => m.id === activeId.value)) select(modules.value[0])
}

function select(m?: any) {
  if (!m) { activeId.value = null; active.value = null; pageList.value = []; return }
  activeId.value = m.id
  active.value = { ...m }
  pageList.value = parsePages(m.pages)
}

function parsePages(json: string): string[] {
  try { const v = JSON.parse(json || '[]'); return Array.isArray(v) ? v.filter(Boolean) : [] } catch (e) { return [] }
}

function goodsName(id: number) {
  return goodsList.value.find(g => g.id === id)?.name || `商品#${id}`
}

function openCreate() {
  activeId.value = null
  active.value = { name: '', goodsId: null, status: 1 }
  pageList.value = []
}

async function onImagePick(file: any) {
  const res = await uploadFile(file.raw)
  const url = res.data?.url
  if (!url) return ElMessage.error('上传失败')
  if (pageList.value.length >= MAX_PAGES) return ElMessage.warning(`最多 ${MAX_PAGES} 页`)
  pageList.value = [...pageList.value, url]
}

async function onPdfPick(file: any) {
  converting.value = true
  try {
    const up = await uploadFile(file.raw)
    const url = up.data?.url
    if (!url) return ElMessage.error('上传失败')
    const res = await convertModulePdf(url)
    const pages = res.data || []
    if (!pages.length) return ElMessage.error('PDF 未能转出图片，请确认文件未加密')
    if (pageList.value.length + pages.length > MAX_PAGES) return ElMessage.warning(`转换后超过 ${MAX_PAGES} 页，请拆分 PDF`)
    pageList.value = [...pageList.value, ...pages]
    ElMessage.success(`已转换 ${pages.length} 页`)
  } finally { converting.value = false }
}

function removePage(i: number) {
  pageList.value = pageList.value.filter((_, idx) => idx !== i)
}

function handleRevert() { select(modules.value.find(m => m.id === activeId.value)) }

async function handleSave() {
  if (!active.value) return
  // 文案与后端 LongModuleService.validate 一致：本地先拦是为了不必为一个必然失败的请求
  // 走一趟网络，两处说法不同才会让用户怀疑自己
  if (!String(active.value.name || '').trim()) return ElMessage.warning('模块名称不能为空')
  if (!pageList.value.length) return ElMessage.warning('模块至少一张长图')
  saving.value = true
  try {
    const payload = { ...active.value, pages: JSON.stringify(pageList.value) }
    if (active.value.id) await updateLongModule(active.value.id, payload)
    else await createLongModule(payload)
    ElMessage.success('保存成功')
    await loadData()
    // 能按 id 就按 id 回选：名称不可靠——新建时后端会 trim 后落库，改名时旧名字已不存在，
    // 两种情况按名字找都会落空并跳到第一个模块，用户以为保存没生效
    const saved = payload.id
      ? modules.value.find(m => m.id === payload.id)
      : modules.value.find(m => m.name === String(payload.name || '').trim())
    select(saved || modules.value[0])
  } finally { saving.value = false }
}

async function handleDelete() {
  if (!active.value?.id) { active.value = null; return }
  await deleteLongModule(active.value.id)
  ElMessage.success('删除成功')
  activeId.value = null
  await loadData()
  select(modules.value[0])
}

function initSortable() {
  nextTick(() => {
    if (listRef.value) {
      listSortable?.destroy()
      listSortable = new Sortable(listRef.value, {
        animation: 200, handle: '.lm-item',
        onEnd: async (evt: any) => {
          if (evt.oldIndex === undefined || evt.newIndex === undefined) return
          const moved = modules.value.splice(evt.oldIndex, 1)[0]
          modules.value.splice(evt.newIndex, 0, moved)
          try {
            await sortLongModules(modules.value.map(m => m.id))
          } catch (e) {
            // 排序没落库就得把本地顺序拉回服务端真值，否则用户眼前这个顺序刷新后会跳回去，
            // 而拦截器只弹一句错误文案，看不出"界面在骗你"
            await loadData()
          }
        },
      })
    }
    if (pagesRef.value) {
      pagesSortable?.destroy()
      pagesSortable = new Sortable(pagesRef.value, {
        animation: 200, handle: '.lm-page',
        onEnd: (evt: any) => {
          if (evt.oldIndex === undefined || evt.newIndex === undefined) return
          const moved = pageList.value.splice(evt.oldIndex, 1)[0]
          pageList.value.splice(evt.newIndex, 0, moved)
        },
      })
    } else {
      // 详情区被 v-if 摘掉（删掉最后一个模块、还原未保存的新模块）时 pagesRef 为 null，
      // 不主动销毁就会留一个挂在已脱离文档的节点上的实例，直到下次选中才回收
      pagesSortable?.destroy()
      pagesSortable = null
    }
  })
}

// 详情区受 v-if="active" 控制，首次挂载时不存在，须在每次编辑对象切换后重建拖拽。
// 监听 active 而非 activeId：新增未保存模块时 activeId 一直是 null，监听它不会触发，
// 结果是企业建第一个模块时长图无法拖动排序。
watch(active, () => initSortable())

onUnmounted(() => { listSortable?.destroy(); pagesSortable?.destroy() })
</script>

<style scoped lang="scss">
.lm-layout { display: flex; gap: 16px; align-items: flex-start; }
.lm-list { width: 220px; flex: none; border: 1px solid var(--el-border-color); border-radius: 8px; padding: 8px; }
.lm-item { display: flex; align-items: center; gap: 6px; padding: 8px; border-radius: 6px; cursor: pointer; }
.lm-item:hover { background: var(--el-fill-color-light); }
.lm-item.active { background: var(--el-color-primary-light-9); }
.lm-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lm-empty, .lm-blank { color: #909399; font-size: 13px; padding: 16px; }
.lm-detail { flex: 1; min-width: 0; }
.lm-detail-footer { display: flex; gap: 8px; margin-top: 8px; }
.lm-pages { width: 100%; }
.lm-pages-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
.lm-page { position: relative; border: 1px solid var(--el-border-color); border-radius: 6px; overflow: hidden; cursor: move; }
.lm-page img { display: block; width: 100%; }
.lm-page-del { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; line-height: 18px; text-align: center; background: rgba(0,0,0,.55); color: #fff; border-radius: 50%; }
.lm-pages-actions { display: flex; align-items: center; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.lm-tip { font-size: 12px; color: #909399; }
</style>
