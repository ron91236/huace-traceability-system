<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="搜索海报标题" clearable style="width:220px" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="primary" @click="showUploadDialog = true">
            <el-icon><Upload /></el-icon> 上传海报
          </el-button>
        </div>
      </template>

      <el-table :data="posters" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="海报标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="slug" label="访问标识" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.slug }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="原始文件" min-width="140" show-overflow-tooltip />
        <el-table-column prop="posterUrl" label="访问链接" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :href="row.posterUrl" target="_blank" size="small">{{ row.posterUrl }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v: boolean) => handleStatusChange(row, v)"
              active-text="启用" inactive-text="禁用" inline-prompt />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link @click="handlePreviewQr(row)">
              <el-icon><Iphone /></el-icon> 二维码
            </el-button>
            <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除此海报？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="currentPage" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 上传海报弹窗 -->
    <el-dialog v-model="showUploadDialog" title="上传海报" width="500px" destroy-on-close :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="海报标题">
          <el-input v-model="uploadTitle" placeholder="可选，默认使用文件名" />
        </el-form-item>
        <el-form-item label="HTML文件" required>
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" accept=".html,.htm" :on-change="onFileChange" :on-exceed="() => ElMessage.warning('只能上传一个文件')">
            <el-button type="primary"><el-icon><Upload /></el-icon> 选择HTML文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .html 文件，上传后自动生成二维码</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑海报弹窗：可替换HTML内容，访问链接与二维码保持不变 -->
    <el-dialog v-model="showEditDialog" title="编辑海报" width="920px" top="4vh" destroy-on-close :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="海报标题">
          <el-input v-model="editTitle" placeholder="海报标题" />
        </el-form-item>
        <el-form-item label="HTML内容">
          <div style="width:100%" v-loading="htmlLoading">
            <div class="html-editor-toolbar">
              <el-upload :show-file-list="false" accept=".html,.htm" :auto-upload="false" :on-change="onImportHtml">
                <el-button size="small"><el-icon><FolderOpened /></el-icon> 导入HTML文件</el-button>
              </el-upload>
              <el-upload :show-file-list="false" accept=".jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.mp4,.mov,.webm" :auto-upload="false" :on-change="onResourceSelect">
                <el-button size="small" type="primary" :loading="resUploading"><el-icon><Upload /></el-icon> 上传图片/PDF/视频</el-button>
              </el-upload>
              <span class="editor-tip">上传后点"插入"将资源标签写入光标处；保存后链接与二维码不变</span>
            </div>
            <el-input ref="htmlTextareaRef" v-model="editHtml" type="textarea" :rows="16" class="html-editor" placeholder="在此编辑HTML内容，或导入新的HTML文件替换" />
            <div v-if="resources.length" class="resource-list">
              <div v-for="(r, i) in resources" :key="i" class="resource-item">
                <el-tag size="small" :type="r.kind === 'image' ? 'success' : r.kind === 'video' ? 'warning' : 'info'">{{ r.kindText }}</el-tag>
                <span class="resource-name" :title="r.name">{{ r.name }}</span>
                <el-input :model-value="r.url" readonly size="small" class="resource-url" />
                <el-button size="small" type="primary" link @click="insertResource(r)">插入到光标处</el-button>
                <el-button size="small" type="info" link @click="copyText(r.url)">复制链接</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 二维码预览弹窗 -->
    <el-dialog v-model="showQrDialog" title="海报二维码" width="460px" destroy-on-close align-center :close-on-click-modal="false">
      <div class="qr-preview">
        <div v-loading="qrLoading" class="qr-img-wrap">
          <img v-if="qrData" :src="qrData" class="qr-img" />
        </div>
        <div v-if="qrUrl" class="qr-url">
          <span class="qr-url-label">访问链接：</span>
          <el-link type="primary" :href="qrUrl" target="_blank" :underline="false" style="word-break:break-all">{{ qrUrl }}</el-link>
        </div>
        <p class="qr-hint">扫描二维码或点击链接即可访问海报页面</p>
      </div>
      <template #footer>
        <el-button @click="showQrDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleOpenPoster">打开海报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Refresh, Upload, Iphone, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPosters, uploadPoster, updatePoster, deletePoster, getPosterQrcode, getPosterHtml, updatePosterHtml } from '@/api/admin'
import { uploadFile } from '@/api/common'

const loading = ref(false)
const posters = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')

// 上传
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadTitle = ref('')
const selectedFile = ref<File | null>(null)
const uploadRef = ref<any>(null)

// 二维码
const showQrDialog = ref(false)
const qrLoading = ref(false)
const qrData = ref('')
const qrUrl = ref('')

// 编辑（弹窗内替换HTML内容，slug/链接/二维码保持不变）
const showEditDialog = ref(false)
const editId = ref<number | null>(null)
const editTitle = ref('')
const editHtml = ref('')
const htmlLoading = ref(false)
const saving = ref(false)
const htmlTextareaRef = ref<any>(null)

// 资源（图片/PDF/视频）
interface ResourceItem { name: string; url: string; kind: 'image' | 'video' | 'pdf'; kindText: string }
const resources = ref<ResourceItem[]>([])
const resUploading = ref(false)

function formatDate(d: string) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 19)
}

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getPosters({ page: currentPage.value, size: pageSize.value, keyword: keyword.value || undefined })
    posters.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function onFileChange(file: any) {
  selectedFile.value = file.raw || null
}

async function handleUpload() {
  if (!selectedFile.value) return ElMessage.warning('请选择HTML文件')
  uploading.value = true
  try {
    await uploadPoster(selectedFile.value, uploadTitle.value || undefined)
    ElMessage.success('海报上传成功')
    showUploadDialog.value = false
    uploadTitle.value = ''
    selectedFile.value = null
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '上传失败')
  } finally { uploading.value = false }
}

async function handlePreviewQr(row: any) {
  showQrDialog.value = true
  qrLoading.value = true
  qrData.value = ''
  qrUrl.value = ''
  try {
    const res = await getPosterQrcode(row.id)
    qrData.value = res.data?.qrCode || ''
    qrUrl.value = res.data?.posterUrl || ''
  } catch (e: any) {
    ElMessage.error('获取二维码失败')
  } finally { qrLoading.value = false }
}

function handleOpenPoster() {
  if (qrUrl.value) window.open(qrUrl.value, '_blank')
}

async function openEdit(row: any) {
  editId.value = row.id
  editTitle.value = row.title
  editHtml.value = ''
  resources.value = []
  showEditDialog.value = true
  htmlLoading.value = true
  try {
    const res = await getPosterHtml(row.id)
    editHtml.value = res.data?.content || ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '读取HTML内容失败')
  } finally { htmlLoading.value = false }
}

/** 导入本地HTML文件整体替换内容 */
function onImportHtml(file: any) {
  const raw = file.raw as File
  if (!raw) return
  if (!/\.html?$/i.test(raw.name)) return ElMessage.warning('仅支持导入HTML文件')
  const reader = new FileReader()
  reader.onload = () => {
    editHtml.value = String(reader.result || '')
    ElMessage.success('已导入文件内容，点击保存后生效')
  }
  reader.readAsText(raw)
}

/** 上传资源文件（图片/PDF/视频），上传成功后可插入HTML */
async function onResourceSelect(file: any) {
  const raw = file.raw as File
  if (!raw) return
  const ext = (raw.name.split('.').pop() || '').toLowerCase()
  let kind: ResourceItem['kind']
  let kindText: string
  if (/^(jpg|jpeg|png|gif|bmp|webp)$/.test(ext)) { kind = 'image'; kindText = '图片' }
  else if (/^(mp4|mov|webm)$/.test(ext)) { kind = 'video'; kindText = '视频' }
  else if (ext === 'pdf') { kind = 'pdf'; kindText = 'PDF' }
  else return ElMessage.warning('仅支持图片、PDF、视频格式')
  resUploading.value = true
  try {
    const res = await uploadFile(raw)
    resources.value.push({ name: raw.name, url: res.data?.url || '', kind, kindText })
    ElMessage.success(`${kindText}上传成功，可点击"插入到光标处"写入HTML`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '上传失败')
  } finally { resUploading.value = false }
}

function buildResourceTag(r: ResourceItem) {
  if (r.kind === 'image') return `<img src="${r.url}" alt="${r.name}" style="max-width:100%" />`
  if (r.kind === 'video') return `<video src="${r.url}" controls style="max-width:100%"></video>`
  return `<iframe src="${r.url}" style="width:100%;height:600px;border:none"></iframe>`
}

/** 将资源标签插入到编辑器光标处 */
function insertResource(r: ResourceItem) {
  const tag = buildResourceTag(r)
  const textarea = getTextareaEl()
  if (textarea) {
    const start = textarea.selectionStart ?? editHtml.value.length
    const end = textarea.selectionEnd ?? start
    editHtml.value = editHtml.value.slice(0, start) + tag + editHtml.value.slice(end)
    requestAnimationFrame(() => {
      textarea.focus()
      const pos = start + tag.length
      textarea.setSelectionRange(pos, pos)
    })
  } else {
    editHtml.value += tag
  }
  ElMessage.success('已插入，保存后生效')
}

function getTextareaEl(): HTMLTextAreaElement | null {
  const inst = htmlTextareaRef.value
  return inst?.textarea || inst?.$el?.querySelector?.('textarea') || null
}

function copyText(text: string) {
  navigator.clipboard?.writeText(text).then(
    () => ElMessage.success('链接已复制'),
    () => ElMessage.warning('复制失败，请手动复制')
  )
}

async function saveEdit() {
  if (!editId.value) return
  if (!editHtml.value.trim()) return ElMessage.warning('HTML内容不能为空')
  saving.value = true
  try {
    await updatePosterHtml(editId.value, editHtml.value)
    if (editTitle.value) await updatePoster(editId.value, { title: editTitle.value })
    ElMessage.success('保存成功，访问链接与二维码保持不变')
    showEditDialog.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

async function handleStatusChange(row: any, val: boolean) {
  try {
    await updatePoster(row.id, { status: val ? 1 : 0 })
    ElMessage.success(val ? '已启用' : '已禁用')
    await loadData()
  } catch (e: any) { ElMessage.error('操作失败') }
}

async function handleDelete(id: number) {
  try {
    await deletePoster(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '删除失败') }
}
</script>

<style scoped lang="scss">
.page-container { padding: 0; }
.table-toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

.qr-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.qr-img-wrap {
  width: 300px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}
.qr-img {
  width: 280px;
  height: 280px;
  object-fit: contain;
}
.qr-url {
  max-width: 380px;
  text-align: center;
  font-size: 13px;
  color: #606266;
  .qr-url-label { color: #909399; }
}
.qr-hint {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.html-editor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  .editor-tip { font-size: 12px; color: #909399; }
}
.html-editor :deep(.el-textarea__inner) {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}
.resource-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  .resource-item {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #f5f7fa;
    border-radius: 6px;
    padding: 6px 10px;
    .resource-name {
      font-size: 12px;
      color: #606266;
      max-width: 160px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .resource-url { flex: 1; :deep(.el-input__inner) { font-size: 12px; } }
  }
}
</style>
