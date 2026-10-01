import { rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const frontendRoot = new URL('../', import.meta.url);
const generatedDirectories = ['.nyc_output/', 'coverage/'];

await Promise.all(
  generatedDirectories.map((directory) =>
    rm(fileURLToPath(new URL(directory, frontendRoot)), { recursive: true, force: true }),
  ),
);
