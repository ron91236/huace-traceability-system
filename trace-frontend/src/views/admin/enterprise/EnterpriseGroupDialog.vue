<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    :title="`子企业管理 - ${parentEnterprise?.name || ''}`" width="720px" top="5vh" destroy-on-close>
    <div style="margin-bottom:12px;display:flex;justify-content:flex-end">
      <el-button type="primary" size="small" @click="showAddForm = true">添加子企业</el-button>
    </div>
    <el-table :data="children" v-loading="loading" border stripe size="small">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="企业名称" min-width="140" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="联系电话" width="120" />
      <el-table-column prop="loginAccount" label="登录账号" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-popconfirm title="确认移除该子企业?" @confirm="handleRemove(row.id)">
            <template #reference><el-button type="danger" link size="small">移除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty><el-empty description="暂无子企业" :image-size="60" /></template>
    </el-table>

    <!-- 添加子企业表单 -->
    <el-dialog v-model="showAddForm" title="添加子企业" width="520px" append-to-body :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="企业名称" prop="name"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="联系人" prop="contact"><el-input v-model="addForm.contact" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="addForm.phone" /></el-form-item>
        <el-form-item label="登录账号" prop="loginAccount"><el-input v-model="addForm.loginAccount" /></el-form-item>
        <el-form-item label="登录密码" prop="loginPassword"><el-input v-model="addForm.loginPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddForm = false">取消</el-button>
        <el-button type="primary" :loading="addSubmitting" @click="handleAddChild">确定</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getChildEnterprises, createChildEnterprise, removeChildEnterprise } from '@/api/admin'

const props = defineProps<{ modelValue: boolean; parentEnterprise: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; saved: [] }>()

const loading = ref(false)
const addSubmitting = ref(false)
const showAddForm = ref(false)
const children = ref<any[]>([])
const addFormRef = ref()

const addForm = reactive({
  name: '', contact: '', phone: '', loginAccount: '', loginPassword: '',
})

const addRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  loginAccount: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  loginPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

watch(() => props.modelValue, async (val) => {
  if (val && props.parentEnterprise?.id) {
    await loadChildren()
  }
})

async function loadChildren() {
  loading.value = true
  try {
    const res = await getChildEnterprises(props.parentEnterprise.id)
    children.value = res.data || []
  } finally { loading.value = false }
}

async function handleAddChild() {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  addSubmitting.value = true
  try {
    await createChildEnterprise(props.parentEnterprise.id, addForm)
    ElMessage.success('子企业添加成功')
    showAddForm.value = false
    Object.keys(addForm).forEach(k => { (addForm as any)[k] = '' })
    loadChildren()
    emit('saved')
  } finally { addSubmitting.value = false }
}

async function handleRemove(childId: number) {
  try {
    await removeChildEnterprise(props.parentEnterprise.id, childId)
    ElMessage.success('已移除')
    loadChildren()
    emit('saved')
  } catch (e) {
    ElMessage.error('移除失败')
  }
}
</script>
