-- Row counts per table and per status, to check a load.
SELECT 'app_user' t, count(*) FROM app_user UNION ALL SELECT 'account', count(*) FROM account
UNION ALL SELECT 'person', count(*) FROM person UNION ALL SELECT 'person_employment', count(*) FROM person_employment
UNION ALL SELECT 'contact', count(*) FROM contact UNION ALL SELECT 'network_link', count(*) FROM network_link
UNION ALL SELECT 'job_order', count(*) FROM job_order UNION ALL SELECT 'application', count(*) FROM application
UNION ALL SELECT 'proposal', count(*) FROM proposal UNION ALL SELECT 'activity', count(*) FROM activity
UNION ALL SELECT 'next_action', count(*) FROM next_action UNION ALL SELECT 'timeline_entry', count(*) FROM timeline_entry
UNION ALL SELECT 'account_open_job', count(*) FROM account_open_job UNION ALL SELECT 'person_privacy', count(*) FROM person_privacy
UNION ALL SELECT 'privacy_notice', count(*) FROM privacy_notice UNION ALL SELECT 'consent_request', count(*) FROM consent_request
UNION ALL SELECT 'consent_record', count(*) FROM consent_record UNION ALL SELECT 'data_subject_request', count(*) FROM data_subject_request
UNION ALL SELECT 'retention_settings', count(*) FROM retention_settings UNION ALL SELECT 'retention_run', count(*) FROM retention_run;
SELECT 'account.status' t, status, count(*) FROM account GROUP BY status
UNION ALL SELECT 'job_order.status', status, count(*) FROM job_order GROUP BY status
UNION ALL SELECT 'application.stage', stage, count(*) FROM application GROUP BY stage
UNION ALL SELECT 'proposal.status', status, count(*) FROM proposal GROUP BY status
UNION ALL SELECT 'person.type', type, count(*) FROM person GROUP BY type
UNION ALL SELECT 'person.consent', consent_status, count(*) FROM person GROUP BY consent_status
UNION ALL SELECT 'person.retention', retention_status, count(*) FROM person GROUP BY retention_status
UNION ALL SELECT 'activity.type', type, count(*) FROM activity GROUP BY type
UNION ALL SELECT 'next_action', CASE WHEN done THEN 'done' WHEN due_on < current_date THEN 'overdue'
                                     WHEN due_on = current_date THEN 'today' ELSE 'upcoming' END, count(*) FROM next_action GROUP BY 2
UNION ALL SELECT 'timeline.kind', kind, count(*) FROM timeline_entry GROUP BY kind
UNION ALL SELECT 'data_subject_request', status, count(*) FROM data_subject_request GROUP BY status
ORDER BY 1, 2;
SELECT 'open_job_count mismatches' t, count(*) FROM account a
WHERE a.open_job_count <> (SELECT count(*) FROM job_order j WHERE j.account_id = a.id AND j.status NOT IN ('FILLED', 'CANCELLED'));
