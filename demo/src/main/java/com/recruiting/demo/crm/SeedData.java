package com.recruiting.demo.crm;

import com.recruiting.demo.domain.Account;
import com.recruiting.demo.domain.AccountStatus;
import com.recruiting.demo.domain.Activity;
import com.recruiting.demo.domain.Application;
import com.recruiting.demo.domain.Contact;
import com.recruiting.demo.domain.JobOrder;
import com.recruiting.demo.domain.JobOrderStatus;
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
import java.util.List;
import java.util.Locale;

/**
 * Fictional demo data for the recruiting CRM: accounts, people, job orders, pipeline,
 * proposals, activities and next actions.
 *
 * <p>All companies, people and e-mail addresses are fictional. Every date is relative
 * to the day the demo starts, so the data always looks current.
 */
final class SeedData {

    private static final String DENIZ = "Deniz Arslan";
    private static final String EMRE = "Emre Koç";
    private static final String SELIN = "Selin Aydın";

    private SeedData() {
    }

    /**
     * Fills the store with the complete fictional data set.
     *
     * @param s     the store to fill
     * @param today the reference date all other dates are relative to
     */
    static void load(DemoStore s, LocalDate today) {
        accounts(s, today);
        clientContacts(s, today);
        candidates(s, today);
        links(s);
        jobs(s, today);
        applications(s, today);
        proposals(s, today);
        activities(s, today);
        statusEvents(s, today);
        actions(s, today);
    }

    // ---------------------------------------------------------------- accounts

    /**
     * Adds the ten client accounts.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void accounts(DemoStore s, LocalDate today) {
        account(s, "acc-1", "Anadolu Perakende A.Ş.", "Retail", "İstanbul", DENIZ, AccountStatus.ACTIVE, today.minusDays(540));
        account(s, "acc-2", "Boğaziçi Yatırım Bankası", "Banking", "İstanbul", EMRE, AccountStatus.ACTIVE, today.minusDays(310));
        account(s, "acc-3", "Egeli Enerji Holding", "Energy", "İzmir", DENIZ, AccountStatus.PROPOSAL_SENT, today.minusDays(35));
        account(s, "acc-4", "Marmara Gıda Sanayi", "FMCG", "Bursa", SELIN, AccountStatus.ACTIVE, today.minusDays(220));
        account(s, "acc-5", "Kuzeyyıldız Otomotiv", "Automotive", "Kocaeli", EMRE, AccountStatus.ON_HOLD, today.minusDays(400));
        account(s, "acc-6", "Nordlicht Logistics GmbH", "Logistics", "Hamburg", SELIN, AccountStatus.LEAD, today.minusDays(24));
        account(s, "acc-7", "Lale Yazılım Teknolojileri", "Technology", "Ankara", DENIZ, AccountStatus.ACTIVE, today.minusDays(150));
        account(s, "acc-8", "Toroslar İlaç", "Pharma", "İstanbul", SELIN, AccountStatus.PROPOSAL_SENT, today.minusDays(48));
        account(s, "acc-9", "Kapadokya Otelcilik", "Hospitality", "Nevşehir", EMRE, AccountStatus.CLOSED, today.minusDays(690));
        account(s, "acc-10", "Vadi Sigorta", "Insurance", "İstanbul", DENIZ, AccountStatus.LEAD, today.minusDays(20));
    }

    // ------------------------------------------------------------------ people

    /**
     * Adds the client contacts and links them to their accounts.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void clientContacts(DemoStore s, LocalDate today) {
        person(s, "per-1", "Ayşe Demir", "Chief People Officer", "Anadolu Perakende A.Ş.", PersonType.CLIENT_CONTACT,
                "İstanbul", "Met at HR Summit 2025", DENIZ, today.minusDays(3), ConsentStatus.GIVEN,
                "Chief People Officer, Anadolu Perakende A.Ş. (2020–now)", "HR Director, Sahil Mağazacılık (2014–2020)");
        person(s, "per-2", "Murat Yıldız", "CEO", "Anadolu Perakende A.Ş.", PersonType.CLIENT_CONTACT,
                "İstanbul", "Referred by Ali Rıza Önal", DENIZ, today.minusDays(14), ConsentStatus.GIVEN,
                "CEO, Anadolu Perakende A.Ş. (2018–now)", "COO, Pazarkent Grup (2011–2018)");
        person(s, "per-3", "Elif Şahin", "HR Director", "Boğaziçi Yatırım Bankası", PersonType.CLIENT_CONTACT,
                "İstanbul", "Former candidate (2024)", EMRE, today.minusDays(6), ConsentStatus.GIVEN,
                "HR Director, Boğaziçi Yatırım Bankası (2022–now)", "HR Manager, Altınova Bank (2016–2022)");
        person(s, "per-4", "Can Öztürk", "CFO", "Egeli Enerji Holding", PersonType.CLIENT_CONTACT,
                "İzmir", "Introduced by Leyla Sarı", DENIZ, today.minusDays(9), ConsentStatus.SENT,
                "CFO, Egeli Enerji Holding (2019–now)", "Finance Director, Rüzgar Enerji A.Ş. (2012–2019)");
        person(s, "per-5", "Zeynep Kaya", "Talent Acquisition Lead", "Marmara Gıda Sanayi", PersonType.CLIENT_CONTACT,
                "Bursa", "Applied via website", SELIN, today.minusDays(2), ConsentStatus.GIVEN,
                "Talent Acquisition Lead, Marmara Gıda Sanayi (2021–now)", "Recruiter, Ova Süt Ürünleri (2017–2021)");
        person(s, "per-6", "Hakan Çelik", "Plant Director", "Kuzeyyıldız Otomotiv", PersonType.CLIENT_CONTACT,
                "Kocaeli", "Met at HR Summit 2025", EMRE, today.minusDays(75), ConsentStatus.NOT_SENT,
                "Plant Director, Kuzeyyıldız Otomotiv (2017–now)", "Production Manager, Gebze Yedek Parça (2009–2017)");
        person(s, "per-7", "Julia Becker", "Head of HR", "Nordlicht Logistics GmbH", PersonType.CLIENT_CONTACT,
                "Hamburg", "LinkedIn direct message", SELIN, today.minusDays(18), ConsentStatus.SENT,
                "Head of HR, Nordlicht Logistics GmbH (2020–now)", "HR Business Partner, Elbe Spedition (2015–2020)");
        person(s, "per-8", "Burak Aksoy", "CTO", "Lale Yazılım Teknolojileri", PersonType.CLIENT_CONTACT,
                "Ankara", "Referred by Ali Rıza Önal", DENIZ, today.minusDays(5), ConsentStatus.GIVEN,
                "CTO, Lale Yazılım Teknolojileri (2019–now)", "Engineering Manager, Başkent Bilişim (2012–2019)");
        person(s, "per-9", "Selda Erdem", "HR Business Partner", "Lale Yazılım Teknolojileri", PersonType.CLIENT_CONTACT,
                "Ankara", "Applied via website", DENIZ, today.minusDays(1), ConsentStatus.GIVEN,
                "HR Business Partner, Lale Yazılım Teknolojileri (2021–now)", "HR Specialist, Çankaya Telekom (2016–2021)");
        person(s, "per-10", "Okan Polat", "Chief Operating Officer", "Toroslar İlaç", PersonType.CLIENT_CONTACT,
                "İstanbul", "Met at HR Summit 2025", SELIN, today.minusDays(11), ConsentStatus.SENT,
                "Chief Operating Officer, Toroslar İlaç (2020–now)", "Plant Manager, Akdeniz Farma (2013–2020)");
        person(s, "per-11", "Derya Uçar", "Owner and General Director", "Kapadokya Otelcilik", PersonType.CLIENT_CONTACT,
                "Nevşehir", "Former candidate (2024)", EMRE, today.minusDays(120), ConsentStatus.GIVEN,
                "General Director, Kapadokya Otelcilik (2015–now)", "Hotel Manager, Peribacası Konukevi (2008–2015)");
        person(s, "per-12", "Kerem Tan", "Head of Distribution", "Vadi Sigorta", PersonType.CLIENT_CONTACT,
                "İstanbul", "LinkedIn direct message", DENIZ, today.minusDays(16), ConsentStatus.NOT_SENT,
                "Head of Distribution, Vadi Sigorta (2021–now)", "Regional Sales Director, Güven Emeklilik (2013–2021)");

        contact(s, "acc-1", "per-1", "Chief People Officer", true);
        contact(s, "acc-1", "per-2", "CEO", false);
        contact(s, "acc-2", "per-3", "HR Director", true);
        contact(s, "acc-3", "per-4", "CFO", true);
        contact(s, "acc-4", "per-5", "Talent Acquisition Lead", true);
        contact(s, "acc-5", "per-6", "Plant Director", true);
        contact(s, "acc-6", "per-7", "Head of HR", true);
        contact(s, "acc-7", "per-8", "CTO", false);
        contact(s, "acc-7", "per-9", "HR Business Partner", true);
        contact(s, "acc-8", "per-10", "Chief Operating Officer", true);
        contact(s, "acc-9", "per-11", "General Director", true);
        contact(s, "acc-10", "per-12", "Head of Distribution", true);
    }

    /**
     * Adds candidates, executives and referrers.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void candidates(DemoStore s, LocalDate today) {
        person(s, "per-20", "Gökhan Yalçın", "Retail Operations Director", "Moda Zinciri A.Ş.", PersonType.EXECUTIVE,
                "İstanbul", "Referred by Ali Rıza Önal", DENIZ, today.minusDays(4), ConsentStatus.GIVEN,
                "Retail Operations Director, Moda Zinciri A.Ş. (2021–now)", "Regional Manager, Moda Zinciri A.Ş. (2016–2021)",
                "Store Manager, Çarşı Giyim (2011–2016)");
        person(s, "per-21", "Nilay Kaplan", "Head of Stores", "Hızlı Market A.Ş.", PersonType.EXECUTIVE,
                "Ankara", "LinkedIn direct message", DENIZ, today.minusDays(8), ConsentStatus.GIVEN,
                "Head of Stores, Hızlı Market A.Ş. (2020–now)", "Regional Operations Manager, Yeşilova Gıda (2015–2020)");
        person(s, "per-22", "Tolga Arıkan", "Supply Chain Director", "Mavi Lojistik A.Ş.", PersonType.EXECUTIVE,
                "İstanbul", "Met at HR Summit 2025", DENIZ, today.minusDays(22), ConsentStatus.GIVEN,
                "Supply Chain Director, Mavi Lojistik A.Ş. (2019–now)", "Logistics Manager, Doruk Dağıtım (2013–2019)",
                "Planning Specialist, Anka Plastik (2009–2013)");
        person(s, "per-23", "Pelin Güneş", "Operations Director", "Pazarkent Grup", PersonType.CANDIDATE,
                "İzmir", "Applied via website", DENIZ, today.minusDays(35), ConsentStatus.GIVEN,
                "Operations Director, Pazarkent Grup (2020–now)", "Operations Manager, Pazarkent Grup (2015–2020)");
        person(s, "per-24", "Serkan Doğan", "Regional Sales Manager", "Ege Mağazacılık", PersonType.CANDIDATE,
                "İzmir", "Former candidate (2024)", DENIZ, today.minusDays(40), ConsentStatus.SENT,
                "Regional Sales Manager, Ege Mağazacılık (2019–now)", "Store Manager, Ege Mağazacılık (2014–2019)");
        person(s, "per-25", "Merve Başaran", "Area Manager", "Sahil Mağazacılık", PersonType.CANDIDATE,
                "İzmir", "LinkedIn direct message", DENIZ, today.minusDays(12), ConsentStatus.GIVEN,
                "Area Manager, Sahil Mağazacılık (2018–now)", "Store Manager, Sahil Mağazacılık (2013–2018)");
        person(s, "per-26", "Cem Tuna", "Treasury Manager", "Altınova Bank", PersonType.EXECUTIVE,
                "İstanbul", "Referred by Leyla Sarı", EMRE, today.minusDays(7), ConsentStatus.GIVEN,
                "Treasury Manager, Altınova Bank (2018–now)", "Senior Dealer, Anıt Menkul Değerler (2012–2018)");
        person(s, "per-27", "Ebru Karahan", "Treasury Director", "Gümüşhane Katılım Bankası", PersonType.EXECUTIVE,
                "İstanbul", "Met at HR Summit 2025", EMRE, today.minusDays(15), ConsentStatus.GIVEN,
                "Treasury Director, Gümüşhane Katılım Bankası (2017–now)", "Head of ALM, Karadeniz Bank (2011–2017)");
        person(s, "per-28", "Volkan Ergin", "Corporate Finance Director", "Tuna Holding", PersonType.CANDIDATE,
                "İstanbul", "Applied via website", EMRE, today.minusDays(50), ConsentStatus.NOT_SENT,
                "Corporate Finance Director, Tuna Holding (2020–now)", "Finance Manager, Tuna Holding (2015–2020)");
        person(s, "per-29", "Gizem Altun", "Supply Chain Director", "Ova Süt Ürünleri", PersonType.EXECUTIVE,
                "Bursa", "Referred by Ali Rıza Önal", SELIN, today.minusDays(3), ConsentStatus.GIVEN,
                "Supply Chain Director, Ova Süt Ürünleri (2019–now)", "Procurement Manager, Ova Süt Ürünleri (2014–2019)");
        person(s, "per-30", "Barış Sezer", "Logistics Director", "Yeşilova Gıda", PersonType.CANDIDATE,
                "Bursa", "Former candidate (2024)", SELIN, today.minusDays(9), ConsentStatus.GIVEN,
                "Logistics Director, Yeşilova Gıda (2018–now)", "Distribution Manager, Doruk Dağıtım (2012–2018)");
        person(s, "per-31", "Ozan Türkmen", "Supply Chain Manager", "Anka Plastik", PersonType.CANDIDATE,
                "Ankara", "Applied via website", SELIN, today.minusDays(30), ConsentStatus.WITHDRAWN,
                "Supply Chain Manager, Anka Plastik (2019–now)", "Planning Lead, Anka Plastik (2014–2019)");
        person(s, "per-32", "Hande Yurt", "Director of Engineering", "Başkent Bilişim", PersonType.EXECUTIVE,
                "Ankara", "LinkedIn direct message", DENIZ, today.minusDays(6), ConsentStatus.GIVEN,
                "Director of Engineering, Başkent Bilişim (2020–now)", "Senior Engineering Manager, Çankaya Telekom (2015–2020)",
                "Software Engineer, Çankaya Telekom (2010–2015)");
        person(s, "per-33", "Levent Aydoğan", "VP Technology", "Sınır Ödeme Sistemleri", PersonType.EXECUTIVE,
                "İstanbul", "Referred by Ali Rıza Önal", DENIZ, today.minusDays(17), ConsentStatus.SENT,
                "VP Technology, Sınır Ödeme Sistemleri (2019–now)", "Head of Platform, Sınır Ödeme Sistemleri (2016–2019)");
        person(s, "per-34", "Sibel Orhan", "Head of Product", "Bulut Ticaret", PersonType.EXECUTIVE,
                "Ankara", "Met at HR Summit 2025", DENIZ, today.minusDays(2), ConsentStatus.GIVEN,
                "Head of Product, Bulut Ticaret (2021–now)", "Senior Product Manager, Çankaya Telekom (2017–2021)");
        person(s, "per-35", "Thomas Wagner", "Managing Director", "Elbe Spedition", PersonType.EXECUTIVE,
                "Hamburg", "LinkedIn direct message", SELIN, today.minusDays(60), ConsentStatus.NOT_SENT,
                "Managing Director, Elbe Spedition (2018–now)", "Operations Director, Elbe Spedition (2012–2018)");
        person(s, "per-36", "Ceren Bulut", "Product Lead", "Pusula Dijital", PersonType.CANDIDATE,
                "İstanbul", "Applied via website", DENIZ, today.minusDays(10), ConsentStatus.GIVEN,
                "Product Lead, Pusula Dijital (2020–now)", "Product Manager, Pusula Dijital (2016–2020)");
        person(s, "per-40", "Ali Rıza Önal", "Board Member", "Önal Danışmanlık", PersonType.REFERRER,
                "İstanbul", "Former client CEO", DENIZ, today.minusDays(21), ConsentStatus.GIVEN,
                "Board Member, Önal Danışmanlık (2022–now)", "CEO, Pazarkent Grup (2010–2022)");
        person(s, "per-41", "Leyla Sarı", "Partner", "Sarı & Demirtaş Hukuk Bürosu", PersonType.REFERRER,
                "İstanbul", "Met at HR Summit 2025", EMRE, today.minusDays(28), ConsentStatus.GIVEN,
                "Partner, Sarı & Demirtaş Hukuk Bürosu (2016–now)", "Senior Associate, Marmara Hukuk (2009–2016)");
    }

    /**
     * Adds the relationships between people.
     *
     * @param s the store
     */
    private static void links(DemoStore s) {
        link(s, "per-40", "per-20", "introduced");
        link(s, "per-40", "per-2", "introduced");
        link(s, "per-40", "per-8", "introduced");
        link(s, "per-40", "per-29", "introduced");
        link(s, "per-40", "per-33", "introduced");
        link(s, "per-41", "per-4", "knows");
        link(s, "per-41", "per-26", "introduced");
        link(s, "per-22", "per-23", "worked with");
        link(s, "per-20", "per-21", "worked with");
        link(s, "per-3", "per-26", "knows");
        link(s, "per-32", "per-34", "worked with");
        link(s, "per-29", "per-30", "knows");
        link(s, "per-8", "per-32", "knows");
        link(s, "per-1", "per-20", "knows");
    }

    // ---------------------------------------------------------- job orders, apps

    /**
     * Adds the nine job orders.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void jobs(DemoStore s, LocalDate today) {
        String fee = "28% of first-year package, retainer in 3 instalments";
        job(s, "job-1", "acc-1", "Retail Operations Director", "İstanbul", "Director", JobOrderStatus.INTERVIEWING, today.minusDays(45), DENIZ, fee);
        job(s, "job-2", "acc-1", "Regional Manager – Aegean", "İzmir", "Senior manager", JobOrderStatus.SOURCING, today.minusDays(12), DENIZ,
                "22% of first-year package, paid on placement");
        job(s, "job-3", "acc-2", "Treasury Director", "İstanbul", "Director", JobOrderStatus.SHORTLIST_SENT, today.minusDays(30), EMRE,
                "30% of first-year package, retainer in 3 instalments");
        job(s, "job-4", "acc-2", "Head of Credit Risk", "İstanbul", "Director", JobOrderStatus.OPEN, today.minusDays(3), EMRE,
                "30% of first-year package, retainer in 3 instalments");
        job(s, "job-5", "acc-4", "Supply Chain Director", "Bursa", "Director", JobOrderStatus.OFFER, today.minusDays(60), SELIN,
                "25% of first-year package, one third retainer");
        job(s, "job-6", "acc-7", "VP Engineering", "Ankara", "VP", JobOrderStatus.SOURCING, today.minusDays(20), DENIZ,
                "30% of first-year package, retainer in 3 instalments");
        job(s, "job-7", "acc-7", "Head of Product", "Ankara / hybrid", "Director", JobOrderStatus.INTERVIEWING, today.minusDays(38), DENIZ,
                "28% of first-year package, retainer in 2 instalments");
        job(s, "job-8", "acc-5", "Plant Manager", "Kocaeli", "Senior manager", JobOrderStatus.CANCELLED, today.minusDays(90), EMRE,
                "24% of first-year package, paid on placement");
        job(s, "job-9", "acc-9", "General Manager", "Nevşehir", "C-level", JobOrderStatus.FILLED, today.minusDays(200), EMRE,
                "30% of first-year package, retainer in 3 instalments");
    }

    /**
     * Adds the pipeline; some candidates appear in several job orders.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void applications(DemoStore s, LocalDate today) {
        app(s, "app-1", "job-1", "per-20", PipelineStage.CLIENT_INTERVIEW, null, today.minusDays(4));
        app(s, "app-2", "job-1", "per-21", PipelineStage.PRESENTED, null, today.minusDays(9));
        app(s, "app-3", "job-1", "per-22", PipelineStage.SCREENED, null, today.minusDays(15));
        app(s, "app-4", "job-1", "per-23", PipelineStage.REJECTED, "Salary expectation above budget", today.minusDays(20));
        app(s, "app-5", "job-2", "per-20", PipelineStage.SCREENED, null, today.minusDays(6));
        app(s, "app-6", "job-2", "per-24", PipelineStage.SCREENED, null, today.minusDays(8));
        app(s, "app-7", "job-2", "per-25", PipelineStage.SOURCED, null, today.minusDays(5));
        app(s, "app-8", "job-3", "per-26", PipelineStage.PRESENTED, null, today.minusDays(7));
        app(s, "app-9", "job-3", "per-27", PipelineStage.PRESENTED, null, today.minusDays(7));
        app(s, "app-10", "job-3", "per-28", PipelineStage.REJECTED, "Client prefers banking background", today.minusDays(10));
        app(s, "app-11", "job-5", "per-29", PipelineStage.OFFER, null, today.minusDays(3));
        app(s, "app-12", "job-5", "per-30", PipelineStage.CLIENT_INTERVIEW, null, today.minusDays(11));
        app(s, "app-13", "job-5", "per-31", PipelineStage.REJECTED, "Relocation to Bursa not possible", today.minusDays(30));
        app(s, "app-14", "job-5", "per-22", PipelineStage.PRESENTED, null, today.minusDays(25));
        app(s, "app-15", "job-6", "per-32", PipelineStage.SCREENED, null, today.minusDays(6));
        app(s, "app-16", "job-6", "per-33", PipelineStage.SOURCED, null, today.minusDays(14));
        app(s, "app-17", "job-6", "per-34", PipelineStage.SCREENED, null, today.minusDays(10));
        app(s, "app-18", "job-7", "per-34", PipelineStage.CLIENT_INTERVIEW, null, today.minusDays(2));
        app(s, "app-19", "job-7", "per-36", PipelineStage.PRESENTED, null, today.minusDays(12));
        app(s, "app-20", "job-7", "per-32", PipelineStage.REJECTED, "Prefers engineering leadership role", today.minusDays(18));
        app(s, "app-21", "job-9", "per-27", PipelineStage.PLACED, null, today.minusDays(150));
        app(s, "app-22", "job-9", "per-31", PipelineStage.REJECTED, "Client prefers hospitality background", today.minusDays(170));
    }

    /**
     * Adds the six proposals.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void proposals(DemoStore s, LocalDate today) {
        proposal(s, "pro-1", "acc-1", "Retail leadership search 2026", "Retainer plus 28% success fee, 12 month exclusivity",
                ProposalStatus.ACCEPTED, today.minusDays(530));
        proposal(s, "pro-2", "acc-2", "Banking senior roles framework", "Framework agreement, 30% per placement, 90 day guarantee",
                ProposalStatus.ACCEPTED, today.minusDays(300));
        proposal(s, "pro-3", "acc-3", "CFO and finance leadership search", "Retainer in 3 instalments, 27% success fee",
                ProposalStatus.SENT, today.minusDays(8));
        proposal(s, "pro-4", "acc-8", "Plant and quality leadership search", "Fixed fee per search, 6 month guarantee",
                ProposalStatus.SENT, today.minusDays(12));
        proposal(s, "pro-5", "acc-10", "Distribution director search", "Draft: 25% success fee, no retainer",
                ProposalStatus.DRAFT, today.minusDays(2));
        proposal(s, "pro-6", "acc-5", "Executive search programme", "Retainer plus 24% success fee",
                ProposalStatus.REJECTED, today.minusDays(100));
    }

    // -------------------------------------------------------------- activities

    /**
     * Adds the logged activities, each with its timeline entry.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void activities(DemoStore s, LocalDate today) {
        activity(s, today, "acc-1", "per-1", ActivityType.MEETING, "Kick-off for the Aegean regional manager search", 2, 10, 0, DENIZ);
        activity(s, today, "acc-1", "per-2", ActivityType.CALL, "CEO feedback on the first interview round", 5, 17, 30, DENIZ);
        activity(s, today, "acc-1", "per-20", ActivityType.CALL, "Prepared candidate for client interview", 6, 9, 15, DENIZ);
        activity(s, today, "acc-1", null, ActivityType.EMAIL, "Sent updated shortlist report for Retail Operations Director", 9, 14, 0, DENIZ);
        activity(s, today, "acc-1", "per-1", ActivityType.NOTE, "Budget for director level confirmed by CPO", 21, 11, 40, DENIZ);
        activity(s, today, "acc-2", "per-3", ActivityType.VISIT, "Visited head office, discussed Head of Credit Risk profile", 3, 15, 0, EMRE);
        activity(s, today, "acc-2", "per-26", ActivityType.CALL, "Screening call for Treasury Director", 8, 16, 20, EMRE);
        activity(s, today, "acc-2", "per-3", ActivityType.EMAIL, "Shortlist of two treasury candidates sent", 7, 12, 0, EMRE);
        activity(s, today, "acc-2", null, ActivityType.NOTE, "Client prefers banking background for all treasury roles", 11, 10, 10, EMRE);
        activity(s, today, "acc-3", "per-4", ActivityType.MEETING, "Presented proposal to CFO in İzmir", 8, 11, 0, DENIZ);
        activity(s, today, "acc-3", "per-4", ActivityType.EMAIL, "Sent follow-up questions on board approval timeline", 4, 9, 45, DENIZ);
        activity(s, today, "acc-3", "per-41", ActivityType.CALL, "Asked Leyla Sarı for context on the holding's board", 13, 18, 0, DENIZ);
        activity(s, today, "acc-4", "per-5", ActivityType.CALL, "Offer terms discussed for Supply Chain Director", 3, 14, 30, SELIN);
        activity(s, today, "acc-4", "per-29", ActivityType.MEETING, "Offer meeting with finalist", 4, 16, 0, SELIN);
        activity(s, today, "acc-4", "per-30", ActivityType.CALL, "Debrief after second client interview", 10, 17, 45, SELIN);
        activity(s, today, "acc-4", null, ActivityType.NOTE, "Client wants decision before month end", 14, 9, 30, SELIN);
        activity(s, today, "acc-5", "per-6", ActivityType.CALL, "Plant hiring stays frozen until next budget cycle", 45, 10, 30, EMRE);
        activity(s, today, "acc-5", "per-6", ActivityType.EMAIL, "Confirmed cancellation of Plant Manager search", 52, 13, 0, EMRE);
        activity(s, today, "acc-6", "per-7", ActivityType.MEETING, "Video call, introduction to Turkish executive market", 17, 15, 30, SELIN);
        activity(s, today, "acc-6", "per-35", ActivityType.EMAIL, "Shared market overview for logistics executives", 24, 11, 0, SELIN);
        activity(s, today, "acc-7", "per-8", ActivityType.VISIT, "Visited engineering team in Ankara", 5, 13, 0, DENIZ);
        activity(s, today, "acc-7", "per-9", ActivityType.CALL, "Agreed interview slots for Head of Product", 1, 10, 0, DENIZ);
        activity(s, today, "acc-7", "per-34", ActivityType.MEETING, "Interview preparation with Sibel Orhan", 2, 16, 0, DENIZ);
        activity(s, today, "acc-7", null, ActivityType.NOTE, "VP Engineering profile refined: needs payments experience", 19, 9, 20, DENIZ);
        activity(s, today, "acc-8", "per-10", ActivityType.MEETING, "Proposal walkthrough with COO", 12, 14, 0, SELIN);
        activity(s, today, "acc-8", "per-10", ActivityType.EMAIL, "Reminder on open proposal questions", 5, 9, 0, SELIN);
        activity(s, today, "acc-9", "per-11", ActivityType.CALL, "Check-in after General Manager's first 100 days", 55, 12, 0, EMRE);
        activity(s, today, "acc-10", "per-12", ActivityType.CALL, "First call about distribution leadership", 16, 11, 30, DENIZ);
        activity(s, today, "acc-10", "per-12", ActivityType.EMAIL, "Sent credentials and sample search brief", 8, 15, 15, DENIZ);
        activity(s, today, null, "per-40", ActivityType.MEETING, "Coffee with Ali Rıza Önal, new introductions discussed", 21, 9, 30, DENIZ);
        activity(s, today, null, "per-22", ActivityType.CALL, "Informal catch-up, open to senior supply chain roles", 22, 18, 30, DENIZ);
    }

    /**
     * Adds timeline events for status changes, candidate moves and proposals.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void statusEvents(DemoStore s, LocalDate today) {
        statusEvent(s, "acc-1", today.minusDays(500).atTime(10, 0), "proposal", "Proposal accepted: Retail leadership search 2026", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(500).atTime(10, 5), "status", "Status changed from Proposal sent to Active", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(45).atTime(9, 0), "job", "Job order opened: Retail Operations Director", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(4).atTime(16, 30), "candidate",
                "Gökhan Yalçın moved to Client interview · Retail Operations Director", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(9).atTime(12, 0), "candidate",
                "Nilay Kaplan moved to Presented · Retail Operations Director", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(20).atTime(15, 0), "candidate",
                "Pelin Güneş rejected · Retail Operations Director (Salary expectation above budget)", DENIZ);
        statusEvent(s, "acc-1", today.minusDays(12).atTime(9, 30), "job", "Job order opened: Regional Manager – Aegean", DENIZ);
        statusEvent(s, "acc-2", today.minusDays(290).atTime(9, 30), "status", "Status changed from Proposal sent to Active", EMRE);
        statusEvent(s, "acc-2", today.minusDays(7).atTime(12, 30), "job", "Shortlist sent: Treasury Director", EMRE);
        statusEvent(s, "acc-2", today.minusDays(3).atTime(9, 0), "job", "Job order opened: Head of Credit Risk", EMRE);
        statusEvent(s, "acc-2", today.minusDays(10).atTime(14, 0), "candidate",
                "Volkan Ergin rejected · Treasury Director (Client prefers banking background)", EMRE);
        statusEvent(s, "acc-3", today.minusDays(8).atTime(11, 30), "proposal", "Proposal sent: CFO and finance leadership search", DENIZ);
        statusEvent(s, "acc-3", today.minusDays(8).atTime(11, 35), "status", "Status changed from Lead to Proposal sent", DENIZ);
        statusEvent(s, "acc-4", today.minusDays(3).atTime(15, 0), "candidate",
                "Gizem Altun moved to Offer · Supply Chain Director", SELIN);
        statusEvent(s, "acc-4", today.minusDays(11).atTime(18, 0), "candidate",
                "Barış Sezer moved to Client interview · Supply Chain Director", SELIN);
        statusEvent(s, "acc-4", today.minusDays(30).atTime(10, 0), "candidate",
                "Ozan Türkmen rejected · Supply Chain Director (Relocation to Bursa not possible)", SELIN);
        statusEvent(s, "acc-5", today.minusDays(52).atTime(13, 10), "job", "Job order cancelled: Plant Manager", EMRE);
        statusEvent(s, "acc-5", today.minusDays(45).atTime(10, 45), "status", "Status changed from Active to On hold", EMRE);
        statusEvent(s, "acc-5", today.minusDays(100).atTime(9, 0), "proposal", "Proposal rejected: Executive search programme", EMRE);
        statusEvent(s, "acc-6", today.minusDays(24).atTime(10, 0), "status", "Account created as Lead", SELIN);
        statusEvent(s, "acc-7", today.minusDays(20).atTime(10, 0), "job", "Job order opened: VP Engineering", DENIZ);
        statusEvent(s, "acc-7", today.minusDays(2).atTime(17, 0), "candidate",
                "Sibel Orhan moved to Client interview · Head of Product", DENIZ);
        statusEvent(s, "acc-7", today.minusDays(18).atTime(16, 0), "candidate",
                "Hande Yurt rejected · Head of Product (Prefers engineering leadership role)", DENIZ);
        statusEvent(s, "acc-8", today.minusDays(12).atTime(14, 30), "proposal", "Proposal sent: Plant and quality leadership search", SELIN);
        statusEvent(s, "acc-8", today.minusDays(12).atTime(14, 35), "status", "Status changed from Lead to Proposal sent", SELIN);
        statusEvent(s, "acc-9", today.minusDays(150).atTime(11, 0), "candidate",
                "Ebru Karahan placed · General Manager", EMRE);
        statusEvent(s, "acc-9", today.minusDays(140).atTime(11, 0), "job", "Job order filled: General Manager", EMRE);
        statusEvent(s, "acc-9", today.minusDays(130).atTime(9, 0), "status", "Status changed from Active to Closed", EMRE);
        statusEvent(s, "acc-10", today.minusDays(2).atTime(16, 0), "proposal", "Proposal drafted: Distribution director search", DENIZ);
        statusEvent(s, "acc-10", today.minusDays(20).atTime(10, 0), "status", "Account created as Lead", DENIZ);
    }

    // ----------------------------------------------------------- next actions

    /**
     * Adds the follow-ups for "My day": two overdue, two due today, the rest this week.
     *
     * @param s     the store
     * @param today the reference date
     */
    private static void actions(DemoStore s, LocalDate today) {
        action(s, "act-1", "Send revised candidate report to Ayşe Demir", today.minusDays(2), DENIZ, "acc-1", "per-1", false);
        action(s, "act-2", "Call Can Öztürk about proposal decision", today.minusDays(1), DENIZ, "acc-3", "per-4", false);
        action(s, "act-3", "Prepare Sibel Orhan for final interview", today, DENIZ, "acc-7", "per-34", false);
        action(s, "act-4", "Request references for Gökhan Yalçın", today, DENIZ, "acc-1", "per-20", false);
        action(s, "act-5", "Send credentials deck to Kerem Tan", today.plusDays(1), DENIZ, "acc-10", "per-12", false);
        action(s, "act-6", "Schedule VP Engineering sourcing review with Burak Aksoy", today.plusDays(3), DENIZ, "acc-7", "per-8", false);
        action(s, "act-7", "Follow up with Ali Rıza Önal on new introductions", today.plusDays(6), DENIZ, null, "per-40", false);
        action(s, "act-8", "Draft long list for Head of Credit Risk", today.plusDays(2), EMRE, "acc-2", null, false);
        action(s, "act-9", "Confirm offer acceptance with Gizem Altun", today.plusDays(1), SELIN, "acc-4", "per-29", false);
        action(s, "act-10", "Chase Okan Polat for proposal feedback", today.plusDays(4), SELIN, "acc-8", "per-10", false);
        action(s, "act-11", "Send KVKK consent request to Levent Aydoğan", today.minusDays(3), DENIZ, null, "per-33", true);
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Adds an account.
     *
     * @param s      the store
     * @param id     account id
     * @param name   company name
     * @param sector business sector
     * @param city   main city
     * @param owner  responsible consultant
     * @param status account status
     * @param since  date the account was opened
     */
    private static void account(DemoStore s, String id, String name, String sector, String city,
                                String owner, AccountStatus status, LocalDate since) {
        s.accounts.put(id, new Account(id, name, sector, city, owner, status, since));
    }

    /**
     * Adds a person; the e-mail address is derived from the name.
     *
     * @param s           the store
     * @param id          person id
     * @param name        full name
     * @param title       current title
     * @param company     current employer
     * @param type        person type
     * @param city        city of residence
     * @param howWeKnow   how the firm knows the person
     * @param owner       relationship owner
     * @param lastContact date of last contact
     * @param consent     KVKK consent status
     * @param career      company history, most recent first
     */
    private static void person(DemoStore s, String id, String name, String title, String company,
                               PersonType type, String city, String howWeKnow, String owner,
                               LocalDate lastContact, ConsentStatus consent, String... career) {
        s.people.put(id, new Person(id, name, title, company, type, emailOf(name), city, howWeKnow,
                owner, lastContact, consent, List.of(career)));
    }

    /**
     * Builds a fictional e-mail address such as {@code ayse.demir@example.com}.
     *
     * @param name full name
     * @return the e-mail address
     */
    private static String emailOf(String name) {
        String ascii = name.replace('ç', 'c').replace('Ç', 'C').replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ı', 'i').replace('İ', 'I').replace('ö', 'o').replace('Ö', 'O')
                .replace('ş', 's').replace('Ş', 'S').replace('ü', 'u').replace('Ü', 'U');
        return ascii.toLowerCase(Locale.ROOT).trim().replace(' ', '.') + "@example.com";
    }

    /**
     * Links a person to an account.
     *
     * @param s         the store
     * @param accountId the account
     * @param personId  the person
     * @param role      role at the client
     * @param primary   whether this is the main contact
     */
    private static void contact(DemoStore s, String accountId, String personId, String role, boolean primary) {
        s.contacts.add(new Contact(accountId, personId, role, primary));
    }

    /**
     * Adds a relationship between two people.
     *
     * @param s    the store
     * @param from first person
     * @param to   second person
     * @param kind relationship kind
     */
    private static void link(DemoStore s, String from, String to, String kind) {
        s.links.add(new Link(from, to, kind));
    }

    /**
     * Adds a job order.
     *
     * @param s         the store
     * @param id        job id
     * @param accountId client account
     * @param title     role title
     * @param location  work location
     * @param seniority seniority level
     * @param status    job status
     * @param opened    opening date
     * @param owner     lead consultant
     * @param fee       fee terms
     */
    private static void job(DemoStore s, String id, String accountId, String title, String location,
                            String seniority, JobOrderStatus status, LocalDate opened, String owner, String fee) {
        s.jobOrders.put(id, new JobOrder(id, accountId, title, location, seniority, status, opened, owner, fee));
    }

    /**
     * Adds an application of a candidate to a job order.
     *
     * @param s       the store
     * @param id      application id
     * @param jobId   job order id
     * @param person  candidate id
     * @param stage   pipeline stage
     * @param reason  rejection reason or {@code null}
     * @param updated date of the last stage change
     */
    private static void app(DemoStore s, String id, String jobId, String person, PipelineStage stage,
                            String reason, LocalDate updated) {
        s.applications.put(id, new Application(id, jobId, person, stage, reason, updated));
    }

    /**
     * Adds a proposal.
     *
     * @param s         the store
     * @param id        proposal id
     * @param accountId client account
     * @param title     what the proposal covers
     * @param terms     commercial terms
     * @param status    proposal status
     * @param date      date of the last change
     */
    private static void proposal(DemoStore s, String id, String accountId, String title, String terms,
                                 ProposalStatus status, LocalDate date) {
        s.proposals.put(id, new Proposal(id, accountId, title, terms, status, date));
    }

    /**
     * Adds an activity and, when it belongs to an account, the matching timeline event.
     *
     * @param s         the store
     * @param today     the reference date
     * @param accountId related account or {@code null}
     * @param personId  related person or {@code null}
     * @param type      activity type
     * @param note      what happened
     * @param daysAgo   how many days ago it happened
     * @param hour      hour of day
     * @param minute    minute of hour
     * @param user      who logged it
     */
    private static void activity(DemoStore s, LocalDate today, String accountId, String personId,
                                 ActivityType type, String note, int daysAgo, int hour, int minute, String user) {
        var at = today.minusDays(daysAgo).atTime(hour, minute);
        s.activities.add(new Activity(s.newId("act"), accountId, personId, type, note, at, user));
        if (accountId != null) {
            String label = type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT);
            s.timeline.add(new TimelineEvent(accountId, at, "activity", label + ": " + note, user));
        }
    }

    /**
     * Adds a timeline event that is not an activity.
     *
     * @param s         the store
     * @param accountId the account
     * @param at        when it happened
     * @param kind      event kind: status, candidate, proposal or job
     * @param text      description
     * @param user      who did it
     */
    private static void statusEvent(DemoStore s, String accountId, java.time.LocalDateTime at,
                                    String kind, String text, String user) {
        s.timeline.add(new TimelineEvent(accountId, at, kind, text, user));
    }

    /**
     * Adds a next action.
     *
     * @param s         the store
     * @param id        action id
     * @param title     what to do
     * @param due       due date
     * @param owner     who must do it
     * @param accountId related account or {@code null}
     * @param personId  related person or {@code null}
     * @param done      whether it is completed
     */
    private static void action(DemoStore s, String id, String title, LocalDate due, String owner,
                               String accountId, String personId, boolean done) {
        s.nextActions.put(id, new NextAction(id, title, due, owner, accountId, personId, done));
    }
}
