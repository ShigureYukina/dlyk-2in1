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
    <el-table
        ref="multipleTableRef"
        :data="activityRemarkList"
        style="width: 100%"
    >

      <el-table-column type="index" label="序号" width="60"/>
      <el-table-column property="noteContent" label="备注内容"/>
      <el-table-column property="createTime" label="备注时间"/>
      <el-table-column property="createByDO.name" label="备注人"/>
      <el-table-column property="editTime" label="编辑时间"/>
      <el-table-column property="editByDO.name" label="编辑人"/>
      <el-table-column label="操作" show-overflow-tooltip>
        <template #default="scope">
          <el-button size="small" @click="edit(scope.row.id)">编辑</el-button>
          <el-button type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>

    </el-table>
    <el-pagination background layout="prev, pager, next" :page-size="pageSize" :total="total"
                   @prev-click="toPage" @next-click="toPage" @current-change="toPage"/>

  </el-form>
  <el-dialog v-model="activityRemarkDialogVisible" title="编辑市场活动备注" center width="55%" draggable>
    <el-form ref="editActivityRemarkForm" :model="editaActivityRemarkQuery" :rules="activityRemarkRules"
             label-width="110px">
      <el-form-item label="活动备注" prop="noteContent">
        <el-input
            v-model="editaActivityRemarkQuery.noteContent"
            style="width: 100%"
            :autosize="{ minRows: 2, maxRows: 4 }"
            type="textarea"
            placeholder="请输入活动描述"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="activityRemarkDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="editActivityRemarkSubmit">保存</el-button>
    </template>
  </el-dialog>

</template>

<!--标签<script>，里面写javascript代码-->
<script>
import {defineComponent} from 'vue'
import {doDelete, doGet, doPost, doPut} from "../http/httpRequest.js";
import {goback, messageConfirm, messageTip} from "../util/util.js";

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

      activityRemarkQuery: {},
      editaActivityRemarkQuery: {},

      activityRemarkList: [{
        editByDO: {},
        createByDO: {},

      }],

      pageSize: 10,
      total: 0,

      //活动备注弹窗相关
      activityRemarkDialogVisible: false,
    }
  },
  mounted() {
    this.loadActivityDetail()
    this.loadActivityRemarkList(1)
  },
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
    }
    ,
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
    },

    loadActivityRemarkList(current) {
      doGet("/api/activity/remark", {
        current: current,
        activityId: this.$route.params.id,

        pageSize: this.pageSize
      }).then(response => {
        if (response.data.code === 200) {
          console.log("获取活动列表成功:");
          // console.log(response.data.data.list);
          // 假设后端返回的数据在response.data中
          this.activityRemarkList = response.data.data.list;
          this.pageSize = response.data.data.pageSize;
          this.total = response.data.data.total;
        }
      }).catch(error => {
        console.error("获取活动列表失败:", error);
      });
    },
    toPage(current) {
      this.loadActivityRemarkList(current);
    },
    //编辑备注
    edit(id) {
      this.activityRemarkDialogVisible = true;
      doGet("/api/activity/remark/" + id, {})
          .then(response => {
            if (response.data.code === 200) {
              this.editaActivityRemarkQuery = response.data.data;
            }
          });

    },
    editActivityRemarkSubmit() {
      this.$refs.editActivityRemarkForm.validate((valid) => {
        if (valid) {
          doPut("/api/activity/remark", {
            id: this.editaActivityRemarkQuery.id,
            noteContent: this.editaActivityRemarkQuery.noteContent
          }).then(response => {
            if (response.data.code === 200) {
              messageTip("编辑成功", "success");
              this.activityRemarkDialogVisible = false;
              this.reload();
            } else {
              messageTip("编辑失败", "error");
            }
          })
        }
      })
    },
    del(id) {
      messageConfirm("你确定要删除该备注吗？").then(() => {
        return doDelete("/api/activity/remark/" + id, {}).then(response => {
          if (response.data.code === 200) {
            messageTip("删除备注成功", "success");
            this.reload(); // 刷新
          } else {
            messageTip("删除备注失败", "error"); // 使用 messageTip 保持一致性
          }
        })
      });
    },

  }
})

</script>

<!--标签<style>，里面写css样式-->
<style scoped>
/* 在这里添加样式 */
</style>
