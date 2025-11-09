package ru.se.ifmo.is1.concurrency;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Scope("singleton")
public class KeyedLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock get(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock(true));
    }
}
