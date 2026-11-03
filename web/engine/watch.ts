/**
 * Calls `onChange` whenever the page URL changes, including in-page (SPA) navigations.
 * Polling plus `popstate` works in any userscript manager, including isolated worlds
 * where `history.pushState` cannot be patched.
 */
export function watchUrl(
  win: Window,
  onChange: (href: string) => void,
  intervalMs = 300,
): () => void {
  let last = win.location.href;
  const check = () => {
    const href = win.location.href;
    if (href === last) return;
    last = href;
    onChange(href);
  };
  const timer = win.setInterval(check, intervalMs);
  win.addEventListener("popstate", check);
  return () => {
    win.clearInterval(timer);
    win.removeEventListener("popstate", check);
  };
}
