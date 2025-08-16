//从axios框架导入axios组件
import axios from "axios";
import {getTokenName, messageConfirm, messageTip, removeToken} from "../util/util.js";
import {ElMessage, ElMessageBox} from "element-plus";

//定义后端接口地址的前缀
axios.defaults.baseURL = "http://localhost:8089";

export function doGet(url, params) {
    return axios({
        method: "get",
        url: url,
        params: params, //{name: "对的", age: 22},
        dataType:"json"
    })
}

export function doPost(url, data) {
    return axios({
        method: "post",
        url: url,
        data: data, //{name: "好的呢", age: 22},
        dataType: "json"
    })
}

export function doPut(url, data) {
    axios({
        method: "put",
        url: url,
        data: data, //{name:"好的呢", age: 22},
        dataType: "json"
    }).then(function (rep){
        // rep.data = [{"name":"李四","age":22},{"name":"张三","age":23},{"name":"王五","age":24}]
        var s = "";
        rep.data.forEach(function (stu){
            s += stu.name + "--------------" + stu.age +"<br>";
        });
        document.getElementById("mydiv").innerHTML = s;
    })
}

export function doDelete(url, params) {
    axios({
        method: "delete",
        url: url,
        params: params, //{name: "对的", age: 22},
        dataType:"json"
    }).then(function (rep){
        // rep.data = [{"name":"李四","age":22},{"name":"张三","age":23},{"name":"王五","age":24}]
        var s = "";
        rep.data.forEach(function (stu){
            s += stu.name + "--------------" + stu.age +"<br>";
        });
        document.getElementById("mydiv").innerHTML = s;
    })
}


// 添加请求拦截器
axios.interceptors.request.use( (config) => {
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
},  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error);
});


// 添加响应拦截器
axios.interceptors.response.use( (response) => {
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么，拦截token验证的结果，进行相应的提示和页面跳转
    if (response.data.code > 900) { //code大于900说明是token验证未通过
        //给前端用户提示，并且跳转页面
        messageConfirm(response.data.msg + "，是否重新去登录？").then(() => { //用户点击“确定”按钮就会触发then函数
            //既然后端验证token未通过，那么前端的token肯定是有问题的，那没必要存储在浏览器中，直接删除一下
            removeToken();
            //跳到登录页
            window.location.href = "/";
        }).catch(() => { //用户点击“取消”按钮就会触发then函数
            messageTip("取消去登录", "warning");
        })
        return ;
    }
    return response;
}, function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error);
});