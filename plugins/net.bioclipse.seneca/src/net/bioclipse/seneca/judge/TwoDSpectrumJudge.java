/*
 *  TwoDSpectrumJudge.java
 *
 *  Copyright (C) 1997, 1998, 1999, 2000  Christoph Steinbeck
 *
 *  Contact: c.steinbeck@uni-koeln.de
 *
 *  This software is published and distributed under artistic license.
 *  The intent of this license is to state the conditions under which this Package
 *  may be copied, such that the Copyright Holder maintains some semblance
 *  of artistic control over the development of the package, while giving the
 *  users of the package the right to use and distribute the Package in a
 *  more-or-less customary fashion, plus the right to make reasonable modifications.
 *
 *  THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES,
 *  INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  The complete text of the license can be found in a file called LICENSE
 *  accompanying this package.
 */
package net.bioclipse.seneca.judge;

import java.util.ArrayList;
import java.util.Vector;

import org.openscience.cdk.Molecule;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesGenerator;

/**
 * Description of the Class
 *
 * @author steinbeck
 * @created October 6, 2001
 */
public abstract class TwoDSpectrumJudge extends AbstractJudge {

	/**
	 * Description of the Field
	 */
	boolean[][][] assignment;
	/**
	 * Description of the Field
	 */
	int scores[] = new int[7];

	/**
	 * Description of the Field
	 */
	ArrayList rules = new ArrayList();
	/**
	 * Description of the Field
	 */
	protected int numberOf2DSignals = 0;
	private int cutOff;
	Vector sphere = null;

	/**
	 * Constructor for the TwoDSpectrumJudge object
	 *
	 * @param name
	 *            Description of Parameter
	 */
	public TwoDSpectrumJudge(String name) {
		super(name);
		for (int i = 0; i < scores.length; i++) {
			scores[i] = 0;
		}
		hasMaxScore = true;
		sphere = new Vector();
		// debug = true;
	}

	/**
	 * Sets the Scores attribute of the TwoDSpectrumJudge object
	 *
	 * @param scores
	 *            The new Scores value
	 */
	public void setScores(int[] scores) {
		this.scores = scores;
	}

	/**
	 * Sets the Score attribute of the TwoDSpectrumJudge object
	 *
	 * @param score
	 *            The new Score value
	 * @param position
	 *            The new Score value
	 */
	public void setScore(int score, int position) {
		if (position >= scores.length || position < 0) {
			return;
		}
		scores[position] = score;
	}

	/**
	 * Sets the CutOff attribute of the TwoDSpectrumJudge object
	 *
	 * @param cutOff
	 *            The new CutOff value
	 */
	public void setCutOff(int cutOff) {
		this.cutOff = cutOff;
	}

	/**
	 * Gets the CutOff attribute of the TwoDSpectrumJudge object
	 *
	 * @return The CutOff value
	 */
	public int getCutOff() {
		return cutOff;
	}

	/**
	 * must be called before this Judge is passed to the Structure Generators
	 * Judges vector, in order for it to function properly
	 */
	public void init() {
		for (int f = 0; f < assignment.length; f++) {
			for (int g = 0; g < assignment.length; g++) {
				for (int h = 0; h < assignment.length; h++) {
					if (assignment[f][g][h]) {
						rules.add(new TwoDRule(f, h));
					}
				}
			}
		}
		for (int i = 0; i < scores.length; i++) {
			if (scores[i] > 0) {
				cutOff = i + 1;
			}
		}
		if (debug)
			System.out.println("CutOff for pathlength search set to " + cutOff
					+ " in " + getName());
	}

	/**
	 * Description of the Method
	 */
	public void calcMaxScore() {
		int max = 0;
		for (int f = 0; f < scores.length; f++) {
			if (scores[f] > max) {
				max = scores[f];
			}
		}
		numberOf2DSignals = 0;
		for (int f = 0; f < assignment.length; f++) {
			for (int g = 0; g < assignment.length; g++) {
				for (int h = 0; h < assignment.length; h++) {
					if (assignment[f][g][h]) {
						numberOf2DSignals += 1;
					}
				}
			}
		}
		maxScore = numberOf2DSignals * max;
		if (debug)
			System.out.println("MaxScore in " + getName() + " set to "
					+ maxScore);
	}

	/**
	 * Description of the Method
	 *
	 * @param ac
	 *            Description of Parameter
	 * @return Description of the Returned Value
	 */
	public JudgeResult evaluate(IAtomContainer ac) {
		if (assignment == null) {
			resultString = "No signals available for " + name;
			return new JudgeResult(0, 0, 0, resultString);
		}
		scoreSum = 0;
		int satisfiedSignals = 0;
		int plength = 0;
		TwoDRule rule;
		if (debug) {
			System.out.println("TwoDSpectrumJudge->evaluate()->rules.size(): "
					+ rules.size());
		}
		if (debug) {
			System.out.println(ac);
		}

		for (int f = 0; f < rules.size(); f++) {
			rule = (TwoDRule) rules.get(f);
			sphere.clear();
			if (debug) {
				System.out
						.println("TwoDSpectrumJudge->evaluate()->rule.atom1: "
								+ rule.atom1);
				System.out
						.println("TwoDSpectrumJudge->evaluate()->rule.atom2: "
								+ rule.atom2);
			}
			sphere.add(ac.getAtom(rule.atom1));
			plength =
				PathTools.breadthFirstTargetSearch(
						ac, sphere, ac.getAtom(rule.atom2), 0, cutOff);
			if (debug)
				System.out.println("TwoDSpectrumJudge->evaluate()->plength: "
						+ plength);
			if (plength > 0) {
				scoreSum += scores[plength - 1];
				if (scores[plength - 1] > 0) {
					satisfiedSignals++;
				}
			}

		}
		resultString = satisfiedSignals + "/" + numberOf2DSignals
				+ " Signals satisfied in " + name + ". Score " + scoreSum + "/"
				+ maxScore;
		if (debug)
			System.out.println(resultString);
		SmilesGenerator sg = new SmilesGenerator();
		System.err.println(maxScore+" "+scoreSum);
		return
			new JudgeResult(maxScore, scoreSum, satisfiedSignals, resultString);
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Returned Value
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (name == null || assignment == null) {
			return "";
		}
		sb.append("Configuration of TwoDSpectrumJudge " + name + ":\n");
		sb.append("Listing relation of nuclei by number...\n");
		for (int f = 0; f < assignment.length; f++) {
			for (int g = 0; g < assignment.length; g++) {
				for (int h = 0; h < assignment.length; h++) {
					if (assignment[f][g][h]) {
						sb.append(f + "-" + h + "\n");
					}
				}
			}
		}
		sb.append("End of listing");
		return sb.toString();
	}

	public boolean[][][] getAssignment() {
		return assignment;
	}

	public void setAssignment(boolean[][][] assignment) {
		this.assignment = assignment;
	}

	public int getNumberOf2DSignals() {
		return numberOf2DSignals;
	}

	public void setNumberOf2DSignals(int numberOf2DSignals) {
		this.numberOf2DSignals = numberOf2DSignals;
	}

	public ArrayList getRules() {
		return rules;
	}

	public void setRules(ArrayList rules) {
		this.rules = rules;
	}

	public Vector getSphere() {
		return sphere;
	}

	public void setSphere(Vector sphere) {
		this.sphere = sphere;
	}

	public int[] getScores() {
		return scores;
	}

	public class TwoDRule {
		int atom1;
		int atom2;

		public TwoDRule(TwoDRule other) {
			this.atom1 = other.atom1;
			this.atom2 = other.atom2;
		}

		public TwoDRule(int a1, int a2) {
			this.atom1 = a1;
			this.atom2 = a2;
		}

	}

}
