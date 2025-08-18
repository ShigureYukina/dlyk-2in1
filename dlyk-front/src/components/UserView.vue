<template>
  <el-button type="primary" @click="add()">添加用户</el-button>
  <el-button type="danger">批量删除</el-button>
  <el-table
      ref="multipleTableRef"
      :data="userlist"
      style="width: 100%"
      @selection-change="handleSelectionChange"
  >
    <el-table-column type="selection" width="55"/>

    <el-table-column type="index" label="序号" width="60"/>
    <el-table-column property="loginAct" label="账号"/>
    <el-table-column property="name" label="姓名" width="120" show-overflow-tooltip/>
    <el-table-column property="phone" label="手机" show-overflow-tooltip/>
    <el-table-column property="email" label="邮箱" show-overflow-tooltip/>
    <el-table-column property="createTime" label="创建时间" show-overflow-tooltip/>
    <el-table-column label="操作" show-overflow-tooltip>
      <template #default="scope">
        <el-button type="primary" size="small" @click="view(scope.row.id)">详情</el-button>
        <el-button type="warning" size="small">编辑</el-button>
        <el-button type="danger" size="small">删除</el-button>
      </template>
    </el-table-column>

  </el-table>
  <el-pagination background layout="prev, pager, next" :page-size="pagesize" :total="total"
                 @prev-click="toPage" @next-click="toPage" @current-change="toPage"/>
  <el-dialog v-model="UserdialogVisible" title="新增用户" center width="55%" draggable>
    <el-form ref="loginRefForm" :model="userQuery" :rules="userRules" label-width="110px">
      <el-form-item label="账号" prop="loginAct">
        <el-input v-model="userQuery.loginAct"/>
      </el-form-item>

      <el-form-item label="密码" prop="loginPwd">
        <el-input type="password" v-model="userQuery.loginPwd"/>
      </el-form-item>
      <el-form-item label="姓名" prop="name">
        <el-input v-model="userQuery.name"/>
      </el-form-item>
      <el-form-item label="手机" prop="phone">
        <el-input v-model="userQuery.phone"/>
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="userQuery.email"/>
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
      <el-button @click="UserdialogVisible = false">取消</el-button>
      <el-button type="primary" @click="userSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {defineComponent} from "vue";
import {doGet, doPost} from "../http/httpRequest.js";

export default defineComponent({
  name: "UserView",

  data() {
    return {
      userlist: [{}],
      pagesize: 0,
      total: 0,
      UserdialogVisible: false,
      // 登录表单
      userQuery: {},
      userRules: {
        loginAct: [
          {required: true, message: '请输入账号', trigger: 'blur'},
          {min: 3, max: 20, message: '账号长度应在3到20个字符之间', trigger: 'blur'}
        ],
        loginPwd: [
          {required: true, message: '请输入密码', trigger: 'blur'},
          {min: 6, max: 20, message: '密码长度应在6到20个字符之间', trigger: 'blur'}
        ],
        name: [
          {required: true, message: '请输入姓名', trigger: 'blur'},
          {min: 2, max: 20, message: '姓名长度应在2到20个字符之间', trigger: 'blur'}
        ],
        phone: [
          {required: true, message: '请输入手机号', trigger: 'blur'},
          {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur'}
        ],
        email: [
          {required: true, message: '请输入邮箱', trigger: 'blur'},
          {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
        ],
        credentialsNoExpired: [
          {required: true, message: '请选择密码是否未过期', trigger: 'change'}
        ],
        accountNoExpired: [
          {required: true, message: '请选择账号是否未过期', trigger: 'change'}
        ],
        accountNoLocked: [
          {required: true, message: '请选择账号是否未锁定', trigger: 'change'}
        ],
        accountEnabled: [
          {required: true, message: '请选择账号是否启用', trigger: 'change'}
        ]
      }
    }
  },
  mounted() {
    this.getData(1);
  },
  methods: {
    handleSelectionChange() {
    },

    getData(current) {
      doGet("/api/users", {current: current}).then(response => {
        if (response.data.code === 200) {
          console.log("获取用户列表成功:");
          // console.log(response.data.data.list);
          // 假设后端返回的数据在response.data中
          this.userlist = response.data.data.list;
          this.pagesize = response.data.data.pageSize;
          this.total = response.data.data.total;
        }
      }).catch(error => {
        console.error("获取用户列表失败:", error);
      });
    },
    toPage(current) {
      this.getData(current);
    },
    view(id) {
      console.log("查看用户详情:", id);
      this.$router.push({path: "/dashboard/user/" + id});
    },
    add() {
      this.UserdialogVisible = true;
    },
    userSubmit() {
      // 先进行表单验证
      this.$refs.loginRefForm.validate((valid) => {
        if (valid) {
          // 验证通过，提交表单
          let formData = new FormData();
          for (let key in this.userQuery) {
            formData.append(key, this.userQuery[key]);
          }
          doPost("/api/user", formData).then(response => {
            if (response.data.code === 200) {
              this.$message.success("新增用户成功");
              // 重置表单
              this.$refs.loginRefForm.resetFields();
              // 关闭对话框
              this.UserdialogVisible = false;
              // 刷新用户列表
              this.reload();
            } else {
              this.$message.error("新增用户失败");
            }
          }).catch(error => {
            console.error("新增用户失败:", error);
            this.$message.error("新增用户失败");
          });
        } else {
          // 验证不通过，提示用户
          this.$message.warning("请填写正确的表单信息");
          return false;
        }
      });
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