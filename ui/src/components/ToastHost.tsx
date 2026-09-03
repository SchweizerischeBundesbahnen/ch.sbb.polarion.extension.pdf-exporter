import { useEffect, useState } from 'react';
import { Toaster } from '@sbb-polarion/react-sbb-polarion';

/**
 * Where a toast appears: the shared RSP `Toaster`, and only one of them at a time.
 *
 * An administration page needs no such thing - it mounts one `Toaster` at the app root and is done. The
 * export surfaces cannot: each of them lives in a shadow root of its own, which sees none of the rules
 * sonner puts in the document, and the export dialog is a native `<dialog>` in the browser's top layer,
 * which paints above everything in the normal layer whatever its z-index - so a host outside that dialog
 * would report *behind* it, under its backdrop. Each surface therefore renders a host of its own, the
 * dialog's inside the dialog.
 *
 * Which is a problem, because `toast()` is a module singleton that broadcasts to **every** mounted
 * `Toaster`, and two of these surfaces are on one page whenever a document is open in the editor: the
 * Document Properties side panel, and the dialog the toolbar button opens over it. Both hosts would show
 * every message - twice, in the same place, one of the two behind the backdrop.
 *
 * So the hosts take turns: the newest one renders, the rest stand down until it is gone. The dialog is
 * always the newer one (it is opened over the panel, or over an administration page in the development
 * harness), and closing it hands the previous host its toasts back.
 */

/** The mounted hosts, oldest first. Module scope on purpose - this is exactly what has to be shared. */
let hosts: object[] = [];
const listeners = new Set<() => void>();

const announce = () => listeners.forEach((listener) => listener());

export default function ToastHost() {
  /** This host's identity, stable across renders. */
  const [host] = useState(() => ({}));
  const [active, setActive] = useState(false);

  useEffect(() => {
    const update = () => setActive(hosts[hosts.length - 1] === host);
    hosts = [...hosts, host];
    listeners.add(update);
    // Every host is told, this one included: it has just become the newest.
    announce();
    return () => {
      hosts = hosts.filter((mounted) => mounted !== host);
      listeners.delete(update);
      announce();
    };
  }, [host]);

  // `expand`, because one operation can report twice: a conversion that produced a file *and* had something
  // to say about it raises a warning and a success. Sonner stacks its toasts by default - the newest in
  // front, the rest scaled down behind it with their text hidden until the pointer is over them - so the
  // "PDF was successfully generated" would all but cover the warning it belongs with. Expanded, each is laid
  // out under the one before it and both are read at once.
  return active ? <Toaster expand /> : null;
}
