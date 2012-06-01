package edu.unc.genomics.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BBTotalSummaryBlock;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.RPChromosomeRegion;
import org.broad.igv.bbfile.WigItem;

import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;

/**
 * A BigWig file. For more information, see: http://genome.ucsc.edu/goldenPath/help/bigWig.html
 * 
 * BigWig and regular ASCII-text Wig files may be used interchangeably as the base class WigFile,
 * and the correct format (Wig/BigWig) can be autodetected by calling WigFile.autodetect()
 * 
 * Wrapper for Broad Institute BigWig classes
 * See: http://code.google.com/p/bigwig/
 * 
 * @author timpalpant
 *
 */
public class BigWigFileReader extends WigFileReader {
	
	private static final Logger log = Logger.getLogger(BigWigFileReader.class);
	
	private BBFileReader reader;
	private BBTotalSummaryBlock summary;

	public BigWigFileReader(Path p) throws IOException {
		super(p);
		
		log.debug("Opening BigWig file reader "+p);
		reader = new BBFileReader(p.toString());
		summary = reader.getTotalSummaryBlock();
	}
	
	public static boolean isBigWig(Path p) throws IOException {
		boolean isBigWig = false;
		try {
			BBFileReader reader = new BBFileReader(p.toString());
			isBigWig = reader.isBigWigFile();
		} catch (RuntimeException e) { }
		return isBigWig;
	}
	
	@Override
	public void close() { 
		log.debug("Closing BigWig file reader "+p);
	}
	
	@Override
	public synchronized Contig query(Interval interval) throws WigFileException {
		if (!includes(interval)) {
			throw new WigFileException("BigWigFile does not contain data for region: "+interval);
		}
		
		BigWigIterator it = reader.getBigWigIterator(interval.getChr(), interval.low()-1, interval.getChr(), interval.high(), false);
		float[] values = new float[interval.length()];
		Arrays.fill(values, Float.NaN);
		int count = 0;
		while (it.hasNext()) {
			WigItem item = it.next();
			float value = item.getWigValue();
			if (!Float.isNaN(value)) {
				count++;
				for (int bp = item.getStartBase()+1; bp <= item.getEndBase(); bp++) {
					if(interval.includes(bp)) {
						values[bp-interval.low()] = value;
					}
				}
			}
		}
		log.debug("Collected values from "+count+" Wig entries");
		
		if (interval.isCrick()) {
			ArrayUtils.reverse(values);
		}
		
		return new Contig(interval, values);
	}

	@Override
	public Set<String> chromosomes() {
		return new LinkedHashSet<String>(reader.getChromosomeNames());
	}

	@Override
	public int getChrStart(String chr) {
		int chrID = reader.getChromosomeID(chr);
		RPChromosomeRegion region = reader.getChromosomeBounds(chrID, chrID);
		return region.getStartBase()+1;
	}

	@Override
	public int getChrStop(String chr) {
		int chrID = reader.getChromosomeID(chr);
		RPChromosomeRegion region = reader.getChromosomeBounds(chrID, chrID);
		return region.getEndBase();
	}

	@Override
	public boolean includes(String chr, int start, int stop) {
		int chrID = reader.getChromosomeID(chr);
		if (chrID == -1) { return false; }
		RPChromosomeRegion region = reader.getChromosomeBounds(chrID, chrID);
		return region.getStartBase() <= start && region.getEndBase() >= stop;
	}

	@Override
	public boolean includes(String chr) {
		return reader.getChromosomeID(chr) != -1;
	}

	@Override
	public long numBases() {
		return summary.getBasesCovered();
	}

	@Override
	public double total() {
		return summary.getSumData();
	}

	@Override
	public double mean() {
		return total() / numBases();
	}

	@Override
	public double stdev() {
		return Math.sqrt(summary.getSumSquares()/numBases() - Math.pow(mean(), 2));
	}

	@Override
	public double min() {
		return summary.getMinVal();
	}

	@Override
	public double max() {
		return summary.getMaxVal();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("BigWig file:\n");
		
		for (String chr : chromosomes()) {
			s.append("Chromosome ").append(chr).append(", start=").append(getChrStart(chr));
			s.append(", stop=").append(getChrStop(chr)).append("\n");
		}
		
		s.append("Basic Statistics:\n");
		s.append("\tMean:\t\t\t").append(mean()).append("\n");
		s.append("\tStandard Deviation:\t").append(stdev()).append("\n");
		s.append("\tTotal:\t\t\t").append(total()).append("\n");
		s.append("\tBases Covered:\t\t").append(numBases()).append("\n");
		s.append("\tMin value:\t\t").append(min()).append("\n");
		s.append("\tMax value:\t\t").append(max());
		
		return s.toString();
	}

}
