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

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.protocol;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.constants.*;
@SuppressWarnings("unused")

public final class ClassLoaderReferenceCommands {
    public static final int COMMAND_SET = 14;
    private ClassLoaderReferenceCommands() { }  // hide constructor

    public static class VisibleClasses {
        public static final byte COMMAND = 1;
        public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {
            public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }
            public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {assert false : "If this method can be called, it must be overwritten in subclasses!"; return 0; }
            public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }
            public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }
        }

        public static class IncomingRequest implements IncomingData {
            public ID.ClassLoaderID classLoaderObject;
            public IncomingRequest(ID.ClassLoaderID classLoaderObject) {
                this.classLoaderObject = classLoaderObject;
            }
            public IncomingRequest() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                classLoaderObject = ID.read(ps.getInputStream(), ID.ClassLoaderID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                classLoaderObject.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classLoaderObject=" + classLoaderObject);
                return stringBuilder.toString();
            }
        }

        public static class ClassInfo {

            public byte refTypeTag;

            public ID.ReferenceTypeID typeID;
            public ClassInfo(byte refTypeTag,
                ID.ReferenceTypeID typeID) {
                this.refTypeTag = refTypeTag;
                this.typeID = typeID;
            }
            public ClassInfo() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                refTypeTag = ps.readByte();
                typeID = ID.read(ps.getInputStream(), ID.ReferenceTypeID.class);
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(refTypeTag);
                typeID.write(ps.getOutputStream());
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("refTypeTag=" + refTypeTag);
                stringBuilder.append(", ");
                stringBuilder.append("typeID=" + typeID);
                return stringBuilder.toString();
            }
        }

        public static class Reply implements OutgoingData {
            public byte getCommandId() { return COMMAND; }
            public byte getCommandSetId() { return COMMAND_SET; }

            public ClassInfo[] classes;
            public Reply(ClassInfo[] classes) {
                this.classes = classes;
            }
            public Reply() {
            }
            public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {
                final int classesCount = ps.readInt();
                classes = new ClassInfo[classesCount];
                for (int i = 0; i < classesCount; i++) {
                    classes[i] = new ClassInfo();
                    classes[i].read(ps);
                }
            }
            public void write(JDWPOutputStream ps) throws java.io.IOException {
                ps.write(classes.length);
                for (int i = 0; i < classes.length; i++) {
                    classes[i].write(ps);
                }
            }
            @Override
            public String toString() {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("classes=[" + classes.length + "]{");
                for (int i = 0; i < classes.length; i++) {
                    if (i != 0) { stringBuilder.append(", "); }
                    stringBuilder.append("classes[i]=" + classes[i]);
                }
                stringBuilder.append("}");
                return stringBuilder.toString();
            }
        }
    }
}
