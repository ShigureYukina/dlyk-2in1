<!--标签<template>，里面写html页面要展示的内容-->
<template>
  <el-form ref="activityRemark" :model="activityRemarkQuery" label-width="120px" :rules="activityRemarkRules">
    <el-form-item label="ID">
      {{ activityDetail.id }}
    </el-form-item>

    <el-form-item label="负责人">
      {{ activityDetail.ownerDO.name }}
    </el-form-item>

    <el-form-item label="活动名称">
      {{ activityDetail.name }}
    </el-form-item>

    <el-form-item label="开始时间">
      {{ activityDetail.startTime }}
    </el-form-item>

    <el-form-item label="结束时间">
      {{ activityDetail.endTime }}
    </el-form-item>
    <el-form-item label="活动预算">
      {{ activityDetail.cost }}
    </el-form-item>
    <el-form-item label="活动描述">
      {{ activityDetail.description }}
    </el-form-item>

    <el-form-item label="创建时间">
      {{ activityDetail.createTime }}
    </el-form-item>
    <el-form-item label="创建人">
      {{ activityDetail.createByDO.name }}
    </el-form-item>

    <el-form-item label="编辑时间">
      {{ activityDetail.editTime }}
    </el-form-item>
    <el-form-item label="编辑人">
      {{ activityDetail.editByDO.name }}
    </el-form-item>
    <el-form-item label="填写备注" prop="noteContent">
      <el-input
          v-model="activityRemarkQuery.noteContent"
          style="width: 100%;"
          :rows="8"
          type="textarea"
          placeholder="请输入活动备注"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="activitysubmit">提交</el-button>
      <el-button @click="goback">返回</el-button>
    </el-form-item>

  </el-form>
</template>

<!--标签<script>，里面写javascript代码-->
<script>
import {defineComponent} from 'vue'
import {doGet, doPost} from "../http/httpRequest.js";
import {goback, messageTip} from "../util/util.js";

export default defineComponent({
  name: 'ActivityDetailView',
  data() {
    return {
      activityDetail: {
        ownerDO: '',
        editByDO: '',
        createByDO: '',
      },
      activityRemarkRules: {
        noteContent: [
          {required: true, message: '请填写备注内容', trigger: 'blur'},
          {min: 5, max: 255, message: '长度在 5 到 255 个字符', trigger: 'blur'}
        ]
      },
      activityRemarkQuery: {}
    }
  },
  mounted() {
    this.loadActivityDetail()
  }
  ,
  methods: {
    goback,
    loadActivityDetail() {
      const id = this.$route.params.id;
      doGet("api/activity/" + id, {})
          .then(response => {
            this.activityDetail = response.data.data;
            if (!this.activityDetail.ownerDO) {
              this.activityDetail.ownerDO = {};
            }
            if (!this.activityDetail.editByDO) {
              this.activityDetail.editByDO = {};
            }
            if (!this.activityDetail.createByDO) {
              this.activityDetail.createByDO = {};
            }
          });
    },
    activitysubmit() {
      this.$refs.activityRemark.validate((valid) => {
        if (valid) {
          doPost("/api/activity/remark", {
            activityId: this.$route.params.id,
            noteContent: this.activityRemarkQuery.noteContent
          }).then(response => {
            if (response.data.code === 200) {
              messageTip("提交成功", "success");
              // 提交成功后清空输入框
              this.activityRemarkQuery.noteContent = '';
            } else {
              messageTip("提交失败", "error");
            }
          }).catch(error => {
            // 增加错误处理机制
            console.error("网络请求失败:", error);
            messageTip("网络请求失败，请稍后再试", "error");
          });
        } else {
          // 虽然在这个上下文中不是必须的，但可以增加对表单验证失败时的提示
          messageTip("表单验证失败，请检查填写内容", "warning");
        }
      });
    }

  }
})

</script>

<!--标签<style>，里面写css样式-->
<style scoped>
/* 在这里添加样式 */
</style>
