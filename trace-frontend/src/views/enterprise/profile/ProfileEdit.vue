<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="table-toolbar">
          <span class="card-title">企业信息</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="max-width:860px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="企业名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="企业性质"><el-input v-model="form.nature" placeholder="如：民营企业" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属行业"><el-input v-model="form.industry" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主营类型"><el-input v-model="form.mainType" placeholder="如：农产品种植" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人" prop="contact"><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="统一社会信用代码"><el-input v-model="form.creditCode" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">地址信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="省"><el-input v-model="form.province" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="区"><el-input v-model="form.district" /></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="详细地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="邮编"><el-input v-model="form.zipcode" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">企业介绍与资质</el-divider>
        <el-form-item label="企业介绍"><el-input v-model="form.introduction" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="荣誉资质"><el-input v-model="form.honors" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="资质证书"><el-input v-model="form.qualifications" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="标准体系"><el-input v-model="form.standardSystem" type="textarea" :rows="2" /></el-form-item>
        <el-divider content-position="left">企业展示</el-divider>
        <el-form-item label="企业形象图">
          <el-upload
            :file-list="enterpriseImageFileList"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
            :http-request="(opt: any) => handleImageUpload(opt, 'enterpriseImage', enterpriseImageFileList)"
            :on-remove="() => handleImageRemove('enterpriseImage', enterpriseImageFileList)"
            :on-preview="(file: any) => previewImage(file.url)"
          >
            <el-icon :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议上传企业logo或形象展示图</div>
        </el-form-item>
        <el-form-item label="营业执照">
          <el-upload
            :file-list="licenseImageFileList"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
            :http-request="(opt: any) => handleImageUpload(opt, 'licenseImage', licenseImageFileList)"
            :on-remove="() => handleImageRemove('licenseImage', licenseImageFileList)"
            :on-preview="(file: any) => previewImage(file.url)"
          >
            <el-icon :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="宣传视频">
          <el-upload
            :file-list="promoVideoFileList"
            accept="video/*"
            :limit="1"
            :http-request="(opt: any) => handleVideoUpload(opt)"
            :on-remove="() => handleImageRemove('promoVideo', promoVideoFileList)"
          >
            <el-button>上传视频</el-button>
            <template #tip><div class="upload-tip">支持 mp4 等常见视频格式</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getEnterpriseProfile, updateEnterpriseProfile } from '@/api/enterprise'
import { uploadFile } from '@/api/common'

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const enterpriseImageFileList = ref<any[]>([])
const licenseImageFileList = ref<any[]>([])
const promoVideoFileList = ref<any[]>([])

const form = reactive<any>({
  name: '', nature: '', industry: '', contact: '', phone: '', email: '',
  creditCode: '', province: '', city: '', district: '', address: '', zipcode: '',
  introduction: '', honors: '', qualifications: '', standardSystem: '',
  enterpriseImage: '', licenseImage: '', promoVideo: '', mainType: '',
})

const rules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

function urlsToFileList(url: string) {
  if (!url) return []
  return [{ name: 'file', url }]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getEnterpriseProfile()
    const d = res.data || {}
    Object.keys(form).forEach(k => { form[k] = d[k] ?? '' })
    enterpriseImageFileList.value = urlsToFileList(form.enterpriseImage)
    licenseImageFileList.value = urlsToFileList(form.licenseImage)
    promoVideoFileList.value = urlsToFileList(form.promoVideo)
  } finally { loading.value = false }
})

async function handleImageUpload(options: any, field: string, fileList: any) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    fileList.value = [{ name: options.file.name, url }]
    form[field] = url
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

async function handleVideoUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    promoVideoFileList.value = [{ name: options.file.name, url }]
    form.promoVideo = url
    ElMessage.success('视频上传成功')
  } catch (e) {
    ElMessage.error('视频上传失败')
  }
}

function handleImageRemove(field: string, fileList: any) {
  fileList.value = []
  form[field] = ''
}

function previewImage(url: string) {
  window.open(url, '_blank')
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateEnterpriseProfile({ ...form })
    ElMessage.success('保存成功')
  } finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}
</style>
