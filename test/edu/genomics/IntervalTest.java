package edu.genomics;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.unc.genomics.Interval;

public class IntervalTest {
	
	protected final Interval watson = new Interval("chr23", 30, 40);
	protected final Interval crick = new Interval("chr23", 101, 95);

	@Test
	public void testToBed() {
		assertEquals("chr23\t29\t40\t.\t.\t+", watson.toBed());
		assertEquals("chr23\t94\t101\t.\t.\t-", crick.toBed());
	}

	@Test
	public void testToBedGraph() {
		assertEquals("chr23\t29\t40", watson.toBedGraph());
		assertEquals("chr23\t94\t101", crick.toBedGraph());
	}

	@Test
	public void testToGFF() {
		assertEquals("chr23\tSpotArray\tfeature\t30\t40\t.\t+\t.\tprobe_id=no_id;count=1", watson.toGFF());
		assertEquals("chr23\tSpotArray\tfeature\t95\t101\t.\t-\t.\tprobe_id=no_id;count=1", crick.toGFF());
	}

	@Test
	public void testCenter() {
		assertEquals(35, watson.center());
		assertEquals(98, crick.center());
	}

	@Test
	public void testLength() {
		assertEquals(11, watson.length());
		assertEquals(7, crick.length());
	}

	@Test
	public void testIncludes() {
		assertFalse(watson.includes(29));
		for (int i = 30; i <= 40; i++) {
			assertTrue(watson.includes(i));
		}
		assertFalse(watson.includes(41));
		assertFalse(watson.includes(-35));
		
		assertFalse(crick.includes(94));
		for (int i = 95; i <= 101; i++) {
			assertTrue(crick.includes(i));
		}
		assertFalse(crick.includes(102));
		assertFalse(crick.includes(-35));
	}

	@Test
	public void testLow() {
		assertEquals(30, watson.low());
		assertEquals(95, crick.low());
	}

	@Test
	public void testHigh() {
		assertEquals(40, watson.high());
		assertEquals(101, crick.high());
	}

	@Test
	public void testIsWatson() {
		assertTrue(watson.isWatson());
		assertFalse(crick.isWatson());
	}

	@Test
	public void testIsCrick() {
		assertFalse(watson.isCrick());
		assertTrue(crick.isCrick());
	}

	@Test
	public void testStrand() {
		assertEquals("+", watson.strand());
		assertEquals("-", crick.strand());
	}

}