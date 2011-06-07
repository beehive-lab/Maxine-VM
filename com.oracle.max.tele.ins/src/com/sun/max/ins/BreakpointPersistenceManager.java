/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins;

import java.util.*;

import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.util.*;
import com.sun.max.program.option.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.DefaultMethodKey;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Singleton manager for the Inspector's relationship with breakpoints in the VM.
 * Saves breakpoints to persistent storage and reloads at initialization.
 *
 * @author Michael Van De Vanter
 */
public final class BreakpointPersistenceManager extends AbstractSaveSettingsListener implements MaxBreakpointListener  {

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
            loadMachineCodeBreakpoints(settings);
            loadBytecodeBreakpoints(settings);
        } else {
            // TODO (mlvdv) some breakpoints could be saved across image builds,
            // for example method entry bytecode breakpoints.
            InspectorWarning.message(inspection, "Ignoring breakpoints related to a different boot image");
        }

        // Once load-in is finished, register for notification of subsequent breakpoint changes in the VM.
        inspection.vm().breakpointManager().addListener(this);
    }

    // Keys used for making data persistent
    private static final String MACHINE_CODE_BREAKPOINT_KEY = "machineCodeBreakpoint";
    private static final String BYTECODE_BREAKPOINT_KEY = "bytecodeBreakpoint";
    private static final String ADDRESS_KEY = "address";
    private static final String CONDITION_KEY = "condition";
    private static final String COUNT_KEY = "count";
    private static final String DESCRIPTION_KEY = "description";
    private static final String ENABLED_KEY = "enabled";
    private static final String METHOD_HOLDER_KEY = "method.holder";
    private static final String METHOD_NAME_KEY = "method.name";
    private static final String METHOD_SIGNATURE_KEY = "method.signature";
    private static final String BCI_KEY = "bci";

    public void breakpointsChanged() {
        // Breakpoints in the VM have changed.
        inspection.settings().save();
    }

    public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
        final List<MaxBreakpoint> machineCodeBreakpoints = new LinkedList<MaxBreakpoint>();
        final List<MaxBreakpoint> bytecodeBreakpoints = new LinkedList<MaxBreakpoint>();
        for (MaxBreakpoint breakpoint : inspection.vm().breakpointManager().breakpoints()) {
            if (breakpoint.isBytecodeBreakpoint()) {
                bytecodeBreakpoints.add(breakpoint);
            } else {
                machineCodeBreakpoints.add(breakpoint);
            }
        }
        saveMachineCodeBreakpoints(saveSettingsEvent, machineCodeBreakpoints);
        saveBytecodeBreakpoints(saveSettingsEvent, bytecodeBreakpoints);
    }

    private void saveMachineCodeBreakpoints(SaveSettingsEvent saveSettingsEvent, List<MaxBreakpoint> machineCodeBreakpoints) {
        saveSettingsEvent.save(MACHINE_CODE_BREAKPOINT_KEY + "." + COUNT_KEY, machineCodeBreakpoints.size());
        int index = 0;
        for (MaxBreakpoint breakpoint : machineCodeBreakpoints) {
            final String prefix = MACHINE_CODE_BREAKPOINT_KEY + index++;
            final Address bootImageOffset = breakpoint.codeLocation().address().minus(inspection.vm().bootImageStart());
            saveSettingsEvent.save(prefix + "." + ADDRESS_KEY, bootImageOffset.toLong());
            saveSettingsEvent.save(prefix + "." + ENABLED_KEY, breakpoint.isEnabled());
            final BreakpointCondition condition = breakpoint.getCondition();
            if (condition != null) {
                saveSettingsEvent.save(prefix + "." + CONDITION_KEY, condition.toString());
            }
            // Always save, even if empty, so old values get overwritten if description is removed.
            saveSettingsEvent.save(prefix + "." + DESCRIPTION_KEY, breakpoint.getDescription() == null ? "" : breakpoint.getDescription());
        }
    }

    private void loadMachineCodeBreakpoints(final InspectionSettings settings) {
        final int numberOfBreakpoints = settings.get(this, MACHINE_CODE_BREAKPOINT_KEY + "." + COUNT_KEY, OptionTypes.INT_TYPE, 0);
        for (int i = 0; i < numberOfBreakpoints; i++) {
            final String prefix = MACHINE_CODE_BREAKPOINT_KEY + i;
            final Address bootImageOffset = Address.fromLong(settings.get(this, prefix + "." + ADDRESS_KEY, OptionTypes.LONG_TYPE, null));
            final Address address = inspection.vm().bootImageStart().plus(bootImageOffset);
            final boolean enabled = settings.get(this, prefix + "." + ENABLED_KEY, OptionTypes.BOOLEAN_TYPE, null);
            final String condition = settings.get(this, prefix + "." + CONDITION_KEY, OptionTypes.STRING_TYPE, null);
            final String description = settings.get(this, prefix + "." + DESCRIPTION_KEY, OptionTypes.STRING_TYPE, null);
            if (inspection.vm().codeCache().contains(address)) {
                try {
                    final MaxCodeLocation codeLocation = inspection.vm().codeManager().createMachineCodeLocation(address, "loaded by breakpoint persistence manager");
                    final MaxBreakpoint breakpoint = inspection.vm().breakpointManager().makeBreakpoint(codeLocation);
                    if (condition != null) {
                        breakpoint.setCondition(condition);
                    }
                    breakpoint.setDescription(description);
                    breakpoint.setEnabled(enabled);
                } catch (BreakpointCondition.ExpressionException expressionException) {
                    inspection.gui().errorMessage(String.format("Error parsing saved breakpoint condition:%n  expression: %s%n       error: " + condition, expressionException.getMessage()), "Breakpoint Condition Error");
                } catch (MaxVMBusyException maxVMBusyException) {
                    InspectorWarning.message(inspection, "Unable to recreate machine code breakpoint from saved settings at: " + address, maxVMBusyException);
                }
            } else {
                InspectorWarning.message(inspection, "dropped former breakpoint in runtime-generated code at address: " + address);
            }
        }
    }

    private void saveBytecodeBreakpoints(SaveSettingsEvent settings, List<MaxBreakpoint> bytecodeBreakpoints) {
        int index;
        settings.save(BYTECODE_BREAKPOINT_KEY + "." + COUNT_KEY, bytecodeBreakpoints.size());
        index = 0;
        for (MaxBreakpoint breakpoint : bytecodeBreakpoints) {
            final String prefix = BYTECODE_BREAKPOINT_KEY + index++;
            final MaxCodeLocation codeLocation = breakpoint.codeLocation();
            final MethodKey methodKey = codeLocation.methodKey();
            if (methodKey != null) {
                settings.save(prefix + "." + METHOD_HOLDER_KEY, methodKey.holder().string);
                settings.save(prefix + "." + METHOD_NAME_KEY, methodKey.name().string);
                settings.save(prefix + "." + METHOD_SIGNATURE_KEY, methodKey.signature().string);
                settings.save(prefix + "." + BCI_KEY, codeLocation.bci());
                settings.save(prefix + "." + ENABLED_KEY, breakpoint.isEnabled());
            } else {
                InspectorWarning.message(inspection, "Unable to save bytecode breakpoint, no key in " + breakpoint);
            }
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
            final int bci = settings.get(this, prefix + "." + BCI_KEY, OptionTypes.INT_TYPE, 0);
            if (bci > 0) {
                InspectorWarning.message(inspection, "Ignoring non-zero bytecode index for saved breakpoint in " + methodKey);
            }
            final boolean enabled = settings.get(this, prefix + "." + ENABLED_KEY, OptionTypes.BOOLEAN_TYPE, true);
            final MaxCodeLocation location = inspection.vm().codeManager().createBytecodeLocation(methodKey, "loaded by breakpoint persistence manager");
            MaxBreakpoint bytecodeBreakpoint;
            try {
                bytecodeBreakpoint = inspection.vm().breakpointManager().makeBreakpoint(location);
                if (bytecodeBreakpoint.isEnabled() != enabled) {
                    bytecodeBreakpoint.setEnabled(enabled);
                }
            } catch (MaxVMBusyException maxVMBusyException) {
                InspectorWarning.message(inspection, "Unable to recreate bytecode breakpoint from saved settings at: " + methodKey, maxVMBusyException);
            }
        }
    }

}
