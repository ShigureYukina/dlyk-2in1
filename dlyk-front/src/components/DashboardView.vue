<template>
  <el-container>
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="menuTitle">动力云客管理系统</div>
      <el-menu
          background-color="#334157"
          text-color="white"
          style=" border-right: solid 0;"
          :collapse="isCollapse"
          :default-active="activeIndex"
          :collapse-transition="false"
          :router="true"
          :unique-opened="true"
      >
        <!-- 修改点1: 使用正确的属性名 subPermissionDO -->
        <el-sub-menu :index="String(menuPermission.id)" v-for="menuPermission in user.menuPermissionList"
                     :key="menuPermission.id">
          <template #title>
            <el-icon>
              <component :is="menuPermission.icon"></component>
            </el-icon>
            <span> {{ menuPermission.name }} </span>
          </template>
          <el-menu-item v-for="subPermission in menuPermission.subPermissionDO" :key="subPermission.id"
                        :index="subPermission.url">
            <el-icon>
              <component :is="subPermission.icon"></component>
            </el-icon>
            {{ subPermission.name }}
          </el-menu-item>
        </el-sub-menu>

      </el-menu>
    </el-aside>

    <el-container class="rightContent">
      <el-header>
        <el-icon class="show" @click="showMenu">
          <Fold/>
        </el-icon>

        <el-dropdown :hide-on-click="false">
          <span class="el-dropdown-link">
            {{ user.name }}
            <el-icon class="el-icon--right"><arrow-down/></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>我的资料</el-dropdown-item>
              <el-dropdown-item>修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

      </el-header>
      <el-main>
        <router-view v-if="isRouteActive"/>
      </el-main>
      <el-footer>你所热爱的就是你的生活</el-footer>
    </el-container>
  </el-container>
</template>
<script>
import {defineComponent} from "vue";
import {doGet} from "../http/httpRequest.js";
import {DataAnalysis, Film, Magnet, Notification, OfficeBuilding} from "@element-plus/icons-vue";
import {messageTip, removeToken} from "../util/util.js";

export default defineComponent({
      name: 'DashboardView',
      components: {Film, Magnet, DataAnalysis, Notification, OfficeBuilding},
      data() {
        return {
          isCollapse: false,
          user: {},
          isRouteActive: true,
          activeIndex: ''

        };
      },
      provide() {
        return {
          reload: () => {
            this.isRouteActive = false;
            this.$nextTick(() => {
              {
                this.isRouteActive = true;
              }
            })
          },
        }
      },

      mounted() {
        this.loadLoginUser()
        this.loadCurrentRoute()
      },
      methods: {
        showMenu() {
          this.isCollapse = !this.isCollapse;
        },
        loadLoginUser() {
          doGet("/api/login/info", {}).then((resp) => {
            this.user = resp.data.data;
            // 修改点2: 添加调试日志，检查数据结构
            console.log("用户数据:", this.user);
            console.log("菜单数据:", this.user.menuPermissionList);
          })
        },
        logout() {
          doGet("/api/logout", {}).then((response) => {
            removeToken()
            messageTip("退出成功", "success")
            window.location.href = "/";
          })
        },
        loadCurrentRoute() {
          let arr = this.$route.path.split("/");
          if (arr.length > 2) {
            this.activeIndex = "/" + arr[1] + "/" + arr[2];
          } else {
            this.activeIndex = this.$route.path;
          }
        }
      }
    }
);
</script>
<style scoped>
.el-aside {
  background: #1a1a1a;
}

.el-header {
  background: azure;
  height: 35px;
  line-height: 35px;
}

.el-footer {
  background: aliceblue;
  height: 35px;
  line-height: 35px;
  text-align: center;
}

.rightContent {
  height: calc(100vh);
}

.menuTitle {
  height: 35px;
  line-height: 35px;
  color: #f9f9f9;
  text-align: center;
}

.show {
  cursor: pointer;
}

.el-dropdown {
  float: right;
  line-height: 35px;
}
</style>
