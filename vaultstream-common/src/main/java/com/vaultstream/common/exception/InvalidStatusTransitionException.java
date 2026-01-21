package com.vaultstream.common.exception;

import lombok.Getter;

/**
 * Exception thrown when an invalid status transition is attempted.
 * 
 * For example, trying to activate a suspended customer directly
 * without going through the proper workflow.
 */
@Getter
public class InvalidStatusTransitionException extends VaultStreamException {

    private final String fromStatus;
    private final String toStatus;

    public InvalidStatusTransitionException(Object fromStatus, Object toStatus, String message) {
        super("INVALID_STATUS_TRANSITION", message);
        this.fromStatus = fromStatus != null ? fromStatus.toString() : "null";
        this.toStatus = toStatus != null ? toStatus.toString() : "null";
    }

    public InvalidStatusTransitionException(Object fromStatus, Object toStatus) {
        this(fromStatus, toStatus,
                String.format("Invalid status transition from %s to %s", fromStatus, toStatus));
    }
}
