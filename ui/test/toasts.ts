import { toast } from 'sonner';
import { expect, vi } from 'vitest';

/**
 * Reading the toasts an export surface reported.
 *
 * Both surfaces report through `sonner` (see `src/export/reporting.ts`), so both suites read them the same
 * way: sonner's own markup, `[data-sonner-toast][data-type=...]` with the message in `[data-title]`.
 *
 * A toast is not on screen the moment `toast.error(...)` returns - sonner queues one task before it flushes
 * the state into its host - so a toast is waited for rather than looked for. And the queue is a module
 * singleton that outlives a test: a host mounted by the next test is handed every toast still active (that
 * is how a toast raised before its host mounts is not lost), so a suite that reports has to clear up after
 * itself with {@link clearToasts}.
 */

export type ToastKind = 'error' | 'warning' | 'success';

/** What a toast of this kind says, or '' where there is none. */
export const toastText = (kind: ToastKind): string =>
  document.querySelector(`[data-sonner-toast][data-type="${kind}"] [data-title]`)?.textContent ?? '';

/** Waits for a toast of this kind and answers what it says. */
export const toasted = (kind: ToastKind): Promise<string> =>
  vi.waitFor(() => {
    const said = toastText(kind);
    expect(said, `no ${kind} toast`).not.toBe('');
    return said;
  });

/** Waits for every toast of this kind to be gone. */
export const untoasted = (kind: ToastKind): Promise<void> =>
  vi.waitFor(() => {
    expect(document.querySelector(`[data-sonner-toast][data-type="${kind}"]`)).toBeNull();
  });

/** Takes back every toast, so what one test reported cannot be read as the next one's. */
export const clearToasts = (): void => {
  toast.dismiss();
};
