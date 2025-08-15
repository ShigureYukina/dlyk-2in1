import {createRouter, createWebHistory} from "vue-router";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: () => import('../components/LoginView.vue'),
        },
        {
            path: '/dashboard',
            component: () => import('../components/DashboardView.vue'),
        },

    ]
})
export default router;
