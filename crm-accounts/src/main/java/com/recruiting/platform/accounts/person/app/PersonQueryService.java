package com.recruiting.platform.accounts.person.app;

import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonFileRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.person.web.dto.PersonSummaryResponse;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.Specs;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.accounts.domain.Compensation;
import com.recruiting.platform.contracts.CompensationPeriod;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.JobFunction;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.Seniority;
import java.math.BigDecimal;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.SourceChannel;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Filtered network list (A-12, UX-06, candidate pool B-02) and the people predicate reused by global search (A-14). */
@Service
@Transactional(readOnly = true)
public class PersonQueryService {

    public static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    /** All filters of GET /people. */
    public record Filter(String q, List<PersonType> types, UUID ownerId, List<ConsentStatus> consent,
            LocalDate lastContactBefore, LocalDate lastContactAfter, List<String> cities, String company,
            List<String> tags, List<SourceChannel> sourceChannels, String cv, Boolean shareable,
            Pool pool) {
        /** Filter without the pool filters. */
        public Filter(String q, List<PersonType> types, UUID ownerId, List<ConsentStatus> consent,
                LocalDate lastContactBefore, LocalDate lastContactAfter, List<String> cities, String company,
                List<String> tags, List<SourceChannel> sourceChannels, String cv, Boolean shareable) {
            this(q, types, ownerId, consent, lastContactBefore, lastContactAfter, cities, company, tags,
                    sourceChannels, cv, shareable, null);
        }
    }

    /** The candidate pool filters of contract 1.3.0. */
    public record Pool(List<String> skills, List<JobFunction> functionAreas, List<Seniority> seniorities,
            List<String> languages, Integer noticeMaxDays, List<Mobility> mobilities, BigDecimal salaryMin,
            BigDecimal salaryMax, String salaryCurrency, Boolean hasCv) {
    }

    private static final int SALARY_ROW_CAP = 5000;

    private final PersonRepository people;
    private final PersonFileRepository files;
    private final PersonMapper mapper;

    private final CompensationAccess compensationAccess;

    public PersonQueryService(PersonRepository people, PersonFileRepository files, PersonMapper mapper,
            CompensationAccess compensationAccess) {
        this.compensationAccess = compensationAccess;
        this.people = people;
        this.files = files;
        this.mapper = mapper;
    }

    /** Old signature kept for callers without the pool filters. */
    public PageResponse<PersonSummaryResponse> list(String q, List<PersonType> types, UUID ownerId,
            List<ConsentStatus> consent, LocalDate lastContactBefore, LocalDate lastContactAfter, Pageable pageable) {
        return list(new Filter(q, types, ownerId, consent, lastContactBefore, lastContactAfter, null, null, null,
                null, null, null), pageable, true);
    }

    /**
     * @param explicitSort true when the client sent {@code sort}; with {@code cv} and no explicit sort the result is
     *                     ordered by relevance
     */
    public PageResponse<PersonSummaryResponse> list(Filter f, Pageable pageable, boolean explicitSort) {
        if (f.cities() != null && f.cities().size() > 50) {
            throw PatchBinder.validation("city", "SIZE", "at most 50 cities");
        }
        if (f.tags() != null && f.tags().size() > 20) {
            throw PatchBinder.validation("tag", "SIZE", "at most 20 tags");
        }
        if (f.cv() != null && (f.cv().trim().length() < 2 || f.cv().length() > 200)) {
            throw PatchBinder.validation("cv", "SIZE", "between 2 and 200 characters");
        }
        Specification<Person> spec = spec(f.q(), f.types(), f.ownerId(), f.consent(), f.lastContactBefore(),
                f.lastContactAfter());
        Set<UUID> idLimit = null;
        if (f.cities() != null && !f.cities().isEmpty()) {
            idLimit = intersect(idLimit, new LinkedHashSet<>(people.findIdsByCities(
                    String.join("\u001f", f.cities().stream().filter(c -> !c.isBlank()).toList()))));
        }
        if (f.tags() != null && !f.tags().isEmpty()) {
            String csv = String.join(",", f.tags().stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).toList());
            idLimit = intersect(idLimit, new LinkedHashSet<>(people.findIdsByAnyTag(csv)));
        }
        Pool pool = f.pool();
        boolean salary = false;
        if (pool != null) {
            if (pool.skills() != null && !pool.skills().isEmpty()) {
                if (pool.skills().size() > 20) {
                    throw PatchBinder.validation("skill", "SIZE", "at most 20 skills");
                }
                idLimit = intersect(idLimit, new LinkedHashSet<>(people.findIdsByAnySkill(
                        String.join(",", pool.skills().stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).toList()))));
            }
            if (pool.languages() != null && !pool.languages().isEmpty()) {
                if (pool.languages().size() > 5) {
                    throw PatchBinder.validation("language", "SIZE", "at most 5 languages");
                }
                idLimit = intersect(idLimit, new LinkedHashSet<>(people.findIdsByAllLanguages(
                        String.join(",", pool.languages().stream().map(t -> t.trim().toLowerCase(Locale.ROOT)).toList()))));
            }
            if (pool.hasCv() != null) {
                Set<UUID> withCv = new LinkedHashSet<>(files.findPersonIdsWithSearchableCv());
                if (pool.hasCv()) {
                    idLimit = intersect(idLimit, withCv);
                } else if (!withCv.isEmpty()) {
                    spec = spec.and((root, query, cb) -> cb.not(root.get("id").in(withCv)));
                }
            }
            salary = pool.salaryMin() != null || pool.salaryMax() != null;
            if (salary && (pool.salaryCurrency() == null || !pool.salaryCurrency().matches("^[A-Z]{3}$"))) {
                throw PatchBinder.validation("salaryCurrency", "REQUIRED", "required with salaryMin or salaryMax");
            }
            spec = spec.and(poolSpec(pool));
        }
        Map<UUID, Double> rank = null;
        if (f.cv() != null) {
            rank = new LinkedHashMap<>();
            for (Object[] row : files.searchCurrentCv(f.cv().trim())) {
                rank.put((UUID) row[0], ((Number) row[1]).doubleValue());
            }
            idLimit = intersect(idLimit, rank.keySet());
        }
        if (idLimit != null) {
            if (idLimit.isEmpty()) {
                return PageResponse.of(new PageImpl<Person>(List.of(), pageable, 0), List.of());
            }
            Collection<UUID> ids = idLimit;
            spec = spec.and((root, query, cb) -> root.get("id").in(ids));
        }
        if (f.company() != null && !f.company().isBlank()) {
            String company = f.company();
            spec = spec.and((root, query, cb) -> Specs.unaccentContains(cb, root.get("company"), company));
        }
        if (f.sourceChannels() != null && !f.sourceChannels().isEmpty()) {
            List<SourceChannel> channels = f.sourceChannels();
            spec = spec.and((root, query, cb) -> root.get("sourceChannel").in(channels));
        }
        if (f.shareable() != null) {
            boolean want = f.shareable();
            spec = spec.and((root, query, cb) -> {
                Predicate yes = cb.and(cb.equal(root.get("consentStatus"), ConsentStatus.GIVEN),
                        cb.isNull(root.get("anonymisedAt")));
                return want ? yes : cb.not(yes);
            });
        }
        Page<Person> page;
        if (salary) {
            List<Person> matched = new ArrayList<>(people.findAll(spec, pageable.getSort()));
            if (matched.size() > SALARY_ROW_CAP) {
                throw PatchBinder.validation("salaryMin", "TOO_BROAD", "narrow the other filters first");
            }
            List<Person> kept = new ArrayList<>(matched.stream().filter(p -> salaryMatches(p, f.pool())).toList());
            if (rank != null && !explicitSort) {
                Map<UUID, Double> scores = rank;
                kept.sort(Comparator.<Person>comparingDouble(p -> -scores.getOrDefault(p.getId(), 0.0))
                        .thenComparing(Person::getName, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), kept.size());
            int to = Math.min(from + pageable.getPageSize(), kept.size());
            page = new PageImpl<>(kept.subList(from, to), pageable, kept.size());
        } else if (rank != null && !explicitSort) {
            Map<UUID, Double> scores = rank;
            List<Person> all = new ArrayList<>(people.findAll(spec));
            all.sort(Comparator.<Person>comparingDouble(p -> -scores.getOrDefault(p.getId(), 0.0))
                    .thenComparing(Person::getName, Comparator.nullsLast(Comparator.naturalOrder())));
            int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), all.size());
            int to = Math.min(from + pageable.getPageSize(), all.size());
            page = new PageImpl<>(all.subList(from, to), pageable, all.size());
        } else {
            page = people.findAll(spec, pageable);
        }
        List<PersonSummaryResponse> content = mapper.summaries(page.getContent());
        if (f.cv() != null && !content.isEmpty()) {
            Map<UUID, String> headlines = new HashMap<>();
            for (Object[] row : files.headlines(f.cv().trim(), page.getContent().stream().map(Person::getId).toList())) {
                String h = String.valueOf(row[1]);
                headlines.put((UUID) row[0], h.length() > 300 ? h.substring(0, 300) : h);
            }
            content = content.stream().map(s -> s.withCvMatch(headlines.get(s.id()))).toList();
        }
        return PageResponse.of(page, content);
    }

    private static Specification<Person> poolSpec(Pool pool) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (pool.functionAreas() != null && !pool.functionAreas().isEmpty()) {
                all.add(root.get("functionArea").in(pool.functionAreas()));
            }
            if (pool.seniorities() != null && !pool.seniorities().isEmpty()) {
                all.add(root.get("seniority").in(pool.seniorities()));
            }
            if (pool.mobilities() != null && !pool.mobilities().isEmpty()) {
                all.add(root.get("mobility").in(pool.mobilities()));
            }
            if (pool.noticeMaxDays() != null) {
                all.add(cb.lessThanOrEqualTo(root.<Short>get("noticePeriodDays"), pool.noticeMaxDays().shortValue()));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }

    /** Expected pay per year in the requested currency; people the caller may not read never match. */
    private boolean salaryMatches(Person p, Pool pool) {
        Compensation c = p.getCompensation();
        if (c == null || c.expected() == null || !compensationAccess.allowed(p)
                || !pool.salaryCurrency().equals(c.currency())) {
            return false;
        }
        BigDecimal yearly = c.period() == CompensationPeriod.MONTHLY ? c.expected().multiply(BigDecimal.valueOf(12))
                : c.expected();
        return (pool.salaryMin() == null || yearly.compareTo(pool.salaryMin()) >= 0)
                && (pool.salaryMax() == null || yearly.compareTo(pool.salaryMax()) <= 0);
    }

    private static Set<UUID> intersect(Set<UUID> current, Set<UUID> other) {
        if (current == null) {
            return new LinkedHashSet<>(other);
        }
        Set<UUID> out = new LinkedHashSet<>(current);
        out.retainAll(other);
        return out;
    }

    /** Same filters as the list; used by search. */
    public Specification<Person> spec(String q, List<PersonType> types, UUID ownerId, List<ConsentStatus> consent,
            LocalDate lastContactBefore, LocalDate lastContactAfter) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                all.add(cb.or(Specs.unaccentContains(cb, root.get("name"), q),
                        Specs.unaccentContains(cb, root.get("company"), q),
                        Specs.unaccentContains(cb, root.get("email"), q)));
            }
            if (types != null && !types.isEmpty()) {
                all.add(root.get("type").in(types));
            }
            if (ownerId != null) {
                all.add(cb.equal(root.get("ownerId"), ownerId));
            }
            if (consent != null && !consent.isEmpty()) {
                all.add(root.get("consentStatus").in(consent));
            }
            if (lastContactBefore != null) {
                Instant end = lastContactBefore.plusDays(1).atStartOfDay(ZONE).toInstant();
                all.add(cb.or(cb.isNull(root.get("lastContactAt")), cb.lessThan(root.<Instant>get("lastContactAt"), end)));
            }
            if (lastContactAfter != null) {
                Instant start = lastContactAfter.atStartOfDay(ZONE).toInstant();
                all.add(cb.greaterThanOrEqualTo(root.<Instant>get("lastContactAt"), start));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }
}
