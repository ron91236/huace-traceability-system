import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface ChildEnterprise {
  id: number
  name: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  userType: 'admin' | 'enterprise'
  enterpriseId?: number
  enterpriseName?: string
  roleId: number
  avatar?: string
  accountLevel?: 'master' | 'child' | 'standalone'
  childEnterprises?: ChildEnterprise[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('trace_token') || '')
  let parsedUserInfo: UserInfo | null = null
  try {
    parsedUserInfo = JSON.parse(localStorage.getItem('trace_userInfo') || 'null')
  } catch {
    localStorage.removeItem('trace_userInfo')
  }
  const userInfo = ref<UserInfo | null>(parsedUserInfo)
  const currentViewEnterpriseId = ref<number | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.userType === 'admin')
  const isEnterprise = computed(() => userInfo.value?.userType === 'enterprise')
  const isMasterAccount = computed(() => userInfo.value?.accountLevel === 'master')

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('trace_token', val)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('trace_userInfo', JSON.stringify(info))
  }

  function setCurrentViewEnterpriseId(id: number | null) {
    currentViewEnterpriseId.value = id
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    currentViewEnterpriseId.value = null
    localStorage.removeItem('trace_token')
    localStorage.removeItem('trace_userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isEnterprise,
    isMasterAccount,
    currentViewEnterpriseId,
    setToken,
    setUserInfo,
    setCurrentViewEnterpriseId,
    logout,
  }
})
