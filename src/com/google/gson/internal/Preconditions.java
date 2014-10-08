package com.google.gson.internal;

/**
 * A simple utility class used to check method Preconditions.
 * 
 * <pre> 
 * public long divideBy(long value) { 
 *   Preconditions.checkArgument(value != 0); 
 *   return this.value / value; 
 * } 
 * </pre>
 * 
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class Preconditions {
    public static <T> T checkNotNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static void checkArgument(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }
}
