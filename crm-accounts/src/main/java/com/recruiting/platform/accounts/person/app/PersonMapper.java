package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEducation;
import com.recruiting.platform.accounts.domain.PersonEducationRepository;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonFile;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.person.web.dto.EducationDto;
import com.recruiting.platform.accounts.person.web.dto.Employment;
import com.recruiting.platform.accounts.person.web.dto.LanguageSkillDto;
import com.recruiting.platform.accounts.person.web.dto.PersonFileResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonResponse;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.LanguageLevel;
import com.recruiting.platform.contracts.PersonFileKind;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Entity to response records for people. Owners are looked up in one query per list. */
@Component
public class PersonMapper {

    private final UserDirectory users;
    private final PersonEmploymentRepository employments;
    private final PersonEducationRepository educations;
    private final PersonFileRepository files;
    private final CompensationAccess compensationAccess;

    public PersonMapper(UserDirectory users, PersonEmploymentRepository employments,
            PersonEducationRepository educations, PersonFileRepository files, CompensationAccess compensationAccess) {
        this.users = users;
        this.employments = employments;
        this.educations = educations;
        this.files = files;
        this.compensationAccess = compensationAccess;
    }

    public List<PersonSummaryResponse> summaries(Collection<Person> people) {
        Map<UUID, UserRef> owners = users.refs(people.stream().map(Person::getOwnerId).toList());
        return people.stream().map(p -> summary(p, owners)).toList();
    }

    public PersonSummaryResponse summary(Person p) {
        return summary(p, users.refs(List.of(p.getOwnerId())));
    }

    private PersonSummaryResponse summary(Person p, Map<UUID, UserRef> owners) {
        boolean shareable = p.getConsentStatus() == ConsentStatus.GIVEN && p.getAnonymisedAt() == null;
        return new PersonSummaryResponse(p.getId(), p.getName(), p.getTitle(), p.getCompany(), p.getType(),
                p.getCity(), p.getEmail(), owners.get(p.getOwnerId()), p.getLastContactAt(),
                p.getConsentStatus(), p.getRetentionStatus(), List.copyOf(p.getTags()), p.getSourceChannel(),
                shareable, null);
    }

    public PersonResponse response(Person p) {
        PersonSummaryResponse s = summary(p);
        List<Employment> history = employments.findByPersonIdOrderBySortOrder(p.getId()).stream()
                .map(PersonMapper::employment).toList();
        List<EducationDto> education = educations.findByPersonIdOrderBySortOrder(p.getId()).stream()
                .map(PersonMapper::education).toList();
        boolean canSee = compensationAccess.allowed(p);
        PersonFileResponse currentCv = files.findByPersonIdAndKindAndCurrentTrue(p.getId(), PersonFileKind.CV)
                .map(this::file).orElse(null);
        return new PersonResponse(s.id(), s.name(), s.title(), s.company(), s.type(), s.city(), s.email(),
                s.owner(), s.lastContactAt(), s.consentStatus(), s.retentionStatus(), s.tags(), s.sourceChannel(),
                s.shareable(), null, p.getPhone(), p.getLinkedinUrl(), p.getHowWeKnow(), p.getNotes(), history,
                p.getAnonymisedAt() != null, p.getCreatedAt(), p.getUpdatedAt(),
                p.getVersion() == null ? 0 : p.getVersion(),
                p.getNoticePeriodDays() == null ? null : p.getNoticePeriodDays().intValue(), p.getMobility(),
                canSee ? p.getCompensation() : null, !canSee, languages(p), education, currentCv, List.of());
    }

    public PersonFileResponse file(PersonFile f) {
        return new PersonFileResponse(f.getId(), f.getPersonId(), f.getKind(), f.getVersion(), f.isCurrent(),
                f.getFileName(), f.getContentType(), f.getSizeBytes(), f.getTextStatus(), f.getCreatedAt(),
                users.ref(f.getUploadedBy()).orElse(null));
    }

    private static List<LanguageSkillDto> languages(Person p) {
        List<LanguageSkillDto> out = new ArrayList<>();
        for (Map<String, Object> l : p.getLanguages()) {
            out.add(new LanguageSkillDto(String.valueOf(l.get("language")).toLowerCase(),
                    LanguageLevel.valueOf(String.valueOf(l.get("level")))));
        }
        return out;
    }

    private static Employment employment(PersonEmployment e) {
        return new Employment(e.getCompany(), e.getTitle(), e.getStartDate(), e.getEndDate());
    }

    private static EducationDto education(PersonEducation e) {
        return new EducationDto(e.getSchool(), e.getDegree(), e.getField(),
                e.getStartYear() == null ? null : e.getStartYear().intValue(),
                e.getEndYear() == null ? null : e.getEndYear().intValue());
    }
}
