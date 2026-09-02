<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="搜索证号/产品/主体" clearable style="width:220px" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-if="isAdminMode" v-model="filterEnterpriseId" placeholder="全部企业" clearable filterable style="width:220px" @change="loadData">
            <el-option v-for="e in enterpriseOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button v-if="!isAdminMode" type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon> 新建合格证
          </el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="code" label="证号" width="180">
          <template #default="{ row }">
            <el-link type="primary" :href="row.qrUrl" target="_blank">{{ row.code }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="number" label="数量(重量)" width="100" show-overflow-tooltip />
        <el-table-column prop="placeOfOrigin" label="产地" min-width="130" show-overflow-tooltip />
        <el-table-column prop="promiseUser" label="承诺主体" min-width="140" show-overflow-tooltip />
        <el-table-column prop="batchName" label="关联批次" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.batchName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="useTime" label="开具日期" width="105" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '有效' : '已作废' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link @click="openQr(row)">二维码</el-button>
            <el-button size="small" type="primary" link @click="openPrint(row)">打印</el-button>
            <el-button v-if="!isAdminMode && row.status === 1" size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-popconfirm v-if="row.status === 1" title="作废后公开页面将显示作废标识，确认作废该合格证？" width="260" @confirm="handleVoid(row)">
              <template #reference><el-button size="small" type="danger" link>作废</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > pageSize">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="currentPage" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 新建/编辑合格证弹窗 -->
    <el-dialog v-model="showFormDialog" :title="editingId ? '编辑合格证' : '新建合格证'" width="780px" top="4vh" destroy-on-close :close-on-click-modal="false">
      <el-form label-width="100px" v-loading="formLoading">
        <el-form-item label="关联批次">
          <el-select v-model="form.batchId" placeholder="选择批次（自动带出产品/产地）" clearable filterable remote
            :remote-method="searchBatches" :loading="batchLoading" style="width:100%" @change="onBatchChange">
            <el-option v-for="b in batchOptions" :key="b.id" :label="b.name + (b.goodsName ? ' - ' + b.goodsName : '')" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体类型">
          <el-radio-group v-model="form.userType">
            <el-radio :value="1">生产者</el-radio>
            <el-radio :value="2">收购者</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="产品名称" required>
              <el-input v-model="form.productName" placeholder="产品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数量(重量)">
              <el-input v-model="form.number" placeholder="如：500公斤 / 2000只" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="产地">
              <el-input v-model="form.placeOfOrigin" placeholder="产地（省/市/县，可到基地）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="承诺主体" required>
              <el-input v-model="form.promiseUser" placeholder="开具单位名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="联系方式">
              <el-input v-model="form.contact" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开具日期">
              <el-date-picker v-model="form.useTime" type="date" value-format="YYYY-MM-DD" placeholder="开具日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="承诺事项" required>
          <div class="check-list">
            <el-checkbox v-for="(item, i) in form.promiseList" :key="'p' + i" v-model="item.isSelect">{{ item.title }}</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="承诺依据" required>
          <div class="check-list">
            <div v-for="(item, i) in form.basisList" :key="'b' + i" class="basis-item">
              <el-checkbox v-model="item.isSelect">{{ item.title }}</el-checkbox>
              <template v-if="item.isSelect">
                <el-upload :show-file-list="false" accept="image/*" :http-request="(opt: any) => uploadBasisImage(opt, i)">
                  <el-button size="small" :loading="item._uploading">{{ item.image ? '重新上传报告' : '上传检测报告图' }}</el-button>
                </el-upload>
                <el-image v-if="item.image" :src="item.image" fit="cover" class="basis-img" :preview-src-list="[item.image]" />
              </template>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="签名/盖章">
          <div class="sign-wrap">
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadSignature">
              <el-button size="small">{{ form.signature ? '重新上传' : '上传签名/盖章图' }}</el-button>
            </el-upload>
            <el-image v-if="form.signature" :src="form.signature" fit="contain" class="sign-img" :preview-src-list="[form.signature]" />
          </div>
        </el-form-item>
        <el-form-item label="展示企业信息">
          <el-switch v-model="form.isShowEnterprise" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后公开页展示企业名称/简介/形象图</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存并生成证号</el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="showQrDialog" title="合格证二维码" width="460px" destroy-on-close align-center :close-on-click-modal="false">
      <div class="qr-preview">
        <div v-loading="qrLoading" class="qr-img-wrap">
          <img v-if="qrData" :src="qrData" class="qr-img" />
        </div>
        <div v-if="qrUrl" class="qr-url">
          <span class="qr-url-label">合格证链接：</span>
          <el-link type="primary" :href="qrUrl" target="_blank" :underline="false" style="word-break:break-all">{{ qrUrl }}</el-link>
        </div>
        <p class="qr-hint">扫描二维码或点击链接即可查看合格证；点击"打印"可输出实体标签</p>
      </div>
      <template #footer>
        <el-button @click="copyUrl">复制链接</el-button>
        <el-button type="primary" @click="showQrDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getHgzList, getHgzDefaults, createHgz, getHgzDetail, updateHgz, voidHgz, getHgzQrcode, getBatches } from '@/api/enterprise'
import { getAllEnterprises } from '@/api/admin'
import { uploadFile } from '@/api/common'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdminMode = computed(() => route.path.startsWith('/admin'))

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const filterEnterpriseId = ref<number | null>(null)
const enterpriseOptions = ref<any[]>([])

const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (isAdminMode.value) params.enterpriseId = filterEnterpriseId.value || undefined
    const res = await getHgzList(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// ==================== 新建/编辑 ====================

const showFormDialog = ref(false)
const formLoading = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<any>({
  batchId: null,
  userType: 1,
  productName: '',
  number: '',
  placeOfOrigin: '',
  promiseUser: '',
  contact: '',
  useTime: '',
  signature: '',
  isShowEnterprise: 1,
  promiseList: [] as any[],
  basisList: [] as any[],
})

const batchOptions = ref<any[]>([])
const batchLoading = ref(false)

async function searchBatches(q: string) {
  batchLoading.value = true
  try {
    const res = await getBatches({ page: 1, size: 20, keyword: q })
    batchOptions.value = res.data?.list || []
  } finally {
    batchLoading.value = false
  }
}

function onBatchChange(batchId: number | null) {
  if (!batchId) return
  const b = batchOptions.value.find((x) => x.id === batchId)
  if (!b) return
  if (b.goodsName && !form.productName) form.productName = b.goodsName
  if (b.baseName && !form.placeOfOrigin) form.placeOfOrigin = b.baseName
}

async function loadDefaults() {
  const res = await getHgzDefaults()
  form.promiseList = (res.data?.promiseItems || []).map((x: any) => ({ ...x }))
  form.basisList = (res.data?.basisItems || []).map((x: any) => ({ ...x, image: '' }))
}

async function openCreate() {
  editingId.value = null
  Object.assign(form, {
    batchId: null, userType: 1, productName: '', number: '', placeOfOrigin: '',
    promiseUser: userStore.userInfo?.enterpriseName || '', contact: '',
    useTime: todayStr(), signature: '', isShowEnterprise: 1, promiseList: [], basisList: [],
  })
  formLoading.value = true
  try {
    await loadDefaults()
    showFormDialog.value = true
  } finally {
    formLoading.value = false
  }
}

async function openEdit(row: any) {
  editingId.value = row.id
  formLoading.value = true
  showFormDialog.value = true
  try {
    await loadDefaults()
    const res = await getHgzDetail(row.id)
    const d = res.data
    Object.assign(form, {
      batchId: d.batchId, userType: d.userType, productName: d.productName, number: d.number,
      placeOfOrigin: d.placeOfOrigin, promiseUser: d.promiseUser, contact: d.contact,
      useTime: d.useTime, signature: d.signature || '', isShowEnterprise: d.isShowEnterprise,
      promiseList: d.promiseItems || [], basisList: (d.basisItems || []).map((x: any) => ({ ...x, image: x.image || '' })),
    })
  } finally {
    formLoading.value = false
  }
}

async function uploadBasisImage(opt: any, idx: number) {
  form.basisList[idx]._uploading = true
  try {
    const res = await uploadFile(opt.file)
    form.basisList[idx].image = res.data?.url || ''
    ElMessage.success('检测报告已上传')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    form.basisList[idx]._uploading = false
  }
}

async function uploadSignature(opt: any) {
  const res = await uploadFile(opt.file)
  form.signature = res.data?.url || ''
  ElMessage.success('签名图已上传')
}

async function saveForm() {
  if (!form.productName?.trim()) return ElMessage.warning('请填写产品名称')
  if (!form.promiseUser?.trim()) return ElMessage.warning('请填写承诺主体')
  if (!form.promiseList.some((x: any) => x.isSelect)) return ElMessage.warning('请至少勾选一项承诺事项')
  if (!form.basisList.some((x: any) => x.isSelect)) return ElMessage.warning('请至少选择一项承诺依据')
  saving.value = true
  try {
    const payload = {
      batchId: form.batchId || null,
      userType: form.userType,
      productName: form.productName,
      number: form.number,
      placeOfOrigin: form.placeOfOrigin,
      promiseUser: form.promiseUser,
      contact: form.contact,
      useTime: form.useTime,
      signature: form.signature || null,
      isShowEnterprise: form.isShowEnterprise,
      promiseList: form.promiseList.map(({ title, isSelect }: any) => ({ title, isSelect })),
      basisList: form.basisList.map(({ title, isSelect, image }: any) => ({ title, isSelect, image: image || null })),
    }
    if (editingId.value) {
      await updateHgz(editingId.value, payload)
      ElMessage.success('合格证已更新')
    } else {
      await createHgz(payload)
      ElMessage.success('合格证开具成功')
    }
    showFormDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== 二维码 / 打印 / 作废 ====================

const showQrDialog = ref(false)
const qrLoading = ref(false)
const qrData = ref('')
const qrUrl = ref('')

async function openQr(row: any) {
  showQrDialog.value = true
  qrLoading.value = true
  qrData.value = ''
  qrUrl.value = row.qrUrl || ''
  try {
    const res = await getHgzQrcode(row.id)
    qrData.value = res.data
  } finally {
    qrLoading.value = false
  }
}

function copyUrl() {
  if (!qrUrl.value) return
  navigator.clipboard?.writeText(qrUrl.value).then(() => ElMessage.success('链接已复制'))
}

function openPrint(row: any) {
  router.push(`/hgz-print/${row.id}`)
}

async function handleVoid(row: any) {
  try {
    await voidHgz(row.id)
    ElMessage.success('合格证已作废')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '作废失败')
  }
}

onMounted(async () => {
  if (isAdminMode.value) {
    try {
      const res = await getAllEnterprises()
      enterpriseOptions.value = res.data || []
    } catch {}
  }
  loadData()
})
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;

  .basis-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .basis-img {
      width: 56px;
      height: 56px;
      border-radius: 6px;
      border: 1px solid #e5e7eb;
    }
  }
}

.sign-wrap {
  display: flex;
  align-items: center;
  gap: 12px;

  .sign-img {
    width: 90px;
    height: 60px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background: #fff;
  }
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.qr-preview {
  text-align: center;

  .qr-img-wrap {
    min-height: 220px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .qr-img {
    width: 220px;
    height: 220px;
  }

  .qr-url {
    margin-top: 12px;
    text-align: left;
    display: flex;
    align-items: flex-start;
    gap: 4px;
  }

  .qr-url-label {
    color: #606266;
    font-size: 13px;
    flex-shrink: 0;
  }

  .qr-hint {
    margin-top: 8px;
    color: #909399;
    font-size: 12px;
  }
}
</style>
