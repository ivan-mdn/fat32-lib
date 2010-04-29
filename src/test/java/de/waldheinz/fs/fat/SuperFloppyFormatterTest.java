
package de.waldheinz.fs.fat;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.util.FileDisk;
import de.waldheinz.fs.util.RamDisk;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class SuperFloppyFormatterTest {
    
    @Test
    public void testSetVolumeLabel() throws IOException {
        System.out.println("setVolumeLabel");

        final String label = "Vol Label";

        BlockDevice dev = new RamDisk(50 * 1024 * 1024);
        FatFileSystem fs = SuperFloppyFormatter.get(dev).
                setFatType(FatType.FAT32).setVolumeLabel(label).format();
        assertEquals(label, fs.getVolumeLabel());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFat12FormatInvalid() throws IOException {
        System.out.println("fat12Format (invalid)");

        BlockDevice dev = new RamDisk(16800000);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT12).format();
    }
    
    @Test
    public void testFat12FormatValid() throws IOException {
        System.out.println("fat12Format (valid)");

        RamDisk dev = new RamDisk(16700000);
        
        FatFileSystem fs = SuperFloppyFormatter.get(dev).
                setFatType(FatType.FAT12).format();

        assertTrue (fs.getBootSector() instanceof Fat16BootSector);
        final Fat16BootSector bs = (Fat16BootSector) fs.getBootSector();
        
        System.out.println("sectors=" + bs.getNrTotalSectors());
        System.out.println("sectors per cluster=" + bs.getSectorsPerCluster());
        System.out.println("clusters=" +
                bs.getNrTotalSectors() / bs.getSectorsPerCluster());

        ByteBuffer b = dev.getBuffer();
        b.position(0);
        b.limit(512);
        
        Utils.hexDump(System.out, b);
        
        assertEquals(FatType.FAT12, fs.getFatType());
        assertEquals("FAT12   ", bs.getFileSystemTypeLabel());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFat16FormatTooSmall() throws IOException {
        System.out.println("fat16Format (too small)");

        BlockDevice dev = new RamDisk(4 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT16);
    }

    @Test
    public void testFat16FormatValid() throws IOException {
        System.out.println("fat16Format (valid)");
        
        BlockDevice dev = new RamDisk(16 * 1024 * 1024);
        final FatFileSystem fs = SuperFloppyFormatter.get(dev).
                setFatType(FatType.FAT16).format();

        assertEquals(FatType.FAT16, fs.getFatType());
        assertEquals("FAT16   ", fs.getBootSector().getFileSystemTypeLabel());
    }
    
    @Test(expected=IllegalArgumentException.class) @Ignore
    public void testFat16FormatTooBig() throws IOException {
        System.out.println("fat16Format (too big)");
        final File file = File.createTempFile("fat32-test", ".img");
        
        try {
            BlockDevice dev = FileDisk.create(file, 3 * 1024 * 1024 * 1024);
            SuperFloppyFormatter.get(dev).setFatType(FatType.FAT16);
        } finally {
            file.delete();
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFat32FormatInvalid() throws IOException {
        System.out.println("fat32Format (invalid)");

        BlockDevice dev = new RamDisk(16 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT32);
    }
    
    @Test
    public void testFat32FormatValid() throws IOException {
        System.out.println("fat32Format (valid)");

        BlockDevice dev = new RamDisk(50 * 1024 * 1024);
        FatFileSystem fs = SuperFloppyFormatter.get(dev).
                setFatType(FatType.FAT32).format();
        
        assertEquals(FatType.FAT32, fs.getFatType());
        assertEquals("FAT32   ", fs.getBootSector().getFileSystemTypeLabel());

        fs.getRoot().addFile("this is another looooong name");
        fs.getRoot().addDirectory("this is a sub-directory!!!");
        fs.close();
    }
    
    @Test
    public void testAutoFormat() throws IOException {
        System.out.println("autoFormat");
        
        RamDisk rd = new RamDisk(1024 * 1024);
        FatFileSystem fs = SuperFloppyFormatter.get(rd).format();
        assertNotNull(fs.getRoot());
    }
}
