import request from '../utils/request.js'
const api_name = '/pan/folder'
export function getUserPaths(data) {
  return request({
    url: `${api_name}/getUserDir/v2`,
    method: 'post',
		data
  })
}

export function createDir(data) {
  return request({
    url: `${api_name}/createPath`,
    method: 'post',
		data
  })
}
export function deleteDir(id) {
  return request({
    url: `${api_name}/delete/`+id,
    method: 'post'
  })
}
export function renameDir(folderVo) {
  return request({
    url: `${api_name}/rename`,
    method: 'post',
		folderVo
  })
}
export function renameV2(data) {
  return request({
    url: `${api_name}/rename`,
    method: 'post',
		data
  })
}

export function downloadFolder(path) {
	const formData = new FormData();
	  formData.append('path', path);
  return request({
    url: `${api_name}/download`,
    method: 'post',
		data:formData
  })
}