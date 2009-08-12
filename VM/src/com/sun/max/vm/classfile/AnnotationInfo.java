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
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class AnnotationInfo {

    private final TypeDescriptor annotationTypeDescriptor;

    public TypeDescriptor annotationTypeDescriptor() {
        return annotationTypeDescriptor;
    }

    /**
     * The spec calls this 'element_value'.
     */
    public abstract static class Element {
        private Element() {
        }
    }

    public static final class ValueElement extends Element {
        private final Value value;

        public Value value() {
            return value;
        }

        private ValueElement(Value value) {
            this.value = value;
        }
    }

    public static final class StringElement extends Element {
        private final String string;

        public String string() {
            return string;
        }

        private StringElement(String string) {
            this.string = string;
        }
    }

    public static final class EnumElement extends Element {
        private final TypeDescriptor enumConstantTypeDescriptor;
        private final String enumConstantName;

        public TypeDescriptor enumConstantTypeDescriptor() {
            return enumConstantTypeDescriptor;
        }

        public String enumConstantName() {
            return enumConstantName;
        }

        private EnumElement(TypeDescriptor enumConstantTypeDescriptor, String enumConstantName) {
            this.enumConstantTypeDescriptor = enumConstantTypeDescriptor;
            this.enumConstantName = enumConstantName;
        }
    }

    public static final class TypeElement extends Element {
        private final TypeDescriptor typeDescriptor;

        public TypeDescriptor typeDescriptor() {
            return typeDescriptor;
        }

        private TypeElement(TypeDescriptor typeDescriptor) {
            this.typeDescriptor = typeDescriptor;
        }
    }

    public static final class AnnotationElement extends Element {
        private final AnnotationInfo annotationInfo;

        public AnnotationInfo annotationInfo() {
            return annotationInfo;
        }

        private AnnotationElement(AnnotationInfo annotationInfo) {
            this.annotationInfo = annotationInfo;
        }
    }

    public static final class ArrayElement extends Element {
        private final Element[] arrayElements;

        public Element[] arrayElements() {
            return arrayElements;
        }

        private ArrayElement(Element[] arrayElements) {
            this.arrayElements = arrayElements;
        }
    }

    static Element createElement(ClassfileStream classfileStream, ConstantPool constantPool) {
        final int tag = classfileStream.readUnsigned1();
        switch (tag) { // table #4.8
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I': {
                final int constValueIndex = classfileStream.readUnsigned2();
                return new ValueElement(IntValue.from(constantPool.intAt(constValueIndex, "integer value")));
            }
            case 'J': {
                final int constValueIndex = classfileStream.readUnsigned2();
                return new ValueElement(LongValue.from(constantPool.longAt(constValueIndex, "long value")));
            }
            case 'D': {
                final int constValueIndex = classfileStream.readUnsigned2();
                return new ValueElement(DoubleValue.from(constantPool.doubleAt(constValueIndex, "double value")));
            }
            case 'F': {
                final int constValueIndex = classfileStream.readUnsigned2();
                return new ValueElement(FloatValue.from(constantPool.floatAt(constValueIndex, "float value")));
            }
            case 's': {
                final int constValueIndex = classfileStream.readUnsigned2();
                final String s = constantPool.utf8At(constValueIndex, "string value").toString();
                return new StringElement(s);
            }
            case 'e': {
                final int typeNameIndex = classfileStream.readUnsigned2();
                final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(constantPool.utf8At(typeNameIndex, "enum constant type").toString());
                final int constNameIndex = classfileStream.readUnsigned2();
                final String enumConstantName = constantPool.utf8At(constNameIndex, "enum constant value").toString();
                return new EnumElement(typeDescriptor, enumConstantName);
            }
            case 'c': {
                final int classInfoIndex = classfileStream.readUnsigned2();
                final TypeDescriptor typeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(constantPool.utf8At(classInfoIndex, "class type").toString());
                return new TypeElement(typeDescriptor);
            }
            case '@': {
                final AnnotationInfo annotationInfo = new AnnotationInfo(classfileStream, constantPool);
                return new AnnotationElement(annotationInfo);
            }
            case '[': {
                final int numberOfValues = classfileStream.readUnsigned2();
                final Element[] elements = new Element[numberOfValues];
                for (int i = 0; i < numberOfValues; i++) {
                    elements[i] = createElement(classfileStream, constantPool);
                }
                return new ArrayElement(elements);
            }
            default: {
                throw classFormatError("Invalid annotation element tag (" + (char) tag + ")");
            }
        }
    }

    /**
     * The spec calls this 'element_value_pair'.
     */
    public final class NameElementPair {
        final String name;
        final Element element;

        public String name() {
            return name;
        }

        public Element element() {
            return element;
        }

        private NameElementPair(String name, Element element) {
            this.name = name;
            this.element = element;
        }
    }

    private final NameElementPair[] nameElementPairs;

    public NameElementPair[] nameElementPairs() {
        return nameElementPairs;
    }

    public AnnotationInfo(ClassfileStream classfileStream, ConstantPool constantPool) {
        final int typeIndex = classfileStream.readUnsigned2();
        annotationTypeDescriptor = JavaTypeDescriptor.parseTypeDescriptor(constantPool.utf8At(typeIndex, "annotation type").toString());
        final int numberOfNameElementPairs = classfileStream.readUnsigned2();
        nameElementPairs = new NameElementPair[numberOfNameElementPairs];
        for (int i = 0; i < numberOfNameElementPairs; i++) {
            final int elementNameIndex = classfileStream.readUnsigned2();
            final String name = constantPool.utf8At(elementNameIndex, "element name").toString();
            final Element element = createElement(classfileStream, constantPool);
            nameElementPairs[i] = new NameElementPair(name, element);
        }
    }

    public static AnnotationInfo[] parse(ClassfileStream classfileStream, ConstantPool constantPool) {
        final int numberOfAnnotationInfos = classfileStream.readUnsigned2();
        final AnnotationInfo[] annotationInfos = new AnnotationInfo[numberOfAnnotationInfos];
        for (int i = 0; i < numberOfAnnotationInfos; i++) {
            annotationInfos[i] = new AnnotationInfo(classfileStream, constantPool);
        }
        return annotationInfos;
    }

}
