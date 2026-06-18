import { getDetail, submit } from '../api/process/draft';

export default {
  methods: {
    initDraft({ processDefId, taskId }) {
      return new Promise((resolve, reject) => {
        getDetail({ processDefId, taskId, platform: 'pc' }).then(res => {
          const { data } = res.data
          if (data && data.variables && Object.keys(data.variables).length > 0) resolve(data.variables)
          else reject()
        }).catch((e) => {
          reject(e)
        })
      })
    },
    handleDraft({ processDefId, taskId, formKey, variables }) {
      if (!this.$refs.form) {
        this.$message.error("找不到可用的表单，请检查表单配置");
        return;
      }
      this.$confirm('保存至草稿箱并关闭?', '提示', {
        type: 'warning'
      }).then(() => {
        submit({ processDefId, taskId, formKey, variables: JSON.stringify(variables), platform: 'pc' }).then(() => {
          if (!taskId) this.handleCloseTag('/plugin/workflow/pages/process/start')
          else this.handleCloseTag('/plugin/workflow/pages/process/todo')
        })
      }).catch(() => {
      })
    }
  }
}