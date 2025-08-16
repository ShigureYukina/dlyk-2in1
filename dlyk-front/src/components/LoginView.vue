<template>
  <el-container>
    <!--左侧-->
    <el-aside width="200px">
      <img src="../assets/loginBox.svg" class="asideImg">
      <p class="asideTitle">
        欢迎使用动力云客系统
      </p>
    </el-aside>

    <!--右侧-->
    <el-main>
      <div class="mainTitle">欢迎登录</div>
      <div class="mainForm">
        <el-form ref="loginRefForm" :model="user" label-width="120px" :rules="loginRules">
          <el-form-item label="账号" prop="loginAct">
            <el-input v-model="user.loginAct"/>
          </el-form-item>

          <el-form-item label="密码" prop="loginPwd">
            <el-input type="password" v-model="user.loginPwd"/>
          </el-form-item>


          <el-form-item>
            <el-button type="primary" @click="login">登 录</el-button>
          </el-form-item>
          <el-form-item>
            <el-checkbox value="Online activities" v-model="user.rememberMe">
              记住我
            </el-checkbox>
          </el-form-item>
        </el-form>
      </div>
    </el-main>
  </el-container>
</template>

<script>
//从httpRequest.js中导入doPost()函数（.js后缀可以省略）
import {doPost} from "../http/httpRequest";
import {getTokenName, messageTip, removeToken} from "../util/util.js";
import router from "../router/index.js";
// import {clearToken, jwtName, messageTip} from "../util/utils";

//export default { } 这个语法结构是固定的
export default {
  //定义组件名字，可以省略，默认组件名就是文件名
  name: "LoginView",

  //凡是页面上使用到的变量，都需要再data()函数中定义
  data() {
    //一定要return，代码结构是固定的
    return {
      //表单数据对象,定义对象用{},一般初始值都为空，具体值需要从后端查询后再赋值，此处只是定义一下
      user: {},
      age: 0,
      name: '',
      arr: [],
      userList: [{}],
      //定义form表单的验证规则对象
      loginRules: {
        //验证登录账号字段
        loginAct: [
          {required: true, message: '请输入登录账号', trigger: 'blur'}
        ],
        //验证登录密码字段
        loginPwd: [
          {required: true, message: '请输入登录密码', trigger: 'blur'}
        ]
      }
    }
  },

  //vue的生命周期函数，是一个函数钩子
  mounted() {
    //判断用户是否需要免登录（要不要给用户自动登录）
    // this.freeLogin();
  },

  //页面上所有的js函数写在methods属性中
  methods: {
    //登录函数
    login() {
      // 验证表单
      this.$refs.loginRefForm.validate((valid) => {
        if (valid) { // valid 是 true 表示验证通过了
          // 既然去登录，那么把浏览器之前存放的 token 都删除一下
          let formData = new FormData(); // js 对象
          formData.append("loginAct", this.user.loginAct);
          formData.append("loginPwd", this.user.loginPwd);
          formData.append("rememberMe", this.user.rememberMe);

          doPost('/api/login', formData).then((response) => { // 获取 ajax 异步请求后的结果
            console.log(response.data.data);
            if (response.data.code === 200) {
              // 可以登录
              messageTip("登录成功", "success");
              // 清除 token
              removeToken();

              // 把 token 存放到浏览器本地存储中
              if (this.user.rememberMe === true) {
                window.localStorage.setItem(getTokenName(), response.data.data); // 修正这里
              } else {
                window.sessionStorage.setItem(getTokenName(), response.data.data); // 修正这里
              }
              router.push("/dashboard"); // 跳转到首页

            } else {
              messageTip("登录失败", "error");
            }
          }).catch((error) => { // 当发生异常执行该 catch 函数
            console.log(error);
          });
        }
      });
    },


    //免登录（自动登录）
    // freeLogin() {
    //   let token = window.localStorage.getItem(jwtName());
    //   if (token) { //这个判断和java不一样，它表示token不是空，token有值，token是存在的
    //     //需要免登录（自动登录），免登录不需要用账号去数据库查询登录，只需要把token发送给后台，后台验证一下token如果是合法的，就自动完成登录
    //     doGet("/api/freeLogin", {}).then((resp) => { //获取ajax异步请求后的结果
    //       console.log(resp);
    //       if (resp.data.code === 200) {
    //         //可以免登录
    //         window.location.href = "/dashboard";
    //       }
    //     }).catch((error) => { //当发生异常执行该catch函数
    //       console.log(error);
    //     }).finally(() => { //总是会执行
    //       // 总是会执行
    //     });
    //   }
    // }
  }
}
</script>

<style scoped>
.el-aside {
  background: black;
  width: 40%;
  text-align: center;
}

.el-main {
  height: calc(100vh);
}

.asideImg {
  width: 400px;
}

.asideTitle {
  color: white;
  font-size: 28px;
}

.mainTitle {
  text-align: center;
  margin-top: 100px;
  margin-bottom: 25px;
  font-weight: bold;
}

.mainForm {
  text-align: center;
  width: 60%;
  margin: auto;
}

.el-button {
  width: 100%;
}

.el-form {
  margin: auto;
}
</style>