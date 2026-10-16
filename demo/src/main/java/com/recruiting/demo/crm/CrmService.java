package com.recruiting.demo.crm;

import com.recruiting.demo.domain.Account;
import com.recruiting.demo.domain.AccountStatus;
import com.recruiting.demo.domain.Activity;
import com.recruiting.demo.domain.Application;
import com.recruiting.demo.domain.Contact;
import com.recruiting.demo.domain.JobOrder;
import com.recruiting.demo.domain.JobOrderStatus;
import com.recruiting.demo.domain.Lifecycle;
import com.recruiting.demo.domain.Link;
import com.recruiting.demo.domain.NextAction;
import com.recruiting.demo.domain.Person;
import com.recruiting.demo.domain.PipelineStage;
import com.recruiting.demo.domain.Proposal;
import com.recruiting.demo.domain.ProposalStatus;
import com.recruiting.demo.domain.TimelineEvent;
import com.recruiting.demo.domain.Types.ActivityType;
import com.recruiting.demo.domain.Types.ConsentStatus;
import com.recruiting.demo.domain.Types.PersonType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * Use cases of the CRM demo: read views for each screen and the commands that change
 * statuses, move candidates and log activities.
 *
 * <p>Every status change is checked against its {@link Lifecycle} and written to the
 * account timeline (A-02, A-08). Views are returned as JSON-ready maps whose shapes are
 * fixed in {@code demo/API.md}. Methods are synchronised because the store is a plain
 * in-memory structure.
 */
@Service
public class CrmService {

    private final DemoStore store;

    /**
     * Creates the service.
     *
     * @param store the in-memory store with the demo data
     */
    public CrmService(DemoStore store) {
        this.store = store;
    }

    // ---------------------------------------------------------------- meta and home

    /**
     * Returns everything the UI needs to render statuses and forms: lifecycles with
     * labels, tones and allowed next statuses, users and vocabularies.
     *
     * @return the meta view
     */
    public synchronized Map<String, Object> meta() {
        Map<String, Object> lifecycles = map(
                "account", lifecycle(AccountStatus.values()),
                "job", lifecycle(JobOrderStatus.values()),
                "stage", lifecycle(PipelineStage.values()),
                "proposal", lifecycle(ProposalStatus.values()));
        List<String> sectors = store.accounts.values().stream().map(Account::sector)
                .distinct().sorted().toList();
        return map(
                "currentUser", DemoStore.CURRENT_USER,
                "users", DemoStore.USERS,
                "today", today(),
                "lifecycles", lifecycles,
                "stageOrder", Arrays.asList(PipelineStage.values()),
                "activityTypes", Arrays.asList(ActivityType.values()),
                "personTypes", Arrays.asList(PersonType.values()),
                "consentStatuses", Arrays.asList(ConsentStatus.values()),
                "sectors", sectors);
    }

    /**
     * Returns the "My day" home screen for the current user (UX-01, A-10).
     *
     * @return the home view
     */
    public synchronized Map<String, Object> home() {
        String user = DemoStore.CURRENT_USER;
        List<Map<String, Object>> actions = store.nextActions.values().stream()
                .filter(a -> !a.done() && user.equals(a.owner()))
                .sorted(Comparator.comparing(NextAction::due))
                .map(this::actionView).toList();
        List<Map<String, Object>> openJobs = store.jobOrders.values().stream()
                .filter(j -> j.status().isOpen() && user.equals(j.owner()))
                .sorted(Comparator.comparing(JobOrder::opened))
                .map(this::jobSummary).toList();
        List<Map<String, Object>> recent = store.accounts.values().stream()
                .sorted(Comparator.comparing(this::lastActivity,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).map(this::accountSummary).toList();
        long active = store.accounts.values().stream()
                .filter(a -> a.status() == AccountStatus.ACTIVE).count();
        long open = store.jobOrders.values().stream().filter(j -> j.status().isOpen()).count();
        long inProcess = store.applications.values().stream()
                .filter(a -> a.stage() != PipelineStage.REJECTED && a.stage() != PipelineStage.PLACED)
                .filter(a -> job0(a.jobOrderId()).status().isOpen()).count();
        long placed = store.applications.values().stream()
                .filter(a -> a.stage() == PipelineStage.PLACED)
                .filter(a -> a.updated().getYear() == today().getYear()).count();
        return map(
                "user", user,
                "today", today(),
                "actions", actions,
                "openJobs", openJobs,
                "recentAccounts", recent,
                "stats", map("activeAccounts", active, "openJobs", open,
                        "candidatesInProcess", inProcess, "placedThisYear", placed));
    }

    // ---------------------------------------------------------------- accounts

    /**
     * Lists accounts, optionally filtered (A-01, A-14).
     *
     * @param q      free text matched against name, sector and city, or {@code null}
     * @param status account status name, or {@code null}
     * @param sector exact sector, or {@code null}
     * @param owner  exact owner name, or {@code null}
     * @return matching account summaries
     */
    public synchronized List<Map<String, Object>> accounts(String q, String status, String sector,
                                                           String owner) {
        return store.accounts.values().stream()
                .filter(a -> blank(q) || matches(q, a.name(), a.sector(), a.city()))
                .filter(a -> blank(status) || a.status().name().equals(status))
                .filter(a -> blank(sector) || a.sector().equals(sector))
                .filter(a -> blank(owner) || a.owner().equals(owner))
                .map(this::accountSummary).toList();
    }

    /**
     * Opens a new client account as a lead (A-01).
     *
     * @param name   company name; required
     * @param sector sector; required
     * @param city   city, or {@code null}
     * @param owner  responsible consultant, or {@code null} for the current user
     * @return the new account summary
     */
    public synchronized Map<String, Object> createAccount(String name, String sector, String city,
                                                          String owner) {
        if (blank(name) || blank(sector)) {
            throw CrmException.invalid("Enter the company name and sector.");
        }
        String id = store.newId("acc");
        String by = blank(owner) ? DemoStore.CURRENT_USER : owner;
        Account account = new Account(id, name.trim(), sector.trim(), blank(city) ? "" : city.trim(),
                by, AccountStatus.LEAD, today());
        store.accounts.put(id, account);
        event(id, "status", "Account opened as Lead");
        return accountSummary(account);
    }

    /**
     * Returns the account page: header, contacts, job orders, proposals, timeline and
     * dashboard (UX-05, A-08, A-09).
     *
     * @param id the account id
     * @return the account view
     */
    public synchronized Map<String, Object> account(String id) {
        Account account = account0(id);
        List<Map<String, Object>> contacts = store.contacts.stream()
                .filter(c -> c.accountId().equals(id))
                .sorted(Comparator.comparing(Contact::primary).reversed())
                .map(c -> {
                    Person p = person0(c.personId());
                    return map("personId", p.id(), "name", p.name(), "title", p.title(),
                            "role", c.role(), "primary", c.primary(), "email", p.email(),
                            "consent", p.consent(), "lastContact", p.lastContact());
                }).toList();
        List<JobOrder> jobs = store.jobOrders.values().stream()
                .filter(j -> j.accountId().equals(id)).toList();
        List<Map<String, Object>> proposals = store.proposals.values().stream()
                .filter(p -> p.accountId().equals(id))
                .sorted(Comparator.comparing(Proposal::date).reversed())
                .map(this::proposalView).toList();
        List<Map<String, Object>> timeline = store.timeline.stream()
                .filter(t -> t.accountId().equals(id))
                .sorted(Comparator.comparing(TimelineEvent::at).reversed())
                .map(this::timelineView).toList();
        List<NextAction> openActions = store.nextActions.values().stream()
                .filter(a -> !a.done() && id.equals(a.accountId()))
                .sorted(Comparator.comparing(NextAction::due)).toList();

        List<JobOrder> openJobs = jobs.stream().filter(j -> j.status().isOpen()).toList();
        Map<String, Object> perStage = stageCounts(openJobs.stream().map(JobOrder::id).toList());
        long avgDays = Math.round(openJobs.stream().mapToLong(this::daysOpen).average().orElse(0));
        Map<String, Object> dashboard = map(
                "openJobs", openJobs.size(),
                "candidatesPerStage", perStage,
                "avgDaysOpen", avgDays,
                "nextAction", openActions.isEmpty() ? null : actionView(openActions.get(0)));

        return map(
                "account", accountSummary(account),
                "contacts", contacts,
                "jobs", jobs.stream().map(this::jobSummary).toList(),
                "proposals", proposals,
                "timeline", timeline,
                "actions", openActions.stream().map(this::actionView).toList(),
                "dashboard", dashboard);
    }

    /**
     * Changes an account's status if its lifecycle allows it (A-02).
     *
     * @param id     the account id
     * @param target the requested status name
     * @return the updated account summary
     */
    public synchronized Map<String, Object> changeAccountStatus(String id, String target) {
        Account account = account0(id);
        AccountStatus to = parse(AccountStatus.class, target);
        check(account.status(), to, "an account");
        store.accounts.put(id, account.withStatus(to));
        event(id, "status", "Account status changed from " + account.status().labelEn()
                + " to " + to.labelEn());
        return accountSummary(store.accounts.get(id));
    }

    // ---------------------------------------------------------------- job orders and pipeline

    /**
     * Returns the job order page with its pipeline (A-06, A-07, UX-04).
     *
     * @param id the job order id
     * @return the job view
     */
    public synchronized Map<String, Object> job(String id) {
        JobOrder job = job0(id);
        Account account = account0(job.accountId());
        List<Map<String, Object>> apps = store.applications.values().stream()
                .filter(a -> a.jobOrderId().equals(id))
                .sorted(Comparator.comparing(Application::updated).reversed())
                .map(this::applicationView).toList();
        return map(
                "job", jobSummary(job),
                "account", map("id", account.id(), "name", account.name(), "status", account.status()),
                "applications", apps);
    }

    /**
     * Changes a job order's status if its lifecycle allows it (A-06).
     *
     * @param id     the job order id
     * @param target the requested status name
     * @return the updated job summary
     */
    public synchronized Map<String, Object> changeJobStatus(String id, String target) {
        JobOrder job = job0(id);
        JobOrderStatus to = parse(JobOrderStatus.class, target);
        check(job.status(), to, "a job order");
        store.jobOrders.put(id, job.withStatus(to));
        event(job.accountId(), "job", job.title() + ": status changed from "
                + job.status().labelEn() + " to " + to.labelEn());
        return jobSummary(store.jobOrders.get(id));
    }

    /**
     * Lists people from the pool who can still be added to a job order.
     *
     * @param jobId the job order id
     * @return candidates and executives not yet in this pipeline
     */
    public synchronized List<Map<String, Object>> candidatesToAdd(String jobId) {
        job0(jobId);
        List<String> already = store.applications.values().stream()
                .filter(a -> a.jobOrderId().equals(jobId)).map(Application::personId).toList();
        return store.people.values().stream()
                .filter(p -> p.type() == PersonType.CANDIDATE || p.type() == PersonType.EXECUTIVE)
                .filter(p -> !already.contains(p.id()))
                .sorted(Comparator.comparing(Person::name))
                .map(this::personSummary).toList();
    }

    /**
     * Adds a person to a job order's pipeline at stage {@link PipelineStage#SOURCED}.
     * The same person may sit in several job orders (B-05).
     *
     * @param jobId    the job order id
     * @param personId the person id
     * @return the new application view
     */
    public synchronized Map<String, Object> addApplication(String jobId, String personId) {
        JobOrder job = job0(jobId);
        Person person = person0(personId);
        if (!job.status().isOpen()) {
            throw CrmException.illegalTransition("This job order is " + job.status().labelEn()
                    + "; reopen it before adding candidates.");
        }
        boolean exists = store.applications.values().stream()
                .anyMatch(a -> a.jobOrderId().equals(jobId) && a.personId().equals(personId));
        if (exists) {
            throw CrmException.invalid(person.name() + " is already in this pipeline.");
        }
        String id = store.newId("app");
        Application app = new Application(id, jobId, personId, PipelineStage.SOURCED, null, today());
        store.applications.put(id, app);
        event(job.accountId(), "candidate", person.name() + " added to " + job.title());
        return applicationView(app);
    }

    /**
     * Moves a candidate to another pipeline stage (A-07). Moving to
     * {@link PipelineStage#REJECTED} requires a reason.
     *
     * @param id     the application id
     * @param target the requested stage name
     * @param reason rejection reason, or {@code null}
     * @return the updated application view
     */
    public synchronized Map<String, Object> moveApplication(String id, String target, String reason) {
        Application app = Optional.ofNullable(store.applications.get(id))
                .orElseThrow(() -> CrmException.notFound("Application", id));
        PipelineStage to = parse(PipelineStage.class, target);
        check(app.stage(), to, "a candidate");
        if (to.requiresReason() && blank(reason)) {
            throw CrmException.invalid("Enter a reason for rejecting the candidate.");
        }
        Application moved = app.moveTo(to, to.requiresReason() ? reason.trim() : null, today());
        store.applications.put(id, moved);
        JobOrder job = job0(app.jobOrderId());
        String text = person0(app.personId()).name() + " moved to " + to.labelEn() + " · " + job.title()
                + (to.requiresReason() ? " (" + reason.trim() + ")" : "");
        event(job.accountId(), "candidate", text);
        return applicationView(moved);
    }

    // ---------------------------------------------------------------- proposals

    /**
     * Changes a proposal's status if its lifecycle allows it (A-05).
     *
     * @param id     the proposal id
     * @param target the requested status name
     * @return the updated proposal view
     */
    public synchronized Map<String, Object> changeProposalStatus(String id, String target) {
        Proposal proposal = Optional.ofNullable(store.proposals.get(id))
                .orElseThrow(() -> CrmException.notFound("Proposal", id));
        ProposalStatus to = parse(ProposalStatus.class, target);
        check(proposal.status(), to, "a proposal");
        Proposal updated = proposal.withStatus(to, today());
        store.proposals.put(id, updated);
        event(proposal.accountId(), "proposal", "Proposal " + to.labelEn().toLowerCase(Locale.ROOT)
                + ": " + proposal.title());
        return proposalView(updated);
    }

    // ---------------------------------------------------------------- network

    /**
     * Lists people in the network (A-12).
     *
     * @param q    free text matched against name, title, company and city, or {@code null}
     * @param type person type name, or {@code null}
     * @return matching person summaries, by name
     */
    public synchronized List<Map<String, Object>> people(String q, String type) {
        return store.people.values().stream()
                .filter(p -> blank(q) || matches(q, p.name(), p.title(), p.company(), p.city()))
                .filter(p -> blank(type) || p.type().name().equals(type))
                .sorted(Comparator.comparing(Person::name))
                .map(this::personSummary).toList();
    }

    /**
     * Returns the person page: profile, connections, accounts, pipelines and activity
     * (A-12, A-13, B-05).
     *
     * @param id the person id
     * @return the person view
     */
    public synchronized Map<String, Object> person(String id) {
        Person p = person0(id);
        List<Map<String, Object>> connections = new ArrayList<>();
        for (Link link : store.links) {
            String other = link.fromId().equals(id) ? link.toId()
                    : link.toId().equals(id) ? link.fromId() : null;
            if (other != null && store.people.containsKey(other)) {
                Person o = store.people.get(other);
                String kind = link.fromId().equals(id) ? link.kind() : reverse(link.kind());
                connections.add(map("personId", o.id(), "name", o.name(), "title", o.title(),
                        "company", o.company(), "kind", kind));
            }
        }
        List<Map<String, Object>> accounts = store.contacts.stream()
                .filter(c -> c.personId().equals(id))
                .map(c -> map("accountId", c.accountId(), "name", account0(c.accountId()).name(),
                        "role", c.role()))
                .toList();
        List<Map<String, Object>> apps = store.applications.values().stream()
                .filter(a -> a.personId().equals(id))
                .sorted(Comparator.comparing(Application::updated).reversed())
                .map(a -> {
                    JobOrder j = job0(a.jobOrderId());
                    return map("id", a.id(), "jobId", j.id(), "jobTitle", j.title(),
                            "accountId", j.accountId(), "accountName", account0(j.accountId()).name(),
                            "stage", a.stage(), "updated", a.updated());
                }).toList();
        List<Map<String, Object>> activities = store.activities.stream()
                .filter(a -> id.equals(a.personId()))
                .sorted(Comparator.comparing(Activity::at).reversed())
                .map(a -> map("id", a.id(), "type", a.type(), "note", a.note(), "at", a.at(),
                        "user", a.user(), "accountId", a.accountId(),
                        "accountName", a.accountId() == null ? null : account0(a.accountId()).name()))
                .toList();
        List<Map<String, Object>> actions = store.nextActions.values().stream()
                .filter(a -> !a.done() && id.equals(a.personId()))
                .sorted(Comparator.comparing(NextAction::due))
                .map(this::actionView).toList();
        Map<String, Object> person = map("id", p.id(), "name", p.name(), "title", p.title(),
                "company", p.company(), "type", p.type(), "email", p.email(), "city", p.city(),
                "howWeKnow", p.howWeKnow(), "owner", p.owner(), "lastContact", p.lastContact(),
                "consent", p.consent(), "career", p.career());
        return map("person", person, "connections", connections, "accounts", accounts,
                "applications", apps, "activities", activities, "actions", actions);
    }

    // ---------------------------------------------------------------- activities and actions

    /**
     * Logs a call, visit, meeting, e-mail or note against an account and/or a person
     * (A-04, UX-06). Updates the person's last contact date and the account timeline.
     *
     * @param accountId account id, or {@code null}
     * @param personId  person id, or {@code null}
     * @param type      activity type name
     * @param note      what happened; required
     * @return the new activity
     */
    public synchronized Activity logActivity(String accountId, String personId, String type,
                                             String note) {
        if (blank(accountId) && blank(personId)) {
            throw CrmException.invalid("Choose an account or a person for the activity.");
        }
        if (blank(note)) {
            throw CrmException.invalid("Write a short note about the activity.");
        }
        ActivityType activityType = parse(ActivityType.class, type);
        String account = blank(accountId) ? null : account0(accountId).id();
        String person = blank(personId) ? null : person0(personId).id();
        if (account == null && person != null) {
            account = store.contacts.stream().filter(c -> c.personId().equals(person))
                    .map(Contact::accountId).findFirst().orElse(null);
        }
        Activity activity = new Activity(store.newId("act"), account, person, activityType,
                note.trim(), LocalDateTime.now().withNano(0), DemoStore.CURRENT_USER);
        store.activities.add(activity);
        if (person != null) {
            store.people.put(person, store.people.get(person).withLastContact(today()));
        }
        if (account != null) {
            String who = person == null ? "" : " with " + store.people.get(person).name();
            event(account, "activity", label(activityType) + who + ": " + activity.note());
        }
        return activity;
    }

    /**
     * Creates a next action for the current user (A-10).
     *
     * @param title     what to do; required
     * @param due       due date (ISO); required
     * @param accountId related account, or {@code null}
     * @param personId  related person, or {@code null}
     * @return the new action view
     */
    public synchronized Map<String, Object> createAction(String title, String due, String accountId,
                                                         String personId) {
        if (blank(title) || blank(due)) {
            throw CrmException.invalid("Enter what to do and a due date.");
        }
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(due);
        } catch (RuntimeException e) {
            throw CrmException.invalid("Use the date format YYYY-MM-DD for the due date.");
        }
        String account = blank(accountId) ? null : account0(accountId).id();
        String person = blank(personId) ? null : person0(personId).id();
        NextAction action = new NextAction(store.newId("todo"), title.trim(), dueDate,
                DemoStore.CURRENT_USER, account, person, false);
        store.nextActions.put(action.id(), action);
        return actionView(action);
    }

    /**
     * Marks a next action as done.
     *
     * @param id the action id
     * @return the updated action view
     */
    public synchronized Map<String, Object> completeAction(String id) {
        NextAction action = Optional.ofNullable(store.nextActions.get(id))
                .orElseThrow(() -> CrmException.notFound("Next action", id));
        NextAction done = action.complete();
        store.nextActions.put(id, done);
        return actionView(done);
    }

    // ---------------------------------------------------------------- search

    /**
     * Searches accounts, people and job orders with one query (A-14, UX-02).
     *
     * @param q the search text
     * @return matches grouped by kind
     */
    public synchronized Map<String, Object> search(String q) {
        if (blank(q)) {
            return map("accounts", List.of(), "people", List.of(), "jobs", List.of());
        }
        Predicate<JobOrder> jobMatch = j -> matches(q, j.title(), j.location(),
                account0(j.accountId()).name());
        return map(
                "accounts", accounts(q, null, null, null),
                "people", people(q, null),
                "jobs", store.jobOrders.values().stream().filter(jobMatch).map(this::jobSummary).toList());
    }

    // ---------------------------------------------------------------- view builders

    private Map<String, Object> accountSummary(Account a) {
        long openJobs = store.jobOrders.values().stream()
                .filter(j -> j.accountId().equals(a.id()) && j.status().isOpen()).count();
        return map("id", a.id(), "name", a.name(), "sector", a.sector(), "city", a.city(),
                "owner", a.owner(), "status", a.status(), "since", a.since(),
                "openJobs", openJobs, "lastActivity", lastActivity(a));
    }

    private Map<String, Object> jobSummary(JobOrder j) {
        long active = store.applications.values().stream()
                .filter(a -> a.jobOrderId().equals(j.id()) && a.stage() != PipelineStage.REJECTED)
                .count();
        return map("id", j.id(), "title", j.title(), "accountId", j.accountId(),
                "accountName", account0(j.accountId()).name(), "location", j.location(),
                "seniority", j.seniority(), "status", j.status(), "opened", j.opened(),
                "daysOpen", daysOpen(j), "owner", j.owner(), "fee", j.fee(),
                "candidates", active, "stageCounts", stageCounts(List.of(j.id())));
    }

    private Map<String, Object> personSummary(Person p) {
        return map("id", p.id(), "name", p.name(), "title", p.title(), "company", p.company(),
                "type", p.type(), "city", p.city(), "owner", p.owner(),
                "lastContact", p.lastContact(), "consent", p.consent());
    }

    private Map<String, Object> applicationView(Application a) {
        Person p = person0(a.personId());
        return map("id", a.id(), "jobId", a.jobOrderId(), "personId", p.id(), "name", p.name(),
                "title", p.title(), "company", p.company(), "stage", a.stage(),
                "rejectionReason", a.rejectionReason(), "updated", a.updated(),
                "consent", p.consent());
    }

    private Map<String, Object> proposalView(Proposal p) {
        return map("id", p.id(), "accountId", p.accountId(), "title", p.title(),
                "terms", p.terms(), "status", p.status(), "date", p.date());
    }

    private Map<String, Object> actionView(NextAction a) {
        String accountName = a.accountId() == null || !store.accounts.containsKey(a.accountId())
                ? null : store.accounts.get(a.accountId()).name();
        String personName = a.personId() == null || !store.people.containsKey(a.personId())
                ? null : store.people.get(a.personId()).name();
        return map("id", a.id(), "title", a.title(), "due", a.due(), "owner", a.owner(),
                "accountId", a.accountId(), "accountName", accountName,
                "personId", a.personId(), "personName", personName,
                "overdue", !a.done() && a.due().isBefore(today()), "done", a.done());
    }

    private Map<String, Object> timelineView(TimelineEvent t) {
        return map("accountId", t.accountId(), "at", t.at(), "kind", t.kind(), "text", t.text(),
                "user", t.user());
    }

    private Map<String, Object> stageCounts(List<String> jobIds) {
        Map<String, Object> counts = new LinkedHashMap<>();
        for (PipelineStage stage : PipelineStage.values()) {
            counts.put(stage.name(), store.applications.values().stream()
                    .filter(a -> jobIds.contains(a.jobOrderId()) && a.stage() == stage).count());
        }
        return counts;
    }

    private static <S extends Enum<S> & Lifecycle<S>> Map<String, Object> lifecycle(S[] values) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (S s : values) {
            out.put(s.name(), map("en", s.labelEn(), "tr", s.labelTr(), "tone", s.tone(),
                    "next", s.next().stream().map(Enum::name).toList()));
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private void event(String accountId, String kind, String text) {
        store.timeline.add(new TimelineEvent(accountId, LocalDateTime.now().withNano(0), kind, text,
                DemoStore.CURRENT_USER));
    }

    private LocalDateTime lastActivity(Account a) {
        return store.timeline.stream().filter(t -> t.accountId().equals(a.id()))
                .map(TimelineEvent::at).max(Comparator.naturalOrder()).orElse(null);
    }

    private long daysOpen(JobOrder j) {
        return ChronoUnit.DAYS.between(j.opened(), today());
    }

    private static <S extends Enum<S> & Lifecycle<S>> void check(S from, S to, String what) {
        if (from == to) {
            throw CrmException.illegalTransition("The status is already " + to.labelEn() + ".");
        }
        if (!from.canMoveTo(to)) {
            throw CrmException.illegalTransition("Cannot move " + what + " from " + from.labelEn()
                    + " to " + to.labelEn() + ".");
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (blank(value)) {
            throw CrmException.invalid("Choose a value for " + type.getSimpleName() + ".");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw CrmException.invalid("Unknown " + type.getSimpleName() + ": " + value + ".");
        }
    }

    private Account account0(String id) {
        return Optional.ofNullable(store.accounts.get(id))
                .orElseThrow(() -> CrmException.notFound("Account", id));
    }

    private Person person0(String id) {
        return Optional.ofNullable(store.people.get(id))
                .orElseThrow(() -> CrmException.notFound("Person", id));
    }

    private JobOrder job0(String id) {
        return Optional.ofNullable(store.jobOrders.get(id))
                .orElseThrow(() -> CrmException.notFound("Job order", id));
    }

    private static String reverse(String kind) {
        return switch (kind) {
            case "introduced" -> "introduced by";
            case "introduced by" -> "introduced";
            case "referred" -> "referred by";
            case "reports to" -> "manages";
            default -> kind;
        };
    }

    private static String label(ActivityType type) {
        String n = type.name().toLowerCase(Locale.ROOT);
        return type == ActivityType.EMAIL ? "E-mail" : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static boolean matches(String q, String... fields) {
        String needle = fold(q);
        return Arrays.stream(fields).filter(Objects::nonNull).anyMatch(f -> fold(f).contains(needle));
    }

    /** Lower-cases and strips Turkish diacritics so "ozturk" finds "Öztürk". */
    private static String fold(String s) {
        String lower = s.trim().toLowerCase(Locale.forLanguageTag("tr"));
        StringBuilder out = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            out.append(switch (c) {
                case 'ç' -> 'c';
                case 'ğ' -> 'g';
                case 'ı' -> 'i';
                case 'ö' -> 'o';
                case 'ş' -> 's';
                case 'ü' -> 'u';
                case 'â' -> 'a';
                case 'î' -> 'i';
                case 'û' -> 'u';
                default -> c;
            });
        }
        return out.toString();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static LocalDate today() {
        return LocalDate.now();
    }

    /** Builds an ordered map that, unlike {@code Map.of}, accepts {@code null} values. */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
