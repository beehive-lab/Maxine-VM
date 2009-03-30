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

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;

/**
 * Singleton inspector for the boot image from which the {@link TeleVM} was started.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class BootImageInspector extends Inspector {

    // Set to null when inspector closed.
    private static BootImageInspector _bootImageInspector;

    /**
     * Display and highlight the (singleton) Boot Image inspector.
     * @return  The Boot Image inspector, possibly newly created.
     */
    public static BootImageInspector make(Inspection inspection) {
        if (_bootImageInspector == null) {
            _bootImageInspector = new BootImageInspector(inspection);
        }
        _bootImageInspector.highlight();
        return _bootImageInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "bootImageInspector");

    private JPanel _infoPanel;

    private BootImageInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1, tracePrefix() + "initializing");
        createFrame(null);
        Trace.end(1, tracePrefix() + "initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    public String getTextForTitle() {
        return "Boot Image: " + teleVM().bootImageFile().getAbsolutePath();
    }

    @Override
    public void createView(long epoch) {
        _infoPanel = new InspectorPanel(inspection(), new SpringLayout());
        populateInfoPanel();
        final JScrollPane scrollPane = new InspectorScrollPane(inspection(), _infoPanel);
        frame().setContentPane(scrollPane);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _bootImageInspector = null;
        super.inspectorClosing();
    }

    private void addInfo(String name, InspectorLabel label) {
        _infoPanel.add(new TextLabel(inspection(), name));
        _infoPanel.add(label);
    }

    private void populateInfoPanel() {
        final BootImage bootImage = teleVM().bootImage();
        final BootImage.Header header = bootImage.header();
        final VMConfiguration vmConfiguration = bootImage.vmConfiguration();
        final Platform platform = vmConfiguration.platform();
        final ProcessorKind processorKind = platform.processorKind();
        final DataModel dataModel = processorKind.dataModel();

        addInfo("identification:", new DataLabel.IntAsHex(inspection(), header._identification));
        addInfo("version:", new DataLabel.IntAsDecimal(inspection(),  header._version));
        addInfo("random ID:", new DataLabel.IntAsHex(inspection(), header._randomID));

        addInfo("build level:", new DataLabel.EnumAsText(inspection(), vmConfiguration.buildLevel()));

        addInfo("processor model:", new DataLabel.EnumAsText(inspection(), processorKind.processorModel()));
        addInfo("instruction set:", new DataLabel.EnumAsText(inspection(), processorKind.instructionSet()));

        addInfo("bits/word:", new DataLabel.IntAsDecimal(inspection(), dataModel.wordWidth().numberOfBits()));
        addInfo("endianness:", new DataLabel.EnumAsText(inspection(), dataModel.endianness()));
        addInfo("alignment:", new DataLabel.IntAsDecimal(inspection(), dataModel.alignment().numberOfBytes()));

        addInfo("operating system:", new DataLabel.EnumAsText(inspection(), platform.operatingSystem()));
        addInfo("page size:", new DataLabel.IntAsDecimal(inspection(), platform.pageSize()));

        addInfo("grip scheme:", new JavaNameLabel(inspection(), vmConfiguration.gripScheme().name(), vmConfiguration.gripScheme().getClass().getName()));
        addInfo("reference scheme:", new JavaNameLabel(inspection(), vmConfiguration.referenceScheme().name(), vmConfiguration.referenceScheme().getClass().getName()));
        addInfo("layout scheme:",  new JavaNameLabel(inspection(), vmConfiguration.layoutScheme().name(), vmConfiguration.layoutScheme().getClass().getName()));
        addInfo("heap scheme:", new JavaNameLabel(inspection(), vmConfiguration.heapScheme().name(), vmConfiguration.heapScheme().getClass().getName()));
        addInfo("monitor scheme:", new JavaNameLabel(inspection(), vmConfiguration.monitorScheme().name(), vmConfiguration.monitorScheme().getClass().getName()));
        addInfo("compilation scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilationScheme().name(), vmConfiguration.compilationScheme().getClass().getName()));
        addInfo("optimizing compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.compilerScheme().name(), vmConfiguration.compilerScheme().getClass().getName()));
        addInfo("JIT compiler scheme:", new JavaNameLabel(inspection(), vmConfiguration.jitScheme().name(), vmConfiguration.jitScheme().getClass().getName()));
        addInfo("interpreter scheme:", new JavaNameLabel(inspection(), vmConfiguration.interpreterScheme().name(), vmConfiguration.interpreterScheme().getClass().getName()));
        addInfo("trampoline scheme:", new JavaNameLabel(inspection(), vmConfiguration.trampolineScheme().name(), vmConfiguration.trampolineScheme().getClass().getName()));
        addInfo("target ABIs scheme:", new JavaNameLabel(inspection(), vmConfiguration.targetABIsScheme().name(), vmConfiguration.targetABIsScheme().getClass().getName()));
        addInfo("run scheme:", new JavaNameLabel(inspection(), vmConfiguration.runScheme().name(), vmConfiguration.runScheme().getClass().getName()));

        addInfo("relocation scheme:", new DataLabel.IntAsHex(inspection(), header._relocationScheme));
        addInfo("relocation data size:", new DataLabel.IntAsHex(inspection(), header._relocationDataSize));
        addInfo("string data size:", new DataLabel.IntAsHex(inspection(), header._stringInfoSize));

        final Pointer bootImageStart = teleVM().bootImageStart();

        final Pointer bootHeapStart = bootImageStart;
        final Pointer bootHeapEnd = bootHeapStart.plus(header._bootHeapSize);

        addInfo("boot heap start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapStart));
        addInfo("boot heap size:", new DataLabel.IntAsHex(inspection(), header._bootHeapSize));
        addInfo("boot heap end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapEnd));

        final Pointer bootCodeStart = bootHeapEnd;
        final Pointer bootCodeEnd = bootCodeStart.plus(header._bootCodeSize);

        addInfo("boot code start:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeStart));
        addInfo("boot code size:", new DataLabel.IntAsHex(inspection(), header._bootCodeSize));
        addInfo("boot code end:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeEnd));

        addInfo("code cache size:", new DataLabel.IntAsHex(inspection(), header._codeCacheSize));
        addInfo("thread local space size:", new DataLabel.IntAsHex(inspection(), header._vmThreadLocalsSize));

        addInfo("vmStartupMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT,  bootImageStart.plus(header._vmRunMethodOffset)));
        addInfo("vmThreadRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, bootImageStart.plus(header._vmThreadRunMethodOffset)));
        addInfo("runSchemeRunMethod:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.CALL_ENTRY_POINT, bootImageStart.plus(header._runSchemeRunMethodOffset)));

        addInfo("class registry:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.REFERENCE, bootHeapStart.plus(header._classRegistryOffset)));
        addInfo("heap regions pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootHeapStart.plus(header._heapRegionsPointerOffset)));
        addInfo("code regions pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootCodeStart.plus(header._codeRegionsPointerOffset)));

        addInfo("messenger info pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootImageStart.plus(header._messengerInfoOffset)));
        addInfo("thread specifics list pointer:", new WordValueLabel(inspection(), WordValueLabel.ValueMode.WORD, bootImageStart.plus(header._threadSpecificsListOffset)));
        SpringUtilities.makeCompactGrid(_infoPanel, 2);
    }

}
