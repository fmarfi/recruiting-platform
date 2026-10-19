package com.recruiting.platform.privacy.retention.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.privacy.domain.RetentionSettings;
import com.recruiting.platform.privacy.domain.RetentionSettingsRepository;
import com.recruiting.platform.privacy.retention.web.dto.RetentionDtos.Settings;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and changes the single retention settings row (NFR-03, admin only to change). */
@Service
public class RetentionSettingsService {

    private final RetentionSettingsRepository repository;

    RetentionSettingsService(RetentionSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Settings get() {
        return toDto(repository.findById((short) 1).orElseGet(RetentionSettings::new));
    }

    @Transactional
    public Settings update(Settings request) {
        RetentionSettings s = repository.findById((short) 1).orElseGet(RetentionSettings::new);
        if (request.version() != null && !request.version().equals(s.getVersion())) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT, "The settings were changed by someone else.");
        }
        s.setRetentionMonths(request.retentionMonths());
        s.setBasis(request.basis());
        s.setAction(request.action());
        s.setWarningDays(request.warningDays());
        s.setAutoSendNotice(request.autoSendNotice());
        s.setAutoSendPersonTypes(String.join(",", request.autoSendPersonTypes().stream().map(Enum::name)
                .distinct().toList()));
        s.setRenewOnConsent(request.renewOnConsent());
        return toDto(repository.saveAndFlush(s));
    }

    private static Settings toDto(RetentionSettings s) {
        List<PersonType> types = Arrays.stream(s.getAutoSendPersonTypes().split(","))
                .filter(t -> !t.isBlank()).map(PersonType::valueOf).toList();
        return new Settings(s.getRetentionMonths(), s.getBasis(), s.getAction(), s.getWarningDays(),
                s.isAutoSendNotice(), types, s.isRenewOnConsent(), s.getVersion());
    }
}
