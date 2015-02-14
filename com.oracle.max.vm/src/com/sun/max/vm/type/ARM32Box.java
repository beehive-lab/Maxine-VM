package com.sun.max.vm.type;

import com.sun.max.platform.Platform;

/**
 * Created by andyn on 14/02/15.
 */
public class ARM32Box {
    private Object value;

    public ARM32Box(Object x) {
        value = x;
    }

    @Override
    public int hashCode() {
        if (Platform.target().arch.is32bit()) {
            return (0xfffff & System.identityHashCode(value));
        } else {
            return System.identityHashCode(value);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        if (this.value == ((ARM32Box) obj).value) {
            return true;
        } else {
            return false;
        }

    }

    public Object get() {
        return value;
    }

}
