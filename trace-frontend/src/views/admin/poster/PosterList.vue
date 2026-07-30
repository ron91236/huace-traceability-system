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
        <el-table-column prop="title" label="海报标题" min-width="160">
          <template #default="{ row }">
            <el-input v-if="editingId === row.id" v-model="editTitle" size="small" style="width:180px" />
            <span v-else>{{ row.title }}</span>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link @click="handlePreviewQr(row)">
              <el-icon><Iphone /></el-icon> 二维码
            </el-button>
            <el-button v-if="editingId !== row.id" size="small" type="primary" link @click="startEdit(row)">编辑</el-button>
            <template v-if="editingId === row.id">
              <el-button size="small" type="success" link @click="saveEdit(row)">保存</el-button>
              <el-button size="small" type="info" link @click="editingId = null">取消</el-button>
            </template>
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
    <el-dialog v-model="showUploadDialog" title="上传海报" width="500px" destroy-on-close>
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

    <!-- 二维码预览弹窗 -->
    <el-dialog v-model="showQrDialog" title="海报二维码" width="460px" destroy-on-close align-center>
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
import { Search, Refresh, Upload, Iphone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPosters, uploadPoster, updatePoster, deletePoster, getPosterQrcode } from '@/api/admin'

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

// 编辑
const editingId = ref<number | null>(null)
const editTitle = ref('')

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

function startEdit(row: any) {
  editingId.value = row.id
  editTitle.value = row.title
}

async function saveEdit(row: any) {
  try {
    await updatePoster(row.id, { title: editTitle.value })
    ElMessage.success('保存成功')
    editingId.value = null
    await loadData()
  } catch (e: any) { ElMessage.error('保存失败') }
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
</style>
