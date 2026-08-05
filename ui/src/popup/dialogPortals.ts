import { useEffect } from 'react';
import type { RefObject } from 'react';

/**
 * Moves the dropdown popups into the dialog they belong to, so that they are painted above it.
 *
 * RSP's `SearchableSelect` shows its option list in a `position: fixed` portal which the shared dropdown
 * appends to the element's root node - the shadow root here, making it a **sibling** of the dialog. That is
 * right on an ordinary page and right in the document properties side panel, which has no dialog at all.
 * Inside RSP's `Modal` it is not: the modal is a native `<dialog>` opened with `showModal()`, which puts it
 * in the browser's top layer, and the top layer paints above everything in the normal layer whatever its
 * z-index says. So the option list opened *underneath* the dialog, with only the part hanging past the
 * dialog's bottom edge visible.
 *
 * A descendant of the dialog is in the top layer with it, so moving each portal there fixes the paint order.
 * The position needs no adjusting: the portal is `position: fixed` against the viewport, and neither the
 * dialog nor anything above it establishes a containing block that would change what that is measured from.
 * The dialog must not clip its overflow for this to be visible - see `export-popup.css`.
 *
 * A `MutationObserver` rather than a one-off pass, because the form grows dropdowns as it goes: ticking
 * "Cover page" or switching comments on mounts a `SearchableSelect` that creates its portal right then.
 *
 * This is a regression of RSP 0.2.0, not a fact of life. Its `Modal` was a `<div class="rsp-modal-overlay">`
 * at `z-index: 1000` until then, and `.sd-portal` declares `z-index: 2147483000` - so the option list won on
 * z-index, which is how strictdoc-exporter's format dropdown works today on 0.1.0 with no code of its own for
 * it. The rewrite onto `<dialog>` moved the modal into the top layer, where z-index does not compare, and
 * that deliberate "always on top" value stopped being reachable. pdf-exporter is only the first extension to
 * combine the 0.2.0 `Modal` with a `SearchableSelect`; strictdoc breaks the same way the moment it upgrades.
 *
 * So the fix belongs in RSP, which knows both the portal and the dialog. Until it is there, this is the one
 * place in this extension that needs it.
 */
export default function useDropdownPopupsInDialog(inside: RefObject<HTMLElement | null>): void {
  useEffect(() => {
    const anchor = inside.current;
    const dialog = anchor?.closest('dialog');
    const root = anchor?.getRootNode();
    if (!dialog || !(root instanceof ShadowRoot)) {
      return undefined;
    }

    const adopt = () => {
      // The root's own children, read as `children` rather than matched with `:scope >`: `:scope` does not
      // resolve to a shadow root, so that selector silently matches nothing. A portal already moved is a
      // child of the dialog and so is not seen here again.
      [...root.children]
        .filter((child) => child.classList.contains('sd-portal'))
        .forEach((portal) => dialog.appendChild(portal));
    };

    adopt();
    const observer = new MutationObserver(adopt);
    observer.observe(root, { childList: true });
    return () => observer.disconnect();
  }, [inside]);
}
