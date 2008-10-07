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
package com.sun.max.vm.actor.holder;

import static com.sun.max.vm.actor.holder.ClassActorFactory.*;

import java.lang.annotation.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representations of "primitive classes" such as 'int', 'char', etc.
 *
 * @author Bernd Mathiske
 */
public final class PrimitiveClassActor<Value_Type extends Value<Value_Type>> extends ClassActor {


    private final Kind<Value_Type> _kind;

    @PROTOTYPE_ONLY
    PrimitiveClassActor(Kind<Value_Type> kind) {
        super(kind,
              NO_SPECIFIC_LAYOUT,
              null, // primitive classes do not have a class loader
              kind.name(),
              NO_MAJOR_VERSION,
              NO_MINOR_VERSION,
              ACC_PUBLIC | ACC_FINAL,
              kind.typeDescriptor(),
              NO_SUPER_CLASS_ACTOR,
              NO_COMPONENT_CLASS_ACTOR,
              InterfaceActor.NONE,
              FieldActor.NONE,
              MethodActor.NONE,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
        _kind = kind;
    }

    public TupleClassActor toWrapperClassActor() {
        return (TupleClassActor) ClassActor.fromJava(_kind.boxedClass());
    }

    @Override
    public boolean isPrimitiveClassActor() {
        return true;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return null;
    }

    public static final PrimitiveClassActor<VoidValue> VOID_CLASS_ACTOR = createPrimitiveClassActor(Kind.VOID);

    public static final PrimitiveClassActor<ByteValue> BYTE_CLASS_ACTOR = createPrimitiveClassActor(Kind.BYTE);
    public static final PrimitiveClassActor<BooleanValue> BOOLEAN_CLASS_ACTOR = createPrimitiveClassActor(Kind.BOOLEAN);
    public static final PrimitiveClassActor<ShortValue> SHORT_CLASS_ACTOR = createPrimitiveClassActor(Kind.SHORT);
    public static final PrimitiveClassActor<CharValue> CHAR_CLASS_ACTOR = createPrimitiveClassActor(Kind.CHAR);
    public static final PrimitiveClassActor<IntValue> INT_CLASS_ACTOR = createPrimitiveClassActor(Kind.INT);
    public static final PrimitiveClassActor<FloatValue> FLOAT_CLASS_ACTOR = createPrimitiveClassActor(Kind.FLOAT);
    public static final PrimitiveClassActor<LongValue> LONG_CLASS_ACTOR = createPrimitiveClassActor(Kind.LONG);
    public static final PrimitiveClassActor<DoubleValue> DOUBLE_CLASS_ACTOR = createPrimitiveClassActor(Kind.DOUBLE);

    public static final ArrayClassActor<ByteValue> BYTE_ARRAY_CLASS_ACTOR = createArrayClassActor(BYTE_CLASS_ACTOR);
    public static final ArrayClassActor<BooleanValue> BOOLEAN_ARRAY_CLASS_ACTOR = createArrayClassActor(BOOLEAN_CLASS_ACTOR);
    public static final ArrayClassActor<ShortValue> SHORT_ARRAY_CLASS_ACTOR = createArrayClassActor(SHORT_CLASS_ACTOR);
    public static final ArrayClassActor<CharValue> CHAR_ARRAY_CLASS_ACTOR = createArrayClassActor(CHAR_CLASS_ACTOR);
    public static final ArrayClassActor<IntValue> INT_ARRAY_CLASS_ACTOR = createArrayClassActor(INT_CLASS_ACTOR);
    public static final ArrayClassActor<FloatValue> FLOAT_ARRAY_CLASS_ACTOR = createArrayClassActor(FLOAT_CLASS_ACTOR);
    public static final ArrayClassActor<LongValue> LONG_ARRAY_CLASS_ACTOR = createArrayClassActor(LONG_CLASS_ACTOR);
    public static final ArrayClassActor<DoubleValue> DOUBLE_ARRAY_CLASS_ACTOR = createArrayClassActor(DOUBLE_CLASS_ACTOR);

    public static final PrimitiveClassActor[] PRIMITIVE_CLASS_ACTORS = {
        BYTE_CLASS_ACTOR,
        BOOLEAN_CLASS_ACTOR,
        SHORT_CLASS_ACTOR,
        CHAR_CLASS_ACTOR,
        INT_CLASS_ACTOR,
        FLOAT_CLASS_ACTOR,
        LONG_CLASS_ACTOR,
        DOUBLE_CLASS_ACTOR
    };
}
