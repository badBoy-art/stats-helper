/**
 * 
 */
package com.github.phantomthief.stats.n.profiler.util;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.phantomthief.stats.n.profiler.anntation.Max;
import com.github.phantomthief.stats.n.profiler.anntation.Sum;
import com.github.phantomthief.stats.n.profiler.stats.Stats;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

/**
 * @author w.vela
 */
public class StatsMergeUtils {

    private static final Set<Class<?>> SUPPORT_CLASS = ImmutableSet.of(Number.class, Integer.TYPE,
            Long.TYPE);

    public static <T extends Stats> T annotationBasedMerge(T base, T other) {
        Field[] fields = base.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!SUPPORT_CLASS.stream().anyMatch(type -> type.isAssignableFrom(field.getType()))) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (field.isAnnotationPresent(Sum.class)) {
                doAggregation(field, base, other);
            } else if (field.isAnnotationPresent(Max.class)) {
                doMaxValue(field, base, other);
            } else if (field.getDeclaringClass().isAnnotationPresent(Sum.class)) {
                doAggregation(field, base, other);
            } else if (field.getDeclaringClass().isAnnotationPresent(Max.class)) {
                doMaxValue(field, base, other);
            } else {
                throw new IllegalArgumentException("no annotation found in field:" + field);
            }
        }
        return base;
    }

    private static void doMaxValue(Field field, Object base, Object other) {
        if (field.getType() == Integer.TYPE) {
            try {
                int int1 = field.getInt(base);
                int int2 = field.getInt(other);
                field.set(base, Math.max(int1, int2));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Throwables.propagate(e);
            }
        } else if (field.getType() == Long.TYPE) {
            try {
                long long1 = field.getLong(base);
                long long2 = field.getLong(other);
                field.set(base, Math.max(long1, long2));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private static void doAggregation(Field field, Object base, Object other) {
        if (field.getType() == AtomicInteger.class) {
            try {
                AtomicInteger atomicInteger = (AtomicInteger) field.get(base);
                atomicInteger.addAndGet(((AtomicInteger) field.get(other)).get());
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Throwables.propagate(e);
            }
        } else if (field.getType() == AtomicLong.class) {
            try {
                AtomicLong atomicLong = (AtomicLong) field.get(base);
                atomicLong.addAndGet(((AtomicLong) field.get(other)).get());
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
