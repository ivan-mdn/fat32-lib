/*
 * $Id: FatDirEntry.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package com.meetwise.fs.fat;

import java.io.IOException;
import java.util.Date;

import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FSFile;
import com.meetwise.fs.util.DosUtils;
import com.meetwise.fs.util.LittleEndian;

/**
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
class FatDirEntry extends FatBasicDirEntry {

    /** Name of this entry */
    private ShortName shortName;
    
    /** Has this entry been deleted? */
    private boolean deleted;
    
    /** Is this entry not used? */
    private boolean unused;
    
    /** Flags of this entry */
    private int flags;

    /** Time of creation. */
    private long created;

    /** Time of last modification. */
    private long lastModified;

    /** Time of last access. */
    private long lastAccessed;

    /** First cluster of the data of this entry */
    private long startCluster;
    
    /** Length in bytes of the data of this entry */
    private long length;
    
    /** Has this entry been changed and not yet flushed to disk? */
    private boolean _dirty;
    
    /** FSDirectory this entry is a part of */
    private final AbstractDirectory parent;

    public static FatBasicDirEntry create(
            AbstractDirectory dir, byte[] src, int offset) {
        
        int flags = LittleEndian.getUInt8(src, offset + 0x0b);
        boolean r = (flags & F_READONLY) != 0;
        boolean h = (flags & F_HIDDEN) != 0;
        boolean s = (flags & F_SYSTEM) != 0;
        boolean v = (flags & F_LABEL) != 0;

        if (r && h && s && v) {
            // this is a LFN entry, don't need to parse it!
            return new FatLfnDirEntry(dir, src, offset);
        }
        
        return new FatDirEntry(dir, src, offset);
    }
    
    /**
     * Create a new entry
     * 
     * @param dir
     * @param name
     * @param ext
     */
    public FatDirEntry(AbstractDirectory dir, ShortName name) {
        super(dir);
        
        this.parent = dir;
        this.shortName = name;
        this.flags = F_ARCHIVE;
        this.created = this.lastModified = this.lastAccessed = System.currentTimeMillis();
        this._dirty = false;
    }

    /**
     * Create a new entry from a FAT directory image.
     * 
     * @param dir
     * @param src
     * @param offset
     */
    public FatDirEntry(AbstractDirectory dir, byte[] src, int offset) {
        super(dir, src, offset);

        this.parent = dir;
        unused = (src[offset] == 0);
        deleted = (LittleEndian.getUInt8(src, offset) == 0xe5);

        char[] nameArr = new char[8];
        for (int i = 0; i < nameArr.length; i++) {
            nameArr[i] = (char) LittleEndian.getUInt8(src, offset + i);
        }
        if (LittleEndian.getUInt8(src, offset) == 0x05) {
            nameArr[0] = (char) 0xe5;
        }
        
        char[] extArr = new char[3];
        for (int i = 0; i < extArr.length; i++) {
            extArr[i] = (char) LittleEndian.getUInt8(src, offset + 0x08 + i);
        }
        
        this.shortName = new ShortName(
                new String(nameArr).trim(),
                new String(extArr).trim());

        this.flags = LittleEndian.getUInt8(src, offset + 0x0b);
        this.created =
                DosUtils.decodeDateTime(LittleEndian.getUInt16(src, offset + 0x10),
                                        LittleEndian.getUInt16(src, offset + 0x0e));
        this.lastModified =
                DosUtils.decodeDateTime(LittleEndian.getUInt16(src, offset + 0x18),
                                        LittleEndian.getUInt16(src, offset + 0x16));
        this.lastAccessed =
                DosUtils.decodeDateTime(LittleEndian.getUInt16(src, offset + 0x12), 0); // time not stored
        this.startCluster = LittleEndian.getUInt16(src, offset + 0x1a);
        this.length = LittleEndian.getUInt32(src, offset + 0x1c);
        this._dirty = false;
    }

    /**
     * Returns the attribute.
     * 
     * @return int
     */
    public int getFlags() {
        return flags;
    }

    public long getCreated() {
        return created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Returns the deleted.
     * 
     * @return boolean
     */
    public boolean isDeleted() {
        return deleted;
    }
    
    public String getName() {
        return shortName.toString();
    }
    
    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return length;
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public ShortName getShortName() {
        return shortName;
    }
    
    /**
     * Returns the startCluster.
     * 
     * @return int
     */
    public long getStartCluster() {
        return startCluster;
    }

    /**
     * Returns the unused.
     * 
     * @return boolean
     */
    public boolean isUnused() {
        return unused;
    }

    /**
     * Sets the flags.
     * 
     * @param flags
     */
    public void setFlags(int flags) {
        this.flags = flags;
        setDirty();
    }

    public void setCreated(long created) {
        this.created = created;
        setDirty();
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
        setDirty();
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
        setDirty();
    }

    /**
     * Sets the deleted.
     * 
     * @param deleted The deleted to set
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        setDirty();
    }

    /**
     * Updates the length of the entry. This method is called by
     * FatFile.setLength.
     * 
     * @param newLength The length to set
     */
    public void updateLength(long newLength) {
        this.length = newLength;
        setDirty();
    }

    /**
     * Gets the single instance of the file connected to this entry. Returns
     * null if the file is 0 bytes long
     * 
     * @return File
     * @throws IOException on read error
     */
    public FSFile getFile() throws IOException {
        if (isFile()) {
            return getFatFile();
        } else {
            throw new IOException("Not a file");
        }
    }

    /**
     * Gets the directory this entry refers to. This method can only be called
     * if <code>isDirectory</code> returns true.
     *
     * @throws IOException on read error
     */
    public FSDirectory getDirectory() throws IOException {
        if (isDirectory()) {
            return getFatFile().getDirectory();
        } else {
            throw new IOException("Not a directory");
        }
    }

    /**
     * Gets the single instance of the file connected to this entry. Returns
     * null if the file is 0 bytes long
     * 
     * @return File
     */
    public FatFile getFatFile() {
        return getDir().getFile(this);
    }
    
    /**
     * Sets the name.
     * 
     * @param name The name to set
     */
    public void setName(String name) {
        this.shortName = new ShortName(name);
        setDirty();
    }

    public void setName(ShortName sn) {
        if (this.shortName.equals(sn)) return;
        this.shortName = sn;
        setDirty();
    }

    /**
     * Sets the startCluster.
     * 
     * @param startCluster The startCluster to set
     */
    protected void setStartCluster(long startCluster) {
        this.startCluster = startCluster;
        setDirty();
    }

    /**
     * Sets the unused.
     * 
     * @param unused The unused to set
     */
    public void setUnused(boolean unused) {
        this.unused = unused;
        setDirty();
    }

    public boolean isReadonly() {
        return ((flags & F_READONLY) != 0);
    }

    public void setReadonly() {
        setFlags(flags | F_READONLY);
    }

    public boolean isHidden() {
        return ((flags & F_HIDDEN) != 0);
    }

    public void setHidden() {
        setFlags(flags | F_HIDDEN);
    }

    public boolean isSystem() {
        return ((flags & F_SYSTEM) != 0);
    }

    public void setSystem() {
        setFlags(flags | F_SYSTEM);
    }

    public boolean isLabel() {
        return ((flags & F_LABEL) != 0);
    }

    public void setLabel() {
        setFlags(F_LABEL);
    }

    /**
     * Does this entry refer to a file?
     * 
     * @return 
     * @see org.jnode.fs.FSDirectoryEntry#isFile()
     */
    public boolean isFile() {
        return (!(isDirectory() || isLabel()));
    }

    /**
     * Does this entry refer to a directory?
     * 
     * @return 
     * @see org.jnode.fs.FSDirectoryEntry#isDirectory()
     */
    public boolean isDirectory() {
        return ((flags & F_DIRECTORY) != 0);
    }

    public void setDirectory() {
        setFlags(F_DIRECTORY);
    }

    public boolean isArchive() {
        return ((flags & F_ARCHIVE) != 0);
    }

    public void setArchive() {
        setFlags(flags | F_ARCHIVE);
    }

    /**
     * Write my contents to the given byte-array
     * 
     * @param dest
     * @param offset
     */
    @Override
    public void write(byte[] dest, int offset) {
        
        if (unused) {
            dest[offset] = 0;
        } else if (deleted) {
            dest[offset] = (byte) 0xe5;
        }

        final String name = shortName.getName();
        final String ext = shortName.getExt();

        for (int i = 0; i < 8; i++) {
            char ch;
            if (i < name.length()) {
                if (!isLabel()) {
                    ch = Character.toUpperCase(name.charAt(i));
                } else {
                    ch = name.charAt(i);
                }
                if (ch == 0xe5) {
                    ch = (char) 0x05;
                }
            } else {
                ch = ' ';
            }
            
            dest[offset + i] = (byte) ch;
        }

        for (int i = 0; i < 3; i++) {
            char ch;
            if (i < ext.length()) {
                if (!isLabel()) {
                    ch = Character.toUpperCase(ext.charAt(i));
                } else {
                    ch = ext.charAt(i);
                }
            } else {
                ch = ' ';
            }

            dest[offset + 0x08 + i] = (byte) ch;
        }
        
        LittleEndian.setInt8(dest, offset + 0x0b, flags);
        LittleEndian.setInt16(dest, offset + 0x0e, DosUtils.encodeTime(created));
        LittleEndian.setInt16(dest, offset + 0x10, DosUtils.encodeDate(created));
        LittleEndian.setInt16(dest, offset + 0x12, DosUtils.encodeDate(lastAccessed));
        LittleEndian.setInt16(dest, offset + 0x16, DosUtils.encodeTime(lastModified));
        LittleEndian.setInt16(dest, offset + 0x18, DosUtils.encodeDate(lastModified));
        if (startCluster > Integer.MAX_VALUE) throw new AssertionError();
        LittleEndian.setInt16(dest, offset + 0x1a, (int) startCluster);
        LittleEndian.setInt32(dest, offset + 0x1c, (int) length);
        this._dirty = false;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(64);

        b.append(getName());

        b.append(" attr=");
        if (isReadonly()) {
            b.append('R');
        }
        if (isHidden()) {
            b.append('H');
        }
        if (isSystem()) {
            b.append('S');
        }
        if (isLabel()) {
            b.append('L');
        }
        if (isDirectory()) {
            b.append('D');
        }
        if (isArchive()) {
            b.append('A');
        }
        
        b.append(" created=");
        b.append(new Date(getCreated()));
        b.append(" lastModified=");
        b.append(new Date(getLastModified()));
        b.append(" lastAccessed=");
        b.append(new Date(getLastAccessed()));
        b.append(" startCluster=");
        b.append(getStartCluster());
        b.append(" length=");
        b.append(getLength());
        
        if (deleted) {
            b.append(" deleted");
        }

        return b.toString();
    }

    /**
     * Returns the dirty.
     * 
     * @return boolean
     */
    public final boolean isDirty() {
        return _dirty;
    }

    protected final void setDirty() {
        this._dirty = true;
        parent.setDirty();
    }
}
