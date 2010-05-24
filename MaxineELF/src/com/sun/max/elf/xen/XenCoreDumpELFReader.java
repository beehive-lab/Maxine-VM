/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems, licensed from the University of California. UNIX is a
 * registered trademark in the U.S. and in other countries, exclusively licensed through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the
 * U.S. and other countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in
 * other countries. Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether
 * direct or indirect, are strictly prohibited. Export or reexport to countries subject to U.S. embargo or to entities
 * identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 */
package com.sun.max.elf.xen;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import com.sun.max.elf.ELFHeader;
import com.sun.max.elf.ELFLoader;
import com.sun.max.elf.ELFSectionHeaderTable;
import com.sun.max.elf.ELFHeader.FormatError;
import com.sun.max.elf.xen.section.notes.NotesSection;
import com.sun.max.elf.xen.section.pages.PagesSection;
import com.sun.max.elf.xen.section.prstatus.GuestContext;

/**
 * @author Puneeet Lakhina
 *
 */
public class XenCoreDumpELFReader {

    public static final String NOTES_SECTION_NAME = ".note.Xen";
    public static final String CONTEXT_SECTION_NAME = ".xen_prstatus";
    public static final String SHARED_INFO_SECTION_NAME = ".xen_shared_info";
    public static final String P2M_SECTION_NAME = ".xen.p2m";
    public static final String PFN_SECTION_NAME = ".xen.pfn";
    public static final String XEN_PAGES_SECTION_NAME = ".xen_pages";

    private RandomAccessFile fis;
    private ELFHeader header;

    private ELFSectionHeaderTable sectionHeaderTable;
    private ELFSectionHeaderTable.Entry notesSectionHeader;
    private ELFSectionHeaderTable.Entry contextSectionHeader;
    private ELFSectionHeaderTable.Entry pagesSectionHeader;
    private ELFSectionHeaderTable.Entry p2mSectionHeader;

    private NotesSection notesSection;
    private PagesSection pagesSection;
    private int vcpus;
    GuestContext[] guestContexts;

    public XenCoreDumpELFReader(File dumpFile) throws IOException, FormatError {
        this(new RandomAccessFile(dumpFile, "r"));
    }

    public XenCoreDumpELFReader(RandomAccessFile raf) throws IOException, FormatError {
        this.fis = raf;
        this.header = ELFLoader.readELFHeader(fis);
        this.sectionHeaderTable = ELFLoader.readSHT(raf, header);
        for (ELFSectionHeaderTable.Entry entry : sectionHeaderTable.entries) {
            String sectionName = entry.getName();
            System.out.println(sectionName);
            if (NOTES_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                notesSectionHeader = entry;
            }
            if (CONTEXT_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                contextSectionHeader = entry;
            }
            if (XEN_PAGES_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                pagesSectionHeader = entry;
            }
            if (P2M_SECTION_NAME.equalsIgnoreCase(sectionName)) {
                p2mSectionHeader = entry;
            }
        }

    }

    public NotesSection getNotesSection() throws IOException, ImproperDumpFileException {
        if (notesSection == null) {
            notesSection = new NotesSection(fis, header, notesSectionHeader);
            notesSection.read();
        }
        return notesSection;
    }

    public GuestContext getGuestContext(int cpuid) throws IOException, ImproperDumpFileException {
        int vcpus = getVcpus();
        if(vcpus == -1) {
            throw new IOException("Couldnt not get no of vcpus from noets section");
        }
        if(cpuid < 0 || cpuid >= vcpus) {
            throw new IllegalArgumentException("Improper CPU Id value");
        }
        if (guestContexts == null) {
            guestContexts = new GuestContext[vcpus];
        }
        if (guestContexts[cpuid] == null) {
            guestContexts = new GuestContext[vcpus];
            GuestContext context = new GuestContext(fis, header, contextSectionHeader, cpuid);
            context.read();
            guestContexts[cpuid] = context;
            return context;
        }else {
            return guestContexts[cpuid];
        }
    }

    public GuestContext[] getAllGuestContexts() throws IOException, ImproperDumpFileException {
        if (guestContexts == null) {
            for (int i = 0; i < getVcpus(); i++) {
                getGuestContext(i);
            }
        }
        return guestContexts;
    }

    public PagesSection getPagesSection() throws IOException,ImproperDumpFileException {
        if (pagesSection != null) {
            pagesSection = new PagesSection(fis, pagesSectionHeader, p2mSectionHeader, header, getNotesSection().getHeaderNoteDescriptor().getNoOfPages(), getNotesSection()
                            .getHeaderNoteDescriptor().getPageSize());
        }
        return pagesSection;

    }



    public int getVcpus() {
        try {
            if (this.vcpus == 0) {
                vcpus = (int) getNotesSection().getHeaderNoteDescriptor().getVcpus();
            }
            return vcpus;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }



}
