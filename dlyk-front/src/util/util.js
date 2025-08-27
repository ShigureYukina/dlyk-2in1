import {ElMessage, ElMessageBox} from "element-plus";

/**
 * 消息提示
 *
 * @param msg
 * @param type
 */
export function messageTip(msg, type) {
    ElMessage({
        showClose: true, //是否显示关闭按钮
        center: true, //文字是否居中
        duration: 3000, //显示时间，单位为毫秒
        message: msg, //提示的消息内容
        type: type, //消息类型：'success' | 'warning' | 'info' | 'error'
    })
}

/**
 * 获取存储在sessionStorage或者localStorage中的token(jwt)的名字
 *
 * @returns {string}
 */
export function getTokenName() {
    return "dlyk_token";
}

/**
 * 删除localStorage和sessionStorage中存储的token
 *
 */
export function removeToken() {
    window.sessionStorage.removeItem(getTokenName());
    window.localStorage.removeItem(getTokenName());
}

/**
 * 消息确认提示
 *
 * @param msg
 * @returns {Promise<MessageBoxData>}
 */
export function messageConfirm(msg) {
    return ElMessageBox.confirm(
        msg, //提示语
        '系统提醒',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
}

export function goback() {
    this.$router.go(-1);
}

//获取token方法
export function getToken() {
    // 在发送请求之前做些什么，在请求头中放一个token（jwt），传给后端接口
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) { //前面加了一个！，表示token不存在，token是空的，token没有值，这个意思
        token = window.localStorage.getItem(getTokenName());
    }
    if (token) { //表示token存在，token不是空的，token有值，这个意思
        return token
    } else {
        messageConfirm('请求token为空，是否重新登录').then(() => {
            removeToken();
            window.location.href = "/";
        }).catch(() => {
            messageTip('取消去登录', 'warning')
        });
    }
}
