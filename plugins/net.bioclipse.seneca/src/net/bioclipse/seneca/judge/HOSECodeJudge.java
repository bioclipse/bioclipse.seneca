/* HOSECodeJudge.java
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

package net.bioclipse.seneca.judge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.BremserOneSphereHOSECodePredictor;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.xmlcml.cml.base.CMLElement;

/**
 * This Judge assigns a score to a structure depending on the the deviation of
 * the experimental 13C carbon spectrum from a backcalculated one. Currently the
 * backcalculation is very rudimentary (based on a one-sphere HOSE code
 * prediction), so that the role of this judge can only be to assure that the
 * carbon atom environment is in the correct range with respect to hybridization
 * state and hetero attachments
 */

public class HOSECodeJudge extends Abstract13CJudge {

	public int score = 100; // Score for optimum fit of exp. with calc. shift
	protected transient HOSECodeGenerator hcg;
	protected transient BremserOneSphereHOSECodePredictor predictor;

	public HOSECodeJudge() {
		super("Simple HOSECodeJudge");
		hasMaxScore = true;
		hcg = new HOSECodeGenerator();
		predictor = new BremserOneSphereHOSECodePredictor();
	}

	public void calcMaxScore() {
		maxScore = carbonShifts.length * score;
	}

	/**
	 * The methods evaluates a given structure by recalculating the carbon shift
	 * for each carbon atom using a one-sphere HOSE Code method and calculating
	 * the deviation from the experimental carbon spectrum. The deviation is
	 * normalized to 100 using confidence limit given by the HOSE code table,
	 * i.e. a deviation of excatly the size of the confidence limit is score
	 * zero, no deviation is core 100.
	 *
	 * @param bm
	 *            The bond matrix to judge
	 * @return A JudgeResult containing the score for this structure
	 */
	public JudgeResult evaluate(IAtomContainer ac) throws Exception {
		scoreSum = 0;
		debug = false;
		String hoseCode = null;
		double shift = 0, confidenceLimit = 0, deviation = 0, mediumDeviation = 0;
		int carbonCount = 0;
		for (int f = 0; f < ac.getAtomCount(); f++) {
			if (ac.getAtom(f).getSymbol().equals("C")) {
				try {
					hoseCode = hcg.makeBremserCompliant(
							hcg.getHOSECode(ac, ac.getAtom(f), 1));
					shift = predictor.predict(hoseCode);
					if (debug)
						System.out.println(
								"HOSECodeJudge -> evaluate: shift=" + shift);
					confidenceLimit = predictor.getConfidenceLimit(hoseCode);
					if (debug)
						System.out.println(
								"HOSECodeJudge -> evaluate: confidenceLimit="
										+ confidenceLimit);
					deviation = Math.abs(shift - carbonShifts[f]);
					carbonCount++;
					mediumDeviation += deviation;
					// deviation = deviation / confidenceLimit;
					// if (deviation < 1) scoreSum += (1 -
					// Math.pow(deviation,2)) * score;
					if (deviation < confidenceLimit)
						scoreSum += score;
				} catch (Exception exc) {
					exc.printStackTrace();
					throw exc;
				}
			}
		}
		String message = "Score: " + scoreSum + "/" + maxScore
				+ ", Carbon shift medium deveation: "
				+ (mediumDeviation / carbonCount);
		return new JudgeResult(maxScore, scoreSum, 0, message);
	}

    public String getDescription() {
        return "Calculates a score based on a very simple 13C NMR prediction, using "+
        "a simple score function. The WCCHOSECodeJudge and primarily NMRShiftDB judge will do much better";
    }

}
