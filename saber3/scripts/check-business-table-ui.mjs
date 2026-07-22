import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const viewRoot = path.resolve('src/views');
const businessDirectories = ['business', 'settlement', 'contract', 'finance', 'enterprise'];
const expectedPageSizes = '10,20,30,40,50,100';
const issues = [];

const collectVueFiles = directory => {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const filePath = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...collectVueFiles(filePath));
    else if (entry.name.endsWith('.vue')) files.push(filePath);
  }
  return files;
};

const lineNumber = (source, index) => source.slice(0, index).split('\n').length;

for (const directory of businessDirectories) {
  const directoryPath = path.join(viewRoot, directory);
  for (const filePath of collectVueFiles(directoryPath)) {
    const source = fs.readFileSync(filePath, 'utf8');
    const relativePath = path.relative(process.cwd(), filePath);

    for (const match of source.matchAll(/<el-table(?=\s|>)[\s\S]*?>/g)) {
      if (/\bstripe(?:\s|>|=)/.test(match[0])) {
        issues.push(`${relativePath}:${lineNumber(source, match.index)} 主表格不允许使用 stripe`);
      }
    }

    for (const match of source.matchAll(/<el-table-column\b[\s\S]*?>/g)) {
      const tag = match[0];
      const line = lineNumber(source, match.index);
      if (/type="selection"/.test(tag) && !/width="44"/.test(tag)) {
        issues.push(`${relativePath}:${line} 多选列宽度必须为 44px`);
      }
      if (/label="操作"/.test(tag) && !/align="center"/.test(tag)) {
        issues.push(`${relativePath}:${line} 操作列必须居中`);
      }
    }

    for (const match of source.matchAll(/:page-sizes="\[([^\]]+)\]"/g)) {
      const pageSizes = match[1].replace(/\s/g, '');
      if (pageSizes !== expectedPageSizes) {
        issues.push(`${relativePath}:${lineNumber(source, match.index)} 分页容量必须为 [10, 20, 30, 40, 50, 100]`);
      }
    }
  }
}

if (issues.length) {
  console.error(`业务表格 UI 检查失败，共 ${issues.length} 项：`);
  issues.forEach(issue => console.error(`- ${issue}`));
  process.exit(1);
}

console.log('业务表格 UI 检查通过');
