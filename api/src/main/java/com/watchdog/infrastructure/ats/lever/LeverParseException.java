package com.watchdog.infrastructure.ats.lever;

/** Raised when a Lever payload can't be parsed. Lets the agent loop isolate one
 * source's failure and continue the cycle (spec §8.5). */
public class LeverParseException extends RuntimeException {
    public LeverParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
