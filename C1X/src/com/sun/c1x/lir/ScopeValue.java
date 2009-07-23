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
import com.sun.c1x.util.Util;

/**
 * The <code>ScopeValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public abstract class ScopeValue {

    public enum ScopeValueCode {
        LocationCode, ConstantIntCode, ConstantOopCode, ConstantLongCode, ConstantDoubleCode, ObjectCode, ObjectIdCode;

        public static ScopeValueCode fromInt(int value) {
            switch (value) {
                case 0:
                    return LocationCode;
                case 1:
                    return ConstantIntCode;
                case 2:
                    return ConstantOopCode;
                case 3:
                    return ConstantLongCode;
                case 4:
                    return ConstantDoubleCode;
                case 5:
                    return ObjectCode;
                case 6:
                    return ObjectIdCode;
                default:
                    Util.shouldNotReachHere();
            }
            return null;
        }
    }

    // Testers
    public boolean isLocation() {
        return false;
    }

    public boolean isObject() {
        return false;
    }

    public boolean isConstantInt() {
        return false;
    }

    public boolean isConstantDouble() {
        return false;
    }

    public boolean isConstantLong() {
        return false;
    }

    public boolean isConstantOop() {
        return false;
    }

    public boolean equals(ScopeValue other) {
        return false;
    }

    // Serialization of debugging information
    public abstract void writeOn(DebugInfoWriteStream stream);

    static ScopeValue readFrom(DebugInfoReadStream stream) {
        ScopeValue result = null;
        switch (stream.readScopeValueCode()) {
            case LocationCode:
                result = new LocationValue(stream);
                break;
            case ConstantIntCode:
                result = new ConstantIntValue(stream);
                break;
            case ConstantOopCode:
                result = new ConstantOopReadValue(stream);
                break;
            case ConstantLongCode:
                result = new ConstantLongValue(stream);
                break;
            case ConstantDoubleCode:
                result = new ConstantDoubleValue(stream);
                break;
            case ObjectCode:
                result = stream.readObjectValue();
                break;
            case ObjectIdCode:
                result = stream.getCachedObject();
                break;
            default:
                Util.shouldNotReachHere();
        }
        return result;
    }

    public abstract void printOn(LogStream out);
}
