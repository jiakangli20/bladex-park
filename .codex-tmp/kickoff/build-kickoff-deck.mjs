import fs from "node:fs/promises";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const OUT_DIR = "/Users/jiakangli/Desktop/bladex-park/.codex-tmp/kickoff/rendered";
const FINAL_PPTX = "/Users/jiakangli/Desktop/bladex-park/园区运营管理平台项目开工会.pptx";
const ARCH_IMAGE = "/Users/jiakangli/Desktop/bladex-park/模块迁移/部署/园区管理平台技术架构图.png";
const ROADMAP = "/Users/jiakangli/Desktop/bladex-park/模块迁移/部署/模块审计88分上线路线图.md";

const W = 1280;
const H = 720;
const C = {
  canvas: "#FFFFFF",
  ink: "#111418",
  muted: "#5D6670",
  faint: "#8A929A",
  panel: "#F2F3F3",
  panelGreen: "#EAF2EE",
  rule: "#B9BFC3",
  accent: "#2F6B5B",
  accentDark: "#264A40",
  accentLight: "#BBD5CA",
  warn: "#A56B17",
  warnBg: "#F5EAD8",
  white: "#FFFFFF",
};

const FONT = "PingFang SC";

async function writeBlob(path, blob) {
  await fs.writeFile(path, new Uint8Array(await blob.arrayBuffer()));
}

async function readImageBlob(path) {
  const bytes = await fs.readFile(path);
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

function addText(slide, text, position, options = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    name: options.name || "text",
    position,
    fill: options.fill || "none",
    line: { style: "solid", fill: options.lineFill || "none", width: options.lineWidth || 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: options.fontSize || 24,
    bold: options.bold || false,
    color: options.color || C.ink,
    alignment: options.alignment || "left",
    verticalAlignment: options.verticalAlignment || "top",
    autoFit: options.autoFit || "shrinkText",
    typeface: FONT,
  };
  return shape;
}

function addRect(slide, position, options = {}) {
  return slide.shapes.add({
    geometry: options.geometry || "rect",
    name: options.name || "panel",
    position,
    fill: options.fill || C.panel,
    line: {
      style: "solid",
      fill: options.lineFill || "none",
      width: options.lineWidth || 0,
    },
  });
}

function addLine(slide, x, y, width, color = C.rule, weight = 1) {
  return slide.shapes.add({
    geometry: "straightConnector1",
    name: "rule",
    position: { left: x, top: y, width, height: 0.01 },
    fill: "none",
    line: { style: "solid", fill: color, width: weight },
  });
}

function addTitle(slide, title, page, section = "项目开工会") {
  addText(slide, title, { left: 42, top: 34, width: 1120, height: 78 }, {
    name: "slide-title",
    fontSize: 48,
    bold: true,
    color: C.ink,
    autoFit: "none",
  });
  addText(slide, section, { left: 1010, top: 43, width: 228, height: 26 }, {
    name: "section-label",
    fontSize: 16,
    color: C.accent,
    alignment: "right",
    bold: true,
  });
  addText(slide, String(page).padStart(2, "0"), { left: 1184, top: 666, width: 54, height: 20 }, {
    name: "page-number",
    fontSize: 14,
    color: C.faint,
    alignment: "right",
  });
}

function addNotes(slide, sources, presenter = "") {
  const lines = [];
  if (presenter) lines.push(presenter, "");
  lines.push("[Sources]");
  for (const source of sources) lines.push(`- ${source}`);
  lines.push("[/Sources]");
  slide.speakerNotes.textFrame.setText(lines.join("\n"));
}

function addCard(slide, x, y, w, h, number, title, body, options = {}) {
  if (options.fill) addRect(slide, { left: x, top: y, width: w, height: h }, { fill: options.fill });
  addText(slide, number, { left: x, top: y, width: 58, height: 40 }, {
    fontSize: 20,
    bold: true,
    color: options.accent || C.accent,
  });
  addText(slide, title, { left: x, top: y + 42, width: w - 10, height: 42 }, {
    fontSize: 28,
    bold: true,
    color: C.ink,
  });
  addText(slide, body, { left: x, top: y + 94, width: w - 10, height: h - 100 }, {
    fontSize: options.bodySize || 22,
    color: C.muted,
  });
}

const presentation = Presentation.create({ slideSize: { width: W, height: H } });

// 1. Cover — Codex Grid sparse cover reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addRect(slide, { left: 0, top: 0, width: 16, height: H }, { fill: C.accent });
  addText(slide, "项目启动与实施计划", { left: 58, top: 54, width: 420, height: 40 }, {
    fontSize: 22,
    bold: true,
    color: C.accent,
  });
  addText(slide, "园区运营管理平台\n项目开工会", { left: 58, top: 188, width: 760, height: 220 }, {
    fontSize: 72,
    bold: true,
    color: C.ink,
    autoFit: "none",
  });
  addText(slide, "统一建设边界 · 锁定3+1月计划 · 建立协同机制", { left: 58, top: 470, width: 800, height: 48 }, {
    fontSize: 30,
    color: C.muted,
  });
  addText(slide, "2026年8月｜项目实施团队", { left: 58, top: 604, width: 420, height: 30 }, {
    fontSize: 20,
    color: C.faint,
  });
  addRect(slide, { left: 1010, top: 90, width: 180, height: 510 }, { fill: C.panelGreen });
  addText(slide, "3+1", { left: 930, top: 228, width: 290, height: 120 }, {
    fontSize: 92,
    bold: true,
    color: C.accentDark,
    alignment: "center",
    verticalAlignment: "middle",
  });
  addText(slide, "3个月建设上线\n1个月试运行", { left: 968, top: 362, width: 220, height: 96 }, {
    fontSize: 24,
    bold: true,
    color: C.accentDark,
    alignment: "center",
  });
  addNotes(slide, ["用户本次要求：3个月建设上线，1个月试运行", ROADMAP]);
}

// 2. Agenda — Codex Grid agenda/table reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "本次开工会要达成四项共识", 2);
  const items = [
    ["01", "项目目标与建设边界"],
    ["02", "建设内容与技术架构"],
    ["03", "已完成基础与剩余工作"],
    ["04", "3个月建设、1个月试运行计划"],
    ["05", "测试验收与项目协同机制"],
    ["06", "需要双方现场确认的事项"],
  ];
  const top = 170;
  for (let i = 0; i < items.length; i++) {
    const y = top + i * 76;
    addLine(slide, 42, y + 64, 1196, C.rule, 1);
    addText(slide, items[i][0], { left: 52, top: y + 12, width: 70, height: 36 }, {
      fontSize: 22,
      bold: true,
      color: C.accent,
    });
    addText(slide, items[i][1], { left: 146, top: y + 8, width: 980, height: 44 }, {
      fontSize: 28,
      bold: i === 5,
      color: i === 5 ? C.accentDark : C.ink,
    });
  }
  addNotes(slide, ["用户本次开工会要求", ROADMAP]);
}

// 3. Goal — Codex Grid four-point layout reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "项目目标：3个月完成建设，第4个月试运行", 3);
  addCard(slide, 42, 184, 540, 170, "01", "业务全链路", "贯通客户、入驻、房源、合同、逾期及园区服务，形成线上闭环。", { bodySize: 22 });
  addCard(slide, 660, 184, 540, 170, "02", "PC与小程序协同", "管理端统一办理，企业端便捷申请与查询，业务数据同步一致。", { bodySize: 22 });
  addCard(slide, 42, 392, 540, 170, "03", "安全稳定运行", "建立认证、权限、审计、备份与异常恢复机制，满足上线要求。", { bodySize: 22 });
  addCard(slide, 660, 392, 540, 170, "04", "持续运营支撑", "采用模块化架构，兼顾后续业务扩展、运行维护与数据分析需要。", { bodySize: 22 });
  addRect(slide, { left: 42, top: 608, width: 1196, height: 48 }, { fill: C.panelGreen });
  addText(slide, "最终交付标准：可上线、可使用、可运维、可验收", { left: 62, top: 616, width: 900, height: 30 }, {
    fontSize: 24,
    bold: true,
    color: C.accentDark,
  });
  addNotes(slide, ["用户本次要求：3个月建设上线，1个月试运行", ROADMAP]);
}

// 4. Scope — Codex Grid eight-topic layout reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "建设范围覆盖七大业务域与公共支撑能力", 4);
  const topics = [
    ["01", "首页工作台", "指标、待办、提醒与业务入口"],
    ["02", "园区与房源", "园区、楼宇、楼层、房源及租控"],
    ["03", "入驻与客户", "商机、背景调查、审批、客户档案"],
    ["04", "合同管理", "创建、审批、到期、归档与退租"],
    ["05", "逾期管理", "逾期识别、提醒、处置与资料留痕"],
    ["06", "物业与企业服务", "服务配置、工单、商户与广告"],
    ["07", "流程与文档", "审批、模板打印、预览下载与通知"],
    ["08", "系统管理与运维", "用户、角色、权限、日志与备份"],
  ];
  const xs = [42, 350, 658, 966];
  for (let i = 0; i < topics.length; i++) {
    const row = i < 4 ? 0 : 1;
    const col = i % 4;
    const y = 196 + row * 220;
    addText(slide, topics[i][0], { left: xs[col], top: y, width: 50, height: 30 }, {
      fontSize: 18,
      bold: true,
      color: C.accent,
    });
    addText(slide, topics[i][1], { left: xs[col], top: y + 42, width: 255, height: 42 }, {
      fontSize: 27,
      bold: true,
    });
    addText(slide, topics[i][2], { left: xs[col], top: y + 96, width: 255, height: 80 }, {
      fontSize: 21,
      color: C.muted,
    });
    addLine(slide, xs[col], y + 184, 252, C.rule, 1);
  }
  addNotes(slide, [ROADMAP, "/Users/jiakangli/Desktop/bladex-park/模块迁移/部署/园区管理平台技术架构图.png"]);
}

// 5. Architecture — user-supplied architecture image shown at readable scale.
{
  const slide = presentation.slides.add();
  slide.background.fill = "#F5F1E8";
  const imageBytes = await readImageBlob(ARCH_IMAGE);
  slide.images.add({
    blob: imageBytes,
    contentType: "image/png",
    alt: "园区运营管理平台技术架构图",
    fit: "contain",
    position: { left: 66, top: 12, width: 1148, height: 696 },
  });
  addNotes(slide, [ARCH_IMAGE], "架构由用户与终端、前端展示、接入安全、后端业务与公共能力、数据基础设施五层构成。");
}

// 6. Current status — Codex Grid three-stat layout reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "代码整改已完成，正式验收仍需真实UAT", 6);
  addText(slide, "当前成果可作为项目建设基础直接复用，但“代码完成”不等于“验收通过”。", { left: 42, top: 126, width: 1050, height: 54 }, {
    fontSize: 25,
    color: C.muted,
  });
  const stats = [
    ["7/8", "当前纳入范围的代码审计进度"],
    ["88–91", "主要已审计模块代码复评分"],
    ["0", "已审计批次P0/P1代码问题"],
  ];
  const xs = [42, 453, 864];
  for (let i = 0; i < 3; i++) {
    addRect(slide, { left: xs[i], top: 250, width: 375, height: 286 }, { fill: i === 2 ? C.panelGreen : C.panel });
    addText(slide, stats[i][0], { left: xs[i] + 30, top: 292, width: 310, height: 110 }, {
      fontSize: 72,
      bold: true,
      color: i === 2 ? C.accentDark : C.ink,
      verticalAlignment: "bottom",
    });
    addText(slide, stats[i][1], { left: xs[i] + 30, top: 430, width: 310, height: 76 }, {
      fontSize: 23,
      color: C.muted,
    });
  }
  addRect(slide, { left: 42, top: 570, width: 1196, height: 64 }, { fill: C.warnBg });
  addText(slide, "上线门槛仍包括：真实多角色UAT、生产安全配置、性能验证、备份恢复与部署回滚。", { left: 64, top: 586, width: 1110, height: 32 }, {
    fontSize: 23,
    bold: true,
    color: C.warn,
  });
  addNotes(slide, [ROADMAP], "数字均为路线图截至2026年8月3日记录的代码审计口径，不代表正式验收结论。");
}

// 7. Done vs remaining — Codex Grid comparison layout reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "已有成果可复用，后续重点转向联调与上线", 7);
  addText(slide, "已完成基础", { left: 42, top: 154, width: 520, height: 44 }, {
    fontSize: 30,
    bold: true,
    color: C.accentDark,
  });
  addText(slide, "开工后必须完成", { left: 660, top: 154, width: 520, height: 44 }, {
    fontSize: 30,
    bold: true,
    color: C.warn,
  });
  addRect(slide, { left: 42, top: 216, width: 540, height: 350 }, { fill: C.panelGreen });
  addRect(slide, { left: 660, top: 216, width: 540, height: 350 }, { fill: C.panel });
  addText(slide,
    "01  核心业务模块完成代码整改\n02  首页、菜单及表格视觉统一\n03  审批、模板、通知链路完成整改\n04  编译与规范检查通过\n05  权限、状态及数据隔离规则已加固",
    { left: 72, top: 248, width: 480, height: 292 },
    { fontSize: 22, color: C.ink });
  addText(slide,
    "01  多角色真实业务UAT\n02  PC端与微信小程序真实联调\n03  历史数据、流程与文档复验\n04  生产安全、性能与稳定性验证\n05  回滚、培训、试运行与正式验收",
    { left: 690, top: 248, width: 480, height: 292 },
    { fontSize: 22, color: C.ink });
  addRect(slide, { left: 42, top: 598, width: 1158, height: 42 }, { fill: C.accentDark });
  addText(slide, "实施策略：保留现有成果，按需求基线复验，不重复建设已验证的基础能力。", { left: 62, top: 605, width: 1100, height: 28 }, {
    fontSize: 22,
    bold: true,
    color: C.white,
  });
  addNotes(slide, [ROADMAP]);
}

// 8. Gantt — Codex Grid Gantt reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "3个月建设上线，第4个月试运行验收", 8);
  const x0 = 42;
  const labelW = 270;
  const monthW = 225;
  const gridTop = 156;
  const rowH = 66;
  const headers = ["工作主线", "第1月", "第2月", "第3月", "第4月"];
  const widths = [labelW, monthW, monthW, monthW, monthW];
  let cursor = x0;
  for (let i = 0; i < headers.length; i++) {
    addRect(slide, { left: cursor, top: gridTop, width: widths[i], height: 48 }, { fill: i === 0 ? C.accentDark : C.panelGreen, lineFill: C.white, lineWidth: 1 });
    addText(slide, headers[i], { left: cursor + 8, top: gridTop + 9, width: widths[i] - 16, height: 28 }, {
      fontSize: 21,
      bold: true,
      color: i === 0 ? C.white : C.accentDark,
      alignment: "center",
    });
    cursor += widths[i];
  }
  const rows = [
    ["需求基线与环境准备", 1, 1],
    ["业务整改与多角色UAT", 1, 2],
    ["PC端与小程序联调", 1, 2],
    ["安全、性能与全量回归", 2, 3],
    ["部署、培训与上线", 3, 3],
    ["试运行、问题闭环与终验", 4, 4],
  ];
  for (let r = 0; r < rows.length; r++) {
    const y = gridTop + 48 + r * rowH;
    addRect(slide, { left: x0, top: y, width: labelW + monthW * 4, height: rowH }, { fill: r % 2 === 0 ? "#FAFAFA" : C.white, lineFill: C.rule, lineWidth: 0.5 });
    addText(slide, rows[r][0], { left: x0 + 14, top: y + 17, width: labelW - 28, height: 30 }, {
      fontSize: 20,
      bold: true,
    });
    const start = rows[r][1];
    const end = rows[r][2];
    const barX = x0 + labelW + (start - 1) * monthW + 15;
    const barW = (end - start + 1) * monthW - 30;
    addRect(slide, { left: barX, top: y + 15, width: barW, height: 36 }, { fill: r === 5 ? C.warn : C.accent, geometry: "roundRect" });
  }
  addText(slide, "M1 范围确认", { left: 330, top: 625, width: 210, height: 26 }, { fontSize: 18, bold: true, color: C.accentDark });
  addText(slide, "M2 核心UAT", { left: 555, top: 625, width: 210, height: 26 }, { fontSize: 18, bold: true, color: C.accentDark });
  addText(slide, "M3 正式上线", { left: 780, top: 625, width: 210, height: 26 }, { fontSize: 18, bold: true, color: C.accentDark });
  addText(slide, "M4 正式验收", { left: 1005, top: 625, width: 210, height: 26 }, { fontSize: 18, bold: true, color: C.warn });
  addNotes(slide, ["用户本次要求：3个月建设上线，1个月试运行", ROADMAP]);
}

// 9. Monthly milestones — Codex Grid three-milestone timeline reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "三个月逐月收口，每月形成可验收成果", 9);
  addLine(slide, 74, 330, 1100, C.ink, 1);
  const xs = [74, 470, 866];
  const labels = ["第1月", "第2月", "第3月"];
  const titles = ["确认与收口", "联调与验证", "上线与移交"];
  const bodies = [
    "需求与范围基线\n账号、数据、环境准备\n重点模块真实UAT\n历史数据问题收口",
    "PC端与小程序联调\n审批、模板、通知复验\n权限、安全、性能测试\n跨模块全链路验证",
    "全量回归与上线评审\n生产部署、备份和回滚\n用户培训与资料移交\n系统正式上线运行",
  ];
  for (let i = 0; i < 3; i++) {
    addRect(slide, { left: xs[i] - 6, top: 323, width: 14, height: 14 }, { fill: C.accent, geometry: "ellipse" });
    addText(slide, labels[i], { left: xs[i], top: 278, width: 120, height: 28 }, { fontSize: 20, bold: true, color: C.accent });
    addText(slide, titles[i], { left: xs[i], top: 374, width: 300, height: 42 }, { fontSize: 29, bold: true });
    addText(slide, bodies[i], { left: xs[i], top: 430, width: 310, height: 160 }, { fontSize: 22, color: C.muted });
  }
  addRect(slide, { left: 74, top: 610, width: 1100, height: 48 }, { fill: C.warnBg });
  addText(slide, "第4月：正式业务试运行、运行观察、问题闭环、QA完善及最终验收。", { left: 94, top: 619, width: 1040, height: 30 }, {
    fontSize: 23,
    bold: true,
    color: C.warn,
  });
  addNotes(slide, ["用户本次要求：3个月建设上线，1个月试运行", ROADMAP]);
}

// 10. Test and acceptance — four-point layout reference.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "测试与验收必须形成真实、可追溯证据", 10);
  addCard(slide, 42, 178, 540, 170, "01", "业务闭环", "以真实客户、房源、合同、账单和工单验证正向及异常流程。", { bodySize: 22 });
  addCard(slide, 660, 178, 540, 170, "02", "权限与安全", "使用管理员、业务、财务及普通用户验证角色权限和数据边界。", { bodySize: 22 });
  addCard(slide, 42, 386, 540, 170, "03", "性能与稳定性", "验证并发访问、关键接口耗时、备份恢复、异常回退和运行稳定性。", { bodySize: 22 });
  addCard(slide, 660, 386, 540, 170, "04", "文档与培训", "交付设计、部署、测试、操作、培训、运维和验收等完整资料。", { bodySize: 22 });
  addRect(slide, { left: 42, top: 596, width: 1196, height: 56 }, { fill: C.accentDark });
  addText(slide, "上线门禁：核心功能通过｜P0/P1为0｜UAT证据完整｜安全与回滚验证完成", { left: 62, top: 609, width: 1140, height: 32 }, {
    fontSize: 22,
    bold: true,
    color: C.white,
    alignment: "center",
  });
  addNotes(slide, [ROADMAP]);
}

// 11. Collaboration — four-column flat layout.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "四方协同，确保需求、进度与上线责任清晰", 11);
  const roles = [
    ["01", "招标人项目负责人", "统筹决策、资源协调、重大事项确认与里程碑审批"],
    ["02", "业务部门与关键用户", "确认业务口径、准备测试数据、参与UAT和培训验收"],
    ["03", "项目实施团队", "负责设计开发、联调测试、部署培训和问题闭环"],
    ["04", "运维与安全团队", "准备环境、域名证书、安全评测、备份和上线保障"],
  ];
  const xs = [42, 350, 658, 966];
  for (let i = 0; i < roles.length; i++) {
    addText(slide, roles[i][0], { left: xs[i], top: 206, width: 52, height: 30 }, { fontSize: 18, bold: true, color: C.accent });
    addText(slide, roles[i][1], { left: xs[i], top: 256, width: 250, height: 74 }, { fontSize: 27, bold: true });
    addText(slide, roles[i][2], { left: xs[i], top: 350, width: 250, height: 150 }, { fontSize: 22, color: C.muted });
    addLine(slide, xs[i], 530, 250, C.rule, 1);
  }
  addRect(slide, { left: 42, top: 574, width: 1174, height: 64 }, { fill: C.panelGreen });
  addText(slide, "协同节奏：固定周例会｜里程碑联合评审｜问题清单闭环｜重大事项即时升级", { left: 62, top: 591, width: 1120, height: 34 }, {
    fontSize: 24,
    bold: true,
    color: C.accentDark,
    alignment: "center",
  });
  addNotes(slide, [ROADMAP, "用户提供的项目实施要求"]);
}

// 12. Risks — four rows.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "关键风险需在项目初期同步化解", 12);
  const risks = [
    ["建设范围持续扩张", "开工会确认需求基线，新增需求履行评估与变更确认。"],
    ["测试账号和业务数据不足", "提前准备多角色账号及新旧业务样本，按模块安排UAT。"],
    ["生产环境与外部渠道延迟", "首月确认服务器、域名、证书、存储及小程序和通知条件。"],
    ["历史数据及流程配置差异", "先备份再清理，以真实流程节点和历史样本开展回归。"],
  ];
  for (let i = 0; i < risks.length; i++) {
    const y = 160 + i * 112;
    addText(slide, String(i + 1).padStart(2, "0"), { left: 50, top: y + 14, width: 58, height: 32 }, { fontSize: 18, bold: true, color: C.warn });
    addText(slide, risks[i][0], { left: 130, top: y + 8, width: 330, height: 42 }, { fontSize: 27, bold: true });
    addText(slide, risks[i][1], { left: 500, top: y + 8, width: 680, height: 56 }, { fontSize: 22, color: C.muted });
    addLine(slide, 42, y + 82, 1196, C.rule, 1);
  }
  addRect(slide, { left: 42, top: 620, width: 1196, height: 38 }, { fill: C.warnBg });
  addText(slide, "原则：风险提前暴露、责任落实到人、处理过程留痕、关闭结果可验证。", { left: 64, top: 626, width: 1100, height: 26 }, {
    fontSize: 21,
    bold: true,
    color: C.warn,
    alignment: "center",
  });
  addNotes(slide, [ROADMAP]);
}

// 13. Communication items — business and scope.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "需确认：业务边界与应用口径", 13, "会议确认事项");
  const items = [
    ["01", "本期建设边界", "确认必建模块及存量扩展功能是否纳入本期交付与验收。"],
    ["02", "背景调查来源", "明确采用人工核验、采购方数据还是第三方服务及费用边界。"],
    ["03", "微信小程序范围", "确认使用主体、企业绑定方式、功能清单及上线发布条件。"],
    ["04", "通知发送渠道", "明确站内信、短信、邮件、小程序消息的正式接入范围。"],
    ["05", "审批流程与角色", "确认各业务流程节点、审批人、抄送人和异常处理规则。"],
    ["06", "历史数据范围", "明确需要迁移、清理、保留和归档的数据范围及责任分工。"],
  ];
  for (let i = 0; i < items.length; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = col === 0 ? 42 : 660;
    const y = 160 + row * 158;
    addText(slide, items[i][0], { left: x, top: y, width: 48, height: 28 }, { fontSize: 18, bold: true, color: C.accent });
    addText(slide, items[i][1], { left: x + 66, top: y - 3, width: 250, height: 36 }, { fontSize: 26, bold: true });
    addText(slide, items[i][2], { left: x + 66, top: y + 48, width: 500, height: 70 }, { fontSize: 21, color: C.muted });
    addLine(slide, x, y + 124, 540, C.rule, 1);
  }
  addNotes(slide, [ROADMAP, "用户本次开工会要求"]);
}

// 14. Communication items — resources and acceptance; deliberate close.
{
  const slide = presentation.slides.add();
  slide.background.fill = C.canvas;
  addTitle(slide, "需确认：资源条件与验收安排", 14, "会议确认事项");
  const items = [
    ["01", "项目组织", "确认双方负责人、业务骨干、技术人员及驻场协同安排。"],
    ["02", "生产环境", "确认服务器、网络、域名证书、数据库、缓存和文件存储条件。"],
    ["03", "UAT资源", "确认测试账号、业务样本、测试时间窗口及问题反馈责任人。"],
    ["04", "安全评测", "确认评测组织方式、时间安排、整改责任和复测要求。"],
    ["05", "培训与试运行", "确认培训对象、上线窗口、1个月试运行和终验计划。"],
    ["06", "决策与变更", "确认重大事项升级路径、需求变更流程和书面确认机制。"],
  ];
  for (let i = 0; i < items.length; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = col === 0 ? 42 : 660;
    const y = 150 + row * 140;
    addText(slide, items[i][0], { left: x, top: y, width: 48, height: 28 }, { fontSize: 18, bold: true, color: C.accent });
    addText(slide, items[i][1], { left: x + 66, top: y - 3, width: 250, height: 36 }, { fontSize: 26, bold: true });
    addText(slide, items[i][2], { left: x + 66, top: y + 46, width: 500, height: 58 }, { fontSize: 21, color: C.muted });
    addLine(slide, x, y + 110, 540, C.rule, 1);
  }
  addRect(slide, { left: 42, top: 590, width: 1196, height: 72 }, { fill: C.accentDark });
  addText(slide, "会后形成：会议纪要｜需求边界清单｜里程碑计划｜责任人与完成时限", { left: 64, top: 610, width: 1150, height: 34 }, {
    fontSize: 25,
    bold: true,
    color: C.white,
    alignment: "center",
  });
  addNotes(slide, [ROADMAP, "用户本次开工会要求"], "以明确责任和下一步行动结束会议，不以泛化的致谢页收尾。");
}

await fs.mkdir(OUT_DIR, { recursive: true });

for (const [index, slide] of presentation.slides.items.entries()) {
  const stem = `slide-${String(index + 1).padStart(2, "0")}`;
  await writeBlob(`${OUT_DIR}/${stem}.png`, await presentation.export({ slide, format: "png", scale: 1 }));
  const layout = await slide.export({ format: "layout" });
  await fs.writeFile(`${OUT_DIR}/${stem}.layout.json`, await layout.text());
}

await writeBlob(`${OUT_DIR}/deck-montage.webp`, await presentation.export({ format: "webp", montage: true, scale: 1 }));
const pptx = await PresentationFile.exportPptx(presentation);
await pptx.save(FINAL_PPTX);

console.log(`Created ${FINAL_PPTX}`);
