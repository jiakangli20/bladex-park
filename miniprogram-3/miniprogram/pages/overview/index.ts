import { adminApi } from "../../services/miniapp";
import { hasCapability, requireLogin } from "../../utils/session";

const demoMetrics = [
  {
    label: "出租率",
    value: "86",
    unit: "%",
    tone: "blue",
    icon: "↗",
    trend: "+2.4%",
  },
  {
    label: "入驻企业",
    value: "128",
    unit: "家",
    tone: "green",
    icon: "◇",
    trend: "+6 家",
  },
  {
    label: "可租面积",
    value: "2.4",
    unit: "万m²",
    tone: "orange",
    icon: "▦",
    trend: "待租",
  },
  {
    label: "待办工单",
    value: "16",
    unit: "件",
    tone: "red",
    icon: "✓",
    trend: "需处理",
  },
];

const demoProgress = {
  totalArea: "12.8万m²",
  rentedArea: "10.4万m²",
  rentRate: 86,
  monthReceivable: "326.8万",
  monthReceived: "284.5万",
  collectionRate: 87,
};
const demoTodos = [
  { label: "物业申请", count: 8, tone: "blue", icon: "⌂", desc: "待受理工单" },
  {
    label: "增值服务",
    count: 5,
    tone: "orange",
    icon: "✦",
    desc: "待跟进服务",
  },
  { label: "通知提醒", count: 3, tone: "red", icon: "!", desc: "未读服务通知" },
];
const demoTrend = [
  { month: "3月", value: "268", height: 63 },
  { month: "4月", value: "286", height: 72 },
  { month: "5月", value: "274", height: 67 },
  { month: "6月", value: "302", height: 82 },
  { month: "7月", value: "285", height: 75 },
  { month: "8月", value: "327", height: 94 },
];
const demoTenants = [
  {
    id: "tenant-001",
    companyName: "上海科技有限公司",
    industry: "软件研发",
    room: "A座12层 1201-1205",
    area: "820m²",
    leaseEnd: "2027.12.31",
    rentStatus: "正常",
    tone: "blue",
    occupancy: 78,
  },
  {
    id: "tenant-002",
    companyName: "苏州智造产业服务有限公司",
    industry: "智能制造",
    room: "C座20层 整层",
    area: "1,200m²",
    leaseEnd: "2027.07.31",
    rentStatus: "待缴",
    tone: "orange",
    occupancy: 92,
  },
  {
    id: "tenant-003",
    companyName: "金控企业服务中心",
    industry: "企业服务",
    room: "B座8层 801-806",
    area: "560m²",
    leaseEnd: "2026.04.30",
    rentStatus: "正常",
    tone: "green",
    occupancy: 64,
  },
];

const nowText = () => {
  const date = new Date();
  return `${String(date.getHours()).padStart(2, "0")}:${String(
    date.getMinutes()
  ).padStart(2, "0")}`;
};

const normalizeTenants = (items: Record<string, any>[]) =>
  items.map((item, index) => {
    const companyName = String(item.companyName || item.enterpriseName || '未命名企业');
    return {
      ...item,
      id: String(item.id || index),
      companyName,
      industry: item.industry || '-',
      shortName: companyName.slice(0, 1),
      room: item.room || '-',
      area: item.area ? `${item.area}m²` : '-',
      leaseEnd: item.leaseEnd || item.leasePeriod?.split(" - ").pop() || '-',
      rentStatus: item.rentStatus || (['0', '1'].includes(String(item.status)) ? '正常' : '需关注'),
      occupancy: Number(item.occupancy || 0),
      tone: ['blue', 'green', 'orange'][index % 3],
    };
  });

const formatArea = (value: unknown) => `${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}m²`;

const topIndustryOf = (tenants: Record<string, any>[]) => {
  const counts = new Map<string, number>();
  tenants.forEach(item => { if (item.industry) counts.set(item.industry, (counts.get(item.industry) || 0) + 1); });
  return [...counts.entries()].sort((left, right) => right[1] - left[1])[0]?.[0] || '-';
};

Page({
  data: {
    loading: false,
    usingDemo: true,
    lastUpdated: nowText(),
    metrics: demoMetrics,
    progress: demoProgress,
    trend: demoTrend,
    todos: demoTodos,
    todoTotal: 16,
    tenants: demoTenants.map((item) => ({
      ...item,
      shortName: item.companyName.slice(0, 1),
    })),
    allTenants: demoTenants.map((item) => ({
      ...item,
      shortName: item.companyName.slice(0, 1),
    })),
    tenantKeyword: "",
    tenantCount: 128,
    topIndustry: "软件研发",
    normalTenantCount: 126,
    warningTenantCount: 2,
  },
  onLoad() {
    if (
      !requireLogin("/pages/overview/index") ||
      !hasCapability("admin.overview.view")
    )
      return;
    this.loadOverview();
  },
  async onPullDownRefresh() {
    await this.loadOverview();
    wx.stopPullDownRefresh();
  },
  async loadOverview() {
    this.setData({ loading: true });
    try {
      const [overview, tenantRows, workOrders] = await Promise.all([
        adminApi.overview(),
        adminApi.tenants(),
        adminApi.workOrders(),
      ]);
      const room = overview?.roomSummary || {};
      const rent = overview?.rentMetrics || {};
      const progress = {
        totalArea: formatArea(room.totalArea || rent.totalArea),
        rentedArea: formatArea(room.rentedArea || rent.rentedArea),
        rentRate: Number(rent.rentRate || 0),
        monthReceived: formatArea(rent.billableArea),
        monthReceivable: formatArea(rent.totalArea),
        collectionRate: Number(rent.billingRate || 0),
      };
      const tenants = normalizeTenants(Array.isArray(tenantRows) ? tenantRows : []);
      const pending = (kind: string) => (workOrders || []).filter(item => item.kind === kind && !['2', '3', '4', 'COMPLETED', 'REJECTED', 'CANCELLED'].includes(String(item.status))).length;
      const pendingTotal = (workOrders || []).filter(item => !['2', '3', '4', 'COMPLETED', 'REJECTED', 'CANCELLED'].includes(String(item.status))).length;
      const metrics = [
        { ...demoMetrics[0], value: String(progress.rentRate), trend: `${room.occupiedRooms || 0}/${room.totalRooms || 0} 间` },
        { ...demoMetrics[1], value: String(tenants.length), trend: topIndustryOf(tenants) },
        { ...demoMetrics[2], label: '可租房源', value: String(room.vacantRooms || 0), unit: '间', trend: formatArea(room.vacantArea) },
        { ...demoMetrics[3], value: String(pendingTotal), trend: `${overview?.unreadNotifications || 0} 条未读` },
      ];
      const todos = [
        { label: '物业申请', count: pending('property'), tone: 'blue', icon: '⌂', desc: '待处理物业工单' },
        { label: '增值服务', count: pending('value'), tone: 'orange', icon: '✦', desc: '待跟进服务申请' },
        { label: '招商待办', count: pending('appointment') + pending('settlement'), tone: 'green', icon: '◇', desc: '预约与入驻商机' },
        { label: '通知提醒', count: Number(overview?.unreadNotifications || 0), tone: 'red', icon: '!', desc: '未读服务通知' },
      ];
      const rawTrend = Array.isArray(overview?.rentalTrend) ? overview.rentalTrend : [];
      const trend = rawTrend.map(item => ({ month: String(item.month || '').slice(5) + '月', value: String(item.rentRate || 0), height: Math.max(8, Math.min(100, Number(item.rentRate || 0))) }));
      const tenantCount = tenants.length;
      this.setData({
        metrics,
        progress,
        trend,
        todos,
        todoTotal: todos.reduce(
          (total, item) => total + (Number(item.count) || 0),
          0
        ),
        tenants,
        allTenants: tenants,
        tenantCount,
        topIndustry: topIndustryOf(tenants),
        normalTenantCount:
          tenants.filter((item) => item.rentStatus === "正常").length,
        warningTenantCount:
          tenants.filter((item) => item.rentStatus !== "正常").length,
        usingDemo: false,
        lastUpdated: nowText(),
      });
    } catch (error) {
      this.setData({ usingDemo: true, lastUpdated: nowText() });
    } finally {
      this.setData({ loading: false });
    }
  },
  applyTenantSearch() {
    const keyword = String(this.data.tenantKeyword || "").trim().toLowerCase();
    if (!keyword) {
      this.setData({ tenants: this.data.allTenants });
      return;
    }
    const tenants = this.data.allTenants.filter((item) =>
      [item.companyName, item.industry, item.room, item.area]
        .some((value) => String(value || "").toLowerCase().includes(keyword))
    );
    this.setData({ tenants });
  },
  handleTenantSearchInput(event: WechatMiniprogram.Input) {
    this.setData(
      { tenantKeyword: event.detail.value || "" },
      () => this.applyTenantSearch()
    );
  },
  clearTenantSearch() {
    this.setData({ tenantKeyword: "" }, () => this.applyTenantSearch());
  },
  goBack() {
    const returnHome = () => wx.reLaunch({ url: "/pages/index/index" });
    if (getCurrentPages().length <= 1) returnHome();
    else wx.navigateBack({ delta: 1, fail: returnHome });
  },
  openTenant(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({
      url: `/pages/tenant-detail/index?id=${event.currentTarget.dataset.id}`,
    });
  },
  openTenants() {
    wx.showToast({ title: "租客信息已展示在下方", icon: "none" });
  },
  openTodo(event: WechatMiniprogram.TouchEvent) {
    const label = event.currentTarget.dataset.label;
    if (label === "物业申请")
      return void wx.navigateTo({
        url: "/pages/admin-work-orders/index?type=property",
      });
    if (label === "增值服务")
      return void wx.navigateTo({
        url: "/pages/admin-work-orders/index?type=value",
      });
    if (label === '招商待办') return void wx.navigateTo({ url: '/pages/admin-work-orders/index?type=appointment' });
    wx.navigateTo({ url: "/pages/notifications/index" });
  },
});
