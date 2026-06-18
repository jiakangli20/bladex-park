import { dayjs } from 'element-plus';

/**
 * 日期格式化
 */
export function dateFormat(date, format = 'YYYY-MM-DD HH:mm:ss') {
  return dayjs(date).format(format);
}
