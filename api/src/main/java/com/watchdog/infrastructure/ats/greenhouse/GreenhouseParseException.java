package com.watchdog.infrastructure.ats.greenhouse;

/** Raised when a Greenhouse payload can't be parsed. Lets the agent loop isolate
 * one source's failure and continue the cycle (spec §8.5). */
public class GreenhouseParseException extends RuntimeException {
    public GreenhouseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
