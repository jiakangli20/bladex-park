import { customerApi, publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

Page({
  data: {
    serviceKey: '',
    service: { id: '', title: '物业服务申请', desc: '提交需求后由园区管理员受理。', submitText: '提交物业申请' },
    form: { contact: '', phone: '', room: '', carNo: '', useTime: '', amount: '', content: '' },
  },

  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/property-form/index?id=${options.id || ''}&type=${options.type || ''}`)) return
    if (!hasCapability('customer.profile.view')) {
      wx.showToast({ title: '当前账号暂无物业服务权限', icon: 'none' })
      navigateBackOr('/pages/index/index')
      return
    }
    const services = await publicApi.propertyServices()
    const parkId = getSession()?.parkId
    const parkServices = parkId ? services.filter(item => String(item.parkId) === String(parkId)) : services
    const selected: Record<string, any> | undefined = parkServices.find(item => String(item.id) === options.id)
      || parkServices.find(item => String(item.type).includes(options.type || ''))
    if (!selected) {
      wx.showToast({ title: '该物业服务暂未配置', icon: 'none' })
      return
    }
    this.setData({
      serviceKey: options.type || selected.type,
      service: {
        id: String(selected.id),
        title: selected.title || '物业服务申请',
        desc: selected.desc || '提交需求后由园区管理员受理。',
        submitText: (options.type || selected.type) === 'parking' ? '提交车位申请' : '提交物业申请',
      },
    })
  },

  goBack() { navigateBackOr('/pages/services/index?tab=property') },
  handleInput(event: WechatMiniprogram.Input) {
    const field = event.currentTarget.dataset.field as string | undefined
    if (field) this.setData({ [`form.${field}`]: event.detail.value })
  },

  async submitForm() {
    const { contact, phone, content, room, carNo, useTime, amount } = this.data.form
    if (!contact || !/^1\d{10}$/.test(phone) || !content) {
      wx.showToast({ title: '请填写联系人、正确手机号和需求说明', icon: 'none' })
      return
    }
    const extra = [carNo && `车牌：${carNo}`, useTime && `使用时间：${useTime}`, amount && `登记金额：${amount}`].filter(Boolean).join('；')
    await customerApi.createPropertyOrder({
      serviceId: this.data.service.id,
      contactName: contact,
      contactPhone: phone,
      roomInfo: room,
      demandDesc: extra ? `${content}；${extra}` : content,
      priority: '1',
    })
    wx.showModal({
      title: '提交成功', content: '已生成物业服务申请，可在我的工单查看处理进度。',
      confirmText: '查看工单', cancelText: '留在本页',
      success: result => result.confirm && wx.navigateTo({ url: '/pages/work-orders/index?tab=property' }),
    })
  },
})
