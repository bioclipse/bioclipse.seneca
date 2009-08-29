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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.BremserOneSphereHOSECodePredictor;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLElements;
import org.xmlcml.cml.element.CMLAtom;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLPeak;
import org.xmlcml.cml.element.CMLSpectrum;

/**
 * This Judge assigns a score to a structure depending on the the deviation of
 * the experimental 13C carbon spectrum from a backcalculated one. Currently the
 * backcalculation is very rudimentary (based on a one-sphere HOSE code
 * prediction), so that the role of this judge can only be to assure that the
 * carbon atom environment is in the correct range with respect to hybridization
 * state and hetero attachments
 */
public class WCCHOSECodeDEPTJudge extends Judge implements IJudge {

	public double score = 1000; // Score for optimum fit of exp. with calc.
								// shift
	protected transient HOSECodeGenerator hcg;
	protected transient BremserOneSphereHOSECodePredictor predictor;
	protected double[] zeroHydrogenCarbonShifts;
	protected double[] singleHydrogenCarbonShifts;
	protected double[] twoHydrogenCarbonShifts;
	protected double[] threeHydrogenCarbonShifts;

	public WCCHOSECodeDEPTJudge() {
		super("HOSECodeDEPTJudge (using the WCC)");
		hasMaxScore = true;
	}

	public IJudge createJudge(IPath data) throws MissingInformationException {
		IJudge judge = new WCCHOSECodeDEPTJudge();
		//TODO use data
		//judge.configure(data);
		judge.setEnabled(super.getEnabled());
		return judge;
	}

	public void setScore(int s) {
		// XXX?
	}

	public void init() {
		hcg = new HOSECodeGenerator();
		predictor = new BremserOneSphereHOSECodePredictor();
		// predictor = new PredictionTool();
	}

	public void calcMaxScore() {
		maxScore = 1000;
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
		for (int hCount = 0; hCount <= 3; hCount++) {
			List<Double> shiftList = new ArrayList<Double>();
			carbonCount = 0;
			for (int f = 0; f < ac.getAtomCount(); f++) {
				if (ac.getAtom(f).getSymbol().equals("C")) {
					if (getHydrogenCount(ac, ac.getAtom(f)) == hCount) {
						try {
							hoseCode = hcg.makeBremserCompliant(
									hcg.getHOSECode(ac, ac.getAtom(f), 1));
							shiftList.add(predictor.predict(hoseCode));
							carbonCount++;
						} catch (Exception exc) {
							exc.printStackTrace();
							throw exc;
						}
					}
				}
			}
			double[] shifts = new double[shiftList.size()];
			for (int i = 0; i < shifts.length; i++) {
				shifts[i] = shiftList.get(i);
			}
			if (hCount == 0) {
				scoreSum += (int) (shiftwcc(this.zeroHydrogenCarbonShifts,
						shifts, 20.0)
						* maxScore * 0.25);
			} else if (hCount == 1) {
				scoreSum += (int) (shiftwcc(this.singleHydrogenCarbonShifts,
						shifts, 20.0)
						* maxScore * 0.25);
			} else if (hCount == 2) {
				scoreSum += (int) (shiftwcc(this.twoHydrogenCarbonShifts,
						shifts, 20.0)
						* maxScore * 0.25);
			} else if (hCount == 3) {
				scoreSum += (int) (shiftwcc(this.threeHydrogenCarbonShifts,
						shifts, 20.0)
						* maxScore * 0.25);
			}
		}
		String message = "Score: " + scoreSum + "/" + maxScore;
		return new JudgeResult(maxScore, scoreSum, 0, message);
	}

	private int getHydrogenCount(IAtomContainer ac, IAtom atom) {
		int hCount = atom.getHydrogenCount();
		Iterator<IAtom> atoms = ac.getConnectedAtomsList(atom).iterator();
		while (atoms.hasNext()) {
			if (atoms.next().getSymbol().equals("H")) {
				hCount++;
			}
		}
		return hCount;
	}

	public boolean[][][] getAssignment() {
		return null;
	}

	public double shiftwcc(double[] positions1, double[] positions2,
			double width) {

		if (positions1.length == 0 || positions2.length == 0) {
			return 0.0;
		}

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

	public void configure(CMLElement input) throws MissingInformationException {
		if (!(input instanceof CMLCml)) {
			throw new MissingInformationException("Root element must be <cml>!");
		}
		CMLCml root = (CMLCml) input;

		String CML_NAMESPACE = "http://www.xml-cml.org/schema";
		XPathContext context = new XPathContext("cml", CML_NAMESPACE);
		Nodes result = root
				.query(
						"./cml:spectrum[./cml:metadataList/cml:metadata/@content=\"13C\"]",
						context);
		if (result.size() == 0) {
			throw new MissingInformationException(
					"No 13C NMR spectrum is defined!");
		}

		CMLSpectrum cmlSpect = (CMLSpectrum) result.get(0);
		if (cmlSpect.getPeakListElements() == null) {
			throw new MissingInformationException("No peaks are defined!");
		}
		CMLElements<CMLPeak> peaks = cmlSpect.getPeakListElements().get(0)
				.getPeakElements();

		// the number indicates the number the number of hydrogens
		// associated with the peak
		List<Double> shiftList0 = new ArrayList<Double>();
		List<Double> shiftList1 = new ArrayList<Double>();
		List<Double> shiftList2 = new ArrayList<Double>();
		List<Double> shiftList3 = new ArrayList<Double>();

		for (int peakNo = 0; peakNo < peaks.size(); peakNo++) {
			CMLPeak peak = peaks.get(peakNo);
			String[] atomRefs = peak.getAtomRefs();
			if (atomRefs.length != 1) {
				throw new MissingInformationException(
						"Expecting one and only one hydrogen count association per peak!");
			}
			Nodes peakDetails = root.query("//cml:atom[./@id='" + atomRefs[0]
					+ "']", context);
			if (peakDetails.size() != 1) {
				throw new MissingInformationException(
						"Hydrogen count not specified for the atom associated with the peak!");
			}
			CMLAtom atomSpec = (CMLAtom) peakDetails.get(0);
			if (atomSpec.getHydrogenCount() == 0) {
				shiftList0.add(peak.getXValue());
			} else if (atomSpec.getHydrogenCount() == 1) {
				shiftList1.add(peak.getXValue());
			} else if (atomSpec.getHydrogenCount() == 2) {
				shiftList2.add(peak.getXValue());
			} else if (atomSpec.getHydrogenCount() == 3) {
				shiftList3.add(peak.getXValue());
			} else {
				throw new MissingInformationException(
						"Unexpected high hydrogen count on carbon shift: "
								+ atomSpec.getHydrogenCount());
			}
		}

		// OK, now copy the info into the arrays
		zeroHydrogenCarbonShifts = new double[shiftList0.size()];
		for (int i = 0; i < zeroHydrogenCarbonShifts.length; i++) {
			zeroHydrogenCarbonShifts[i] = shiftList0.get(i);
		}
		singleHydrogenCarbonShifts = new double[shiftList1.size()];
		for (int i = 0; i < singleHydrogenCarbonShifts.length; i++) {
			singleHydrogenCarbonShifts[i] = shiftList1.get(i);
		}
		twoHydrogenCarbonShifts = new double[shiftList2.size()];
		for (int i = 0; i < twoHydrogenCarbonShifts.length; i++) {
			twoHydrogenCarbonShifts[i] = shiftList2.get(i);
		}
		threeHydrogenCarbonShifts = new double[shiftList3.size()];
		for (int i = 0; i < threeHydrogenCarbonShifts.length; i++) {
			threeHydrogenCarbonShifts[i] = shiftList3.get(i);
		}

		System.out.println("SHIFTS 0: " + shiftList0);
		System.out.println("SHIFTS 1: " + shiftList1);
		System.out.println("SHIFTS 2: " + shiftList2);
		System.out.println("SHIFTS 3: " + shiftList3);

	}

    public String getDescription() {

        // TODO Auto-generated method stub
        return null;
    }

    public boolean checkJudge( String data ) {

        // TODO Auto-generated method stub
        return false;
    }

    public IFile setData( ISelection selection, IFile sjsFile ) {

        // TODO Auto-generated method stub
        return null;
    }

}
