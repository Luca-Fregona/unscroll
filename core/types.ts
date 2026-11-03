export interface RedirectRule {
  id: string;
  description: string;
  from: string;
  to: string;
}

export interface HideRule {
  id: string;
  description: string;
  selector: string;
}

export interface WebTarget {
  matches: string[];
  redirects: RedirectRule[];
  hide: HideRule[];
}

export interface AndroidRule {
  id: string;
  description: string;
  when: { viewId?: string; contentDescription?: string };
  action: "back" | "home" | "overlay";
}

export interface AndroidTarget {
  package: string;
  testedVersion: string;
  rules: AndroidRule[];
}

export interface RuleSet {
  schemaVersion: 1;
  id: string;
  name: string;
  description: string;
  version: string;
  web?: WebTarget;
  android?: AndroidTarget;
}

export type ValidationResult =
  | { ok: true; ruleSet: RuleSet }
  | { ok: false; errors: string[] };
