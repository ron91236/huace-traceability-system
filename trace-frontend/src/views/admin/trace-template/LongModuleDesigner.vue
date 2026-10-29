<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    :title="templateData?.id ? '编辑长图模板' : '新增长图模板'" width="720px" top="6vh"
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
        <img v-if="form.backgroundImage" :src="form.backgroundImage" class="ld-bg" />
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" class="ld-tip"
      title="这里只定义模块骨架（名称与顺序）。整页长图由各企业在自己的「溯源长图」里上传，管理端不碰内容。" />

    <div class="ld-list" ref="listRef">
      <div v-for="(m, i) in form.modules" :key="m.key" class="ld-item">
        <span class="ld-drag">⠿</span>
        <el-input v-model="m.name" size="small" maxlength="50" class="ld-name" placeholder="模块名称，如 产品信息" />
        <span class="ld-key">{{ m.key }}</span>
        <el-popconfirm title="删除该模块？企业已传的图会保留但不再展示" @confirm="removeModule(i)">
          <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
        </el-popconfirm>
      </div>
      <div v-if="!form.modules.length" class="ld-empty">还没有模块。企业端要看到这一栏才能填图，至少加一个。</div>
    </div>

    <div class="ld-foot">
      <el-button size="small" :disabled="form.modules.length >= MODULE_MAX" @click="addModule">
        新增模块（{{ form.modules.length }}/{{ MODULE_MAX }}）
      </el-button>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import Sortable from 'sortablejs'
import { createTraceTemplate, updateTraceTemplate } from '@/api/admin'
import { uploadFile } from '@/api/common'

/** 与后端 TemplateConfigReader.MODULE_MAX 同一个上限，两处说法要一致 */
const MODULE_MAX = 10
/** 与后端 TemplateConfigReader.NAME_MAX 一致 */
const NAME_MAX = 50

const props = defineProps<{ modelValue: boolean; templateData: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; saved: [] }>()

const saving = ref(false)
const listRef = ref<HTMLElement>()
let sortable: Sortable | null = null

// rest 保存 config_json 里本轮设计器不负责的那些键（theme 等），保存时原样带回去，
// 免得"编辑长图骨架"这个动作顺手把别人写的配置洗掉。
const form = ref<any>({
  templateName: '',
  templateType: '长图溯源',
  backgroundImage: '',
  modules: [] as Array<{ key: string; name: string }>,
  rest: {} as any,
})

watch(() => props.templateData, (val) => {
  let config: any = null
  if (val?.configJson) {
    try { config = typeof val.configJson === 'string' ? JSON.parse(val.configJson) : val.configJson } catch { config = null }
  }
  const { longModules, ...rest } = config || {}
  const rows = Array.isArray(longModules) ? [...longModules] : []
  // 读的时候先按 sortOrder 排一次，数组顺序就是界面顺序；写的时候再按新顺序重编 sortOrder
  rows.sort((a: any, b: any) => Number(a?.sortOrder ?? 0) - Number(b?.sortOrder ?? 0))
  form.value = {
    templateName: val?.templateName || '',
    templateType: val?.templateType || '长图溯源',
    backgroundImage: val?.backgroundImage || '',
    modules: rows
      .filter((m: any) => m && m.key)
      .map((m: any) => ({ key: String(m.key), name: m.name ?? '' })),
    rest,
  }
}, { immediate: true })

function addModule() {
  if (form.value.modules.length >= MODULE_MAX) return ElMessage.warning(`最多 ${MODULE_MAX} 个模块`)
  // key 一经生成不可变：企业端的行是按 key 关联的，改名只动 name
  form.value.modules.push({ key: 'm' + Date.now(), name: '' })
}

function removeModule(i: number) {
  form.value.modules.splice(i, 1)
}

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
  const seen = new Set<string>()
  for (const m of form.value.modules) {
    if (!String(m.name || '').trim()) return ElMessage.warning('模块名称不能为空')
    if (String(m.name).trim().length > NAME_MAX) return ElMessage.warning(`模块名称不能超过 ${NAME_MAX} 字`)
    if (seen.has(m.key)) return ElMessage.warning('模块标识重复，请刷新页面后重试')
    seen.add(m.key)
  }
  saving.value = true
  try {
    const config = {
      ...form.value.rest,
      renderMode: 'long-image',
      schemaVersion: 2,
      longModules: form.value.modules.map((m: any, i: number) => ({
        key: m.key, name: String(m.name).trim(), sortOrder: i,
      })),
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
    // 后端 TemplateConfigReader.validate 的拒绝（重复 key / 超上限）已由拦截器弹出原文
  } finally {
    saving.value = false
  }
}

function initSortable() {
  nextTick(() => {
    if (!listRef.value) return
    sortable?.destroy()
    sortable = new Sortable(listRef.value, {
      animation: 200, handle: '.ld-drag',
      onEnd: (evt: any) => {
        if (evt.oldIndex === undefined || evt.newIndex === undefined) return
        const moved = form.value.modules.splice(evt.oldIndex, 1)[0]
        form.value.modules.splice(evt.newIndex, 0, moved)
      },
    })
  })
}

onMounted(initSortable)
// 列表由空变非空时 .ld-list 才刚被渲染出来，不重建就没有拖拽把手
watch(() => form.value.modules.length, initSortable)
onUnmounted(() => sortable?.destroy())
</script>

<style scoped lang="scss">
.ld-tip { margin-bottom: 12px; }
.ld-list { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 8px; min-height: 120px; }
.ld-item { display: flex; align-items: center; gap: 8px; padding: 6px 4px; border-radius: 6px; }
.ld-item:hover { background: var(--el-fill-color-light); }
.ld-drag { cursor: grab; color: #a8abb2; width: 16px; text-align: center; }
.ld-name { flex: 1; }
.ld-key { font-size: 12px; color: #909399; font-family: monospace; }
.ld-empty { color: #909399; font-size: 13px; padding: 16px; }
.ld-foot { margin-top: 10px; }
.ld-bg { display: inline-block; max-height: 32px; margin-left: 8px; vertical-align: middle; border-radius: 4px; }
</style>
