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
/*VCSID=6a18cac1-6cc5-4898-997f-4dc226aed748*/
package com.sun.max.util;

import java.util.*;

import com.sun.max.program.*;


/**
 * The {@code Registry} class implements a type of configuration mechanism
 * that allows a short string name (called an alias) to refer to a class name.
 * The short name can be used to quickly look up an internally registered class and
 * instantiate it (assuming the target class as a public constructor which takes
 * no parameters). If the alias is not registered, the registry will try to use
 * the alias as a fully-qualified Java class and load it using reflection.
 *
 * @author "Ben L. Titzer"
 */
public class Registry<Class_Type> {

    protected final boolean _loadClass;
    protected final Class<Class_Type> _classClass;
    protected final Map<String, Class<? extends Class_Type>> _classMap;
    protected final Map<String, Class_Type> _objectMap;
    protected final Map<String, String> _stringMap;

    public Registry(Class<Class_Type> classType, boolean loadClass) {
        _loadClass = loadClass;
        _classClass = classType;
        _classMap = new HashMap<String, Class<? extends Class_Type>>();
        _objectMap = new HashMap<String, Class_Type>();
        _stringMap = new HashMap<String, String>();
    }

    public void registerObject(String alias, Class_Type object) {
        _objectMap.put(alias, object);
    }

    public void registerClass(String alias, Class<? extends Class_Type> classType) {
        _classMap.put(alias, classType);
    }

    public void registerClass(String alias, String className) {
        _stringMap.put(alias, className);
    }

    public Class_Type getInstance(String alias) {
        return getInstance(alias, true);
    }

    public Class_Type getInstance(String alias, boolean fatal) {
        final Class_Type object = _objectMap.get(alias);
        if (object != null) {
            return object;
        }
        Class<? extends Class_Type> classRef = _classMap.get(alias);
        String className = alias;
        try {
            if (classRef == null) {
                className = _stringMap.get(alias);
                if (className != null) {
                    classRef = Class.forName(className).asSubclass(_classClass);
                } else if (_loadClass) {
                    classRef = Class.forName(alias).asSubclass(_classClass);
                } else {
                    return genError(fatal, "cannot find alias", alias, className);
                }
            }
            className = classRef.getName();
            return classRef.newInstance();
        } catch (ClassNotFoundException e) {
            return genError(fatal, "cannot find class", alias, className);
        } catch (InstantiationException e) {
            return genError(fatal, "cannot instantiate class", alias, className);
        } catch (IllegalAccessException e) {
            return genError(fatal, "cannot instantiate class", alias, className);
        } catch (ClassCastException e) {
            return genError(fatal, "not a subclass of " + _classClass.getName(), alias, className);
        }
    }

    public Iterable<String> getAliases() {
        final LinkedList<String> lista = new LinkedList<String>();
        lista.addAll(_objectMap.keySet());
        lista.addAll(_classMap.keySet());
        lista.addAll(_stringMap.keySet());
        return lista;
    }

    private Class_Type genError(boolean fatal, String message, String alias, String className) {
        if (!fatal) {
            return null;
        }
        String mstr = message + ": " + alias;
        if (className != null) {
            mstr = mstr + "(" + className + ")";
        }
        throw ProgramError.unexpected(mstr);
    }

    public static <T> Registry<T> newRegistry(Class<T> cl) {
        return new Registry<T>(cl, true);
    }
}
