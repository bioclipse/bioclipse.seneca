/*

 * $RCSfile: AnnealingLog.java,v $

 * $Author: steinbeck $

 * $Date: 2004/02/16 09:50:54 $

 * $Revision: 1.3 $

 *

 * Copyright (C) 1997 - 2001  Christoph Steinbeck

 *

 * Contact: c.steinbeck@uni-koeln.de

 *

 * This software is published and distributed under artistic license.

 * The intent of this license is to state the conditions under which this Package

 * may be copied, such that the Copyright Holder maintains some semblance

 * of artistic control over the development of the package, while giving the

 * users of the package the right to use and distribute the Package in a

 * more-or-less customary fashion, plus the right to make reasonable modifications.

 *

 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES,

 * INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND

 * FITNESS FOR A PARTICULAR PURPOSE.

 *

 * The complete text of the license can be found in a file called LICENSE

 * accompanying this package.

 */

package net.bioclipse.seneca.structgen;

/**
 * AnnealingLog records the development of the overall score and the
 *  temperature over time (iteration steps, i.e.)
 **/

public class AnnealingLog {

	private int startsize = 100;

	private int growsize = 100;

	private int currentsize = startsize;

	private int seriesCount = 1;

	private int typeCount = 2;

	private Float[][][] values = new Float[seriesCount][currentsize][typeCount + 1];

	public static int SCORES = 0;

	public static int TEMPERATURES = 1;

	public static int ITERATION = 0;

	public static int Y = 1;

	private int[] itemCount;

	private int iterationAxisStepsize = 500;

	public AnnealingLog() {
		this(1, 2);
	}

	/**
	 * Initializes a new Annealing Log. This class can store different types of
	 * information (temperature, score, etc.) for a number of different series
	 * (different StochasticGenerators). The regular log of one Generator will
	 * obviously have only one series but if used in the seneca client it can
	 * store the data from many servers.
	 *
	 * @param seriesCount
	 *            The number of series (number of generators)
	 * @param typeCount
	 *            The number of types of information (excluding x axis type)
	 */

	public AnnealingLog(int seriesCount, int typeCount) {
		this.seriesCount = seriesCount;
		this.typeCount = typeCount;
		values = new Float[seriesCount][currentsize][typeCount + 1];
		itemCount = new int[seriesCount];
	}

	public void addEntry(float temperature, float score) {
		float iteration = (float) (itemCount[0] * iterationAxisStepsize);
		addEntry(iteration, temperature, score);
	}

	public void addEntry(float iteration, float temperature, float score) {
		addEntry(new Float(iteration), new Float(temperature), new Float(score));
	}

	public void addEntry(Float iteration, Float temperature, Float score) {
		Float[] entries = { iteration, score, temperature };
		addEntry(0, itemCount[0], entries);
	}

	public void addEntry(int series, int item, Float[] entries) {

		if (item >= currentsize) {
			growArrays();
		}

		for (int f = 0; f < entries.length; f++) {
			values[series][item][f] = entries[f];
		}
		itemCount[series]++;
	}

	public Float[] getEntry(int series, int thisEntry) {
		Float[] entry = new Float[typeCount + 1];
		for (int f = 0; f < typeCount + 1; f++) {
			entry[f] = values[series][thisEntry][f];
		}
		return entry;
	}

	private void growArrays() {

		int oldsize = currentsize;

		currentsize += growsize;

		Float[][][] newValues = new Float[seriesCount][currentsize][typeCount + 1];

		for (int x = 0; x < seriesCount; x++) {
			for (int y = 0; y < oldsize; y++) {
				for (int z = 0; z < typeCount + 1; z++) {
					newValues[x][y][z] = values[x][y][z];
				}
			}
		}
		values = newValues;
	}

	public Float[][][] getValues() {

		Float[][][] retvalues = new Float[seriesCount][maxItemCount()][typeCount + 1];

		for (int x = 0; x < seriesCount; x++) {
			for (int y = 0; y < itemCount[x]; y++) {
				for (int z = 0; z < typeCount + 1; z++) {
					retvalues[x][y][z] = values[x][y][z];
				}
			}
		}
		return retvalues;
	}

	public int getGrowsize() {
		return this.growsize;
	}

	public void setGrowsize(int growsize) {
		this.growsize = growsize;
	}

	public int getStartsize() {
		return this.startsize;
	}

	public void setStartsize(int startsize) {
		this.startsize = startsize;
	}

	public int getIterationAxisStepsize() {
		return this.iterationAxisStepsize;
	}

	public void setIterationAxisStepsize(int iterationAxisStepsize) {
		this.iterationAxisStepsize = iterationAxisStepsize;
	}

	public int getItemCount(int series) {
		return itemCount[series];
	}

	public int getSeriesCount() {
		return this.seriesCount;
	}

	public void setSeriesCount(int seriesCount) {
		this.seriesCount = seriesCount;
	}

	public int getTypeCount() {
		return this.typeCount;
	}

	public void setTypeCount(int typeCount) {
		this.typeCount = typeCount;
	}

	public int maxItemCount() {
		int max = 0;
		for (int f = 0; f < itemCount.length; f++) {
			if (itemCount[f] > max) {
				max = itemCount[f];
			}
		}
		return max;
	}

	public String toString() {
		String s = "Annealing Log -> ";
		for (int x = 0; x < seriesCount; x++) {
			s += "Series " + x + ": ";
			for (int y = 0; y < itemCount[x]; y++) {
				s += y + "[ ";
				for (int z = 0; z < typeCount + 1; z++) {
					s += values[x][y][z] + " ";
				}
				s += "] ";
			}
			s += "\n";
		}
		return s;
	}
}
