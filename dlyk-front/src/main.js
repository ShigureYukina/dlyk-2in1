//import...from...语句导入，从vue框架导入createApp函数
import {createApp} from 'vue'

//导入css样式，不需要from子句
//import './style.css'
//import...from...语句导入，从element-plus框架导入ElementPlus组件
import ElementPlus from 'element-plus'
//导入element-plus的语言包，不需要from子句
import zhCn from 'element-plus/es/locale/lang/zh-cn'

//导入element-plus的css样式，不需要from子句
import 'element-plus/dist/index.css'

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

//从router.js中导入router组件
import router from './router/router.js'

//从./App.vue页面导入App组件
import App from './App.vue' //根组件

let app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

//利用上面所导入的createApp()函数，创建一个vue应用，mount是挂载到#app地方
app.use(ElementPlus, {locale: zhCn}).use(router).mount('#app')
