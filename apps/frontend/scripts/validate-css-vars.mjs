import { readdir, readFile } from 'node:fs/promises';
import { basename, join, relative } from 'node:path';

const srcDir = join(process.cwd(), 'src');
const globalStylesDir = join(srcDir, 'styles');
const definitionPattern = /--([A-Za-z0-9_-]+)\s*:/g;
const usagePattern = /var\(\s*--([A-Za-z0-9_-]+)\b/g;
const disallowedColorPattern = /#[0-9a-fA-F]{3,8}\b|rgba\([^)]*\)/g;
const classSelectorPattern = /\.(-?[_a-zA-Z]+[_a-zA-Z0-9-]*)/g;
const allowedGlobalElements = new Set([
  'html',
  'body',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'p',
  'a',
  'button',
  'input',
  'select',
  'textarea',
  'label',
  'form',
  'table',
  'thead',
  'tbody',
  'tr',
  'th',
  'td',
]);

async function collectCssFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = await Promise.all(
    entries.map(async (entry) => {
      const entryPath = join(dir, entry.name);

      if (entry.isDirectory()) {
        return collectCssFiles(entryPath);
      }

      return entry.isFile() && entry.name.endsWith('.css') ? [entryPath] : [];
    }),
  );

  return files.flat();
}

function extractMatches(pattern, content) {
  return [...content.matchAll(pattern)].map((match) => match[1]);
}

function lineForIndex(content, index) {
  return content.slice(0, index).split('\n').length;
}

function isColorDefinitionFile(file) {
  const name = basename(file);

  return name === '00-primitives.css' || /^theme\..+\.css$/.test(name);
}

function stripCssComments(content) {
  return content.replace(/\/\*[\s\S]*?\*\//g, (comment) => ' '.repeat(comment.length));
}

function collectGlobalClassViolations(file, content) {
  if (!file.startsWith(globalStylesDir) || basename(file) === '00-primitives.css') {
    return [];
  }

  const violations = [];
  const css = stripCssComments(content);
  const rulePattern = /([^{}]+)\{/g;

  for (const rule of css.matchAll(rulePattern)) {
    const selectorText = rule[1].trim();

    if (selectorText.startsWith('@') || selectorText.startsWith(':root')) {
      continue;
    }

    const selectors = selectorText.split(',');

    for (const selector of selectors) {
      const normalizedSelector = selector.trim();

      if (allowedGlobalElements.has(normalizedSelector.toLowerCase())) {
        continue;
      }

      for (const classMatch of normalizedSelector.matchAll(classSelectorPattern)) {
        const className = classMatch[1];

        if (!className.startsWith('app-')) {
          violations.push({
            className,
            file,
            line: lineForIndex(content, rule.index + rule[1].indexOf(selector) + classMatch.index),
          });
        }
      }
    }
  }

  return violations;
}

const cssFiles = await collectCssFiles(srcDir);
const definitions = new Set();
const usages = [];
const colorViolations = [];
const classViolations = [];

for (const file of cssFiles) {
  const content = await readFile(file, 'utf8');

  for (const name of extractMatches(definitionPattern, content)) {
    definitions.add(name);
  }

  for (const match of content.matchAll(usagePattern)) {
    usages.push({ name: match[1], file, line: lineForIndex(content, match.index) });
  }

  if (!isColorDefinitionFile(file)) {
    for (const match of content.matchAll(disallowedColorPattern)) {
      colorViolations.push({ value: match[0], file, line: lineForIndex(content, match.index) });
    }
  }

  classViolations.push(...collectGlobalClassViolations(file, content));
}

const missingUsages = usages.filter(({ name }) => !definitions.has(name));
const failures = [];

if (missingUsages.length > 0) {
  failures.push('Undefined CSS custom properties found:');

  for (const { name, file, line } of missingUsages) {
    failures.push(`- --${name} used in ${relative(process.cwd(), file)}:${line}`);
  }
}

if (colorViolations.length > 0) {
  failures.push('Disallowed raw CSS colors found outside 00-primitives.css and theme files:');

  for (const { value, file, line } of colorViolations) {
    failures.push(`- ${value} in ${relative(process.cwd(), file)}:${line}`);
  }
}

if (classViolations.length > 0) {
  failures.push('Global style classes without the app- prefix found:');

  for (const { className, file, line } of classViolations) {
    failures.push(`- .${className} in ${relative(process.cwd(), file)}:${line}`);
  }
}

if (failures.length > 0) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log(`Validated ${cssFiles.length} CSS files: custom properties, raw colors, and global class prefixes are valid.`);
