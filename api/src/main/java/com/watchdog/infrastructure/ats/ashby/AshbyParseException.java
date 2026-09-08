package com.watchdog.infrastructure.ats.ashby;

/** Raised when an Ashby payload can't be parsed. Lets the agent loop isolate one
 * source's failure and continue the cycle (spec §8.5). */
public class AshbyParseException extends RuntimeException {
    public AshbyParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
