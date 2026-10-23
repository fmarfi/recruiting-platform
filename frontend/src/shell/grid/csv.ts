// Safe CSV for grid exports. Cells starting with = + - @ are prefixed so spreadsheets do not run them as formulas.
const cell = (v: unknown) => {
  let s = v == null ? '' : String(v);
  if (/^[=+\-@\t\r]/.test(s)) s = `'${s}`;
  return /[",\n\r;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

export function toCsv(header: string[], rows: unknown[][]): string {
  return [header, ...rows].map((r) => r.map(cell).join(';')).join('\r\n');
}

/** Semicolon separated with a BOM: opens correctly in Excel with Turkish regional settings. */
export function downloadCsv(fileName: string, csv: string) {
  const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000); // some browsers cancel the download if revoked at once
}
