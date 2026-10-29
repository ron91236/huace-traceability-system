<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    :title="templateData?.id ? '编辑落地页模板' : '新增落地页模板'" width="760px" top="6vh"
    destroy-on-close :close-on-click-modal="false">

    <el-form :inline="true" size="small">
      <el-form-item label="模板名称" required>
        <el-input v-model="form.templateName" placeholder="模板名称" style="width:180px" />
      </el-form-item>
      <el-form-item label="模板类型">
        <el-input v-model="form.templateType" placeholder="类型" style="width:120px" />
      </el-form-item>
      <el-form-item label="背景图">
        <el-upload :show-file-list="false" :http-request="uploadBackground" accept="image/*" style="display:inline-block">
          <el-button size="small">上传</el-button>
        </el-upload>
        <el-button v-if="form.backgroundImage" size="small" type="danger" link
          @click="form.backgroundImage = ''" style="margin-left:4px">移除</el-button>
        <img v-if="form.backgroundImage" :src="form.backgroundImage" class="db-bg" />
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" class="db-tip"
      title="区块种类由系统固定（防伪查询 / 内容 / 宣传视频 / 宣传图片），这里只决定哪些块存在、按什么顺序排。内容由企业在「静态落地页」里填。" />

    <div class="db-list" ref="listRef">
      <div v-for="b in orderedBlocks" :key="b.key" class="db-item">
        <el-switch v-model="blocks[b.key]" size="small" />
        <span class="db-drag">⠿</span>
        <span class="db-name">{{ b.name }}</span>
        <el-tag size="small" type="info">{{ b.key }}</el-tag>
      </div>
    </div>

    <el-divider />

    <el-form :inline="true" size="small">
      <el-form-item label="侧边栏">
        <el-switch v-model="form.sideMenu.enabled" />
      </el-form-item>
      <el-form-item label="显示方式">
        <el-radio-group v-model="form.sideMenu.display" :disabled="!form.sideMenu.enabled">
          <el-radio-button value="float">悬浮（带把手）</el-radio-button>
          <el-radio-button value="inline">直显（常驻）</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="含「溯源详情」">
        <el-switch v-model="form.sideMenu.showTraceDetail" :disabled="!form.sideMenu.enabled" />
      </el-form-item>
      <el-form-item label="底部大按钮">
        <el-switch v-model="form.sideMenu.showBottomEntry" :disabled="!form.sideMenu.enabled" />
      </el-form-item>
      <el-form-item label="未验防伪禁止跳转">
        <el-switch v-model="form.gates.requireAntiFakeBeforeJump" />
      </el-form-item>
    </el-form>

    <el-alert v-if="form.gates.requireAntiFakeBeforeJump" type="warning" :closable="false" class="db-tip"
      title="开启后，消费者在完成防伪查询前点侧边栏任何跳转都会被拦下（企业外链与详情页都受约束）。" />

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import Sortable from 'sortablejs'
import { createTraceTemplate, updateTraceTemplate } from '@/api/admin'
import { uploadFile } from '@/api/common'

/** 与后端 TemplateConfigReader.LANDING_BLOCK_KEYS 一致；数组顺序就是新建模板时的默认顺序 */
const BLOCK_DEFS = [
  { key: 'antiFake', name: '防伪查询' },
  { key: 'richtext', name: '内容' },
  { key: 'video', name: '宣传视频' },
  { key: 'promoImages', name: '宣传图片' },
]
/** 与后端 TemplateConfigReader.SIDE_MENU_DISPLAYS 一致 */
const SIDE_MENU_DISPLAYS = ['float', 'inline']
/** 新模板的默认勾选：防伪与正文开，视频与宣传图关——落地页最常见的形态 */
const DEFAULT_ON: Record<string, boolean> = { antiFake: true, richtext: true, video: false, promoImages: false }
const DEFAULT_MENU = { enabled: true, display: 'float', showTraceDetail: true, showBottomEntry: true }
const DEFAULT_GATES = { requireAntiFakeBeforeJump: false }

const props = defineProps<{ modelValue: boolean; templateData: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; saved: [] }>()

const saving = ref(false)
const listRef = ref<HTMLElement>()
let sortable: Sortable | null = null

// order 是 key 数组，代表界面顺序；blocks 记每个 key 的启停
const order = ref<string[]>(BLOCK_DEFS.map(b => b.key))
const blocks = ref<Record<string, boolean>>({ ...DEFAULT_ON })
// rest 保存本设计器不负责的那些 config_json 键（theme 等），保存时原样带回，
// 免得"编辑落地页骨架"顺手把别人写的配置洗掉
const rest = ref<any>({})
const form = ref<any>({
  templateName: '',
  templateType: '落地页',
  backgroundImage: '',
  sideMenu: { ...DEFAULT_MENU },
  gates: { ...DEFAULT_GATES },
})

// 只给界面用的 key/name；启停直接绑在 blocks[key] 上，
// 走 computed 里的副本会让 el-switch 的 v-model 写进一个没人读的对象
const orderedBlocks = computed(() => order.value.map(k => ({
  key: k,
  name: BLOCK_DEFS.find(b => b.key === k)?.name || k,
})))

watch(() => props.templateData, (val) => {
  let config: any = null
  if (val?.configJson) {
    try { config = typeof val.configJson === 'string' ? JSON.parse(val.configJson) : val.configJson } catch { config = null }
  }
  const { landingBlocks, sideMenu, gates, ...others } = config || {}
  const rows = Array.isArray(landingBlocks) ? [...landingBlocks] : []
  rows.sort((a: any, b: any) => Number(a?.sortOrder ?? 0) - Number(b?.sortOrder ?? 0))
  // 只认枚举里的 key：脏数据或别人写进来的未知块直接忽略，
  // 否则保存会把一块永远渲染不出来的东西留在骨架里
  const known = rows.filter((b: any) => b && BLOCK_DEFS.some(d => d.key === b.key)).map((b: any) => b.key)
  const nextOrder = [...new Set(known)]
  for (const d of BLOCK_DEFS) if (!nextOrder.includes(d.key)) nextOrder.push(d.key)
  order.value = nextOrder

  const saved: Record<string, boolean> = {}
  for (const b of rows) saved[b.key] = b.enabled !== false
  // 以默认为底、只覆盖已存在的项：直接整表替换会让"系统后来新增的区块"丢掉默认勾选
  const nextBlocks: Record<string, boolean> = { ...DEFAULT_ON }
  for (const k of Object.keys(saved)) nextBlocks[k] = saved[k]
  blocks.value = nextBlocks

  rest.value = others
  form.value = {
    templateName: val?.templateName || '',
    templateType: val?.templateType || '落地页',
    backgroundImage: val?.backgroundImage || '',
    sideMenu: { ...DEFAULT_MENU, ...(sideMenu || {}) },
    gates: { ...DEFAULT_GATES, ...(gates || {}) },
  }
}, { immediate: true })

async function uploadBackground(options: any) {
  try {
    const res: any = await uploadFile(options.file)
    form.value.backgroundImage = res.data?.url || ''
    ElMessage.success('背景图上传成功')
  } catch {
    // request 拦截器已处理错误提示
  }
}

async function handleSave() {
  if (!String(form.value.templateName || '').trim()) return ElMessage.warning('请输入模板名称')
  if (!SIDE_MENU_DISPLAYS.includes(form.value.sideMenu.display)) {
    return ElMessage.warning('侧边栏显示方式只能是悬浮或直显')
  }
  saving.value = true
  try {
    const config = {
      ...rest.value,
      renderMode: 'landing',
      schemaVersion: 2,
      landingBlocks: order.value.map((k, i) => ({
        key: k,
        name: BLOCK_DEFS.find(b => b.key === k)?.name || k,
        sortOrder: i,
        enabled: blocks.value[k] !== false,
      })),
      sideMenu: {
        enabled: !!form.value.sideMenu.enabled,
        display: form.value.sideMenu.display,
        showTraceDetail: form.value.sideMenu.showTraceDetail !== false,
        showBottomEntry: form.value.sideMenu.showBottomEntry !== false,
      },
      gates: { requireAntiFakeBeforeJump: !!form.value.gates.requireAntiFakeBeforeJump },
    }
    const data = {
      templateName: form.value.templateName,
      templateType: form.value.templateType,
      backgroundImage: form.value.backgroundImage || null,
      configJson: JSON.stringify(config),
    }
    if (props.templateData?.id) await updateTraceTemplate(props.templateData.id, data)
    else await createTraceTemplate(data)
    ElMessage.success('保存成功')
    emit('saved')
  } catch {
    // 后端 TemplateConfigReader.validate 的拒绝已由拦截器弹出原文
  } finally { saving.value = false }
}

function initSortable() {
  nextTick(() => {
    if (!listRef.value) return
    sortable?.destroy()
    sortable = new Sortable(listRef.value, {
      animation: 200, handle: '.db-drag',
      onEnd: (evt: any) => {
        if (evt.oldIndex === undefined || evt.newIndex === undefined) return
        const moved = order.value.splice(evt.oldIndex, 1)[0]
        order.value.splice(evt.newIndex, 0, moved)
      },
    })
  })
}

onMounted(initSortable)
onUnmounted(() => sortable?.destroy())
</script>

<style scoped lang="scss">
.db-tip { margin-bottom: 12px; }
.db-list { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 8px; }
.db-item { display: flex; align-items: center; gap: 10px; padding: 7px 4px; border-radius: 6px; }
.db-item:hover { background: var(--el-fill-color-light); }
.db-drag { cursor: grab; color: #a8abb2; width: 16px; text-align: center; }
.db-name { flex: 1; font-size: 14px; }
.db-bg { display: inline-block; max-height: 32px; margin-left: 8px; vertical-align: middle; border-radius: 4px; }
</style>
