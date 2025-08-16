<template>
  <el-container>
    <!--左侧-->
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="menuTitle">@动力云客管理系统</div>
      <el-menu
          active-text-color="#ffd04b"
          background-color="#334157"
          class="el-menu-vertical-demo"
          default-active="2"
          text-color="#fff"
          style="border-right: solid 0px;"
          :collapse="isCollapse"
          :collapse-transition="false"
          :router="true"
          :unique-opened="true">

        <!--市场活动菜单-->
        <el-sub-menu index="1">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>市场活动</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><Notification /></el-icon>
            市场活动
          </el-menu-item>
          <el-menu-item index="1-2">
            <el-icon><DataAnalysis /></el-icon>
            市场统计
          </el-menu-item>
        </el-sub-menu>

        <!--线索管理菜单-->
        <el-sub-menu index="2">
          <template #title>
            <el-icon><Operation /></el-icon>
            <span>线索管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><Film /></el-icon>
            线索管理
          </el-menu-item>
        </el-sub-menu>

        <!--客户管理菜单-->
        <el-sub-menu index="3">
          <template #title>
            <el-icon><User /></el-icon>
            <span>客户管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><CreditCard /></el-icon>
            客户管理
          </el-menu-item>
        </el-sub-menu>

        <!--交易管理菜单-->
        <el-sub-menu index="4">
          <template #title>
            <el-icon><location /></el-icon>
            <span>交易管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><CreditCard /></el-icon>
            交易管理
          </el-menu-item>
        </el-sub-menu>

        <!--产品管理菜单-->
        <el-sub-menu index="5">
          <template #title>
            <el-icon><location /></el-icon>
            <span>产品管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><CreditCard /></el-icon>
            产品管理
          </el-menu-item>
        </el-sub-menu>

        <!--字典管理菜单-->
        <el-sub-menu index="6">
          <template #title>
            <el-icon><location /></el-icon>
            <span>字典管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><CreditCard /></el-icon>
            字典管理
          </el-menu-item>
        </el-sub-menu>

        <!--用户管理菜单-->
        <el-sub-menu index="7">
          <template #title>
            <el-icon><location /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/dashboard/user">
            <el-icon><CreditCard /></el-icon>
            用户管理
          </el-menu-item>
        </el-sub-menu>

        <!--系统管理菜单-->
        <el-sub-menu index="8">
          <template #title>
            <el-icon><location /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="1-1">
            <el-icon><CreditCard /></el-icon>
            系统管理
          </el-menu-item>
        </el-sub-menu>

      </el-menu>

    </el-aside>

    <!--右侧-->
    <el-container class="rightContent">
      <!--右侧：上-->
      <el-header>
        <el-icon class="show" @click="showMenu"><Fold /></el-icon>

        <el-dropdown :hide-on-click="false">
          <span class="el-dropdown-link">
            {{ user.name }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
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

      <!--右侧：中-->
      <el-main>
        <router-view/>
      </el-main>

      <!--右侧：下-->
      <el-footer>@版权所有 2009-2099 动力节点 北京市通州区马驹桥镇景盛中街17号顺景总部公元B4栋</el-footer>
    </el-container>
  </el-container>
</template>

<script>
import {defineComponent} from 'vue'
import {doGet} from "../http/httpRequest.js";
import {messageConfirm, messageTip, removeToken} from "../util/util.js";

export default defineComponent({
  //定义组件名
  name: "DashboardView",

  data() {
    return {
      //控制左侧菜单左右的展开和折叠，true是折叠，false是展开
      isCollapse : false,
      //登录用户对象，初始值是空
      user : {}
    }
  },

  //vue的生命周期中的一个函数钩子，该钩子是在页面渲染后执行
  mounted() {
    //加载当前登录用户
    this.loadLoginUser();
  },

  methods : {
    //左侧菜单左右展开和折叠
    showMenu() {
      this.isCollapse = !this.isCollapse;
    },

    //加载当前登录用户
    loadLoginUser() {
      doGet("/api/login/info", {}).then( (resp) => {
        console.log(resp)
        this.user = resp.data.data;
      })
    },

    //退出登录
    logout() {
      doGet("/api/logout", {}).then(resp => {
        if (resp.data.code === 200) {
          removeToken();
          messageTip("退出成功", "success");
          //跳到登录页
          window.location.href = "/";
        } else {
          messageConfirm("退出异常，是否要强制退出？").then(() => { //用户点击“确定”按钮就会触发then函数
            //既然后端验证token未通过，那么前端的token肯定是有问题的，那没必要存储在浏览器中，直接删除一下
            removeToken();
            //跳到登录页
            window.location.href = "/";
          }).catch(() => { //用户点击“取消”按钮就会触发then函数
            messageTip("取消强制退出", "warning");
          })
        }
      })
    }
  }
})
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