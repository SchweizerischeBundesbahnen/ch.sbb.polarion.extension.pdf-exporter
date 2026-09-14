import { describe, expect, it } from 'vitest';
import { lineDiff, toRows } from '../src/services/lineDiff';

const kinds = (oldText: string, newText: string) =>
  lineDiff(oldText, newText).map((l) => `${{ same: ' ', removed: '-', added: '+' }[l.kind]}${l.text}`);

describe('lineDiff', () => {
  it('keeps identical texts as they are', () => {
    expect(kinds('a\nb', 'a\nb')).toEqual([' a', ' b']);
  });

  it('marks added and removed lines between common ones', () => {
    expect(kinds('a\nb\nc', 'a\nc\nd')).toEqual([' a', '-b', ' c', '+d']);
  });

  it('shows a changed line as removed then added', () => {
    expect(kinds('h1 {\n  color: red;\n}', 'h1 {\n  color: blue;\n}')).toEqual([
      ' h1 {',
      '-  color: red;',
      '+  color: blue;',
      ' }',
    ]);
  });

  it('reads windows line endings as unix ones', () => {
    expect(kinds('a\r\nb', 'a\nb')).toEqual([' a', ' b']);
  });

  it('handles empty texts', () => {
    expect(kinds('', '')).toEqual([]);
    expect(kinds('', 'a')).toEqual(['+a']);
    expect(kinds('a', '')).toEqual(['-a']);
  });

  it('finds the longest common part in reordered lines', () => {
    expect(kinds('x\na\nb\nc', 'a\nb\nc\nx')).toEqual(['-x', ' a', ' b', ' c', '+x']);
  });

  it('lays the removed and added lines of one change side by side', () => {
    const rows = toRows(lineDiff('a\nold 1\nold 2\nb', 'a\nnew 1\nb\nc'));
    const side = (cell?: { number: number; text: string; kind: string }) =>
      cell ? `${cell.number}${{ same: ' ', removed: '-', added: '+' }[cell.kind]}${cell.text}` : '';
    expect(rows.map((row) => [side(row.left), side(row.right)])).toEqual([
      ['1 a', '1 a'],
      ['2-old 1', '2+new 1'],
      ['3-old 2', ''],
      ['4 b', '3 b'],
      ['', '4+c'],
    ]);
  });

  it('shows the middle of very long different texts as replaced', () => {
    const oldText = Array.from({ length: 2500 }, (_, i) => `old ${i}`).join('\n');
    const newText = Array.from({ length: 2500 }, (_, i) => `new ${i}`).join('\n');
    const diff = lineDiff(`head\n${oldText}\ntail`, `head\n${newText}\ntail`);
    expect(diff[0]).toEqual({ kind: 'same', text: 'head' });
    expect(diff[1]).toEqual({ kind: 'removed', text: 'old 0' });
    expect(diff[2501]).toEqual({ kind: 'added', text: 'new 0' });
    expect(diff[diff.length - 1]).toEqual({ kind: 'same', text: 'tail' });
  });
});
