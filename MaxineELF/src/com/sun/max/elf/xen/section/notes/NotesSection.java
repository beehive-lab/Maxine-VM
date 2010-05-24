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
package com.sun.max.elf.xen.section.notes;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.sun.max.elf.ELFDataInputStream;
import com.sun.max.elf.ELFHeader;
import com.sun.max.elf.ELFSectionHeaderTable;
import com.sun.max.elf.xen.ImproperDumpFileException;

/**
 * Represents the notes section in the xen core dump elf file
 *
 * @author Puneeet Lakhina
 *
 */
public class NotesSection {

    private ELFDataInputStream elfdis;
    private NoneNoteDescriptor noneNoteDescriptor;
    private HeaderNoteDescriptor headerNoteDescriptor;
    private XenVersionDescriptor xenVersionDescriptor;
    private FormatVersionDescriptor formatVersionDescriptor;
    private ELFHeader elfHeader;
    private ELFSectionHeaderTable.Entry sectionHeader;
    private RandomAccessFile dumpraf;

    static enum DescriptorType {
        NONE, HEADER, XEN_VERSION, FORMAT_VERSION;

        public static DescriptorType fromIntType(int type) {
            switch (type) {
                case 0x2000000:
                    return NONE;
                case 0x2000001:
                    return HEADER;
                case 0x2000002:
                    return XEN_VERSION;
                case 0x2000003:
                    return FORMAT_VERSION;
                default:
                    throw new IllegalArgumentException("Improper type value");
            }
        }
    };

    public NotesSection(RandomAccessFile raf, ELFHeader elfheader, ELFSectionHeaderTable.Entry sectionHeader) {
        this.dumpraf = raf;
        this.elfHeader = elfheader;
        this.sectionHeader = sectionHeader;
    }

    public void read() throws IOException, ImproperDumpFileException {
        dumpraf.seek(sectionHeader.getOffset());
        this.elfdis = new ELFDataInputStream(elfHeader, dumpraf);
        // readNone
        // readHeader
        // readVersion
        /*
         * the layout of the notes sections is Name Size (4 bytes) Descriptor Size (4 bytes) Type (4 bytes) - usually
         * interpreted as Int. Name Descriptor
         */
        /* the Name is always Xen and in case of notes section in thus coredump thus is length = 4 (including nullbyte) */
        int readLength = 0;
        while (readLength < sectionHeader.getSize()) {
            int nameLength = elfdis.read_Elf64_Word();
            if (nameLength != 4) {
                throw new ImproperDumpFileException("Length of name in notes section must be 4");
            }
            int descriptorlength = elfdis.read_Elf64_Word();
            DescriptorType type = DescriptorType.fromIntType(elfdis.read_Elf64_Word());
            String name = readString(nameLength);
            if (!name.equals("Xen")) {
                throw new ImproperDumpFileException("Name of each descriptor in the notes section should be xen");
            }
            readLength += (12 + nameLength);
            switch (type) {
                case NONE:
                    if (descriptorlength != 0) {
                        throw new ImproperDumpFileException("None descriptor should be 0 length");
                    }
                    this.noneNoteDescriptor = new NoneNoteDescriptor();
                    readLength += descriptorlength;
                    break;
                case HEADER:
                    readHeaderDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;
                case XEN_VERSION:
                    readXenVersionDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;

                case FORMAT_VERSION:
                    readFormatVersionDescriptor(descriptorlength);
                    readLength += descriptorlength;
                    break;
            }
        }
    }

    private void readHeaderDescriptor(int length) throws IOException, ImproperDumpFileException {
        if (length != 32) {
            throw new ImproperDumpFileException("Length of the header section should be 32 bytes");
        }
        this.headerNoteDescriptor = new HeaderNoteDescriptor();
        this.headerNoteDescriptor.setMagicnumber(elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setVcpus((int)elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setNoOfPages(elfdis.read_Elf64_XWord());
        this.headerNoteDescriptor.setPageSize(elfdis.read_Elf64_XWord());
    }

    private void readXenVersionDescriptor(int length) throws IOException, ImproperDumpFileException {
        this.xenVersionDescriptor = new XenVersionDescriptor();
        // 1272 =
        // sizeof(majorversion)+sizeof(minorversion)+sizeof(extraversion)+sizeof(compileinfo)+sizeof(capabilitiesinfo)+sizeof(changesetinfo)+sizeof(pagesize)
        // the platform param length is platform dependent thus we deduce it based on the total size
        int platformParamLength = length - 1272;
        if (platformParamLength != 4 && platformParamLength != 8) {
            throw new ImproperDumpFileException("Improper xen version descriptor");
        }
        this.xenVersionDescriptor.setMajorVersion(elfdis.read_Elf64_XWord());
        this.xenVersionDescriptor.setMinorVersion(elfdis.read_Elf64_XWord());
        this.xenVersionDescriptor.setExtraVersion(readString(XenVersionDescriptor.EXTRA_VERSION_LENGTH));
        this.xenVersionDescriptor.setCompileInfo(readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILER_LENGTH),
                        readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILE_BY_LENGTH), readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILER_DOMAIN_LENGTH),
                        readString(XenVersionDescriptor.CompileInfo.COMPILE_INFO_COMPILE_DATE_LENGTH));
        this.xenVersionDescriptor.setCapabilities(readString(XenVersionDescriptor.CAPABILITIES_LENGTH));
        this.xenVersionDescriptor.setChangeSet(readString(XenVersionDescriptor.CHANGESET_LENGTH));
        if(platformParamLength == 4) {
            this.xenVersionDescriptor.setPlatformParamters(elfdis.read_Elf64_Word());
        }else {
            this.xenVersionDescriptor.setPlatformParamters(elfdis.read_Elf64_XWord());
        }
        this.xenVersionDescriptor.setPageSize(elfdis.read_Elf64_XWord());
    }

    private void readFormatVersionDescriptor(int length) throws IOException, ImproperDumpFileException {
        if (length != 8) {
            throw new ImproperDumpFileException("the format version notes descriptor should be 8 bytes");
        }

        this.formatVersionDescriptor = new FormatVersionDescriptor();
        this.formatVersionDescriptor.setFormatVersion(this.elfdis.read_Elf64_XWord());
    }

    /**
     * Read a string from the file with length length. The returned string is of size length - 1 as java strings arent
     * null terminated
     *
     * @param length
     * @return
     */
    private String readString(int length) throws IOException {
        byte[] arr = new byte[length - 1];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = elfdis.read_Elf64_byte();
        }
        elfdis.read_Elf64_byte();
        return new String(arr);

    }

    public String toString() {
        return "Header:"+headerNoteDescriptor != null ? headerNoteDescriptor.toString():null;
    }


    /**
     * @return the headerNoteDescriptor
     */
    public HeaderNoteDescriptor getHeaderNoteDescriptor() {
        return headerNoteDescriptor;
    }


    /**
     * @param headerNoteDescriptor the headerNoteDescriptor to set
     */
    public void setHeaderNoteDescriptor(HeaderNoteDescriptor headerNoteDescriptor) {
        this.headerNoteDescriptor = headerNoteDescriptor;
    }



}
