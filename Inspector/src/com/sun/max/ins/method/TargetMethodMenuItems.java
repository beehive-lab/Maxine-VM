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
package com.sun.max.ins.method;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.debug.*;


/**
 * Provides the menu items related to a {@link TeleTargetMethod}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TargetMethodMenuItems extends AbstractInspectionHolder implements InspectorMenuItems {

    private final TeleTargetMethod teleTargetMethod;

    private final class ViewTargetMethodCodeAction extends InspectorAction {
        private ViewTargetMethodCodeAction() {
            super(inspection(), "View Disassembled Target Code");
        }

        @Override
        public void procedure() {
            inspection().focus().setCodeLocation(maxVM().createCodeLocation(teleTargetMethod.callEntryPoint()), false);
        }
    }

    private final ViewTargetMethodCodeAction viewTargetMethodCodeAction;

    private final class CopyTargetMethodCodeToClipboardAction extends InspectorAction {
        private CopyTargetMethodCodeToClipboardAction() {
            super(inspection(), "Copy Disassembled Target Code to Clipboard");
        }

        @Override
        public void procedure() {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.println("target method: " + teleTargetMethod.classMethodActor().format("%H.%n(%p)"));
            writer.println("compilation: " + inspection().nameDisplay().methodCompilationID(teleTargetMethod) + "  " + teleTargetMethod.classActorForType().simpleName());
            teleTargetMethod.traceBundle(writer);
            writer.flush();
            final ProcessorKind processorKind = maxVM().vmConfiguration().platform().processorKind();
            final InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(teleTargetMethod.getEncodedInlineDataDescriptors());
            final Pointer startAddress = teleTargetMethod.getCodeStart();
            final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
                @Override
                protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                    final String string = super.disassembledObjectString(disassembler, disassembledObject);
                    if (string.startsWith("call ")) {
                        final BytecodeLocation bytecodeLocation = null; //_teleTargetMethod.getBytecodeLocationFor(startAddress.plus(disassembledObject.startPosition()));
                        if (bytecodeLocation != null) {
                            final MethodRefConstant methodRef = bytecodeLocation.getCalleeMethodRef();
                            if (methodRef != null) {
                                final ConstantPool pool = bytecodeLocation.classMethodActor().codeAttribute().constantPool();
                                return string + " [" + methodRef.holder(pool).toJavaString(false) + "." + methodRef.name(pool) + methodRef.signature(pool).toJavaString(false, false) + "]";
                            }
                        }
                    }
                    return string;
                }
            };
            Disassemble.disassemble(byteArrayOutputStream, teleTargetMethod.getCode(), processorKind, startAddress, inlineDataDecoder, disassemblyPrinter);
            inspection().gui().postToClipboard(byteArrayOutputStream.toString());
        }
    }

    private final CopyTargetMethodCodeToClipboardAction copyTargetMethodCodeToClipboardAction;

    private final class TargetCodeBreakOnEntryAction extends InspectorAction {
        private TargetCodeBreakOnEntryAction() {
            super(inspection(), "Set Target Code Breakpoint at Entry");
        }

        @Override
        public void procedure() {
            teleTargetMethod.setTargetBreakpointAtEntry();
        }
    }

    private final TargetCodeBreakOnEntryAction targetCodeBreakOnEntryAction;

    private final class InspectTargetMethodObjectAction extends InspectorAction {
        private InspectTargetMethodObjectAction() {
            super(inspection(), "Inspect Target Method Object");
        }

        @Override
        public void procedure() {
            inspection().focus().setHeapObject(teleTargetMethod);
        }
    }

    private final InspectTargetMethodObjectAction inspectTargetMethodObjectAction;


    private final class InspectTargetCodeMemoryAction extends InspectorAction {
        private InspectTargetCodeMemoryAction() {
            super(inspection(), "Inspect Target Code Region Memory");
        }
        @Override
        protected void procedure() {
            MemoryInspector.create(inspection(), teleTargetMethod.targetCodeRegion().start(), teleTargetMethod.targetCodeRegion().size().toInt(), 1, 8).highlight();
        }
    }

    private final InspectTargetCodeMemoryAction inspectTargetCodeMemoryAction;

    private final class InspectTargetCodeMemoryWordsAction extends InspectorAction {
        private InspectTargetCodeMemoryWordsAction() {
            super(inspection(), "Inspect Target Code Region Memory Words");
        }

        @Override
        protected void procedure() {
            MemoryWordInspector.create(inspection(), teleTargetMethod.targetCodeRegion().start(), teleTargetMethod.targetCodeRegion().size().toInt()).highlight();
        }
    }

    private final InspectTargetCodeMemoryWordsAction inspectTargetCodeMemoryWordsAction;

    public TargetMethodMenuItems(Inspection inspection, TeleTargetMethod teleTargetMethod) {
        super(inspection);
        this.teleTargetMethod = teleTargetMethod;
        viewTargetMethodCodeAction = new ViewTargetMethodCodeAction();
        targetCodeBreakOnEntryAction = new TargetCodeBreakOnEntryAction();
        copyTargetMethodCodeToClipboardAction = new CopyTargetMethodCodeToClipboardAction();
        inspectTargetMethodObjectAction = new InspectTargetMethodObjectAction();
        inspectTargetCodeMemoryAction = new InspectTargetCodeMemoryAction();
        inspectTargetCodeMemoryWordsAction = new InspectTargetCodeMemoryWordsAction();
        refresh(true);
    }

    public void addTo(InspectorMenu menu) {
        menu.add(viewTargetMethodCodeAction);
        menu.add(targetCodeBreakOnEntryAction);
        menu.add(copyTargetMethodCodeToClipboardAction);
        menu.addSeparator();
        menu.add(inspectTargetMethodObjectAction);
        menu.add(inspectTargetCodeMemoryAction);
        menu.add(inspectTargetCodeMemoryWordsAction);
    }

    public void refresh(boolean force) {
    }

    public void redisplay() {
    }

}

