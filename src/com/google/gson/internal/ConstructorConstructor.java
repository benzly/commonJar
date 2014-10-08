package com.google.gson.internal;

import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Returns a function that can construct an instance of a requested type.
 */
public final class ConstructorConstructor {
    private final Map<Type, InstanceCreator<?>> instanceCreators;

    public ConstructorConstructor(Map<Type, InstanceCreator<?>> instanceCreators) {
        this.instanceCreators = instanceCreators;
    }

    public ConstructorConstructor() {
        this(Collections.<Type, InstanceCreator<?>>emptyMap());
    }

    public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
        final Type type = typeToken.getType();
        final Class<? super T> rawType = typeToken.getRawType();

        // first try an instance creator

        @SuppressWarnings("unchecked")
        // types must agree
        final InstanceCreator<T> creator = (InstanceCreator<T>) instanceCreators.get(type);
        if (creator != null) {
            return new ObjectConstructor<T>() {
                @Override
                public T construct() {
                    return creator.createInstance(type);
                }
            };
        }

        ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType);
        if (defaultConstructor != null) {
            return defaultConstructor;
        }

        ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(rawType);
        if (defaultImplementation != null) {
            return defaultImplementation;
        }

        // finally try unsafe
        return newUnsafeAllocator(type, rawType);
    }

    private <T> ObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {
        try {
            final Constructor<? super T> constructor = rawType.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return new ObjectConstructor<T>() {
                @Override
                @SuppressWarnings("unchecked")
                // T is the same raw type as is requested
                public T construct() {
                    try {
                        Object[] args = null;
                        return (T) constructor.newInstance(args);
                    } catch (InstantiationException e) {
                        // TODO: JsonParseException ?
                        throw new RuntimeException("Failed to invoke " + constructor + " with no args", e);
                    } catch (InvocationTargetException e) {
                        // TODO: don't wrap if cause is unchecked!
                        // TODO: JsonParseException ?
                        throw new RuntimeException("Failed to invoke " + constructor + " with no args", e.getTargetException());
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Constructors for common interface types like Map and List and their subytpes.
     */
    @SuppressWarnings("unchecked")
    // use runtime checks to guarantee that 'T' is what it is
    private <T> ObjectConstructor<T> newDefaultImplementationConstructor(Class<? super T> rawType) {
        if (Collection.class.isAssignableFrom(rawType)) {
            if (SortedSet.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new TreeSet<Object>();
                    }
                };
            } else if (Set.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new LinkedHashSet<Object>();
                    }
                };
            } else if (Queue.class.isAssignableFrom(rawType)) {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new LinkedList<Object>();
                    }
                };
            } else {
                return new ObjectConstructor<T>() {
                    @Override
                    public T construct() {
                        return (T) new ArrayList<Object>();
                    }
                };
            }
        }

        if (Map.class.isAssignableFrom(rawType)) {
            return new ObjectConstructor<T>() {
                @Override
                public T construct() {
                    // TODO: if the map's key type is a string, should this be StringMap?
                    return (T) new LinkedHashMap<Object, Object>();
                }
            };
            // TODO: SortedMap ?
        }

        return null;
    }

    private <T> ObjectConstructor<T> newUnsafeAllocator(final Type type, final Class<? super T> rawType) {
        return new ObjectConstructor<T>() {
            private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();

            @Override
            @SuppressWarnings("unchecked")
            public T construct() {
                try {
                    Object newInstance = unsafeAllocator.newInstance(rawType);
                    return (T) newInstance;
                } catch (Exception e) {
                    throw new RuntimeException(
                            ("Unable to invoke no-args constructor for " + type + ". " + "Register an InstanceCreator with Gson for this type may fix this problem."),
                            e);
                }
            }
        };
    }

    @Override
    public String toString() {
        return instanceCreators.toString();
    }
}
