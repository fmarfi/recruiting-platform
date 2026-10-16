/* Search CRM demo — browser UI. Plain ES2020, no build step.
 * Talks to the Spring Boot API under /api (see demo/API.md). Hash routing:
 *   #/                 My day
 *   #/accounts         accounts list
 *   #/accounts/{id}    account page
 *   #/jobs/{id}        job order with pipeline board
 *   #/network          people list
 *   #/people/{id}      person page
 *   #/search?q=...     global search results
 */
(() => {
  'use strict';

  // ---------------------------------------------------------------------------
  // Dictionary (UI labels). Status labels come from GET /meta.
  // ---------------------------------------------------------------------------
  const DICT = {
    en: {
      skipToContent: 'Skip to content',
      navHome: 'My day', navAccounts: 'Accounts', navNetwork: 'Network',
      searchLabel: 'Search accounts, people and jobs',
      searchPlaceholder: 'Search accounts, people, jobs',
      demoNote: 'Demo — all companies and people are fictional.',
      language: 'Language',
      loading: 'Loading…',
      errNetwork: 'Could not reach the server. Check that the demo is running.',
      errGeneric: 'Something went wrong (HTTP {status}).',
      errLoadTitle: 'This page could not be loaded',
      notFoundTitle: 'Not found',
      notFoundText: 'This record does not exist or the link is wrong.',
      retry: 'Try again', backHome: 'Back to My day',
      goodMorning: 'Good morning, {name}', goodAfternoon: 'Good afternoon, {name}', goodEvening: 'Good evening, {name}',
      statActiveAccounts: 'Active accounts', statOpenJobs: 'Open jobs',
      statInProcess: 'Candidates in process', statPlacedYear: 'Placed this year',
      myNextActions: 'My next actions', myOpenJobs: 'My open jobs', recentAccounts: 'Recent accounts',
      noActionsTitle: 'You are all caught up',
      noActionsText: 'Add a next action from an account or person page to plan your follow-ups.',
      noOpenJobs: 'No open job orders assigned to you.',
      noRecentAccounts: 'No recent account activity yet.',
      overdue: 'Overdue', dueToday: 'Due today', dueTomorrow: 'Tomorrow', daysOverdue: '{n} days overdue', dayOverdue: '1 day overdue',
      markDone: 'Mark as done: {title}', actionDone: 'Done: {title}',
      viewAll: 'View all',
      daysOpenShort: '{n}d open', daysOpen: 'Days open',
      candidatesN: '{n} candidates', candidate1: '1 candidate',
      today: 'today', yesterday: 'yesterday', daysAgo: '{n} days ago', monthsAgo: '{n} months ago', monthAgo: '1 month ago',
      yearsAgo: '{n} years ago', yearAgo: '1 year ago', inDays: 'in {n} days', never: 'never',
      accountsTitle: 'Accounts', accountsSub: 'Client companies and where each relationship stands.',
      addClient: 'Add client', searchAccounts: 'Search by name or city',
      allStatuses: 'All', allSectors: 'All sectors', allOwners: 'All owners',
      colAccount: 'Account', colStatus: 'Status', colOwner: 'Owner', colOpenJobs: 'Open jobs', colLastActivity: 'Last activity',
      colPerson: 'Person', colType: 'Type', colCity: 'City', colLastContact: 'Last contact', colConsent: 'Consent',
      emptyAccountsTitle: 'Add your first client',
      emptyAccountsText: 'Start with the companies you are talking to. You can add contacts, job orders and proposals on each account.',
      noMatchTitle: 'Nothing matches these filters',
      noMatchText: 'Try a different search term or clear the filters.',
      clearFilters: 'Clear filters',
      clientName: 'Company name', sector: 'Sector', city: 'City', owner: 'Owner', chooseSector: 'Choose a sector',
      addClientCtx: 'New accounts start as Lead.',
      clientAdded: '{name} added as a lead',
      save: 'Save', cancel: 'Cancel', close: 'Close', add: 'Add', adding: 'Adding…', saving: 'Saving…',
      required: 'Please fill in {field}.',
      since: 'Since', sinceDate: 'Client since {date}',
      logActivity: 'Log activity', addNextAction: 'Add next action',
      dashOpenJobs: 'Open jobs', dashPerStage: 'Candidates per stage', dashAvgDays: 'Avg days open', dashNextAction: 'Next action',
      none: 'None', noneYet: 'None yet', days: 'days',
      tabJobs: 'Job orders', tabContacts: 'Contacts', tabProposals: 'Proposals',
      noJobs: 'No job orders for this account yet.',
      noContacts: 'No contacts linked to this account yet.',
      noProposals: 'No proposals for this account yet.',
      primary: 'Primary', timeline: 'Timeline', noTimeline: 'Nothing has happened on this account yet.',
      nextActions: 'Next actions', noOpenActions: 'No open actions.',
      kindStatus: 'Status change', kindActivity: 'Activity', kindProposal: 'Proposal', kindCandidate: 'Candidate', kindJob: 'Job order',
      changeStatus: 'Change status', statusChangeAria: 'Status: {status}. Change status',
      statusChanged: 'Status changed to {status}',
      jobClient: 'Client', location: 'Location', seniority: 'Seniority', fee: 'Fee', opened: 'Opened',
      addCandidate: 'Add candidate', pipeline: 'Pipeline',
      moveTo: 'Move to', moveAria: 'Move {name} to another stage',
      moved: '{name} moved to {stage}',
      dropHere: 'Drop here', noCandidatesCol: 'No candidates',
      boardHint: 'Drag a card to a highlighted column, or use the arrow button on a card.',
      finalStage: 'Final stage',
      rejectTitle: 'Reason for rejection', rejectCtx: '{name} will be moved to Rejected.',
      rejectReason: 'Reason', rejectPlaceholder: 'e.g. Salary expectations above budget',
      rejectSave: 'Reject candidate',
      reasonLabel: 'Reason',
      addCandidateTitle: 'Add a candidate to this job',
      filterPeople: 'Filter by name, title or company',
      noCandidatesToAdd: 'Everyone in the pool is already on this job.',
      noFilterMatch: 'No one matches this filter.',
      candidateAdded: '{name} added to the pipeline as Sourced',
      networkTitle: 'Network', networkSub: 'Executives, client contacts, referrers and candidates the firm knows.',
      searchPeople: 'Search by name, company or title',
      allTypes: 'All',
      emptyNetworkTitle: 'Your network is empty',
      emptyNetworkText: 'People appear here once they are added as contacts or candidates.',
      email: 'Email', howWeKnow: 'How we know them', lastContact: 'Last contact', consent: 'Consent',
      career: 'Career history', noCareer: 'No career history recorded.',
      connections: 'Connections', noConnections: 'No known connections.',
      contactAt: 'Contact at', noAccountsForPerson: 'Not a contact at any account.',
      pipelines: 'Pipelines', noPipelines: 'Not in any pipeline.',
      activityHistory: 'Activity history', noActivities: 'No activities logged yet.',
      updated: 'Updated {date}',
      at: '@',
      searchTitle: 'Search', searchResultsFor: 'Results for “{q}”',
      searchPrompt: 'Type in the search box above and press Enter.',
      searchNoneTitle: 'No results', searchNoneText: 'Nothing matched “{q}”. Try a company, a person or a job title.',
      groupAccounts: 'Accounts', groupPeople: 'People', groupJobs: 'Job orders',
      activityTitle: 'Log activity', activityType: 'Type', activityNote: 'Note',
      activityPlaceholder: 'What happened? Who did you talk to, what is next?',
      activityHint: 'Ctrl + Enter to save', activityLogged: '{type} logged',
      actionTitle: 'Add next action', actionWhat: 'What needs to happen?', actionPlaceholder: 'e.g. Call back about the shortlist',
      actionDue: 'Due date', actionAdded: 'Next action added',
      forCtx: 'For {name}',
      typeEXECUTIVE: 'Executive', typeCLIENT_CONTACT: 'Client contact', typeREFERRER: 'Referrer', typeCANDIDATE: 'Candidate',
      consentGIVEN: 'Consent given', consentSENT: 'Consent sent', consentNOT_SENT: 'Consent not sent',
      consentREFUSED: 'Consent refused', consentWITHDRAWN: 'Consent withdrawn',
      actCALL: 'Call', actVISIT: 'Visit', actMEETING: 'Meeting', actEMAIL: 'Email', actNOTE: 'Note',
      relKNOWS: 'Knows', relINTRODUCED: 'Introduced', relINTRODUCED_BY: 'Introduced by', relWORKED_WITH: 'Worked with',
      relREPORTS_TO: 'Reports to', relREFERRED: 'Referred', relREFERRED_BY: 'Referred by', relCOLLEAGUE: 'Colleague',
    },
    tr: {
      skipToContent: 'İçeriğe geç',
      navHome: 'Günüm', navAccounts: 'Müşteriler', navNetwork: 'Ağ',
      searchLabel: 'Müşteri, kişi ve iş ara',
      searchPlaceholder: 'Müşteri, kişi, iş ara',
      demoNote: 'Demo — tüm şirketler ve kişiler kurgusaldır.',
      language: 'Dil',
      loading: 'Yükleniyor…',
      errNetwork: 'Sunucuya ulaşılamadı. Demonun çalıştığını kontrol edin.',
      errGeneric: 'Bir sorun oluştu (HTTP {status}).',
      errLoadTitle: 'Bu sayfa yüklenemedi',
      notFoundTitle: 'Bulunamadı',
      notFoundText: 'Bu kayıt yok ya da bağlantı hatalı.',
      retry: 'Tekrar dene', backHome: 'Günüm sayfasına dön',
      goodMorning: 'Günaydın, {name}', goodAfternoon: 'İyi günler, {name}', goodEvening: 'İyi akşamlar, {name}',
      statActiveAccounts: 'Aktif müşteriler', statOpenJobs: 'Açık işler',
      statInProcess: 'Süreçteki adaylar', statPlacedYear: 'Bu yıl yerleşen',
      myNextActions: 'Sıradaki işlerim', myOpenJobs: 'Açık işlerim', recentAccounts: 'Son müşteriler',
      noActionsTitle: 'Bekleyen işiniz yok',
      noActionsText: 'Takiplerinizi planlamak için bir müşteri ya da kişi sayfasından sıradaki iş ekleyin.',
      noOpenJobs: 'Size atanmış açık iş yok.',
      noRecentAccounts: 'Henüz müşteri hareketi yok.',
      overdue: 'Gecikti', dueToday: 'Bugün', dueTomorrow: 'Yarın', daysOverdue: '{n} gün gecikti', dayOverdue: '1 gün gecikti',
      markDone: 'Tamamlandı olarak işaretle: {title}', actionDone: 'Tamamlandı: {title}',
      viewAll: 'Tümü',
      daysOpenShort: '{n} gün', daysOpen: 'Açık gün',
      candidatesN: '{n} aday', candidate1: '1 aday',
      today: 'bugün', yesterday: 'dün', daysAgo: '{n} gün önce', monthsAgo: '{n} ay önce', monthAgo: '1 ay önce',
      yearsAgo: '{n} yıl önce', yearAgo: '1 yıl önce', inDays: '{n} gün sonra', never: 'hiç',
      accountsTitle: 'Müşteriler', accountsSub: 'Müşteri şirketler ve her ilişkinin durumu.',
      addClient: 'Müşteri ekle', searchAccounts: 'Ad veya şehre göre ara',
      allStatuses: 'Tümü', allSectors: 'Tüm sektörler', allOwners: 'Tüm sorumlular',
      colAccount: 'Müşteri', colStatus: 'Durum', colOwner: 'Sorumlu', colOpenJobs: 'Açık iş', colLastActivity: 'Son hareket',
      colPerson: 'Kişi', colType: 'Tür', colCity: 'Şehir', colLastContact: 'Son temas', colConsent: 'Onay',
      emptyAccountsTitle: 'İlk müşterinizi ekleyin',
      emptyAccountsText: 'Görüştüğünüz şirketlerle başlayın. Her müşteriye kişiler, iş talepleri ve teklifler ekleyebilirsiniz.',
      noMatchTitle: 'Bu filtrelere uyan kayıt yok',
      noMatchText: 'Başka bir arama deneyin ya da filtreleri temizleyin.',
      clearFilters: 'Filtreleri temizle',
      clientName: 'Şirket adı', sector: 'Sektör', city: 'Şehir', owner: 'Sorumlu', chooseSector: 'Sektör seçin',
      addClientCtx: 'Yeni müşteriler Aday müşteri olarak başlar.',
      clientAdded: '{name} aday müşteri olarak eklendi',
      save: 'Kaydet', cancel: 'Vazgeç', close: 'Kapat', add: 'Ekle', adding: 'Ekleniyor…', saving: 'Kaydediliyor…',
      required: 'Lütfen {field} alanını doldurun.',
      since: 'Başlangıç', sinceDate: '{date} tarihinden beri müşteri',
      logActivity: 'Hareket ekle', addNextAction: 'Sıradaki iş ekle',
      dashOpenJobs: 'Açık işler', dashPerStage: 'Aşamaya göre adaylar', dashAvgDays: 'Ort. açık gün', dashNextAction: 'Sıradaki iş',
      none: 'Yok', noneYet: 'Henüz yok', days: 'gün',
      tabJobs: 'İş talepleri', tabContacts: 'Kişiler', tabProposals: 'Teklifler',
      noJobs: 'Bu müşteri için henüz iş talebi yok.',
      noContacts: 'Bu müşteriye bağlı kişi yok.',
      noProposals: 'Bu müşteri için henüz teklif yok.',
      primary: 'Birincil', timeline: 'Zaman akışı', noTimeline: 'Bu müşteride henüz bir hareket yok.',
      nextActions: 'Sıradaki işler', noOpenActions: 'Açık iş yok.',
      kindStatus: 'Durum değişikliği', kindActivity: 'Hareket', kindProposal: 'Teklif', kindCandidate: 'Aday', kindJob: 'İş talebi',
      changeStatus: 'Durumu değiştir', statusChangeAria: 'Durum: {status}. Durumu değiştir',
      statusChanged: 'Durum {status} olarak değişti',
      jobClient: 'Müşteri', location: 'Konum', seniority: 'Kıdem', fee: 'Ücret', opened: 'Açılış',
      addCandidate: 'Aday ekle', pipeline: 'Süreç',
      moveTo: 'Taşı', moveAria: '{name} adayını başka aşamaya taşı',
      moved: '{name} → {stage}',
      dropHere: 'Buraya bırakın', noCandidatesCol: 'Aday yok',
      boardHint: 'Kartı vurgulanan bir sütuna sürükleyin ya da karttaki ok düğmesini kullanın.',
      finalStage: 'Son aşama',
      rejectTitle: 'Eleme nedeni', rejectCtx: '{name} Elendi aşamasına taşınacak.',
      rejectReason: 'Neden', rejectPlaceholder: 'ör. Maaş beklentisi bütçenin üzerinde',
      rejectSave: 'Adayı ele',
      reasonLabel: 'Neden',
      addCandidateTitle: 'Bu işe aday ekle',
      filterPeople: 'Ad, unvan veya şirkete göre süz',
      noCandidatesToAdd: 'Havuzdaki herkes bu işte zaten var.',
      noFilterMatch: 'Bu filtreye uyan kimse yok.',
      candidateAdded: '{name} sürece Bulundu olarak eklendi',
      networkTitle: 'Ağ', networkSub: 'Firmanın tanıdığı yöneticiler, müşteri kişileri, referanslar ve adaylar.',
      searchPeople: 'Ad, şirket veya unvana göre ara',
      allTypes: 'Tümü',
      emptyNetworkTitle: 'Ağınız boş',
      emptyNetworkText: 'Kişiler, müşteri kişisi ya da aday olarak eklendiklerinde burada görünür.',
      email: 'E-posta', howWeKnow: 'Nereden tanıyoruz', lastContact: 'Son temas', consent: 'Onay',
      career: 'Kariyer geçmişi', noCareer: 'Kariyer geçmişi girilmemiş.',
      connections: 'Bağlantılar', noConnections: 'Bilinen bağlantı yok.',
      contactAt: 'Kişi olduğu müşteriler', noAccountsForPerson: 'Hiçbir müşteride kişi değil.',
      pipelines: 'Süreçler', noPipelines: 'Hiçbir süreçte değil.',
      activityHistory: 'Hareket geçmişi', noActivities: 'Henüz hareket yok.',
      updated: 'Güncellendi {date}',
      at: '@',
      searchTitle: 'Arama', searchResultsFor: '“{q}” için sonuçlar',
      searchPrompt: 'Yukarıdaki arama kutusuna yazıp Enter’a basın.',
      searchNoneTitle: 'Sonuç yok', searchNoneText: '“{q}” ile eşleşen kayıt yok. Bir şirket, kişi ya da iş unvanı deneyin.',
      groupAccounts: 'Müşteriler', groupPeople: 'Kişiler', groupJobs: 'İş talepleri',
      activityTitle: 'Hareket ekle', activityType: 'Tür', activityNote: 'Not',
      activityPlaceholder: 'Ne oldu? Kiminle konuştunuz, sırada ne var?',
      activityHint: 'Kaydetmek için Ctrl + Enter', activityLogged: '{type} kaydedildi',
      actionTitle: 'Sıradaki iş ekle', actionWhat: 'Ne yapılmalı?', actionPlaceholder: 'ör. Kısa liste için geri ara',
      actionDue: 'Son tarih', actionAdded: 'Sıradaki iş eklendi',
      forCtx: '{name} için',
      typeEXECUTIVE: 'Yönetici', typeCLIENT_CONTACT: 'Müşteri kişisi', typeREFERRER: 'Referans', typeCANDIDATE: 'Aday',
      consentGIVEN: 'Onay verildi', consentSENT: 'Onay gönderildi', consentNOT_SENT: 'Onay gönderilmedi',
      consentREFUSED: 'Onay reddedildi', consentWITHDRAWN: 'Onay geri çekildi',
      actCALL: 'Arama', actVISIT: 'Ziyaret', actMEETING: 'Toplantı', actEMAIL: 'E-posta', actNOTE: 'Not',
      relKNOWS: 'Tanıyor', relINTRODUCED: 'Tanıştırdı', relINTRODUCED_BY: 'Tanıştıran', relWORKED_WITH: 'Birlikte çalıştı',
      relREPORTS_TO: 'Bağlı olduğu kişi', relREFERRED: 'Önerdi', relREFERRED_BY: 'Öneren', relCOLLEAGUE: 'Meslektaş',
    },
  };

  const TONES = ['neutral', 'info', 'progress', 'good', 'warn', 'bad', 'muted'];
  const CONSENT_TONE = { GIVEN: 'good', SENT: 'info', NOT_SENT: 'neutral', REFUSED: 'bad', WITHDRAWN: 'bad' };
  const FALLBACK_ACTIVITY_TYPES = ['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'];
  const FALLBACK_PERSON_TYPES = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];

  // ---------------------------------------------------------------------------
  // Icons (inline SVG, stroke based)
  // ---------------------------------------------------------------------------
  const svg = (d, cls = 'icon') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true">${d}</svg>`;
  const ICON = {
    caret: svg('<path d="m7 10 5 5 5-5"/>', 'icon caret'),
    plus: svg('<path d="M12 5v14M5 12h14"/>'),
    search: svg('<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>'),
    move: svg('<path d="M5 12h14M13 6l6 6-6 6"/>', 'icon icon-sm'),
    check: svg('<path d="m5 12.5 4.5 4.5L19 7.5"/>'),
    alert: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5v5.5M12 16.5v.01"/>'),
    building: svg('<path d="M4 21V6a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v15M14 10h5a1 1 0 0 1 1 1v10M3 21h18M8 9h2M8 13h2M8 17h2"/>'),
    people: svg('<circle cx="9" cy="8" r="3.2"/><path d="M3.5 20a5.5 5.5 0 0 1 11 0M16 5.2a3 3 0 0 1 0 5.6M17.5 14.2A5.5 5.5 0 0 1 20.5 20"/>'),
    flag: svg('<path d="M5 21V4M5 4h11l-2 4 2 4H5"/>'),
    doc: svg('<path d="M7 3h7l4 4v14H7z"/><path d="M14 3v4h4M10 12h5M10 16h5"/>'),
    person: svg('<circle cx="12" cy="8" r="3.5"/><path d="M5 20a7 7 0 0 1 14 0"/>'),
    briefcase: svg('<rect x="3.5" y="7" width="17" height="12" rx="1.5"/><path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7M3.5 12.5h17"/>'),
    phone: svg('<path d="M6.5 3.5h3l1.5 4-2 1.3a10 10 0 0 0 6.2 6.2l1.3-2 4 1.5v3a2 2 0 0 1-2 2A15.5 15.5 0 0 1 4.5 5.5a2 2 0 0 1 2-2z"/>'),
    pin: svg('<path d="M12 21s-6.5-6-6.5-11a6.5 6.5 0 0 1 13 0c0 5-6.5 11-6.5 11z"/><circle cx="12" cy="10" r="2.3"/>'),
    meeting: svg('<circle cx="8" cy="9" r="2.6"/><circle cx="16" cy="9" r="2.6"/><path d="M3.5 18a4.5 4.5 0 0 1 9 0M11.5 18a4.5 4.5 0 0 1 9 0"/>'),
    mail: svg('<rect x="3.5" y="5.5" width="17" height="13" rx="1.5"/><path d="m4 7 8 6 8-6"/>'),
    note: svg('<path d="M15.5 4.5 19.5 8.5 9 19H5v-4z"/><path d="M13.5 6.5l4 4"/>'),
    calendar: svg('<rect x="4" y="5.5" width="16" height="14" rx="1.5"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4"/>'),
    link: svg('<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>'),
  };
  const KIND_ICON = { status: ICON.flag, activity: ICON.note, proposal: ICON.doc, candidate: ICON.person, job: ICON.briefcase };
  const ACT_ICON = { CALL: ICON.phone, VISIT: ICON.pin, MEETING: ICON.meeting, EMAIL: ICON.mail, NOTE: ICON.note };

  // ---------------------------------------------------------------------------
  // State
  // ---------------------------------------------------------------------------
  const S = {
    meta: null,
    lang: 'en',
    renderToken: 0,
    lastPath: null,
    accountTab: {},
    accFilter: { q: '', status: '', sector: '', owner: '' },
    netFilter: { q: '', type: '' },
  };

  // ---------------------------------------------------------------------------
  // Small helpers
  // ---------------------------------------------------------------------------
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  /** Escapes any value for safe use in HTML text and attribute values. */
  function esc(value) {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  /** Translates a key, filling {placeholders}. Values are NOT escaped; escape at the call site. */
  function t(key, vars) {
    const table = DICT[S.lang] || DICT.en;
    let s = table[key] !== undefined ? table[key] : (DICT.en[key] !== undefined ? DICT.en[key] : key);
    if (vars) s = s.replace(/\{(\w+)\}/g, (m, k) => (vars[k] !== undefined && vars[k] !== null ? String(vars[k]) : m));
    return s;
  }

  /** Translation of a key that may not exist; falls back to a humanised enum name. */
  function tOr(key, raw) {
    const v = (DICT[S.lang] || DICT.en)[key] || DICT.en[key];
    return v || humanize(raw);
  }

  function humanize(raw) {
    if (!raw) return '';
    const s = String(raw).replace(/_/g, ' ').toLowerCase();
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  const enc = encodeURIComponent;

  function qs(params) {
    const parts = Object.entries(params)
      .filter(([, v]) => v !== '' && v !== null && v !== undefined)
      .map(([k, v]) => `${enc(k)}=${enc(v)}`);
    return parts.length ? `?${parts.join('&')}` : '';
  }

  function initials(name) {
    if (!name) return '?';
    const parts = String(name).trim().split(/\s+/);
    const first = parts[0] ? parts[0][0] : '';
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase();
  }

  function plural(n, oneKey, manyKey) {
    return n === 1 ? t(oneKey) : t(manyKey, { n });
  }

  // ---- dates -----------------------------------------------------------------
  /** Parses ISO date or date-time ("2026-10-16" / "2026-10-16T09:30:00") as local time. */
  function parseDate(s) {
    if (!s) return null;
    const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(String(s));
    if (!m) return null;
    return new Date(+m[1], +m[2] - 1, +m[3], m[4] ? +m[4] : 0, m[5] ? +m[5] : 0, m[6] ? +m[6] : 0);
  }
  function dayOnly(d) { return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }
  function todayDate() { return parseDate(S.meta && S.meta.today) || dayOnly(new Date()); }
  /** Whole days from a to b (b - a), ignoring time of day. */
  function dayDiff(a, b) { return Math.round((dayOnly(b) - dayOnly(a)) / 86400000); }
  function locale() { return S.lang === 'tr' ? 'tr-TR' : 'en-GB'; }

  function fmtDate(s) {
    const d = parseDate(s);
    if (!d) return '';
    const sameYear = d.getFullYear() === todayDate().getFullYear();
    const opts = sameYear ? { day: 'numeric', month: 'short' } : { day: 'numeric', month: 'short', year: 'numeric' };
    return new Intl.DateTimeFormat(locale(), opts).format(d);
  }
  function fmtDateTime(s) {
    const d = parseDate(s);
    if (!d) return '';
    const hasTime = /T|\s\d/.test(String(s));
    const date = fmtDate(s);
    if (!hasTime) return date;
    const time = new Intl.DateTimeFormat(locale(), { hour: '2-digit', minute: '2-digit' }).format(d);
    return `${date}, ${time}`;
  }
  function fmtLongDate(s) {
    const d = parseDate(s) || new Date();
    return new Intl.DateTimeFormat(locale(), { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(d);
  }
  /** "today", "12 days ago", "3 months ago" relative to meta.today. */
  function relDate(s) {
    const d = parseDate(s);
    if (!d) return t('never');
    const n = dayDiff(d, todayDate());
    if (n < 0) return t('inDays', { n: -n });
    if (n === 0) return t('today');
    if (n === 1) return t('yesterday');
    if (n < 45) return t('daysAgo', { n });
    if (n < 365) { const mo = Math.round(n / 30); return mo === 1 ? t('monthAgo') : t('monthsAgo', { n: mo }); }
    const y = Math.round(n / 365);
    return y === 1 ? t('yearAgo') : t('yearsAgo', { n: y });
  }
  function dueLabel(s) {
    const d = parseDate(s);
    if (!d) return '';
    const n = dayDiff(todayDate(), d);
    if (n === 0) return t('dueToday');
    if (n === 1) return t('dueTomorrow');
    if (n === -1) return t('dayOverdue');
    if (n < -1) return t('daysOverdue', { n: -n });
    return fmtDate(s);
  }
  function isoAddDays(baseIso, n) {
    const d = parseDate(baseIso) || new Date();
    d.setDate(d.getDate() + n);
    const p = (x) => String(x).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  }

  // ---- meta lookups ------------------------------------------------------------
  function lifecycle(kind, status) {
    const lc = S.meta && S.meta.lifecycles && S.meta.lifecycles[kind];
    return lc && lc[status] ? lc[status] : null;
  }
  function statusLabel(kind, status) {
    const lc = lifecycle(kind, status);
    if (!lc) return humanize(status);
    return lc[S.lang] || lc.en || humanize(status);
  }
  function statusTone(kind, status) {
    const lc = lifecycle(kind, status);
    return lc && TONES.includes(lc.tone) ? lc.tone : 'neutral';
  }
  function nextStatuses(kind, status) {
    const lc = lifecycle(kind, status);
    return lc && Array.isArray(lc.next) ? lc.next : [];
  }
  function stageOrder() {
    if (S.meta && Array.isArray(S.meta.stageOrder) && S.meta.stageOrder.length) return S.meta.stageOrder;
    return Object.keys((S.meta && S.meta.lifecycles && S.meta.lifecycles.stage) || {});
  }
  const activityTypes = () => (S.meta && S.meta.activityTypes && S.meta.activityTypes.length ? S.meta.activityTypes : FALLBACK_ACTIVITY_TYPES);
  const personTypes = () => (S.meta && S.meta.personTypes && S.meta.personTypes.length ? S.meta.personTypes : FALLBACK_PERSON_TYPES);
  const typeLabel = (type) => tOr(`type${type}`, type);
  const activityLabel = (type) => tOr(`act${type}`, type);
  const relationLabel = (kind) => tOr(`rel${String(kind || '').trim().toUpperCase().replace(/\s+/g, '_')}`, kind);
  const consentLabel = (c) => tOr(`consent${c}`, c);

  // ---------------------------------------------------------------------------
  // Rendering fragments
  // ---------------------------------------------------------------------------
  /** Status badge. With opts.editable + opts.id it becomes a button that opens the next-status menu. */
  function badge(kind, status, opts = {}) {
    if (!status) return '';
    const label = statusLabel(kind, status);
    const tone = statusTone(kind, status);
    const size = opts.size ? ` badge-${opts.size}` : '';
    const cls = `badge tone-${tone}${size}`;
    if (opts.editable && opts.id && nextStatuses(kind, status).length) {
      return `<button type="button" class="${cls} is-editable" data-badge="${esc(kind)}" data-id="${esc(opts.id)}"`
        + ` data-status="${esc(status)}" data-name="${esc(opts.name || '')}" aria-haspopup="menu"`
        + ` aria-label="${esc(t('statusChangeAria', { status: label }))}">${esc(label)}${ICON.caret}</button>`;
    }
    return `<span class="${cls}">${esc(label)}</span>`;
  }

  function consentBadge(c, size = 'sm') {
    if (!c) return '';
    const tone = CONSENT_TONE[c] || 'neutral';
    return `<span class="badge consent badge-${size} tone-${tone}" title="${esc(t('consent'))}">${esc(consentLabel(c))}</span>`;
  }

  function avatar(name, cls = '') {
    return `<span class="avatar ${cls}" aria-hidden="true">${esc(initials(name))}</span>`;
  }

  /** Mini stacked bar of active candidates per stage (REJECTED excluded). */
  function stageBar(counts) {
    const c = counts || {};
    const stages = stageOrder().filter((s) => s !== 'REJECTED');
    const total = stages.reduce((sum, s) => sum + (Number(c[s]) || 0), 0);
    const summary = stages.filter((s) => c[s]).map((s) => `${statusLabel('stage', s)}: ${c[s]}`).join(', ');
    const segs = total
      ? stages.filter((s) => c[s]).map((s) => {
        const w = ((Number(c[s]) || 0) / total) * 100;
        return `<span class="seg-${statusTone('stage', s)}" style="width:${w.toFixed(2)}%" title="${esc(`${statusLabel('stage', s)}: ${c[s]}`)}"></span>`;
      }).join('')
      : '';
    return `<span class="stagebar" role="img" aria-label="${esc(summary || t('none'))}">${segs}</span>`;
  }

  function personRole(title, company) {
    if (title && company) return `${esc(title)} <span class="muted">${esc(t('at'))}</span> ${esc(company)}`;
    return esc(title || company || '');
  }

  function actionList(actions, opts = {}) {
    if (!actions || !actions.length) {
      return opts.emptyHtml || `<p class="empty-inline">${esc(t('noOpenActions'))}</p>`;
    }
    return `<ul class="list">${actions.map((a) => {
      const links = [];
      if (opts.links !== false) {
        if (a.accountId && a.accountId !== opts.skipAccount) links.push(`<a href="#/accounts/${enc(a.accountId)}">${esc(a.accountName || '')}</a>`);
        if (a.personId && a.personId !== opts.skipPerson) links.push(`<a href="#/people/${enc(a.personId)}">${esc(a.personName || '')}</a>`);
      }
      return `<li class="row action${a.overdue ? ' is-overdue' : ''}">
        <input type="checkbox" class="check" data-done="${esc(a.id)}" data-title="${esc(a.title)}" aria-label="${esc(t('markDone', { title: a.title }))}">
        <div class="row-main">
          <div class="row-title">${esc(a.title)}${a.overdue ? `<span class="overdue-flag">${esc(t('overdue'))}</span>` : ''}</div>
          ${links.length ? `<div class="action-links">${links.join('')}</div>` : ''}
        </div>
        <span class="due" title="${esc(fmtDate(a.due))}">${esc(dueLabel(a.due))}</span>
      </li>`;
    }).join('')}</ul>`;
  }

  function jobRow(j, opts = {}) {
    const sub = [];
    if (opts.showClient && j.accountName) sub.push(`<a href="#/accounts/${enc(j.accountId)}">${esc(j.accountName)}</a>`);
    if (j.location) sub.push(esc(j.location));
    if (opts.showCandidates) sub.push(esc(plural(j.candidates || 0, 'candidate1', 'candidatesN')));
    return `<li class="row job-row">
      <div class="row-main">
        <a class="row-title" href="#/jobs/${enc(j.id)}">${esc(j.title)}</a>
        <div class="row-sub">${sub.join('<span class="dot-sep"></span>')}</div>
      </div>
      <div class="row-meta">
        ${stageBar(j.stageCounts)}
        <span class="days" title="${esc(t('daysOpen'))}">${esc(t('daysOpenShort', { n: j.daysOpen }))}</span>
        ${badge('job', j.status, { editable: !!opts.editable, id: j.id, size: 'sm' })}
      </div>
    </li>`;
  }

  function accountRow(a) {
    const sub = [a.sector, a.city].filter(Boolean).map(esc).join('<span class="dot-sep"></span>');
    return `<li class="row">
      <div class="row-main">
        <a class="row-title" href="#/accounts/${enc(a.id)}">${esc(a.name)}</a>
        <div class="row-sub">${sub}</div>
      </div>
      <div class="row-meta">
        <span class="days" title="${esc(t('colLastActivity'))}">${esc(a.lastActivity ? relDate(a.lastActivity) : '')}</span>
        ${badge('account', a.status, { size: 'sm' })}
      </div>
    </li>`;
  }

  function personRow(p) {
    return `<li class="row">
      ${avatar(p.name)}
      <div class="row-main">
        <a class="row-title" href="#/people/${enc(p.id)}">${esc(p.name)}</a>
        <div class="row-sub">${personRole(p.title, p.company)}</div>
      </div>
      <div class="row-meta">
        <span class="tag">${esc(typeLabel(p.type))}</span>
        ${consentBadge(p.consent)}
      </div>
    </li>`;
  }

  function emptyState({ icon = ICON.building, title, text, actions = '' }) {
    return `<div class="empty">
      <div class="empty-mark">${icon}</div>
      <h3>${esc(title)}</h3>
      ${text ? `<p>${esc(text)}</p>` : ''}
      ${actions ? `<div class="btn-row">${actions}</div>` : ''}
    </div>`;
  }

  function loadingHtml() {
    return `<div class="loading" role="status"><span class="spinner" aria-hidden="true"></span>${esc(t('loading'))}</div>
      <div class="skeleton" aria-hidden="true"><div></div><div></div><div></div></div>`;
  }

  function errorHtml(err) {
    const nf = err && err.status === 404;
    return `<div class="card">${emptyState({
      icon: ICON.alert,
      title: nf ? t('notFoundTitle') : t('errLoadTitle'),
      text: nf ? t('notFoundText') : (err && err.message) || '',
      actions: `<button type="button" class="btn" data-retry>${esc(t('retry'))}</button><a class="btn btn-ghost" href="#/">${esc(t('backHome'))}</a>`,
    })}</div>`;
  }

  // ---------------------------------------------------------------------------
  // API
  // ---------------------------------------------------------------------------
  class ApiError extends Error {
    constructor(message, status) { super(message); this.status = status; }
  }

  /** Fetches /api{path}. Throws ApiError carrying the ProblemDetail `detail` on 4xx/5xx. */
  async function api(path, { method = 'GET', body } = {}) {
    const headers = { Accept: 'application/json' };
    const init = { method, headers };
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json';
      init.body = JSON.stringify(body);
    }
    let res;
    try {
      res = await fetch(`/api${path}`, init);
    } catch (e) {
      throw new ApiError(t('errNetwork'), 0);
    }
    if (!res.ok) {
      let detail = '';
      try {
        const problem = await res.json();
        detail = (problem && (problem.detail || problem.title)) || '';
      } catch (e) { /* not JSON */ }
      throw new ApiError(detail || t('errGeneric', { status: res.status }), res.status);
    }
    if (res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
  }

  // ---------------------------------------------------------------------------
  // Toasts
  // ---------------------------------------------------------------------------
  function toast(message, kind = 'ok') {
    const host = $('#toasts');
    const el = document.createElement('div');
    el.className = `toast${kind === 'error' ? ' is-error' : ''}`;
    if (kind === 'error') el.setAttribute('role', 'alert');
    el.innerHTML = `${kind === 'error' ? ICON.alert : ICON.check}<span>${esc(message)}</span>`;
    host.appendChild(el);
    const ttl = kind === 'error' ? 6000 : 3200;
    setTimeout(() => {
      el.classList.add('is-leaving');
      setTimeout(() => el.remove(), 220);
    }, ttl);
  }
  const toastError = (err) => toast((err && err.message) || t('errGeneric', { status: '?' }), 'error');

  // ---------------------------------------------------------------------------
  // Popover menu (status changes, stage moves)
  // ---------------------------------------------------------------------------
  let openMenuState = null;

  function closeMenu(restoreFocus) {
    if (!openMenuState) return;
    const { el, anchor, cleanup } = openMenuState;
    openMenuState = null;
    cleanup();
    el.remove();
    if (anchor) anchor.setAttribute('aria-expanded', 'false');
    if (restoreFocus && anchor && document.contains(anchor)) anchor.focus();
  }

  /** items: [{ html, label, onSelect }] */
  function openMenu(anchor, { title, items }) {
    closeMenu();
    const el = document.createElement('div');
    el.className = 'menu';
    el.setAttribute('role', 'menu');
    el.innerHTML = (title ? `<div class="menu-title">${esc(title)}</div>` : '')
      + items.map((it, i) => `<button type="button" role="menuitem" data-i="${i}" aria-label="${esc(it.label || '')}">${it.html}</button>`).join('');
    document.body.appendChild(el);

    const r = anchor.getBoundingClientRect();
    const w = el.offsetWidth;
    const h = el.offsetHeight;
    let left = r.left;
    let top = r.bottom + 4;
    if (left + w > window.innerWidth - 8) left = Math.max(8, window.innerWidth - 8 - w);
    if (top + h > window.innerHeight - 8) top = Math.max(8, r.top - h - 4);
    el.style.left = `${left}px`;
    el.style.top = `${top}px`;
    anchor.setAttribute('aria-expanded', 'true');

    const buttons = $$('button', el);
    const onClick = (e) => {
      const b = e.target.closest('button[data-i]');
      if (!b) return;
      const item = items[Number(b.dataset.i)];
      closeMenu(true);
      if (item && item.onSelect) item.onSelect();
    };
    const onKey = (e) => {
      const idx = buttons.indexOf(document.activeElement);
      if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); closeMenu(true); }
      else if (e.key === 'ArrowDown') { e.preventDefault(); buttons[(idx + 1) % buttons.length].focus(); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); buttons[(idx - 1 + buttons.length) % buttons.length].focus(); }
      else if (e.key === 'Tab') { closeMenu(false); }
    };
    const onOutside = (e) => { if (!el.contains(e.target) && !anchor.contains(e.target)) closeMenu(false); };
    const onScroll = (e) => { if (!el.contains(e.target)) closeMenu(false); };
    const onResize = () => closeMenu(false);
    el.addEventListener('click', onClick);
    el.addEventListener('keydown', onKey);
    document.addEventListener('pointerdown', onOutside, true);
    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onResize);
    openMenuState = {
      el, anchor,
      cleanup: () => {
        document.removeEventListener('pointerdown', onOutside, true);
        window.removeEventListener('scroll', onScroll, true);
        window.removeEventListener('resize', onResize);
      },
    };
    if (buttons[0]) buttons[0].focus();
  }

  // ---------------------------------------------------------------------------
  // Modal dialog
  // ---------------------------------------------------------------------------
  let openModalState = null;

  function closeModal() {
    if (!openModalState) return;
    const { root, returnFocus, onKey } = openModalState;
    openModalState = null;
    document.removeEventListener('keydown', onKey, true);
    root.remove();
    document.body.style.overflow = '';
    if (returnFocus && document.contains(returnFocus)) returnFocus.focus();
  }

  /**
   * Opens a modal form.
   * onSubmit(form, ui) may be async; return false to keep the dialog open.
   * submitLabel null = no submit button (e.g. pick lists).
   */
  function openModal({ title, ctx, body, submitLabel, submitBusy, onSubmit, wide, hint, onOpen }) {
    closeMenu();
    closeModal();
    const returnFocus = document.activeElement;
    const root = document.createElement('div');
    root.className = 'modal-backdrop';
    const titleId = `m-title-${Date.now()}`;
    root.innerHTML = `<form class="modal${wide ? ' wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="${titleId}" novalidate>
      <div class="modal-head">
        <div>
          <h2 id="${titleId}">${esc(title)}</h2>
          ${ctx ? `<div class="ctx">${esc(ctx)}</div>` : ''}
        </div>
        <button type="button" class="btn btn-ghost btn-icon" data-close aria-label="${esc(t('close'))}">${svg('<path d="M6 6l12 12M18 6 6 18"/>')}</button>
      </div>
      <div class="modal-body">${body}</div>
      <div class="modal-foot">
        ${hint ? `<span class="hint">${esc(hint)}</span>` : ''}
        <button type="button" class="btn" data-close>${esc(submitLabel === null ? t('close') : t('cancel'))}</button>
        ${submitLabel === null ? '' : `<button type="submit" class="btn btn-primary">${esc(submitLabel || t('save'))}</button>`}
      </div>
    </form>`;
    document.body.appendChild(root);
    document.body.style.overflow = 'hidden';
    const form = $('form', root);
    const submitBtn = $('button[type="submit"]', form);
    let busy = false;

    const submit = async () => {
      if (busy || !onSubmit) return;
      busy = true;
      const label = submitBtn ? submitBtn.textContent : '';
      if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = submitBusy || t('saving'); }
      let keepOpen = false;
      try {
        keepOpen = (await onSubmit(form)) === false;
      } catch (err) {
        keepOpen = true;
        toastError(err);
      }
      busy = false;
      if (keepOpen && document.contains(form)) {
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = label; }
      } else {
        closeModal();
      }
    };

    form.addEventListener('submit', (e) => { e.preventDefault(); submit(); });
    root.addEventListener('click', (e) => {
      if (e.target === root || e.target.closest('[data-close]')) closeModal();
    });

    const onKey = (e) => {
      if (!openModalState || openModalState.root !== root) return;
      if (e.key === 'Escape' && !openMenuState) { e.preventDefault(); closeModal(); return; }
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey) && submitBtn) { e.preventDefault(); submit(); return; }
      if (e.key === 'Tab') {
        const f = $$('a[href], button:not([disabled]), input:not([disabled]), select, textarea, [tabindex]:not([tabindex="-1"])', form)
          .filter((x) => x.offsetParent !== null);
        if (!f.length) return;
        const first = f[0];
        const last = f[f.length - 1];
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
      }
    };
    document.addEventListener('keydown', onKey, true);
    openModalState = { root, returnFocus, onKey };

    const focusTarget = $('[autofocus]', form) || $('input, textarea, select', form) || submitBtn;
    if (focusTarget) setTimeout(() => focusTarget.focus(), 0);
    if (onOpen) onOpen(form);
    return { root, form, close: closeModal };
  }

  /** Reads a required trimmed value; toasts and focuses when empty. */
  function requireField(form, name, label) {
    const el = form.elements[name];
    const v = el ? String(el.value || '').trim() : '';
    if (!v) {
      toast(t('required', { field: label }), 'error');
      if (el) el.focus();
      return null;
    }
    return v;
  }

  // ---------------------------------------------------------------------------
  // Mutations (status changes, stage moves, actions, activities)
  // ---------------------------------------------------------------------------
  const STATUS_PATH = {
    account: (id) => `/accounts/${enc(id)}/status`,
    job: (id) => `/jobs/${enc(id)}/status`,
    proposal: (id) => `/proposals/${enc(id)}/status`,
  };

  async function changeStatus(kind, id, status) {
    try {
      await api(STATUS_PATH[kind](id), { method: 'PATCH', body: { status } });
      toast(t('statusChanged', { status: statusLabel(kind, status) }));
    } catch (err) {
      toastError(err);
    }
    refresh();
  }

  /** Moves an application to a stage; REJECTED asks for a reason first. */
  function moveStage(app, stage) {
    if (stage === 'REJECTED') {
      openModal({
        title: t('rejectTitle'),
        ctx: t('rejectCtx', { name: app.name }),
        submitLabel: t('rejectSave'),
        body: `<div class="field">
          <label for="reject-reason">${esc(t('rejectReason'))}</label>
          <textarea id="reject-reason" name="reason" class="textarea" required autofocus placeholder="${esc(t('rejectPlaceholder'))}"></textarea>
        </div>`,
        onSubmit: async (form) => {
          const reason = requireField(form, 'reason', t('rejectReason'));
          if (!reason) return false;
          await api(`/applications/${enc(app.id)}/stage`, { method: 'PATCH', body: { stage, reason } });
          toast(t('moved', { name: app.name, stage: statusLabel('stage', stage) }));
          refresh();
          return true;
        },
      });
      return;
    }
    api(`/applications/${enc(app.id)}/stage`, { method: 'PATCH', body: { stage } })
      .then(() => toast(t('moved', { name: app.name, stage: statusLabel('stage', stage) })))
      .catch(toastError)
      .finally(refresh);
  }

  function openStatusMenu(btn) {
    const kind = btn.dataset.badge;
    const id = btn.dataset.id;
    const current = btn.dataset.status;
    const next = nextStatuses(kind, current);
    if (!next.length) return;
    openMenu(btn, {
      title: kind === 'stage' ? t('moveTo') : t('changeStatus'),
      items: next.map((s) => ({
        label: statusLabel(kind, s),
        html: badge(kind, s, { size: 'sm' }),
        onSelect: () => {
          if (kind === 'stage') moveStage({ id, name: btn.dataset.name || '' }, s);
          else changeStatus(kind, id, s);
        },
      })),
    });
  }

  async function completeAction(checkbox) {
    const id = checkbox.dataset.done;
    checkbox.disabled = true;
    try {
      await api(`/actions/${enc(id)}/done`, { method: 'POST' });
      toast(t('actionDone', { title: checkbox.dataset.title || '' }));
      const li = checkbox.closest('li');
      if (li) li.style.opacity = '.45';
    } catch (err) {
      checkbox.checked = false;
      checkbox.disabled = false;
      toastError(err);
      return;
    }
    refresh();
  }

  /** Quick add activity (UX-06): type chips + autofocused note. */
  function openQuickActivity({ accountId, personId, contextName }) {
    const types = activityTypes();
    let selected = types.includes('CALL') ? 'CALL' : types[0];
    const chips = types.map((ty) => `<button type="button" class="chip" role="radio" data-type="${esc(ty)}"
        aria-checked="${ty === selected}" tabindex="${ty === selected ? 0 : -1}">${ACT_ICON[ty] || ICON.note}${esc(activityLabel(ty))}</button>`).join('');
    openModal({
      title: t('activityTitle'),
      ctx: contextName ? t('forCtx', { name: contextName }) : '',
      hint: t('activityHint'),
      body: `<div class="field">
          <span class="label" id="act-type-label">${esc(t('activityType'))}</span>
          <div class="chips" role="radiogroup" aria-labelledby="act-type-label">${chips}</div>
        </div>
        <div class="field">
          <label for="act-note">${esc(t('activityNote'))}</label>
          <textarea id="act-note" name="note" class="textarea" rows="4" autofocus placeholder="${esc(t('activityPlaceholder'))}"></textarea>
        </div>`,
      onOpen: (form) => {
        const group = $('[role="radiogroup"]', form);
        const all = $$('[role="radio"]', group);
        const select = (b, focus) => {
          selected = b.dataset.type;
          all.forEach((x) => { x.setAttribute('aria-checked', String(x === b)); x.tabIndex = x === b ? 0 : -1; });
          if (focus) b.focus();
        };
        group.addEventListener('click', (e) => { const b = e.target.closest('[role="radio"]'); if (b) select(b, false); });
        group.addEventListener('keydown', (e) => {
          const i = all.indexOf(document.activeElement);
          if (i < 0) return;
          if (e.key === 'ArrowRight' || e.key === 'ArrowDown') { e.preventDefault(); select(all[(i + 1) % all.length], true); }
          if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') { e.preventDefault(); select(all[(i - 1 + all.length) % all.length], true); }
        });
      },
      onSubmit: async (form) => {
        const note = requireField(form, 'note', t('activityNote'));
        if (!note) return false;
        const body = { type: selected, note };
        if (accountId) body.accountId = accountId;
        if (personId) body.personId = personId;
        await api('/activities', { method: 'POST', body });
        toast(t('activityLogged', { type: activityLabel(selected) }));
        refresh();
        return true;
      },
    });
  }

  function openAddAction({ accountId, personId, contextName }) {
    const due = isoAddDays(S.meta && S.meta.today, 3);
    openModal({
      title: t('actionTitle'),
      ctx: contextName ? t('forCtx', { name: contextName }) : '',
      body: `<div class="field">
          <label for="na-title">${esc(t('actionWhat'))}</label>
          <input id="na-title" name="title" class="input" autocomplete="off" required autofocus placeholder="${esc(t('actionPlaceholder'))}">
        </div>
        <div class="field">
          <label for="na-due">${esc(t('actionDue'))}</label>
          <input id="na-due" name="due" type="date" class="input" required value="${esc(due)}">
        </div>`,
      onSubmit: async (form) => {
        const title = requireField(form, 'title', t('actionWhat'));
        if (!title) return false;
        const dueVal = requireField(form, 'due', t('actionDue'));
        if (!dueVal) return false;
        const body = { title, due: dueVal };
        if (accountId) body.accountId = accountId;
        if (personId) body.personId = personId;
        await api('/actions', { method: 'POST', body });
        toast(t('actionAdded'));
        refresh();
        return true;
      },
    });
  }

  function openAddClient() {
    const sectors = (S.meta && S.meta.sectors) || [];
    const users = (S.meta && S.meta.users) || [];
    const me = S.meta && S.meta.currentUser;
    openModal({
      title: t('addClient'),
      ctx: t('addClientCtx'),
      submitLabel: t('addClient'),
      submitBusy: t('adding'),
      body: `<div class="field">
          <label for="ac-name">${esc(t('clientName'))}</label>
          <input id="ac-name" name="name" class="input" autocomplete="off" required autofocus>
        </div>
        <div class="field-row" style="margin-top:14px">
          <div class="field">
            <label for="ac-sector">${esc(t('sector'))}</label>
            <select id="ac-sector" name="sector" class="select" required>
              <option value="">${esc(t('chooseSector'))}</option>
              ${sectors.map((s) => `<option value="${esc(s)}">${esc(s)}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label for="ac-city">${esc(t('city'))}</label>
            <input id="ac-city" name="city" class="input" autocomplete="off">
          </div>
        </div>
        ${users.length ? `<div class="field">
          <label for="ac-owner">${esc(t('owner'))}</label>
          <select id="ac-owner" name="owner" class="select">
            ${users.map((u) => `<option value="${esc(u)}"${u === me ? ' selected' : ''}>${esc(u)}</option>`).join('')}
          </select>
        </div>` : ''}`,
      onSubmit: async (form) => {
        const name = requireField(form, 'name', t('clientName'));
        if (!name) return false;
        const sector = requireField(form, 'sector', t('sector'));
        if (!sector) return false;
        const body = { name, sector, city: String(form.elements.city.value || '').trim() };
        if (form.elements.owner && form.elements.owner.value) body.owner = form.elements.owner.value;
        const created = await api('/accounts', { method: 'POST', body });
        toast(t('clientAdded', { name: created.name || name }));
        location.hash = `#/accounts/${enc(created.id)}`;
        return true;
      },
    });
  }

  function openAddCandidate(jobId, jobTitle) {
    let people = [];
    const renderPick = (form) => {
      const host = $('#pick', form);
      const q = String(form.elements.filter.value || '').trim().toLowerCase();
      const list = q
        ? people.filter((p) => [p.name, p.title, p.company, p.city].some((x) => x && String(x).toLowerCase().includes(q)))
        : people;
      if (!people.length) { host.innerHTML = `<p class="empty-inline">${esc(t('noCandidatesToAdd'))}</p>`; return; }
      if (!list.length) { host.innerHTML = `<p class="empty-inline">${esc(t('noFilterMatch'))}</p>`; return; }
      host.innerHTML = `<ul class="list">${list.map((p) => `<li class="row">
        ${avatar(p.name)}
        <div class="row-main">
          <div class="row-title">${esc(p.name)}</div>
          <div class="row-sub">${personRole(p.title, p.company)}${p.city ? `<span class="dot-sep"></span>${esc(p.city)}` : ''}</div>
        </div>
        ${consentBadge(p.consent)}
        <button type="button" class="btn btn-sm" data-add="${esc(p.id)}" data-name="${esc(p.name)}">${ICON.plus}${esc(t('add'))}</button>
      </li>`).join('')}</ul>`;
    };
    openModal({
      title: t('addCandidateTitle'),
      ctx: jobTitle || '',
      wide: true,
      submitLabel: null,
      body: `<div class="field">
          <label class="sr-only" for="pick-filter">${esc(t('filterPeople'))}</label>
          <input id="pick-filter" name="filter" type="search" class="input" autocomplete="off" autofocus placeholder="${esc(t('filterPeople'))}">
        </div>
        <div id="pick" class="pick-list"><div class="loading" role="status" style="padding:16px"><span class="spinner" aria-hidden="true"></span>${esc(t('loading'))}</div></div>`,
      onOpen: (form) => {
        form.elements.filter.addEventListener('input', () => renderPick(form));
        form.elements.filter.addEventListener('keydown', (e) => { if (e.key === 'Enter') e.preventDefault(); });
        $('#pick', form).addEventListener('click', async (e) => {
          const b = e.target.closest('button[data-add]');
          if (!b) return;
          b.disabled = true;
          b.textContent = t('adding');
          try {
            await api(`/jobs/${enc(jobId)}/applications`, { method: 'POST', body: { personId: b.dataset.add } });
            toast(t('candidateAdded', { name: b.dataset.name }));
            closeModal();
            refresh();
          } catch (err) {
            b.disabled = false;
            b.innerHTML = `${ICON.plus}${esc(t('add'))}`;
            toastError(err);
          }
        });
        api(`/jobs/${enc(jobId)}/candidates-to-add`)
          .then((list) => { people = list || []; if (document.contains(form)) renderPick(form); })
          .catch((err) => {
            if (document.contains(form)) $('#pick', form).innerHTML = `<p class="empty-inline">${esc(err.message)}</p>`;
          });
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Views
  // ---------------------------------------------------------------------------

  // ---- My day ------------------------------------------------------------------
  async function viewHome(view, ctx) {
    const d = await api('/home');
    if (!ctx.alive()) return;
    const hour = new Date().getHours();
    const greetKey = hour < 12 ? 'goodMorning' : hour < 18 ? 'goodAfternoon' : 'goodEvening';
    const firstName = String(d.user || (S.meta && S.meta.currentUser) || '').split(/\s+/)[0];
    const st = d.stats || {};
    const tile = (label, value) => `<div class="card stat"><div class="label">${esc(label)}</div><div class="value">${esc(value ?? 0)}</div></div>`;
    const actions = d.actions || [];
    const overdue = actions.filter((a) => a.overdue).length;
    const jobs = d.openJobs || [];
    const recent = d.recentAccounts || [];

    view.innerHTML = `
      <div class="page-head greeting">
        <div>
          <h1>${esc(t(greetKey, { name: firstName }))}</h1>
          <p class="date">${esc(fmtLongDate(d.today || (S.meta && S.meta.today)))}</p>
        </div>
      </div>
      <div class="stats">
        ${tile(t('statActiveAccounts'), st.activeAccounts)}
        ${tile(t('statOpenJobs'), st.openJobs)}
        ${tile(t('statInProcess'), st.candidatesInProcess)}
        ${tile(t('statPlacedYear'), st.placedThisYear)}
      </div>
      <div class="home-grid">
        <div>
          <section class="section">
            <div class="section-head">
              <h2>${esc(t('myNextActions'))} <span class="count">${actions.length}</span>
                ${overdue ? `<span class="overdue-flag">${esc(overdue)} ${esc(t('overdue'))}</span>` : ''}</h2>
            </div>
            <div class="card">${actionList(actions, {
              emptyHtml: emptyState({ icon: ICON.check, title: t('noActionsTitle'), text: t('noActionsText') }),
            })}</div>
          </section>
          <section class="section">
            <div class="section-head">
              <h2>${esc(t('myOpenJobs'))} <span class="count">${jobs.length}</span></h2>
            </div>
            <div class="card">${jobs.length
              ? `<ul class="list">${jobs.map((j) => jobRow(j, { showClient: true })).join('')}</ul>`
              : `<p class="empty-inline">${esc(t('noOpenJobs'))}</p>`}</div>
          </section>
        </div>
        <div>
          <section class="section">
            <div class="section-head">
              <h2>${esc(t('recentAccounts'))}</h2>
              <a href="#/accounts" class="muted">${esc(t('viewAll'))}</a>
            </div>
            <div class="card">${recent.length
              ? `<ul class="list">${recent.map(accountRow).join('')}</ul>`
              : emptyState({
                title: t('emptyAccountsTitle'), text: t('emptyAccountsText'),
                actions: `<button type="button" class="btn btn-primary" data-add-client>${ICON.plus}${esc(t('addClient'))}</button>`,
              })}</div>
          </section>
        </div>
      </div>`;
  }

  // ---- Accounts list -------------------------------------------------------------
  let accListToken = 0;
  let debounceTimer = 0;
  const debounce = (fn, ms = 250) => { clearTimeout(debounceTimer); debounceTimer = setTimeout(fn, ms); };

  async function viewAccounts(view, ctx) {
    const f = S.accFilter;
    const statuses = Object.keys((S.meta && S.meta.lifecycles && S.meta.lifecycles.account) || {});
    const sectors = (S.meta && S.meta.sectors) || [];
    const users = (S.meta && S.meta.users) || [];
    view.innerHTML = `
      <div class="page-head">
        <div>
          <h1>${esc(t('accountsTitle'))}</h1>
          <p class="sub">${esc(t('accountsSub'))}</p>
        </div>
        <button type="button" class="btn btn-primary" data-add-client>${ICON.plus}${esc(t('addClient'))}</button>
      </div>
      <div class="filters" id="acc-filters">
        <div class="search-input">
          ${ICON.search}
          <label class="sr-only" for="acc-q">${esc(t('searchAccounts'))}</label>
          <input id="acc-q" type="search" class="input" autocomplete="off" placeholder="${esc(t('searchAccounts'))}" value="${esc(f.q)}">
        </div>
        <label class="sr-only" for="acc-sector">${esc(t('sector'))}</label>
        <select id="acc-sector" class="select">
          <option value="">${esc(t('allSectors'))}</option>
          ${sectors.map((s) => `<option value="${esc(s)}"${s === f.sector ? ' selected' : ''}>${esc(s)}</option>`).join('')}
        </select>
        <label class="sr-only" for="acc-owner">${esc(t('owner'))}</label>
        <select id="acc-owner" class="select">
          <option value="">${esc(t('allOwners'))}</option>
          ${users.map((u) => `<option value="${esc(u)}"${u === f.owner ? ' selected' : ''}>${esc(u)}</option>`).join('')}
        </select>
        <div class="chips" role="group" aria-label="${esc(t('colStatus'))}">
          <button type="button" class="chip" data-status="" aria-pressed="${!f.status}">${esc(t('allStatuses'))}</button>
          ${statuses.map((s) => `<button type="button" class="chip" data-status="${esc(s)}" aria-pressed="${s === f.status}">${esc(statusLabel('account', s))}</button>`).join('')}
        </div>
      </div>
      <div class="card table-wrap" id="acc-list">${loadingHtml()}</div>`;

    const filters = $('#acc-filters', view);
    $('#acc-q', view).addEventListener('input', (e) => { f.q = e.target.value.trim(); debounce(() => loadAccountsList(view, ctx)); });
    $('#acc-sector', view).addEventListener('change', (e) => { f.sector = e.target.value; loadAccountsList(view, ctx); });
    $('#acc-owner', view).addEventListener('change', (e) => { f.owner = e.target.value; loadAccountsList(view, ctx); });
    filters.addEventListener('click', (e) => {
      const chip = e.target.closest('.chip[data-status]');
      if (!chip) return;
      f.status = chip.dataset.status;
      $$('.chip[data-status]', filters).forEach((c) => c.setAttribute('aria-pressed', String(c === chip)));
      loadAccountsList(view, ctx);
    });
    await loadAccountsList(view, ctx);
  }

  async function loadAccountsList(view, ctx) {
    const my = ++accListToken;
    const f = S.accFilter;
    const host = $('#acc-list', view);
    if (!host) return;
    let list;
    try {
      list = await api(`/accounts${qs({ q: f.q, status: f.status, sector: f.sector, owner: f.owner })}`);
    } catch (err) {
      if (my === accListToken && ctx.alive()) host.innerHTML = `<p class="empty-inline">${esc(err.message)}</p>`;
      return;
    }
    if (my !== accListToken || !ctx.alive()) return;
    const filtered = !!(f.q || f.status || f.sector || f.owner);
    if (!list || !list.length) {
      host.innerHTML = filtered
        ? emptyState({
          icon: ICON.search, title: t('noMatchTitle'), text: t('noMatchText'),
          actions: `<button type="button" class="btn" data-clear-acc>${esc(t('clearFilters'))}</button>
                    <button type="button" class="btn btn-primary" data-add-client>${ICON.plus}${esc(t('addClient'))}</button>`,
        })
        : emptyState({
          title: t('emptyAccountsTitle'), text: t('emptyAccountsText'),
          actions: `<button type="button" class="btn btn-primary" data-add-client>${ICON.plus}${esc(t('addClient'))}</button>`,
        });
      return;
    }
    host.innerHTML = `<table class="table">
      <thead><tr>
        <th scope="col">${esc(t('colAccount'))}</th>
        <th scope="col">${esc(t('colStatus'))}</th>
        <th scope="col">${esc(t('colOwner'))}</th>
        <th scope="col" class="num">${esc(t('colOpenJobs'))}</th>
        <th scope="col">${esc(t('colLastActivity'))}</th>
      </tr></thead>
      <tbody>${list.map((a) => `<tr data-href="#/accounts/${enc(a.id)}">
        <td class="c-main">
          <a class="cell-title" href="#/accounts/${enc(a.id)}">${esc(a.name)}</a>
          <div class="cell-sub">${[a.sector, a.city].filter(Boolean).map(esc).join('<span class="dot-sep"></span>')}</div>
        </td>
        <td class="c-badge">${badge('account', a.status, { size: 'sm' })}</td>
        <td class="c-extra" data-label="${esc(t('colOwner'))}">${esc(a.owner || '')}</td>
        <td class="c-extra num" data-label="${esc(t('colOpenJobs'))}">${esc(a.openJobs ?? 0)}</td>
        <td class="c-extra" data-label="${esc(t('colLastActivity'))}">${esc(a.lastActivity ? relDate(a.lastActivity) : t('never'))}</td>
      </tr>`).join('')}</tbody>
    </table>`;
  }

  // ---- Account page --------------------------------------------------------------
  async function viewAccount(view, ctx, id) {
    const d = await api(`/accounts/${enc(id)}`);
    if (!ctx.alive()) return;
    const a = d.account || {};
    const dash = d.dashboard || {};
    const jobs = d.jobs || [];
    const contacts = d.contacts || [];
    const proposals = d.proposals || [];
    const timeline = d.timeline || [];
    const tab = S.accountTab[id] || 'jobs';

    const perStage = dash.candidatesPerStage || {};
    const stageChips = stageOrder().filter((s) => perStage[s])
      .map((s) => `<span class="badge badge-sm tone-${statusTone('stage', s)}" title="${esc(statusLabel('stage', s))}">${esc(statusLabel('stage', s))} <span class="num">${esc(perStage[s])}</span></span>`)
      .join('');
    const na = dash.nextAction;
    const avg = dash.avgDaysOpen;

    const tabs = [
      ['jobs', t('tabJobs'), jobs.length],
      ['contacts', t('tabContacts'), contacts.length],
      ['proposals', t('tabProposals'), proposals.length],
    ];

    view.innerHTML = `
      <nav class="crumbs" aria-label="Breadcrumb"><a href="#/accounts">${esc(t('navAccounts'))}</a><span aria-hidden="true">/</span><span>${esc(a.name)}</span></nav>
      <div class="card">
        <div class="record-head">
          <div class="titles">
            <h1>${esc(a.name)} ${badge('account', a.status, { editable: true, id: a.id, size: 'lg' })}</h1>
            <div class="facts">
              ${a.sector ? `<span><span class="k">${esc(t('sector'))}</span> ${esc(a.sector)}</span>` : ''}
              ${a.city ? `<span><span class="k">${esc(t('city'))}</span> ${esc(a.city)}</span>` : ''}
              ${a.owner ? `<span><span class="k">${esc(t('owner'))}</span> ${esc(a.owner)}</span>` : ''}
              ${a.since ? `<span><span class="k">${esc(t('since'))}</span> <span class="num">${esc(fmtDate(a.since))}</span></span>` : ''}
            </div>
          </div>
          <div class="btn-row">
            <button type="button" class="btn btn-primary" data-log-activity>${ICON.note}${esc(t('logActivity'))}</button>
            <button type="button" class="btn" data-add-action>${ICON.calendar}${esc(t('addNextAction'))}</button>
          </div>
        </div>
        <div class="dash" style="border-top:1px solid var(--border);margin-top:0">
          <div><div class="label">${esc(t('dashOpenJobs'))}</div><div class="value">${esc(dash.openJobs ?? 0)}</div></div>
          <div><div class="label">${esc(t('dashPerStage'))}</div>${stageChips ? `<div class="stage-chips">${stageChips}</div>` : `<div class="value muted" style="font-size:1rem">${esc(t('noneYet'))}</div>`}</div>
          <div><div class="label">${esc(t('dashAvgDays'))}</div><div class="value">${avg === null || avg === undefined ? '–' : esc(Math.round(Number(avg)))} <small>${esc(t('days'))}</small></div></div>
          <div><div class="label">${esc(t('dashNextAction'))}</div>${na
            ? `<div class="next">${esc(na.title)}</div><div class="next-due${na.overdue ? ' overdue' : ''}">${esc(dueLabel(na.due))}</div>`
            : `<div class="value muted" style="font-size:1rem">${esc(t('none'))}</div>`}</div>
        </div>
      </div>

      <div class="record-grid">
        <div class="card">
          <div class="tabs" role="tablist" aria-label="${esc(a.name)}">
            ${tabs.map(([key, label, n]) => `<button type="button" role="tab" id="tab-${key}" data-tab="${key}" aria-controls="tabpanel"
              aria-selected="${key === tab}" tabindex="${key === tab ? 0 : -1}">${esc(label)} <span class="count">${n}</span></button>`).join('')}
          </div>
          <div id="tabpanel" role="tabpanel" aria-labelledby="tab-${tab}"></div>
        </div>
        <aside class="aside">
          <section class="card">
            <div class="section-head card-pad" style="padding-bottom:0;margin-bottom:6px"><h2>${esc(t('nextActions'))} <span class="count">${(d.actions || []).length}</span></h2></div>
            ${actionList(d.actions, { skipAccount: a.id })}
          </section>
          <section class="card">
            <div class="section-head card-pad" style="padding-bottom:0;margin-bottom:0"><h2>${esc(t('timeline'))}</h2></div>
            ${timeline.length ? `<ol class="timeline">${timeline.map(timelineItem).join('')}</ol>` : `<p class="empty-inline">${esc(t('noTimeline'))}</p>`}
          </section>
        </aside>
      </div>`;

    const panel = $('#tabpanel', view);
    const renderTab = (key) => {
      S.accountTab[id] = key;
      panel.setAttribute('aria-labelledby', `tab-${key}`);
      if (key === 'jobs') {
        panel.innerHTML = jobs.length
          ? `<ul class="list">${jobs.map((j) => jobRow(j, { editable: true, showCandidates: true })).join('')}</ul>`
          : emptyState({ icon: ICON.briefcase, title: t('noJobs') });
      } else if (key === 'contacts') {
        panel.innerHTML = contacts.length
          ? `<ul class="list">${contacts.map((c) => `<li class="row">
              ${avatar(c.name)}
              <div class="row-main">
                <div><a class="row-title" href="#/people/${enc(c.personId)}">${esc(c.name)}</a>${c.primary ? `<span class="tag tag-accent primary-mark">${esc(t('primary'))}</span>` : ''}</div>
                <div class="row-sub">${[c.role, c.title].filter(Boolean).map(esc).join('<span class="dot-sep"></span>')}</div>
                ${c.email ? `<div class="row-sub"><a href="mailto:${esc(c.email)}">${esc(c.email)}</a></div>` : ''}
              </div>
              <div class="row-meta">
                <span class="days" title="${esc(t('lastContact'))}">${esc(relDate(c.lastContact))}</span>
                ${consentBadge(c.consent)}
              </div>
            </li>`).join('')}</ul>`
          : emptyState({ icon: ICON.people, title: t('noContacts') });
      } else {
        panel.innerHTML = proposals.length
          ? `<ul class="list">${proposals.map((p) => `<li class="row">
              <div class="row-main">
                <div class="row-title">${esc(p.title)}</div>
                ${p.terms ? `<div class="terms">${esc(p.terms)}</div>` : ''}
              </div>
              <div class="row-meta">
                <span class="days">${esc(fmtDate(p.date))}</span>
                ${badge('proposal', p.status, { editable: true, id: p.id, size: 'sm' })}
              </div>
            </li>`).join('')}</ul>`
          : emptyState({ icon: ICON.doc, title: t('noProposals') });
      }
    };
    renderTab(tab);

    const tablist = $('[role="tablist"]', view);
    const tabBtns = $$('[role="tab"]', tablist);
    const selectTab = (b, focus) => {
      tabBtns.forEach((x) => { x.setAttribute('aria-selected', String(x === b)); x.tabIndex = x === b ? 0 : -1; });
      renderTab(b.dataset.tab);
      if (focus) b.focus();
    };
    tablist.addEventListener('click', (e) => { const b = e.target.closest('[role="tab"]'); if (b) selectTab(b, false); });
    tablist.addEventListener('keydown', (e) => {
      const i = tabBtns.indexOf(document.activeElement);
      if (i < 0) return;
      if (e.key === 'ArrowRight') { e.preventDefault(); selectTab(tabBtns[(i + 1) % tabBtns.length], true); }
      if (e.key === 'ArrowLeft') { e.preventDefault(); selectTab(tabBtns[(i - 1 + tabBtns.length) % tabBtns.length], true); }
    });

    $('[data-log-activity]', view).addEventListener('click', () => openQuickActivity({ accountId: a.id, contextName: a.name }));
    $('[data-add-action]', view).addEventListener('click', () => openAddAction({ accountId: a.id, contextName: a.name }));
  }

  function timelineItem(ev) {
    const kind = ['status', 'activity', 'proposal', 'candidate', 'job'].includes(ev.kind) ? ev.kind : 'job';
    const kindKey = `kind${kind.charAt(0).toUpperCase()}${kind.slice(1)}`;
    return `<li class="tl-item">
      <span class="tl-mark k-${kind}" title="${esc(t(kindKey))}">${KIND_ICON[kind]}</span>
      <div>
        <div class="tl-text">${esc(ev.text)}</div>
        <div class="tl-meta"><span class="sr-only">${esc(t(kindKey))}. </span>${ev.user ? `${esc(ev.user)}<span class="dot-sep"></span>` : ''}<span class="mono">${esc(fmtDateTime(ev.at))}</span></div>
      </div>
    </li>`;
  }

  // ---- Job order + pipeline board ----------------------------------------------
  async function viewJob(view, ctx, id) {
    const d = await api(`/jobs/${enc(id)}`);
    if (!ctx.alive()) return;
    const j = d.job || {};
    const acc = d.account || {};
    const apps = d.applications || [];
    const stages = stageOrder();
    const byStage = {};
    stages.forEach((s) => { byStage[s] = []; });
    apps.forEach((ap) => { (byStage[ap.stage] = byStage[ap.stage] || []).push(ap); });

    const card = (ap) => {
      const next = nextStatuses('stage', ap.stage);
      const final = !next.length;
      return `<article class="kcard${final ? ' is-final' : ''}" data-app="${esc(ap.id)}" draggable="${final ? 'false' : 'true'}">
        <div class="kcard-top">
          <a class="kcard-name" href="#/people/${enc(ap.personId)}" draggable="false">${esc(ap.name)}</a>
          ${final ? '' : `<button type="button" class="btn btn-icon move" data-move="${esc(ap.id)}" aria-haspopup="menu"
            aria-label="${esc(t('moveAria', { name: ap.name }))}" title="${esc(t('moveTo'))}">${ICON.move}</button>`}
        </div>
        ${ap.title || ap.company ? `<div class="kcard-role">${personRole(ap.title, ap.company)}</div>` : ''}
        ${ap.stage === 'REJECTED' && ap.rejectionReason ? `<div class="reason"><span class="sr-only">${esc(t('reasonLabel'))}: </span>${esc(ap.rejectionReason)}</div>` : ''}
        <div class="kcard-foot">
          ${consentBadge(ap.consent)}
          <span class="mono" title="${esc(t('updated', { date: fmtDate(ap.updated) }))}">${esc(fmtDate(ap.updated))}</span>
        </div>
      </article>`;
    };

    view.innerHTML = `
      <nav class="crumbs" aria-label="Breadcrumb">
        <a href="#/accounts">${esc(t('navAccounts'))}</a><span aria-hidden="true">/</span>
        <a href="#/accounts/${enc(acc.id || j.accountId)}">${esc(acc.name || j.accountName)}</a><span aria-hidden="true">/</span>
        <span>${esc(j.title)}</span>
      </nav>
      <div class="card">
        <div class="record-head">
          <div class="titles">
            <h1>${esc(j.title)} ${badge('job', j.status, { editable: true, id: j.id, size: 'lg' })}</h1>
            <div class="facts">
              <span><span class="k">${esc(t('jobClient'))}</span> <a href="#/accounts/${enc(acc.id || j.accountId)}">${esc(acc.name || j.accountName)}</a>
                ${acc.status ? badge('account', acc.status, { size: 'sm' }) : ''}</span>
              ${j.location ? `<span><span class="k">${esc(t('location'))}</span> ${esc(j.location)}</span>` : ''}
              ${j.seniority ? `<span><span class="k">${esc(t('seniority'))}</span> ${esc(j.seniority)}</span>` : ''}
              ${j.fee !== null && j.fee !== undefined && j.fee !== '' ? `<span><span class="k">${esc(t('fee'))}</span> <span class="num">${esc(j.fee)}</span></span>` : ''}
              ${j.owner ? `<span><span class="k">${esc(t('owner'))}</span> ${esc(j.owner)}</span>` : ''}
              ${j.opened ? `<span><span class="k">${esc(t('opened'))}</span> <span class="num">${esc(fmtDate(j.opened))}</span> <span class="muted">(${esc(t('daysOpenShort', { n: j.daysOpen }))})</span></span>` : ''}
            </div>
          </div>
          <div class="btn-row">
            <button type="button" class="btn btn-primary" data-add-candidate>${ICON.plus}${esc(t('addCandidate'))}</button>
          </div>
        </div>
      </div>
      <div class="section-head" style="margin-top:22px">
        <h2>${esc(t('pipeline'))} <span class="count">${apps.filter((x) => x.stage !== 'REJECTED').length}</span></h2>
        <span class="hint">${esc(t('boardHint'))}</span>
      </div>
      <div class="board-wrap" style="margin-top:8px">
        <div class="board" id="board">
          ${stages.map((s) => `<section class="col" data-stage="${esc(s)}" aria-label="${esc(statusLabel('stage', s))}">
            <header class="col-head">
              <span class="badge badge-sm tone-${statusTone('stage', s)}">${esc(statusLabel('stage', s))}</span>
              <span class="count">${(byStage[s] || []).length}</span>
            </header>
            <div class="col-body">
              ${(byStage[s] || []).length ? byStage[s].map(card).join('') : `<div class="col-empty">${esc(t('noCandidatesCol'))}</div>`}
            </div>
          </section>`).join('')}
        </div>
      </div>`;

    $('[data-add-candidate]', view).addEventListener('click', () => openAddCandidate(j.id, j.title));
    bindBoard($('#board', view), apps);
  }

  /** HTML5 drag and drop plus per-card stage menu. Only lifecycle-allowed targets accept drops. */
  function bindBoard(board, apps) {
    const byId = new Map(apps.map((a) => [String(a.id), a]));
    const cols = $$('.col', board);
    let drag = null;

    const clear = () => {
      board.classList.remove('is-dragging');
      cols.forEach((c) => c.classList.remove('is-valid', 'is-invalid', 'is-over', 'is-source'));
      $$('.kcard.is-drag-src', board).forEach((c) => c.classList.remove('is-drag-src'));
      drag = null;
    };

    board.addEventListener('dragstart', (e) => {
      const cardEl = e.target.closest && e.target.closest('.kcard');
      if (!cardEl) return;
      const app = byId.get(cardEl.dataset.app);
      if (!app) return;
      const valid = nextStatuses('stage', app.stage);
      if (!valid.length) { e.preventDefault(); return; }
      drag = { app, valid };
      e.dataTransfer.effectAllowed = 'move';
      try { e.dataTransfer.setData('text/plain', String(app.id)); } catch (err) { /* IE-style */ }
      cardEl.classList.add('is-drag-src');
      board.classList.add('is-dragging');
      cols.forEach((c) => {
        const s = c.dataset.stage;
        if (s === app.stage) c.classList.add('is-source');
        else c.classList.add(valid.includes(s) ? 'is-valid' : 'is-invalid');
      });
    });
    board.addEventListener('dragend', clear);
    board.addEventListener('dragover', (e) => {
      if (!drag) return;
      const col = e.target.closest && e.target.closest('.col');
      if (!col || !drag.valid.includes(col.dataset.stage)) {
        if (e.dataTransfer) e.dataTransfer.dropEffect = 'none';
        return;
      }
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      cols.forEach((c) => c.classList.toggle('is-over', c === col));
    });
    board.addEventListener('dragleave', (e) => {
      const col = e.target.closest && e.target.closest('.col');
      if (col && !col.contains(e.relatedTarget)) col.classList.remove('is-over');
    });
    board.addEventListener('drop', (e) => {
      if (!drag) return;
      const col = e.target.closest && e.target.closest('.col');
      e.preventDefault();
      const { app, valid } = drag;
      clear();
      if (!col) return;
      const stage = col.dataset.stage;
      if (stage === app.stage || !valid.includes(stage)) return;
      moveStage(app, stage);
    });

    board.addEventListener('click', (e) => {
      const btn = e.target.closest('button[data-move]');
      if (!btn) return;
      const app = byId.get(btn.dataset.move);
      if (!app) return;
      openMenu(btn, {
        title: t('moveTo'),
        items: nextStatuses('stage', app.stage).map((s) => ({
          label: statusLabel('stage', s),
          html: badge('stage', s, { size: 'sm' }),
          onSelect: () => moveStage(app, s),
        })),
      });
    });
  }

  // ---- Network list --------------------------------------------------------------
  let netListToken = 0;

  async function viewNetwork(view, ctx) {
    const f = S.netFilter;
    const types = personTypes();
    view.innerHTML = `
      <div class="page-head">
        <div>
          <h1>${esc(t('networkTitle'))}</h1>
          <p class="sub">${esc(t('networkSub'))}</p>
        </div>
      </div>
      <div class="filters" id="net-filters">
        <div class="search-input">
          ${ICON.search}
          <label class="sr-only" for="net-q">${esc(t('searchPeople'))}</label>
          <input id="net-q" type="search" class="input" autocomplete="off" placeholder="${esc(t('searchPeople'))}" value="${esc(f.q)}">
        </div>
        <div class="chips" role="group" aria-label="${esc(t('colType'))}">
          <button type="button" class="chip" data-type="" aria-pressed="${!f.type}">${esc(t('allTypes'))}</button>
          ${types.map((ty) => `<button type="button" class="chip" data-type="${esc(ty)}" aria-pressed="${ty === f.type}">${esc(typeLabel(ty))}</button>`).join('')}
        </div>
      </div>
      <div class="card table-wrap" id="net-list">${loadingHtml()}</div>`;

    const filters = $('#net-filters', view);
    $('#net-q', view).addEventListener('input', (e) => { f.q = e.target.value.trim(); debounce(() => loadNetworkList(view, ctx)); });
    filters.addEventListener('click', (e) => {
      const chip = e.target.closest('.chip[data-type]');
      if (!chip) return;
      f.type = chip.dataset.type;
      $$('.chip[data-type]', filters).forEach((c) => c.setAttribute('aria-pressed', String(c === chip)));
      loadNetworkList(view, ctx);
    });
    await loadNetworkList(view, ctx);
  }

  async function loadNetworkList(view, ctx) {
    const my = ++netListToken;
    const f = S.netFilter;
    const host = $('#net-list', view);
    if (!host) return;
    let list;
    try {
      list = await api(`/people${qs({ q: f.q, type: f.type })}`);
    } catch (err) {
      if (my === netListToken && ctx.alive()) host.innerHTML = `<p class="empty-inline">${esc(err.message)}</p>`;
      return;
    }
    if (my !== netListToken || !ctx.alive()) return;
    if (!list || !list.length) {
      host.innerHTML = (f.q || f.type)
        ? emptyState({ icon: ICON.search, title: t('noMatchTitle'), text: t('noMatchText'), actions: `<button type="button" class="btn" data-clear-net>${esc(t('clearFilters'))}</button>` })
        : emptyState({ icon: ICON.people, title: t('emptyNetworkTitle'), text: t('emptyNetworkText') });
      return;
    }
    host.innerHTML = `<table class="table">
      <thead><tr>
        <th scope="col">${esc(t('colPerson'))}</th>
        <th scope="col">${esc(t('colConsent'))}</th>
        <th scope="col">${esc(t('colType'))}</th>
        <th scope="col">${esc(t('colCity'))}</th>
        <th scope="col">${esc(t('colOwner'))}</th>
        <th scope="col">${esc(t('colLastContact'))}</th>
      </tr></thead>
      <tbody>${list.map((p) => `<tr data-href="#/people/${enc(p.id)}">
        <td class="c-main">
          <a class="cell-title" href="#/people/${enc(p.id)}">${esc(p.name)}</a>
          <div class="cell-sub">${personRole(p.title, p.company)}</div>
        </td>
        <td class="c-badge">${consentBadge(p.consent)}</td>
        <td class="c-extra" data-label="${esc(t('colType'))}"><span class="tag">${esc(typeLabel(p.type))}</span></td>
        <td class="c-extra c-hide-sm">${esc(p.city || '')}</td>
        <td class="c-extra c-hide-sm">${esc(p.owner || '')}</td>
        <td class="c-extra" data-label="${esc(t('colLastContact'))}" title="${esc(fmtDate(p.lastContact))}">${esc(relDate(p.lastContact))}</td>
      </tr>`).join('')}</tbody>
    </table>`;
  }

  // ---- Person page ---------------------------------------------------------------
  async function viewPerson(view, ctx, id) {
    const d = await api(`/people/${enc(id)}`);
    if (!ctx.alive()) return;
    const p = d.person || {};
    const career = p.career || [];
    const conns = d.connections || [];
    const accs = d.accounts || [];
    const apps = d.applications || [];
    const acts = d.activities || [];

    view.innerHTML = `
      <nav class="crumbs" aria-label="Breadcrumb"><a href="#/network">${esc(t('navNetwork'))}</a><span aria-hidden="true">/</span><span>${esc(p.name)}</span></nav>
      <div class="card">
        <div class="record-head">
          ${avatar(p.name, 'avatar-lg')}
          <div class="titles">
            <h1>${esc(p.name)} <span class="tag">${esc(typeLabel(p.type))}</span> ${consentBadge(p.consent, 'lg')}</h1>
            ${p.title || p.company ? `<p class="sub" style="margin-top:4px;color:var(--ink-2)">${personRole(p.title, p.company)}</p>` : ''}
            <div class="facts">
              ${p.city ? `<span><span class="k">${esc(t('city'))}</span> ${esc(p.city)}</span>` : ''}
              ${p.email ? `<span><span class="k">${esc(t('email'))}</span> <a href="mailto:${esc(p.email)}">${esc(p.email)}</a></span>` : ''}
              ${p.owner ? `<span><span class="k">${esc(t('owner'))}</span> ${esc(p.owner)}</span>` : ''}
              <span><span class="k">${esc(t('lastContact'))}</span> <span title="${esc(fmtDate(p.lastContact))}">${esc(relDate(p.lastContact))}</span></span>
            </div>
          </div>
          <div class="btn-row">
            <button type="button" class="btn btn-primary" data-log-activity>${ICON.note}${esc(t('logActivity'))}</button>
            <button type="button" class="btn" data-add-action>${ICON.calendar}${esc(t('addNextAction'))}</button>
          </div>
        </div>
        ${p.howWeKnow ? `<div style="padding:0 22px 18px"><div class="how"><span class="eyebrow">${esc(t('howWeKnow'))}</span><div style="margin-top:4px">${esc(p.howWeKnow)}</div></div></div>` : ''}
      </div>

      <div class="record-grid">
        <div>
          <section class="section">
            <div class="section-head"><h2>${esc(t('pipelines'))} <span class="count">${apps.length}</span></h2></div>
            <div class="card">${apps.length ? `<ul class="list">${apps.map((ap) => `<li class="row">
                <div class="row-main">
                  <a class="row-title" href="#/jobs/${enc(ap.jobId)}">${esc(ap.jobTitle)}</a>
                  <div class="row-sub"><a href="#/accounts/${enc(ap.accountId)}">${esc(ap.accountName)}</a><span class="dot-sep"></span>${esc(t('updated', { date: fmtDate(ap.updated) }))}</div>
                </div>
                ${badge('stage', ap.stage, { editable: true, id: ap.id, name: p.name, size: 'sm' })}
              </li>`).join('')}</ul>` : `<p class="empty-inline">${esc(t('noPipelines'))}</p>`}</div>
          </section>

          <section class="section">
            <div class="section-head"><h2>${esc(t('career'))}</h2></div>
            <div class="card card-pad">${career.length ? `<ul class="career">${career.map((c) => `<li>${esc(c)}</li>`).join('')}</ul>` : `<p class="muted">${esc(t('noCareer'))}</p>`}</div>
          </section>

          <section class="section">
            <div class="section-head"><h2>${esc(t('connections'))} <span class="count">${conns.length}</span></h2></div>
            <div class="card">${conns.length ? `<ul class="list">${conns.map((c) => `<li class="row">
                ${avatar(c.name)}
                <div class="row-main">
                  <a class="row-title" href="#/people/${enc(c.personId)}">${esc(c.name)}</a>
                  <div class="row-sub">${personRole(c.title, c.company)}</div>
                </div>
                <span class="tag">${esc(relationLabel(c.kind))}</span>
              </li>`).join('')}</ul>` : `<p class="empty-inline">${esc(t('noConnections'))}</p>`}</div>
          </section>

          <section class="section">
            <div class="section-head"><h2>${esc(t('contactAt'))} <span class="count">${accs.length}</span></h2></div>
            <div class="card">${accs.length ? `<ul class="list">${accs.map((x) => `<li class="row">
                <div class="row-main">
                  <a class="row-title" href="#/accounts/${enc(x.accountId)}">${esc(x.name)}</a>
                  ${x.role ? `<div class="row-sub">${esc(x.role)}</div>` : ''}
                </div>
              </li>`).join('')}</ul>` : `<p class="empty-inline">${esc(t('noAccountsForPerson'))}</p>`}</div>
          </section>
        </div>

        <aside class="aside" style="margin-top:0">
          <section class="card">
            <div class="section-head card-pad" style="padding-bottom:0;margin-bottom:6px"><h2>${esc(t('nextActions'))} <span class="count">${(d.actions || []).length}</span></h2></div>
            ${actionList(d.actions, { skipPerson: p.id })}
          </section>
          <section class="card">
            <div class="section-head card-pad" style="padding-bottom:0;margin-bottom:0"><h2>${esc(t('activityHistory'))}</h2></div>
            ${acts.length ? `<ol class="timeline">${acts.map((ac) => `<li class="tl-item">
                <span class="tl-mark k-activity" title="${esc(activityLabel(ac.type))}">${ACT_ICON[ac.type] || ICON.note}</span>
                <div>
                  <div class="tl-text"><strong>${esc(activityLabel(ac.type))}</strong>${ac.accountId ? ` · <a href="#/accounts/${enc(ac.accountId)}">${esc(ac.accountName || '')}</a>` : ''}</div>
                  ${ac.note ? `<div class="tl-text tl-note">${esc(ac.note)}</div>` : ''}
                  <div class="tl-meta">${ac.user ? `${esc(ac.user)}<span class="dot-sep"></span>` : ''}<span class="mono">${esc(fmtDateTime(ac.at))}</span></div>
                </div>
              </li>`).join('')}</ol>` : `<p class="empty-inline">${esc(t('noActivities'))}</p>`}
          </section>
        </aside>
      </div>`;

    $('[data-log-activity]', view).addEventListener('click', () => openQuickActivity({ personId: p.id, contextName: p.name }));
    $('[data-add-action]', view).addEventListener('click', () => openAddAction({ personId: p.id, contextName: p.name }));
  }

  // ---- Search --------------------------------------------------------------------
  async function viewSearch(view, ctx, q) {
    const input = $('#global-q');
    if (input && document.activeElement !== input) input.value = q;
    if (!q) {
      view.innerHTML = `<div class="page-head"><div><h1>${esc(t('searchTitle'))}</h1></div></div>
        <div class="card">${emptyState({ icon: ICON.search, title: t('searchTitle'), text: t('searchPrompt') })}</div>`;
      return;
    }
    const d = await api(`/search${qs({ q })}`);
    if (!ctx.alive()) return;
    const accounts = d.accounts || [];
    const people = d.people || [];
    const jobs = d.jobs || [];
    const total = accounts.length + people.length + jobs.length;
    const group = (title, n, inner) => (n ? `<section>
        <div class="section-head"><h2>${esc(title)} <span class="count">${n}</span></h2></div>
        <div class="card"><ul class="list">${inner}</ul></div>
      </section>` : '');
    view.innerHTML = `
      <div class="page-head"><div>
        <span class="eyebrow">${esc(t('searchTitle'))}</span>
        <h1>${esc(t('searchResultsFor', { q }))}</h1>
      </div></div>
      ${total ? `<div class="search-groups">
        ${group(t('groupAccounts'), accounts.length, accounts.map(accountRow).join(''))}
        ${group(t('groupPeople'), people.length, people.map(personRow).join(''))}
        ${group(t('groupJobs'), jobs.length, jobs.map((j) => jobRow(j, { showClient: true })).join(''))}
      </div>` : `<div class="card">${emptyState({ icon: ICON.search, title: t('searchNoneTitle'), text: t('searchNoneText', { q }) })}</div>`}`;
  }

  // ---------------------------------------------------------------------------
  // Router
  // ---------------------------------------------------------------------------
  function parseHash() {
    const raw = location.hash.replace(/^#/, '') || '/';
    const qi = raw.indexOf('?');
    const path = qi >= 0 ? raw.slice(0, qi) : raw;
    const query = qi >= 0 ? raw.slice(qi + 1) : '';
    const parts = path.split('/').filter(Boolean).map((p) => { try { return decodeURIComponent(p); } catch (e) { return p; } });
    return { parts, params: new URLSearchParams(query), path };
  }

  function resolve(parts, params) {
    const [a, b] = parts;
    if (!a) return { section: 'home', fn: viewHome, titleKey: 'navHome' };
    if (a === 'accounts' && !b) return { section: 'accounts', fn: viewAccounts, titleKey: 'navAccounts' };
    if (a === 'accounts' && b) return { section: 'accounts', fn: (v, c) => viewAccount(v, c, b) };
    if (a === 'jobs' && b) return { section: 'accounts', fn: (v, c) => viewJob(v, c, b) };
    if (a === 'network') return { section: 'network', fn: viewNetwork, titleKey: 'navNetwork' };
    if (a === 'people' && b) return { section: 'network', fn: (v, c) => viewPerson(v, c, b) };
    if (a === 'search') return { section: '', fn: (v, c) => viewSearch(v, c, (params.get('q') || '').trim()), titleKey: 'searchTitle' };
    return { section: '', fn: () => { throw new ApiError(t('notFoundText'), 404); } };
  }

  function setNav(section) {
    $$('[data-nav]').forEach((el) => {
      if (el.dataset.nav === section) el.setAttribute('aria-current', 'page');
      else el.removeAttribute('aria-current');
    });
  }

  /** Renders the current route. soft = keep current content until new data arrives (after mutations). */
  async function render(opts = {}) {
    closeMenu();
    if (!opts.keepModal) closeModal();
    const { parts, params, path } = parseHash();
    const token = ++S.renderToken;
    const ctx = { alive: () => token === S.renderToken };
    const view = $('#view');
    const route = resolve(parts, params);
    setNav(route.section);
    const routeChanged = S.lastPath !== path;
    S.lastPath = path;
    if (parts[0] !== 'search') {
      const input = $('#global-q');
      if (input && document.activeElement !== input) input.value = '';
    }
    if (!opts.soft) {
      view.innerHTML = loadingHtml();
      if (routeChanged) window.scrollTo(0, 0);
    }
    try {
      await route.fn(view, ctx);
      if (!ctx.alive()) return;
      const h1 = $('h1', view);
      document.title = `${h1 ? h1.childNodes[0].textContent.trim() : t(route.titleKey || 'navHome')} · Search CRM`;
      if (routeChanged && !opts.soft && opts.focus) view.focus({ preventScroll: true });
    } catch (err) {
      if (!ctx.alive()) return;
      view.innerHTML = errorHtml(err);
    }
  }

  /** Re-fetches and re-renders the current screen after a change. */
  function refresh() { return render({ soft: true }); }

  // ---------------------------------------------------------------------------
  // Shell: language, nav, global search, delegated events
  // ---------------------------------------------------------------------------
  function applyStaticI18n() {
    document.documentElement.lang = S.lang;
    $$('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
    $$('[data-i18n-placeholder]').forEach((el) => { el.placeholder = t(el.dataset.i18nPlaceholder); });
    $$('[data-lang]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.lang === S.lang)));
    const lt = $('.lang-toggle');
    if (lt) lt.setAttribute('aria-label', t('language'));
  }

  function renderMe() {
    const host = $('#me');
    const name = S.meta && S.meta.currentUser;
    if (!host || !name) return;
    host.innerHTML = `${avatar(name)}<div class="who"><strong>${esc(name)}</strong></div>`;
  }

  function setLang(lang) {
    if (!DICT[lang] || lang === S.lang) return;
    S.lang = lang;
    try { localStorage.setItem('crm-lang', lang); } catch (e) { /* storage unavailable */ }
    applyStaticI18n();
    render({ soft: true });
  }

  function bindShell() {
    $$('[data-lang]').forEach((b) => b.addEventListener('click', () => setLang(b.dataset.lang)));

    $('#global-search').addEventListener('submit', (e) => {
      e.preventDefault();
      const q = $('#global-q').value.trim();
      if (!q) return;
      const target = `#/search?q=${enc(q)}`;
      if (location.hash === target) render();
      else location.hash = target;
    });

    document.addEventListener('click', (e) => {
      const badgeBtn = e.target.closest('button[data-badge]');
      if (badgeBtn) { e.preventDefault(); openStatusMenu(badgeBtn); return; }
      if (e.target.closest('[data-add-client]')) { openAddClient(); return; }
      if (e.target.closest('[data-retry]')) { render(); return; }
      if (e.target.closest('[data-clear-acc]')) {
        S.accFilter = { q: '', status: '', sector: '', owner: '' };
        render();
        return;
      }
      if (e.target.closest('[data-clear-net]')) {
        S.netFilter = { q: '', type: '' };
        render();
        return;
      }
      // Whole table row is clickable; inner links/buttons keep their own behaviour.
      const tr = e.target.closest('tr[data-href]');
      if (tr && !e.target.closest('a, button, input, select, textarea')) location.hash = tr.dataset.href;
    });

    document.addEventListener('change', (e) => {
      const cb = e.target.closest && e.target.closest('input.check[data-done]');
      if (cb && cb.checked) completeAction(cb);
    });

    window.addEventListener('hashchange', () => render({ focus: true }));
  }

  // ---------------------------------------------------------------------------
  // Boot
  // ---------------------------------------------------------------------------
  async function boot() {
    try {
      const saved = localStorage.getItem('crm-lang');
      if (saved && DICT[saved]) S.lang = saved;
    } catch (e) { /* storage unavailable: default EN */ }
    applyStaticI18n();
    bindShell();

    const view = $('#view');
    view.innerHTML = loadingHtml();
    try {
      S.meta = await api('/meta');
    } catch (err) {
      view.innerHTML = `<div class="card">${emptyState({
        icon: ICON.alert, title: t('errLoadTitle'), text: err.message,
        actions: `<button type="button" class="btn" id="boot-retry">${esc(t('retry'))}</button>`,
      })}</div>`;
      const retry = $('#boot-retry');
      if (retry) retry.addEventListener('click', () => location.reload());
      return;
    }
    renderMe();
    render();
  }

  boot();
})();
