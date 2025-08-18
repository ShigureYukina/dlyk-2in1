<template>
  <el-form ref="loginRefForm" :model="user" label-width="120px" :rules="loginRules">
    <el-form-item label="ID">
      {{ userdetail.id }}
    </el-form-item>

    <el-form-item label="账号">
      {{ userdetail.loginAct }}
    </el-form-item>

    <el-form-item label="姓名">
      {{ userdetail.name }}
    </el-form-item>

    <el-form-item label="邮箱">
      {{ userdetail.email }}
    </el-form-item>

    <el-form-item label="电话">
      {{ userdetail.phone }}
    </el-form-item>

    <el-form-item label="账号启用">
      <el-tag v-if="userdetail.accountEnabled == 1" type="success">启用</el-tag>
      <el-tag v-else type="danger">禁用</el-tag>
    </el-form-item>

    <el-form-item label="账号未过期">
      <el-tag v-if="userdetail.accountNoExpired == 1" type="success">未过期</el-tag>
      <el-tag v-else type="danger">已过期</el-tag>
    </el-form-item>

    <el-form-item label="账号未锁定">
      <el-tag v-if="userdetail.accountNoLocked == 1" type="success">未锁定</el-tag>
      <el-tag v-else type="danger">已锁定</el-tag>
    </el-form-item>

    <el-form-item label="凭证未过期">
      <el-tag v-if="userdetail.credentialsNoExpired == 1" type="success">未过期</el-tag>
      <el-tag v-else type="danger">已过期</el-tag>
    </el-form-item>

    <el-form-item label="创建时间">
      {{ userdetail.createTime }}
    </el-form-item>

    <el-form-item label="编辑时间">
      {{ userdetail.editTime }}
    </el-form-item>

    <el-form-item label="最后登录时间">
      {{ userdetail.lastLoginTime }}
    </el-form-item>
  </el-form>
</template>
<script>
import {defineComponent} from "vue";
import {doGet} from "../http/httpRequest.js";

export default defineComponent({
  name: "UserDetailView",
  data() {
    return {
      userdetail: {}
    }
  },
  mounted() {
    this.loadUserDetail()
  },
  methods: {
    loadUserDetail() {
      let id = this.$route.params.id;
      doGet('/api/user/', {id: id}).then(res => {
        console.log(res.data.data);
        this.userdetail = res.data.data;
      });
    }
  }
})

</script>
<style scoped>

</style>