import request from '../utils/request.js'
const api_name = '/pan/user'
export function login(data) {
  return request({
    url: `${api_name}/login`,
    method: 'post',
    data 
  })
}

export function getCurrentUser() {
  return request({
    url: `${api_name}/get/login`,
    method: 'get'
  })
}

export function logout() {
  return request({
    url: `${api_name}/logout`,
    method: 'post'
  })
}
export function register(data) {
  return request({
    url: `${api_name}/register`,
    method: 'post',
		data
  })
}
export function setAvatar(data) {
  return request({
    url: `${api_name}/uploadAvatar`,
    method: 'post',
		data
  })
}
