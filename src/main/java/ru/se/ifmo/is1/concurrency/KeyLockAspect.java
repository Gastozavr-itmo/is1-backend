package ru.se.ifmo.is1.concurrency;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class KeyLockAspect {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock(true));
    }


    @Around("@annotation(ru.se.ifmo.is1.concurrency.KeyLock)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        KeyLock keyLock = method.getAnnotation(KeyLock.class);
        if (keyLock == null) {
            return pjp.proceed();
        }
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        ctx.setVariable("args", args);
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("p" + i, args[i]);
        }

        Expression expr = parser.parseExpression(keyLock.value());
        Object raw = expr.getValue(ctx);
        List<String> keys = toKeyList(raw);

        Collections.sort(keys);

        List<ReentrantLock> acquired = new ArrayList<>(keys.size());
        try {
            for (String k : keys) {
                ReentrantLock lock = getLock(k);
                lock.lock();
                acquired.add(lock);
            }

            return pjp.proceed();
        } finally {
            for (int i = acquired.size() - 1; i >= 0; i--) {
                acquired.get(i).unlock();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static List<String> toKeyList(Object v) {
        List<String> res = new ArrayList<>();

        if (v == null) {
            return res;
        }

        if (v instanceof String s) {
            res.add(s);
            return res;
        }

        if (v instanceof Collection coll) {
            for (Object o : coll) {
                res.add(Objects.toString(o));
            }
            return res;
        }

        if (v.getClass().isArray()) {
            int n = Array.getLength(v);
            for (int i = 0; i < n; i++) {
                res.add(Objects.toString(Array.get(v, i)));
            }
            return res;
        }

        res.add(Objects.toString(v));
        return res;
    }
}
