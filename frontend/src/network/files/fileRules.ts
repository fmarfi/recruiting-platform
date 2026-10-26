// Client-side pre-checks for CV uploads. The server decides (Tika detects the real type, 10 MB, 415/413); this only avoids pointless uploads.
export const MAX_FILE_BYTES = 10 * 1024 * 1024;
export const MAX_BATCH = 50;
export const ACCEPT_EXT = ['pdf', 'docx', 'doc', 'rtf', 'odt', 'txt'];
export const ACCEPT_ATTR = ACCEPT_EXT.map((e) => `.${e}`).join(',');

export type Rejection = { fileName: string; code: 'PAYLOAD_TOO_LARGE' | 'UNSUPPORTED_MEDIA_TYPE' | 'TOO_MANY' | 'EMPTY' };

export function splitFiles(files: { name: string; size: number }[], max = MAX_BATCH): { ok: number[]; rejected: Rejection[] } {
  const ok: number[] = [];
  const rejected: Rejection[] = [];
  files.forEach((f, i) => {
    const ext = f.name.includes('.') ? f.name.split('.').pop()!.toLowerCase() : '';
    if (f.size === 0) rejected.push({ fileName: f.name, code: 'EMPTY' });
    else if (!ACCEPT_EXT.includes(ext)) rejected.push({ fileName: f.name, code: 'UNSUPPORTED_MEDIA_TYPE' });
    else if (f.size > MAX_FILE_BYTES) rejected.push({ fileName: f.name, code: 'PAYLOAD_TOO_LARGE' });
    else if (ok.length >= max) rejected.push({ fileName: f.name, code: 'TOO_MANY' });
    else ok.push(i);
  });
  return { ok, rejected };
}

export const formatSize = (b: number) => (b < 1024 * 1024 ? `${Math.max(1, Math.round(b / 1024))} KB` : `${(b / 1024 / 1024).toFixed(1)} MB`);

/** Whole days until `iso` (0 when past); intake items are deleted at expiry. */
export const daysLeft = (iso: string, now = Date.now()) => Math.max(0, Math.ceil((new Date(iso).getTime() - now) / 86_400_000));
