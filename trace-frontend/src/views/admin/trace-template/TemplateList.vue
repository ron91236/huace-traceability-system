<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-size:16px;font-weight:600">溯源模板管理</span>
          <div style="display:flex;gap:12px">
            <el-input v-model="keyword" placeholder="搜索模板名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>新增模板
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="templates" border stripe v-loading="loading" size="default">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="templateName" label="模板名称" min-width="140" />
        <el-table-column prop="templateKey" label="模板标识" width="160" />
        <el-table-column prop="templateType" label="模板类型" width="100">
          <template #default="{ row }">{{ row.templateType || '-' }}</template>
        </el-table-column>
        <el-table-column label="渲染方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="renderModeOf(row) === 'long-image'" type="warning" size="small">长图</el-tag>
            <el-tag v-else-if="renderModeOf(row) === 'landing'" type="success" size="small">落地页</el-tag>
            <el-tag v-else size="small" type="info">可视化</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主题风格" width="140">
          <template #default="{ row }">
            <el-tag v-if="getThemeKey(row)" :color="getThemeColor(getThemeKey(row))" style="color:#fff;border:none">
              {{ getThemeLabel(getThemeKey(row)) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="handleToggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该模板？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" link size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无模板" :image-size="60" /></template>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="loadData" />
      </div>
    </el-card>

    <TemplateEditor v-model="showEditor" :template-data="editingTemplate" @saved="onSaved" />
    <LongModuleDesigner v-model="showDesigner" :template-data="editingTemplate" @saved="onSaved" />
    <LandingBlockDesigner v-model="showBlockDesigner" :template-data="editingTemplate" @saved="onSaved" />

    <el-dialog v-model="showTypeChooser" title="选择模板类型" width="640px" :close-on-click-modal="false">
      <div class="type-grid">
        <div class="type-card" @click="startCreate('visual')">
          <div class="type-name">可视化模板</div>
          <div class="type-desc">拖拽排版要素，逐屏展示溯源信息</div>
        </div>
        <div class="type-card" @click="startCreate('long-image')">
          <div class="type-name">长图模板</div>
          <div class="type-desc">只定模块名称与顺序，整页图由企业上传</div>
        </div>
        <div class="type-card" @click="startCreate('landing')">
          <div class="type-name">落地页模板</div>
          <div class="type-desc">扫码先进静态页，右侧按钮栏跳详情或企业外链</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTraceTemplates, updateTraceTemplate, deleteTraceTemplate } from '@/api/admin'
import TemplateEditor from './TemplateEditor.vue'
import LongModuleDesigner from './LongModuleDesigner.vue'
import LandingBlockDesigner from './LandingBlockDesigner.vue'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const templates = ref<any[]>([])

const showEditor = ref(false)
const showDesigner = ref(false)
const showBlockDesigner = ref(false)
const showTypeChooser = ref(false)
const editingTemplate = ref<any>(null)

const THEME_MAP: Record<string, { label: string; color: string }> = {
  'standard-green': { label: '标准绿', color: '#059669' },
  'tech-blue': { label: '科技蓝', color: '#1e40af' },
  'premium-gold': { label: '品质金', color: '#b45309' },
}

function getThemeKey(row: any): string {
  try {
    const config = typeof row.configJson === 'string' ? JSON.parse(row.configJson) : row.configJson
    return config?.theme?.key || ''
  } catch { return '' }
}

function getThemeLabel(key: string) { return THEME_MAP[key]?.label || key }
function getThemeColor(key: string) { return THEME_MAP[key]?.color || '#999' }

async function loadData() {
  loading.value = true
  try {
    const res = await getTraceTemplates({ page: page.value, size: pageSize.value, keyword: keyword.value })
    templates.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    // request 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}

/** 渲染模式埋在 config_json 里，列表页要它来决定点"编辑"时打开哪个编辑器 */
function renderModeOf(row: any): string {
  try {
    const config = typeof row.configJson === 'string' ? JSON.parse(row.configJson) : row.configJson
    return config?.renderMode || ''
  } catch { return '' }
}

function handleCreate() {
  showTypeChooser.value = true
}

/** 三种模板类型共用同一个"基本信息"入口，编辑器各自独立，避免继续把 1389 行的 TemplateEditor 撑大 */
function modeToDialog(mode: string) {
  if (mode === 'long-image') showDesigner.value = true
  else if (mode === 'landing') showBlockDesigner.value = true
  else showEditor.value = true
}

function startCreate(mode: string) {
  showTypeChooser.value = false
  editingTemplate.value = null
  modeToDialog(mode)
}

function handleEdit(row: any) {
  editingTemplate.value = { ...row }
  modeToDialog(renderModeOf(row))
}

async function handleDelete(id: number) {
  try {
    await deleteTraceTemplate(id)
    ElMessage.success('已删除')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

async function handleToggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateTraceTemplate(row.id, { status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch (e: any) {
    ElMessage.error('操作失败')
  }
}

function onSaved() {
  showEditor.value = false
  showDesigner.value = false
  showBlockDesigner.value = false
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.type-grid { display: flex; gap: 12px; }
.type-card {
  flex: 1; padding: 18px 16px; border: 1px solid var(--el-border-color);
  border-radius: 10px; cursor: pointer; transition: all .15s;
}
.type-card:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.type-name { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.type-desc { font-size: 12px; color: #909399; line-height: 1.5; }
</style>
