package ru.se.ifmo.is1.concurrency;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KeyLock {
    String value();
}
