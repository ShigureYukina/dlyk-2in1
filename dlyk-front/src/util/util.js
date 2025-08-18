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