<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <span>条码使用记录</span>
          <el-button type="primary" @click="dialogVisible = true">新增使用记录</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="orderCodeId" label="订单条码ID" width="120" />
        <el-table-column prop="startSerial" label="开始流水号" width="140" />
        <el-table-column prop="endSerial" label="结束流水号" width="140" />
        <el-table-column prop="produceTime" label="生产时间" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增条码使用记录" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="订单条码"><el-select v-model="form.orderCodeId" filterable style="width:100%"><el-option v-for="o in orderCodes" :key="o.id" :label="`${o.orderNo} - ${o.productName}`" :value="o.id" /></el-select></el-form-item>
        <el-form-item label="开始流水号"><el-input v-model="form.startSerial" /></el-form-item>
        <el-form-item label="结束流水号"><el-input v-model="form.endSerial" /></el-form-item>
        <el-form-item label="生产时间"><el-date-picker v-model="form.produceTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCodeUsages, createCodeUsage, deleteCodeUsage, getOrderCodes } from '@/api/enterprise'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialogVisible = ref(false)
const orderCodes = ref<any[]>([])
const form = reactive({ orderCodeId: null as number | null, startSerial: '', endSerial: '', produceTime: '' })

onMounted(async () => { const res = await getOrderCodes({ page: 1, size: 200 }); orderCodes.value = res.data?.list || []; loadData() })
async function loadData() { loading.value = true; try { const res = await getCodeUsages({ page: page.value, size: size.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
async function handleCreate() { await createCodeUsage(form); ElMessage.success('新增成功'); dialogVisible.value = false; loadData() }
async function handleDelete(id: number) { await deleteCodeUsage(id); ElMessage.success('删除成功'); loadData() }
</script>
