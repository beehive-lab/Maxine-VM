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
/*VCSID=fa7fe11e-a920-4f8e-9ed0-4ea27d4a1882*/
package com.sun.max.vm.layout.ohm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class OhmFloatArrayLayout extends OhmPrimitiveArrayLayout<FloatValue> implements FloatArrayLayout {

    public OhmFloatArrayLayout(GripScheme gripScheme) {
        super(gripScheme, Kind.FLOAT);
    }

    @INLINE
    public final float getFloat(Accessor accessor, int index) {
        return accessor.getFloat(originDisplacement(), index);
    }

    @INLINE
    public final void setFloat(Accessor accessor, int index, float value) {
        accessor.setFloat(originDisplacement(), index, value);
    }

}
