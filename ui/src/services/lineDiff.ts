/** One line of a line diff: kept in both texts, only in the old one, or only in the new one. */
export interface DiffLine {
  kind: 'same' | 'removed' | 'added';
  text: string;
}

/** One line of one side of a side-by-side diff, with its number in that side's text. */
export interface DiffCell {
  number: number;
  text: string;
  kind: DiffLine['kind'];
}

/** One row of a side-by-side diff. A side without a cell has no line there: the other side added or removed one. */
export interface DiffRow {
  left?: DiffCell;
  right?: DiffCell;
}

/**
 * Above this many cells the table of common lengths costs more memory than a settings page should take, so
 * the differing middle is shown as replaced as a whole.
 */
const MAX_CELLS = 4_000_000;

/**
 * The lines of a text. A newline at its end ends the last line rather than starting an empty one: the extension
 * compares the values trimmed, so a copy which lost it is still a copy, and the comparison must not mark it.
 */
const splitLines = (text: string): string[] => {
  const lines = text.replace(/\r\n/g, '\n').split('\n');
  if (lines[lines.length - 1] === '') lines.pop();
  return lines;
};
const line =
  (kind: DiffLine['kind']) =>
  (text: string): DiffLine => ({ kind, text });

/**
 * Compares two texts line by line, by the longest common subsequence of their lines. The common head and tail
 * are matched first, so an edit in one place of a long template stays cheap.
 */
export function lineDiff(oldText: string, newText: string): DiffLine[] {
  const oldLines = splitLines(oldText);
  const newLines = splitLines(newText);

  let start = 0;
  while (start < oldLines.length && start < newLines.length && oldLines[start] === newLines[start]) start++;
  let oldEnd = oldLines.length;
  let newEnd = newLines.length;
  while (oldEnd > start && newEnd > start && oldLines[oldEnd - 1] === newLines[newEnd - 1]) {
    oldEnd--;
    newEnd--;
  }

  return [
    ...oldLines.slice(0, start).map(line('same')),
    ...diffMiddle(oldLines.slice(start, oldEnd), newLines.slice(start, newEnd)),
    ...oldLines.slice(oldEnd).map(line('same')),
  ];
}

/**
 * Lays a line diff out in two columns: the old text on the left, the new one on the right. The removed and added
 * lines between two common ones face each other, so a changed line reads as one row.
 */
export function toRows(diff: DiffLine[]): DiffRow[] {
  const rows: DiffRow[] = [];
  let leftNumber = 0;
  let rightNumber = 0;
  let index = 0;
  while (index < diff.length) {
    if (diff[index].kind === 'same') {
      const text = diff[index].text;
      rows.push({
        left: { number: ++leftNumber, text, kind: 'same' },
        right: { number: ++rightNumber, text, kind: 'same' },
      });
      index++;
      continue;
    }
    const removed: string[] = [];
    const added: string[] = [];
    for (; index < diff.length && diff[index].kind !== 'same'; index++) {
      (diff[index].kind === 'removed' ? removed : added).push(diff[index].text);
    }
    for (let k = 0; k < Math.max(removed.length, added.length); k++) {
      rows.push({
        left: k < removed.length ? { number: ++leftNumber, text: removed[k], kind: 'removed' } : undefined,
        right: k < added.length ? { number: ++rightNumber, text: added[k], kind: 'added' } : undefined,
      });
    }
  }
  return rows;
}

function diffMiddle(oldLines: string[], newLines: string[]): DiffLine[] {
  const rows = oldLines.length;
  const columns = newLines.length;
  if ((rows + 1) * (columns + 1) > MAX_CELLS) {
    return [...oldLines.map(line('removed')), ...newLines.map(line('added'))];
  }

  // common[i * width + j] is the length of the longest common subsequence of oldLines[i..] and newLines[j..]
  const width = columns + 1;
  const common = new Uint32Array((rows + 1) * width);
  for (let i = rows - 1; i >= 0; i--) {
    for (let j = columns - 1; j >= 0; j--) {
      common[i * width + j] =
        oldLines[i] === newLines[j]
          ? common[(i + 1) * width + j + 1] + 1
          : Math.max(common[(i + 1) * width + j], common[i * width + j + 1]);
    }
  }

  const result: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < rows && j < columns) {
    if (oldLines[i] === newLines[j]) {
      result.push({ kind: 'same', text: oldLines[i] });
      i++;
      j++;
    } else if (common[(i + 1) * width + j] >= common[i * width + j + 1]) {
      result.push({ kind: 'removed', text: oldLines[i] });
      i++;
    } else {
      result.push({ kind: 'added', text: newLines[j] });
      j++;
    }
  }
  for (; i < rows; i++) result.push({ kind: 'removed', text: oldLines[i] });
  for (; j < columns; j++) result.push({ kind: 'added', text: newLines[j] });
  return result;
}
