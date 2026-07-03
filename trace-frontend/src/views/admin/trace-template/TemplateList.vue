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
        <el-table-column prop="createdAt" label="创建时间" width="170" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTraceTemplates, updateTraceTemplate, deleteTraceTemplate } from '@/api/admin'
import TemplateEditor from './TemplateEditor.vue'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const templates = ref<any[]>([])

const showEditor = ref(false)
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

function handleCreate() {
  editingTemplate.value = null
  showEditor.value = true
}

function handleEdit(row: any) {
  editingTemplate.value = { ...row }
  showEditor.value = true
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
  loadData()
}

onMounted(() => loadData())
</script>
