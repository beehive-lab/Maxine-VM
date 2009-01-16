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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Utf8Constant} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class TeleUtf8Constant extends TelePoolConstant {

    protected TeleUtf8Constant(TeleVM teleVM, Reference utf8ConstantReference) {
        super(teleVM, utf8ConstantReference);
    }

    // The field is final; cache it.
    private String _string;

    /**
     * @return a local copy of the string contained in this object in the {@link TeleVM}.
     */
    public String getString() {
        if (_string == null) {
            final Reference stringReference = teleVM().fields().Utf8Constant_string.readReference(reference());
            final TeleString teleString = (TeleString) makeTeleObject(stringReference);
            _string = teleString.getString();
        }
        return _string;
    }

    @Override
    protected Object createDeepCopy(DeepCopyContext context) {
        // Translate into local equivalent
        return SymbolTable.makeSymbol(getString());
    }

    @Override
    public String maxineRole() {
        return "Utf8Constant";
    }

    @Override
    public String maxineTerseRole() {
        return "Utf8Const";
    }

}
