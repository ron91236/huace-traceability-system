import request from './request'

// 登录
export function login(data: { username: string; password: string; loginType?: string }) {
  return request.post('/auth/login', data)
}
