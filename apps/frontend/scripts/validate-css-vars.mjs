import { readdir, readFile } from 'node:fs/promises';
import { join, relative } from 'node:path';

const srcDir = join(process.cwd(), 'src');
const definitionPattern = /--([A-Za-z0-9_-]+)\s*:/g;
const usagePattern = /var\(\s*--([A-Za-z0-9_-]+)\b/g;

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

const cssFiles = await collectCssFiles(srcDir);
const definitions = new Set();
const usages = [];

for (const file of cssFiles) {
  const content = await readFile(file, 'utf8');

  for (const name of extractMatches(definitionPattern, content)) {
    definitions.add(name);
  }

  for (const match of content.matchAll(usagePattern)) {
    const line = content.slice(0, match.index).split('\n').length;
    usages.push({ name: match[1], file, line });
  }
}

const missingUsages = usages.filter(({ name }) => !definitions.has(name));

if (missingUsages.length > 0) {
  console.error('Undefined CSS custom properties found:');

  for (const { name, file, line } of missingUsages) {
    console.error(`- --${name} used in ${relative(process.cwd(), file)}:${line}`);
  }

  process.exit(1);
}

console.log(`Validated ${cssFiles.length} CSS files. All CSS custom properties are defined.`);
