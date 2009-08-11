/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.collect;

import java.util.*;

/**
 * This class provides a skeletal implementation of the {@link Mapping} interface, to minimize the effort required to
 * implement this interface.
 * <p>
 * This class also includes a number of factory methods for creating {@code Mapping} instances with various properties.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class HashMapping<Key_Type, Value_Type> implements Mapping<Key_Type, Value_Type> {

    private final HashEquivalence<Key_Type> equivalence;

    /**
     * Determines if two given keys are equal.
     * <p>
     * Subclasses override this method to define equivalence without delegating to a {@link HashEquivalence} object.
     */
    protected boolean equivalent(Key_Type key1, Key_Type key2) {
        return equivalence.equivalent(key1, key2);
    }

    /**
     * Computes a hash code for a given key.
     * <p>
     * Subclasses override this method to compute a hash code without delegating to a {@link HashEquivalence} object.
     */
    protected int hashCode(Key_Type key) {
        // Don't guard against a negative number here as the caller needs to convert the hash code into a valid index
        // which will involve range checking anyway
        return equivalence.hashCode(key);
    }

    /**
     * Creates a hash table.
     *
     * @param equivalence
     *            the semantics to be used for comparing keys. If {@code null} is provided, then {@link HashEquality} is
     *            used.
     */
    protected HashMapping(HashEquivalence<Key_Type> equivalence) {
        if (equivalence == null) {
            final Class<HashEquality<Key_Type>> type = null;
            this.equivalence = HashEquality.instance(type);
        } else {
            this.equivalence = equivalence;
        }
    }

    public boolean containsKey(Key_Type key) {
        return get(key) != null;
    }

    protected abstract class HashMappingIterable<Type> implements IterableWithLength<Type> {
        public int length() {
            return HashMapping.this.length();
        }
    }

    /**
     * Gets an iterator over the values in this mapping by looking up each {@linkplain #keys() key}.
     * <p>
     * Subclasses will most likely override this method with a more efficient implementation.
     */
    public IterableWithLength<Value_Type> values() {
        return new HashMappingIterable<Value_Type>() {
            private final IterableWithLength<Key_Type> keys = keys();
            public Iterator<Value_Type> iterator() {
                return new Iterator<Value_Type>() {
                    private final Iterator<Key_Type> keyIterator = keys.iterator();

                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }

                    public Value_Type next() {
                        return get(keyIterator.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static <Key_Type, Value_Type> GrowableMapping<Key_Type, Value_Type> createMapping(HashEquivalence<Key_Type> equivalence) {
        return new OpenAddressingHashMapping<Key_Type, Value_Type>(equivalence);
    }

    public static <Key_Type, Value_Type> GrowableMapping<Key_Type, Value_Type> createIdentityMapping() {
        final Class<HashIdentity<Key_Type>> type = null;
        return createMapping(HashIdentity.instance(type));
    }

    public static <Key_Type, Value_Type> GrowableMapping<Key_Type, Value_Type> createEqualityMapping() {
        final Class<HashEquality<Key_Type>> type = null;
        return createMapping(HashEquality.instance(type));
    }

    public static <Key_Type, Value_Type> VariableMapping<Key_Type, Value_Type> createVariableMapping(HashEquivalence<Key_Type> equivalence) {
        return new ChainedHashMapping<Key_Type, Value_Type>(equivalence);
    }

    public static <Key_Type, Value_Type> VariableMapping<Key_Type, Value_Type> createVariableIdentityMapping() {
        final Class<HashIdentity<Key_Type>> type = null;
        return createVariableMapping(HashIdentity.instance(type));
    }

    public static <Key_Type, Value_Type> VariableMapping<Key_Type, Value_Type> createVariableEqualityMapping() {
        final Class<HashEquality<Key_Type>> type = null;
        return createVariableMapping(HashEquality.instance(type));
    }
}
