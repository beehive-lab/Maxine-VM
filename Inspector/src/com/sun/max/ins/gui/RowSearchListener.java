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
package com.sun.max.ins.gui;

import com.sun.max.collect.*;


/**
 * Callback interface for views that incorporate of row-based searching and
 * allow forward and backward navigation among the most recent set of matching rows.
 *
 * @author Michael Van De Vanter
 */
public interface RowSearchListener {

    /**
     * Notifies the result of a new table search.
     *
     * @param searchMatchingRows the rows that match the supplied pattern, length=0 if no matches, null if pattern is empty (no search).
     */
    void searchResult(IndexedSequence<Integer> searchMatchingRows);

    /**
     * Notifies that the user has requested to see the next match, relative to the current selection, of the most recent search.
     * This is not supposed to happen if the most recent search produced no matches.
     */
    void selectNextResult();

    /**
     * Notifies that the user has requested to see the previous match, relative to the current selection, of the most recent search.
    * This is not supposed to happen if the most recent search produced no matches.
     */
    void selectPreviousResult();

    /**
     * Notifies that the user has requested that the search be closed.
     */
    void closeSearch();
}
