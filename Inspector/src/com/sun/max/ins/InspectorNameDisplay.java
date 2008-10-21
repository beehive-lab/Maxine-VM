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
package com.sun.max.ins;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleBytecodeBreakpoint.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;


/**
 * Standardized ways to display textual names of common entities during Inspection sessions.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class InspectorNameDisplay extends InspectionHolder {

    public InspectorNameDisplay(Inspection inspection) {
        super(inspection);
    }

    /**
     * Constants specifying where the return type should appear in the value returned by the methods in this class that
     * produce a display name for a method.
     */
    public enum ReturnTypeSpecification {
        /**
         * Denotes that the return type is to be omitted from the display name of a method.
         */
        ABSENT,

        /**
         * Denotes that the return type is to be prefixed to the display name of a method.
         */
        AS_PREFIX,

        /**
         * Denotes that the return type is to be suffixed to the display name of a method.
         */
        AS_SUFFIX;
    }

    /**
     * A standardized way to identify a heap object in the tele VM.
     *
     * @param prefix an optional string to precede everything else
     * @param teleObject an optional surrogate for the tele object being named, null if local
     * @param role an optional "role" name for low level Maxine objects whose implementation types aren't too interesting
     * @param type a name to describe the object, type name in simple cases
     * @return human readable string identifying an object in a standard format
     */
    public String longName(String prefix, TeleObject teleObject, String role, String type) {
        final StringBuilder name = new StringBuilder(32);
        if (prefix != null) {
            name.append(prefix);
        }

        if (teleObject != null) {
            name.append('<');
            name.append(teleObject.getOID());
            name.append('>');
        }
        if (role != null) {
            name.append(role);
            name.append('{');
            name.append(type);
            name.append('}');
        } else {
            name.append(type);
        }
        return name.toString();
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String shortName(TeleNativeThread teleNativeThread) {
        if (teleNativeThread == null) {
            return "null";
        }
        if (teleNativeThread == teleNativeThread.teleProcess().primordialThread()) {
            return "primordial";
        } else if (teleNativeThread.teleVmThread() != null) {
            return teleNativeThread.teleVmThread().name();
        }
        return "native unnamed";
    }

    /**
     * @return human readable string identifying a thread in a standard format.
     */
    public String longName(TeleNativeThread teleNativeThread) {
        if (teleNativeThread == null) {
            return "null";
        }
        return shortName(teleNativeThread) + " [" + teleNativeThread.id() + "]";
    }

    /**
     * E.g.: "[n]", where n is the index into the compilation history; first compilation n=0.
     */
    public String methodCompilationID(TeleTargetMethod teleTargetMethod) {
        if (teleTargetMethod != null) {
            final int compilationIndex = teleTargetMethod.getTeleClassMethodActor().indexOf(teleTargetMethod);
            if (compilationIndex >= 0) {
                return "[" + compilationIndex + "]";
            }
        }
        return "";
    }

    /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionShortAnnotation(TeleMethodActor teleMethodActor) {
        return teleMethodActor.isSubstituted() ? " *" : "";
    }

    /**
     * E.g. an asterisk when a method has been substituted.
     */
    public String methodSubstitutionLongAnnotation(TeleMethodActor teleMethodActor) {
        return teleMethodActor.isSubstituted() ? " substituted from " + teleMethodActor.teleClassActorSubstitutedFrom().getName() : "";
    }

    /**
     * E.g. "Element.foo()[0]"
     */
    public String veryShortName(TeleTargetMethod teleTargetMethod) {
        return teleTargetMethod.classMethodActor().format("%h.%n()" + methodCompilationID(teleTargetMethod));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]"
     *
     * @param returnTypeSpecification specifies where the return type should appear in the returned value
     */
    public String shortName(TeleTargetMethod teleTargetMethod, ReturnTypeSpecification returnTypeSpecification) {
        final ClassMethodActor classMethodActor = teleTargetMethod.classMethodActor();
        switch (returnTypeSpecification) {
            case ABSENT: {
                return classMethodActor.format("%n(%p)" + methodCompilationID(teleTargetMethod));
            }
            case AS_PREFIX: {
                return classMethodActor.format("%r %n(%p)" + methodCompilationID(teleTargetMethod));
            }
            case AS_SUFFIX: {
                return classMethodActor.format("%n(%p)" + methodCompilationID(teleTargetMethod) + " %r");
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    private String positionString(TeleTargetMethod teleTargetMethod, Address address) {
        final Pointer entry = teleTargetMethod.codeStart();
        final int position = address.minus(entry.asAddress()).toInt();
        return position == 0 ? "" : "+0x" + Integer.toHexString(position);
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0] in com.sun.max.ins.Bar"
     */
    public String longName(TeleTargetMethod teleTargetMethod) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + " in %H");
    }

    /**
     * E.g. "foo()[0]+0x7"
     */
    public String veryShortName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%n()" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]+0x7"
     */
    public String shortName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address));
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])[0]+0x7 in com.sun.max.ins.Bar"
     */
    public String longName(TeleTargetMethod teleTargetMethod, Address address) {
        return teleTargetMethod.classMethodActor().format("%r %n(%p)" + methodCompilationID(teleTargetMethod) + positionString(teleTargetMethod, address) + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])"
     */
    public String shortName(TeleClassMethodActor teleClassMethodActor, ReturnTypeSpecification returnTypeSpecification) {
        final ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
        switch (returnTypeSpecification) {
            case ABSENT: {
                return classMethodActor.format("%n(%p)");
            }
            case AS_PREFIX: {
                return classMethodActor.format("%r %n(%p)");
            }
            case AS_SUFFIX: {
                return classMethodActor.format("%n(%p) %r");
            }
            default: {
                throw ProgramError.unknownCase();
            }
        }
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     */
    public String longName(TeleClassMethodActor teleClassMethodActor) {
        return teleClassMethodActor.classMethodActor().format("%r %n(%p)" + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[]) in com.sun.max.ins.Bar"
     * E.g. "int foo(Pointer, Word, int[])+14 in com.sun.max.ins.Bar"
     */
    public String longName(BytecodeLocation bytecodeLocation) {
        final int position = bytecodeLocation.position();
        return bytecodeLocation.classMethodActor().format("%r %n(%p)" + (position != 0 ? " +" + bytecodeLocation.position() : "") + " in %H");
    }

    /**
     * E.g. "int foo(Pointer, Word, int[])  in com.sun.max.ins.Bar"
     * E.g. "int foo(Pointer, Word, int[])  +14 in com.sun.max.ins.Bar"
     */
    public String longName(Key key) {
        final StringBuilder name = new StringBuilder();
        name.append(key.signature().getResultDescriptor().toJavaString(false)).append(" ").append(key.name()).append(key.signature().toJavaString(false, false));
        if (key.position() != 0) {
            name.append(" +").append(key.position());
        }
        name.append(" in ").append(key.holder().toJavaString());
        return name.toString();
    }

    public String longName(TeleCodeLocation teleCodeLocation) {
        if (teleCodeLocation == null) {
            return "null";
        }
        final StringBuilder name = new StringBuilder();
        if (teleCodeLocation.hasTargetCodeLocation()) {
            final Address address = teleCodeLocation.targetCodeInstructionAddresss();
            name.append("Target{0x").append(address.toHexString());
            if (TeleNativeTargetRoutine.get(teleVM(), address) != null) {
                name.append("}");
            } else {
                final TeleTargetMethod teleTargetMethod = TeleTargetMethod.make(teleVM(), address);
                if (teleTargetMethod != null) {
                    name.append(",  ").append(longName(teleTargetMethod, address)).append("} ");
                } else {
                    name.append("}");
                }
            }
        }
        if (teleCodeLocation.hasBytecodeLocation()) {
            name.append("Bytecode{").append(teleCodeLocation.bytecodeLocation()).append("} ");
        } else if (teleCodeLocation.hasKey()) {
            name.append("Key{").append(longName(teleCodeLocation.key())).append("} ");
        }
        return name.toString();
    }
}
