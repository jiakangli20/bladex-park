import { statusDic } from './contract';

export const paymentStatusDic = [
  { label: '未缴', value: '0' },
  { label: '已缴', value: '1' },
  { label: '逾期', value: '2' },
  { label: '部分缴纳', value: '3' },
];

export const changeTypeDic = [
  { label: '租金变更', value: 'rent' },
  { label: '租期变更', value: 'endDate' },
  { label: '其他变更', value: 'other' },
];

export const changeApprovalStatusDic = [
  { label: '草稿', value: '0' },
  { label: '待审批', value: '1' },
  { label: '已通过', value: '2' },
  { label: '已驳回', value: '3' },
];

export const terminationApprovalStatusDic = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已批准', value: 2 },
  { label: '已驳回', value: 3 },
];

export const terminationStatusDic = [
  { label: '待结算', value: 0 },
  { label: '已结算', value: 1 },
  { label: '已取消', value: 2 },
];

export const tableOption = {
  height: 'auto',
  calcHeight: 130,
  tip: false,
  searchShow: true,
  searchMenuSpan: 6,
  border: true,
  index: true,
  addBtn: false,
  editBtn: false,
  delBtn: false,
  viewBtn: false,
  menuWidth: 260,
  column: [
    {
      label: '合同编号',
      prop: 'contractNo',
      search: true,
      width: 160,
    },
    {
      label: '合同名称',
      prop: 'contractName',
      search: true,
      slot: true,
      minWidth: 180,
    },
    {
      label: '客户名称',
      prop: 'customerName',
      search: true,
      minWidth: 150,
    },
    {
      label: '园区ID',
      prop: 'parkId',
      type: 'number',
      controls: false,
      search: true,
      hide: true,
    },
    {
      label: '园区',
      prop: 'parkName',
      minWidth: 120,
      showOverflowTooltip: true,
    },
    {
      label: '房源',
      prop: 'roomName',
      minWidth: 140,
      showOverflowTooltip: true,
    },
    {
      label: '合同状态',
      prop: 'contractStatus',
      type: 'select',
      search: true,
      slot: true,
      dicData: statusDic,
      width: 100,
    },
    {
      label: '开始日期',
      prop: 'startDate',
      type: 'date',
      format: 'YYYY-MM-DD',
      valueFormat: 'YYYY-MM-DD',
      width: 120,
    },
    {
      label: '结束日期',
      prop: 'endDate',
      type: 'date',
      format: 'YYYY-MM-DD',
      valueFormat: 'YYYY-MM-DD',
      width: 120,
    },
    {
      label: '月租金',
      prop: 'monthlyRent',
      align: 'right',
      width: 120,
    },
  ],
};
