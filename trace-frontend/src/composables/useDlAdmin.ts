import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDlEnterprises } from '@/api/digital-label'

/**
 * 数字标签模块管理员模式支持：
 * - isAdmin：当前是否管理员（全局只读查看）
 * - entFilter：企业筛选（仅管理员可用，undefined=全部企业）
 * - enterprises：已创建数字标签的企业列表
 * - entName：按企业ID取企业名称
 */
export function useDlAdmin() {
  const userStore = useUserStore()
  const isAdmin = computed(() => userStore.isAdmin)
  const entFilter = ref<number | undefined>(undefined)
  const enterprises = ref<{ id: number; name: string }[]>([])

  onMounted(async () => {
    if (isAdmin.value) {
      try {
        const res = await getDlEnterprises()
        enterprises.value = res.data || []
      } catch (e) { /* ignore */ }
    }
  })

  function entName(id?: number | null) {
    if (!id) return '-'
    return enterprises.value.find(e => e.id === id)?.name || String(id)
  }

  return { isAdmin, entFilter, enterprises, entName }
}
