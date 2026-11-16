/** Client-side check of the signed copy before upload (the server re-checks the bytes): PDF only, at most 50 MB. */
export const MAX_SIGNED_BYTES = 50 * 1024 * 1024;

export type FileCheck = 'ok' | 'notPdf' | 'tooLarge' | 'empty';

export function checkSignedFile(f: { name: string; type?: string; size: number }): FileCheck {
  const named = /\.pdf$/i.test(f.name);
  const pdf = f.type === 'application/pdf' || ((!f.type || f.type === 'application/octet-stream') && named);
  if (!pdf) return 'notPdf';
  if (f.size <= 0) return 'empty';
  if (f.size > MAX_SIGNED_BYTES) return 'tooLarge';
  return 'ok';
}

export function fmtSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
