<template>
  <el-button type="primary" class="btn" @click="addClue">录入线索</el-button>
  <el-button type="success" class="btn" @click="importExcel">导入线索（Excel）</el-button>
  <el-button type="danger" class="btn" @click="batchDelClue">批量删除</el-button>
  <el-table
      :data="clueList"
      style="width: 100%"
      @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50"/>
    <el-table-column type="index" label="序号" width="65"/>
    <el-table-column property="ownerDO.name" label="负责人" width="120"/>
    <el-table-column property="activityDO.name" label="所属活动"/>
    <el-table-column label="姓名">
      <template #default="scope">
        <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.fullName }}</a>
      </template>
    </el-table-column>
    <el-table-column property="appellationDO.typeValue" label="称呼"/>
    <el-table-column property="phone" label="手机" width="120"/>
    <el-table-column property="weixin" label="微信" width="120"/>
    <el-table-column property="needLoanDO.typeValue" label="是否贷款"/>
    <el-table-column property="intentionStateDO.typeValue" label="意向状态"/>
    <el-table-column property="intentionProductDO.name" label="意向产品"/>

    <el-table-column property="stateDO.typeValue" label="线索状态">
      <template #default="scope">
        <span style="color: #409EFF" v-if="scope.row.state===-1">{{ scope.row.stateDO.typeValue }}</span>
        <span v-else>{{ scope.row.stateDO.typeValue }}</span>
      </template>
    </el-table-column>


    <el-table-column property="sourceDO.typeValue" label="线索来源"/>
    <el-table-column property="nextContactTime" label="下次联系时间" width="165"/>
    <el-table-column label="操作" width="230">
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)">详情</el-button>
        <el-button type="success" @click="edit(scope.row.id)">编辑</el-button>
        <el-button type="danger" @click="del(scope.row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="toPage"
        @next-click="toPage"
        @current-change="toPage"/>
  </p>
  <el-dialog v-model="importExcelDialogVisible"
             title="导入线索（Excel）"
             center width="55%" draggable>
    <el-upload
        ref="uploadRef"
        method="post"
        :http-request="uploadFile"
        :auto-upload="false"
    >
      <template #trigger>
        <el-button type="primary">选择Excel文件</el-button>
      </template>
      仅支持.xlsx或.xls格式文件

      <template #tip>
        <div class="fileTip">
          重要提示
          <ul>
            <li>请不要上传超过50M的Excel文件</li>
            <li>给定Excel文件中，第一行视为字段名</li>
            <li>日期值以文本形式保存，必须为yyyy-MM-dd</li>
            <li>日期时间值以文本形式保存，必须为yyyy-MM-dd HH:mm:ss</li>
          </ul>
        </div>
      </template>
    </el-upload>

    <template #footer>
      <el-button @click="importExcelDialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="submitFile">上传</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {defineComponent} from "vue";
import {doGet, doPost} from "../http/httpRequest.js";
import {messageTip} from "../util/util.js";

export default defineComponent({
  name: 'ClueView',
  inject: ['reload'],
  data() {
    return {
      clueList: [],
      selected: [],
      pageSize: 0,
      total: 0,
      importExcelDialogVisible: false,
    }
  },
  mounted() {
    this.getData(1)
  },
  methods: {
    addClue() {
      this.$router.push('/dashboard/clue/add')
    },
    importExcel() {
      this.importExcelDialogVisible = true;
    },
    batchDelClue() {
      // 批量删除逻辑
    },
    handleSelectionChange(val) {
      this.selected = val
    },
    submitFile() {
      this.$refs.uploadRef.submit()
    },
    view(id) {
      this.$router.push('/dashboard/clue/detail/' + id)
    },
    edit(id) {
      this.$router.push('/dashboard/clue/edit/' + id)
    },
    del(id) {
      this.$confirm('确认删除该线索？').then(() => {
        // 删除逻辑
      }).catch(() => {
      });
    },
    getData(current) {
      doGet("/api/clues", {current: current}).then(response => {
        if (response.data.code === 200) {
          this.clueList = response.data.data.list.map(item => ({
            ...item,
            ownerDO: item.ownerDO || {},
            activityDO: item.activityDO || {},
            appellationDO: item.appellationDO || {},
            needLoanDO: item.needLoanDO || {},
            intentionStateDO: item.intentionStateDO || {},
            intentionProductDO: item.intentionProductDO || {},
            stateDO: item.stateDO || {},
            sourceDO: item.sourceDO || {},
          }));
          this.pageSize = response.data.data.pageSize;
          this.total = response.data.data.total;
        }
      }).catch(error => {
        console.error("获取线索列表失败:", error);
      });
    },
    toPage(current) {
      this.getData(current);
    },
    uploadFile(param) {
      let file = param.file;
      let formData = new FormData();
      formData.append("file", file);
      doPost("/api/importExcel", formData,).then(response => {
        if (response.data.code === 200) {
          messageTip("导入成功", "success")
          this.$refs.uploadRef.clearFiles();
          this.reload();
        } else {
          this.$message.error("导入失败：" + "error");
        }
      })
    }
  },
})
</script>

<style scoped>

.el-table {
  margin-top: 20px;
}

.fileTip {
  padding-top: 25px;
}

/* 你的样式 */
</style>
