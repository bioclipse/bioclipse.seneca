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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.bioclipse.chemoinformatics.wizards.WizardHelper;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.spectrum.domain.IJumboSpectrum;
import net.bioclipse.spectrum.domain.JumboSpectrum;
import net.bioclipse.spectrum.editor.MetadataUtils;
import net.bioclipse.spectrum.editor.SpectrumEditor;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.BremserOneSphereHOSECodePredictor;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLElements;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLMetadata;
import org.xmlcml.cml.element.CMLMetadataList;
import org.xmlcml.cml.element.CMLPeak;
import org.xmlcml.cml.element.CMLSpectrum;

import spok.utils.SpectrumUtils;

/**
 * This Judge assigns a score to a structure depending on the the deviation of
 * the experimental 13C carbon spectrum from a backcalculated one. Currently the
 * backcalculation is very rudimentary (based on a one-sphere HOSE code
 * prediction), so that the role of this judge can only be to assure that the
 * carbon atom environment is in the correct range with respect to hybridization
 * state and hetero attachments
 */
public class WCCHOSECodeJudge extends Abstract13CJudge implements IJudge {

  private static Logger logger = Logger.getLogger(WCCNMRShiftDBJudge.class);

	protected transient HOSECodeGenerator hcg;
	protected transient BremserOneSphereHOSECodePredictor predictor;
	protected double[] carbonShifts;

	public WCCHOSECodeJudge() {
		super("Simple HOSE Code Scoring (using the WCC)");
		hasMaxScore = true;
    hcg = new HOSECodeGenerator();
    predictor = new BremserOneSphereHOSECodePredictor();
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
		scoreSum = 0;
		debug = false;
		String hoseCode = null;
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
            hoseCode = hcg.makeBremserCompliant(hcg.getHOSECode(ac, ac
                                                .getAtom(f), 1));
            double predictedShift = predictor.predict(hoseCode);
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
		scoreSum = shiftwcc(this.carbonShifts, shiftsarray, 20.0);
		String message = "Score: " + scoreSum + "/" + maxScore;
		return new JudgeResult(maxScore, scoreSum, 0, message);
	}

	public double shiftwcc(double[] positions1, double[] positions2,
			double width) {
		// one carbon per peak
		double[] intensities1 = new double[positions1.length];
		for (int i = 0; i < intensities1.length; i++)
			intensities1[i] = 1.0;
		double[] intensities2 = new double[positions2.length];
		for (int i = 0; i < intensities2.length; i++)
			intensities2[i] = 1.0;

		return net.bioclipse.spectrum.Activator.getDefault()
		    .getJavaSpectrumManager().calculateSimilarityWCC(
		        positions1, intensities1, positions2, intensities2,
				width);
	}

    public String getDescription() {
        return "Calculates a score based on a very simple 13C NMR prediction, using "+
        "weighted cross correlation for similarity calculation. \n"+
        "Normally, NMRShiftDB judge will do much better";
    }

}
