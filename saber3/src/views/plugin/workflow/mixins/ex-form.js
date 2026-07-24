import {
  getFormByProcessId,
  getFormByProcessDefKey,
  startProcess,
  startProcessByKey,
  detail,
  completeTask,
  transferTask,
  delegateTask,
  rollbackTask,
  terminateProcess,
  addMultiInstance,
  withdrawTask,
} from '../api/process/process.js';

import Layout from '@/page/index/index.vue';
import defaultValues from './default-values.js';

import Print from '../utils/print.js';
import Watermark from '../utils/watermark.js';
import { versionCompare } from '../utils/index.js';

import { mapGetters } from 'vuex';
import module from './module.js'

import { version } from '@nutflow/nf-design-elp';

export default {
  mixins: [defaultValues, module(['d'])],
  computed: {
    ...mapGetters(['tag', 'userInfo', 'permission', 'isCollapse']),
  },
  data() {
    return {
      process: {}, // 流程定义/流程实例信息
      buttonList: [], // 配置按钮信息
      flow: [], // 流转信息
      userSelectType: '', // 人员选择类型 transfer转办 delegate委托 copy抄送 assignee审核人
      checkType: 'radio', // 人员选择check类型 radio单选 checkbox多选
      comment: '', // 评论
      bpmnOption: {}, // 流程图配置信息
      defaultChecked: '', // 人员选择默认选中
      waiting: true, // 骨架屏加载中
      isPrintShow: false, // 是否打印显示
    };
  },
  methods: {
    // 动态路由跳转
    dynamicRoute(row, type, async = false) {
      const { id, taskId, processInstanceId, processId, formKey, formUrl, processDefKey, params } = row;
      const businessType = row && row.variables ? row.variables.businessType : '';
      const resolvedFormKey = businessType === 'tenant_entry' ? 'wf_ex_TenantEntry' : formKey;
      let param = window.btoa(
        JSON.stringify({
          processId: id,
          taskId,
          processInsId: processInstanceId || processId,
          processDefKey,
          params,
        })
      );
      const encodedParam = encodeURIComponent(param);

      return new Promise(resolve => {
        if (resolvedFormKey && resolvedFormKey.startsWith('wf_ex_')) {
          if (formUrl) {
            // 配置了自定义路由
            const url = formUrl.startsWith('/workflow/process')
              ? formUrl.replace('/workflow/process', '/plugin/workflow/pages/process')
              : formUrl;
            this.$router.push(url + `?p=${encodedParam}`);
          } else {
            const exFormKey = resolvedFormKey.substring(6);
            // 动态添加路由
            this.$router.addRoute({
              path: `/plugin/workflow/pages/process/external`,
              component: Layout,
              children: [
                {
                  path: `${exFormKey}/${type}`,
                  name:
                    type == 'start'
                      ? `发起流程${exFormKey}`
                      : `流程详情${exFormKey}`,
                  component: () => import(`@/views/plugin/workflow/pages/external/${exFormKey}/${type}.vue`),
                },
              ],
            });
            this.$router.push(
              `/plugin/workflow/pages/process/external/${exFormKey}/${type}?p=${encodedParam}`
            );
          }
        } else {
          if (async) {
            resolve({
              row,
              type,
              param,
            });
          } else {
            this.$router.push(`/plugin/workflow/pages/process/form/${type}/${encodedParam}`);
          }
        }
      });
    },
    // 根据可读可写，过滤column
    filterAvueColumn(
      column,
      taskForm,
      isExForm = false,
      props = {
        label: 'label',
        prop: 'prop',
      }
    ) {
      const _this = this;

      if (!column || column.length == 0)
        return {
          column,
          vars: [],
        };

      const values = [];
      const vars = [];

      const versionFlag106 = version && versionCompare(version, '1.0.6');

      column.forEach(col => {
        let c = taskForm.find(s => s.id == col[props.prop]);
        if (c && c.readable) {
          // 非外置表单 处理事件
          if (!isExForm) {
            let event = ['change', 'blur', 'click', 'focus', 'lazyLoad', 'onLoad'];
            event.forEach(e => {
              if (col[e]) col[e] = eval((col[e] + '').replace(/this/g, '_this'));
            });
            if (col.event)
              Object.keys(col.event).forEach(
                key => (col.event[key] = eval((col.event[key] + '').replace(/this/g, '_this')))
              );
          }
          // 可写，记录需要提交的字段、处理字段默认值
          if (c.writable) {
            vars.push(col[props.prop]);
            if (col.value) col.value = _this.getDefaultValues(col.value);
          }
          // 不可写，清除校验、默认值
          else {
            if (col.children && col.children.column) {
              col.children.addBtn = false;
              col.children.delBtn = false;

              // 子表单中有字段可读可写，记录主字段可提交
              col.children.column.forEach(cc => {
                const child = taskForm.find(t => t.id == cc[props.prop]);
                if (child && child.writable && child.readable) vars.push(col[props.prop]);
              });
            } else {
              col.readonly = true;
              col.disabled = true;
            }
            delete col.rules;
            delete col.value;
            // delete col.event
            // event.forEach(e => delete col[e])
          }
          // 处理子表单/表格选择器
          if (col.children && col.children.column) {
            col.children.column = _this.filterAvueColumn(
              col.children.column,
              taskForm,
              isExForm
            ).column;
          }
          // 处理表格
          if (col.type == 'table' && col.rows && col.rows.length > 0) {
            col.rows.forEach(row => {
              if (row.cols && row.cols.length > 0) {
                row.cols.forEach(c => {
                  c.column = _this.filterAvueColumn(c.column, taskForm, isExForm).column;
                })
              }
            })
          }
          // 处理正则
          if (col.rules && col.pattern) {
            col.rules.forEach(c => {
              if (c.pattern) c.pattern = new RegExp(col.pattern);
            });
          }
          if (col.display !== false) col.display = true;

          // 处理字段必填
          let required = false;
          if (versionFlag106) {
            // 1.0.6及版本以下，required默认为true
            required = true;
          } else {
            // 1.0.7流程设计器表单配置支持required属性
            required = c.required;
          }
          if (!required && col.rules) {
            const index = col.rules.findIndex(c => c.required);
            if (index != -1) col.rules.splice(index, 1);
          }
        } else {
          col.display = false;
          col.hide = true;
        }
        values.push(col);
      });
      return {
        column: values,
        vars,
      };
    },
    /**
     * 获取流程发起表单
     * @param processId 流程定义id
     * @returns Promise({"process": "流程定义信息", "startForm": "开始节点表单"})
     */
    getStartForm(processId) {
      return new Promise((resolve, reject) => {
        getFormByProcessId({
          processId,
        })
          .then(res => {
            const { process } = res.data.data;
            process.hideComment = true;
            this.process = process;
            this.tag.name = `发起流程 - ${process.name}`;
            resolve(res.data.data);
          })
          .catch(() => {
            reject();
          });
      });
    },
    /**
     * 获取流程发起表单
     * @param processDefKey 流程定义key
     * @returns Promise({"process": "流程定义信息", "startForm": "开始节点表单"})
     */
    getStartFormByProcessDefKey(processDefKey) {
      return new Promise((resolve, reject) => {
        getFormByProcessDefKey({
          processDefKey,
        })
          .then(res => {
            const { process } = res.data.data;
            process.hideComment = true;
            this.process = process;
            this.tag.name = `发起流程 - ${process.name}`;
            resolve(res.data.data);
          })
          .catch(() => {
            reject();
          });
      });
    },
    /**
     * 发起流程
     * @param form {"processId": "流程定义id", ...表单自定义字段变量}
     */
    handleStartProcess(isExForm = false) {
      return new Promise(resolve => {
        if (!this.$refs.form) {
          this.$message.error("找不到可用的表单，请检查表单配置");
          return;
        }
        this.loading = true;
        this.$refs.form.validate((valid, done, msg) => {
          let form = this.deepClone(this.form);
          if (this.$refs.examineForm && this.$refs.examineForm.examineForm) {
            const { copyUser, assignee } = this.$refs.examineForm.examineForm;
            form = {
              ...form,
              copyUser,
              "wf_assignee": assignee,
            };
          }
          if (valid) {
            form['wf_form_option'] = JSON.stringify(this.option);
            form['wf_form_variable'] = JSON.stringify(form);
            startProcess(form)
              .then(res => {
                if (isExForm === true) {
                  resolve(res, done);
                } else {
                  this.$message.success('发起成功');
                  if (this.isDialog) {
                    this.visible = false;
                    this.$emit('success', res);
                  } else {
                    this.handleCloseTag('/plugin/workflow/pages/process/send');
                  }
                  done();
                  this.loading = false;
                }
              })
              .catch(() => {
                done();
                this.loading = false;
              });
          } else {
            done();
            this.loading = false;
            if (msg) {
              const key = Object.keys(msg)[0];
              const rules = Array.isArray(msg[key])
                ? msg[key]
                : msg[key][Object.keys(msg[key])[0]];
              this.$message.error(rules.map((r) => r.message).join(" | "));
            }
          }
        });
      });
    },
    /**
     * 发起流程
     * @param form {"processDefKey": "流程定义key", ...表单自定义字段变量}
     */
    handleStartProcessByKey(isExForm = false) {
      return new Promise(resolve => {
        if (!this.$refs.form) {
          this.$message.error("找不到可发起的表单，请检查表单配置");
          return;
        }
        this.loading = true;
        this.$refs.form.validate((valid, done, msg) => {
          let form = this.deepClone(this.form);
          if (this.$refs.examineForm && this.$refs.examineForm.examineForm) {
            const { copyUser, assignee } = this.$refs.examineForm.examineForm;
            form = {
              ...form,
              copyUser,
              "wf_assignee": assignee,
            };
          }
          if (valid) {
            form['wf_form_option'] = JSON.stringify(this.option);
            form['wf_form_variable'] = JSON.stringify(form);
            startProcessByKey(form)
              .then(res => {
                if (isExForm === true) {
                  resolve(res, done);
                } else {
                  this.$message.success('发起成功');
                  if (this.isDialog) {
                    this.visible = false;
                    this.$emit('success', res);
                  } else {
                    this.handleCloseTag('/plugin/workflow/pages/process/send');
                  }
                  done();
                  this.loading = false;
                }
              })
              .catch(() => {
                done();
                this.loading = false;
              });
          } else {
            done();
            this.loading = false;
            if (msg) {
              const key = Object.keys(msg)[0];
              const rules = Array.isArray(msg[key])
                ? msg[key]
                : msg[key][Object.keys(msg[key])[0]];
              this.$message.error(rules.map((r) => r.message).join(" | "));
            }
          }
        });
      });
    },
    /**
     * 获取流程任务详情
     * @param taskId 任务id
     * @param processInsId 流程实例id
     * @returns Promise({"process": "流程实例信息", "form": "表单信息", "flow": "流转信息", "button": "配置按钮信息", "bpmnOption": "流程图配置"})
     */
    getTaskDetail(taskId, processInsId) {
      return new Promise((resolve, reject) => {
        detail({
          taskId,
          processInsId,
        })
          .then(res => {
            const { process, form, flow, button } = res.data.data;
            const { xml } = process;

            const bpmnOption = {
              mode: 'view',
              xml,
              flows: this.handleResolveFlows(flow),
            };
            this.process = process;
            this.flow = flow;
            this.buttonList = button;
            this.bpmnOption = bpmnOption;
            this.tag.name = `流程详情 - ${process.processDefinitionName}`;
            resolve({
              process,
              form,
              flow,
              button,
              bpmnOption,
            });
          })
          .catch(() => {
            reject();
          });
      });
    },
    /**
     * 任务审核
     * @param pass 驳回/通过
     */
    handleCompleteTask(pass, variables) {
      return new Promise((resolve, reject) => {
        const { comment, copyUser, assignee, attachment } = this.$refs.examineForm.examineForm;
        if (!pass && !comment) {
          this.$message.error('请填写批复意见');
          this.submitLoading = false;
          reject();
          return;
        }
        variables['wf_form_option'] = JSON.stringify(this.option);
        variables['wf_form_variable'] = JSON.stringify(this.form);
        const { taskId, processInstanceId, processDefinitionName, processDefinitionId } =
          this.process;
        const param = {
          taskId,
          processInstanceId,
          processDefinitionName,
          processDefinitionId,
          pass,
          comment,
          copyUser,
          assignee,
          variables,
          attachment,
        };
        completeTask(param)
          .then(() => {
            resolve();
          })
          .catch(() => {
            reject();
          });
      });
    },
    /**
     * 驳回到指定节点
     * @param nodeId 节点id
     */
    handleRollbackTask(nodeId) {
      const { taskId } = this.process;
      rollbackTask({
        comment: this.comment,
        nodeId,
        taskId,
      }).then(() => {
        this.$message.success('回退成功');
        this.handleCloseTag('/plugin/workflow/pages/process/todo');
      });
    },
    /**
     * 终止流程
     */
    handleTerminateProcess() {
      const comment = this.comment;
      if (!comment) {
        this.$message.error('请填写批复意见');
        return;
      }
      this.$confirm('确定要终止此流程吗?', '警告', {
        type: 'warning',
      })
        .then(() => {
          const { taskId } = this.process;

          terminateProcess({
            taskId,
            comment,
          }).then(() => {
            this.$message.success('操作成功');
            this.handleCloseTag('/plugin/workflow/pages/process/todo');
          });
        })
        .catch(() => { });
    },
    // 人员选择弹窗
    handleUserSelect({ type, checkType }) {
      if (!this.comment && ['transfer', 'delegate'].includes(type)) {
        this.$message.error('请填写批复意见');
        return;
      }
      if (type == 'assignee') this.defaultChecked = this.$refs.examineForm.examineForm.assignee;
      else if (type == 'copy') this.defaultChecked = this.$refs.examineForm.examineForm.copyUser;

      this.$refs['user-select'].visible = true;
      this.userSelectType = type;
      this.checkType = checkType;
    },
    // 选人回调
    handleUserSelectConfirm(id, name) {
      const { comment, copyUser } = this.$refs.examineForm.examineForm;
      const { taskId, processInstanceId, processDefinitionName, processDefinitionId } =
        this.process;

      const type = this.userSelectType;
      const param = {
        taskId,
        processInstanceId,
        processDefinitionName,
        processDefinitionId,
        assignee: id,
        comment,
        copyUser,
      };
      if (type == 'transfer') {
        transferTask(param).then(() => {
          // 转办
          this.$message.success('转办成功');
          this.handleCloseTag('/plugin/workflow/pages/process/todo');
        });
      } else if (type == 'delegate') {
        // 委托
        delegateTask(param).then(() => {
          this.$message.success('委托成功');
          this.handleCloseTag('/plugin/workflow/pages/process/todo');
        });
      } else if (type == 'addInstance') {
        // 加签
        addMultiInstance(param).then(() => {
          this.$message.success('加签成功');
        });
      } else if (type == 'copy') {
        // 抄送
        this.$refs.examineForm.examineForm.copyUser = id;
        this.$refs.examineForm.examineForm.$copyUser = name;
      } else if (type == 'assignee') {
        // 指定下一步审批人
        this.$refs.examineForm.examineForm.assignee = id;
        this.$refs.examineForm.examineForm.$assignee = name;
      }
      this.$refs['user-select'].visible = false;
    },
    handleWithdrawTask() {
      const { taskId } = this.process;
      this.$confirm(
        '<p><span style="color: red;">撤销：</span>撤销终止此流程</p><p><span style="color: red;">撤回：</span>撤回到发起人重新提交，若当前流程不存在发起人节点，功能同撤销</p>',
        '请选择撤销/撤回操作',
        {
          type: 'warning',
          distinguishCancelAndClose: true,
          confirmButtonText: '撤销',
          cancelButtonText: '撤回',
          dangerouslyUseHTMLString: true,
        }
      )
        .then(() => {
          withdrawTask({
            taskId,
            withdrawType: 'wf_withdraw_end',
          }).then(() => {
            this.$message.success('操作成功');
            this.handleCloseTag('/plugin/workflow/pages/process/todo');
          });
        })
        .catch(action => {
          if (action == 'cancel') {
            withdrawTask({
              taskId,
              withdrawType: 'wf_withdraw_start',
            }).then(() => {
              this.$message.success('操作成功');
              this.handleCloseTag('/plugin/workflow/pages/process/todo');
            });
          }
        });
    },
    handlePrint() {
      // 打印
      const loading = this.$loading({
        lock: true,
        text: 'Loading',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)',
      });
      const option = this.deepClone(this.option);
      this.option.detail = true;
      if (this.option.column) {
        this.option.column.forEach(col => {
          this.handleTemporaryPrintOption(col);
        });
      }
      if (this.option.group) {
        this.option.group.forEach(g => {
          g.collapse = true;
          g.column.forEach(col => {
            this.handleTemporaryPrintOption(col);
          });
        });
      }
      // 汇总表单打印
      const summaryOption = this.deepClone(this.summaryOption);
      if (this.summaryOption && this.summaryOption.group) {
        this.summaryOption.group.forEach(g => {
          g.collapse = true;
          g.column.forEach(col => {
            this.handleTemporaryPrintOption(col);
          });
        });
      }
      this.isPrintShow = true;
      setTimeout(() => {
        loading.close();
        // const watermarkText = this.userInfo.user_name + " " + this.userInfo.dept_name
        // Watermark.set({ watermark_txt: watermarkText }) // 添加水印
        Print('#printBody');
        this.option = option;
        this.summaryOption = summaryOption;
        Watermark.remove(); // 删除水印
        this.isPrintShow = false;
      }, 500);
    },
    // 生成打印临时option
    handleTemporaryPrintOption(obj) {
      if (!obj.type) return;
      obj.span = 24;
      if (obj.type == 'dynamic') {
        obj.children.type = 'form';
        obj.children.column.forEach(col => {
          this.handleTemporaryPrintOption(col);
        });
      }
    },
    // 关闭当前tag，并跳转
    handleCloseTag(path) {
      this.$store.commit('DEL_TAG', this.tag);
      if (path) this.$router.push(path);
    },
    handleResolveFlows(flow) {
      // 优化多实例节点人员显示。
      flow = this.deepClone(flow);
      const result = {};
      flow.forEach(f => {
        if (result[f.historyActivityId]) {
          result[f.historyActivityId].push(f);
        } else {
          result[f.historyActivityId] = [f];
        }
      })
      for (let key in result) {
        const list = result[key];
        if (list.length > 1) {
          const node = flow.findLast((f) => f.historyActivityId == key);
          node.assigneeName = Array.from(new Set(list.map(l => l.assigneeName))).join("/");
        }
      }

      const flows = [];

      flow.forEach(f => {
        let { assigneeName, createTime, endTime, comments } = f;

        if (/Safari/.test(navigator.userAgent) && !/Chrome/.test(navigator.userAgent)) {
          // safari
          createTime = createTime.replace(/-/g, '/');
          endTime = endTime.replace(/-/g, '/');
        }

        const ff = {
          id: f.historyActivityId,
          class: !endTime && f.historyActivityType != 'candidate' ? 'nodePrimary' : '',
        };
        let tooltip = '';
        if (assigneeName) {
          tooltip = `<span title='${assigneeName}'>${assigneeName}</span><br>`;
          if (createTime)
            tooltip += `<span title='${createTime}'>${this.dateFormat(
              createTime,
              'YYYY-MM-DD HH:mm'
            )}</span><br>`;

          if (comments && comments.length > 0) {
            let comment;
            let { type, fullMessage } = comments.find(c => c.action == 'AddComment') || {};

            if (type == 'assigneeComment') {
              comment = '变更审核人：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'dispatchComment') {
              comment = '调度：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'transferComment') {
              comment = '转办：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'delegateComment') {
              comment = '委托：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'rollbackComment') {
              comment = '驳回：' + fullMessage;
              ff.class = 'nodeError';
            }
            if (type == 'terminateComment') {
              comment = '终止：' + fullMessage;
              ff.class = 'nodeError';
            }
            if (type == 'addMultiInstanceComment') {
              comment = '加签：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'deleteMultiInstanceComment') {
              comment = '减签：' + fullMessage;
              ff.class = 'nodeError';
            }
            if (type == 'withdrawComment') {
              comment = '撤销：' + fullMessage;
              ff.class = 'nodeWarn';
            }
            if (type == 'deleteProcessComment') {
              comment = '删除流程：' + fullMessage;
              ff.class = 'nodeError';
            }
            if (type == 'comment') {
              comment = '审批：' + fullMessage;
              ff.class = 'nodeSuccess';
            }
            if (comment) tooltip += `<span title='${comment}'>${comment}</span>`;
          }
          ff.tooltip = tooltip;
        }

        if (f.historyActivityType == 'sequenceFlow') ff.class = 'lineWarn';
        else if (!ff.class && f.historyActivityType != 'candidate') ff.class = 'nodeSuccess';

        const index = flows.findIndex(fl => fl.id == f.historyActivityId);
        if (index != -1) flows.splice(index, 1, ff);
        else flows.push(ff);
      });
      return flows;
    },
    // 上传组件预览
    handleUploadPreview(file, column, done) {
      const { url } = file;
      const video = /\.(swf|avi|flv|mpg|rm|mov|wav|asf|3gp|mkv|rmvb|ogg|mp4)/;
      const img = /\.(gif|jpg|jpeg|png|GIF|JPG|PNG)/;
      if (video.test(url) || img.test(url)) done();
      else window.open(url);
    },
  },
};
