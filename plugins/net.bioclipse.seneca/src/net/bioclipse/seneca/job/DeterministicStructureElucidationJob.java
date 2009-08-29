/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rob Schellhorn
 ******************************************************************************/

package net.bioclipse.seneca.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.seneca.anneal.TemperatureListener;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.MissingInformationException;
import net.bioclipse.seneca.judge.ScoreSummary;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.IStructureGenerationListener;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.xmlcml.cml.base.CMLElement;

public class DeterministicStructureElucidationJob 
		implements IStructureGenerationListener, ICASEJob {

	/**
	 * The selection to run this computation on, never <code>null</code>.
	 */
	private IStructuredSelection selection = StructuredSelection.EMPTY;

	StructureGeneratorResult sgr = null;

	private String molecularFormula;

	// private GENMDeterministicGenerator gdg;

	private long start;

	private long end;

	private List<IMolecule> hitList;

	private CDKHydrogenAdder hydrogenAdder;

	// private SimplePredictionTool predictor;

	private ChiefJustice chief = new ChiefJustice();

	private int structureCount;
	private String jobTitle;

	private IProgressMonitor monitor;
	private List<TemperatureAndScoreListener> temperatureListeners = new ArrayList<TemperatureAndScoreListener>();

    private boolean detectAromaticity;

  public void setDetectAromaticity(boolean detectAromaticity){
      this.detectAromaticity = detectAromaticity;
  }
  
	public DeterministicStructureElucidationJob(String jobTitle) {
		sgr = new StructureGeneratorResult(20);
		this.jobTitle = jobTitle;
	}

	public void configure(CMLElement input) throws MissingInformationException {
		// I only need the MF, so anything is fine. I might consider checking
		// the
		// number of peaks or so...
	}

	public void predict(List list) {
		ArrayList predSpect = new ArrayList();	// XXX : what is this for?
		for (int i = 0; i < list.size(); i++) {
			predSpect.clear();					// XXX : the only reference!
			IAtomContainer ac = (IAtomContainer) list.get(i);
			try {
				hydrogenAdder.addImplicitHydrogens(ac);
			} catch (CDKException e) {
				e.printStackTrace();
			}
			ScoreSummary scoreSum = chief.getScore((IMolecule) ac);
			double score = scoreSum.score; // scoreSum.maxScore;
			ac.setProperty("SIMILARITY", new Float(score));
		}
	}

	private Object[] sort(List list) {
		Object[] molarray = list.toArray();
		Arrays.sort(molarray, new SimCom());
		return molarray;
	}

	public void stateChanged(List list) {
		structureCount += list.size();
		monitor.worked(1);
		System.out.println(structureCount + "; ");
		start = System.currentTimeMillis();
		predict(list);
		end = System.currentTimeMillis();
		sort(list);
		for (int f = 0; f < list.size(); f++) {
			hitList.add((IMolecule) list.get(f));
		}
		Object[] os = sort(hitList);
		hitList.clear();
		for (int f = 0; f < 20; f++)
			hitList.add((Molecule) os[f]);
	}

	public class SimCom implements Comparator {
		public int compare(Object o1, Object o2) {
			Float sim1 = (Float) ((Molecule) o1).getProperty("SIMILARITY");
			Float sim2 = (Float) ((Molecule) o2).getProperty("SIMILARITY");
			if (sim1.floatValue() < sim2.floatValue())
				return 1;
			if (sim1.floatValue() > sim2.floatValue())
				return -1;
			return 0;
		}

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
			// gdg = new GENMDeterministicGenerator(molecularFormula,"");
			// gdg.addListener(this);
			// gdg.setStructuresAtATime(500);
			// gdg.setReturnedStructureCount(1000000000000l);

			// run structgen
			System.out.println("Starting Structure Generation");
			start = System.currentTimeMillis();
			// gdg.generate();
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

	public void setMolecularFormula(String formula) {
		this.molecularFormula = formula;
	}

	public List<IJudge> getJudges() {
		return chief.getJudges();
	}

	public void addJudge(IJudge judge) {
		chief.addJudge(judge);
	}

    public void addScoreImprovedListener( IScoreImprovedListener listener ) {

        // TODO Auto-generated method stub
        
    }
    public void addTemperatureAndScoreListener( TemperatureAndScoreListener listener ) {
        temperatureListeners.add(listener);
    }

    public void setJobSpecification( SenecaJobSpecification specification ) {

        // TODO Auto-generated method stub
        
    }
}