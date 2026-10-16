package com.recruiting.demo.domain;

/**
 * A relationship between two people in the network (requirement A-13).
 *
 * @param fromId the first person
 * @param toId   the second person
 * @param kind   the relationship, e.g. "introduced", "worked with", "knows"
 */
public record Link(String fromId, String toId, String kind) {
}
