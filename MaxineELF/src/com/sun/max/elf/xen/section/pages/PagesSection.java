/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 *
 */
package com.sun.max.elf.xen.section.pages;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.sun.max.elf.ELFDataInputStream;
import com.sun.max.elf.ELFHeader;
import com.sun.max.elf.ELFSectionHeaderTable;


/**
 * @author Puneeet Lakhina
 *
 */
public class PagesSection {

    private RandomAccessFile raf;
    private ELFSectionHeaderTable.Entry pageSectionHeader;
    private ELFSectionHeaderTable.Entry p2mSectionHeader;
    private ELFHeader elfHeader;
    private long noOfPages;
    private long pageSize;

    public PagesSection(RandomAccessFile raf,ELFSectionHeaderTable.Entry pageSectionHeader,ELFSectionHeaderTable.Entry p2mSectionHeader, ELFHeader elfHeader,long noOfPages,long pageSize) {
        this.raf = raf;
        this.pageSectionHeader = pageSectionHeader;
        this.p2mSectionHeader = p2mSectionHeader;
        this.elfHeader=elfHeader;
        this.noOfPages = noOfPages;
        this.pageSize = pageSize;
    }

    public ELFDataInputStream getDataInputStream(long sectionLocalOffset) throws IOException {
        raf.seek(pageSectionHeader.getOffset()+sectionLocalOffset);
        return new ELFDataInputStream(elfHeader, raf);
    }

    public ELFDataInputStream getDataInputStream() throws IOException {
        return getDataInputStream(0);
    }
}
