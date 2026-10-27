package com.recruiting.platform.accounts.offlimits.app;

import com.recruiting.platform.accounts.OffLimitsApi;
import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.OffLimitsRule;
import com.recruiting.platform.accounts.domain.OffLimitsRuleRepository;
import com.recruiting.platform.accounts.domain.Person;
import com.recruiting.platform.accounts.domain.PersonEmployment;
import com.recruiting.platform.accounts.domain.PersonEmploymentRepository;
import com.recruiting.platform.accounts.domain.PersonRepository;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleCreateRequest;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleResponse;
import com.recruiting.platform.accounts.offlimits.web.dto.OffLimitsRuleUpdateRequest;
import com.recruiting.platform.accounts.person.app.PersonQueryService;
import com.recruiting.platform.accounts.shared.Paging;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.accounts.shared.dto.AccountRef;
import com.recruiting.platform.accounts.shared.dto.PageMeta;
import com.recruiting.platform.accounts.shared.dto.PageResponse;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.security.UserDirectory;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Off-limits rules (ADR 0011 item 10): CRUD for the admin screen and the {@link OffLimitsApi} for pipeline. */
@Service
@Transactional
public class OffLimitsService implements OffLimitsApi {

    private final OffLimitsRuleRepository rules;
    private final AccountRepository accounts;
    private final PersonRepository people;
    private final PersonEmploymentRepository employments;
    private final UserDirectory users;

    public OffLimitsService(OffLimitsRuleRepository rules, AccountRepository accounts, PersonRepository people,
            PersonEmploymentRepository employments, UserDirectory users) {
        this.rules = rules;
        this.accounts = accounts;
        this.people = people;
        this.employments = employments;
        this.users = users;
    }

    private static LocalDate today() {
        return LocalDate.now(PersonQueryService.ZONE);
    }

    // ---------------------------------------------------------------- REST

    @Transactional(readOnly = true)
    public PageResponse<OffLimitsRuleResponse> list(UUID accountId, Boolean active, String q, Integer page,
            Integer size, List<String> sort) {
        Pageable pageable = Paging.of(page, size, sort, Map.of("startsOn", "startsOn", "endsOn", "endsOn",
                "createdAt", "createdAt"), Sort.by(Sort.Order.desc("startsOn")));
        LocalDate today = today();
        Map<UUID, Account> accountById = accounts.findAllById(rules.findAll().stream()
                .map(OffLimitsRule::getAccountId).filter(java.util.Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
        String needle = q == null || q.isBlank() ? null : fold(q);
        List<OffLimitsRule> all = rules.findAll().stream()
                .filter(r -> accountId == null || accountId.equals(r.getAccountId()))
                .filter(r -> active == null || isActive(r, today) == active)
                .filter(r -> needle == null || fold(targetName(r, accountById)).contains(needle))
                .sorted(comparator(pageable.getSort()))
                .toList();
        int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        List<OffLimitsRuleResponse> content = all.subList(from, to).stream().map(r -> response(r, accountById)).toList();
        int pages = (int) Math.ceil(all.size() / (double) pageable.getPageSize());
        return new PageResponse<>(content, new PageMeta(pageable.getPageSize(), pageable.getPageNumber(), all.size(),
                pages));
    }

    @Transactional(readOnly = true)
    public OffLimitsRuleResponse get(UUID id) {
        return response(find(id), null);
    }

    public OffLimitsRuleResponse create(OffLimitsRuleCreateRequest r) {
        if ((r.accountId() == null) == (r.companyName() == null || r.companyName().isBlank())) {
            throw PatchBinder.validation("accountId", "ONE_OF", "send exactly one of accountId and companyName");
        }
        LocalDate starts = r.startsOn() == null ? today() : r.startsOn();
        if (r.endsOn() != null && r.endsOn().isBefore(starts)) {
            throw PatchBinder.validation("endsOn", "RANGE", "must not be before startsOn");
        }
        OffLimitsRule rule = new OffLimitsRule();
        if (r.accountId() != null) {
            accounts.findById(r.accountId()).orElseThrow(
                    () -> PatchBinder.validation("accountId", "INVALID", "unknown account"));
            rule.setAccountId(r.accountId());
        } else {
            rule.setCompanyName(r.companyName().trim());
        }
        rule.setReason(r.reason());
        rule.setNote(r.note());
        rule.setStartsOn(starts);
        rule.setEndsOn(r.endsOn());
        failIfOverlapping(rule, null);
        return response(rules.saveAndFlush(rule), null);
    }

    public OffLimitsRuleResponse update(UUID id, Patch<OffLimitsRuleUpdateRequest> patch) {
        OffLimitsRule rule = find(id);
        OffLimitsRuleUpdateRequest r = patch.body();
        Versions.check(r.version(), rule.getVersion());
        boolean any = patch.has("reason") || patch.has("note") || patch.has("startsOn") || patch.has("endsOn");
        if (!any) {
            throw PatchBinder.validation("body", "REQUIRED", "send at least one field to change");
        }
        PatchBinder.notNull(patch, "reason", r.reason());
        PatchBinder.notNull(patch, "startsOn", r.startsOn());
        if (patch.has("reason")) {
            rule.setReason(r.reason());
        }
        if (patch.has("note")) {
            rule.setNote(r.note());
        }
        if (patch.has("startsOn")) {
            rule.setStartsOn(r.startsOn());
        }
        if (patch.has("endsOn")) {
            rule.setEndsOn(r.endsOn());
        }
        if (rule.getEndsOn() != null && rule.getEndsOn().isBefore(rule.getStartsOn())) {
            throw PatchBinder.validation("endsOn", "RANGE", "must not be before startsOn");
        }
        failIfOverlapping(rule, rule.getId());
        return response(rules.saveAndFlush(rule), null);
    }

    // ---------------------------------------------------------------- OffLimitsApi

    @Override
    @Transactional(readOnly = true)
    public List<Match> findForPerson(UUID personId, UUID ignoreAccountId) {
        Person p = people.findById(personId).orElse(null);
        if (p == null || p.getAnonymisedAt() != null) {
            return List.of();
        }
        return matchesFor(p, ignoreAccountId).stream().map(OffLimitsService::match).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findForCompany(String companyName, UUID ignoreAccountId) {
        if (companyName == null || companyName.isBlank()) {
            return List.of();
        }
        return rules.findActiveByCompany(companyName, today()).stream()
                .filter(r -> ignoreAccountId == null || !ignoreAccountId.equals(r.getAccountId()))
                .map(OffLimitsService::match).toList();
    }

    /** The Person.offLimits field: active rules that match the person's current company. */
    @Transactional(readOnly = true)
    public List<OffLimitsRuleResponse> activeFor(Person p) {
        if (p.getAnonymisedAt() != null) {
            return List.of();
        }
        List<OffLimitsRule> found = matchesFor(p, null);
        return found.stream().map(r -> response(r, null)).toList();
    }

    // ---------------------------------------------------------------- helpers

    private List<OffLimitsRule> matchesFor(Person p, UUID ignoreAccountId) {
        List<String> companies = new ArrayList<>();
        if (p.getCompany() != null && !p.getCompany().isBlank()) {
            companies.add(p.getCompany());
        }
        for (PersonEmployment e : employments.findByPersonIdOrderBySortOrder(p.getId())) {
            if (e.getEndDate() == null && e.getCompany() != null && !e.getCompany().isBlank()) {
                companies.add(e.getCompany());
            }
        }
        Map<UUID, OffLimitsRule> found = new LinkedHashMap<>();
        for (String c : companies) {
            for (OffLimitsRule r : rules.findActiveByCompany(c, today())) {
                if (ignoreAccountId == null || !ignoreAccountId.equals(r.getAccountId())) {
                    found.putIfAbsent(r.getId(), r);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private static Match match(OffLimitsRule r) {
        return new Match(r.getId(), r.getAccountId(), r.getCompanyName(), r.getReason(), r.getStartsOn(),
                r.getEndsOn());
    }

    /** An overlapping rule for the same account or company gives 409 DUPLICATE with the existing id. */
    private void failIfOverlapping(OffLimitsRule candidate, UUID selfId) {
        Map<UUID, Account> accountById = Map.of();
        for (OffLimitsRule o : rules.findAll()) {
            if (o.getId().equals(selfId) || !sameTarget(candidate, o)) {
                continue;
            }
            boolean startsBeforeOtherEnds = o.getEndsOn() == null || !candidate.getStartsOn().isAfter(o.getEndsOn());
            boolean endsAfterOtherStarts = candidate.getEndsOn() == null
                    || !candidate.getEndsOn().isBefore(o.getStartsOn());
            if (startsBeforeOtherEnds && endsAfterOtherStarts) {
                throw new ApiException(ErrorCode.DUPLICATE, "A rule for this target is already in force then.",
                        Map.of("existingId", o.getId()));
            }
        }
    }

    private boolean sameTarget(OffLimitsRule a, OffLimitsRule b) {
        if (a.getAccountId() != null || b.getAccountId() != null) {
            return a.getAccountId() != null && a.getAccountId().equals(b.getAccountId());
        }
        return fold(a.getCompanyName()).equals(fold(b.getCompanyName()));
    }

    private static boolean isActive(OffLimitsRule r, LocalDate today) {
        return !r.getStartsOn().isAfter(today) && (r.getEndsOn() == null || !r.getEndsOn().isBefore(today));
    }

    private static Comparator<OffLimitsRule> comparator(Sort sort) {
        Comparator<OffLimitsRule> result = null;
        for (Sort.Order o : sort) {
            Comparator<OffLimitsRule> c = switch (o.getProperty()) {
                case "endsOn" -> Comparator.comparing(OffLimitsRule::getEndsOn,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                case "createdAt" -> Comparator.comparing(OffLimitsRule::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                default -> Comparator.comparing(OffLimitsRule::getStartsOn);
            };
            c = o.isDescending() ? c.reversed() : c;
            result = result == null ? c : result.thenComparing(c);
        }
        Comparator<OffLimitsRule> byId = Comparator.comparing(OffLimitsRule::getId);
        return result == null ? byId : result.thenComparing(byId);
    }

    private String targetName(OffLimitsRule r, Map<UUID, Account> accountById) {
        if (r.getAccountId() == null) {
            return r.getCompanyName();
        }
        Account a = accountById.get(r.getAccountId());
        return a == null ? "" : a.getName();
    }

    private static String fold(String s) {
        String t = s.trim().replace('İ', 'I').replace('ı', 'i').toLowerCase(Locale.ROOT);
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private OffLimitsRule find(UUID id) {
        return rules.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Off-limits rule " + id + " not found."));
    }

    private OffLimitsRuleResponse response(OffLimitsRule r, Map<UUID, Account> known) {
        AccountRef account = null;
        if (r.getAccountId() != null) {
            Account a = known != null && known.containsKey(r.getAccountId()) ? known.get(r.getAccountId())
                    : accounts.findById(r.getAccountId()).orElse(null);
            account = a == null ? null : new AccountRef(a.getId(), a.getName(), a.getStatus());
        }
        return new OffLimitsRuleResponse(r.getId(), account, r.getCompanyName(), r.getReason(), r.getNote(),
                r.getStartsOn(), r.getEndsOn(), isActive(r, today()), r.getCreatedAt(),
                users.ref(r.getCreatedBy()).orElse(null), r.getVersion() == null ? 0 : r.getVersion());
    }
}
