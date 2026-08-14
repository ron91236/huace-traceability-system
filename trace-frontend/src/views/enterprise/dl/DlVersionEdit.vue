<template>
  <div class="page-container dl-version-edit" :class="{ readonly: isAdmin }" v-loading="loading">
    <!-- 管理员只读提示 -->
    <el-alert v-if="isAdmin" title="管理员只读模式：仅可查看标签内容，不可修改" type="warning" :closable="false" show-icon style="margin-bottom:16px" />
    <!-- 基础信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">基础信息</span></template>
      <el-form label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="食品名称">
              <el-input v-model="form.foodName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品条码">
              <el-input v-model="form.barcode" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="配料表">
          <el-input v-model="form.ingredients" type="textarea" :rows="3" placeholder="请输入配料表" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.spec" placeholder="如：袋装" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="净含量">
              <el-input v-model="form.netContent" placeholder="如：500g" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="食品图片">
          <el-upload list-type="picture-card" :file-list="foodImageList" :http-request="handleFoodImageUpload"
            :on-remove="handleFoodImageRemove" :on-preview="handlePreview" accept=".jpg,.jpeg,.png">
            <template v-if="foodImageList.length < 5"><el-icon><Plus /></el-icon></template>
          </el-upload>
          <div class="form-tip">最多5张，jpg/png格式，建议800x800以上，单张不超过5M</div>
        </el-form-item>
        <el-form-item label="营养成分表">
          <el-upload list-type="picture-card" :file-list="nutritionImageList" :http-request="handleNutritionUpload"
            :on-remove="() => form.nutritionImage = ''" :on-preview="handlePreview" accept=".jpg,.jpeg,.png" :limit="1">
            <template v-if="!form.nutritionImage"><el-icon><Plus /></el-icon></template>
          </el-upload>
          <div class="form-tip">上传营养成分表图片（选填）</div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 标签详情 -->
    <el-card class="section-card">
      <template #header><span class="section-title">标签详情</span><span class="section-tip">（以下信息均可选填）</span></template>
      <el-form label-width="120px">
        <el-form-item label="食品分类">
          <el-input :model-value="form.foodCategory" placeholder="点击选择食品分类" readonly @click="categoryVisible = true">
            <template #append><el-button @click="categoryVisible = true">选择</el-button></template>
          </el-input>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="保质期"><el-input v-model="form.shelfLife" placeholder="如：12个月" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生产日期标示"><el-input v-model="form.productionDateLabel" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="保质期到期日标示"><el-input v-model="form.expiryDateLabel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生产许可证编号"><el-input v-model="form.licenseNo" placeholder="食品生产许可证编号" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="产品标准代号"><el-input v-model="form.standardCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="质量等级"><el-input v-model="form.qualityGrade" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="贮存条件"><el-input v-model="form.storageCondition" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="转基因食品">
              <el-radio-group v-model="form.gmoFood">
                <el-radio value="是">是</el-radio><el-radio value="否">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="辐照食品">
              <el-radio-group v-model="form.irradiatedFood">
                <el-radio value="是">是</el-radio><el-radio value="否">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="定量标识"><el-input v-model="form.quantityLabel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="批号标示"><el-input v-model="form.batchNoLabel" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="致敏物质">
          <el-input v-model="form.allergens" type="textarea" :rows="2" placeholder="如：含有大豆、乳制品" />
        </el-form-item>
        <el-form-item label="食用方法">
          <el-input v-model="form.consumptionMethod" type="textarea" :rows="2" />
        </el-form-item>
        <!-- 自定义扩展字段 -->
        <el-form-item v-for="(field, idx) in customFields" :key="idx" :label="field.name">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="field.value" :placeholder="`请输入${field.name}`" />
            <el-button type="danger" text @click="customFields.splice(idx, 1)">删除</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button text type="primary" @click="customFieldVisible = true">
            <el-icon><Plus /></el-icon>添加其他扩展信息
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 生产信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">生产信息</span></template>
      <div v-for="(info, idx) in productionInfo" :key="idx" class="production-block">
        <div class="production-block-header">
          <span class="production-type">{{ info.type }}</span>
          <div class="production-actions">
            <el-button size="small" text :disabled="idx === 0" @click="moveInfo(idx, -1)">上移</el-button>
            <el-button size="small" text :disabled="idx === productionInfo.length - 1" @click="moveInfo(idx, 1)">下移</el-button>
            <el-button size="small" text type="danger" @click="productionInfo.splice(idx, 1)">删除</el-button>
          </div>
        </div>
        <el-form label-width="100px">
          <el-form-item label="名称"><el-input v-model="info.name" :placeholder="`请输入${info.type}名称`" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="info.address" :placeholder="`请输入${info.type}地址`" /></el-form-item>
          <el-form-item label="联系方式">
            <div style="width:100%">
              <div v-for="(c, cIdx) in info.contacts" :key="cIdx" style="display:flex;gap:8px;margin-bottom:8px">
                <el-input v-model="info.contacts[cIdx]" placeholder="电话/邮箱" />
                <el-button type="danger" text @click="info.contacts.splice(cIdx, 1)">删除</el-button>
              </div>
              <el-button text type="primary" @click="info.contacts.push('')">
                <el-icon><Plus /></el-icon>添加联系方式
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <el-button text type="primary" @click="productionTypeVisible = true">
        <el-icon><Plus /></el-icon>添加生产者信息
      </el-button>
    </el-card>

    <!-- 扩展信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">扩展信息</span></template>
      <el-form label-width="120px">
        <el-form-item label="食品介绍视频">
          <el-upload :file-list="videoFileList" :http-request="handleVideoUpload" :on-remove="() => form.introVideo = ''"
            accept=".mp4,.avi" :limit="1">
            <el-button v-if="!form.introVideo"><el-icon><Upload /></el-icon>上传视频</el-button>
          </el-upload>
          <div class="form-tip">MP4/AVI格式，不超过12M</div>
        </el-form-item>
        <el-form-item label="资质证书附件">
          <el-upload list-type="picture-card" :file-list="certFileList" :http-request="handleCertUpload"
            :on-remove="handleCertRemove" :on-preview="handlePreview" accept=".jpg,.jpeg,.png">
            <template v-if="certFileList.length < 3"><el-icon><Plus /></el-icon></template>
          </el-upload>
          <div class="form-tip">营业执照、检测报告等，jpg/png格式，单张不超过10M，最多3张</div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 版本信息 -->
    <el-card class="section-card">
      <template #header><span class="section-title">标签版本信息</span></template>
      <el-form label-width="120px">
        <el-form-item label="版本描述">
          <el-input v-model="form.versionDesc" type="textarea" :rows="3" placeholder="非必填，用于记录本版本的变更说明" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 底部操作 -->
    <div class="footer-actions">
      <el-button @click="$router.back()">{{ isAdmin ? '返回' : '取消' }}</el-button>
      <template v-if="!isAdmin">
        <el-button :loading="saving" @click="handleSave('draft')">存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave('publish')">保存并发布</el-button>
      </template>
    </div>

    <!-- 食品分类选择弹窗 -->
    <el-dialog v-model="categoryVisible" title="选择食品分类" width="420px">
      <el-input v-model="categoryKeyword" placeholder="搜索分类" clearable style="margin-bottom:12px" />
      <el-tree ref="categoryTreeRef" :data="categoryTree" :props="{ label: 'name', children: 'children' }"
        :filter-node-method="filterCategory" highlight-current default-expand-all
        @node-click="(node: any) => selectCategory(node)" />
    </el-dialog>

    <!-- 自定义扩展字段弹窗 -->
    <el-dialog v-model="customFieldVisible" title="添加扩展信息" width="400px">
      <el-form label-width="90px">
        <el-form-item label="属性名称">
          <el-input v-model="customFieldName" placeholder="请输入自定义属性名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customFieldVisible = false">取消</el-button>
        <el-button type="primary" @click="addCustomField">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加生产信息弹窗 -->
    <el-dialog v-model="productionTypeVisible" title="添加生产者信息" width="400px">
      <el-form label-width="110px">
        <el-form-item label="企业类型">
          <el-select v-model="productionType" style="width:100%">
            <el-option label="生产者" value="生产者" />
            <el-option label="经营者" value="经营者" />
            <el-option label="代理商" value="代理商" />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义名称">
          <el-input v-model="productionCustomType" placeholder="或输入自定义企业类型名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productionTypeVisible = false">取消</el-button>
        <el-button type="primary" @click="addProductionInfo">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" width="600px">
      <img :src="previewUrl" style="width:100%" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDlVersion, updateDlVersion, publishDlVersion, getDlCategories } from '@/api/digital-label'
import { uploadFile } from '@/api/common'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin } = useDlAdmin()
const route = useRoute()
const router = useRouter()
const versionId = Number(route.params.id)

const loading = ref(false)
const saving = ref(false)
const form = reactive<any>({
  foodName: '', barcode: '', ingredients: '', spec: '', netContent: '',
  foodImages: '', nutritionImage: '', foodCategory: '', shelfLife: '',
  productionDateLabel: '', expiryDateLabel: '', licenseNo: '', standardCode: '',
  qualityGrade: '', storageCondition: '', gmoFood: '', irradiatedFood: '',
  quantityLabel: '', batchNoLabel: '', allergens: '', consumptionMethod: '',
  introVideo: '', certificates: '', versionDesc: '', status: 'draft',
})
const customFields = ref<{ name: string; value: string }[]>([])
const productionInfo = ref<{ type: string; name: string; address: string; contacts: string[] }[]>([])

// 食品分类
const categoryVisible = ref(false)
const categoryKeyword = ref('')
const categoryTree = ref<any[]>([])
const categoryTreeRef = ref()
watch(categoryKeyword, v => categoryTreeRef.value?.filter(v))
function filterCategory(value: string, data: any) {
  if (!value) return true
  return data.name.includes(value)
}
function selectCategory(node: any) {
  form.foodCategory = node.fullPath || node.name
  categoryVisible.value = false
}

// 自定义扩展字段
const customFieldVisible = ref(false)
const customFieldName = ref('')
function addCustomField() {
  const name = customFieldName.value.trim()
  if (!name) { ElMessage.warning('请输入属性名称'); return }
  if (customFields.value.some(f => f.name === name)) { ElMessage.warning('该属性已存在'); return }
  customFields.value.push({ name, value: '' })
  customFieldName.value = ''
  customFieldVisible.value = false
}

// 生产信息
const productionTypeVisible = ref(false)
const productionType = ref('生产者')
const productionCustomType = ref('')
function addProductionInfo() {
  const type = productionCustomType.value.trim() || productionType.value
  productionInfo.value.push({ type, name: '', address: '', contacts: [] })
  productionCustomType.value = ''
  productionTypeVisible.value = false
}
function moveInfo(idx: number, dir: number) {
  const target = idx + dir
  if (target < 0 || target >= productionInfo.value.length) return
  const arr = productionInfo.value
  ;[arr[idx], arr[target]] = [arr[target], arr[idx]]
}

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')
function handlePreview(file: any) {
  previewUrl.value = file.url
  previewVisible.value = true
}

// 文件列表（供 el-upload 展示）
const foodImageList = computed(() =>
  (form.foodImages ? form.foodImages.split(',') : []).filter(Boolean).map((url: string) => ({ url })))
const nutritionImageList = computed(() =>
  form.nutritionImage ? [{ url: form.nutritionImage }] : [])
const certFileList = computed(() =>
  (form.certificates ? form.certificates.split(',') : []).filter(Boolean).map((url: string) => ({ url })))
const videoFileList = computed(() =>
  form.introVideo ? [{ name: form.introVideo.split('/').pop(), url: form.introVideo }] : [])

async function doUpload(options: any, maxSizeMb: number): Promise<string> {
  const file = options.file as File
  if (file.size > maxSizeMb * 1024 * 1024) {
    throw new Error(`文件大小不能超过${maxSizeMb}M`)
  }
  const res = await uploadFile(file)
  return res.data?.url || res.data || ''
}

async function handleFoodImageUpload(options: any) {
  try {
    const url = await doUpload(options, 5)
    form.foodImages = form.foodImages ? form.foodImages + ',' + url : url
    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '图片上传失败')
  }
}
function handleFoodImageRemove(file: any) {
  const urls = form.foodImages.split(',').filter(Boolean)
  const idx = urls.indexOf(file.url)
  if (idx >= 0) urls.splice(idx, 1)
  form.foodImages = urls.join(',')
}

async function handleNutritionUpload(options: any) {
  try {
    form.nutritionImage = await doUpload(options, 5)
    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '图片上传失败')
  }
}

async function handleVideoUpload(options: any) {
  try {
    form.introVideo = await doUpload(options, 12)
    ElMessage.success('视频上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '视频上传失败')
  }
}

async function handleCertUpload(options: any) {
  try {
    const url = await doUpload(options, 10)
    form.certificates = form.certificates ? form.certificates + ',' + url : url
    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '图片上传失败')
  }
}
function handleCertRemove(file: any) {
  const urls = form.certificates.split(',').filter(Boolean)
  const idx = urls.indexOf(file.url)
  if (idx >= 0) urls.splice(idx, 1)
  form.certificates = urls.join(',')
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await getDlVersion(versionId)
    const v = res.data || {}
    Object.keys(form).forEach(k => { form[k] = v[k] ?? '' })
    customFields.value = parseArray(v.customFields)
    const pi = parseArray(v.productionInfo)
    productionInfo.value = pi.length ? pi.map((p: any) => ({
      type: p.type || '', name: p.name || '', address: p.address || '',
      contacts: Array.isArray(p.contacts) ? p.contacts : [],
    })) : [
      { type: '生产者', name: '', address: '', contacts: [] },
      { type: '经营者', name: '', address: '', contacts: [] },
      { type: '代理商', name: '', address: '', contacts: [] },
    ]
  } finally {
    loading.value = false
  }
}

function parseArray(json: string): any[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

async function handleSave(action: 'draft' | 'publish') {
  saving.value = true
  try {
    const payload = {
      ...form,
      customFields: JSON.stringify(customFields.value.filter(f => f.name)),
      productionInfo: JSON.stringify(productionInfo.value
        .filter(p => p.name || p.address || p.contacts.some(c => c))
        .map(p => ({ ...p, contacts: p.contacts.filter(Boolean) }))),
    }
    await updateDlVersion(versionId, payload)
    if (action === 'publish') {
      await publishDlVersion(versionId)
      ElMessage.success('保存并发布成功')
      router.push(`/dl/products`)
    } else {
      ElMessage.success('草稿已保存')
      router.back()
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadData()
  try {
    const res = await getDlCategories()
    categoryTree.value = res.data || []
  } catch (e) {}
})
</script>

<style scoped lang="scss">
.dl-version-edit {
  max-width: 1000px;
  .section-card { margin-bottom: 16px; }
  .section-title { font-weight: 600; font-size: 15px; }
  .section-tip { font-size: 12px; color: #9ca3af; margin-left: 8px; }
  .form-tip { font-size: 12px; color: #9ca3af; line-height: 1.4; }

  /* 管理员只读：禁用表单交互 */
  &.readonly .section-card {
    :deep(.el-button) { display: none; }
    :deep(.el-input__inner), :deep(.el-textarea__inner) { pointer-events: none; }
    :deep(.el-select), :deep(.el-cascader), :deep(.el-radio-group) { pointer-events: none; }
    :deep(.el-upload) { display: none; }
    .production-actions { display: none; }
  }
}
.production-block {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
  .production-block-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    .production-type { font-weight: 600; color: #059669; }
  }
}
.footer-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px 0;
}
</style>
