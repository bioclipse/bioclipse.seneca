/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 ******************************************************************************/

package net.bioclipse.seneca.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.MissingInformationException;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.IStructureGenerationListener;
import org.openscience.cdk.structgen.deterministic.GENMDeterministicGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.xmlcml.cml.base.CMLElement;

public class DeterministicStructureElucidationJob 
		implements IStructureGenerationListener, ICASEJob {


	/**
	 * The selection to run this computation on, never <code>null</code>.
	 */
	private IStructuredSelection selection = StructuredSelection.EMPTY;

	StructureGeneratorResult sgr = null;

	private SenecaJobSpecification            specification          = null;
	
	private GENMDeterministicGenerator gdg;

	private long start;

	private long end;

	private List<IMolecule> hitList;

	private CDKHydrogenAdder hydrogenAdder;

	
	private ChiefJustice chief = new ChiefJustice();

	private int structureCount;
	private String jobTitle;

	private IProgressMonitor monitor;
	private List<TemperatureAndScoreListener> temperatureListeners = new ArrayList<TemperatureAndScoreListener>();

    private boolean detectAromaticity;
    private List<IScoreImprovedListener>      scoreImprovedListeners = new ArrayList<IScoreImprovedListener>();


  public void setDetectAromaticity(boolean detectAromaticity){
      this.detectAromaticity = detectAromaticity;
  }
  
	public DeterministicStructureElucidationJob(String jobTitle) {
		this.jobTitle = jobTitle;
		sgr = new StructureGeneratorResult(20);
	}

	public void configure(CMLElement input) throws MissingInformationException {
		// I only need the MF, so anything is fine. I might consider checking
		// the
		// number of peaks or so...
	}

	public void stateChanged(List<IMolecule> list) {
		structureCount += list.size();
		monitor.worked(1);
		System.out.println(structureCount + "; ");
		for (int f = 0; f < list.size(); f++) {
			list.get(f).setProperty("Score", chief.getScore(list.get(f)).score);
		}
		Collections.sort(list, new ScoreComparator());
		org.openscience.cdk.interfaces.IMolecule current=list.get(0);
		chief.getScore(current);
		current.setProperty( "Score", (chief.calcMaxScore() - (Double)list.get(0).getProperty("Score"))/chief.calcMaxScore() );
		current.setProperty( "Steps so far", structureCount );
		current.setProperty( "Temperature", "n/a" );
		sgr.structures.push( current );
        this.monitor.subTask( "Best score: "
                + current.getProperty("Score")
                + ", s="
                + (System.currentTimeMillis() - start)
                / 1000 + ", #" + structureCount );
        for ( int i = 0; i < scoreImprovedListeners.size(); i++ ) {
            scoreImprovedListeners.get( i ).betterScore( current );
        }					
        for ( int i = 0; i < temperatureListeners.size(); i++ ) {
            temperatureListeners.get( i ).change(0, (Double)current.getProperty("Score") );
        }					
        this.monitor.worked( structureCount );
        //TODO if ( monitor.isCanceled() )
        //	gdg.setCancelled( true );
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

			hydrogenAdder = CDKHydrogenAdder.getInstance(
					DefaultChemObjectBuilder.getInstance());

			// TODO : either remove this class, or create a new GENMDG
			gdg = new GENMDeterministicGenerator(specification.getMolecularFormula(),"/tmp/");
			gdg.addListener(this);
			gdg.setStructuresAtATime(500);
			gdg.setReturnedStructureCount(1000000000000l);

			// run structgen
			System.out.println("Starting Structure Generation");
			start = System.currentTimeMillis();
			gdg.generate();
			end = System.currentTimeMillis();
			// BioclipseConsole.writeToConsole("Computing time: " + (end -
			// start) + " ms");
			// int isomerCount = gdg.getNumberOfStructures();
			// BioclipseConsole.writeToConsole("Generated structures: " +
			// isomerCount);

//			int counter = 1;
			for (int i = 0; i < Math.min(hitList.size(), 10); i++) {
				// createChildResourceInSenecaFolder(
				// counter, (IMolecule)hitList.get(i),
				// jobTitle
				// );
				// counter++;
			}

			// BioclipseConsole.writeToConsole(selection.toString());

		} catch (Exception exception) {
			System.out.println("An exception occured: "
					+ exception.getMessage());
			exception.printStackTrace();
		}

		monitor.done();
		return sgr;
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