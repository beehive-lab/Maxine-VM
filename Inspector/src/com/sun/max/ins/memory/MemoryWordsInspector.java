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
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

// TODO (mlvdv) extend range when resize window? (when there's extra space)
// TODO (mlvdv) try to make columns narrow
// TODO (mlvdv) Parameter for object search extent


/**
 * An inspector that displays the contents of a region of memory in the VM, word aligned, one word per row.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryWordsInspector extends Inspector {

    private static final int TRACE_VALUE = 1;

    private static enum NavMode {
        WORD("Word"),
        OBJECT("Obj"),
        PAGE("Page");

        private final String label;

        private NavMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static final IndexedSequence<NavMode> VALUES = new ArraySequence<NavMode>(values());

    }

    private final Size wordSize;
    private final Size pageSize;
    private final int wordsInPage;

    // The memory region specified when the Inspector was created
    private final MemoryWordRegion originalMemoryRegion;

    // Memory region currently being inspected.
    private MemoryWordRegion memoryRegion;

    // Address of word 0 for the purposes of the Offset columns.
    private Address origin;

    private MemoryWordsTable table;
    private InspectorScrollPane scrollPane;
    private JToolBar toolBar;
    private AddressInputField.Hex originField;
    private AddressInputField.Decimal wordCountField;
    private final InspectorMenuItems frameMenuItems;

    public MemoryWordsInspector(Inspection inspection, MemoryRegion memoryRegion, Address origin) {
        super(inspection);
        wordSize = inspection.maxVM().wordSize();
        pageSize = inspection.maxVM().pageSize();
        wordsInPage = pageSize.dividedBy(wordSize).toInt();
        final Address start = memoryRegion.start().aligned(wordSize.toInt());
        final int wordCount = wordsInRegion(memoryRegion);
        this.originalMemoryRegion = new MemoryWordRegion(start, wordCount, wordSize);
        this.memoryRegion = originalMemoryRegion;
        this.origin = origin == null ? start : origin;
        Trace.line(1, tracePrefix() + " creating for " + getTextForTitle());
        createFrame(null);
        gui().setLocationRelativeToMouse(this, inspection().geometry().objectInspectorNewFrameDiagonalOffset());


        frameMenuItems = new MemoryWordsFrameMenuItems();
        frame().menu().add(frameMenuItems);
        table.scrollToOrigin();
    }

    public MemoryWordsInspector(Inspection inspection, MemoryRegion memoryRegion) {
        this(inspection, memoryRegion, null);
    }

    @Override
    protected void createView() {
        final JPanel panel = new InspectorPanel(inspection(), new BorderLayout());

        toolBar = new InspectorToolBar(inspection());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        toolBar.add(new JLabel("Origin:"));
        originField = new AddressInputField.Hex(inspection(), origin) {
            @Override
            public void update(Address value) {
                if (!value.equals(origin)) {
                    updateOrigin(value.aligned(wordSize.toInt()));
                }
            }
        };
        toolBar.add(originField);

        toolBar.add(new JLabel("Words:"));
        wordCountField = new AddressInputField.Decimal(inspection(), Address.fromInt(memoryRegion.wordCount)) {
            @Override
            public void update(Address value) {
                final int newWordCount = value.toInt();
                final int oldWordCount = memoryRegion.wordCount;
                if (newWordCount <= 0) {
                    // Bogus; reset to prior value
                    wordCountField.setValue(Address.fromInt(oldWordCount));
                } else if (newWordCount != oldWordCount) {
                    updateMemoryRegion(new MemoryWordRegion(memoryRegion.start(), newWordCount, wordSize));
                }

            }
        };
        wordCountField.setRange(1, 1000);
        toolBar.add(wordCountField);

        final InspectorButton upButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                growRegionUp(1);
            }
        });
        upButton.setIcon(style().navigationUpIcon());
        upButton.setToolTipText("Grow displayed region upward (lower address)");
        toolBar.add(upButton);

        final InspectorButton downButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                growRegionDown(1);
            }
        });
        downButton.setIcon(style().navigationDownIcon());
        downButton.setToolTipText("Grow displayed region downward (lower address)");
        toolBar.add(downButton);

        final InspectorButton homeButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setToOriginalRegion();
            }
        });
        homeButton.setIcon(style().navigationHomeIcon());
        homeButton.setToolTipText("Return displayed region to original");
        toolBar.add(homeButton);

        final InspectorButton backButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveToPreviousObject();
            }
        });
        backButton.setIcon(style().navigationBackIcon());
        backButton.setToolTipText("View previous object");
        toolBar.add(backButton);

        final InspectorButton forwardButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveToNextObject();
            }
        });
        forwardButton.setIcon(style().navigationForwardIcon());
        forwardButton.setToolTipText("View next object");
        toolBar.add(forwardButton);

        final InspectorButton backPageButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveToPreviousPage();
            }
        });
        backPageButton.setIcon(style().mediaStepBackIcon());
        backPageButton.setToolTipText("View previous memory page");
        toolBar.add(backPageButton);

        final InspectorButton forwardPageButton = new InspectorButton(inspection(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveToNextPage();
            }
        });
        forwardPageButton.setIcon(style().mediaStepForwardIcon());
        forwardPageButton.setToolTipText("View next memory page");
        toolBar.add(forwardPageButton);

        panel.add(toolBar, BorderLayout.NORTH);

        table = new MemoryWordsTable(inspection(), memoryRegion, origin);
        scrollPane = new InspectorScrollPane(inspection(), table);
        //table.setPreferredScrollableViewportSize(preferredTableDimension());

        //setBounds(preferredTableDimension());

        panel.add(scrollPane, BorderLayout.CENTER);
        frame().setContentPane(panel);

        final JViewport viewport = scrollPane.getViewport();
        viewport.setPreferredSize(new Dimension(viewport.getWidth(), preferredTableHeight()));

        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                super.componentResized(componentEvent);

//                final int preferredHeight = preferredTableDimension().height;
//                final int height = scrollPane.getViewport().getBounds().height;
//                final int extraRows = (preferredHeight - height) / table.getRowHeight();
//                if (extraRows > 0) {
//                    growRegionDown(extraRows);
//                }

                final Rectangle bounds = viewport.getBounds();
                System.out.println(bounds.toString());
                System.out.println("Header=" + table.getTableHeader().getHeight());
                System.out.println("Row height=" + table.getRowHeight());
                System.out.println("Avail=" + ((bounds.height - table.getTableHeader().getHeight()) - (MemoryWordsInspector.this.memoryRegion.wordCount * table.getRowHeight())));
                final int rowCapacity = ((bounds.height - table.getTableHeader().getHeight()) - (MemoryWordsInspector.this.memoryRegion.wordCount * table.getRowHeight())) / table.getRowHeight();
                System.out.println("Capacity =" + rowCapacity);
                System.out.println("Preferred=" + preferredTableHeight());
                if (rowCapacity > 0) {
                    //growRegionDown(rowCapacity );
                }
            }
        });

    }

    @Override
    public String getTextForTitle() {
        return "Memory Words @ " + memoryRegion.start().toHexString();
    }

    @Override
    protected boolean refreshView(boolean force) {
        table.refresh(force);
        frameMenuItems.refresh(force);
        super.refreshView(force);
        return true;
    }

    /**
     * @return the number of words contained in region of VM memory.
     */
    private int wordsInRegion(MemoryRegion memoryRegion) {
        return memoryRegion.size().dividedBy(wordSize.toInt()).toInt();
    }

    /**
     * Sets the view to the parameters specified when inspector was created.
     */
    private void setToOriginalRegion() {
        updateOrigin(originalMemoryRegion.start());
        updateMemoryRegion(originalMemoryRegion);
        table.setPreferredScrollableViewportSize(new Dimension(-1, preferredTableHeight()));
        table.scrollToBeginning();
        frame().pack();
    }

    private int preferredTableHeight() {
        // Try to size the scroll pane vertically for just enough space, up to a specified maximum;
        // this is empirical, based only the fuzziest notion of how these dimensions work
        final int displayRows = Math.min(style().memoryTableMaxDisplayRows(), table.getRowCount());
        final int rowHeight = table.getRowHeight();
        final int rowMargin = table.getRowMargin();
        final int headerHeight = table.getTableHeader().getHeight();
        Trace.line(TRACE_VALUE, tracePrefix() + "rows=" + displayRows + ", rowHeight=" + rowHeight
                + ", rowMargin" + rowMargin + ", headerHeight=" + headerHeight);
        final int preferredHeight = displayRows * (rowHeight + rowMargin) + rowMargin  + headerHeight;
        Trace.line(TRACE_VALUE, tracePrefix() + "preferrerdHeight=" + preferredHeight);
        return preferredHeight;
    }

    /**
     * Changes the viewing origin and updates related state; does not change the region being viewed.
     */
    private void updateOrigin(Address origin) {
        this.origin = origin;
        originField.setText(origin.toUnsignedString(16));
        table.setOrigin(this.origin);
    }

    /**
     * Changes the viewed memory region and updates related state; does not change the origin.
     */
    private void updateMemoryRegion(MemoryWordRegion memoryWordRegion) {
        this.memoryRegion = memoryWordRegion;
        wordCountField.setValue(Address.fromInt(memoryRegion.wordCount));
        table.setMemoryRegion(memoryRegion);
    }

    /**
     * Grows the viewed region at the top (lowest address).
     */
    private void growRegionUp(int addedRowCount) {
        final int newWordCount = memoryRegion.wordCount + addedRowCount;
        final Address newStart = memoryRegion.start().minus(wordSize.times(addedRowCount));
        updateMemoryRegion(new MemoryWordRegion(newStart, newWordCount, wordSize));
        table.scrollToBeginning();
    }

    /**
     * Grows the viewed region at the bottom (highest address).
     */
    private void growRegionDown(int addedRowCount) {
        final int newWordCount = memoryRegion.wordCount + addedRowCount;
        updateMemoryRegion(new MemoryWordRegion(memoryRegion.start(), newWordCount, wordSize));
        table.scrollToEnd();
    }

    private void moveToPreviousObject() {
        final TeleObject teleObject = maxVM().findObjectPreceding(origin, 1000000);
        if (teleObject != null) {
            MemoryRegion objectMemoryRegion = teleObject.getCurrentMemoryRegion();
            final Address start = objectMemoryRegion.start().aligned(wordSize.toInt());
            // User model policy, grow the size of the viewing region if needed, but never shrink it.
            final int newWordCount = Math.max(wordsInRegion(objectMemoryRegion), memoryRegion.wordCount);
            updateMemoryRegion(new MemoryWordRegion(start, newWordCount, wordSize));
            updateOrigin(memoryRegion.start());
            table.scrollToOrigin();
        }
    }

    private void moveToNextObject() {
        final TeleObject teleObject = maxVM().findObjectFollowing(origin, 1000000);
        if (teleObject != null) {
            final MemoryRegion objectMemoryRegion = teleObject.getCurrentMemoryRegion();
            // Start stays the same
            final Address start = memoryRegion.start();
            // Set origin to beginning of next object
            final Address newOrigin = objectMemoryRegion.start().aligned(wordSize.toInt());
            // Default is to leave the viewed size the same
            int newWordCount = memoryRegion.wordCount;
            if (!memoryRegion.contains(objectMemoryRegion.end())) {
                // Grow the end of the viewed region if needed to include the newly found object
                newWordCount = objectMemoryRegion.end().minus(start).dividedBy(wordSize).toInt();
            }
            updateMemoryRegion(new MemoryWordRegion(start, newWordCount, wordSize));
            updateOrigin(newOrigin);
            // Scroll so that whole object is visible if possible
            table.scrollToRange(origin, objectMemoryRegion.end().minus(wordSize));
        }
    }

    private void moveToNextPage() {
        final Address alignedOrigin = origin.aligned(pageSize.toInt());
        final Address nextOrigin = alignedOrigin.plus(pageSize);
        updateOrigin(nextOrigin);
        updateMemoryRegion(new MemoryWordRegion(nextOrigin, wordsInPage, wordSize));
        table.scrollToBeginning();
    }

    private void moveToPreviousPage() {
        final Address alignedOrigin = origin.aligned(pageSize.toInt());
        final Address nextOrigin = alignedOrigin.minus(pageSize);
        updateOrigin(nextOrigin);
        updateMemoryRegion(new MemoryWordRegion(nextOrigin, wordsInPage, wordSize));
        table.scrollToBeginning();
    }

    @Override
    public InspectorAction getViewOptionsAction() {
        return new InspectorAction(inspection(), "View Options") {
            @Override
            public void procedure() {
                final MemoryWordsViewPreferences globalPreferences = MemoryWordsViewPreferences.globalPreferences(inspection());
                MemoryWordsViewPreferences instanceViewPreferences = table.getInstanceViewPreferences();
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
        Trace.line(1, tracePrefix() + " closing for " + getCurrentTitle());
        super.inspectorClosing();
    }

    @Override
    public void watchpointSetChanged() {
        refreshView(false);
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

    public void viewConfigurationChanged() {
        reconstructView();
    }

    private static final Predicate<Inspector> allMemoryWordsInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordsInspector;
        }
    };

    private final Predicate<Inspector> otherMemoryWordsInspectorsPredicate = new Predicate<Inspector>() {
        public boolean evaluate(Inspector inspector) {
            return inspector instanceof MemoryWordsInspector && inspector != MemoryWordsInspector.this;
        }
    };


    private final class MemoryWordsFrameMenuItems implements InspectorMenuItems {

        private InspectorAction setOriginAction = new InspectorAction(inspection(), "Set Origin to selection") {
            @Override
            protected void procedure() {
                updateOrigin(focus().address());
                MemoryWordsInspector.this.refreshView(true);
            }

            @Override
            public void refresh(boolean force) {
                setEnabled(MemoryWordsInspector.this.memoryRegion.contains(focus().address()));
            }
        };

        private InspectorAction cloneAction = new InspectorAction(inspection(), "Clone") {
            @Override
            protected void procedure() {
                new MemoryWordsInspector(inspection(), MemoryWordsInspector.this.memoryRegion, MemoryWordsInspector.this.origin);
            }
        };

        public void addTo(InspectorMenu menu) {
            setOriginAction.refresh(true);
            menu.add(setOriginAction);
            menu.add(cloneAction);
            menu.addSeparator();
            menu.add(actions().closeViews(otherMemoryWordsInspectorsPredicate, "Close other memory words inspectors"));
            menu.add(actions().closeViews(allMemoryWordsInspectorsPredicate, "Close all memory words inspectors"));
        }

        public void redisplay() {
        }

        public void refresh(boolean force) {
            setOriginAction.refresh(force);
        }


    }

}
