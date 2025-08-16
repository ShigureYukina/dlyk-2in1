<template>
  <el-button type="primary" @click="add">添加用户</el-button>
  <el-button type="danger">批量删除</el-button>

  <el-table
      :data="userList"
      style="width: 100%"
      @selection-change="handleSelectionChange">

    <el-table-column type="selection" width="55" />
    <el-table-column type="index" label="序号" width="60" />
    <el-table-column prop="loginAct" label="账号" />
    <el-table-column property="name" label="姓名" show-overflow-tooltip />
    <el-table-column property="phone" label="手机" show-overflow-tooltip />
    <el-table-column property="email" label="邮箱" show-overflow-tooltip />
    <el-table-column property="createTime" label="创建时间" show-overflow-tooltip />
    <el-table-column label="操作" show-overflow-tooltip >
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)">详情</el-button>
        <el-button type="success" @click="edit(scope.row.id)">编辑</el-button>
        <el-button type="danger" @click="del(scope.row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="toPage"
      @next-click="toPage"
      @current-change="toPage"/>

  <!--新增用户的弹窗(对话框)-->
  <el-dialog v-model="userDialogVisible" title="新增用户" width="55%" center draggable>

    <el-form ref="userRefForm" :model="userQuery" label-width="110px" :rules="userRules">
      <el-form-item label="账号" prop="loginAct">
        <el-input v-model="userQuery.loginAct" />
      </el-form-item>

      <el-form-item label="密码" prop="loginPwd">
        <el-input type="password" v-model="userQuery.loginPwd" />
      </el-form-item>

      <el-form-item label="姓名" prop="name">
        <el-input v-model="userQuery.name" />
      </el-form-item>

      <el-form-item label="手机" prop="phone">
        <el-input v-model="userQuery.phone" />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="userQuery.email" />
      </el-form-item>

      <el-form-item label="账号未过期" prop="accountNoExpired">
        <el-select v-model="userQuery.accountNoExpired" placeholder="请选择">
          <el-option label="是" value="1"/>
          <el-option label="否" value="0"/>
        </el-select>
      </el-form-item>

      <el-form-item label="密码未过期" prop="credentialsNoExpired">
        <el-select v-model="userQuery.credentialsNoExpired" placeholder="请选择">
          <el-option label="是" value="1"/>
          <el-option label="否" value="0"/>
        </el-select>
      </el-form-item>

      <el-form-item label="账号未锁定" prop="accountNoLocked">
        <el-select v-model="userQuery.accountNoLocked" placeholder="请选择">
          <el-option label="是" value="1"/>
          <el-option label="否" value="0"/>
        </el-select>
      </el-form-item>

      <el-form-item label="账号是否启用" prop="accountEnabled">
        <el-select v-model="userQuery.accountEnabled" placeholder="请选择">
          <el-option label="是" value="1"/>
          <el-option label="否" value="0"/>
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="userDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="userSubmit">提 交</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script>
import {defineComponent} from 'vue'
import {doGet, doPost} from "../http/httpRequest.js";
import {messageTip} from "../util/util.js";

export default defineComponent({
  name: "UserView",

  data() {
    return {
      //用户列表数据，初始值是空
      userList : [{}],
      pageSize : 0,
      total : 0,
      //用户的弹窗是否弹出来，true就弹出来，false就不弹出来
      userDialogVisible : false,
      //定义用户表单对象，初始值是空
      userQuery : {},
      //定义用户验证规则
      userRules : {
        loginAct : [
          { required: true, message: '请输入登录账号', trigger: 'blur' }
        ],
        loginPwd : [
          { required: true, message: '请输入登录密码', trigger: 'blur' },
          { min: 6, max: 16, message: '登录密码长度为6-16位', trigger: 'blur' }
        ],
        name : [
          { required: true, message: '请输入姓名', trigger: 'blur' },
          { pattern: /^[\u4E00-\u9FA5]{1,5}$/, message: '姓名必须是中文', trigger: 'blur'}
        ],
        phone : [
          { required: true, message: '请输入手机号码', trigger: 'blur' },
          { pattern : /^1[3-9]\d{9}$/, message: '手机号码格式有误', trigger: 'blur'}
        ],
        email : [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { pattern : /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式有误', trigger: 'blur'}
        ],
        accountNoExpired : [
          { required: true, message: '请选择账号是否未过期', trigger: 'blur' },
        ],
        credentialsNoExpired : [
          { required: true, message: '请选择密码是否未过期', trigger: 'blur' },
        ],
        accountNoLocked : [
          { required: true, message: '请选择账号是否未未锁定', trigger: 'blur' },
        ],
        accountEnabled : [
          { required: true, message: '请选择账号是否可用', trigger: 'blur' },
        ]
      }
    }
  },

  mounted() {
    this.getData(1);
  },

  methods : {
    //勾选或者取消勾选时，触发该函数
    handleSelectionChange() {

    },

    //查询用户列表数据
    getData(current) {
      doGet("/api/users", {
        current : current //当前查询第几页
      }).then(resp => {
        console.log(resp);
        if (resp.data.code === 200) {
          this.userList = resp.data.data.list;
          this.pageSize = resp.data.data.pageSize;
          this.total = resp.data.data.total;
        }
      })
    },

    //分页函数(current这个参数是ele-plus组件传过来，就是传的当前页)
    toPage(current) {
      this.getData(current);
    },

    //详情
    view(id) {
      console.log(id);
      //跳转到/dashboard/user/1路由上
      this.$router.push("/dashboard/user/" + id);
    },

    //新增用户
    add() {
      this.userDialogVisible = true;
    },

    //新增用户提交保存
    userSubmit() {
      let formData = new FormData();
      for (let field in this.userQuery) {
        formData.append(field, this.userQuery[field]);
      }
      this.$refs.userRefForm.validate( (isValid) => {
        if (isValid) {
          doPost("/api/user", formData).then(resp => {
            if (resp.data.code === 200) {
              messageTip("提交成功", "success");
            } else {
              messageTip("提交失败", "error");
            }
          })
        }
      })
    }
  }
})
</script>

<style scoped>
.el-table {
  margin-top: 12px;
}
.el-pagination {
  margin-top: 12px;
}
.el-select {
  width: 100%;
}
</style>