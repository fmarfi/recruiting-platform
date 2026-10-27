package com.recruiting.platform.pipeline;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.Role;
import java.util.UUID;

/**
 * One bulk action as the dispatcher hands it to a {@link BulkActionHandler}. The actor is also the current user of
 * the request thread.
 *
 * @param entity  the grid entity
 * @param action  the action
 * @param params  validated params
 * @param actorId the user who started the action
 * @param role    the highest role of the actor
 */
public record BulkCommand(ListEntity entity, BulkActionType action, BulkParams params, UUID actorId, Role role) {
}
