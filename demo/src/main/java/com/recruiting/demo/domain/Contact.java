package com.recruiting.demo.domain;

/**
 * Links a person from the network to a client account with their role there
 * (requirement A-03).
 *
 * @param accountId the account
 * @param personId  the person in the network
 * @param role      role at the client, e.g. "HR Director"
 * @param primary   whether this is the main contact for the account
 */
public record Contact(String accountId, String personId, String role, boolean primary) {
}
