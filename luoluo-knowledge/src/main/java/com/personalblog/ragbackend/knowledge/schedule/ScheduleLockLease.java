package com.personalblog.ragbackend.knowledge.schedule;

/**
 * 调度锁租约
 */
public record ScheduleLockLease(String scheduleId, String lockToken) {
}
