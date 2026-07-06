<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="摄像头名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button type="success" @click="openAdd">新增视频源</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="cameraName" label="摄像头名称" min-width="140" />
        <el-table-column prop="streamType" label="流类型" width="80">
          <template #default="{ row }"><el-tag size="small">{{ row.streamType || 'HLS' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="streamUrl" label="流地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑视频源' : '新增视频源'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="摄像头名称"><el-input v-model="form.cameraName" /></el-form-item>
        <el-form-item label="流地址"><el-input v-model="form.streamUrl" placeholder="https://example.com/stream.m3u8" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="流类型">
              <el-select v-model="form.streamType" style="width:100%">
                <el-option label="HLS" value="HLS" /><el-option label="FLV" value="FLV" /><el-option label="RTMP" value="RTMP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="封面图"><el-input v-model="form.coverImage" placeholder="图片URL" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="999" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="关联基地ID"><el-input-number v-model="form.baseId" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="关联批次ID"><el-input-number v-model="form.batchId" :min="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVideoSources, createVideoSource, updateVideoSource, deleteVideoSource } from '@/api/enterprise'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive<any>({ cameraName: '', streamUrl: '', streamType: 'HLS', status: 1, coverImage: '', sortOrder: 0, baseId: null, batchId: null })
const editId = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try { const res = await getVideoSources({ keyword: keyword.value, page: 1, size: 100 }); list.value = res.data?.list || res.data || [] } finally { loading.value = false }
}

function openAdd() {
  editId.value = 0
  Object.assign(form, { cameraName: '', streamUrl: '', streamType: 'HLS', status: 1, coverImage: '', sortOrder: 0, baseId: null, batchId: null })
  dialogVisible.value = true
}

function openEdit(row: any) {
  editId.value = row.id
  Object.keys(form).forEach(k => { form[k] = row[k] ?? form[k] })
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editId.value) await updateVideoSource(editId.value, form)
    else await createVideoSource(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该视频源？', '提示', { type: 'warning' })
  await deleteVideoSource(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
