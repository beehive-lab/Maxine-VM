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
package com.sun.max.ins.value;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.monitor.modal.modehandlers.*;
import com.sun.max.vm.monitor.modal.modehandlers.inflated.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased.*;
import com.sun.max.vm.monitor.modal.modehandlers.lightweight.thin.*;
import com.sun.max.vm.monitor.modal.schemes.*;
import com.sun.max.vm.monitor.modal.schemes.ModalMonitorScheme.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;


/**
 * A label specialized for displaying the contents of the "misc." word value in
 * the header of a Maxine object in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MiscWordLabel extends ValueLabel {

    private final TeleObject teleObject;
    private TeleObject teleJavaMonitor = null;

    public MiscWordLabel(Inspection inspection, TeleObject teleObject) {
        super(inspection, null);
        this.teleObject = teleObject;
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                switch (Inspection.mouseButtonWithModifiers(mouseEvent)) {
                    case MouseEvent.BUTTON1: {
                        final InspectorAction inspectAction = getInspectJavaMonitorAction();
                        if (inspectAction.isEnabled()) {
                            inspectAction.perform();
                        }
                        break;
                    }
                    case MouseEvent.BUTTON2: {
                        // No toggle display action yet
                        break;
                    }
                    case MouseEvent.BUTTON3: {
                        final InspectorPopupMenu menu = new InspectorPopupMenu("Header Misc. Word");
                        menu.add(getCopyWordAction());
                        menu.add(getInspectJavaMonitorAction());
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });
        initializeValue();
        redisplay();
    }

    @Override
    public Value fetchValue() {
        return new WordValue(teleObject.getMiscWord());
    }

    public void redisplay() {
        setFont(style().hexDataFont());
        updateText();
    }

    private static ModalLockwordDecoder modalLockwordDecoder;

    @Override
    public void updateText() {
        final Word miscWord = value().asWord();
        final String hexString = miscWord.toHexString();
        final MonitorScheme monitorScheme = maxVM().bootImage().vmConfiguration.monitorScheme();
        if (monitorScheme instanceof ModalMonitorScheme) {
            teleJavaMonitor = null;
            if (modalLockwordDecoder == null) {
                final ModalMonitorScheme modalMonitorScheme = (ModalMonitorScheme) monitorScheme;
                modalLockwordDecoder = modalMonitorScheme.getModalLockwordDecoder();
            }
            final ModalLockword64 modalLockword = ModalLockword64.from(miscWord);
            if (modalLockwordDecoder.isLockwordInMode(modalLockword, BiasedLockword64.class)) {
                final BiasedLockword64 biasedLockword = BiasedLockword64.from(modalLockword);
                final int hashcode = biasedLockword.getHashcode();
                final int recursion = biasedLockword.getRecursionCount();
                final int ownerThreadID = BiasedLockModeHandler.decodeBiasOwnerThreadID(biasedLockword);
                final MaxThread thread = maxVM().getThread(ownerThreadID);
                final String threadName = inspection().nameDisplay().longName(thread);
                final int biasEpoch = biasedLockword.getEpoch().toInt();
                setText("BiasedLock(" + recursion + "): " + hexString);
                setToolTipText("BiasedLockword64:  recursion=" + recursion +   ";  thread=" +
                                threadName + ";  biasEpoch=" + biasEpoch  + "; hashcode=" + hashcode);
            } else if (modalLockwordDecoder.isLockwordInMode(modalLockword, ThinLockword64.class)) {
                final ThinLockword64 thinLockword = ThinLockword64.from(modalLockword);
                final int hashcode = thinLockword.getHashcode();
                final int recursionCount = thinLockword.getRecursionCount();
                final int ownerThreadID = ThinLockModeHandler.decodeLockOwnerThreadID(thinLockword);
                final MaxThread thread = maxVM().getThread(ownerThreadID);
                final String threadName = inspection().nameDisplay().longName(thread);
                setText("ThinLock(" + recursionCount + "): " + hexString);
                setToolTipText("ThinLockword64:  recursion=" + recursionCount +   ";  thread=" +
                                threadName  + "; hashcode=" + hashcode);
            } else if (modalLockwordDecoder.isLockwordInMode(modalLockword, InflatedMonitorLockword64.class)) {
                setText("InflatedMonitorLock: " + hexString);
                final InflatedMonitorLockword64 inflatedLockword = InflatedMonitorLockword64.from(modalLockword);
                final boolean isBound = inflatedLockword.isBound();
                if (isBound) {
                    // JavaMonitor is a proper object, not just a Word.
                    final Reference javaMonitorReference = maxVM().wordToReference(inflatedLockword.getBoundMonitorReferenceAsWord());
                    if (javaMonitorReference.isZero()) {
                        setToolTipText("InflatedMonitorLockword64:  bound, monitor=null");
                    } else {
                        teleJavaMonitor = maxVM().makeTeleObject(javaMonitorReference);
                        final String name = teleJavaMonitor.classActorForType().qualifiedName();
                        setToolTipText("InflatedMonitorLockword64:  bound, monitor=" + name);
                    }
                } else {
                    // Field access
                    final int hashcode = inflatedLockword.getHashcode();
                    setToolTipText("InflatedMonitorLockword64:  unbound, hashcode=" + hashcode);
                }
            } else {
                setText(hexString);
                setToolTipText("Non-decodable ModalLockword64");
            }
        } else {
            setText(hexString);
            setToolTipText(null);
        }
    }

    private InspectorAction getCopyWordAction() {
        return inspection().actions().copyWord(value().asWord(), "Copy word to clipboard");
    }

    private InspectorAction getInspectJavaMonitorAction() {
        final InspectorAction action = inspection().actions().inspectObject(teleJavaMonitor, "Inspect JavaMonitor (left-button)");
        action.setEnabled(teleJavaMonitor != null);
        return action;
    }
}
