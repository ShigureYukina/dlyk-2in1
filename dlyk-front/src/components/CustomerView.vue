<template>
  <div class="customer-container">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入客户姓名"
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
          <el-button type="primary" icon="Download" @click="batchExportExcel">批量导出</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="success" icon="Download" @click="chooseExportExcel" :disabled="customerIdArray.length === 0">选择导出</el-button>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="6">
          <el-button type="info" icon="Refresh" @click="refreshData">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 表格区域 -->
    <el-card class="table-card" shadow="never">
      <el-table
          :data="customerList"
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
            <el-link type="primary" @click="view(scope.row.id)">{{ scope.row.clueDO.fullName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column property="appellationDO.typeValue" label="称呼" width="80"/>
        <el-table-column property="clueDO.phone" label="手机" width="130" show-overflow-tooltip/>
        <el-table-column property="clueDO.weixin" label="微信" width="130" show-overflow-tooltip/>
        <el-table-column property="needLoanDO.typeValue" label="是否贷款" width="100"/>
        <el-table-column property="intentionStateDO.typeValue" label="意向状态" width="120"/>
        <el-table-column property="stateDO.typeValue" label="线索状态" width="120">
          <template #default="scope">
            <el-tag type="success">{{ scope.row.stateDO.typeValue }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column property="sourceDO.typeValue" label="线索来源" width="120"/>
        <el-table-column property="intentionProductDO.name" label="意向产品" width="150" show-overflow-tooltip/>
        <el-table-column property="nextContactTime" label="下次联系时间" width="180"/>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" icon="View" @click="view(scope.row.id)">详情</el-button>
            <el-button type="success" size="small" icon="Plus" @click="createTransaction(scope.row.id)">创建交易</el-button>
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
</template>

<script>
import axios from "axios";
import {doGet, doPost} from "../http/httpRequest";
import {getToken, messageTip, messageConfirm} from "../util/util.js";

export default {
  name: "CustomerView",
  data() {
    return {
      //客户列表对象，初始值是空
      customerList: [],
      //分页时每页显示多少条数据
      pageSize: 10,
      //总共有多少条
      total: 0,
      //当前页
      currentPage: 1,
      //加载状态
      loading: false,
      //定义一个customerId的数组，里面存放勾选的那些客户id，初始值是空
      customerIdArray: [],
      //搜索表单
      searchForm: {
        name: '',
        phone: ''
      }
    }
  },
  mounted() {
    this.getData(1);
  },
  methods: {
    //获取客户分页列表数据
    getData(current) {
      this.loading = true;
      const params = {
        current: current,
        pageSize: this.pageSize,
        ...this.searchForm
      };
      
      doGet("/api/customers", params).then(resp => {
        if (resp.data.code === 200) {
          this.customerList = resp.data.data.list.map(item => ({
            ...item,
            clueDO: item.clueDO || {},
            ownerDO: item.ownerDO || {},
            activityDO: item.activityDO || {},
            appellationDO: item.appellationDO || {},
            needLoanDO: item.needLoanDO || {},
            intentionStateDO: item.intentionStateDO || {},
            stateDO: item.stateDO || {},
            sourceDO: item.sourceDO || {},
            intentionProductDO: item.intentionProductDO || {}
          }));
          this.pageSize = resp.data.data.pageSize;
          this.total = resp.data.data.total;
          this.currentPage = current;
        }
      }).catch(error => {
        console.error("获取客户列表失败:", error);
        messageTip("获取数据失败", "error");
      }).finally(() => {
        this.loading = false;
      });
    },

    //搜索数据
    searchData() {
      this.currentPage = 1;
      this.getData(1);
    },

    //重置搜索
    resetSearch() {
      this.searchForm = {
        name: '',
        phone: ''
      };
      this.searchData();
    },

    //刷新数据
    refreshData() {
      this.getData(this.currentPage);
      messageTip("刷新成功", "success");
    },

    //分页大小变化
    handleSizeChange(size) {
      this.pageSize = size;
      this.getData(1);
    },

    //当前页变化
    handleCurrentChange(current) {
      this.getData(current);
    },

    //兼容旧版本分页函数
    page(number) {
      this.getData(number);
    },

    exportExcel(ids) {
      let token = getToken();
      //1、点一下批量导出按钮，向后端发个请求（我们写这个代码，使用iframe发一个请求）
      let iframe = document.createElement("iframe");
      if (ids) {
        iframe.src = axios.defaults.baseURL + "/api/exportExcel?Authorization=" + token + "&ids=" + ids;
      } else {
        iframe.src = axios.defaults.baseURL + "/api/exportExcel?Authorization=" + token;
      }
      iframe.style.display = 'none';
      document.body.appendChild(iframe);
      
      setTimeout(() => {
        document.body.removeChild(iframe);
      }, 3000);

      //2、后端就给前端返回一个Excel文件（我们写这个代码，在后端项目中去写）
      //3、浏览器接收到该Excel文件后弹出下载框进行下载（这一步是浏览器做的，我们不需要管）
    },

    //批量导出
    batchExportExcel() {
      this.exportExcel(null);
      messageTip("导出任务已提交", "success");
    },

    //选择导出
    chooseExportExcel() {
      if (this.customerIdArray.length <= 0) {
        messageTip("请选择要导出的数据", "warning");
        return;
      }
      let ids = this.customerIdArray.join(","); // ids = "2,6,11,12,18"
      this.exportExcel(ids);
      messageTip("导出任务已提交", "success");
    },

    //当选择项发生变化时会触发该函数，也就是勾选或者取消勾选checkbox的时候触发该函数
    handleSelectionChange(dataObjectArray) {
      //触发该函数的时候，把原来数组里面的id清除掉
      this.customerIdArray = [];
      //循环这个数据对象的数据，把数组里面的每个对象的id取出来
      dataObjectArray.forEach(dataObject => {
        let dataId = dataObject.id; //这个数据id就是我们每一行user对象的id
        this.customerIdArray.push(dataId);
      })
    },

    //查看客户详情
    view(id) {
      this.$router.push("/dashboard/customer/detail/" + id);
    },

    //创建交易
    createTransaction(customerId) {
      messageConfirm("确定为该客户创建交易吗？").then(() => {
        const tranData = {
          customerId: customerId,
          stage: '01', // 初步洽谈
          description: '由客户管理页面创建'
        };
        
        doPost(`/api/customer/${customerId}/transaction`, tranData).then(response => {
          if (response.data.code === 200) {
            messageTip("创建交易成功", "success");
            // 可以选择跳转到交易详情页
            this.$router.push('/dashboard/transaction/list');
          } else {
            messageTip("创建交易失败：" + response.data.msg, "error");
          }
        }).catch(error => {
          messageTip("创建交易失败", "error");
        });
      });
    }
  }
}
</script>

<style scoped>
.customer-container {
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
  .customer-container {
    padding: 15px;
  }
}

@media (max-width: 992px) {
  .customer-container {
    padding: 10px;
  }
  
  .operation-card .el-col {
    margin-bottom: 10px;
  }
}

@media (max-width: 768px) {
  .customer-container {
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