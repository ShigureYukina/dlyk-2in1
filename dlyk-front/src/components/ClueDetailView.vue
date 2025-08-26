<template>
  <el-form
      ref="clueRemarkRefForm"
      :model="clueRemark"
      label-width="110px"
      :rules="clueRemarkRules"
      style="max-width: 95%;">

    <el-form-item label="负责人">
      <div class="desc">{{ clueDetail.ownerDO?.name || '' }}</div>
    </el-form-item>

    <el-form-item label="所属活动">
      <div class="desc">{{ clueDetail.activityDO?.name || '' }}</div>
    </el-form-item>

    <el-form-item label="姓名">
      <div class="desc">{{ clueDetail.fullName || '' }}</div>
    </el-form-item>

    <el-form-item label="称呼">
      <div class="desc">{{ clueDetail.appellationDO?.typeValue || '' }}</div>
    </el-form-item>

    <el-form-item label="手机">
      <div class="desc">{{ clueDetail.phone || '' }}</div>
    </el-form-item>

    <el-form-item label="微信">
      <div class="desc">{{ clueDetail.weixin || '' }}</div>
    </el-form-item>

    <el-form-item label="QQ">
      <div class="desc">{{ clueDetail.qq || '' }}</div>
    </el-form-item>

    <el-form-item label="邮箱">
      <div class="desc">{{ clueDetail.email || '' }}</div>
    </el-form-item>

    <el-form-item label="年龄">
      <div class="desc">{{ clueDetail.age || '' }}</div>
    </el-form-item>

    <el-form-item label="职业">
      <div class="desc">{{ clueDetail.job || '' }}</div>
    </el-form-item>

    <el-form-item label="年收入">
      <div class="desc">{{ clueDetail.yearIncome || '' }}</div>
    </el-form-item>

    <el-form-item label="住址">
      <div class="desc">{{ clueDetail.address || '' }}</div>
    </el-form-item>

    <el-form-item label="贷款">
      <div class="desc">{{ clueDetail.needLoanDO?.typeValue || '' }}</div>
    </el-form-item>

    <el-form-item label="意向状态">
      <div class="desc">{{ clueDetail.intentionStateDO?.typeValue || '' }}</div>
    </el-form-item>

    <el-form-item label="意向产品">
      <div class="desc">{{ clueDetail.intentionProductDO?.name || '' }}</div>
    </el-form-item>

    <el-form-item label="线索状态">
      <div class="desc">{{ clueDetail.stateDO?.typeValue || '' }}</div>
    </el-form-item>

    <el-form-item label="线索来源">
      <div class="desc">{{ clueDetail.sourceDO?.typeValue || '' }}</div>
    </el-form-item>

    <el-form-item label="线索描述">
      <div class="desc">{{ clueDetail.description || '' }}</div>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <div class="desc">{{ clueDetail.nextContactTime || '' }}</div>
    </el-form-item>

    <el-form-item label="填写跟踪记录" prop="noteContent">
      <el-input
          v-model="clueRemark.noteContent"
          :rows="8"
          type="textarea"/>
    </el-form-item>
    <el-form-item label="跟踪方式" prop="noteWay">
      <el-select
          v-model="clueRemark.noteWay"
          placeholder="请选择跟踪方式"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in noteWayOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="clueRemarkSubmit">提 交</el-button>
      <el-button type="success" @click="convertCustomerDialogVisible = true" v-if="clueDetail.state !== -1">转换客户
      </el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>

  </el-form>
  <el-table
      :data="clueRemarkList"
      style="width: 100%">
    <el-table-column type="index" label="序号" width="100"/>
    <el-table-column property="noteWayDO.typeValue" label="跟踪方式"/>
    <el-table-column property="noteContent" label="跟踪内容"/>
    <el-table-column property="createTime" label="跟踪时间" width="175"/>
    <el-table-column property="createByDO.name" label="跟踪人" width="175"/>
    <el-table-column property="editTime" label="编辑时间" width="175"/>
    <el-table-column property="editByDO.name" label="编辑人" width="175"/>
    <el-table-column label="操作" width="150">
      <template #default="scope">
        <el-button type="primary" size="small" @click="edit(scope.row.id)">编辑</el-button>
        <el-button type="danger" size="small" @click="del(scope.row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="toPage"
      @next-click="toPage"
      @current-change="toPage"/>

  <el-dialog v-model="clueRemarkDialogVisible" title="编辑线索备注" center width="55%" draggable>
    <el-form ref="editClueRemarkForm" :model="editClueRemarkQuery" :rules="clueRemarkRules"
             label-width="110px">
      <el-form-item label="跟踪方式" prop="noteWay">
        <el-select
            v-model="editClueRemarkQuery.noteWay"
            placeholder="请选择跟踪方式"
            style="width: 100%"
            clearable>
          <el-option
              v-for="item in noteWayOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="跟踪内容" prop="noteContent">
        <el-input
            v-model="editClueRemarkQuery.noteContent"
            style="width: 100%"
            :autosize="{ minRows: 5, maxRows: 10 }"
            type="textarea"
            placeholder="请输入跟踪内容"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="clueRemarkDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="editClueRemarkSubmit">保存</el-button>
    </template>
  </el-dialog>
  <!--线索转换为客户的弹窗（对话框）-->
  <el-dialog v-model="convertCustomerDialogVisible" title="线索转换客户" width="55%" center>
    <el-form ref="convertCustomerRefForm" :model="customerQuery" label-width="110px" :rules="convertCustomerRules">
      <el-form-item label="意向产品" prop="product">
        <el-select v-model="customerQuery.product" placeholder="请选择" style="width: 100%;"
                   @click="loadDicValue('product')">
          <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="客户描述" prop="description">
        <el-input
            v-model="customerQuery.description"
            :rows="8"
            type="textarea"
            placeholder="请输入客户描述"/>
      </el-form-item>
      <el-form-item label="下次跟踪时间" prop="nextContactTime">
        <el-date-picker
            v-model="customerQuery.nextContactTime"
            type="datetime"
            style="width: 100%;"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择下次跟踪时间"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="convertCustomerDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="convertCustomerSubmit">转 换</el-button>
      </span>
    </template>
  </el-dialog>

</template>

<script>
import {defineComponent} from "vue";
import {doGet, doPost, doPut} from "../http/httpRequest.js";
import {messageTip} from "../util/util.js";
import router from "../router/router.js";

export default defineComponent({
  inject: ['reload'],
  name: "ClueDetailView",
  data() {
    return {
      clueDetail: {
        ownerDo: {},
        activityDo: {},
        appellationDo: {},
        needLoanDo: {},
        intentionStateDo: {},
        intentionProductDo: {},
        stateDo: {},
        sourceDo: {},
      },
      clueRemark: {},
      productOptions: [{}],
      editClueRemarkQuery: {},
      // 跟踪方式的下拉选项
      noteWayOptions: [{}],
      clueRemarkRules: {
        noteContent: [
          {required: true, message: '请填写跟踪记录', trigger: 'blur'}
        ],
        noteWay: [
          {required: true, message: '请选择跟踪方式', trigger: 'change'}
        ]
      },
      convertCustomerRules: {
        product: [
          {required: true, message: '请选择意向产品', trigger: ['blur', 'change']}
        ],
        description: [
          {required: true, message: '客户描述不能为空', trigger: 'blur'},
          {min: 5, max: 255, message: '客户描述长度为5-255个字符', trigger: 'blur'}
        ],
        nextContactTime: [
          {required: true, message: '请选择下次联系时间', trigger: 'blur'}
        ]
      },
      clueRemarkList: [{
        editByDO: {},
        createByDO: {},
        noteWayDO: {},
      }],
      customerQuery: {},

      pageSize: 10,
      total: 0,

      //线索备注弹窗相关
      clueRemarkDialogVisible: false,
      convertCustomerDialogVisible: false,
    }
  },
  mounted() {
    // 页面加载时根据id查询线索详情
    let id = this.$route.params.id;
    doGet("/api/clue/detail/" + id, {}).then(response => {
      if (response.data.code === 200) {
        this.clueDetail = response.data.data;
      }
    })

    // 加载跟踪方式下拉选项数据
    this.loadDicValue('noteWay');
    this.loadClueRemarkList(1);
  },
  methods: {
    // 加载跟踪方式下拉选项数据
    loadDicValue(typeCode) {
      doGet("/api/dicvalue/" + typeCode, {}).then(res => {
        if (res.data.code === 200) {
          if (typeCode === 'noteWay') {
            this.noteWayOptions = res.data.data;
          } else if (typeCode === 'product') {
            this.productOptions = res.data.data;
          }
        }
      });
    },

    // 提交线索跟踪记录
    clueRemarkSubmit() {
      // 表单验证
      this.$refs.clueRemarkRefForm.validate((valid) => {
        if (valid) {
          // 表单验证通过，提交数据
          doPost("/api/clue/remark", {
                clueId: this.clueDetail.id,
                noteContent: this.clueRemark.noteContent,
                noteWay: this.clueRemark.noteWay
              }
          ).then(response => {
            if (response.data.code === 200) {
              messageTip("跟踪记录添加成功", "success");
              // 清空表单
              this.clueRemark = {};
              // 重新加载页面数据
              this.reload();
            } else {
              messageTip(response.data.msg, "error");
            }
          })
        } else {
          messageTip("表单填写有误，请检查", "error");
          return false;
        }
      });
    },

    // 转换客户
    convertCustomerSubmit() {
      this.$refs.convertCustomerRefForm.validate((valid) => {
        // 调用转换客户接口
        if (valid) {
          doPost("/api/clue/customer/", {
            clueId: this.clueDetail.id,
            product: this.customerQuery.product,
            description: this.customerQuery.description,
            nextContactTime: this.customerQuery.nextContactTime
          }).then(response => {
            if (response.data.code === 200) {
              messageTip("客户转换成功", "success");
              this.reload();
              router.back();
            } else {
              messageTip("客户转换失败", "error");
            }
          })
        }
      });
    },

    loadClueRemarkList(current) {
      doGet("/api/clue/remark", {
        current: current,
        clueId: this.$route.params.id,

        pageSize: this.pageSize
      }).then(response => {
        if (response.data.code === 200) {
          console.log("获取活动列表成功:");
          // console.log(response.data.data.list);
          // 假设后端返回的数据在response.data中
          this.clueRemarkList = response.data.data.list;
          this.pageSize = response.data.data.pageSize;
          this.total = response.data.data.total;
        }
      }).catch(error => {
        console.error("获取活动列表失败:", error);
      });
    },
    toPage(current) {
      this.loadClueRemarkList(current);
    },
    // 返回
    goBack() {
      router.push("/dashboard/clue");
    },
    //编辑备注
    edit(id) {
      this.clueRemarkDialogVisible = true;
      doGet("/api/clue/remark/" + id, {})
          .then(response => {
            if (response.data.code === 200) {
              this.editClueRemarkQuery = response.data.data;
            }
          });
    },

    editClueRemarkSubmit() {
      this.$refs.editClueRemarkForm.validate((valid) => {
        if (valid) {
          doPut("/api/clue/remark", {
            id: this.editClueRemarkQuery.id,
            clueId: this.editClueRemarkQuery.clueId,
            noteWay: this.editClueRemarkQuery.noteWay,
            noteContent: this.editClueRemarkQuery.noteContent
          }).then(response => {
            if (response.data.code === 200) {
              messageTip("编辑成功", "success");
              this.clueRemarkDialogVisible = false;
              this.reload();
            } else {
              messageTip("编辑失败", "error");
            }
          })
        }
      })
    },


  }
})
</script>


<style scoped>
.desc {
  background-color: #F0FFFF;
  width: 100%;
  padding-left: 15px;
}
</style>