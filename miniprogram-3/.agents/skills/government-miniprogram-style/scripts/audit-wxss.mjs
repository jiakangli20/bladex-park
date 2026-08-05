import fs from 'node:fs';
import path from 'node:path';

const args = process.argv.slice(2);
const target = args.find((arg) => !arg.startsWith('--')) || 'miniprogram';
const strict = args.includes('--strict');
const errors = [];
const warnings = [];

function report(list, entry, line, code, content) {
  list.push(`${entry}:${line + 1}: [${code}] ${content.trim()}`);
}

function visit(entry) {
  const stat = fs.statSync(entry);
  if (stat.isDirectory()) {
    fs.readdirSync(entry).forEach((name) => visit(path.join(entry, name)));
    return;
  }
  if (!entry.endsWith('.wxss') && !entry.endsWith('.wxml')) return;

  const source = fs.readFileSync(entry, 'utf8');
  const lines = source.split(/\r?\n/);
  lines.forEach((line, index) => {
    if (/(?:-webkit-)?mask(?:-image|-repeat|-position|-size)?\s*:/i.test(line) || /\bmask\s*:/i.test(line)) {
      report(errors, entry, index, 'SKYLINE_MASK', line);
    }
    if (/data:image\//i.test(line)) report(errors, entry, index, 'INLINE_DATA_IMAGE', line);
    if (/\b(?:backdrop-)?filter\s*:/i.test(line)) report(errors, entry, index, 'SKYLINE_FILTER', line);

    const isShadow = /box-shadow:/i.test(line) && !/box-shadow:\s*none/i.test(line);
    const isLargeRadius = /border-radius:\s*(?:1[6-9]|[2-9]\d)rpx/i.test(line) && !/border-radius:\s*50%/i.test(line);
    const usesNegativeMargin = /margin(?:-[a-z]+)?:\s*-[0-9]+rpx/i.test(line);
    if (/gradient\(/i.test(line) || isShadow || isLargeRadius || usesNegativeMargin) {
      report(warnings, entry, index, 'VISUAL_REVIEW', line);
    }
  });

  if (entry.endsWith('.wxml')) {
    for (const tag of source.matchAll(/<image\b[\s\S]*?>/gi)) {
      const line = source.slice(0, tag.index).split(/\r?\n/).length - 1;
      if (!/\bmode\s*=/i.test(tag[0])) report(warnings, entry, line, 'IMAGE_MODE', tag[0]);
      if (!/\bclass\s*=/i.test(tag[0])) report(warnings, entry, line, 'IMAGE_CLASS', tag[0]);
    }
  }
}

visit(target);
if (errors.length) console.error(`发现 ${errors.length} 项实现错误：\n${errors.join('\n')}`);
if (warnings.length) console.log(`发现 ${warnings.length} 项人工复核警告：\n${warnings.join('\n')}`);

const passed = !errors.length && (!strict || !warnings.length);
if (passed) console.log(strict ? 'UI 严格审计通过：没有实现错误或人工复核警告。' : 'UI 审计通过：没有实现错误。');
if (strict && warnings.length) console.error('严格模式下，人工复核警告会阻断交付。');
if (!passed) process.exitCode = 1;
