package com.recruiting.demo.crm;

import com.recruiting.demo.domain.Account;
import com.recruiting.demo.domain.Activity;
import com.recruiting.demo.domain.Application;
import com.recruiting.demo.domain.Contact;
import com.recruiting.demo.domain.JobOrder;
import com.recruiting.demo.domain.Link;
import com.recruiting.demo.domain.NextAction;
import com.recruiting.demo.domain.Person;
import com.recruiting.demo.domain.Proposal;
import com.recruiting.demo.domain.TimelineEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * In-memory store for the demo, filled with fictional data on startup.
 *
 * <p>Stands in for the PostgreSQL database of the real build. Maps keep insertion
 * order so lists come back in a stable order. Access is synchronised by
 * {@link CrmService}; nothing else should mutate the store.
 */
@Component
public class DemoStore {

    /** The consultant the demo is logged in as. */
    public static final String CURRENT_USER = "Deniz Arslan";

    /** All consultants in the demo. */
    public static final List<String> USERS = List.of(CURRENT_USER, "Emre Koç", "Selin Aydın");

    final Map<String, Account> accounts = new LinkedHashMap<>();
    final Map<String, Person> people = new LinkedHashMap<>();
    final Map<String, JobOrder> jobOrders = new LinkedHashMap<>();
    final Map<String, Application> applications = new LinkedHashMap<>();
    final Map<String, Proposal> proposals = new LinkedHashMap<>();
    final Map<String, NextAction> nextActions = new LinkedHashMap<>();
    final List<Contact> contacts = new ArrayList<>();
    final List<Link> links = new ArrayList<>();
    final List<Activity> activities = new ArrayList<>();
    final List<TimelineEvent> timeline = new ArrayList<>();

    private final AtomicInteger sequence = new AtomicInteger(1000);

    /** Creates the store and loads the fictional demo data relative to today. */
    public DemoStore() {
        SeedData.load(this, LocalDate.now());
    }

    /**
     * Returns a new unique id.
     *
     * @param prefix id prefix such as {@code acc} or {@code app}
     * @return a fresh id, e.g. {@code app-1001}
     */
    String newId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }
}
