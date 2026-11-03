export interface HeaderMeta {
  id: string;
  name: string;
  description: string;
  matches: string[];
  version: string;
  repoUrl: string;
}

export function buildHeader(meta: HeaderMeta): string {
  const downloadUrl = `${meta.repoUrl}/releases/latest/download/unscroll-${meta.id}.user.js`;
  const fields: Array<[string, string]> = [
    ["name", `unscroll — ${meta.name}`],
    ["namespace", meta.repoUrl],
    ["version", meta.version],
    ["description", meta.description],
    ...meta.matches.map((pattern): [string, string] => ["match", pattern]),
    ["run-at", "document-start"],
    ["grant", "none"],
    ["updateURL", downloadUrl],
    ["downloadURL", downloadUrl],
    ["license", "GPL-3.0-or-later"],
  ];
  return [
    "// ==UserScript==",
    ...fields.map(([key, value]) => `// ${`@${key}`.padEnd(13)}${value}`),
    "// ==/UserScript==",
  ].join("\n");
}
