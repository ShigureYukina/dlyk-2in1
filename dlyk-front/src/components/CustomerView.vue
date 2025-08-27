<template>
  <el-button type="primary" class="btn" @click="batchExportExcel">批量导出(Excel)</el-button>
  <el-button type="success" class="btn" @click="chooseExportExcel">选择导出(Excel)</el-button>

  <el-table
      :data="customerList"
      style="width: 100%"
      @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50"/>
    <el-table-column type="index" label="序号" width="65"/>
    <el-table-column property="ownerDO.name" label="负责人" width="120"/>
    <el-table-column property="activityDO.name" label="所属活动"/>
    <el-table-column label="姓名">
      <template #default="scope">
        <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.clueDO.fullName }}</a>
      </template>
    </el-table-column>
    <el-table-column property="appellationDO.typeValue" label="称呼"/>
    <el-table-column property="clueDO.phone" label="手机" width="120"/>
    <el-table-column property="clueDO.weixin" label="微信" width="120"/>
    <el-table-column property="needLoanDO.typeValue" label="是否贷款"/>
    <el-table-column property="intentionStateDO.typeValue" label="意向状态"/>
    <el-table-column property="stateDO.typeValue" label="线索状态"/>
    <el-table-column property="sourceDO.typeValue" label="线索来源"/>
    <el-table-column property="intentionProductDO.name" label="意向产品"/>
    <el-table-column property="nextContactTime" label="下次联系时间" width="165"/>
    <el-table-column label="操作" width="85">
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)">详情</el-button>
      </template>
    </el-table-column>
  </el-table>
  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="page"
        @next-click="page"
        @current-change="page"/>
  </p>
</template>

<script>
import axios from "axios";
import {doGet} from "../http/httpRequest";
import {getToken, messageTip} from "../util/util.js";

export default {
  name: "CustomerView",

  data() {
    return {
      //客户列表对象，初始值是空
      customerList: [{
        clueDO: {},
        ownerDO: {},
        activityDO: {},
        appellationDO: {},
        needLoanDO: {},
        intentionStateDO: {},
        stateDO: {},
        sourceDO: {},
        intentionProductDO: {},
      }],
      //分页时每页显示多少条数据
      pageSize: 0,
      //总共有多少条
      total: 0,
      //定义一个customerId的数组，里面存放勾选的那些客户id，初始值是空
      customerIdArray: []
    }
  },

  mounted() {
    this.getData(1);
  },

  methods: {
    //获取线索分页列表数据
    getData(current) {
      doGet("/api/customers", {
        current: current //当前页，前面是参数名，后面是参数值
      }).then(resp => {
        if (resp.data.code === 200) {
          this.customerList = resp.data.data.list; // resp.data  =  R 对象
          this.pageSize = resp.data.data.pageSize;
          this.total = resp.data.data.total;
        }
      })
    },

    //分页函数
    page(number) { //number值是element-plus回调时传给我们的，number这个值就是当前页
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
      document.body.appendChild(iframe);

      //2、后端就给前端返回一个Excel文件（我们写这个代码，在后端项目中去写）
      //3、浏览器接收到该Excel文件后弹出下载框进行下载（这一步是浏览器做的，我们不需要管）
    },

    //批量导出
    batchExportExcel() {
      this.exportExcel(null);
    },

    //选择导出
    chooseExportExcel() {
      if (this.customerIdArray.length <= 0) {
        messageTip("请选择要导出的数据", "warning");
        return;
      }
      let ids = this.customerIdArray.join(","); // ids = "2,6,11,12,18"
      this.exportExcel(ids);
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
    }
  }
}
</script>

<style scoped>
.btn {
  margin-bottom: 10px;
}
</style>