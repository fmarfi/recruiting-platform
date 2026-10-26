import { api } from '../../api';
import { useAsync } from '../../pipeline/shared';

/** openapi-fetch sends FormData as multipart (it drops the JSON Content-Type); the typed `file: string` is the generator's view of a binary field. */
const form = (file: File, extra?: Record<string, string>) => () => {
  const fd = new FormData();
  fd.append('file', file, file.name);
  for (const [k, v] of Object.entries(extra ?? {})) fd.append(k, v);
  return fd;
};

export const usePersonFiles = (id: string) => useAsync(() => api.GET('/people/{id}/files', { params: { path: { id } } }), [id]);

export const uploadPersonFile = (id: string, file: File) =>
  api.POST('/people/{id}/files', { params: { path: { id } }, body: { file: '', kind: 'CV' }, bodySerializer: form(file, { kind: 'CV' }) });

export const uploadCvIntake = (files: File[]) =>
  api.POST('/cv-intake', {
    // The generated body type is a single `files` field; the multipart body repeats it per file.
    body: { files: [] },
    bodySerializer: () => {
      const fd = new FormData();
      for (const f of files) fd.append('files', f, f.name);
      return fd;
    },
  });

export const personFileUrl = (id: string, fileId: string) => `/api/v1/people/${id}/files/${fileId}/content`;
export const intakeFileUrl = (id: string) => `/api/v1/cv-intake/${id}/content`;
