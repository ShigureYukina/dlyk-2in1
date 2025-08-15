import axios from 'axios'
axios.defaults.baseURL = 'http://localhost:8089' //设置默认的请求地址

export function doGet(url, params) {
    return axios.get(url, { //发送ajax异步请求
        params: params
    })
}

export function doPost(url, data) {
    return axios.post(url, data); //发送ajax异步请求
}

export function doPut(url, data) {
    return axios.put(url, data); //发送ajax异步请求
}

export function doDelete(url, params) {
    return axios.delete(url, { //发送ajax异步请求
        params: params
    });
}
