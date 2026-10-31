<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>静态落地页</span>
          <el-select v-if="templates.length" v-model="templateId" style="width:240px"
            placeholder="选择落地页模板" @change="loadData">
            <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </div>
      </template>

      <el-alert v-if="!templates.length" type="warning" :closable="false" style="margin-bottom:16px"
        title="尚未开通落地页模板，请联系平台管理员在「企业管理」中分配后再填写" />

      <el-alert v-else-if="!blocks.length" type="info" :closable="false" style="margin-bottom:16px"
        title="该模板还没有配置区块，请联系平台管理员在「溯源模板」中定义区块与顺序" />

      <div v-if="templates.length && blocks.length" class="lc-blocks">
        <div v-for="b in blocks" :key="b.key" class="lc-block">
          <div class="lc-head">
            <span class="lc-name">{{ b.name }}</span>
            <el-tag v-if="b.key === 'display'" size="small" type="info">改动即时生效</el-tag>
            <el-tag v-else-if="!b.enabled" size="small" type="info">模板未启用该区块</el-tag>
            <el-tag v-else size="small" :type="filled(b) ? 'success' : 'default'">{{ filled(b) ? '已填' : '未填' }}</el-tag>
          </div>

          <div v-if="b.key === 'display'" class="lc-display">
            <div v-for="it in DISPLAY_ITEMS" :key="it.key" class="lc-display-row">
              <div>
                <div class="lc-display-label">{{ it.label }}</div>
                <div class="lc-tip">{{ allowed[it.key] === false ? '该模板未开放此项，企业侧不能单独打开' : it.hint }}</div>
              </div>
              <el-switch :model-value="display[it.key] !== false" :disabled="allowed[it.key] === false || saving === b.key"
                @change="(v: any) => toggleDisplay(b, it.key, v === true)" />
            </div>
          </div>

          <RichTextEditor v-else-if="b.key === 'richtext'" v-model="draft[b.key]" :upload-image="uploadRichImage" />

          <div v-else-if="b.key === 'video'" class="lc-field">
            <el-input v-model="draft[b.key]" placeholder="宣传视频地址，以 /uploads/ 开头" />
            <video v-if="draft[b.key]" :src="draft[b.key]" controls class="lc-video" />
          </div>

          <div v-else-if="b.key === 'promoImages'" class="lc-field">
            <div class="lc-img-grid">
              <div v-for="(u, i) in images(b.key)" :key="u + i" class="lc-img">
                <img :src="u" />
                <span class="lc-img-del" @click="removeImage(b.key, i)">×</span>
              </div>
            </div>
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" multiple
              :on-change="(f: any) => addImage(b.key, f)">
              <el-button size="small">添加宣传图片</el-button>
            </el-upload>
          </div>

          <div v-else class="lc-sys">该区块由系统供给，无需填写。</div>

          <div v-if="b.key !== 'display'" class="lc-foot">
            <el-button type="primary" size="small" :loading="saving === b.key" @click="save(b)">保存</el-button>
            <el-button size="small" @click="revert(b)">还原</el-button>
            <span class="lc-tip">清空并保存等于撤掉这一区块的内容</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { getLandingContent, saveLandingBlock, getAssignedTemplates } from '@/api/enterprise'
import { uploadFile } from '@/api/common'

const templates = ref<any[]>([])
const templateId = ref<number | null>(null)
// 后端返回的是"模板骨架 ⊕ 已填内容"，content 已是可直接编辑的字符串；
// 只有末尾那条 display 的 content 是开关对象，另存在 display/allowed 里
const blocks = ref<Array<{ key: string; name: string; sortOrder: number; enabled: boolean; content: any; allowed?: any }>>([])
const draft = reactive<Record<string, string>>({})
const display = reactive<Record<string, boolean>>({})
const allowed = reactive<Record<string, boolean>>({})
const saving = ref('')

const DISPLAY_ITEMS = [
  { key: 'title', label: '页面标题', hint: '落地页顶部显示模板名，不想露出模板名就关掉' },
  { key: 'antiFake', label: '防伪查询区块', hint: '防伪结论由系统按每个溯源码实时生成' },
  { key: 'traceDetail', label: '侧边栏「溯源详情」', hint: '右侧按钮栏里的溯源详情入口' },
  { key: 'bottomEntry', label: '底部「查看溯源详情」', hint: '页面底部那个通栏大按钮' },
]

onMounted(async () => {
  try {
    // 按渲染模式判定而非 templateKey：落地页模板的 key 是 tpl_<时间戳>，判 key 会静默失效
    const t = await getAssignedTemplates()
    templates.value = (t.data || []).filter((x: any) => x.renderMode === 'landing')
    if (templates.value.length) {
      templateId.value = templates.value[0].id
      await loadData()
    }
  } catch {
    // fail-closed：接口异常时保持空数组，页面落在"尚未开通"提示态，
    // 而不是给一个能填但保存必然 500 的表单。错误文案由 request 拦截器弹。
    templates.value = []
  }
})

async function loadData() {
  if (!templateId.value) return
  const res: any = await getLandingContent(templateId.value)
  blocks.value = res.data || []
  for (const b of blocks.value) {
    if (b.key === 'display') {
      Object.assign(display, b.content || {})
      Object.assign(allowed, b.allowed || {})
      continue
    }
    draft[b.key] = String(b.content ?? '')
  }
}

/** promoImages 存的是 URL 的 JSON 数组（与后端 LandingContentService 的约定一致） */
function images(key: string): string[] {
  try {
    const v = JSON.parse(draft[key] || '[]')
    return Array.isArray(v) ? v.filter(Boolean) : []
  } catch { return [] }
}

function filled(b: any) {
  if (b.key === 'promoImages') return images(b.key).length > 0
  return !!String(draft[b.key] || '').replace(/<[^>]*>/g, '').trim()
}

function revert(b: any) { draft[b.key] = b.content || '' }

/** 开关与内容块共用同一个写入接口：值是 '1'/'0'，无行按展示处理 */
async function toggleDisplay(block: any, key: string, on: boolean) {
  if (!templateId.value) return
  saving.value = block.key
  try {
    await saveLandingBlock(templateId.value, key, on ? '1' : '0')
    ElMessage.success(on ? '已设为展示' : '已设为不展示')
  } catch {
    // 保存失败必须把开关弹回去：界面上留一个没落库的状态，企业会以为已经改好了
  } finally {
    saving.value = ''
    await loadData()
  }
}

async function uploadRichImage(file: File): Promise<string> {
  const res: any = await uploadFile(file)
  return res.data?.url || ''
}

async function addImage(key: string, file: any) {
  try {
    const res: any = await uploadFile(file.raw)
    const url = res.data?.url
    if (!url) return ElMessage.error('上传失败')
    draft[key] = JSON.stringify([...images(key), url])
  } catch {
    // request 拦截器已处理错误提示
  }
}

function removeImage(key: string, i: number) {
  draft[key] = JSON.stringify(images(key).filter((_, idx) => idx !== i))
}

async function save(b: any) {
  if (!templateId.value) return
  saving.value = b.key
  try {
    // 空串是合法提交：后端按 spec §12 解释为"企业未填"，删行而不是写空内容
    await saveLandingBlock(templateId.value, b.key, draft[b.key] || '')
    ElMessage.success('保存成功')
    await loadData()
  } catch {
    // request 拦截器已处理错误提示
  } finally { saving.value = '' }
}
</script>

<style scoped lang="scss">
.lc-blocks { display: flex; flex-direction: column; gap: 16px; }
.lc-block { border: 1px solid var(--el-border-color); border-radius: 10px; padding: 14px 16px; }
.lc-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.lc-name { font-size: 15px; font-weight: 600; }
.lc-field { display: flex; flex-direction: column; gap: 10px; }
.lc-video { max-height: 220px; width: 100%; }
.lc-img-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
.lc-img { position: relative; border: 1px solid var(--el-border-color); border-radius: 6px; overflow: hidden; }
.lc-img img { display: block; width: 100%; }
.lc-img-del { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; line-height: 18px; text-align: center; background: rgba(0,0,0,.55); color: #fff; border-radius: 50%; }
.lc-sys { font-size: 13px; color: #606266; }
.lc-display { display: flex; flex-direction: column; }
.lc-display-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 0; border-top: 1px solid var(--el-border-color-lighter); }
.lc-display-row:first-child { border-top: none; }
.lc-display-label { font-size: 14px; }
.lc-foot { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.lc-tip { font-size: 12px; color: #909399; }
</style>
