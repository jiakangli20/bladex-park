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
    const companyName = String(
      item.companyName ||
        item.enterpriseName ||
        demoTenants[index % demoTenants.length].companyName
    );
    const fallback = demoTenants[index % demoTenants.length];
    return {
      ...fallback,
      ...item,
      companyName,
      shortName: companyName.slice(0, 1),
      leaseEnd:
        item.leaseEnd ||
        item.leasePeriod?.split(" - ").pop() ||
        fallback.leaseEnd,
      rentStatus:
        item.rentStatus || (item.status === "1" ? "正常" : fallback.rentStatus),
    };
  });

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
      const [overview, tenantRows] = await Promise.all([
        adminApi.overview(),
        adminApi.tenants(),
      ]);
      const room = overview?.roomSummary || {};
      const rent = overview?.rentMetrics || {};
      const digital = Array.isArray(overview?.digitalOverview)
        ? overview.digitalOverview
        : [];
      const progress = overview?.progress || {
        ...demoProgress,
        totalArea: room.totalArea || demoProgress.totalArea,
        rentedArea: room.occupiedArea || demoProgress.rentedArea,
        rentRate: Number(rent.rentRate ?? demoProgress.rentRate),
        monthReceived: digital[0]?.value || demoProgress.monthReceived,
        monthReceivable: digital[2]?.value || demoProgress.monthReceivable,
        collectionRate: Number(
          overview?.collectionRate ?? demoProgress.collectionRate
        ),
      };
      const tenants = normalizeTenants(
        Array.isArray(tenantRows) && tenantRows.length
          ? tenantRows
          : demoTenants
      );
      const metrics =
        Array.isArray(overview?.metrics) && overview.metrics.length
          ? overview.metrics.map((item, index) => ({
              ...demoMetrics[index % demoMetrics.length],
              ...item,
              icon: item.icon || demoMetrics[index % demoMetrics.length].icon,
              trend:
                item.trend || demoMetrics[index % demoMetrics.length].trend,
            }))
          : demoMetrics.map((item) =>
              item.label === "出租率"
                ? { ...item, value: progress.rentRate }
                : item
            );
      const todos =
        Array.isArray(overview?.todos) && overview.todos.length
          ? overview.todos.map((item) => ({
              ...item,
              icon: item.icon || "•",
              desc: item.desc || "待处理事项",
            }))
          : demoTodos;
      const tenantCount = Number(
        overview?.tenantCount || tenants.length || 128
      );
      this.setData({
        metrics,
        progress,
        todos,
        todoTotal: todos.reduce(
          (total, item) => total + (Number(item.count) || 0),
          0
        ),
        tenants,
        tenantCount,
        normalTenantCount:
          tenants.filter((item) => item.rentStatus === "正常").length || 126,
        warningTenantCount:
          tenants.filter((item) => item.rentStatus !== "正常").length || 2,
        usingDemo: !(
          overview?.metrics?.length ||
          room.totalRooms ||
          tenantRows?.length
        ),
        lastUpdated: nowText(),
      });
    } catch (error) {
      this.setData({ usingDemo: true, lastUpdated: nowText() });
    } finally {
      this.setData({ loading: false });
    }
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
    wx.navigateTo({ url: "/pages/notifications/index" });
  },
});
