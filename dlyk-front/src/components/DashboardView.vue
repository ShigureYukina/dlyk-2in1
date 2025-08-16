<template>
  <el-container>
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="menuTitle">动力云客管理系统</div>
      <el-menu
          background-color="#334157"
          text-color="white"
          style=" border-right: solid 0;"
          :unique-opened="true"
          @open="handleOpen"
          @close="handleClose"
          :collapse="isCollapse"
          :collapse-transition="false"
      >
        <el-sub-menu index="1">
          <template #title>
            <el-icon>
              <OfficeBuilding/>
            </el-icon>
            <span>市场活动</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <Notification/>
              </el-icon>
              市场活动
            </el-menu-item>
            <el-menu-item index="1-2">
              <el-icon>
                <DataAnalysis/>
              </el-icon>
              市场统计
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="2">
          <template #title>
            <el-icon>
              <Magnet/>
            </el-icon>
            <span>线索管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <Film/>
              </el-icon>
              线索管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="3">
          <template #title>
            <el-icon>
              <User/>
            </el-icon>
            <span>客户管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              客户管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="4">
          <template #title>
            <el-icon>
              <Wallet/>
            </el-icon>
            <span>交易管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              交易管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="5">
          <template #title>
            <el-icon>
              <Memo/>
            </el-icon>
            <span>产品管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              产品管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="6">
          <template #title>
            <el-icon>
              <Grid/>
            </el-icon>
            <span>字典管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              字典管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="7">
          <template #title>
            <el-icon>
              <Stamp/>
            </el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              用户管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
        <el-sub-menu index="8">
          <template #title>
            <el-icon>
              <Setting/>
            </el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item-group>
            <el-menu-item index="1-1">
              <el-icon>
                <CreditCard/>
              </el-icon>
              系统管理
            </el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="rightContent">
      <el-header>
        <el-icon class="show" @click="showMenu">
          <Fold/>
        </el-icon>
        <el-dropdown>
    <span class="el-dropdown-link">
      张三
      <el-icon class="el-icon--right">
        <arrow-down/>
      </el-icon>
    </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item></el-dropdown-item>
              <el-dropdown-item>我的资料</el-dropdown-item>
              <el-dropdown-item>修改密码</el-dropdown-item>
              <el-dropdown-item divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>Main</el-main>
      <el-footer>你所热爱的就是你的生活</el-footer>
    </el-container>
  </el-container>
</template>
<script>
import {defineComponent} from "vue";
import {doGet} from "../http/httpRequest.js";
import {DataAnalysis, Film, Magnet, Notification, OfficeBuilding} from "@element-plus/icons-vue";

export default defineComponent({
  name: 'DashboardView',
  components: {Film, Magnet, DataAnalysis, Notification, OfficeBuilding},
  data() {
    return {
      isCollapse: false,
    };
  },
  mounted() {
    this.loadLoginUser()
  },
  methods: {
    showMenu() {
      this.isCollapse = !this.isCollapse;
    },
    loadLoginUser() {
      doGet("/api/login/info").then((resp) => {
        console.log(resp.data)
      })
    },
  }
});


</script>
<style scoped>
.el-aside {
  background: black;
}

.el-header {
  background: azure;
  height: 38px;
  line-height: 38px;
}

.el-footer {
  background: aliceblue;
  height: 38px;
  line-height: 38px;
  text-align: center;
}

.rightContainer {
  height: calc(100vh);
}


.rightContent {
  height: calc(100vh);
}

.menuTitle {
  height: 35px;
  color: #fff;
  text-align: center;
  line-height: 35px;
}

.show {
  cursor: pointer;
}

.el-dropdown {
  float: right;
  line-height: 35px;
}
</style>