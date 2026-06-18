<template>
  <nf-container class="nf-design-step" v-if="moduleLoadInit">
    <el-steps :active="step" finish-status="success" simple style="margin-bottom: 20px">
      <el-step title="设计表单" icon="el-icon-edit"></el-step>
      <el-step title="设计流程" icon="el-icon-upload">
        <template #title>
          <span>设计流程</span>
          <el-icon @click="handleFullScreen">
            <el-icon-full-screen />
          </el-icon>
        </template>
      </el-step>
      <el-step title="完成" icon="el-icon-circle-check"></el-step>
    </el-steps>

    <div v-show="step == 0">
      <nf-form ref="form1" :option="step1.option" v-model="step1.form">
        <template #tip>
          <el-link
            type="primary"
            underline="never"
            @click="$router.push('/plugin/workflow/pages/design/form')"
          >
            没有想要的表单？点击去设计
          </el-link>
        </template>
        <template #form>
          <nf-form
            v-if="
              option &&
              ((option.column && option.column.length > 0) ||
                (option.group && option.group.length > 0))
            "
            ref="form2"
            v-model="form"
            :option="option"
          ></nf-form>
        </template>
      </nf-form>
    </div>
    <div v-if="step == 1">
      <nf-design
        id="bpmn2"
        ref="bpmn2"
        style="height: calc(100vh - 280px); background: white"
        :options="step2.option"
      ></nf-design>
    </div>
    <div v-if="step == 2">
      <nf-design
        ref="bpmn3"
        style="height: calc(100vh - 280px)"
        :options="step3.option"
      ></nf-design>
    </div>

    <div
      class="foot-item"
      :style="{
        width: isCollapse ? 'calc(100% - 71px)' : 'calc(100% - 241px)',
      }"
    >
      <el-button type="primary" size="default" v-if="step > 0" @click="step--"> 上一步 </el-button>
      <el-button type="success" size="default" v-if="step < 2" @click="handleNextStep">
        下一步
      </el-button>
      <el-button type="success" size="default" v-if="step == 2" @click="handleSave">
        保存
      </el-button>
    </div>
  </nf-container>
</template>

<script>
import { getDetail as getFormByKey } from '../../api/design/form';
import { submit, getDetail } from '../../api/design/model';
import { getList as buttonList } from '../../api/design/button';
import { getList as formList } from '../../api/design/form';
import { getList as conditionList } from '../../api/design/condition';
import { getList as listenerList } from '../../api/design/listener';

import { fullscreenToggel } from '@/utils/util';

import { mapGetters } from 'vuex';
import module from '../../mixins/module';

export default {
  name: 'design',
  computed: {
    ...mapGetters(['tag', 'isCollapse', 'language']),
  },
  mixins: [module('d')],
  watch: {
    '$route.params.id': {
      handler(val) {
        if (!val || val == 0) return;
        getDetail(val).then(res => {
          this.process = res.data.data;
          const { formKey, modelXml, exForm } = this.process;
          this.step2.option.xml = modelXml;
          this.step2.option.process = this.process;
          if (formKey.startsWith('wf_ex_')) {
            // 外置表单
            const column = [];
            exForm.forEach(ex => {
              column.push({
                label: ex.name,
                prop: ex.id,
                readable: true,
                writable: true,
              });
            });
            this.step1.form.column = column;
            this.step1.form.formType = 2;
            this.step1.form.exFormKey = formKey.substring(6);
          } else if (formKey.startsWith('wf_indep_')) {
            this.step1.form.formType = 3;
          } else {
            this.step1.form.formKey = formKey;
          }

          this.tag.name = `模型设计 - ${this.process.name}`;
        });
      },
      immediate: true,
    },
    language(val) {
      const option = {
        lang: val,
      };
      if (this.$refs.bpmn2) {
        this.$refs.bpmn2.getData('xml', false, false).then(data => {
          option.xml = data;
          this.step2.option = {
            ...this.step2.option,
            ...option,
          };
        });
      } else
        this.step2.option = {
          ...this.step2.option,
          ...option,
        };
    },
  },
  data() {
    const _this = this;
    return {
      form: {},
      option: {},
      step: 0,
      step1: {
        form: {},
        option: {
          menuBtn: false,
          group: [
            {
              labelPosition: 'left',
              label: '选择表单',
              icon: 'el-icon-warning',
              arrow: false,
              column: [
                {
                  label: '表单类型',
                  prop: 'formType',
                  type: 'radio',
                  dicData: [
                    {
                      label: '内置表单',
                      value: 1,
                    },
                    {
                      label: '外置表单',
                      value: 2,
                    },
                    {
                      label: '节点独立表单',
                      value: 3,
                    },
                  ],
                  span: 24,
                  value: 1,
                  change: val => {
                    if (!val) return;
                    val = typeof val == 'object' ? val.value : val;
                    if (val == 1) {
                      this.findObject(this.step1.option.group, 'exFormKey').display = false;
                      this.findObject(this.step1.option.group, 'column').display = false;
                      this.findObject(this.step1.option.group, 'formKey').display = true;
                      this.findObject(this.step1.option.group, 'tip').display = true;
                      // this.findObject(this.step1.option.group, 'form').display = true
                      this.step1.option.group[1].display = true;
                    } else if (val == 2) {
                      this.findObject(this.step1.option.group, 'exFormKey').display = true;
                      this.findObject(this.step1.option.group, 'column').display = true;
                      this.findObject(this.step1.option.group, 'formKey').display = false;
                      this.findObject(this.step1.option.group, 'tip').display = false;
                      this.option = {};
                      this.step1.option.group[1].display = true;
                    } else if (val == 3) {
                      this.findObject(this.step1.option.group, 'exFormKey').display = false;
                      this.findObject(this.step1.option.group, 'column').display = false;
                      this.findObject(this.step1.option.group, 'formKey').display = false;
                      this.findObject(this.step1.option.group, 'tip').display = false;
                      this.option = {};
                      this.step1.option.group[1].display = false;
                    }
                  },
                },
                {
                  label: '表单key',
                  prop: 'exFormKey',
                  display: false,
                  rules: [{ required: true, message: '请输入外置表单key' }],
                },
                {
                  label: '表单',
                  prop: 'formKey',
                  type: 'select',
                  props: {
                    label: 'name',
                    value: 'formKey',
                  },
                  dicData: [],
                  change: val => {
                    if (val == undefined) return;
                    val = typeof val == 'object' ? val.value : val;
                    _this.option = { menuBtn: false, readonly: true };
                    if (val) {
                      getFormByKey({ formKey: val })
                        .then(res => {
                          _this.option = {
                            ...eval('(' + res.data.data.content.replace(/this/g, '_this') + ')'),
                            menuBtn: false,
                            readonly: true,
                          };
                          _this.findObject(this.step1.option.group, 'form').display = true;
                        })
                        .catch(err => {
                          console.log(err);
                          _this.findObject(this.step1.option.group, 'form').display = false;
                        });
                    } else {
                      _this.findObject(this.step1.option.group, 'form').display = false;
                    }
                  },
                  rules: [{ required: true, message: '请选择表单' }],
                  display: true,
                  filterable: true,
                },
                {
                  labelWidth: 0,
                  prop: 'tip',
                },
              ],
            },
            {
              label: '表单预览',
              icon: 'el-icon-view',
              display: true,
              arrow: false,
              column: [
                {
                  prop: 'form',
                  showLabel: false,
                  span: 24,
                  display: true,
                },
                {
                  prop: 'column',
                  labelWidth: '0',
                  tip: '可用于控制外置表单字段的显隐配置，如果希望自己控制请忽略此字段',
                  tipPlacement: 'top',
                  span: 24,
                  type: 'dynamic',
                  children: {
                    align: 'center',
                    column: [
                      {
                        label: '字段',
                        prop: 'label',
                        rules: [{ required: true, message: '请输入字段名' }],
                      },
                      {
                        label: '属性',
                        prop: 'prop',
                        rules: [{ required: true, message: '请输入属性名' }],
                      },
                      {
                        label: '默认可读',
                        prop: 'readable',
                        type: 'switch',
                        disabled: true,
                        value: true,
                      },
                      {
                        label: '默认可写',
                        prop: 'writable',
                        type: 'switch',
                        disabled: true,
                        value: true,
                      },
                    ],
                  },
                  display: false,
                },
              ],
            },
          ],
        },
      },
      step2: {
        option: {
          config: false,
          mode: 'edit',
          engine: 'flowable',
          toolbar: [
            'open',
            'create',
            'fit',
            'zoom-in',
            'zoom-out',
            'undo',
            'redo',
            'import',
            'preview',
          ],
          script: {
            script: {
              enable: false,
              alert:
                '使用之前请先了解脚本会带来的危害，若确定使用请参考文档放开此配置。<br>1、脚本中可以完全访问JVM。<br>2、脚本执行时阻塞许多系统资源。<br>3、脚本执行死循环/占用大量内存等会导致程序崩溃。',
            },
            shell: {
              enable: false,
              alert:
                '使用之前请先了解Shell会带来的危害，若确定使用请参考文档放开此配置。<br>因不确定是否可执行危险命令，如rm -rf *，请充分了解之后再使用。',
              pattern:
                '(rm|mv|kill|ifconfig|docker|reboot|dd|wget|shutdown|halt|poweroff|init|:(){:|:&};:|^foo^bar)',
            },
          },
        },
      },
      step3: {
        option: {
          config: false,
          mode: 'view',
          simulation: true,
          minimap: true,
          engine: 'flowable',
        },
      },
      process: {},
      fullscreen: false,
    };
  },
  mounted() {
    this.getButtonList();
    this.getUserListV2();
    this.getFormList();
    this.getConditionList();
    this.getListenerList();
  },
  methods: {
    handleNextStep() {
      switch (this.step) {
        case 0:
          this.$refs.form1.validate((valid, done, msg) => {
            if (valid) {
              const { formType, formKey, exFormKey, column } = this.step1.form;
              if (formType == 1) {
                // 内置表单
                this.process.formKey = formKey;
                this.step2.option.form = this.option;
              } else if (formType == 2) {
                // 外置表单
                this.process.formKey = 'wf_ex_' + exFormKey;
                this.step2.option.exForm = {
                  exFormKey,
                  column,
                };
              } else if (formType == 3) {
                // 独立表单
                this.step2.option.indepForm = {
                  mode: 'indep',
                  list: this.formList,
                };
              }
              this.step++;
              done();
            }
          });
          break;
        case 1:
          const registry = this.$refs.bpmn2.getElementRegistry().getAll();
          // 节点独立表单
          if (this.step1.form.formType == 3) {
            let errorList = [];
            registry.forEach(ele => {
              this.validateIndepFormOption(ele, errorList);
            });
            if (errorList.length > 0) {
              errorList = new Set(errorList);
              let message = '';
              errorList.forEach(err => {
                const { businessObject } = err;
                const { id, name } = businessObject;
                message += `<p>${name || id} 节点未正确配置表单</p>`;
              });
              this.$message({
                type: 'error',
                dangerouslyUseHTMLString: true,
                message,
              });
              return;
            }
          }
          // 校验多实例用户配置是否正确
          const result = this.validateMultiInstanceUserOption(registry);
          if (result) return;

          this.$refs.bpmn2.getData('xml').then(data => {
            this.step2.option.xml = data;
            this.step3.option.xml = data;
            this.process.modelXml = data;
            this.step++;
          });
          break;
      }
    },
    handleSave() {
      const registry = this.$refs.bpmn3.getElementRegistry().getAll();
      const { businessObject } = registry[0];
      const { id, name, documentation } = businessObject;
      const description = documentation && documentation.length > 0 ? documentation[0].text : null;

      const { formType } = this.step1.form;
      if (formType == 3) {
        // 节点独立表单
        const startEvent = registry.find(r => r.type == 'bpmn:StartEvent');
        if (startEvent) {
          const indepFormKey = startEvent.businessObject.extensionElements.values.find(
            v => v.$type == 'flowable:indepFormKey'
          );
          if (indepFormKey) this.process.formKey = 'wf_indep_' + indepFormKey.value;
        }
      }

      const params = {
        ...this.process,
        modelKey: id,
        name,
        description,
      };

      if (this.process.id) {
        this.$confirm('是否将此模型保存为新版本？这意味着可以返回到以前的版本。', '提示', {
          distinguishCancelAndClose: true,
          confirmButtonText: '否',
          cancelButtonText: '是',
          type: 'warning',
        })
          .then(() => {
            params.newVersion = false;

            submit(params).then(() => {
              this.$message.success('操作成功');
              this.$store.commit('DEL_TAG', this.tag);
              this.$router.push('/plugin/workflow/pages/design/model');
            });
          })
          .catch(action => {
            if (action == 'cancel') {
              params.newVersion = true;

              submit(params).then(() => {
                this.$message.success('操作成功');
                this.$store.commit('DEL_TAG', this.tag);
                this.$router.push('/plugin/workflow/pages/design/model');
              });
            }
          });
      } else {
        submit(params).then(() => {
          this.$message.success('操作成功');
          this.$store.commit('DEL_TAG', this.tag);
          this.$router.push('/plugin/workflow/pages/design/model');
        });
      }
    },
    validateIndepFormOption(element, errorList) {
      const { type, businessObject, children } = element;
      const indepFormKey = 'flowable:IndepFormKey';
      const indepFormSummary = 'flowable:IndepFormSummary';
      if ('bpmn:StartEvent' == type) {
        const { extensionElements, eventDefinitions } = businessObject;
        if (eventDefinitions && eventDefinitions.length > 0) {
          return;
        }
        if (extensionElements && extensionElements.values && extensionElements.values.length > 0) {
          const summary = extensionElements.values.find(v => v.$type == indepFormSummary);
          if (
            !extensionElements.values.find(v => v.$type == indepFormKey) &&
            (!summary || summary == '0')
          )
            errorList.push(element);
        } else errorList.push(element);
      } else if ('bpmn:UserTask' == type) {
        const summary = businessObject['indepFormSummary'];
        if (!businessObject['indepFormKey'] && (!summary || summary == '0'))
          errorList.push(element);
      } else if ('bpmn:SubProcess' == type) {
        children.forEach(ele => this.validateIndepFormOption(ele, errorList));
      }
    },
    validateMultiInstanceUserOption(elements) {
      let result = false;
      outer: for (const element of elements) {
        if (['bpmn:UserTask', 'bpmn:SubProcess', 'bpmn:CallActivity'].includes(element.type)) {
          const { businessObject, children } = element;
          const { loopCharacteristics, extensionElements, id, name } = businessObject;
          if (loopCharacteristics) {
            // 多实例
            const { values } = extensionElements;
            const assignee = values.find(v => v.$type == 'flowable:Assignee');
            if (!assignee) {
              result = true;
              this.$message.error(`【${name || id}】节点为多实例，人员配置不能为空`);
              break;
            }
            if (element.type == 'bpmn:SubProcess') {
              result = this.validateMultiInstanceUserOption(children);
              if (result) break outer;
            }
          }
        }
      }
      return result;
    },
    getButtonList() {
      buttonList(1, -1, { status: 1 }).then(res => {
        const list = res.data.data.records.map(l => {
          return {
            label: l.name,
            prop: l.buttonKey,
            display: l.display,
          };
        });
        this.step2.option.button = list;
      });
    },
    getFormList() {
      formList(1, -1, { status_equal: '1' }).then(res => {
        this.formList = res.data.data.records;
        this.findObject(this.step1.option.group, 'formKey').dicData = this.formList;
      });
    },
    getUserListV2() {
      this.step2.option.user = {
        version: 'v2',
        userUrl: '/blade-system/search/user',
        roleUrl: '/blade-system/search/role',
        deptUrl: '/blade-system/dept/lazy-list',
        postUrl: '/blade-system/search/post',
        customUrl: '/blade-workflow/design/condition/list',
      };
    },
    getConditionList() {
      conditionList(1, -1, { type: 'flow', status: 1 }).then(res => {
        this.step2.option.condition = res.data.data.records;
      });
    },
    getListenerList() {
      listenerList(1, -1, { status: 1 }).then(res => {
        this.step2.option.listener = res.data.data.records;
      });
    },
    handleFullScreen() {
      fullscreenToggel();
      this.$store.commit('SET_COLLAPSE');
    },
  },
};
</script>
<style scoped lang="scss">
.nf-design-step {
  margin-bottom: 66px !important;
  margin-top: 1px !important;
  outline: 1px solid #e4e7ed;
}
.foot-item {
  position: fixed;
  bottom: 0;
  margin-left: -20px;
  // right: 0;
  z-index: 101;
  height: 66px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  -webkit-transition: 0.3s;
  transition: 0.3s;
  -webkit-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
