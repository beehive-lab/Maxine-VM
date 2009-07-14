/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.target;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.sun.c1x.lir.*;

/**
 * The <code>Target</code> class represents the target of a compilation, including
 * the CPU architecture and other configuration information of the machine. Such
 * configuration information includes the size of pointers and references, alignment
 * of stacks, caches, etc.
 *
 * @author Ben L. Titzer
 */
public class Target {
    public final Architecture arch;
    public final Backend backend;

    public int referenceSize;
    public int stackAlignment;
    public int cacheAlignment;
    public int heapAlignment;

    public Target(Architecture arch) {
        this.arch = arch;
        referenceSize = arch.wordSize;
        stackAlignment = arch.wordSize;
        cacheAlignment = arch.wordSize;
        heapAlignment = arch.wordSize;
        backend = makeBackend(arch);
    }

    private Backend makeBackend(Architecture arch) {
        // load and instantiate the backend via reflection
        String className = "com.sun.c1x.target." + arch.backend + "." + arch.backend.toUpperCase() + "Backend";
        try {
            Class<?> javaClass = Class.forName(className);
            Constructor<?> constructor = javaClass.getDeclaredConstructor(Target.class);
            return (Backend) constructor.newInstance(this);
        } catch (InstantiationException e) {
            throw new Error("could not instantiate backend class: " + className);
        } catch (IllegalAccessException e) {
            throw new Error("could not access backend class: " + className);
        } catch (ClassNotFoundException e) {
            throw new Error("could not find backend class: " + className);
        } catch (NoSuchMethodException e) {
            throw new Error("could not find backend class constructor: " + className);
        } catch (InvocationTargetException e) {
            throw new Error("backend constructor threw an exception: " + e, e.getTargetException());
        }
    }

    /**
     * Checks whether this target requires special stack alignment, which may entail
     * padding stack frames and inserting alignment code.
     * @return <code>true</code> if this target requires special stack alignment
     * (i.e. {@link #stackAlignment} is greater than {@link #arch} the word size.
     */
    public boolean requiresStackAlignment() {
        return stackAlignment > arch.wordSize;
    }

    /**
     * Checks whether this target has compressed oops (i.e. 32-bit references
     * on a 64-bit machine).
     * @return <code>true</code> if this target has compressed oops
     */
    public boolean hasCompressedOops() {
        return referenceSize < arch.wordSize;
    }

    public Register jRarg0() {
        // TODO Auto-generated method stub
        return null;
    }

    public Register jRarg1() {
        // TODO Auto-generated method stub
        return null;
    }

    public Register jRarg2() {
        // TODO Auto-generated method stub
        return null;
    }

    public Register jRarg3() {
        // TODO Auto-generated method stub
        return null;
    }

    public Register jRarg4() {
        // TODO Auto-generated method stub
        return null;
    }

    public Register jRarg5() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean supportsSSE() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supports3DNOW() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsSSE2() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsLzcnt() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsCmov() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsMmx() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsSse42() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsMMX() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isIntel() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAmd() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsPopcnt() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsSse41() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isP6() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean supportsCx8() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isWin64() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isWindows() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSolaris() {
        // TODO Auto-generated method stub
        return false;
    }
}
