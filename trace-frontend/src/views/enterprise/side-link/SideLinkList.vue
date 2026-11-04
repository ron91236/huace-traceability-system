<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>侧边栏链接</span>
          <el-button type="primary" @click="openCreate">新增链接</el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom:16px"
        title="这些按钮出现在消费者扫码后的落地页右侧。填了跳转地址就直接跳转；只填资料（图片或多页 PDF）就点开弹层上下滑动查看。拖动第一列可调整顺序。" />

      <el-table :data="links" border stripe v-loading="loading" size="default" row-key="id">
        <el-table-column width="40">
          <template #default><span class="sl-drag">⠿</span></template>
        </el-table-column>
        <el-table-column label="按钮文案" min-width="120">
          <template #default="{ row }">
            <span class="sl-name-cell">
              <img v-if="isIconImage(row.icon)" :src="row.icon" class="sl-name-ico" :alt="row.name" />
              <span>{{ row.name }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="跳转地址" min-width="200">
          <template #default="{ row }">
            <a v-if="row.url" :href="row.url" target="_blank" rel="noopener noreferrer" class="sl-link">{{ row.url }}</a>
            <span v-else class="sl-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="资料" width="110" align="center">
          <template #default="{ row }">
            <template v-if="mediaOf(row).length">
              <el-image v-if="imagesOf(row).length" :src="imagesOf(row)[0]" :preview-src-list="imagesOf(row)"
                preview-teleported fit="cover" class="sl-thumb" />
              <div class="sl-media-count">{{ mediaOf(row).length }} 份</div>
            </template>
            <span v-else class="sl-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该链接？" @confirm="remove(row.id)">
              <template #reference><el-button type="danger" link size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty><el-empty description="还没有链接" :image-size="60" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing?.id ? '编辑链接' : '新增链接'" width="560px" :close-on-click-modal="false">
      <el-form label-width="96px">
        <el-form-item label="按钮文案" required>
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="如：关注公众号" />
        </el-form-item>
        <el-form-item label="按钮图标">
          <div class="sl-icon-row">
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onIconPick">
              <el-button size="small">选择图标</el-button>
            </el-upload>
            <img v-if="isIconImage(form.icon)" :src="form.icon" class="sl-icon-preview" alt="图标预览" />
            <el-button v-if="form.icon" size="small" link type="danger" @click="form.icon = ''">清除</el-button>
            <span v-else class="sl-muted">未设置</span>
          </div>
          <div class="sl-hint">显示在按钮文字左侧的小图标，建议正方形、不超过 64×64</div>
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.url" placeholder="https://… 微博、企业官网等" />
        </el-form-item>
        <el-form-item label="跳转资料">
          <div class="sl-media">
            <el-upload :auto-upload="false" :show-file-list="false" multiple accept="image/*,.pdf" :on-change="onMediaPick">
              <el-button size="small" :loading="pending > 0">选择图片或 PDF</el-button>
            </el-upload>
            <div class="sl-hint">
              可多选，最多 {{ MAX_ITEMS }} 份；单个 PDF 不超过 {{ MAX_PDF_PAGES }} 页、合计不超过 {{ MAX_TOTAL_PAGES }} 页。
              消费者点开按钮后上下滑动查看。
            </div>
            <div v-for="(m, i) in media" :key="m.url" class="sl-media-row">
              <img v-if="!isPdf(m.url)" :src="m.url" class="sl-media-thumb" alt="资料缩略图" />
              <span v-else class="sl-media-badge">PDF</span>
              <span class="sl-media-name">{{ m.name || (isPdf(m.url) ? 'PDF 文件' : '图片') }}</span>
              <el-button size="small" link type="danger" @click="media.splice(i, 1)">删除</el-button>
            </div>
            <span v-if="!media.length" class="sl-muted">未选择</span>
          </div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import Sortable from 'sortablejs'
import { getSideLinks, createSideLink, updateSideLink, deleteSideLink, sortSideLinks } from '@/api/enterprise'
import { isIconImage } from '@/utils/constants'
import { uploadFile } from '@/api/common'

const loading = ref(false)
const saving = ref(false)
const links = ref<any[]>([])
const dialogVisible = ref(false)
const editing = ref<any>(null)
const form = reactive({ name: '', icon: '', url: '', image: '', status: 1 })
/** 弹窗里正在编辑的资料条目；保存时序列化成 form.image 的 JSON 数组 */
const media = ref<{ url: string; name: string }[]>([])
const pending = ref(0)
const MAX_ITEMS = 10
const MAX_PDF_PAGES = 30
const MAX_TOTAL_PAGES = 60
// 多选时 el-upload 逐个回调，串成一条队列：并发上传的完成顺序不可控，条目顺序就该等于选择顺序
let uploadChain: Promise<void> = Promise.resolve()
let sortable: Sortable | null = null

onMounted(async () => { await loadData(); initSortable() })

async function loadData() {
  loading.value = true
  try {
    const res: any = await getSideLinks()
    links.value = res.data || []
  } catch {
    // request 拦截器已处理错误提示
  } finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', icon: '', url: '', image: '', status: 1 })
  media.value = []
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = row
  Object.assign(form, {
    name: row.name || '', icon: row.icon || '', url: row.url || '', image: row.image || '', status: row.status ?? 1,
  })
  media.value = parseMedia(row.image)
  dialogVisible.value = true
}

async function onIconPick(file: any) {
  try {
    const res: any = await uploadFile(file.raw)
    if (!res.data?.url) return ElMessage.error('上传失败')
    form.icon = res.data.url
  } catch {
    // request 拦截器已处理错误提示
  }
}

function onMediaPick(file: any) {
  const raw: File | undefined = file?.raw
  if (!raw) return
  if (media.value.length + pending.value >= MAX_ITEMS) {
    return ElMessage.warning(`单个按钮最多 ${MAX_ITEMS} 份资料`)
  }
  if (!raw.type.startsWith('image/') && !isPdf(raw.name)) {
    return ElMessage.error(`只支持图片或 PDF：${raw.name}`)
  }
  pending.value++
  uploadChain = uploadChain.then(() => doUploadMedia(raw))
}

async function doUploadMedia(raw: File): Promise<void> {
  try {
    const res: any = await uploadFile(raw)
    if (!res.data?.url) {
      ElMessage.error(`${raw.name} 上传失败`)
      return
    }
    // 弹窗已关就别再往列表里塞：那会让下一次打开时凭空多出条目
    if (dialogVisible.value) media.value.push({ url: res.data.url, name: raw.name })
  } catch {
    // request 拦截器已处理错误提示；队列继续跑剩下的文件
  } finally { pending.value-- }
}

/** 库里的 image 是 JSON 数组；本次改动之前的老数据可能是单个裸路径，两种都要能编辑 */
function parseMedia(image: any): { url: string; name: string }[] {
  const raw = String(image ?? '').trim()
  if (!raw) return []
  if (!raw.startsWith('[')) return [{ url: raw, name: '' }]
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return []
    return arr.filter((u: any) => typeof u === 'string' && u).map((u: string) => ({ url: u, name: '' }))
  } catch {
    return []
  }
}

/** .PDF 也算：判定大小写不敏感，与后端 SideLinkService.isPdf 同口径 */
function isPdf(path: string): boolean {
  return /\.pdf$/i.test(path || '')
}

function mediaOf(row: any): string[] {
  return parseMedia(row?.image).map(m => m.url)
}

function imagesOf(row: any): string[] {
  return mediaOf(row).filter(u => !isPdf(u))
}

async function handleSave() {
  // 与后端 SideLinkService.validate 同口径：两处说法不同，用户就会看到"明明填了还被拒"
  if (!String(form.name || '').trim()) return ElMessage.warning('按钮名称不能为空')
  if (!String(form.url || '').trim() && media.value.length === 0) {
    return ElMessage.warning('跳转链接与跳转资料至少填一项')
  }
  if (pending.value > 0) return ElMessage.warning('资料还在上传，请稍候再保存')
  form.image = media.value.length ? JSON.stringify(media.value.map(m => m.url)) : ''
  saving.value = true
  try {
    if (editing.value?.id) await updateSideLink(editing.value.id, { ...form })
    else await createSideLink({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } catch {
    // request 拦截器已处理错误提示
  } finally { saving.value = false }
}

async function toggle(row: any) {
  const next = row.status === 1 ? 0 : 1
  try {
    await updateSideLink(row.id, { name: row.name, icon: row.icon, url: row.url, image: row.image, status: next })
    row.status = next
    ElMessage.success(next === 1 ? '已启用' : '已停用')
  } catch {
    // request 拦截器已处理错误提示；失败时不动 row.status，界面与库里保持一致
  }
}

async function remove(id: number) {
  try {
    await deleteSideLink(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // request 拦截器已处理错误提示
  }
}

function initSortable() {
  nextTick(() => {
    const el = document.querySelector('.el-table__body-wrapper tbody') as HTMLElement | null
    if (!el) return
    sortable?.destroy()
    sortable = new Sortable(el, {
      animation: 200, handle: '.sl-drag',
      onEnd: async (evt: any) => {
        if (evt.oldIndex === undefined || evt.newIndex === undefined) return
        const moved = links.value.splice(evt.oldIndex, 1)[0]
        links.value.splice(evt.newIndex, 0, moved)
        try {
          await sortSideLinks(links.value.map(l => l.id))
        } catch {
          // 排序没落库就得把顺序拉回服务端真值：否则用户眼前这个顺序刷新后会跳回去，
          // 而拦截器只弹一句错误文案，看不出"界面在骗你"
          await loadData()
        }
      },
    })
  })
}

onUnmounted(() => sortable?.destroy())
</script>

<style scoped lang="scss">
.sl-drag { cursor: grab; color: #a8abb2; }
.sl-link { color: var(--el-color-primary); word-break: break-all; }
.sl-muted { color: #909399; }
.sl-thumb { width: 40px; height: 40px; }
.sl-name-cell { display: inline-flex; align-items: center; gap: 6px; }
.sl-name-ico { width: 16px; height: 16px; object-fit: contain; }
.sl-icon-row { display: flex; align-items: center; gap: 10px; }
.sl-icon-preview { width: 22px; height: 22px; object-fit: contain; border: 1px solid var(--el-border-color); border-radius: 4px; }
.sl-hint { font-size: 12px; color: #909399; margin-top: 4px; line-height: 1.6; }
.sl-media { width: 100%; }
.sl-media-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.sl-media-thumb { width: 40px; height: 40px; object-fit: cover; border: 1px solid var(--el-border-color); border-radius: 4px; }
.sl-media-badge {
  width: 40px; height: 40px; flex: none; display: flex; align-items: center; justify-content: center;
  border-radius: 4px; background: #fef0f0; color: #f56c6c; font-size: 11px; font-weight: 600;
}
.sl-media-name {
  flex: 1; min-width: 0; font-size: 13px; color: #606266;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sl-media-count { font-size: 12px; color: #909399; }
</style>
