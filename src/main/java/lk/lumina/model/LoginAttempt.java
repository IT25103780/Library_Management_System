package lk.lumina.model;

/** An authentication rate-limit counter and the start of its time window. */
public record LoginAttempt(int count, long start) {}
