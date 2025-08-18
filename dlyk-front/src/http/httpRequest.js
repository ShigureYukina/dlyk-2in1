//从axios框架导入axios组件
import axios from "axios";
import {getTokenName, messageConfirm, messageTip, removeToken} from "../util/util.js";

axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
//定义后端接口地址的前缀
axios.defaults.baseURL = "http://localhost:8089";

export function doGet(url, params) {
    return axios({
        method: "get",
        url: url,
        params: params,
        dataType: "json"
    })
}

export function doPost(url, data) {
    return axios.post(url, data)
        .then(response => response)
        .catch(error => {
            console.error("POST请求失败:", error);
            throw error; // 确保错误能够被捕获
        });
}

export function doPut(url, data) {
    return axios.put(url, data)
        .then(response => response)
        .catch(error => {
            console.error("PUT请求失败:", error);
            throw error; // 确保错误能够被捕获
        });
}

export function doDelete(url, data) {
    return axios.delete(url, {data}) // axios.delete 返回一个 Promise
        .then(response => response)
        .catch(error => {
            console.error("DELETE请求失败:", error);
            throw error; // 确保错误能够被捕获
        });
}


// 添加请求拦截器
axios.interceptors.request.use((config) => {
    // 在发送请求之前做些什么，在请求头中放一个token（jwt），传给后端接口
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) { //前面加了一个！，表示token不存在，token是空的，token没有值，这个意思
        token = window.localStorage.getItem(getTokenName());
        if (token) {
            config.headers['rememberMe'] = true;
        }
    }
    if (token) { //表示token存在，token不是空的，token有值，这个意思
        config.headers['Authorization'] = token;
    }
    return config;
}, (error) => {
    // 对请求错误做些什么
    return Promise.reject(error);
});


// 添加响应拦截器
axios.interceptors.response.use((response) => {
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么，拦截token验证的结果，进行相应的提示和页面跳转
    if (response.data.code > 900) { //code大于900说明是token验证未通过
        messageConfirm(response.data.msg + '，是否重新登录').then(() => {
            removeToken();
            window.location.href = "/";
        }).catch(() => {
            messageTip('取消去登录', 'warning')
        });
        //给前端用户提示，并且跳转页面
    }
    return response;
}, function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error);
});