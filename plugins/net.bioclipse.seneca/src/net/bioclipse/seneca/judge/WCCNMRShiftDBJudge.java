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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.bioclipse.seneca.util.PredictionTool;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLElements;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLPeak;
import org.xmlcml.cml.element.CMLSpectrum;

import spok.utils.SpectrumUtils;

/**
 * Calculates a score via a prediction based on NMRShiftDB data. This should 
 * normally work well with a standard 13C spectrum.
 */
public class WCCNMRShiftDBJudge extends Abstract13CJudge implements IJudge {


	private static final long serialVersionUID = 4703522691110253797L;

	protected PredictionTool predictor = null;
	private List<String> elementSymbols;

	public WCCNMRShiftDBJudge() throws IOException {
		super("NMRShiftDB Scoring (using the WCC)");
		hasMaxScore = true;
		elementSymbols = new ArrayList<String>();
		elementSymbols.add( "C" );
		predictor = new PredictionTool(elementSymbols);
	}



	public void calcMaxScore() {
		maxScore = 1;
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

		if (this.carbonShifts == null) {
			String message = "No shifts were set for the target spectrum!";
			// TODO : log
			// BioclipseConsole.writeToConsole(message);
			throw new NullPointerException(message);
		}

		scoreSum = 0;
		debug = false;
		int carbonCount = 0;
		for (int f = 0; f < ac.getAtomCount(); f++) {
			if (ac.getAtom(f).getSymbol().equals("C"))
				carbonCount++;
		}
		List<Double> shifts = new ArrayList<Double>();
		carbonCount = 0;
		for (int f = 0; f < ac.getAtomCount(); f++) {
			if (ac.getAtom(f).getSymbol().equals("C")) {
				try {
				    double predictedShift = predictor.predict(ac, ac.getAtom(f))[1];
				    if(!shifts.contains( predictedShift))
				        shifts.add( predictedShift );
					carbonCount++;
				} catch (Exception exc) {
					exc.printStackTrace();
					throw exc;
				}
			}
		}
		double[] shiftsarray = new double[shifts.size()];
		for(int i=0;i<shifts.size();i++)
		    shiftsarray[i]=shifts.get( i );
		if(shifts.contains(new Double(-1)) && shifts.size()==1)
			scoreSum=0;
		else
			scoreSum=net.bioclipse.spectrum.Activator.getDefault()
		    .getJavaSpectrumManager().calculateSimilarityWCC(
		        this.carbonShifts, shiftsarray, 20.0);
		String message = "Score: " + scoreSum + "/" + maxScore;
		System.err.println(message);
		return new JudgeResult(maxScore, scoreSum, 0, message);
	}

    public String getDescription() {
        return "Calculates a score via a prediction based on NMRShiftDB data. \n" +
        		"This should normally work well with a standard 13C spectrum.";    
    }
}
