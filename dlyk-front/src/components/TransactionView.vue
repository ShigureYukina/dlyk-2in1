<template>
  <div class="transaction-container">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-input
            v-model="searchForm.tranNo"
            placeholder="请输入交易编号"
            clearable
            prefix-icon="Search"
          />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-select
            v-model="searchForm.stage"
            placeholder="请选择交易阶段"
            clearable
            style="width: 100%"
          >
            <el-option label="初步洽谈" :value="1" />
            <el-option label="方案确认" :value="2" />
            <el-option label="合同签署" :value="3" />
            <el-option label="定金支付" :value="4" />
            <el-option label="全款支付" :value="5" />
            <el-option label="产品交付" :value="6" />
            <el-option label="交易完成" :value="7" />
            <el-option label="交易失败" :value="8" />
          </el-select>
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
          <el-button type="info" icon="Refresh" @click="refreshData">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
          :data="tranList"
          style="width: 100%"
          v-loading="loading"
          stripe
          border>
        <el-table-column type="index" label="序号" width="65" fixed="left"/>
        <el-table-column property="tranNo" label="交易编号" width="150" show-overflow-tooltip/>
        <el-table-column property="customerDO.clueDO.fullName" label="客户姓名" width="120" show-overflow-tooltip/>
        <el-table-column property="ownerDO.name" label="负责人" width="120" show-overflow-tooltip/>
        <el-table-column label="交易金额" width="120">
          <template #default="scope">
            <span style="color: #f56c6c; font-weight: bold;">{{ formatMoney(scope.row.money) }}</span>
          </template>
        </el-table-column>
        <el-table-column property="expectedDate" label="预计成交日期" width="150"/>
        <el-table-column label="交易阶段" width="120">
          <template #default="scope">
            <el-tag :type="getStageType(scope.row.stage)">{{ getStageText(scope.row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column property="description" label="交易描述" show-overflow-tooltip/>
        <el-table-column property="createTime" label="创建时间" width="180"/>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" icon="View" @click="view(scope.row.id)">详情</el-button>
            <el-button type="warning" size="small" icon="Edit" @click="updateStage(scope.row)">更新阶段</el-button>
            <el-button type="success" size="small" icon="ChatDotRound" @click="addRemark(scope.row.id)">添加备注</el-button>
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

    <!-- 更新阶段对话框 -->
    <el-dialog v-model="stageDialogVisible" title="更新交易阶段" width="500px">
      <el-form :model="stageForm" label-width="100px">
        <el-form-item label="交易编号">
          <el-input v-model="stageForm.tranNo" disabled/>
        </el-form-item>
        <el-form-item label="当前阶段">
          <el-tag>{{ getStageText(stageForm.currentStage) }}</el-tag>
        </el-form-item>
        <el-form-item label="新阶段" required>
          <el-select v-model="stageForm.stage" placeholder="请选择新阶段" style="width: 100%">
            <el-option label="初步洽谈" :value="1" />
            <el-option label="方案确认" :value="2" />
            <el-option label="合同签署" :value="3" />
            <el-option label="定金支付" :value="4" />
            <el-option label="全款支付" :value="5" />
            <el-option label="产品交付" :value="6" />
            <el-option label="交易完成" :value="7" />
            <el-option label="交易失败" :value="8" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易金额">
          <el-input-number v-model="stageForm.money" :precision="2" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="预计成交">
          <el-date-picker
            v-model="stageForm.expectedDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStageUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { doGet, doPost, doPut } from "../http/httpRequest.js";
import { messageTip, messageConfirm } from "../util/util.js";

export default {
  name: "TransactionView",
  data() {
    return {
      tranList: [],
      pageSize: 10,
      total: 0,
      currentPage: 1,
      loading: false,
      searchForm: {
        tranNo: '',
        stage: ''
      },
      stageDialogVisible: false,
      stageForm: {
        id: null,
        tranNo: '',
        currentStage: '',
        stage: '',
        money: 0,
        expectedDate: ''
      }
    }
  },
  mounted() {
    this.getData(1);
  },
  methods: {
    // 获取数据
    getData(current) {
      this.loading = true;
      const params = {
        current: current,
        pageSize: this.pageSize,
        ...this.searchForm
      };
      
      doGet("/api/trans", params).then(response => {
        if (response.data.code === 200) {
          this.tranList = response.data.data.list.map(item => ({
            ...item,
            customerDO: item.customerDO || { clueDO: {} },
            ownerDO: item.ownerDO || {}
          }));
          this.pageSize = response.data.data.pageSize;
          this.total = response.data.data.total;
          this.currentPage = current;
        }
      }).catch(error => {
        console.error("获取交易列表失败:", error);
        messageTip("获取数据失败", "error");
      }).finally(() => {
        this.loading = false;
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
        tranNo: '',
        stage: ''
      };
      this.searchData();
    },

    // 刷新数据
    refreshData() {
      this.getData(this.currentPage);
      messageTip("刷新成功", "success");
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

    // 查看详情
    view(id) {
      this.$router.push("/dashboard/transaction/detail/" + id);
    },

    // 更新阶段
    updateStage(tran) {
      this.stageForm = {
        id: tran.id,
        tranNo: tran.tranNo,
        currentStage: tran.stage,
        stage: '',
        money: tran.money || 0,
        expectedDate: tran.expectedDate || ''
      };
      this.stageDialogVisible = true;
    },

    // 提交阶段更新
    submitStageUpdate() {
      if (!this.stageForm.stage) {
        messageTip("请选择新阶段", "warning");
        return;
      }

      const updateData = {
        id: this.stageForm.id,
        stage: this.stageForm.stage,
        money: this.stageForm.money,
        expectedDate: this.stageForm.expectedDate
      };

      doPut("/api/tran/stage", updateData).then(response => {
        if (response.data.code === 200) {
          messageTip("阶段更新成功", "success");
          this.stageDialogVisible = false;
          this.getData(this.currentPage);
        } else {
          messageTip("阶段更新失败：" + response.data.msg, "error");
        }
      }).catch(error => {
        messageTip("阶段更新失败", "error");
      });
    },

    // 添加备注
    addRemark(tranId) {
      this.$router.push("/dashboard/transaction/detail/" + tranId + "?tab=remark");
    },

    // 格式化金额
    formatMoney(money) {
      if (!money) return '¥0.00';
      return '¥' + Number(money).toLocaleString('zh-CN', { minimumFractionDigits: 2 });
    },

    // 获取阶段类型
    getStageType(stage) {
      const stageMap = {
        1: 'info',
        2: 'warning',
        3: 'primary',
        4: 'warning',
        5: 'danger',
        6: 'success',
        7: 'success',
        8: 'danger'
      };
      return stageMap[stage] || 'info';
    },

    // 获取阶段文本
    getStageText(stage) {
      const stageMap = {
        1: '初步洽谈',
        2: '方案确认',
        3: '合同签署',
        4: '定金支付',
        5: '全款支付',
        6: '产品交付',
        7: '交易完成',
        8: '交易失败'
      };
      return stageMap[stage] || '未知阶段';
    }
  }
}
</script>

<style scoped>
.transaction-container {
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

/* 响应式设计 */
@media (max-width: 1200px) {
  .transaction-container {
    padding: 15px;
  }
}

@media (max-width: 992px) {
  .transaction-container {
    padding: 10px;
  }
  
  .operation-card .el-col {
    margin-bottom: 10px;
  }
}

@media (max-width: 768px) {
  .transaction-container {
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

/* 加载动画优化 */
.el-loading-mask {
  background-color: rgba(255, 255, 255, 0.8);
}
</style>