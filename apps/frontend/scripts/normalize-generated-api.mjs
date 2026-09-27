import { readdir, readFile, writeFile } from 'node:fs/promises';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const generatedClientDirectory = fileURLToPath(new URL('../src/app/client/', import.meta.url));

async function normalizeDirectory(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      await normalizeDirectory(path);
    } else if (extname(entry.name) === '.ts' || entry.name === 'README.md') {
      const source = await readFile(path, 'utf8');
      const normalized = source.replace(/[ \t]+$/gm, '');
      if (normalized !== source) {
        await writeFile(path, normalized);
      }
    }
  }
}

await normalizeDirectory(generatedClientDirectory);
