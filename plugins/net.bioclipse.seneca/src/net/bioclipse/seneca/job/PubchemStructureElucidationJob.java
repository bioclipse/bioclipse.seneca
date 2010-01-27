/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 ******************************************************************************/

package net.bioclipse.seneca.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.MissingInformationException;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.pubchem.PubchemStructureGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.xmlcml.cml.base.CMLElement;

public class PubchemStructureElucidationJob 
		implements ICASEJob {


	/**
	 * The selection to run this computation on, never <code>null</code>.
	 */
	private IStructuredSelection selection = StructuredSelection.EMPTY;

	StructureGeneratorResult sgr = null;

	private SenecaJobSpecification            specification          = null;
	
	private long start;

	private long end;

	private List<IMolecule> hitList;

	private ChiefJustice chief = new ChiefJustice();

	private int structureCount;
	private String jobTitle;

	private IProgressMonitor monitor;
	private List<TemperatureAndScoreListener> temperatureListeners = new ArrayList<TemperatureAndScoreListener>();

    private boolean detectAromaticity;
    private List<IScoreImprovedListener>      scoreImprovedListeners = new ArrayList<IScoreImprovedListener>();
    private static final Logger logger = Logger.getLogger(PubchemStructureElucidationJob.class);
    private double bestScoreSoFar=0;

  public void setDetectAromaticity(boolean detectAromaticity){
      this.detectAromaticity = detectAromaticity;
  }
  
	public PubchemStructureElucidationJob(String jobTitle) {
		this.jobTitle = jobTitle;
		sgr = new StructureGeneratorResult(20);
	}

	public void configure(CMLElement input) throws MissingInformationException {
		// I only need the MF, so anything is fine. I might consider checking
		// the
		// number of peaks or so...
	}

	public boolean stateChanged(IMolecule molecule) {
		structureCount += 1;
		monitor.worked(PubchemStructureGenerator.worked+structureCount);
		System.out.print(structureCount + "; ");
		molecule.setProperty("Score", chief.getScore(molecule).score);
		molecule.setProperty( "Steps so far", structureCount );
		molecule.setProperty( "Temperature", "n/a" );
		if((Double)molecule.getProperty("Score")>=bestScoreSoFar){
			bestScoreSoFar=(Double)molecule.getProperty("Score");
			sgr.structures.push( molecule );
	        this.monitor.subTask( "Best score: "
	                + molecule.getProperty("Score")
	                + ", s="
	                + (System.currentTimeMillis() - start)
	                / 1000 + ", #" + structureCount );
	        for ( int i = 0; i < scoreImprovedListeners.size(); i++ ) {
	            scoreImprovedListeners.get( i ).betterScore( molecule );
	        }					
	        for ( int i = 0; i < temperatureListeners.size(); i++ ) {
	            temperatureListeners.get( i ).change(0, (Double)molecule.getProperty("Score") );
	        }	
		}
        this.monitor.worked(PubchemStructureGenerator.worked+structureCount );
        if ( monitor.isCanceled() )
        	return true;
        else
        	return false;
	}

	/*
	 * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
	 * IProgressMonitor)
	 */
	public StructureGeneratorResult run(IProgressMonitor monitor) {
		if (monitor == null) {
			this.monitor = new NullProgressMonitor();
		} else {
			this.monitor = monitor;
		}

		try {
			monitor.beginTask("Computing", 50);
			hitList = new ArrayList<IMolecule>();
			// run structgen
			System.out.println("Starting Structure Generation");
			start = System.currentTimeMillis();
			try{
				List<IMolecule> result=PubchemStructureGenerator.doDownload(specification.getMolecularFormula(),monitor);
				end = System.currentTimeMillis();
				for(int i=0;i<result.size();i++){
					//we need to check for H counts since pubchem search ignores these
					IMolecule mol = result.get(i);
            		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                    CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance()).addImplicitHydrogens(mol);
					if(checkForHCountValidity(specification, mol)){
						if(stateChanged(result.get(i)))
							break;
						System.err.println("result");
					}
				}
			}catch(IOException ex){
				LogUtils.handleException(ex, logger, Activator.PLUGIN_ID);
			}

		} catch (Exception exception) {
			System.out.println("An exception occured: "
					+ exception.getMessage());
			exception.printStackTrace();
		}

		monitor.done();
		return sgr;
	}

	/**
	 * Tells if a molecule has the hydrogen counts on its carbons as in a specification.
	 * 
	 * @param specification2 The specification to check against.
	 * @param mol            The molecule to check.
	 * @return               True=molecule is valid, false=molecule is invalid.
	 */
	public static boolean checkForHCountValidity(
			SenecaJobSpecification specification2, IAtomContainer mol) {
		int[] hcounts = new int[4];
		for(IAtom atom : mol.atoms()){
			if(atom.getSymbol().equals("C")){
				int hcount=atom.getHydrogenCount();
				Iterator<IAtom> it = mol.getConnectedAtomsList(atom).iterator();
				while(it.hasNext()){
					if(it.next().getSymbol().equals("H")){
						hcount++;
					}
				}
				hcounts[hcount]++;
			}
		}
		for(int i=0;i<4;i++){
			if(hcounts[i]!=specification2.getDeptData(i))
				return false;
		}
		return true;
	}

	/**
	 * Sets the selection of this job.
	 *
	 * @param selection
	 *            The new selection for this job.
	 * @throws IllegalArgumentException
	 *             If the given selection is <code>null</code>.
	 */
	public void setSelection(IStructuredSelection selection) {
		if (selection == null) {
			throw new IllegalArgumentException();
		}
		this.selection = selection;
	}

	public ISpectrum[] getSpectrumResources() {
		List<ISpectrum> resources = new ArrayList<ISpectrum>();
		for (Object o : selection.toArray()) {

			if (o instanceof ISpectrum) {
				resources.add((ISpectrum) o);
			}
		}
		return resources.toArray(new ISpectrum[resources.size()]);
	}

	public List<IJudge> getJudges() {
		return chief.getJudges();
	}

	public void addJudge(IJudge judge) {
		chief.addJudge(judge);
	}

    public void addScoreImprovedListener( IScoreImprovedListener listener ) {

        scoreImprovedListeners.add(listener);
        
    }
    public void addTemperatureAndScoreListener( TemperatureAndScoreListener listener ) {
        temperatureListeners.add(listener);
    }

    public void setJobSpecification( SenecaJobSpecification specification ) {
    	this.specification = specification;
    }

	public class ScoreComparator implements Comparator<IMolecule> {

		public int compare(IMolecule o1, IMolecule o2) {
			if((Double)o1.getProperty("Score")>(Double)o2.getProperty("Score"))
				return -1;
			else if((Double)o1.getProperty("Score")<(Double)o2.getProperty("Score"))
				return 1;
			else
				return 0;
		}

	}
}