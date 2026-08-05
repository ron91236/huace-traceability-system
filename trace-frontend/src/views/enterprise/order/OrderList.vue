<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div style="display:flex;gap:8px">
            <el-input v-model="search.orderNo" placeholder="订单编号" clearable style="width:140px" />
            <el-select v-model="search.status" placeholder="状态" clearable style="width:120px">
              <el-option label="草稿" value="DRAFT" /><el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
          <el-button type="primary" @click="openForm()">新增订单</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="订单编号" width="160" />
        <el-table-column prop="certName" label="关联证书" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/enterprise/order/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="success" @click="handleSubmit(row)">提交审核</el-button>
            <el-button v-if="row.status === 'DRAFT'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增订单" width="480px" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px">
        <el-form-item label="关联证书"><el-select v-model="form.certId" filterable style="width:100%"><el-option v-for="c in certs" :key="c.id" :label="c.certName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="收货地址"><el-select v-model="form.addressId" filterable style="width:100%"><el-option v-for="a in addresses" :key="a.id" :label="`${a.contact} - ${a.address}`" :value="a.id" /></el-select></el-form-item>
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
import { getOrders, createOrder, deleteOrder, submitOrder, getEnterpriseCerts, getAddresses } from '@/api/enterprise'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = reactive({ orderNo: '', status: '' })
const dialogVisible = ref(false)
const certs = ref<any[]>([])
const addresses = ref<any[]>([])
const form = reactive({ certId: null as number | null, addressId: null as number | null })

onMounted(async () => {
  const [cRes, aRes] = await Promise.all([getEnterpriseCerts({ page: 1, size: 200 }), getAddresses({ page: 1, size: 100 })])
  certs.value = cRes.data?.list || []; addresses.value = aRes.data?.list || []; loadData()
})

async function loadData() { loading.value = true; try { const res = await getOrders({ page: page.value, size: size.value, ...search }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
function openForm() { form.certId = null; form.addressId = null; dialogVisible.value = true }
async function handleCreate() { await createOrder(form); ElMessage.success('新增成功'); dialogVisible.value = false; loadData() }
async function handleSubmit(row: any) { await submitOrder(row.id); ElMessage.success('已提交审核'); loadData() }
async function handleDelete(row: any) { await deleteOrder(row.id); ElMessage.success('删除成功'); loadData() }
</script>
