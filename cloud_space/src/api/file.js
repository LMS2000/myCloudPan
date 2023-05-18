import request from '../utils/request.js'
const api_name = '/pan/file'
export function getFiles(data) {
  return request({
    url: `${api_name}/getFiles`,
    method: 'post',
		data
  })
}

export function deleteFiles(ids) {
  return request({
    url: `${api_name}/delete?ids=`+ids,
    method: 'post'
  })
}

export function moveFiles(ids,path) {
  return request({
    url: `${api_name}/move?ids=`+ids+`?path=`+path,
    method: 'post'
  })
}

export function renameFile(id,name) {
  return request({
    url: `${api_name}/rename/${id}/${name}`,
    method: 'post'
  })
}
export function uploadFile(path,file) {
	const formData = new FormData();
	  formData.append('file', file);

  return request({
    url: `${api_name}/upload?path=`+path,
    method: 'post',
		data:formData
  })
}

export function downloadFile(data) {
  return request({
    url: `${api_name}/download`,
    method: 'post',
		data
  })
}