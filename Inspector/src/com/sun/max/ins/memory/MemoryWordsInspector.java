/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

// TODO (mlvdv) try to make columns narrow
// TODO (mlvdv) Parameter for object search extent

/**
 * An inspector that displays the contents of a region of memory in the VM, word aligned, one word per row.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryWordsInspector extends Inspector {

    private static final String UNKNOWN_REGION_NAME = "unknown region";
    private static final int TRACE_VALUE = 2;

    public static enum ViewMode {
        WORD("Word", "Grows the visible region a word at a time and  navigates to the new location",
            "Grow the visible region upward (lower address) by one word", "Grow the visible region downward (higher address) by one word"),
        OBJECT("Obj", "Move to next/previous object origin",
            "View memory for previous object", "View memory for next object"),
        PAGE("Page", "Move to next/previous page origin and display entire page",
            "View previous memory page", "View next memory page");

        private final String label;
        private final String description;
        private final String previousToolTip;
        private final String nextToolTip;

        /**
         * @param label the label that identifies the mode
         * @param description description of the mode
         * @param previousToolTip description of the move backwards action in this mode
         * @param nextToolTip description of the move forward action in this mode
         */
        private ViewMode(String label, String description, String previousToolTip, String nextToolTip) {
            this.label = label;
            this.description = description;
            this.previousToolTip = previousToolTip;
            this.nextToolTip = nextToolTip;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public String previousToolTip() {
            return previousToolTip;
        }

        public String nextToolTip() {
            return nextToolTip;
        }

    }

    private static MemoryWordsViewPreferences globalPreferences;

    /**
     * @return the global, persistent set of user preferences for viewing these tables..
     */
    public static MemoryWordsViewPreferences globalPreferences(Inspection inspection) {
        if (globalPreferences == null) {
            globalPreferences = new MemoryWordsViewPreferences(inspection);
        }
        return globalPreferences;
    }

    // Prefix for all persistent column preferences in view
    private static final String MEMORY_WORDS_COLUMN_PREFERENCE = "memoryWordsViewColumn";

    public static JPanel globalPreferencesPanel(Inspection inspection) {
        return globalPreferences(inspection).getPanel();
    }

    private static class MemoryWordsViewPreferences extends TableColumnVisibilityPreferences<MemoryWordsColumnKind> {

        private final MemoryWordsInspector memoryWordsInspector;

        /**
         * Creates global preferences for this inspector.
         */
        private MemoryWordsViewPreferences(Inspection inspection) {
            super(inspection, MEMORY_WORDS_COLUMN_PREFERENCE, MemoryWordsColumnKind.values());
            this.memoryWordsInspector = null;
        }

        /**
         * A per-instance set of view preferences, initialized to the global preferences.
         * @param defaultPreferences the global defaults for this kind of view
         */
        public MemoryWordsViewPreferences(MemoryWordsViewPreferences globalPreferences, MemoryWordsInspector memoryWordsInspector) {
            super(globalPreferences);
            this.memoryWordsInspector = memoryWordsInspector;
            // There are no view preferences beyond the column choices, so no additional machinery needed here.
        }

        @Override
        public void setIsVisible(MemoryWordsColumnKind columnKind, boolean visible) {
            super.setIsVisible(columnKind, visible);
            if (memoryWordsInspector != null) {
                memoryWordsInspector.reconstructView();
            }
        }

        @Override
        public MemoryWordsViewPreferences clone() {
            return new MemoryWordsViewPreferences(this, memoryWordsInspector);
        }
    }

    private static Set<MemoryWordsInspector> inspectors = new HashSet<MemoryWordsInspector>();

    /**
     * @return all existing memory words inspectors, even if hidden or iconic.
     */
    public static Set<MemoryWordsInspector> inspectors() {
        return inspectors;
    }

    private final Size wordSize;
    private final Size pageSize;
    private final int wordsInPage;

    //  View specifications from when the Inspector was created
    private final MemoryWordRegion originalMemoryWordRegion;
    private final ViewMode originalViewMode;
    private final String originalRegionName;
    private final Address originalOrigin;

    // Current view specifications.
    private MemoryWordRegion memoryWordRegion;
    private String regionName;  // null if current region is specially named
    // Current view mode held in the ComboBox, which gets retained and reused across view reconstructions.

    // Address of word 0 for the purposes of the Offset columns.
    private Address origin;

    private MemoryWordsTable table;
    private InspectorScrollPane scrollPane;

    private JToolBar toolBar;
    private final AddressInputField.Hex originField;
    private final AddressInputField.Decimal wordCountField;
    private final InspectorComboBox viewModeComboBox;
    private final JLabel viewModeComboBoxRenderer;  // Holds current view mode, even across view reconstructions.
    private final InspectorButton previousButton;
    private final InspectorButton nextButton;
    private final InspectorButton findButton;
    private final InspectorButton prefsButton;
    private final InspectorButton homeButton;
    private final InspectorButton cloneButton;
    private final MemoryWordsViewPreferences instanceViewPreferences;

    private MemoryWordsInspector(Inspection inspection, final MaxMemoryRegion memoryRegion, String regionName, Address origin, ViewMode viewMode, MemoryWordsViewPreferences instanceViewPreferences) {
        super(inspection);
        assert viewMode != null;

        Trace.line(1, tracePrefix() + " creating for region:  " + memoryRegion.toString());

        inspectors.add(this);
        wordSize = inspection.vm().platform().wordSize();
        pageSize = inspection.vm().platform().pageSize();
        wordsInPage = pageSize.dividedBy(wordSize).toInt();

        if (instanceViewPreferences == null) {
            // Clone the global preferences
            this.instanceViewPreferences = new MemoryWordsViewPreferences(globalPreferences(inspection()), this);
        } else {
            // Clone another set of instance preferences
            this.instanceViewPreferences = new MemoryWordsViewPreferences(instanceViewPreferences, this);
        }
        Address start = memoryRegion.start();
        final Address alignedStart = start.aligned(wordSize.toInt());
        start = (start.equals(alignedStart)) ? start : alignedStart.minus(wordSize);
        final int wordCount = wordsInRegion(memoryRegion);
        this.originalMemoryWordRegion = new MemoryWordRegion(inspection.vm(), start, wordCount, wordSize);
        this.memoryWordRegion = originalMemoryWordRegion;
        this.originalOrigin = (origin == null) ? start : origin;
        this.originalRegionName = regionName;
        this.regionName = regionName;
        this.originalViewMode = viewMode;

        this.origin = originalOrigin;

        originField = new AddressInputField.Hex(inspection, this.origin) {
            @Override
            public void update(Address value) {
                if (!value.equals(MemoryWordsInspector.this.origin)) {
                    // User model policy:  any adjustment to the region drops into generic word mode
                    clearViewMode();
                    setOrigin(value.aligned(wordSize.toInt()));
                    setTitle();
                }
            }
        };

        wordCountField = new AddressInputField.Decimal(inspection, Address.fromInt(memoryWordRegion.wordCount)) {
            @Override
            public void update(Address value) {
                final int newWordCount = value.toInt();
                final int oldWordCount = memoryWordRegion.wordCount;
                if (newWordCount <= 0) {
                    // Bogus; reset to prior value
                    wordCountField.setValue(Address.fromInt(oldWordCount));
                } else if (newWordCount != oldWordCount) {
                    // User model policy:  any adjustment to the region drops into generic word mode
                    clearViewMode();
                    final MaxVM vm = MemoryWordsInspector.this.vm();
                    setMemoryRegion(new MemoryWordRegion(vm, memoryRegion.start(), newWordCount, wordSize));
                    setTitle();
                }
            }
        };

        // The combo box holds the current view mode
        viewModeComboBox = new InspectorComboBox(inspection, ViewMode.values());
        viewModeComboBox.setSelectedItem(originalViewMode);
        // Add the listener after the initial selection is set; we're not ready for an update yet.
        viewModeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateViewMode();
            }
        });
        viewModeComboBoxRenderer = new JLabel();
        viewModeComboBox.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final ViewMode viewMode = (ViewMode) value;
                viewModeComboBoxRenderer.setText(viewMode.label());
                return viewModeComboBoxRenderer;
            }
        });

        previousButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveBack();
            }
        });
        previousButton.setIcon(style().navigationBackIcon());

        nextButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveForward();
            }
        });
        nextButton.setIcon(style().navigationForwardIcon());

        prefsButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                new SimpleDialog(inspection(), MemoryWordsInspector.this.instanceViewPreferences.getPanel(), "View Preferences", true);
            }
        });
        prefsButton.setText(null);
        prefsButton.setToolTipText("Column view options");
        prefsButton.setIcon(style().generalPreferencesIcon());

        findButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                gui().informationMessage("memory \"Find\" is unimplemented");
            }
        });
        findButton.setIcon(style().generalFindIcon());
        findButton.setToolTipText("Find (UNIMPLEMENTED)");
        findButton.setEnabled(false);

        homeButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                resetToOriginal();
            }
        });
        homeButton.setIcon(style().navigationHomeIcon());
        homeButton.setToolTipText("Return displayed region to original");

        cloneButton = new InspectorButton(inspection(), cloneAction);
        cloneButton.setText(null);
        cloneButton.setToolTipText("Create a cloned copy of this memory inspector");
        cloneButton.setIcon(style().generalCopyIcon());

        final InspectorFrame frame = createFrame(true);
        final InspectorMenu defaultMenu = frame.makeMenu(MenuKind.DEFAULT_MENU);
        defaultMenu.add(defaultMenuItems(MenuKind.DEFAULT_MENU));
        defaultMenu.addSeparator();
        defaultMenu.add(actions().closeViews(MemoryWordsInspector.class, this, "Close other memory inspectors"));
        defaultMenu.add(actions().closeViews(MemoryWordsInspector.class, null, "Close all memory inspectors"));

        final InspectorMenu memoryMenu = frame.makeMenu(MenuKind.MEMORY_MENU);
        setOriginToSelectionAction.refresh(true);
        memoryMenu.add(setOriginToSelectionAction);
        memoryMenu.add(scrollToFocusAction);
        memoryMenu.add(inspectBytesAction);
        memoryMenu.add(defaultMenuItems(MenuKind.MEMORY_MENU));
        final JMenuItem viewMemoryRegionsMenuItem = new JMenuItem(actions().viewMemoryRegions());
        viewMemoryRegionsMenuItem.setText("View Memory Regions");
        memoryMenu.add(viewMemoryRegionsMenuItem);

        frame.makeMenu(MenuKind.VIEW_MENU).add(defaultMenuItems(MenuKind.VIEW_MENU));

        gui().setLocationRelativeToMouse(this, inspection().geometry().objectInspectorNewFrameDiagonalOffset());

        table.scrollToOrigin();
       // table.setPreferredScrollableViewportSize(new Dimension(-1, preferredTableHeight()));
    }

    /**
     * Create a memory inspector for a designated region of memory, with the view
     * mode set to {@link ViewMode#WORD}.
     */
    public MemoryWordsInspector(Inspection inspection, MaxMemoryRegion memoryRegion) {
        this(inspection, memoryRegion, null, memoryRegion.start(), ViewMode.WORD, null);
    }

    /**
     * Create a memory inspector for a designated, named region of memory, with the view
     * mode set to {@link ViewMode#WORD}.
     */
    public MemoryWordsInspector(Inspection inspection, MaxMemoryRegion memoryRegion, String regionName) {
        this(inspection, memoryRegion, regionName, memoryRegion.start(), ViewMode.WORD, null);
    }

    /**
     * Create a memory inspector for the memory holding an object, with the view
     * mode set to {@link ViewMode#OBJECT}.
     */
    public MemoryWordsInspector(Inspection inspection, TeleObject teleObject) {
        this(inspection, teleObject.objectMemoryRegion(), null, teleObject.origin(), teleObject.isLive() ? ViewMode.OBJECT : ViewMode.WORD, null);
    }

    /**
     * Create a memory inspector for a page of memory, with the view
     * mode set to {@link ViewMode#PAGE}.
     */
    public MemoryWordsInspector(Inspection inspection, Address address) {
        this(inspection, new InspectorMemoryRegion(inspection.vm(), "", address, inspection.vm().platform().pageSize()), null, address, ViewMode.PAGE, null);
    }

    @Override
    protected void createView() {

        table = new MemoryWordsTable(inspection(), memoryWordRegion, origin, instanceViewPreferences, setOriginToSelectionAction);

        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());

        toolBar = new InspectorToolBar(inspection());
        toolBar.setBorder(style().defaultPaneBorder());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(new JLabel("Origin:"));
        toolBar.add(originField);
        toolBar.add(new JLabel("Words:"));
        toolBar.add(wordCountField);
        toolBar.add(previousButton);
        toolBar.add(viewModeComboBox);
        toolBar.add(nextButton);
        toolBar.add(findButton);
        toolBar.add(homeButton);
        toolBar.add(cloneButton);
        toolBar.add(prefsButton);
        panel.add(toolBar, BorderLayout.NORTH);

        scrollPane = new SizedScrollPane(inspection(), table);
        panel.add(scrollPane, BorderLayout.CENTER);
        setContentPane(panel);
        // Force everything into consistency with the current view mode.
        updateViewMode();

        // When user grows window height beyond table size, expand region being viewed.
        final JViewport viewport = scrollPane.getViewport();
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                super.componentResized(componentEvent);
                final Rectangle bounds = viewport.getBounds();
//                System.out.println(bounds.toString());
//                System.out.println("Header=" + table.getTableHeader().getHeight());
//                System.out.println("Row height=" + table.getRowHeight());
//                System.out.println("Avail=" + ((bounds.height - table.getTableHeader().getHeight()) - (MemoryWordsInspector.this.memoryWordRegion.wordCount * table.getRowHeight())));
                final int rowCapacity = ((bounds.height - table.getTableHeader().getHeight()) - (MemoryWordsInspector.this.memoryWordRegion.wordCount * table.getRowHeight())) / table.getRowHeight();
//                System.out.println("Capacity =" + rowCapacity);
//                System.out.println("Preferred=" + preferredTableHeight());
                if (rowCapacity > 0) {
                    Trace.line(TRACE_VALUE, tracePrefix() + "growing viewport rows by " + rowCapacity);
                    growRegionDown(rowCapacity);
                }
            }
        });

    }

    private final class SizedScrollPane extends InspectorScrollPane {

        private final InspectorTable inspectorTable;

        /**
         * Creates a scrollable pane containing the {@link InspectorTable}, with preferred height set to match the size
         * of the table up to a specified limit.
         */
        public SizedScrollPane(Inspection inspection, InspectorTable inspectorTable) {
            super(inspection, inspectorTable);
            this.inspectorTable = inspectorTable;
            // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
            // this is empirical, based only the fuzziest notion of how these dimensions work
            final int displayRows = Math.min(style().memoryTableMaxDisplayRows(), inspectorTable.getRowCount()) + 2;
            final int preferredHeight = displayRows * (inspectorTable.getRowHeight() + inspectorTable.getRowMargin()) +
                                                          inspectorTable.getRowMargin()  + inspectorTable.getTableHeader().getHeight();
            final int preferredWidth = inspectorTable.getPreferredScrollableViewportSize().width;
            inspectorTable.setPreferredScrollableViewportSize(new Dimension(preferredWidth, preferredHeight));
        }

        @Override
        public void redisplay() {
            inspectorTable.redisplay();
        }

        @Override
        public void refresh(boolean force) {
            inspectorTable.refresh(force);
        }

    }

    private int preferredTableHeight() {
        // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
        // this is empirical, based only the fuzziest notion of how these dimensions work
        final int displayRows = Math.min(style().memoryTableMaxDisplayRows(), table.getRowCount());
        final int rowHeight = table.getRowHeight();
        final int rowMargin = table.getRowMargin();
        final int headerHeight = table.getTableHeader().getHeight();
        final int preferredHeight = displayRows * (rowHeight + rowMargin) + rowMargin  + headerHeight;
//        Trace.line(TRACE_VALUE, tracePrefix() + "preferredHeight=" + preferredHeight + "[ rows=" + displayRows + ", rowHeight=" + rowHeight
//                + ", rowMargin=" + rowMargin + ", headerHeight=" + headerHeight + "]");
        return preferredHeight;
    }

    /**
     * Gets current view mode.
     */
    private ViewMode viewMode() {
        return (ViewMode) viewModeComboBox.getSelectedItem();
    }

    /**
     * Sets current view mode and updates related state.
     */
    private void setViewMode(ViewMode viewMode) {
        viewModeComboBox.setSelectedItem(viewMode);
        updateViewMode();
    }

    /**
     * Sets the current view parameters to the default state.
     */
    private void clearViewMode() {
        setViewMode(ViewMode.WORD);
        regionName = null;
    }

    /**
     * Updates state related to current view mode.
     */
    private void updateViewMode() {
        previousButton.setToolTipText(viewMode().previousToolTip());
        nextButton.setToolTipText(viewMode().nextToolTip());
        switch (viewMode()) {
            case OBJECT:
                moveToCurrentObject();
                break;
            case PAGE:
                moveToCurrentPage();
                break;
            case WORD:
                break;
            default:
                InspectorError.unknownCase();
        }
        setTitle();
    }

    @Override
    public String getTextForTitle() {

        // Locate the first and last memory locations with respect to regions
        // allocated by the VM, and build a description of the memory range
        final MaxMemoryRegion startRegion = vm().findMemoryRegion(memoryWordRegion.start());
        final MaxMemoryRegion lastRegion = vm().findMemoryRegion(memoryWordRegion.end().minus(1));
        final String startRegionName = startRegion == null ? UNKNOWN_REGION_NAME : startRegion.regionName();
        final String lastRegionName = lastRegion == null ? UNKNOWN_REGION_NAME : lastRegion.regionName();
        final String regionDescription = " in "
            + (startRegionName.equals(lastRegionName) ? startRegionName : (startRegionName + " - " + lastRegionName));
        final StringBuilder titleBuilder = new StringBuilder();
        switch(viewMode()) {
            case OBJECT:
                TeleObject teleObject = null;
                try {
                    teleObject = vm().heap().findTeleObject(vm().originToReference(origin.asPointer()));
                } catch (MaxVMBusyException e) {
                    // Can't learn anything about the object right now.
                }
                if (teleObject == null) {
                    titleBuilder.append("Memory object: ").append(memoryWordRegion.start().toHexString());
                } else {
                    titleBuilder.append("Memory: object ").append(memoryWordRegion.start().toHexString()).append(inspection().nameDisplay().referenceLabelText(teleObject));
                }
                titleBuilder.append(regionDescription);
                break;
            case PAGE:
                titleBuilder.append("Memory: page ").append(memoryWordRegion.start().toHexString());
                titleBuilder.append(regionDescription);
                break;
            case WORD:
                if (regionName == null) {
                    titleBuilder.append("Memory: ").append(memoryWordRegion.start().toHexString()).append("--").append(memoryWordRegion.end().toHexString());
                    titleBuilder.append(regionDescription);
                } else {
                    titleBuilder.append("Memory: region ").append(regionName);
                    // No suffix; we already know the name of the region
                }
                break;
            default:
                InspectorError.unknownCase();
        }
        return titleBuilder.toString();
    }

    @Override
    protected void refreshView(boolean force) {
        table.refresh(force);
        super.refreshView(force);
    }

    /**
     * @return the number of words contained in region of VM memory.
     */
    private int wordsInRegion(MaxMemoryRegion memoryRegion) {
        return memoryRegion.size().dividedBy(wordSize.toInt()).toInt();
    }

    /**
     * Sets the view to the parameters specified when inspector was created.
     */
    private void resetToOriginal() {
        setOrigin(originalOrigin);
        setMemoryRegion(originalMemoryWordRegion);
        setViewMode(originalViewMode);
        table.setPreferredScrollableViewportSize(new Dimension(-1, preferredTableHeight()));
        table.scrollToBeginning();
        pack();
        regionName = originalRegionName;
        setTitle();
    }

    /**
     * Changes the viewing origin and updates related state; does not change the region being viewed.
     */
    private void setOrigin(Address origin) {
        this.origin = origin;
        originField.setText(origin.toUnsignedString(16));
        table.setOrigin(this.origin);
    }

    /**
     * Changes the viewed memory region and updates related state; does not change the origin.
     */
    private void setMemoryRegion(MemoryWordRegion memoryWordRegion) {
        this.memoryWordRegion = memoryWordRegion;
        wordCountField.setValue(Address.fromInt(memoryWordRegion.wordCount));
        table.setMemoryRegion(memoryWordRegion);

    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveBack() {
        switch (viewMode()) {
            case OBJECT:
                moveToPreviousObject();
                break;
            case PAGE:
                moveToPreviousPage();
                break;
            case WORD:
                growRegionUp(1);
                break;
            default:
                InspectorError.unknownCase();
        }
    }

    /**
     * Modal navigation; the kind of move depends on the currently selected view mode.
     */
    private void moveForward() {
        switch (viewMode()) {
            case OBJECT:
                moveToNextObject();
                break;
            case PAGE:
                moveToNextPage();
                break;
            case WORD:
                growRegionDown(1);
                break;
            default:
                InspectorError.unknownCase();
        }
    }

    /**
     * Grows the viewed region at the top (lowest address).
     */
    private void growRegionUp(int addedRowCount) {
        final int newWordCount = memoryWordRegion.wordCount + addedRowCount;
        final Address newStart = memoryWordRegion.start().minus(wordSize.times(addedRowCount));
        setMemoryRegion(new MemoryWordRegion(vm(), newStart, newWordCount, wordSize));
        table.scrollToBeginning();
        // User model policy:  any adjustment to the region drops into generic word mode
        clearViewMode();
        setTitle();
    }

    /**
     * Grows the viewed region at the bottom (highest address).
     */
    private void growRegionDown(int addedRowCount) {
        final int newWordCount = memoryWordRegion.wordCount + addedRowCount;

        setMemoryRegion(new MemoryWordRegion(vm(), memoryWordRegion.start(), newWordCount, wordSize));
        table.scrollToEnd();
        // User model policy:  any adjustment to the region drops into generic word mode
        clearViewMode();
        setTitle();
    }

    private void moveToCurrentObject() {
        TeleObject teleObject = vm().heap().findObjectAt(origin);
        if (teleObject != null) {
            MaxMemoryRegion objectMemoryRegion = teleObject.objectMemoryRegion();
            final Address start = objectMemoryRegion.start().aligned(wordSize.toInt());
            // User model policy, grow the size of the viewing region if needed, but never shrink it.
            final int newWordCount = Math.max(wordsInRegion(objectMemoryRegion), memoryWordRegion.wordCount);
            setMemoryRegion(new MemoryWordRegion(vm(), start, newWordCount, wordSize));
            setOrigin(teleObject.origin());
            table.scrollToOrigin();
            setTitle();
        } else {
            moveToPreviousObject();
        }
    }

    private void moveToPreviousObject() {
        final TeleObject teleObject = vm().heap().findObjectPreceding(origin, 1000000);
        if (teleObject != null) {
            MaxMemoryRegion objectMemoryRegion = teleObject.objectMemoryRegion();
            final Address start = objectMemoryRegion.start().aligned(wordSize.toInt());
            // User model policy, grow the size of the viewing region if needed, but never shrink it.
            final int newWordCount = Math.max(wordsInRegion(objectMemoryRegion), memoryWordRegion.wordCount);
            setMemoryRegion(new MemoryWordRegion(vm(), start, newWordCount, wordSize));
            setOrigin(teleObject.origin());
            table.scrollToOrigin();
            setTitle();
        }
    }

    private void moveToNextObject() {
        final TeleObject teleObject = vm().heap().findObjectFollowing(origin, 1000000);
        if (teleObject != null) {
            final MaxMemoryRegion objectMemoryRegion = teleObject.objectMemoryRegion();
            // Start stays the same
            final Address start = memoryWordRegion.start();
            // Default is to leave the viewed size the same
            int newWordCount = memoryWordRegion.wordCount;
            if (!memoryWordRegion.contains(objectMemoryRegion.end())) {
                // Grow the end of the viewed region if needed to include the newly found object
                newWordCount = objectMemoryRegion.end().minus(start).dividedBy(wordSize).toInt();
            }
            setMemoryRegion(new MemoryWordRegion(vm(), start, newWordCount, wordSize));
            setOrigin(teleObject.origin());
            // Scroll so that whole object is visible if possible
            table.scrollToRange(origin, objectMemoryRegion.end().minus(wordSize));
            setTitle();
        }
    }

    private void moveToCurrentPage() {
        Address newOrigin = origin.aligned(pageSize.toInt());
        if (!newOrigin.equals(origin)) {
            // We're not at a page boundary, so set to the beginning of the current one.
            newOrigin = newOrigin.minus(pageSize);
        }
        setOrigin(newOrigin);
        setMemoryRegion(new MemoryWordRegion(vm(), newOrigin, wordsInPage, wordSize));
        table.scrollToBeginning();
        setTitle();
    }

    private void moveToNextPage() {
        Address nextOrigin = origin.aligned(pageSize.toInt());
        if (origin.equals(nextOrigin)) {
            // Already at beginning of a page; jump to next.
            nextOrigin = nextOrigin.plus(pageSize);
        }
        setOrigin(nextOrigin);
        setMemoryRegion(new MemoryWordRegion(vm(), nextOrigin, wordsInPage, wordSize));
        table.scrollToBeginning();
        setTitle();
    }

    private void moveToPreviousPage() {
        final Address newOrigin = origin.aligned(pageSize.toInt()).minus(pageSize);
        setOrigin(newOrigin);
        setMemoryRegion(new MemoryWordRegion(vm(), newOrigin, wordsInPage, wordSize));
        table.scrollToBeginning();
        setTitle();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                final MemoryWordsViewPreferences globalPreferences = globalPreferences(inspection());
                new TableColumnVisibilityPreferences.ColumnPreferencesDialog<MemoryWordsColumnKind>(inspection(), "View Options", instanceViewPreferences, globalPreferences);
            }
        };
    }

    @Override
    public void threadFocusSet(MaxThread oldThread, MaxThread thread) {
        // Memory inspector displays are sensitive to the current thread selection (for register values)
        refreshView(true);
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address newAddress) {
        refreshView(true);
    }

    @Override
    public void inspectorClosing() {
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing for " + getTitle());
        inspectors.remove(this);
        super.inspectorClosing();
    }

    @Override
    public void watchpointSetChanged() {
        if (vm().state().processState() != ProcessState.TERMINATED) {
            refreshView(true);
        }
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    /**
     * @return the region of VM memory current in view in this inspector.
     */
    public MaxMemoryRegion getCurrentMemoryRegion() {
        return memoryWordRegion;
    }

    private InspectorAction cloneAction = new InspectorAction(inspection(), "Clone") {
        @Override
        protected void procedure() {
            final Inspector inspector = new MemoryWordsInspector(inspection(), memoryWordRegion, regionName, origin, viewMode(), instanceViewPreferences);
            inspector.highlight();
        }
    };

    private InspectorAction setOriginToSelectionAction = new InspectorAction(inspection(), "Set Origin to selected location") {
        @Override
        protected void procedure() {
            setOrigin(focus().address());
            MemoryWordsInspector.this.refreshView(true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(memoryWordRegion.contains(focus().address()));
        }
    };

    private InspectorAction scrollToFocusAction = new InspectorAction(inspection(), "Scroll to selected memorylocation") {
        @Override
        protected void procedure() {
            table.scrollToAddress(focus().address());
            MemoryWordsInspector.this.refreshView(true);
        }

        @Override
        public void refresh(boolean force) {
            setEnabled(memoryWordRegion.contains(focus().address()));
        }
    };

    private InspectorAction inspectBytesAction = new InspectorAction(inspection(), "Inspect memory at Origin as bytes") {
        @Override
        protected void procedure() {
            MemoryBytesInspector.create(inspection(), origin);
        }
    };

}
