<template>
  <el-form
      ref="clueRefForm"
      :model="clueQuery"
      :rules="clueRules"
      label-width="100px"
      style="max-width: 95%;">

    <el-form-item label="负责人" prop="ownerId">
      <el-select
          v-model="clueQuery.ownerId"
          placeholder="请选择负责人"
          style="width: 100%"
          clearable
          disabled>
        <el-option
            v-for="item in ownerOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="所属活动">
      <el-select
          v-model="clueQuery.activityId"
          placeholder="请选择所属活动"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in activityOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="姓名" prop="fullName">
      <el-input v-model="clueQuery.fullName"/>
    </el-form-item>

    <el-form-item label="称呼">
      <el-select
          v-model="clueQuery.appellation"
          placeholder="请选择称呼"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in appellationOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="手机" v-if="clueQuery.id > 0"><!--此时是编辑-->
      <el-input v-model="clueQuery.phone" disabled/>
    </el-form-item>

    <el-form-item label="手机" prop="phone" v-else><!--此时是录入-->
      <el-input rules="phoneRules" v-model="clueQuery.phone"/>
    </el-form-item>

    <el-form-item label="微信">
      <el-input v-model="clueQuery.weixin"/>
    </el-form-item>

    <el-form-item label="QQ" prop="qq">
      <el-input v-model="clueQuery.qq"/>
    </el-form-item>

    <el-form-item label="邮箱" prop="email">
      <el-input v-model="clueQuery.email"/>
    </el-form-item>

    <el-form-item label="年龄" prop="age">
      <el-input v-model="clueQuery.age"/>
    </el-form-item>

    <el-form-item label="职业">
      <el-input v-model="clueQuery.job"/>
    </el-form-item>

    <el-form-item label="年收入" prop="yearIncome">
      <el-input v-model="clueQuery.yearIncome"/>
    </el-form-item>

    <el-form-item label="住址">
      <el-input v-model="clueQuery.address"/>
    </el-form-item>

    <el-form-item label="贷款">
      <el-select
          v-model="clueQuery.needLoan"
          placeholder="请选择是否需要贷款"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in needLoanOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="意向状态">
      <el-select
          v-model="clueQuery.intentionState"
          placeholder="请选择意向状态"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in intentionStateOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="意向产品">
      <el-select
          v-model="clueQuery.intentionProduct"
          placeholder="请选择意向产品"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in productOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索状态">
      <el-select
          v-model="clueQuery.state"
          placeholder="请选择线索状态"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in clueStateOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索来源">
      <el-select
          v-model="clueQuery.source"
          placeholder="请选择线索来源"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in sourceOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索描述" prop="description">
      <el-input
          v-model="clueQuery.description"
          :rows="5"
          type="textarea"
          placeholder="请输入线索描述"/>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <el-date-picker
          v-model="clueQuery.nextContactTime"
          type="datetime"
          style="width: 100%;"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择下次联系时间"/>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="addClueSubmit">提 交</el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>
</template>
<script>
import {defineComponent} from "vue";
import {doGet, doPost, doPut} from "../http/httpRequest.js";
import router from "../router/router.js";
import {messageTip} from "../util/util.js";

export default defineComponent({
  name: "ClueRecordView",
  data() {
    // 手机号验证规则
    const validatePhone = (rule, value, callback) => {
      if (value && !/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号'));
      } else {
        callback();
      }
    };

    // 邮箱验证规则
    const validateEmail = (rule, value, callback) => {
      if (value && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) {
        callback(new Error('请输入正确的邮箱地址'));
      } else {
        callback();
      }
    };

    // 年龄验证规则
    const validateAge = (rule, value, callback) => {
      if (value && (!/^\d+$/.test(value) || parseInt(value) < 0 || parseInt(value) > 150)) {
        callback(new Error('请输入正确的年龄(0-150)'));
      } else {
        callback();
      }
    };

    // 年收入验证规则
    const validateYearIncome = (rule, value, callback) => {
      if (value && (!/^\d+(\.\d{1,2})?$/.test(value) || parseFloat(value) < 0)) {
        callback(new Error('请输入正确的年收入'));
      } else {
        callback();
      }
    };

    return {
      //线索表单对象，初始值是空
      clueQuery: {},
      //负责人的下拉选项数据，初始值是空
      ownerOptions: [{}],
      //市场活动的下拉选项数据，初始值是空
      activityOptions: [{}],
      //意向产品的下拉选项
      productOptions: [{}],
      appellationOptions: [{}],
      needLoanOptions: [{}],
      intentionStateOptions: [{}],
      clueStateOptions: [{}],
      sourceOptions: [{}],
      //定义录入线索表单验证规则
      clueRules: {
        fullName: [
          {message: '请输入姓名', trigger: 'blur'}
        ],
        phone: [
          {required: true, message: '请输入手机号', trigger: 'blur'},
          {validator: this.validatePhone, trigger: 'blur'}
        ],
        qq: [
          {pattern: /^\d{5,11}$/, message: '请输入正确的QQ号', trigger: 'blur'}
        ],
        email: [
          {validator: validateEmail, trigger: 'blur'}
        ],
        age: [
          {validator: validateAge, trigger: 'blur'}
        ],
        yearIncome: [
          {validator: validateYearIncome, trigger: 'blur'}
        ],
        description: [
          {message: '请输入线索描述', trigger: 'blur'}
        ]
      }
    }
  },
  mounted() {
    this.loadClue();
    this.loadUserData();
    this.loadDicValue('appellation');
    this.loadDicValue('needLoan');
    this.loadDicValue('intentionState');
    this.loadDicValue('clueState');
    this.loadDicValue('source');
    this.loadDicValue('product');
    this.loadDicValue('activity');
  },
  methods: {
    loadUserData() {
      // 获取登录用户信息
      doGet("/api/login/info", {}).then((resp) => {
        this.clueQuery.ownerId = resp.data.data.id;
      })
      // 加载所有者选项
      doGet("/api/owner", {}).then(response => {
        if (response.data.code === 200) {
          this.ownerOptions = response.data.data;
        }
      })
    },
    //获取市场活动的下拉选项数据
    loadDicValue(typeCode) {
      doGet("/api/dicvalue/" + typeCode, {}).then(res => {
        if (res.data.code === 200) {
          if (typeCode === 'appellation') {
            this.appellationOptions = res.data.data;
          } else if (typeCode === 'needLoan') {
            this.needLoanOptions = res.data.data;
          } else if (typeCode === 'intentionState') {
            this.intentionStateOptions = res.data.data;
          } else if (typeCode === 'clueState') {
            this.clueStateOptions = res.data.data;
          } else if (typeCode === 'source') {
            this.sourceOptions = res.data.data;
          } else if (typeCode === 'product') {
            this.productOptions = res.data.data;
          } else if (typeCode === 'activity') {
            this.activityOptions = res.data.data;
          }
        }
      });
    },
    // 表单提交方法
    addClueSubmit() {
      this.$refs.clueRefForm.validate((valid) => {
        if (valid) {
          // 创建FormData对象
          let formData = new FormData();
          for (let key in this.clueQuery) {
            if (this.clueQuery[key] !== undefined) {
              formData.append(key, this.clueQuery[key]);
            }
          }
          // 判断是编辑还是新增
          if (this.clueQuery.id > 0) {
            // 编辑状态，使用PUT方法
            doPut("/api/clue", formData).then(response => {
              if (response.data.code === 200) {
                messageTip('线索更新成功', 'success')
                this.goBack();
              } else {
                messageTip('线索更新失败', 'error')
              }
            })
          } else {
            // 新增状态，使用POST方法
            doPost("/api/clue", formData).then(response => {
              if (response.data.code === 200) {
                messageTip('线索添加成功', 'success')
                this.goBack();
              } else {
                messageTip('线索添加失败', 'error')
              }
            })
          }
        } else {
          console.log('表单验证失败');
          return false;
        }
      });
    },
    // 返回方法
    goBack() {
      router.back()
    },

    validatePhone(rule, value, callback) {
      let phone = value;
      if (phone) {
        doGet("/api/clue/" + phone, {}).then(res => {
          if (res.data.code === 500) {
            return callback(new Error('手机号已存在'));
          } else {
            return callback();
          }
        })
      }

    },
    loadClue() {
      let id = this.$route.params.id;
      if (id) {
        doGet("/api/clue/detail/" + id, {}).then(response => {
          if (response.data.code === 200) {
            this.clueQuery = response.data.data;
          }
        })
      }
    },
  }
});

</script>
<style scoped>

</style>