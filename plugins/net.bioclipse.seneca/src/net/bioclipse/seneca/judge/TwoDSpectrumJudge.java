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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLWriter;

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
	List<TwoDRule> couplings;
	/**
	 * Description of the Field
	 */
	int scores[] = new int[7];

	/**
	 * Description of the Field
	 */
	private int cutOff;

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
		init();
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
		init();
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
		maxScore = couplings.size() * max;
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
		if (couplings == null) {
			resultString = "No signals available for " + name;
			return new JudgeResult(0, 0, 0, resultString);
		}
		scoreSum = 0;
		int satisfiedSignals = 0;
		int plength = 0;
		if (debug) {
			System.out.println("TwoDSpectrumJudge->evaluate()->rules.size(): "
					+ couplings.size());
		}
		if (debug) {
			System.out.println(ac);
		}

		Iterator<TwoDRule> it = couplings.iterator();
		while(it.hasNext()){
			TwoDRule cvalue = it.next();
			if (debug) {
				System.out
						.println("TwoDSpectrumJudge->evaluate()->rule.atom1: "
								+ cvalue.value1);
				System.out
						.println("TwoDSpectrumJudge->evaluate()->rule.atom2: "
								+ cvalue.value2);
			}
			List<IAtom> sphere = new ArrayList<IAtom>();
			sphere.add(findAtom(cvalue.value1, ac));
			plength = 
				PathTools.breadthFirstTargetSearch(
						ac, sphere, findAtom(cvalue.value2, ac), 0, cutOff);
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
		resultString = satisfiedSignals + "/" + couplings.size()
				+ " Signals satisfied in " + name + ". Score " + scoreSum + "/"
				+ maxScore;
		if (debug)
			System.out.println(resultString);
		return
			new JudgeResult(maxScore, scoreSum, satisfiedSignals, resultString);
	}

	private IAtom findAtom(double value1, IAtomContainer ac) {
		for(int i=0;i<ac.getAtomCount();i++){
			if((Double)ac.getAtom(i).getProperty(HMBCJudge.C_SHIFT)==value1
					|| (ac.getAtom(i).getProperty(HMBCJudge.H_SHIFT)!=null && (Double)ac.getAtom(i).getProperty(HMBCJudge.H_SHIFT)==value1)
				    || (ac.getAtom(i).getProperty(HMBCJudge.H_SHIFT_2)!=null && (Double)ac.getAtom(i).getProperty(HMBCJudge.H_SHIFT_2)==value1)){
				return ac.getAtom(i);
			}
		}
		return null;
	}

	/**
	 * Description of the Method
	 *
	 * @return Description of the Returned Value
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (name == null || couplings == null) {
			return "";
		}
		sb.append("Configuration of TwoDSpectrumJudge " + name + ":\n");
		sb.append("Listing relation of nuclei by number...\n");
		Iterator<TwoDRule> it = couplings.iterator();
		while(it.hasNext()){
			TwoDRule cvalue = it.next();
			sb.append(cvalue.value1 + "-" + cvalue.value2 + "\n");
		}
		sb.append("End of listing");
		return sb.toString();
	}

	public int getNumberOf2DSignals() {
		return couplings.size();
	}

	public int[] getScores() {
		return scores;
	}
	
	public class TwoDRule {
		double value1;
		double value2;

		public TwoDRule(TwoDRule other) {
			this.value1 = other.value1;
			this.value2 = other.value2;
		}

		public TwoDRule(double a1, double a2) {
			this.value1 = a1;
			this.value2 = a2;
		}

	}

}
