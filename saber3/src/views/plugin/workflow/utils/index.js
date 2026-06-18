/**
 * 判断版本号，v2是否小于等于v1
 */
export const versionCompare = (v1, v2) => {
  if (!v1 || !v2) return true;
  let v1Arr = v1.split('.');
  let v2Arr = v2.split('.');
  const len = Math.max(v1Arr.length, v2Arr.length);

  // 调整两个版本号位数相同
  while (v1Arr.length < len) {
    v1Arr.push('0');
  }
  while (v2Arr.length < len) {
    v2Arr.push('0');
  }

  // 循环判断每位数的大小
  for (let i = 0; i < len; i++) {
    const num1 = parseInt(v1Arr[i]);
    const num2 = parseInt(v2Arr[i]);

    if (num1 > num2) {
      return false;
    } else if (num1 < num2) {
      return true;
    }
  }
  return true;
};
