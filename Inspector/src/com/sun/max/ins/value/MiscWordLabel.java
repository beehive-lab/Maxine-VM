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
/*VCSID=6a7eeb2f-4796-4513-9615-d2c0640ae3a4*/
package com.sun.max.ins.value;

import java.awt.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.debug.*;
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
 * the header of a Maxine object in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 */
public final class MiscWordLabel extends ValueLabel {

    private final TeleObject _teleObject;
    private TeleObject _teleJavaMonitor = null;

    public MiscWordLabel(Inspection inspection, TeleObject teleObject) {
        super(inspection, null);
        _teleObject = teleObject;
        addMouseListener(new InspectorMouseClickAdapter(inspection()) {
            @Override
            public void procedure(final MouseEvent mouseEvent) {
                switch (MaxineInspector.mouseButtonWithModifiers(mouseEvent)) {
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
                        final InspectorMenu menu = new InspectorMenu();
                        menu.add(getCopyWordAction());
                        menu.add(getInspectJavaMonitorAction());
                        menu.popupMenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });
        initializeValue();
        redisplay();
    }

    @Override
    public Value fetchValue() {
        return new WordValue(_teleObject.getMiscWord());
    }

    public void redisplay() {
        setBackground(style().hexDataBackgroundColor());
        setFont(style().hexDataFont());
        setForeground(style().hexDataColor());
        updateText();
    }

    private static ModalLockWordDecoder _modalLockWordDecoder;

    @Override
    public void updateText() {

        final Word miscWord = value().asWord();
        final String hexString = miscWord.toHexString();
        final MonitorScheme monitorScheme = inspection().teleVM().bootImage().vmConfiguration().monitorScheme();

        if (monitorScheme instanceof ModalMonitorScheme) {
            _teleJavaMonitor = null;
            if (_modalLockWordDecoder == null) {
                final ModalMonitorScheme modalMonitorScheme = (ModalMonitorScheme) monitorScheme;
                _modalLockWordDecoder = modalMonitorScheme.getModalLockWordDecoder();
            }
            final ModalLockWord64 modalLockWord = ModalLockWord64.as(miscWord);
            if (_modalLockWordDecoder.isLockWordInMode(modalLockWord, BiasedLockWord64.class)) {
                final BiasedLockWord64 biasedLockWord = BiasedLockWord64.as(modalLockWord);
                final int hashcode = biasedLockWord.getHashcode();
                final int lockCount = biasedLockWord.getRecursionCount();
                final int ownerThreadID = BiasedLockModeHandler.decodeBiasOwnerThreadID(biasedLockWord);
                final TeleNativeThread thread = teleVM().teleProcess().idToThread(ownerThreadID);
                final String threadName = inspection().nameDisplay().longName(thread);
                final int biasEpoch = biasedLockWord.getEpoch().toInt();
                setText("BiasedLock(" + lockCount + "): " + hexString);
                setToolTipText("BiasedLockWord64:  lockCount=" + lockCount +   ";  thread=" +
                                threadName + ";  biasEpoch=" + biasEpoch  + "; hashcode=" + hashcode);
            } else if (_modalLockWordDecoder.isLockWordInMode(modalLockWord, ThinLockWord64.class)) {
                final ThinLockWord64 thinLockWord = ThinLockWord64.as(modalLockWord);
                final int hashcode = thinLockWord.getHashcode();
                final int lockCount = thinLockWord.getRecursionCount();
                final int ownerThreadID = ThinLockModeHandler.decodeLockOwnerThreadID(thinLockWord);
                final TeleNativeThread thread = teleVM().teleProcess().idToThread(ownerThreadID);
                final String threadName = inspection().nameDisplay().longName(thread);
                setText("ThinLock(" + lockCount + "): " + hexString);
                setToolTipText("ThinLockWord64:  lockCount=" + lockCount +   ";  thread=" +
                                threadName  + "; hashcode=" + hashcode);
            } else if (_modalLockWordDecoder.isLockWordInMode(modalLockWord, InflatedMonitorLockWord64.class)) {
                setText("InflatedMonitorLock: " + hexString);
                final InflatedMonitorLockWord64 inflatedLockWord = InflatedMonitorLockWord64.as(modalLockWord);
                final boolean isBound = inflatedLockWord.isBound();
                if (isBound) {
                    // JavaMonitor is a proper object, not just a Word.
                    final Reference javaMonitorReference = inflatedLockWord.getBoundMonitorReference();
                    _teleJavaMonitor = TeleObject.make(teleVM(), javaMonitorReference);
                    final String name = _teleJavaMonitor.classActorForType().qualifiedName();
                    setToolTipText("InflatedMonitorLockWord64:  bound, monitor=" + name);
                } else {
                    // Field access
                    final int hashcode = inflatedLockWord.getHashcode();
                    setToolTipText("InflatedMonitorLockWord64:  unbound, hashcode=" + hashcode);
                }
            } else {
                setText(hexString);
                setToolTipText("Non-decodable ModalLockWord64");
            }
        } else {
            setText(hexString);
            setToolTipText(null);
        }
    }

    private InspectorAction getCopyWordAction() {
        return inspection().inspectionMenus().getCopyWordAction(value().asWord(), "Copy word to clipboard");
    }

    private InspectorAction getInspectJavaMonitorAction() {
        final InspectorAction action = inspection().inspectionMenus().getInspectObjectAction(_teleJavaMonitor, "Inspect JavaMonitor (left-button)");
        action.setEnabled(_teleJavaMonitor != null);
        return action;
    }
}
