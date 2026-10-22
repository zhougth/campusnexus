import type {
  LoginDTO,
  RegisterDTO,
  UpdatePasswordDTO,
  UpdateProfileDTO,
  UserVO,
} from '@/types/api'
import { request } from '@/utils/http'

export function register(dto: RegisterDTO): Promise<UserVO> {
  return request({ url: '/auth/register', method: 'POST', data: dto })
}

export function login(dto: LoginDTO): Promise<string> {
  return request({ url: '/auth/login', method: 'POST', data: dto })
}

export function logout(): Promise<void> {
  return request({ url: '/auth/logout', method: 'POST', silent: true })
}

export function fetchMe(): Promise<UserVO> {
  return request({ url: '/auth/me', method: 'GET', silent: true })
}

export function updateProfile(dto: UpdateProfileDTO): Promise<void> {
  return request({ url: '/auth/profile', method: 'PUT', data: dto })
}

export function updatePassword(dto: UpdatePasswordDTO): Promise<void> {
  return request({ url: '/auth/password', method: 'PUT', data: dto })
}

export function uploadAvatar(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  return request({ url: '/auth/avatar', method: 'PUT', data: form })
}
