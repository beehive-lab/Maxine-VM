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
package com.sun.c1x.lir;

import com.sun.c1x.debug.LogStream;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ObjectValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class ObjectValue extends ScopeValue {

    private int id;
    private ScopeValue klass;
    private List<ScopeValue> fieldValues;
    private Object value;
    private boolean visited;

    public ObjectValue(int id, ScopeValue klass) {
        this.id = id;
        this.klass = klass;
        fieldValues = new ArrayList<ScopeValue>();
        this.value = new Object();
        this.visited = false;
        assert klass.isConstantOop() : "Should be constant Klass oop";
    }

    public ObjectValue(int id) {
        this.id = id;
        this.klass = null;
        fieldValues = new ArrayList<ScopeValue>();
        this.value = new Object();
        this.visited = false;
    }

    // Accessors
    @Override
    public boolean isObject() {
        return true;
    }

    public int id() {
        return id;
    }

    public ScopeValue klass() {
        return klass;
    }

    public List<ScopeValue> fieldValues() {
        return fieldValues;
    }

    public ScopeValue fieldAt(int i) {
        return fieldValues.get(i);
    }

    public int fieldSize() {
        return fieldValues.size();
    }

    public Object value() {
        return value;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setVisited(boolean visited) {
        visited = false;
    }

    // Serialization of debugging information
    public void readObject(DebugInfoReadStream stream) {
        klass = readFrom(stream);
        assert klass.isConstantOop() : "should be constant klass oop";
        int length = stream.readInt();
        for (int i = 0; i < length; i++) {
            ScopeValue val = readFrom(stream);
            fieldValues.add(val);
        }
    }

    @Override
    public void writeOn(DebugInfoWriteStream stream) {
        if (visited) {
            stream.writeInt(ScopeValueCode.ObjectIdCode.ordinal());
            stream.writeInt(id);
        } else {
            visited = true;
            stream.writeInt(ScopeValueCode.ObjectIdCode.ordinal());
            stream.writeInt(id);
            klass.writeOn(stream);
            int length = fieldValues.size();
            stream.writeInt(length);
            for (int i = 0; i < length; i++) {
                fieldValues.get(i).writeOn(stream);
            }
        }
    }

    // Printing
    @Override
    public void printOn(LogStream st) {
        st.printf("obj[%d]", id);
    }

    public void printFieldsOn(LogStream st) {
        if (fieldValues.size() > 0) {
            fieldValues.get(0).printOn(st);
        }
        for (int i = 1; i < fieldValues.size(); i++) {
            st.print(", ");
            fieldValues.get(i).printOn(st);
        }
    }
}
