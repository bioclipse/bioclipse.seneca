/* HMBCJudge.java
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

import nu.xom.Document;
import nu.xom.Elements;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLSpectrum;

import spok.utils.SpectrumUtils;

/**
 * Gets the AllPairsShortestPath matrix for a given structure and checks if all
 * of the HMBC rules are fullfilled. HMBC rules are given as a 3D matrix of size
 * [n][n][n] where n is the number of atoms in the structure. A value != 0 at
 * [x][y][y] indicates that there was a HMBC crosspeak between the signals of
 * heavyatom x and y. In the third dimension alternatives to y are noted to
 * handle ambigous assignments.
 */

public class HMBCJudge extends TwoDSpectrumJudge {

	CMLCml cmlcml;
	static final String C_SHIFT="C_SHIFT";
	static final String H_SHIFT="H_SHIFT";
	static final String H_SHIFT_2="H_SHIFT_2";
	
	public HMBCJudge() {
		super("HMBCJudge");
		setScore(100, 0);
		setScore(100, 1);
		setScore(5, 2);
	}


	public IJudge createJudge(IPath data) throws MissingInformationException {
		this.setData( data );
        CMLBuilder builder = new CMLBuilder();
        try{
            Document doc =  builder.buildEnsureCML(ResourcesPlugin.getWorkspace().getRoot().getFile( data).getContents());
            SpectrumUtils.namespaceThemAll( doc.getRootElement().getChildElements() );
            doc.getRootElement().setNamespaceURI(CMLUtil.CML_NS);
            cmlcml = (CMLCml)builder.parseString(doc.toXML());
            couplings = new ArrayList<TwoDRule>();
    		//using the hmbc spectrum, we build couplings.
    		Elements spectra = cmlcml.getChildCMLElements("spectrum");
    		for(int i=0;i<spectra.size();i++){
    			CMLSpectrum spectrum = (CMLSpectrum)spectra.get(i);
    			if(spectrum.getType().equals("HMBC")){
    				for(int k=0;k<spectrum.getPeakListElements().get(0).getPeakElements().size();k++){
    					couplings.add(new TwoDRule(spectrum.getPeakListElements().get(0).getPeakElements().get(k).getXValue(), spectrum.getPeakListElements().get(0).getPeakElements().get(k).getYValue()));
    				}
    			}
    		}
            return this;
        }catch(Exception ex){
            throw new MissingInformationException("Could not read the cmlString.");
        }
	}

    public String getDescription() {
    	return "A simple 2d HMBC judge. Configuration is in a jsx file right now";
    }

    public boolean checkJudge( String data ) {

        // TODO implement a check
        return true;
    }

    public IFile setData( ISelection selection, IFile sjsFile ) {
    	//We do nothing to the data right now
        return sjsFile;
    }


	public boolean isLabelling() {
		return true;
	}


	public void labelStartStructure(IAtomContainer startStructure) {
		//using the bb+hsqc spectrum, we assign c and h labels.
		Elements spectra = cmlcml.getChildCMLElements("spectrum");
		for(int i=0;i<spectra.size();i++){
			CMLSpectrum spectrum = (CMLSpectrum)spectra.get(i);
			if(spectrum.getType().equals("NMR")){
				for(int k=0;k<spectrum.getPeakListElements().get(0).getPeakElements().size();k++){
					for(int l=0;l<startStructure.getAtomCount();l++){
						if(startStructure.getAtom(l).getProperty(C_SHIFT)==null && startStructure.getAtom(l).getHydrogenCount()==Integer.parseInt(spectrum.getPeakListElements().get(0).getPeakElements().get(k).getAttributeValue("multiplicity"))){
							startStructure.getAtom(l).setProperty(C_SHIFT,spectrum.getPeakListElements().get(0).getPeakElements().get(k).getXValue());
							break;
						}
					}
				}
			}
		}
		for(int i=0;i<spectra.size();i++){
			CMLSpectrum spectrum = (CMLSpectrum)spectra.get(i);
			if(spectrum.getType().equals("HSQC")){
				for(int k=0;k<spectrum.getPeakListElements().get(0).getPeakElements().size();k++){
					for(int l=0;l<startStructure.getAtomCount();l++){
						if((Double)startStructure.getAtom(l).getProperty(C_SHIFT)==spectrum.getPeakListElements().get(0).getPeakElements().get(k).getXValue()){
							if(startStructure.getAtom(l).getProperty(H_SHIFT)==null)
								startStructure.getAtom(l).setProperty(H_SHIFT,spectrum.getPeakListElements().get(0).getPeakElements().get(k).getYValue());
							else
								startStructure.getAtom(l).setProperty(H_SHIFT_2,spectrum.getPeakListElements().get(0).getPeakElements().get(k).getYValue());
							break;
						}
					}
				}
			}
		}
		/*for(int l=0;l<startStructure.getAtomCount();l++){
			System.err.println(startStructure.getAtom(l).getProperty(C_SHIFT)+" "+startStructure.getAtom(l).getProperty(H_SHIFT)+" "+startStructure.getAtom(l).getProperty(H_SHIFT_2));
		}*/
	}
}
