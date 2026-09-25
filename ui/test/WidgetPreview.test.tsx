import { pageViolations } from '@sbb-polarion/react-sbb-polarion/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import WidgetPreview from '../src/pages/WidgetPreview';

// The Bulk PDF Export widget's development harness: the real widget, mounted into a shadow root of the page
// against the sample data of each scenario the harness offers.

afterEach(() => {
  cleanup();
});

const widgetRoot = () => document.querySelector('#widget-preview-host')?.shadowRoot ?? null;

const scenario = async (name: string) => {
  const button = Array.from(document.querySelectorAll<HTMLButtonElement>('.preview-controls button')).find(
    (b) => b.textContent === name,
  )!;
  await userEvent.click(button);
};

describe('the widget development harness, accessibility', () => {
  it('has no WCAG A/AA violations with the loaded widget', async () => {
    render(<WidgetPreview />);
    await expect.poll(() => widgetRoot()?.querySelector('.polarion-rpw-table-counts')).toBeTruthy();
    expect(await pageViolations()).toEqual([]);
  });

  it('has no WCAG A/AA violations with the failing widget', async () => {
    render(<WidgetPreview />);
    await expect.poll(() => widgetRoot()?.querySelector('.polarion-rpw-table-counts')).toBeTruthy();
    await scenario('Failing');
    await expect.poll(() => widgetRoot()?.querySelector('.widget-error')).toBeTruthy();
    expect(await pageViolations()).toEqual([]);
  });
});
