<template>
  <div class="statistics-container" v-if="moduleLoadInit">
    <section class="mb20">
      <el-form inline :model="form">
        <el-form-item>
          <el-date-picker
            v-model="form.startTimeRangeArr"
            type="datetimerange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit"> 搜索 </el-button>
          <el-button @click="handleReset"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </section>
    <el-row :gutter="20" class="mb20">
      <el-col :xs="24" :sm="24" :md="12" :lg="6">
        <div
          class="left-col col justify-between"
          v-for="(item, index) in processLeft"
          :key="index"
          @click="handleCol(item.type, item.name)"
        >
          <div class="flex-one">
            <p class="name">{{ item.name }}</p>
            <p class="num">
              <nf-count-up :end="item.num"></nf-count-up>
            </p>
            <p class="add-num">今日新增：{{ item.todayNum }}</p>
          </div>
          <img
            :src="`https://oss.nutflow.vip/bladex/saber/statistics/${item.type}.png`"
            alt=""
            width="100"
            height="100"
          />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="18">
        <el-row :gutter="20">
          <el-col
            :xs="24"
            :sm="24"
            :md="12"
            :lg="6"
            v-for="(item, index) in processRight"
            :key="index"
            class="right-wrap"
          >
            <div class="right-col col justify-between" @click="handleCol(item.type, item.name)">
              <div class="flex-one">
                <p class="name">{{ item.name }}</p>
                <p class="num">
                  <nf-count-up :end="item.num"></nf-count-up>
                </p>
              </div>
              <img
                :src="`https://oss.nutflow.vip/bladex/saber/statistics/${item.type}.png`"
                alt=""
                width="50"
                height="50"
              />
            </div>
          </el-col>
        </el-row>
        <div class="col">
          <div id="bar" style="width: 100%; height: 300px"></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="15">
        <div class="col echart-item">
          <div id="task" style="width: 100%; height: 330px"></div>
        </div>
      </el-col>
      <el-col :span="9">
        <div class="col ops-item">
          <div class="ops-head justify-between">
            <div class="th" style="padding-left: 25%">流程名称</div>
            <div class="th" style="padding-right: 10%">开始时间</div>
          </div>
          <div class="swiperlist">
            <div v-for="(item, index) in opsList" :key="index" class="item justify-between">
              <span class="name flex-one">{{ item.processDefinitionName }}</span>
              <span class="time">{{ item.createTime }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { index, bar } from '../../api/statistics/index';
import { getList } from '../../api/ops/ops';
import * as echarts from 'echarts';

import module from '../../mixins/module';

export default {
  mixins: [module()],
  data() {
    return {
      form: {
        startTimeRangeArr: [],
      },
      barParams: {
        type: 'process',
        name: '流程总数',
      },
      formOption: {
        menuSpan: 4,
        size: 'default',
        labelWidth: 15,
        submitText: '搜索',
        column: [
          {
            label: '',
            type: 'datetimerange',
            prop: 'startTimeRange',
            span: 12,
            startPlaceholder: '时间日期开始范围自定义',
            endPlaceholder: '时间日期结束范围自定义',
          },
        ],
      },
      processLeft: [
        {
          name: '流程总数',
          num: 0,
          type: 'process',
          todayType: 'todayProcess',
        },
        {
          name: '进行中',
          num: 0,
          type: 'unfinished',
          todayType: 'todayUnfinished',
        },
        {
          name: '已结束',
          num: 0,
          type: 'finished',
          todayType: 'todayFinished',
        },
      ],
      processRight: [
        {
          name: '已终结',
          num: 0,
          type: 'terminate',
        },
        {
          name: '已撤销',
          num: 0,
          type: 'withdraw',
        },
        {
          name: '已驳回',
          num: 0,
          type: 'reject',
        },
        {
          name: '已删除',
          num: 0,
          type: 'deleted',
        },
      ],
      processList: [],
      taskList: [],
      opsList: [],
      animate: 0,
    };
  },

  mounted() {
    this.drawBar();
    this.getOps();
    this.init();
  },
  methods: {
    init() {
      index(this.form).then(res => {
        const process = res.data.data.process;
        const task = res.data.data.task;
        if (task.task > -1) {
          this.taskList = [
            task.task,
            task.timeout,
            task.unfinished,
            task.todayTask,
            task.todayUnfinished,
            task.todayTimeout,
          ];
          this.drawTask();
        }
        this.processLeft.forEach(e => {
          e.num = process[e.type];
          e.todayNum = process[e.todayType];
        });
        this.processRight.forEach(e => {
          e.num = process[e.type];
        });
      });
    },

    // 获取表格数据
    getOps() {
      const obj = {
        currentPage: 1,
        pageSize: 20,
      };
      getList(obj.currentPage, obj.pageSize).then(res => {
        this.opsList = res.data.data.records;
        this.scrollList();
      });
    },
    drawBar(type, name) {
      bar({ type: type || 'process', startTimeRange: this.form.startTimeRange }).then(res => {
        let data = res.data.data;
        let maxVal = Math.max.apply(null, data.yData);
        let myChart = echarts.init(document.getElementById('bar'), null, {});
        let option = {
          backgroundColor: '',
          title: {
            text: `${name || '流程总数'} - 统计`,
            textStyle: {
              align: 'center',
              color: '#9E78ED',
              fontSize: 16,
            },
            top: '2%',
            left: 'center',
          },
          grid: {
            left: '5%',
            right: '5%',
            top: '25%',
            bottom: '10%', //也可设置left和right设置距离来控制图表的大小
          },
          tooltip: {
            trigger: 'axis',
            formatter: function (params) {
              const series = params[1];
              return series.name + '<br/>数量：' + series.value + '<br/>';
            },
            axisPointer: {
              type: 'shadow',
              label: {
                show: true,
              },
            },
          },
          xAxis: {
            data: data.xData,
            axisLine: {
              show: true, //隐藏X轴轴线
              lineStyle: {
                color: '#f1f1f1',
              },
            },
            axisTick: {
              show: true, //隐藏X轴刻度
            },
            axisLabel: {
              show: true,
              textStyle: {
                color: '#333', //X轴文字颜色
              },
            },
          },
          yAxis: [
            {
              type: 'value',
              name: '数量',
              nameTextStyle: {
                color: '#333',
              },
              splitLine: {
                show: false,
              },
              axisTick: {
                show: true,
              },
              axisLine: {
                show: true,
                lineStyle: {
                  color: '#FFFFFF',
                },
              },
              axisLabel: {
                show: true,
                textStyle: {
                  color: '#333',
                },
              },
            },
            {
              type: 'value',
              name: '',
              nameTextStyle: {
                color: '#333',
              },
              position: 'right',
              splitLine: {
                show: false,
              },
              axisTick: {
                show: false,
              },
              axisLine: {
                show: false,
              },
              axisLabel: {
                show: false,
                formatter: '{value} %', //右侧Y轴文字显示
                textStyle: {
                  color: '#333',
                },
              },
            },
            {
              type: 'value',
              gridIndex: 0,
              min: 0,
              max: maxVal,
              splitNumber: 8,
              splitLine: {
                show: false,
              },
              axisLine: {
                show: false,
              },
              axisTick: {
                show: false,
              },
              axisLabel: {
                show: false,
              },
              splitArea: {
                show: true,
                areaStyle: {
                  color: ['rgba(250,250,250,0.0)', 'rgba(250,250,250,0.05)'],
                },
              },
            },
          ],
          series: [
            {
              name: '',
              type: 'line',
              yAxisIndex: 1, //使用的 y 轴的 index，在单个图表实例中存在多个 y轴的时候有用
              smooth: true, //平滑曲线显示
              showAllSymbol: true, //显示所有图形。
              symbol: 'circle', //标记的图形为实心圆
              symbolSize: 6, //标记的大小
              itemStyle: {
                //折线拐点标志的样式
                color: '#9E78ED',
              },
              lineStyle: {
                color: '#9E78ED',
              },
              areaStyle: {
                normal: {
                  color: new echarts.graphic.LinearGradient(
                    0,
                    0,
                    0,
                    1,
                    [
                      { offset: 0, color: 'rgba(230,221,251, 0.9)' },
                      { offset: 1, color: 'rgba(230,221,251, 0)' },
                    ],
                    false
                  ),

                  shadowColor: 'rgba(230,221,251, 0.9)', //阴影颜色
                  shadowBlur: 9,
                },
              },
              data: data.yData,
            },
            {
              name: '流程',
              type: 'bar',
              barWidth: 10,
              itemStyle: {
                normal: {
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                      offset: 0,
                      color: '#6978F8',
                    },
                    {
                      offset: 1,
                      color: '#B497F4',
                    },
                  ]),
                },
              },
              data: data.yData,
            },
          ],
        };
        myChart.setOption(option);
        window.addEventListener('resize', () => {
          myChart.resize();
        });
      });
    },
    drawTask() {
      let myChart2 = echarts.init(document.getElementById('task'), null, {});
      let option = {
        backgroundColor: '',
        title: {
          text: '任务统计',
          textStyle: {
            align: 'center',
            color: '#ED5355',
            fontSize: 16,
          },
          top: '2%',
          left: 'center',
        },
        grid: {
          left: '5%',
          top: '18%',
          bottom: '12%',
          right: '5%',
        },
        xAxis: {
          data: [
            '任务总数',
            '超时任务',
            '进行中任务',
            '今日任务',
            '今日进行中任务',
            '今日超时任务',
          ],
          axisTick: {
            show: false,
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(255, 129, 109, 0.1)',
              width: 1, //这里是为了突出显示加上的
            },
          },
          axisLabel: {
            textStyle: {
              color: '#999',
              fontSize: 10,
            },
          },
        },
        yAxis: [
          {
            splitNumber: 2,
            axisTick: {
              show: false,
            },
            axisLine: {
              lineStyle: {
                color: 'rgba(255, 129, 109, 0.1)',
                width: 1, //这里是为了突出显示加上的
              },
            },
            axisLabel: {
              textStyle: {
                color: '#999',
              },
            },
            splitArea: {
              areaStyle: {
                color: 'rgba(255,255,255,.5)',
              },
            },
            splitLine: {
              show: true,
              lineStyle: {
                color: 'rgba(255, 129, 109, 0.2)',
                width: 0.8,
                type: 'dashed',
              },
            },
          },
        ],
        series: [
          {
            name: 'hill',
            type: 'pictorialBar',
            barCategoryGap: '0%',
            symbol: 'path://M0,10 L10,10 C5.5,10 5.5,5 5,0 C4.5,5 4.5,10 0,10 z',
            label: {
              show: true,
              position: 'top',
              distance: 15,
              color: '#DB5E6A',
              fontWeight: 'bolder',
              fontSize: 15,
            },
            itemStyle: {
              normal: {
                color: {
                  type: 'linear',
                  x: 0,
                  y: 0,
                  x2: 0,
                  y2: 1,
                  colorStops: [
                    {
                      offset: 0,
                      color: 'rgba(232, 94, 106, .8)', //  0%  处的颜色
                    },
                    {
                      offset: 1,
                      color: 'rgba(232, 94, 106, .1)', //  100%  处的颜色
                    },
                  ],
                  global: false, //  缺省为  false
                },
              },
              emphasis: {
                opacity: 1,
              },
            },
            data: this.taskList,
            z: 10,
          },
        ],
      };

      myChart2.setOption(option);
      window.addEventListener('resize', () => {
        myChart2.resize();
      });
    },
    // 列表滚动
    scrollList() {
      const swiperList = document.getElementsByClassName('swiperlist');
      this.animate = setInterval(() => {
        for (let i = 0; i < swiperList.length; i++) {
          let maxtop = swiperList[i].scrollHeight - swiperList[i].offsetHeight;
          if (maxtop === 0) {
            continue;
          }
          swiperList[i].scrollTop += 1;
          if (swiperList[i].scrollTop >= maxtop - 1) {
            swiperList[i].scrollTop = 0;
          }
        }
      }, 80);
    },
    // 日期清空
    handleReset() {
      this.form.startTimeRange = '';
      this.form.startTimeRangeArr = [];
      this.init();
      this.drawBar(this.barParams.type, this.barParams.name);
    },
    // 日期搜索
    handleSubmit() {
      if (this.form.startTimeRangeArr) {
        this.form.startTimeRange = this.form.startTimeRangeArr.join(',');
        this.init();
        this.drawBar(this.barParams.type, this.barParams.name);
      } else this.$message.error('请选择时间');
    },
    // col点击
    handleCol(type, name) {
      this.barParams = {
        type,
        name,
      };
      this.drawBar(this.barParams.type, this.barParams.name);
    },
  },
  deactivated() {
    clearInterval(this.animate);
  },
  beforeDestroy() {
    clearInterval(this.animate);
  },
};
</script>

<style lang="scss" scoped>
.statistics-container {
  padding: 0 10px 30px !important;
  .flex-one {
    flex: 1;
  }
  .mb20 {
    margin-bottom: 20px;
  }
  .cursor {
    cursor: pointer;
  }
  .justify-between {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .col {
    cursor: pointer;
    background: rgba(255, 255, 255, 0.7);
    border-radius: 10px;
    .name {
      color: #17307a;
      font-size: 15px;
    }
    .num {
      color: #2f2e4a;
      font-size: 32px;
    }
    p {
      padding: 0;
      margin: 0;
      box-sizing: border-box;
    }
  }
  .right-wrap {
    margin-bottom: 20px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      left: 20px;
      top: 10px;
    }
    &:nth-child(1)::before {
      background: #7a78ff;
    }
    &:nth-child(2)::before {
      background: #ffbc1b;
    }
    &:nth-child(3)::before {
      background: #00b6d7;
    }
    &:nth-child(4)::before {
      background: #ec3f3f;
    }
    .right-col {
      padding: 0 25px;
      height: 100px;
      &:hover {
        background: rgba(237, 83, 85, 0.1);
      }
    }
  }

  .left-col {
    height: 128px;
    padding-left: 25px;
    &:hover {
      background: rgba(237, 83, 85, 0.1);
    }
    & + .left-col {
      margin-top: 20px;
    }
    &::before {
      content: '';
      position: absolute;
      left: 10px;
      width: 5px;
      height: 64px;
      border-radius: 2px;
    }
    &:nth-child(1)::before {
      background: #ffcc40;
    }
    &:nth-child(2)::before {
      background: #302bc0;
    }
    &:nth-child(3)::before {
      background: #f05e5e;
    }
    .num {
      margin: 4px 0 6px;
    }
    .add-num {
      font-size: 13px;
      color: #9191a4;
    }
  }
  .ops-item {
    width: 100%;
    height: 330px;
    .ops-head {
      height: 40px;
      background: #f5f7f9;
      .th {
        color: #697582;
        font-size: 12px;
        // text-align: center;
      }
    }

    .swiperlist {
      width: 100%;
      height: 290px;
      overflow: hidden scroll;
    }
    .item {
      padding: 10px 10px;
      border-top: 1px solid #f0f0f0;
      color: #17307a;
      font-size: 12px;
      .name {
        font-size: 12px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .time {
        width: 140px;
      }
    }
  }
  :deep(.el-collapse) {
    border: none;
  }
  :deep(.el-collapse-item__wrap) {
    padding-top: 8px;
    border-radius: 10px;
  }
  :deep(.el-form-item--mini.el-form-item),
  :deep(.el-form-item--small.el-form-item) {
    margin-bottom: 0 !important;
  }
}
</style>
