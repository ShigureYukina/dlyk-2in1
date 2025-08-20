<template>
  <el-form :inline="true" :model="activityQuery" class="demo-form-inline">

    <el-form-item label="负责人">
      <el-select
          v-model="activityQuery.ownerId"
          placeholder="请选择负责人"
          clearable
      >
        <el-option label="Zone one" value="shanghai"/>
        <el-option label="Zone two" value="beijing"/>
      </el-select>
    </el-form-item>

    <el-form-item label="活动名称">
      <el-input v-model="activityQuery.user" placeholder="请输入活动名称" clearable/>
    </el-form-item>

    <el-form-item label="Activity time">
      <div class="block">
        <el-date-picker
            v-model="activityQuery.startTime"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            date-format="YYYY/MM/DD ddd"
            time-format="A hh:mm:ss"
        />
      </div>
    </el-form-item>

    <el-form-item label="活动预算">
      <el-input v-model="activityQuery.cost" placeholder="请输入活动预算" clearable/>
    </el-form-item>

    <el-form-item label="创建时间">
      <el-date-picker
          v-model="activityQuery.createTime"
          type="datetime"
          placeholder="请选择创建时间"
      />
    </el-form-item>


    <el-form-item>
      <el-button type="primary" @click="onSearch">搜 索</el-button>
      <el-button type="primary" plain @click="onReset">重 置</el-button>
    </el-form-item>
  </el-form>

  <el-button type="primary" @click="add()">录入市场活动</el-button>
  <el-button type="danger" @click="batchDel()">批量删除</el-button>

  <el-table
      ref="multipleTableRef"
      :data="activitylist"
      style="width: 100%"
      @selection-change="handleSelectionChange"
  >
    <el-table-column type="selection" width="55"/>

    <el-table-column type="index" label="序号" width="60"/>
    <el-table-column property="ownerDO.name" label="负责人"/>
    <el-table-column property="name" label="活动名称" width="120" show-overflow-tooltip/>
    <el-table-column property="startTime" label="开始时间" show-overflow-tooltip/>
    <el-table-column property="endTime" label="结束时间" show-overflow-tooltip/>
    <el-table-column property="cost" label="活动预算" show-overflow-tooltip/>
    <el-table-column property="createTime" label="创建时间" show-overflow-tooltip/>
    <el-table-column label="操作" width="220" show-overflow-tooltip>
      <template #default="scope">
        <el-button type="primary" size="small" @click="view(scope.row.id)">详情</el-button>
        <el-button type="warning" size="small" @click="edit(scope.row.id)">编辑</el-button>
        <el-button type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
      </template>
    </el-table-column>

  </el-table>

  <el-pagination background layout="prev, pager, next" :page-size="pagesize" :total="total"
                 @prev-click="toPage" @next-click="toPage" @current-change="toPage"/>
</template>
<script>

import {defineComponent} from "vue";
import {doGet} from "../http/httpRequest.js";

export default defineComponent({
  name: "ActivityView",
  data() {
    return {
      activityQuery: {},
      activitylist: [{
        ownerDO: {}
      }],
      //分页数据
      pagesize: 10,
      total: 0,
    }
  },
  mounted() {
    this.getData(1);
  },
  methods: {
    getData(current) {
      doGet("/api/activitys", {current: current}).then(response => {
        if (response.data.code === 200) {
          this.activitylist = response.data.data.list;
          this.pagesize = response.data.data.pageSize;
          this.total = response.data.data.total;
        }
      })
    },
    toPage(current) {
      this.getData(current);
    },
  }
});


</script>
<style>
.el-table {
  margin-top: 20px;
}

.el-pagination {
  margin-top: 20px;
}
.el-form{
  margin-bottom: 20px;
}
</style>