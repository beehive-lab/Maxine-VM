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

import java.util.*;

import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;


/**
 * Singleton manager for the Inspector's relationship with breakpoints in the VM.
 * Saves breakpoints to persistent storage and reloads at initialization.
 *
 * @author Michael Van De Vanter
 */
public final class BreakpointPersistenceManager extends AbstractSaveSettingsListener implements Observer  {

    private static BreakpointPersistenceManager breakpointPersistenceManager;

    /**
     * Sets up a manager for making breakpoints set in the VM persistent, saved
     * after each breakpoint change.
     */
    public static void initialize(Inspection inspection) {
        if (breakpointPersistenceManager == null) {
            breakpointPersistenceManager = new BreakpointPersistenceManager(inspection);
        }
    }

    private final Inspection inspection;

    private BreakpointPersistenceManager(Inspection inspection) {
        super("breakpoints");
        this.inspection = inspection;

        final InspectionSettings settings = inspection.settings();

        // Register with the persistence service; must do this before load.
        settings.addSaveSettingsListener(this);

        if (!settings.bootImageChanged()) {
            loadTargetCodeBreakpoints(settings);
            loadBytecodeBreakpoints(settings);
        } else {
            // TODO (mlvdv) some breakpoints could be saved across image builds,
            // for example method entry bytecode breakpoints.
            ProgramWarning.message("Ignoring breakpoints related to a different boot image");
        }

        // Once load-in is finished, register for notification of subsequent breakpoint changes in the VM.
        inspection.maxVM().addBreakpointObserver(this);
    }

    public void update(Observable o, Object arg) {
        // Breakpoints in the VM have changed.
        inspection.settings().save();
    }

    // Keys used for making data persistent
    private static final String TARGET_BREAKPOINT_KEY = "targetBreakpoint";
    private static final String BYTECODE_BREAKPOINT_KEY = "bytecodeBreakpoint";
    private static final String ADDRESS_KEY = "address";
    private static final String CONDITION_KEY = "condition";
    private static final String COUNT_KEY = "count";
    private static final String ENABLED_KEY = "enabled";
    private static final String METHOD_HOLDER_KEY = "method.holder";
    private static final String METHOD_NAME_KEY = "method.name";
    private static final String METHOD_SIGNATURE_KEY = "method.signature";
    private static final String POSITION_KEY = "position";

    public void saveSettings(SaveSettingsEvent settings) {
        saveTargetCodeBreakpoints(settings);
        saveBytecodeBreakpoints(settings);
    }

    private void saveTargetCodeBreakpoints(SaveSettingsEvent settings) {
        settings.save(TARGET_BREAKPOINT_KEY + "." + COUNT_KEY, inspection.maxVM().targetBreakpointCount());
        int index = 0;
        for (TeleTargetBreakpoint breakpoint : inspection.maxVM().targetBreakpoints()) {
            final String prefix = TARGET_BREAKPOINT_KEY + index++;
            final Address bootImageOffset = breakpoint.address().minus(inspection.maxVM().bootImageStart());
            settings.save(prefix + "." + ADDRESS_KEY, bootImageOffset.toLong());
            settings.save(prefix + "." + ENABLED_KEY, breakpoint.isEnabled());
            final BreakpointCondition condition = breakpoint.condition();
            if (condition != null) {
                settings.save(prefix + "." + CONDITION_KEY, condition.toString());
            }
        }
    }

    private void loadTargetCodeBreakpoints(final InspectionSettings settings) {
        final int numberOfBreakpoints = settings.get(this, TARGET_BREAKPOINT_KEY + "." + COUNT_KEY, OptionTypes.INT_TYPE, 0);
        for (int i = 0; i < numberOfBreakpoints; i++) {
            final String prefix = TARGET_BREAKPOINT_KEY + i;
            final Address bootImageOffset = Address.fromLong(settings.get(this, prefix + "." + ADDRESS_KEY, OptionTypes.LONG_TYPE, null));
            final Address address = inspection.maxVM().bootImageStart().plus(bootImageOffset);
            final boolean enabled = settings.get(this, prefix + "." + ENABLED_KEY, OptionTypes.BOOLEAN_TYPE, null);
            final String condition = settings.get(this, prefix + "." + CONDITION_KEY, OptionTypes.STRING_TYPE, null);
            if (inspection.maxVM().containsInCode(address)) {
                try {
                    final TeleTargetBreakpoint teleBreakpoint = inspection.maxVM().makeTargetBreakpoint(address);
                    if (condition != null) {
                        teleBreakpoint.setCondition(condition);
                    }
                    teleBreakpoint.setEnabled(enabled);
                } catch (BreakpointCondition.ExpressionException expressionException) {
                    inspection.gui().errorMessage(String.format("Error parsing saved breakpoint condition:%n  expression: %s%n       error: " + condition, expressionException.getMessage()), "Breakpoint Condition Error");
                }
            } else {
                ProgramWarning.message("dropped former breakpoint in runtime-generated code at address: " + address);
            }
        }
    }

    private void saveBytecodeBreakpoints(SaveSettingsEvent settings) {
        int index;
        settings.save(BYTECODE_BREAKPOINT_KEY + "." + COUNT_KEY, inspection.maxVM().bytecodeBreakpointCount());
        index = 0;
        for (TeleBytecodeBreakpoint breakpoint : inspection.maxVM().bytecodeBreakpoints()) {
            final String prefix = BYTECODE_BREAKPOINT_KEY + index++;
            final TeleBytecodeBreakpoint.Key key = breakpoint.key();
            settings.save(prefix + "." + METHOD_HOLDER_KEY, key.holder().string);
            settings.save(prefix + "." + METHOD_NAME_KEY, key.name().string);
            settings.save(prefix + "." + METHOD_SIGNATURE_KEY, key.signature().string);
            settings.save(prefix + "." + POSITION_KEY, key.position());
            settings.save(prefix + "." + ENABLED_KEY, breakpoint.isEnabled());
        }
    }

    private void loadBytecodeBreakpoints(final InspectionSettings settings) {
        final int numberOfBreakpoints = settings.get(this, BYTECODE_BREAKPOINT_KEY + "." + COUNT_KEY, OptionTypes.INT_TYPE, 0);
        for (int i = 0; i < numberOfBreakpoints; i++) {
            final String prefix = BYTECODE_BREAKPOINT_KEY + i;
            final TypeDescriptor holder = JavaTypeDescriptor.parseTypeDescriptor(settings.get(this, prefix + "." + METHOD_HOLDER_KEY, OptionTypes.STRING_TYPE, null));
            final Utf8Constant name = PoolConstantFactory.makeUtf8Constant(settings.get(this, prefix + "." + METHOD_NAME_KEY, OptionTypes.STRING_TYPE, null));
            final SignatureDescriptor signature = SignatureDescriptor.create(settings.get(this, prefix + "." + METHOD_SIGNATURE_KEY, OptionTypes.STRING_TYPE, null));
            final MethodKey methodKey = new DefaultMethodKey(holder, name, signature);
            final int bytecodePosition = settings.get(this, prefix + "." + POSITION_KEY, OptionTypes.INT_TYPE, 0);
            final boolean enabled = settings.get(this, prefix + "." + ENABLED_KEY, OptionTypes.BOOLEAN_TYPE, true);

            final TeleBytecodeBreakpoint breakpoint = inspection.maxVM().makeBytecodeBreakpoint(new TeleBytecodeBreakpoint.Key(methodKey, bytecodePosition));
            breakpoint.setEnabled(enabled);
            if (enabled) {
                breakpoint.activate();
            }
        }
    }


}
