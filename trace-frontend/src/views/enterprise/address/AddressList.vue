<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>收货地址管理</span>
          <el-button type="primary" @click="openForm()">新增地址</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="contact" label="联系人" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="address" label="收货地址" />
        <el-table-column prop="zipcode" label="邮编" width="80" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑地址' : '新增地址'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="收货地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="邮编"><el-input v-model="form.zipcode" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAddresses, createAddress, updateAddress, deleteAddress } from '@/api/enterprise'

const loading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = reactive<Record<string, any>>({ contact: '', phone: '', address: '', zipcode: '' })

onMounted(() => loadData())
async function loadData() { loading.value = true; try { const res = await getAddresses({ page: 1, size: 100 }); list.value = res.data?.list || [] } finally { loading.value = false } }
function openForm(row?: any) { editId.value = row?.id || null; Object.keys(form).forEach(k => { form[k] = row?.[k] || '' }); dialogVisible.value = true }
async function handleSubmit() { if (editId.value) await updateAddress(editId.value, form); else await createAddress(form); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() }
async function handleDelete(id: number) { await deleteAddress(id); ElMessage.success('删除成功'); loadData() }
</script>
