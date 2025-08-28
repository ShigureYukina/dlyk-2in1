<template>
  <el-row :gutter="16">
    <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
      <el-statistic :value="summaryData.effectiveActivityCount">
        <template #title>
          <div style="display: inline-flex; align-items: center">
            市场活动
          </div>
        </template>
        <template #suffix>/{{ summaryData.totalActivityCount }}</template>
      </el-statistic>
    </el-col>
    <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
      <el-statistic title="线索总数" :value="summaryData.totalClueCount"/>
    </el-col>
    <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
      <el-statistic title="客户总数" :value="summaryData.totalCustomerCount"/>
    </el-col>
    <el-col :xs="24" :sm="12" :md="6" class="text-center mb-4">
      <el-statistic :value="formatAmount(summaryData.successTranAmount)">
        <template #title>
          <div style="display: inline-flex; align-items: center">
            交易总额
          </div>
        </template>
        <template #suffix>/{{ formatAmount(summaryData.totalTranAmount) }}</template>
      </el-statistic>
    </el-col>
  </el-row>
  <!--为ECharts定义一个div，指定好宽度和高度，用来显示销售漏斗图-->
  <div id="saleFunnel" style="width: 48%; height:350px; margin:10px; float: left;"></div>

  <!--为ECharts定义一个div，指定好宽度和高度，用来显示线索来源饼图-->
  <div id="sourcePie" style="width: 48%; height:350px; margin:10px; float: left;"></div>

  <!--为ECharts定义一个div，指定好宽度和高度，用来显示市场活动统计柱状图-->
  <div id="activityBarChart" style="clear: both; width: 95%; height:300px; margin:15px; text-align: center;"></div>

  <!--为ECharts定义一个div，指定好宽度和高度，用来显示线索统计柱状图-->
  <div id="clueBarChart" style="clear: both; width: 95%; height:300px; margin:15px; text-align: center;"></div>

  <!--为ECharts定义一个div，指定好宽度和高度，用来显示客户统计柱状图-->
  <div id="customerBarChart" style="clear: both; width: 95%; height:300px; margin:15px; text-align: center;"></div>

  <!--为ECharts定义一个div，指定好宽度和高度，用来显示交易统计柱状图-->
  <div id="tranBarChart" style="clear: both; width: 95%; height:300px; margin:15px; text-align: center;"></div>
</template>

<script>
import {defineComponent} from 'vue';
import {doGet} from "../http/httpRequest.js";
import * as echarts from 'echarts';

export default defineComponent({
  name: 'StatisticView',
  data() {
    return {
      summaryData: {
        effectiveActivityCount: 0,
        totalActivityCount: 0,
        totalClueCount: 0,
        totalCustomerCount: 0,
        successTranAmount: 0.00,
        totalTranAmount: 0.00
      },
    }
  },
  mounted() {
    this.loadSummary();
    this.loadSaleFunnelChart();
    // this.loadSourcePieChart();
    // this.loadActivityBarChart();
    // this.loadClueBarChart();
    // this.loadCustomerBarChart();
    // this.loadTranBarChart();
  },
  methods: {
    loadSummary() {
      doGet('/api/summary/data', {}).then(res => {
        // 修正赋值：取接口响应中的 data 字段
        this.summaryData = res.data.data;
        console.log('加载统计数据成功', this.summaryData);
      })
    },
    // 格式化金额，保留两位小数，千分位分隔符
    formatAmount(value) {
      if (typeof value !== 'number') return '0.00';
      return value.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    //加载销售漏斗图
    loadSaleFunnelChart() {
      //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
      doGet("/api/statistic/funnelChart", {}).then(resp => {
        if (resp.data.code === 200) {
          //查询得到销售漏斗图的数据List
          let saleFunnelDataList = resp.data.data;
          console.log('加载销售漏斗图数据成功', saleFunnelDataList);

          //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
          var chartDom = document.getElementById('saleFunnel');
          //2、用echarts对象对要显示图标的dom元素区域进行初始化
          var myChart = echarts.init(chartDom);
          //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
          var option = {
            //标题组件，包含主标题和副标题。
            title: {
              //主标题文本，支持使用 \n 换行。
              text: '销售漏斗图',
              //title 组件离容器上侧的距离。
              top: 'bottom',
              //title 组件离容器左侧的距离。
              left: 'center'
            },
            //提示框组件。
            tooltip: {
              //触发类型。
              trigger: 'item',
              //提示框浮层内容格式器，支持字符串模板和回调函数两种形式。a:系列名称，b:数据项名称，c:数据项的值
              formatter: '{a} <br/>{b} : {c}'
            },
            //工具栏。
            toolbox: {
              //各工具配置项。
              feature: {
                //数据视图工具，可以展现当前图表所用的数据，编辑后可以动态更新。
                dataView: {
                  //是否不可编辑（只读）。
                  readOnly: false
                },
                //配置项还原。
                restore: {},
                //保存为图片。
                saveAsImage: {}
              }
            },
            //图例组件。
            legend: {
              //图例的数据数组。数组项通常为一个字符串，每一项代表一个系列的 name
              //data: ['线索', '客户', '交易', '成交'],
              left: 'center'
            },
            //系列，表示是哪一种类型的图
            series: [
              {
                //系列名称
                name: '统计数据',
                //是哪一种类型的图，funnel代表漏斗图
                type: 'funnel',
                //漏斗图组件离容器左侧的距离。
                left: '10%',
                //漏斗图组件离容器上侧的距离。
                top: 60,
                bottom: 60,
                width: '80%',
                min: 0,
                max: 100,
                minSize: '0%',
                maxSize: '200%',
                sort: 'descending',
                gap: 2,
                label: {
                  show: true,
                  position: 'inside'
                },
                labelLine: {
                  length: 10,
                  lineStyle: {
                    width: 1,
                    type: 'solid'
                  }
                },
                itemStyle: {
                  borderColor: '#fff',
                  borderWidth: 1
                },
                emphasis: {
                  label: {
                    fontSize: 20
                  }
                },
                //系列中的数据内容数组。数组项可以为单个数值，如：[12, 34, 56, 10, 23]，也可以是一个对象数组
                /*data: [
                  //数据项的值和名称
                  { value: 60, name: '交易' },
                  { value: 40, name: '成交' },
                  { value: 80, name: '客户' },
                  { value: 100, name: '线索' }
                ]*/
                data: saleFunnelDataList
              }
            ]
          };
          //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
          option && myChart.setOption(option);
        }
      })
    },

    // //加载线索来源饼图
    // loadSourcePieChart() {
    //   //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
    //   doGet("/api/statistic/pieChart", {}).then(resp => {
    //     if (resp.data.code === 200) {
    //       let sourcePieDataList = resp.data.data;
    //
    //       //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
    //       var chartDom = document.getElementById('sourcePie');
    //       //2、用echarts对象对要显示图标的dom元素区域进行初始化
    //       var myChart = echarts.init(chartDom);
    //       //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
    //       var option = {
    //         //标题组件，包含主标题和副标题。
    //         title: {
    //           //主标题文本，支持使用 \n 换行。
    //           text: '线索来源统计',
    //           //title 组件离容器上侧的距离。
    //           top: 'bottom',
    //           //title 组件离容器左侧的距离。
    //           left: 'center'
    //         },
    //         //提示框组件。
    //         tooltip: {
    //           //触发类型。
    //           trigger: 'item'
    //         },
    //         //图例组件。
    //         legend: {
    //           left: 'center'
    //         },
    //         //系列
    //         series: [
    //           {
    //             //系列名称
    //             name: '线索来源数据',
    //             //图标的类型是饼图
    //             type: 'pie',
    //             //饼图的半径
    //             radius: '60%',
    //             //系列中的数据内容数组。数组项可以为单个数值
    //             /*data: [
    //               { value: 1048, name: 'Search Engine' },
    //               { value: 735, name: 'Direct' },
    //               { value: 580, name: 'Email' },
    //               { value: 484, name: 'Union Ads' },
    //               { value: 300, name: 'Video Ads' }
    //             ],*/
    //             data: sourcePieDataList,
    //             //高亮状态的扇区和标签样式
    //             emphasis: {
    //               //选项样式
    //               itemStyle: {
    //                 //图形阴影的模糊大小
    //                 shadowBlur: 10,
    //                 //阴影水平方向上的偏移距离
    //                 shadowOffsetX: 0,
    //                 //阴影垂直方向上的偏移距离。
    //                 shadowColor: 'rgba(0, 0, 0, 0.5)'
    //               }
    //             }
    //           }
    //         ]
    //       };
    //       //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
    //       option && myChart.setOption(option);
    //     }
    //   })
    // },
    //
    // //加载市场活动柱状图
    // loadActivityBarChart() {
    //   //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
    //   doGet("/api/statistic/activityBarChart", {}).then(resp => {
    //     if (resp.data.code === 200) {
    //       let activityBarDataArray = resp.data.data;
    //
    //       //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
    //       var chartDom = document.getElementById('activityBarChart');
    //       //2、用echarts对象对要显示图标的dom元素区域进行初始化
    //       var myChart = echarts.init(chartDom);
    //       //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
    //       var option = {
    //         //标题组件，包含主标题和副标题。
    //         title: {
    //           //主标题文本，支持使用 \n 换行。
    //           text: '市场活动数据统计',
    //           //title 组件离容器上侧的距离。
    //           top: 'bottom',
    //           //title 组件离容器左侧的距离。
    //           left: 'center'
    //         },
    //         //直角坐标系 grid 中的 x 轴
    //         xAxis: {
    //           //坐标轴类型。
    //           type: 'category', //类目轴
    //           //x轴的刻度名称
    //           data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '8月', '10月', '11月', '12月'],
    //         },
    //         //直角坐标系 grid 中的 y 轴
    //         yAxis: {
    //           //坐标轴类型。value表示数值轴，适用于连续数据。
    //           type: 'value'
    //         },
    //         //提示框组件。
    //         tooltip: {
    //           //触发类型。
    //           trigger: 'item'
    //         },
    //         //系列
    //         series: [
    //           {
    //             //系列中的数据内容数组。数组项可以为单个数值
    //             //data: [120, 200, 150, 80, 70, 110, 130],
    //             data: activityBarDataArray,
    //             type: 'bar',
    //             barWidth: 25,
    //           }
    //         ]
    //       };
    //       //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
    //       option && myChart.setOption(option);
    //     }
    //   })
    // },
    //
    // //加载线索柱状图
    // loadClueBarChart() {
    //   //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
    //   doGet("/api/statistic/clueBarChart", {}).then(resp => {
    //     if (resp.data.code === 200) {
    //       let xDataArray = resp.data.data.x; //后台返回x轴数据
    //       let yDataArray = resp.data.data.y; //后台返回y轴数据
    //
    //       //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
    //       var chartDom = document.getElementById('clueBarChart');
    //       //2、用echarts对象对要显示图标的dom元素区域进行初始化
    //       var myChart = echarts.init(chartDom);
    //       //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
    //       var option = {
    //         //标题组件，包含主标题和副标题。
    //         title: {
    //           //主标题文本，支持使用 \n 换行。
    //           text: '线索数据统计',
    //           //title 组件离容器上侧的距离。
    //           top: 'bottom',
    //           //title 组件离容器左侧的距离。
    //           left: 'center'
    //         },
    //         //直角坐标系 grid 中的 x 轴
    //         xAxis: {
    //           //坐标轴类型。
    //           type: 'category', //类目轴
    //           //x轴的刻度名称
    //           //data: ['1', '2', '3', '4', '5', '6', '7', '8', '8', '10', '11', '12', '13', '14', ......],
    //           data: xDataArray
    //         },
    //         //直角坐标系 grid 中的 y 轴
    //         yAxis: {
    //           //坐标轴类型。value表示数值轴，适用于连续数据。
    //           type: 'value'
    //         },
    //         //提示框组件。
    //         tooltip: {
    //           //触发类型。
    //           trigger: 'item'
    //         },
    //         //系列
    //         series: [
    //           {
    //             //系列中的数据内容数组。数组项可以为单个数值
    //             //data: [120, 200, 150, 80, 70, 110, 130],
    //             data: yDataArray,
    //             type: 'bar',
    //             barWidth: 25,
    //           }
    //         ]
    //       };
    //       //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
    //       option && myChart.setOption(option);
    //     }
    //   })
    // },
    //
    // //加载客户柱状图
    // loadCustomerBarChart() {
    //   //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
    //   doGet("/api/statistic/customerBarChart", {}).then(resp => {
    //     if (resp.data.code === 200) {
    //       let xDataArray = resp.data.data.x; //后台返回x轴数据
    //       let yDataArray = resp.data.data.y; //后台返回y轴数据
    //
    //       //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
    //       var chartDom = document.getElementById('customerBarChart');
    //       //2、用echarts对象对要显示图标的dom元素区域进行初始化
    //       var myChart = echarts.init(chartDom);
    //       //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
    //       var option = {
    //         //标题组件，包含主标题和副标题。
    //         title: {
    //           //主标题文本，支持使用 \n 换行。
    //           text: '客户数据统计',
    //           //title 组件离容器上侧的距离。
    //           top: 'bottom',
    //           //title 组件离容器左侧的距离。
    //           left: 'center'
    //         },
    //         //直角坐标系 grid 中的 x 轴
    //         xAxis: {
    //           //坐标轴类型。
    //           type: 'category', //类目轴
    //           //x轴的刻度名称
    //           //data: ['1', '2', '3', '4', '5', '6', '7', '8', '8', '10', '11', '12', '13', '14', ......],
    //           data: xDataArray
    //         },
    //         //直角坐标系 grid 中的 y 轴
    //         yAxis: {
    //           //坐标轴类型。value表示数值轴，适用于连续数据。
    //           type: 'value'
    //         },
    //         //提示框组件。
    //         tooltip: {
    //           //触发类型。
    //           trigger: 'item'
    //         },
    //         //系列
    //         series: [
    //           {
    //             //系列中的数据内容数组。数组项可以为单个数值
    //             //data: [120, 200, 150, 80, 70, 110, 130],
    //             data: yDataArray,
    //             type: 'bar',
    //             barWidth: 25,
    //           }
    //         ]
    //       };
    //       //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
    //       option && myChart.setOption(option);
    //     }
    //   })
    // },
    //
    // //加载交易统计柱状图
    // loadTranBarChart() {
    //   //先查询出来数据，然后再渲染图（不能颠倒，如果你先显示图表，但是没有数据，图表显示不出来）
    //   doGet("/api/statistic/tranBarChart", {}).then(resp => {
    //     if (resp.data.code === 200) {
    //       let xDataArray = resp.data.data.x; //后台返回x轴数据
    //       let yDataArray1 = resp.data.data.y1; //后台返回y轴数据
    //       let yDataArray2 = resp.data.data.y2; //后台返回y轴数据
    //
    //       //1、根据id获取页面dom元素对象，echarts图表到时候就显示在这个dom元素里面 <div> echarts就显示在这里 </div>
    //       var chartDom = document.getElementById('tranBarChart');
    //       //2、用echarts对象对要显示图标的dom元素区域进行初始化
    //       var myChart = echarts.init(chartDom);
    //
    //       /*var app = {};
    //       const posList = [
    //         'left',
    //         'right',
    //         'top',
    //         'bottom',
    //         'inside',
    //         'insideTop',
    //         'insideLeft',
    //         'insideRight',
    //         'insideBottom',
    //         'insideTopLeft',
    //         'insideTopRight',
    //         'insideBottomLeft',
    //         'insideBottomRight'
    //       ];
    //       app.configParameters = {
    //         rotate: {
    //           min: -90,
    //           max: 90
    //         },
    //         align: {
    //           options: {
    //             left: 'left',
    //             center: 'center',
    //             right: 'right'
    //           }
    //         },
    //         verticalAlign: {
    //           options: {
    //             top: 'top',
    //             middle: 'middle',
    //             bottom: 'bottom'
    //           }
    //         },
    //         position: {
    //           options: posList.reduce(function (map, pos) {
    //             map[pos] = pos;
    //             return map;
    //           }, {})
    //         },
    //         distance: {
    //           min: 0,
    //           max: 100
    //         }
    //       };
    //       app.config = {
    //         rotate: 90,
    //         align: 'left',
    //         verticalAlign: 'middle',
    //         position: 'insideBottom',
    //         distance: 15,
    //         onChange: function () {
    //           const labelOption = {
    //             rotate: app.config.rotate,
    //             align: app.config.align,
    //             verticalAlign: app.config.verticalAlign,
    //             position: app.config.position,
    //             distance: app.config.distance
    //           };
    //           myChart.setOption({
    //             series: [
    //               {
    //                 label: labelOption
    //               },
    //               {
    //                 label: labelOption
    //               },
    //               {
    //                 label: labelOption
    //               },
    //               {
    //                 label: labelOption
    //               }
    //             ]
    //           });
    //         }
    //       };
    //       const labelOption = {
    //         //是否显示标签。
    //         show: true,
    //         //标签的位置。
    //         position: app.config.position,
    //         distance: app.config.distance,
    //         align: app.config.align,
    //         verticalAlign: app.config.verticalAlign,
    //         rotate: app.config.rotate,
    //         formatter: '{c}  {name|{a}}',
    //         fontSize: 16,
    //         rich: {
    //           name: {}
    //         }
    //       };*/
    //
    //       //3、配置可选项，也就是配置图表的各种显示参数（看文档-->配置项手册）
    //       var option = {
    //         //标题组件，包含主标题和副标题。
    //         title: {
    //           //主标题文本，支持使用 \n 换行。
    //           text: '交易数据统计',
    //           //title 组件离容器上侧的距离。
    //           top: 'bottom',
    //           //title 组件离容器左侧的距离。
    //           left: 'center'
    //         },
    //         //提示框组件。
    //         tooltip: {
    //           //触发类型。
    //           trigger: 'axis',
    //           //这是坐标轴指示器（axisPointer）的全局公用设置。
    //           axisPointer: {
    //             //阴影指示器
    //             type: 'shadow'
    //           }
    //         },
    //         //图例组件。
    //         legend: {
    //           data: ['Forest', 'Steppe', 'Desert', 'Wetland'],
    //           left: 'center'
    //         },
    //         //工具栏。
    //         toolbox: {
    //           //图例是否展示
    //           show: true,
    //           //水平还是垂直
    //           //orient: 'vertical',
    //           left: 'right',
    //           top: 'top',
    //           feature: {
    //             dataView: {show: true, readOnly: false},
    //             //动态类型切换
    //             magicType: {show: true, type: ['line', 'bar', 'stack']},
    //             restore: {show: true},
    //             saveAsImage: {show: true}
    //           }
    //         },
    //         //x轴
    //         xAxis: [
    //           {
    //             type: 'category',
    //             //坐标轴刻度相关设置
    //             axisTick: {show: false},
    //             //data: ['1', '2', '3', '4', '5', '6', '7', '8']
    //             data: xDataArray
    //           }
    //         ],
    //         //y轴
    //         yAxis: [
    //           {
    //             type: 'value'
    //           }
    //         ],
    //         //系列
    //         series: [
    //           {
    //             name: '交易',
    //             type: 'bar',
    //             barWidth: '45%',
    //             //不同系列的柱间距离，为百分比（如 '30%'，表示柱子宽度的 30%）。
    //             barGap: '0',
    //             //图形上的文本标签，可用于说明图形的一些数据信息，比如值，名称等。
    //             //label: labelOption,
    //             //高亮的图形样式和标签样式。
    //             emphasis: {
    //               //在高亮图形时，是否淡出其它数据的图形已达到聚焦的效果
    //               focus: 'series'
    //             },
    //             //数据项
    //             //data: [320, 332, 301, 334, 390]
    //             data: yDataArray1
    //           },
    //           {
    //             name: '成交',
    //             type: 'bar',
    //             barWidth: '45%',
    //             //label: labelOption,
    //             emphasis: {
    //               focus: 'series'
    //             },
    //             //data: [220, 182, 191, 234, 290]
    //             data: yDataArray2
    //           }
    //         ]
    //       };
    //       //4、如果已经配置了可选项，然后就把可选项设置到图标对象中去
    //       option && myChart.setOption(option);
    //     }
    //   })
    // },
  }
});
</script>

<style scoped>
.el-row {
  text-align: center;
}
</style>