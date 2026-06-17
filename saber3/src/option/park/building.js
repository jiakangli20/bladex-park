const statusDic = [
  {
    label: '启用',
    value: '0',
  },
  {
    label: '停用',
    value: '1',
  },
];

const buildingTypeDic = [
  {
    label: '自持',
    value: '自持',
  },
  {
    label: '租赁',
    value: '租赁',
  },
  {
    label: '合作',
    value: '合作',
  },
  {
    label: '厂房',
    value: '厂房',
  },
];

export const tableOption = {
  height: 'auto',
  calcHeight: 32,
  tip: false,
  searchShow: true,
  searchMenuSpan: 6,
  border: true,
  index: true,
  selection: true,
  align: 'center',
  headerAlign: 'center',
  menuAlign: 'center',
  menuHeaderAlign: 'center',
  viewBtn: true,
  dialogType: 'drawer',
  dialogWidth: '1040px',
  dialogClickModal: false,
  labelWidth: 120,
  column: [
    {
      label: '建筑名称',
      prop: 'name',
      minWidth: 150,
      search: true,
      rules: [
        {
          required: true,
          message: '请输入建筑名称',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '所属园区',
      prop: 'parkId',
      type: 'select',
      dicData: [],
      props: {
        label: 'name',
        value: 'id',
      },
      minWidth: 140,
      search: true,
      rules: [
        {
          required: true,
          message: '请选择所属园区',
          trigger: 'change',
        },
      ],
    },
    {
      label: '所属园区',
      prop: 'parkName',
      minWidth: 140,
      display: false,
    },
    {
      label: '建筑编号',
      prop: 'code',
      minWidth: 130,
      search: true,
      rules: [
        {
          required: true,
          message: '请输入建筑编码',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '建筑标签',
      prop: 'buildingTag',
      minWidth: 150,
      display: false,
    },
    {
      label: '建筑面积(㎡)',
      prop: 'area',
      type: 'number',
      precision: 2,
      min: 0,
      width: 130,
      rules: [
        {
          required: true,
          message: '请输入建筑面积',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '可租面积(㎡)',
      prop: 'rentableArea',
      type: 'number',
      precision: 2,
      min: 0,
      width: 130,
    },
    {
      label: '管理面积(㎡)',
      prop: 'managementArea',
      width: 130,
      display: false,
    },
    {
      label: '房间总数',
      prop: 'roomCount',
      width: 105,
      display: false,
    },
    {
      label: '出租率(%)',
      prop: 'rentRate',
      width: 110,
      display: false,
    },
    {
      label: '在租面积(㎡)',
      prop: 'rentedArea',
      width: 130,
      display: false,
    },
    {
      label: '在租合同数',
      prop: 'activeContractCount',
      width: 120,
      display: false,
    },
    {
      label: '所属地区',
      prop: 'region',
      minWidth: 150,
      hide: true,
      rules: [
        {
          required: true,
          message: '请输入所属地区',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '建筑地址',
      prop: 'address',
      minWidth: 180,
      hide: true,
    },
    {
      label: '产权性质',
      prop: 'buildingType',
      type: 'select',
      dicData: buildingTypeDic,
      value: '自持',
      hide: true,
      rules: [
        {
          required: true,
          message: '请选择产权性质',
          trigger: 'change',
        },
      ],
    },
    {
      label: '不动产编号',
      prop: 'realEstateNo',
      hide: true,
    },
    {
      label: '产权编号',
      prop: 'propertyNo',
      hide: true,
    },
    {
      label: '土地编号',
      prop: 'landNo',
      hide: true,
    },
    {
      label: '排序值',
      prop: 'sortNum',
      type: 'number',
      precision: 0,
      min: 1,
      value: 1,
      hide: true,
      rules: [
        {
          required: true,
          message: '请输入排序值',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '楼层数',
      prop: 'floors',
      type: 'number',
      precision: 0,
      min: 1,
      value: 1,
      hide: true,
      rules: [
        {
          required: true,
          message: '请输入楼层数',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '产权面积(㎡)',
      prop: 'propertyArea',
      type: 'number',
      precision: 2,
      min: 0,
      hide: true,
      rules: [
        {
          required: true,
          message: '请输入产权面积',
          trigger: 'blur',
        },
      ],
    },
    {
      label: '自用面积(㎡)',
      prop: 'selfUseArea',
      type: 'number',
      precision: 2,
      min: 0,
      hide: true,
    },
    {
      label: '配套面积(㎡)',
      prop: 'supportingArea',
      type: 'number',
      precision: 2,
      min: 0,
      hide: true,
    },
    {
      label: '车位面积(㎡)',
      prop: 'parkingArea',
      type: 'number',
      precision: 2,
      min: 0,
      hide: true,
    },
    {
      label: '标准层高(m)',
      prop: 'standardFloorHeight',
      type: 'number',
      precision: 2,
      min: 0,
      hide: true,
    },
    {
      label: '楼层面积配置',
      prop: 'floorAreas',
      formslot: true,
      span: 24,
      hide: true,
      display: true,
    },
    {
      label: '状态',
      prop: 'status',
      type: 'select',
      dicData: statusDic,
      value: '0',
      width: 90,
      search: true,
      slot: true,
      rules: [
        {
          required: true,
          message: '请选择状态',
          trigger: 'change',
        },
      ],
    },
    {
      label: '备注',
      prop: 'memo',
      type: 'textarea',
      minRows: 3,
      span: 24,
      hide: true,
    },
  ],
};
