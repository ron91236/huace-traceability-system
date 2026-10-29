<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>溯源长图</span>
          <el-select v-if="templates.length" v-model="templateId" style="width:240px"
            placeholder="选择长图模板" @change="loadData">
            <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </div>
      </template>

      <el-alert v-if="!templates.length" type="warning" :closable="false" style="margin-bottom:16px"
        title="尚未开通长图模板，请联系平台管理员在「企业管理」中分配后再配置" />

      <el-alert v-else-if="!modules.length" type="info" :closable="false" style="margin-bottom:16px"
        title="该模板还没有配置模块，请联系平台管理员在「溯源模板」中定义模块名称与顺序" />

      <div class="lm-layout" v-if="templates.length && modules.length">
        <!-- 模块清单只读：名称与顺序由模板决定，这里没有新增/改名/拖序/删除入口 -->
        <div class="lm-list">
          <div v-for="m in modules" :key="m.key" class="lm-item" :class="{ active: m.key === activeKey }"
            @click="select(m)">
            <span class="lm-name">{{ m.name }}</span>
            <el-tag size="small" :type="m.pages.length ? 'success' : 'info'">
              {{ m.pages.length ? m.pages.length + ' 页' : '未填' }}
            </el-tag>
          </div>
        </div>

        <div class="lm-detail" v-if="active">
          <div class="lm-title">{{ active.name }}</div>
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
              <span class="lm-tip">拖动图片可调整顺序；单个模块最多 {{ MAX_PAGES }} 页；清空并保存等于撤掉该模块内容</span>
            </div>
          </div>
          <div class="lm-detail-footer">
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button @click="handleRevert">还原</el-button>
          </div>
        </div>
        <div v-else class="lm-blank">选择左侧模块进行填写</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Sortable from 'sortablejs'
import { getLongModules, saveLongModulePages, convertModulePdf, getAssignedTemplates } from '@/api/enterprise'
import { uploadFile } from '@/api/common'

/** 与后端 LongModuleService.MAX_PAGES 一致 */
const MAX_PAGES = 30

const templates = ref<any[]>([])
const templateId = ref<number | null>(null)
// 后端返回的是"模板骨架 ⊕ 已填页图"，pages 已经是数组，不再是 JSON 字符串
const modules = ref<Array<{ key: string; name: string; sortOrder: number; pages: string[] }>>([])
const activeKey = ref<string | null>(null)
const pageList = ref<string[]>([])
const saving = ref(false)
const converting = ref(false)
const pagesRef = ref<HTMLElement>()
let pagesSortable: Sortable | null = null

const active = computed(() => modules.value.find(m => m.key === activeKey.value) || null)

onMounted(async () => {
  try {
    // 按渲染模式判定而非 templateKey：长图模板的 key 是 tpl_<时间戳>，判 key 会静默失效
    const t = await getAssignedTemplates()
    templates.value = (t.data || []).filter((x: any) => x.renderMode === 'long-image')
    if (templates.value.length) {
      templateId.value = templates.value[0].id
      await loadData()
    }
  } catch {
    // 刻意不给兜底文案：templates 保持空数组，页面就落在"尚未开通"的提示态（fail-closed），
    // 而不是显示一个能点但必然 500 的配置界面。错误提示本身由 request 拦截器弹。
    templates.value = []
  }
})

async function loadData() {
  if (!templateId.value) return
  const res: any = await getLongModules(templateId.value)
  modules.value = res.data || []
  if (!modules.value.some(m => m.key === activeKey.value)) select(modules.value[0])
}

function select(m?: any) {
  if (!m) { activeKey.value = null; pageList.value = []; return }
  activeKey.value = m.key
  pageList.value = [...m.pages]
}

async function onImagePick(file: any) {
  try {
    const res: any = await uploadFile(file.raw)
    const url = res.data?.url
    if (!url) return ElMessage.error('上传失败')
    if (pageList.value.length >= MAX_PAGES) return ElMessage.warning(`最多 ${MAX_PAGES} 页`)
    pageList.value = [...pageList.value, url]
  } catch {
    // request 拦截器已处理错误提示
  }
}

async function onPdfPick(file: any) {
  converting.value = true
  try {
    const up: any = await uploadFile(file.raw)
    const url = up.data?.url
    if (!url) return ElMessage.error('上传失败')
    const res: any = await convertModulePdf(url)
    const pages = res.data || []
    if (!pages.length) return ElMessage.error('PDF 未能转出图片，请确认文件未加密')
    if (pageList.value.length + pages.length > MAX_PAGES) return ElMessage.warning(`转换后超过 ${MAX_PAGES} 页，请拆分 PDF`)
    pageList.value = [...pageList.value, ...pages]
    ElMessage.success(`已转换 ${pages.length} 页`)
  } catch {
    // request 拦截器已处理错误提示
  } finally { converting.value = false }
}

function removePage(i: number) {
  pageList.value = pageList.value.filter((_, idx) => idx !== i)
}

function handleRevert() { select(active.value) }

async function handleSave() {
  if (!templateId.value || !activeKey.value) return
  saving.value = true
  try {
    // 空数组是合法提交：后端按 spec §12 把它解释为"企业未填"，删行而不是写空数组
    await saveLongModulePages(templateId.value, activeKey.value, pageList.value)
    ElMessage.success('保存成功')
    await loadData()
  } catch {
    // request 拦截器已处理错误提示
  } finally { saving.value = false }
}

function initSortable() {
  nextTick(() => {
    if (!pagesRef.value) { pagesSortable?.destroy(); pagesSortable = null; return }
    pagesSortable?.destroy()
    pagesSortable = new Sortable(pagesRef.value, {
      animation: 200, handle: '.lm-page',
      onEnd: (evt: any) => {
        if (evt.oldIndex === undefined || evt.newIndex === undefined) return
        const moved = pageList.value.splice(evt.oldIndex, 1)[0]
        pageList.value.splice(evt.newIndex, 0, moved)
      },
    })
  })
}

// 详情区受 v-if="active" 控制，首次挂载时不存在，须在编辑对象切换后重建拖拽
watch(activeKey, () => initSortable())

onUnmounted(() => pagesSortable?.destroy())
</script>

<style scoped lang="scss">
.lm-layout { display: flex; gap: 16px; align-items: flex-start; }
.lm-list { width: 220px; flex: none; border: 1px solid var(--el-border-color); border-radius: 8px; padding: 8px; }
.lm-item { display: flex; align-items: center; gap: 6px; padding: 8px; border-radius: 6px; cursor: pointer; }
.lm-item:hover { background: var(--el-fill-color-light); }
.lm-item.active { background: var(--el-color-primary-light-9); }
.lm-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lm-blank { color: #909399; font-size: 13px; padding: 16px; }
.lm-detail { flex: 1; min-width: 0; }
.lm-title { font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.lm-detail-footer { display: flex; gap: 8px; margin-top: 12px; }
.lm-pages { width: 100%; }
.lm-pages-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
.lm-page { position: relative; border: 1px solid var(--el-border-color); border-radius: 6px; overflow: hidden; cursor: move; }
.lm-page img { display: block; width: 100%; }
.lm-page-del { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; line-height: 18px; text-align: center; background: rgba(0,0,0,.55); color: #fff; border-radius: 50%; }
.lm-pages-actions { display: flex; align-items: center; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.lm-tip { font-size: 12px; color: #909399; }
</style>
