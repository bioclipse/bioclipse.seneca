/* StructureGeneratorResult.java
 *
 * Copyright (C) 1997, 1998, 1999, 2000  Christoph Steinbeck
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

package net.bioclipse.seneca.util;

import net.bioclipse.seneca.judge.ScoreSummary;

import org.openscience.cdk.interfaces.IMolecule;

/** Class to store the result of a Structure Generation **/
public class StructureGeneratorResult {

	public FixedSizeMoleculeStack structures;
	public FixedSizeMoleculeStack scoreSummaries;
	public int size;

	public StructureGeneratorResult() {
		this.structures = new FixedSizeMoleculeStack(30);
		this.scoreSummaries = new FixedSizeMoleculeStack(30);
		this.size = 30;
	}

	public StructureGeneratorResult(int size) {
		this();
		if (size > 0) {
			this.size = size;
			structures = new FixedSizeMoleculeStack(this.size);
			scoreSummaries = new FixedSizeMoleculeStack(this.size);
		}
	}

	/** Sorts the structures in descending order with respect to the score **/
	public void sort() {
		boolean changed = false;
		Object o1, o2;
		do {
			changed = false;
			for (int f = 0; f < structures.size() - 1; f++) {
				double ssF = ((ScoreSummary) scoreSummaries.get(f)).score;
				double ssF1 = ((ScoreSummary) scoreSummaries.get(f + 1)).score;
				if (ssF < ssF1) {
					o1 = structures.get(f + 1);
					o2 = scoreSummaries.get(f + 1);
					structures.remove(f + 1);
					scoreSummaries.remove(f + 1);
					structures.insertElementAt(o1, f);
					scoreSummaries.insertElementAt(o2, f);
					changed = true;
				}
			}
		} while (changed);
	}

	public int size() {
		return structures.size();
	}

	public void removeIsomorphism() {
	}

	public void merge(StructureGeneratorResult sgr) {
		this.size += sgr.size();
		this.structures.setSize(this.structures.size() + sgr.size());
		this.scoreSummaries.setSize(this.structures.size() + sgr.size());
		for (int f = 0; f < sgr.size(); f++) {
			this.structures.push((IMolecule) sgr.structures.get(f));
			this.scoreSummaries.push((IMolecule) sgr.scoreSummaries.get(f));
		}
	}
}
