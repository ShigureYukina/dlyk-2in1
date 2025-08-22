//从vue-router这个依赖库中导入createRouter()函数, createWebHistory()函数
import {createRouter, createWebHistory} from "vue-router";

//定义一个变量
let router = createRouter({
    //路由历史
    history: createWebHistory(),

    //配置路由，是一个数组，里面可以配置多个路由
    routes: [
        {
            //路由路径
            path: '/',
            //路由路径所对应的页面
            component: () => import('../components/LoginView.vue'),
        },
        {
            //路由路径
            path: '/login',
            //路由路径所对应的页面
            component: () => import('../components/LoginView.vue'),
        },
        {
            //路由路径
            path: '/dashboard',
            //路由路径所对应的页面
            component: () => import('../components/DashboardView.vue'),
            children: [
                {
                    //子路由路径
                    path: 'user',
                    //子路由路径所对应的页面
                    component: () => import('../components/UserView.vue'),
                },
                {
                    //子路由路径,id是动态参数
                    path: 'user/:id',
                    component: () => import('../components/UserDetailView.vue'),

                },
                {
                    //子路由路径
                    path: 'activity',
                    component: () => import('../components/ActivityView.vue'),
                },
                {
                    //子路由路径
                    path: 'activity/:id',
                    component: () => import('../components/ActivityDetailView.vue'),
                },
                {
                    //子路由路径
                    path: 'activity/add',
                    component: () => import('../components/ActivityRecordView.vue'),
                },
                {
                    //子路由路径
                    path: 'activity/edit/:id',
                    component: () => import('../components/ActivityRecordView.vue'),
                }
            ]
        },
        {
            //路由路径
            path: '/hello',
            //路由路径所对应的页面
            component: () => import('../components/HelloWorld.vue'),
        }
    ]
})
//导出创建的路由对象
export default router;