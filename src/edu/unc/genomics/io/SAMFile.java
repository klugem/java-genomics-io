package edu.unc.genomics.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.samtools.BAMIndex;
import net.sf.samtools.BAMIndexMetaData;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import edu.unc.genomics.SAMEntry;
import edu.unc.genomics.util.Samtools;

/**
 * Text SAM files. Will passively convert to BAM if random queries are attempted.
 * @author timpalpant
 *
 */
public class SAMFile extends IntervalFile<SAMEntry> {
	
	private static final Logger log = Logger.getLogger(SAMFile.class);
	
	private SAMFileReader reader;
	private SAMRecordIterator it;
	private int count = 0;
	
	private Path bam;
	private Path index;
	
	public SAMFile(Path p) {
		super(p);
		reader = new SAMFileReader(p.toFile());
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
		
		if (bam != null) {
			Files.deleteIfExists(bam);
		}
		
		if (index != null) {
			Files.deleteIfExists(index);
		}
	}
	
	@Override
	public int count() {
		if (count == 0) {
			if (bam == null) {
				convertToBAM();
			}

			BAMIndex index = reader.getIndex();
	    int nRefs = reader.getFileHeader().getSequenceDictionary().size();
	    for (int i = 0; i < nRefs; i++) {
	    	BAMIndexMetaData data = index.getMetaData(i);
	    	count += data.getAlignedRecordCount();
	    	count += data.getUnalignedRecordCount();
	    }
		}
		
		return count;
	}

	@Override
	public Set<String> chromosomes() {
		Set<String> chromosomes = new HashSet<String>();
		SAMSequenceDictionary dict = reader.getFileHeader().getSequenceDictionary();
		for (SAMSequenceRecord seqRec : dict.getSequences()) {
			chromosomes.add(seqRec.getSequenceName());
		}
		return chromosomes;
	}

	@Override
	public Iterator<SAMEntry> iterator() {
		// Close any previous iterators since SAM-JDK only allows one at a time
		if (it != null) {
			it.close();
		}

		it = reader.iterator();
		return new SAMEntryIterator(it);
	}

	@Override
	public Iterator<SAMEntry> query(String chr, int start, int stop) {
		if (bam == null) {
			convertToBAM();
		}
		
		// Close any previous iterators since SAM-JDK only allows one at a time
		if (it != null) {
			it.close();
		}

		it = reader.query(chr, start, stop, false);
		return new SAMEntryIterator(it);
	}
	
	private void convertToBAM() {
		// Have to convert the SAM file to BAM to do queries
		bam = p.resolveSibling(p.getFileName()+".bam");
		Samtools.samToBam(p, bam);
		
		// Index the BAM file
		index = p.resolveSibling(bam.getFileName()+".bai");
		Samtools.indexBAMFile(bam, index);
				
		reader = new SAMFileReader(bam.toFile());
		// Ensure that we have an index
		if (!reader.hasIndex()) {
			throw new IntervalFileFormatException("Error indexing BAM file: "+bam);
		}
			
		// Turn off memory mapping to avoid BufferUnderRun exceptions
		reader.enableIndexMemoryMapping(false);
		// Turn on index caching
		reader.enableIndexCaching(true);
	}
}
