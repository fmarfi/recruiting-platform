package com.recruiting.platform.accounts.importing.app;

import com.recruiting.platform.accounts.domain.ImportJob;
import com.recruiting.platform.accounts.domain.ImportJobRepository;
import com.recruiting.platform.accounts.importing.parse.TableReader;
import com.recruiting.platform.accounts.importing.web.dto.ImportJobResponse;
import com.recruiting.platform.accounts.importing.web.dto.ImportRowError;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ImportKind;
import com.recruiting.platform.contracts.ImportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Import jobs (A-11): upload and validate, list, read, discard. Writing the rows is {@link ImportCommitService}. */
@Service
@Transactional
public class ImportService {

    public static final int MAX_BYTES = 10 * 1024 * 1024;

    private final ImportJobRepository jobs;
    private final ImportAnalyzer analyzer;
    private final ImportJobMapper mapper;

    public ImportService(ImportJobRepository jobs, ImportAnalyzer analyzer, ImportJobMapper mapper) {
        this.jobs = jobs;
        this.analyzer = analyzer;
        this.mapper = mapper;
    }

    /** Validates (dry run) and stores the job; an unreadable file gives a FAILED job, not an error response. */
    public ImportJobResponse upload(ImportKind kind, String fileName, byte[] content) {
        if (content.length > MAX_BYTES) {
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, "The file is larger than 10 MB.");
        }
        if (!TableReader.isCsv(fileName) && !TableReader.isXlsx(fileName)) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Upload a .csv or .xlsx file.");
        }
        ImportJob job = new ImportJob();
        job.setKind(kind);
        job.setFileName(fileName.length() > 255 ? fileName.substring(0, 255) : fileName);
        job.setTotalRows(0);
        job.setValidRows(0);
        job.setDuplicateRows(0);
        job.setImportedRows(0);
        try {
            ImportAnalysis a = analyzer.analyze(kind, fileName, content);
            job.setStatus(ImportStatus.VALIDATED);
            job.setFileContent(content);
            job.setTotalRows(a.totalRows());
            job.setValidRows(a.valid().size());
            job.setDuplicateRows(a.duplicateRows());
            job.setErrors(a.errors().stream().map(ImportJobMapper::toMap).toList());
        } catch (IllegalArgumentException e) {
            job.setStatus(ImportStatus.FAILED);
            job.setFailureReason(e.getMessage());
            job.setCompletedAt(Instant.now());
        }
        return mapper.response(jobs.saveAndFlush(job));
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportJobResponse> list(Pageable pageable) {
        Page<ImportJob> page = jobs.findAll(pageable);
        return PageResponse.of(page, mapper.responses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public ImportJobResponse get(UUID id) {
        return mapper.response(find(id));
    }

    /** Only a VALIDATED import can be discarded; the stored file and errors are dropped (ADR 0009). */
    public void discard(UUID id) {
        ImportJob job = find(id);
        if (job.getStatus() != ImportStatus.VALIDATED) {
            throw new ApiException(ErrorCode.STATE_CONFLICT, "Only a validated import can be discarded.",
                    Map.of("currentStatus", job.getStatus().name()));
        }
        job.setStatus(ImportStatus.DISCARDED);
        job.setFileContent(null);
        job.setErrors(null);
        job.setCompletedAt(Instant.now());
        jobs.saveAndFlush(job);
    }

    ImportJob find(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Import " + id + " not found."));
    }

    static List<ImportRowError> noErrors() {
        return List.of();
    }
}
