import {createApp} from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from "./router/";
import * as ElementPlusIconsVue from '@element-plus/icons-vue'; // 导入 ElementPlusIconsVue

let app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 创建 Vue 应用实例并挂载到 #app 元素上
app.use(ElementPlus).use(router).mount('#app')
