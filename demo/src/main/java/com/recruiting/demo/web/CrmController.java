package com.recruiting.demo.web;

import com.recruiting.demo.crm.CrmService;
import com.recruiting.demo.domain.Activity;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints of the CRM demo. Shapes are fixed in {@code demo/API.md}; this class
 * only maps HTTP to {@link CrmService}.
 */
@RestController
@RequestMapping("/api")
public class CrmController {

    private final CrmService crm;

    /**
     * Creates the controller.
     *
     * @param crm the CRM use cases
     */
    public CrmController(CrmService crm) {
        this.crm = crm;
    }

    /** Body to change a status. @param status the requested status name */
    public record StatusChange(String status) {
    }

    /** Body to move a candidate. @param stage the requested stage @param reason rejection reason */
    public record StageChange(String stage, String reason) {
    }

    /** Body to open an account. @param name company @param sector sector @param city city @param owner consultant */
    public record NewAccount(String name, String sector, String city, String owner) {
    }

    /** Body to add a candidate to a job. @param personId the person to add */
    public record NewApplication(String personId) {
    }

    /** Body to log an activity. @param accountId account @param personId person @param type type @param note note */
    public record NewActivity(String accountId, String personId, String type, String note) {
    }

    /** Body to create a next action. @param title what to do @param due ISO date @param accountId account @param personId person */
    public record NewAction(String title, String due, String accountId, String personId) {
    }

    /**
     * Lifecycles, users and vocabularies for the UI.
     *
     * @return the meta view
     */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return crm.meta();
    }

    /**
     * The "My day" home screen.
     *
     * @return the home view
     */
    @GetMapping("/home")
    public Map<String, Object> home() {
        return crm.home();
    }

    /**
     * Lists accounts.
     *
     * @param q      free text
     * @param status status filter
     * @param sector sector filter
     * @param owner  owner filter
     * @return account summaries
     */
    @GetMapping("/accounts")
    public List<Map<String, Object>> accounts(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String sector,
                                              @RequestParam(required = false) String owner) {
        return crm.accounts(q, status, sector, owner);
    }

    /**
     * Opens a new account as a lead.
     *
     * @param body name, sector, city and optional owner
     * @return the new account summary
     */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAccount(@RequestBody NewAccount body) {
        return crm.createAccount(body.name(), body.sector(), body.city(), body.owner());
    }

    /**
     * The account page.
     *
     * @param id account id
     * @return the account view
     */
    @GetMapping("/accounts/{id}")
    public Map<String, Object> account(@PathVariable String id) {
        return crm.account(id);
    }

    /**
     * Changes an account's status.
     *
     * @param id   account id
     * @param body the requested status
     * @return the updated account summary
     */
    @PatchMapping("/accounts/{id}/status")
    public Map<String, Object> accountStatus(@PathVariable String id, @RequestBody StatusChange body) {
        return crm.changeAccountStatus(id, body.status());
    }

    /**
     * The job order page with its pipeline.
     *
     * @param id job order id
     * @return the job view
     */
    @GetMapping("/jobs/{id}")
    public Map<String, Object> job(@PathVariable String id) {
        return crm.job(id);
    }

    /**
     * Changes a job order's status.
     *
     * @param id   job order id
     * @param body the requested status
     * @return the updated job summary
     */
    @PatchMapping("/jobs/{id}/status")
    public Map<String, Object> jobStatus(@PathVariable String id, @RequestBody StatusChange body) {
        return crm.changeJobStatus(id, body.status());
    }

    /**
     * People who can still be added to a job order.
     *
     * @param id job order id
     * @return person summaries
     */
    @GetMapping("/jobs/{id}/candidates-to-add")
    public List<Map<String, Object>> candidatesToAdd(@PathVariable String id) {
        return crm.candidatesToAdd(id);
    }

    /**
     * Adds a candidate to a job order.
     *
     * @param id   job order id
     * @param body the person to add
     * @return the new application view
     */
    @PostMapping("/jobs/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addApplication(@PathVariable String id, @RequestBody NewApplication body) {
        return crm.addApplication(id, body.personId());
    }

    /**
     * Moves a candidate to another stage.
     *
     * @param id   application id
     * @param body the stage and optional reason
     * @return the updated application view
     */
    @PatchMapping("/applications/{id}/stage")
    public Map<String, Object> moveApplication(@PathVariable String id, @RequestBody StageChange body) {
        return crm.moveApplication(id, body.stage(), body.reason());
    }

    /**
     * Changes a proposal's status.
     *
     * @param id   proposal id
     * @param body the requested status
     * @return the updated proposal
     */
    @PatchMapping("/proposals/{id}/status")
    public Map<String, Object> proposalStatus(@PathVariable String id, @RequestBody StatusChange body) {
        return crm.changeProposalStatus(id, body.status());
    }

    /**
     * Lists people in the network.
     *
     * @param q    free text
     * @param type person type filter
     * @return person summaries
     */
    @GetMapping("/people")
    public List<Map<String, Object>> people(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String type) {
        return crm.people(q, type);
    }

    /**
     * The person page.
     *
     * @param id person id
     * @return the person view
     */
    @GetMapping("/people/{id}")
    public Map<String, Object> person(@PathVariable String id) {
        return crm.person(id);
    }

    /**
     * Logs an activity.
     *
     * @param body account and/or person, type and note
     * @return the new activity
     */
    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public Activity logActivity(@RequestBody NewActivity body) {
        return crm.logActivity(body.accountId(), body.personId(), body.type(), body.note());
    }

    /**
     * Creates a next action for the current user.
     *
     * @param body title, due date and optional links
     * @return the new action
     */
    @PostMapping("/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAction(@RequestBody NewAction body) {
        return crm.createAction(body.title(), body.due(), body.accountId(), body.personId());
    }

    /**
     * Completes a next action.
     *
     * @param id action id
     * @return the updated action
     */
    @PostMapping("/actions/{id}/done")
    public Map<String, Object> completeAction(@PathVariable String id) {
        return crm.completeAction(id);
    }

    /**
     * Global search across accounts, people and job orders.
     *
     * @param q search text
     * @return grouped matches
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) String q) {
        return crm.search(q);
    }
}
