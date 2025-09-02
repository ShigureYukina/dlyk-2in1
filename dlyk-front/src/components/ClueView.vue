<template>
  <div class="clue-container">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入姓名"
            clearable
            prefix-icon="Search"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-input
            v-model="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            prefix-icon="Phone"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-button type="primary" @click="searchData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 操作区域 -->
    <el-card class="operation-card" shadow="never">
      <el-row :gutter="10">
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="primary" icon="Plus" @click="addClue">录入线索</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="success" icon="Upload" @click="importExcel">导入线索</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="warning" icon="Download" @click="exportClue">导出线索</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="danger" icon="Delete" @click="batchDelClue" :disabled="selected.length === 0">批量删除</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
          :data="clueList"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          v-loading="loading"
          stripe
          border>
        <el-table-column type="selection" width="50" fixed="left"/>
        <el-table-column type="index" label="序号" width="65" fixed="left"/>
        <el-table-column property="ownerDO.name" label="负责人" width="120" show-overflow-tooltip/>
        <el-table-column property="activityDO.name" label="所属活动" width="150" show-overflow-tooltip/>
        <el-table-column label="姓名" width="120">
          <template #default="scope">
            <el-link type="primary" @click="view(scope.row.id)">{{ scope.row.fullName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column property="appellationDO.typeValue" label="称呼" width="80"/>
        <el-table-column property="phone" label="手机" width="130" show-overflow-tooltip/>
        <el-table-column property="weixin" label="微信" width="130" show-overflow-tooltip/>
        <el-table-column property="needLoanDO.typeValue" label="是否贷款" width="100"/>
        <el-table-column property="intentionStateDO.typeValue" label="意向状态" width="120"/>
        <el-table-column property="intentionProductDO.name" label="意向产品" width="150" show-overflow-tooltip/>
        <el-table-column property="stateDO.typeValue" label="线索状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.state === -1 ? 'success' : 'info'">{{ scope.row.stateDO.typeValue }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column property="sourceDO.typeValue" label="线索来源" width="120"/>
        <el-table-column property="nextContactTime" label="下次联系时间" width="180"/>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" icon="View" @click="view(scope.row.id)">详情</el-button>
            <el-button type="warning" size="small" icon="Edit" @click="edit(scope.row.id)" :disabled="scope.row.state === -1">编辑</el-button>
            <el-button type="success" size="small" icon="Promotion" @click="convertToCustomer(scope.row.id)" :disabled="scope.row.state === -1">转客户</el-button>
            <el-button type="danger" size="small" icon="Delete" @click="del(scope.row.id)" :disabled="scope.row.state === -1">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pageSize"
            :total="total"
            :current-page="currentPage"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"/>
      </div>
    </el-card>
  </div>
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
import {doDelete, doGet, doPost} from "../http/httpRequest.js";
import {messageConfirm, messageTip, getToken} from "../util/util.js";
import axios from "axios";

export default defineComponent({
  name: 'ClueView',
  inject: ['reload'],
  data() {
    return {
      clueList: [],
      selected: [],
      pageSize: 10,
      total: 0,
      currentPage: 1,
      loading: false,
      importExcelDialogVisible: false,
      searchForm: {
        name: '',
        phone: ''
      }
    }
  },
  mounted() {
    this.getData(1)
  },
  methods: {
    // 新增线索
    addClue() {
      this.$router.push('/dashboard/clue/add')
    },
    
    // 导入Excel
    importExcel() {
      this.importExcelDialogVisible = true;
    },
    
    // 导出Excel
    exportClue() {
      if (this.selected.length > 0) {
        // 选择导出
        const ids = this.selected.map(item => item.id).join(',');
        this.downloadExcel('/api/clue/export/selected', {ids});
      } else {
        // 全部导出
        this.downloadExcel('/api/clue/export');
      }
    },
    
    // 下载Excel文件
    downloadExcel(url, params = {}) {
      const token = getToken();
      let iframe = document.createElement("iframe");
      const queryString = new URLSearchParams({
        Authorization: token,
        ...params
      }).toString();
      iframe.src = `${axios.defaults.baseURL}${url}?${queryString}`;
      iframe.style.display = 'none';
      document.body.appendChild(iframe);
      
      setTimeout(() => {
        document.body.removeChild(iframe);
      }, 3000);
    },
    
    // 批量删除
    batchDelClue() {
      if (this.selected.length === 0) {
        messageTip("请选择要删除的线索", "warning");
        return;
      }
      
      messageConfirm(`确定删除选中的 ${this.selected.length} 条线索吗？`).then(() => {
        const ids = this.selected.map(item => item.id);
        doDelete("/api/clues", ids).then(response => {
          if (response.data.code === 200) {
            messageTip("删除成功", "success");
            this.getData(this.currentPage);
            this.selected = [];
          } else {
            messageTip("删除失败：" + response.data.msg, "error");
          }
        }).catch(error => {
          messageTip("删除失败", "error");
        });
      });
    },
    
    // 选择变化
    handleSelectionChange(val) {
      this.selected = val;
    },
    
    // 上传文件
    submitFile() {
      this.$refs.uploadRef.submit();
    },
    
    // 查看详情
    view(id) {
      this.$router.push('/dashboard/clue/detail/' + id);
    },
    
    // 编辑
    edit(id) {
      this.$router.push('/dashboard/clue/edit/' + id);
    },
    
    // 转换为客户
    convertToCustomer(id) {
      messageConfirm("确定将此线索转换为客户吗？").then(() => {
        doPost(`/api/clue/convert/${id}`).then(response => {
          if (response.data.code === 200) {
            messageTip("转换成功", "success");
            this.getData(this.currentPage);
          } else {
            messageTip("转换失败：" + response.data.msg, "error");
          }
        }).catch(error => {
          messageTip("转换失败", "error");
        });
      });
    },
    
    // 删除
    del(id) {
      messageConfirm("确定删除该线索吗？").then(() => {
        doDelete("/api/clue/" + id).then(response => {
          if (response.data.code === 200) {
            messageTip("删除成功", "success");
            this.getData(this.currentPage);
          } else {
            messageTip("删除失败：" + response.data.msg, "error");
          }
        }).catch(error => {
          messageTip("删除失败", "error");
        });
      });
    },
    
    // 搜索数据
    searchData() {
      this.currentPage = 1;
      this.getData(1);
    },
    
    // 重置搜索
    resetSearch() {
      this.searchForm = {
        name: '',
        phone: ''
      };
      this.searchData();
    },
    
    // 获取数据
    getData(current) {
      this.loading = true;
      const params = {
        current: current,
        pageSize: this.pageSize,
        ...this.searchForm
      };
      
      doGet("/api/clues", params).then(response => {
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
          this.currentPage = current;
        }
      }).catch(error => {
        console.error("获取线索列表失败:", error);
        messageTip("获取数据失败", "error");
      }).finally(() => {
        this.loading = false;
      });
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pageSize = size;
      this.getData(1);
    },
    
    // 当前页变化
    handleCurrentChange(current) {
      this.getData(current);
    },
    
    // 兼容旧版本
    toPage(current) {
      this.getData(current);
    },
    
    // 上传文件处理
    uploadFile(param) {
      let file = param.file;
      let formData = new FormData();
      formData.append("file", file);
      doPost("/api/importExcel", formData).then(response => {
        if (response.data.code === 200) {
          messageTip("导入成功", "success");
          this.$refs.uploadRef.clearFiles();
          this.importExcelDialogVisible = false;
          this.reload();
        } else {
          messageTip("导入失败：" + response.data.msg, "error");
        }
      }).catch(error => {
        messageTip("导入失败", "error");
      });
    }
  },
})
</script>

<style scoped>
.clue-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.search-card {
  margin-bottom: 20px;
}

.search-card .el-row {
  align-items: center;
}

.operation-card {
  margin-bottom: 20px;
}

.table-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 20px 0;
}

.el-table {
  font-size: 14px;
}

.el-table .el-table__header th {
  background-color: #fafafa;
  color: #333;
  font-weight: 600;
}

.el-table .el-table__row:hover {
  background-color: #f5f7fa;
}

.fileTip {
  padding-top: 25px;
  color: #666;
}

.fileTip ul {
  padding-left: 20px;
}

.fileTip li {
  margin-bottom: 8px;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .clue-container {
    padding: 15px;
  }
}

@media (max-width: 992px) {
  .clue-container {
    padding: 10px;
  }
  
  .operation-card .el-col {
    margin-bottom: 10px;
  }
}

@media (max-width: 768px) {
  .clue-container {
    padding: 10px 5px;
  }
  
  .search-card .el-col,
  .operation-card .el-col {
    margin-bottom: 10px;
  }
  
  .el-table {
    font-size: 12px;
  }
  
  .pagination-container {
    overflow-x: auto;
  }
}

@media (max-width: 576px) {
  .el-button {
    padding: 8px 12px;
    font-size: 12px;
  }
  
  .el-table .el-button {
    padding: 4px 8px;
    font-size: 11px;
  }
}

/* 动画效果 */
.el-card {
  transition: all 0.3s ease;
}

.el-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.el-button {
  transition: all 0.3s ease;
}

/* 状态颜色 */
.status-converted {
  color: #67C23A;
  font-weight: 600;
}

.status-normal {
  color: #909399;
}

/* 加载动画优化 */
.el-loading-mask {
  background-color: rgba(255, 255, 255, 0.8);
}
</style>
