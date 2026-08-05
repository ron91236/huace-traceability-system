<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="基地名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="基地名称" min-width="140" />
        <el-table-column prop="code" label="基地编号" width="100" />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column label="面积" width="100"><template #default="{ row }">{{ row.area }}{{ row.unit }}</template></el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button size="small" @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑基地" width="620px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="基地名称"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.manager" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="认证情况"><el-input v-model="form.certification" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBases, updateBase } from '@/api/enterprise'

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive<any>({})
const editId = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try { const res = await getBases({ keyword: keyword.value, page: 1, size: 100 }); list.value = res.data?.list || [] } finally { loading.value = false }
}

function openEdit(row: any) { editId.value = row.id; Object.keys(form).forEach(k => { form[k] = row[k] }); dialogVisible.value = true }

async function handleSave() {
  await updateBase(editId.value, form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}
</script>
