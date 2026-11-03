import type { RuleSet } from "../core/types";
import { buildHideCss } from "./engine/hide";
import { resolveRedirect } from "./engine/redirect";
import { watchUrl } from "./engine/watch";

export interface RuntimeEnv {
  win: Window;
  navigate: (href: string) => void;
  intervalMs?: number;
}

/** Applies one rule set to the current page. Returns a function that stops watching. */
export function run(ruleSet: RuleSet, env: RuntimeEnv): () => void {
  const web = ruleSet.web;
  if (!web) return () => {};

  const initial = resolveRedirect(web.redirects, env.win.location.href);
  if (initial) {
    env.navigate(initial);
    return () => {};
  }

  injectStyle(env.win, `unscroll-${ruleSet.id}`, buildHideCss(web.hide));

  return watchUrl(
    env.win,
    (href) => {
      const target = resolveRedirect(web.redirects, href);
      if (target) env.navigate(target);
    },
    env.intervalMs,
  );
}

/**
 * At document-start the document can still be empty (Chromium has no <html> yet),
 * so wait for the first element that can hold a <style>.
 */
function injectStyle(win: Window, id: string, css: string): void {
  const doc = win.document;
  const style = doc.createElement("style");
  style.id = id;
  style.textContent = css;

  const parent = () => doc.head ?? doc.documentElement;
  const existing = parent();
  if (existing) {
    existing.appendChild(style);
    return;
  }
  const observer = new MutationObserver(() => {
    const ready = parent();
    if (!ready) return;
    observer.disconnect();
    ready.appendChild(style);
  });
  observer.observe(doc, { childList: true, subtree: true });
}
