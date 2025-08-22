<template>
  <el-form ref="activityRefForm" :model="activityQuery" :rules="activityRules" label-width="110px">
    <el-form-item label="负责人" prop="ownerId" class="width">
      <el-select
          v-model="activityQuery.ownerId"
          placeholder="请选择负责人"
          @click="loadOwner"
          clearable
      >
        <el-option v-for="item in ownerOptions"
                   :key="item.id"
                   :label="item.name"
                   :value="item.id"/>
      </el-select>
    </el-form-item>
    <el-form-item label="活动名称" prop="name">
      <el-input v-model="activityQuery.name"/>
    </el-form-item>
    <el-form-item label="开始时间">
      <el-date-picker
          v-model="activityQuery.startTime"
          type="datetime"
          placeholder="请选择创建时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width:100%"
      />
    </el-form-item>
    <el-form-item label="结束时间">
      <el-date-picker
          v-model="activityQuery.endTime"
          type="datetime"
          placeholder="请选择创建时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width:100%"
      />
    </el-form-item>
    <el-form-item label="活动预算" prop="cost">
      <el-input v-model="activityQuery.cost"/>
    </el-form-item>
    <el-form-item label="活动描述" prop="description">
      <el-input
          v-model="activityQuery.description"
          style="width: 240px"
          :autosize="{ minRows: 2, maxRows: 4 }"
          type="textarea"
          placeholder="请输入活动描述"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">提交</el-button>
      <el-button @click="goback">返回</el-button>
    </el-form-item>
  </el-form>

</template>

<script>
import {doGet, doPost, doPut} from "../http/httpRequest.js";
import {messageTip} from "../util/util.js";

export default {
  inject: ['reload'],
  data() {
    return {
      activityQuery: {
        ownerId: '',
        name: '',
        startTime: '',
        endTime: '',
        cost: '',
        description: ''
      },
      activityRules: {
        ownerId: [
          {required: true, message: '请输入负责人', trigger: 'blur'}
        ],
        name: [
          {required: true, message: '请输入活动名称', trigger: 'blur'}
        ],
        startTime: [
          {required: true, message: '请选择开始时间', trigger: 'change'}
        ],
        endTime: [
          {required: true, message: '请选择结束时间', trigger: 'change'}
        ],
        cost: [
          {
            required: true,
            pattern: /^[0-9]+(\.[0-9]{2})*?$/,
            message: '活动预算必须是整数或最多两位小数',
            trigger: 'blur'
          }
        ],
        description: [
          {required: true, message: '请输入活动描述', trigger: 'blur'},
          {min: 5, max: 255, message: '活动描述不能超过200字', trigger: 'blur'}
        ]
      },
      ownerOptions: [{}],
    }
  },
  mounted() {
    this.loadOwner();
    if (this.$route.params.id) {
      this.loadActivity();
    }
    //判断
  },
  methods: {
    loadOwner() {
      doGet("/api/owner", {}).then(response => {
        if (response.data.code === 200) {
          this.ownerOptions = response.data.data;
        } else {
          messageTip("加载负责人失败", "error");
        }
      }).catch(error => {
        console.error("请求负责人列表时发生错误:", error);
        messageTip("请求负责人列表时发生错误", "error");
      });
    },
    goback() {
      this.$router.go(-1);
    },
    loadActivity() {
      let id = this.$route.params.id;
      if (id) {
        doGet("/api/activity/" + id, {}).then(response => {
          if (response.data.code === 200) {
            this.activityQuery = response.data.data;
          }
        })
      }
    },
    submit() {
      // 先进行表单验证
      this.$refs.activityRefForm.validate((valid) => {
        if (valid) {
          // 验证通过，提交表单
          let formData = new FormData();
          for (let key in this.activityQuery) {
            if (this.activityQuery[key]) {
              formData.append(key, this.activityQuery[key]);
            }
          }

          // 根据 id 判断是编辑还是新增
          const apiUrl = this.activityQuery.id > 0 ? "/api/activity" : "/api/activity"; // 确保 URL 正确
          const request = this.activityQuery.id > 0 ? doPut(apiUrl, formData) : doPost(apiUrl, formData);

          request.then(response => {
            if (response.data.code === 200) {
              const actionMessage = this.activityQuery.id > 0 ? "编辑活动成功" : "新增活动成功";
              this.$message.success(actionMessage);

              // 重置表单
              this.$refs.activityRefForm.resetFields();
              this.$router.go(-1);

            } else {
              const errorMessage = this.activityQuery.id > 0 ? "编辑活动失败" : "新增活动失败";
              this.$message.error(errorMessage);
            }
          }).catch(error => {
            console.error(this.activityQuery.id > 0 ? "编辑活动失败:" : "新增活动失败:", error);
            this.$message.error(this.activityQuery.id > 0 ? "编辑活动失败" : "新增活动失败");
          });
        } else {
          // 验证不通过，提示活动
          this.$message.warning("请填写正确的表单信息");
        }
      });
    }

  },
}
</script>


<style scoped>
.width {
  width: 100%;
}
</style>