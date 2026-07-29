<template>
  <div class="page-container">
    <el-card>
      <template #header><span>订单条码管理</span></template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="specName" label="标签规格" width="120" />
        <el-table-column prop="productName" label="产品名称" />
        <el-table-column prop="traceTemplate" label="溯源模板" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="退订" width="80">
          <template #default="{ row }"><el-tag :type="row.isUnsubscribed ? 'danger' : 'info'" size="small">{{ row.isUnsubscribed ? '是' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="preview(row)">预览码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
    <el-dialog v-model="editVisible" title="编辑订单条码" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="溯源模板"><el-select v-model="form.traceTemplate" style="width:100%"><el-option v-for="t in templates" :key="t.templateKey" :label="t.templateName" :value="t.templateKey" /></el-select></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="previewVisible" title="预览溯源二维码" width="460px" align-center>
      <div style="text-align:center">
        <img v-if="previewUrl" :src="previewUrl" style="width:300px;height:300px" />
        <p v-if="traceUrl" style="margin-top:16px;font-size:13px;color:#666;word-break:break-all">溯源链接：{{ traceUrl }}</p>
        <p v-if="serialNoText" style="font-size:12px;color:#999;margin-top:4px">流水号：{{ serialNoText }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderCodes, updateOrderCode, previewOrderCode } from '@/api/enterprise'
import { getTraceTemplateOptions } from '@/api/common'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const editVisible = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const traceUrl = ref('')
const serialNoText = ref('')
const editId = ref(0)
const templates = ref<any[]>([])
const form = reactive({ traceTemplate: '', quantity: 1 })

onMounted(async () => { const res = await getTraceTemplateOptions(); templates.value = res.data || []; loadData() })
async function loadData() { loading.value = true; try { const res = await getOrderCodes({ page: page.value, size: size.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
function openEdit(row: any) { editId.value = row.id; form.traceTemplate = row.traceTemplate || ''; form.quantity = row.quantity || 1; editVisible.value = true }
async function handleSave() { await updateOrderCode(editId.value, form); ElMessage.success('保存成功'); editVisible.value = false; loadData() }
async function preview(row: any) {
  try {
    const res = await previewOrderCode(row.id)
    const d = res.data || {}
    previewUrl.value = d.qrcode || d.qrUrl || (typeof res.data === 'string' ? res.data : '')
    traceUrl.value = d.traceUrl || ''
    serialNoText.value = d.serialNo || ''
    previewVisible.value = true
  } catch (e) { ElMessage.error('获取预览码失败') }
}
</script>
