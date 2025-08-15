import {ElMessage} from "element-plus";

export function messageTip(msg, type) {
    ElMessage({
        showClose: true,
        center: true,
        duration: 3000,
        message: msg,
        type: type,
    })
}