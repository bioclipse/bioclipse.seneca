/*******************************************************************************
 * Copyright (c) 2009  Stefan Kuhn <Stefan Kuhn@ebi.ac.uk>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org—epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.seneca.job;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.anneal.AnnealerAdapterI;
import net.bioclipse.seneca.anneal.AnnealingEngineI;
import net.bioclipse.seneca.anneal.MoleculeState;
import net.bioclipse.seneca.anneal.State;
import net.bioclipse.seneca.anneal.StateListener;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.StructureGeneratorSettingsPage;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.ga.MoleculeCandidateFactory;
import net.bioclipse.seneca.ga.MoleculeCrossover;
import net.bioclipse.seneca.ga.MoleculeEvolutionObserver;
import net.bioclipse.seneca.ga.MoleculeFitnessEvaluator;
import net.bioclipse.seneca.ga.MoleculeMutation;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLWriter;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.ConcurrentEvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.UserAbort;

/**
 * @author Stefan Kuhn
 */
public class GAStructureElucidationJob implements StateListener, ICASEJob {

	private SenecaJobSpecification            specification = null;
	
    private static final Logger               logger                 =
         Logger.getLogger( GAStructureElucidationJob.class );

    private ChiefJustice                      chiefJustice           =
         new ChiefJustice();

    private StructureGeneratorResult          sgr                    = null;

    private IAtomContainer                    initialContainer       = null;

    private AnnealingEngineI                  annealingEngine;

    private MonitorWrapper                    monitor;

    private long                              startTime;

    private List<TemperatureAndScoreListener> temperatureListeners   =
         new ArrayList<TemperatureAndScoreListener>();

    private List<IScoreImprovedListener>      scoreImprovedListeners =
                                                                             new ArrayList<IScoreImprovedListener>();
    private UserAbort                         userAbort              =
                                                                             new UserAbort();
    private boolean                           detectAromaticity;

    private class MonitorWrapper extends ProgressMonitorWrapper {

        private AnnealerAdapterI aa;

        public MonitorWrapper(IProgressMonitor monitor) {

            super( monitor );
        }

        // this is the key method that justifies this class
        public void setCanceled( boolean value ) {

            aa.setCancelled( value );
            super.setCanceled( value );
        }
    }

    public GAStructureElucidationJob(String jobTitle,
            IAtomContainer initialAtomContainer) {

        this.initialContainer = initialAtomContainer;
        System.out.println( "Constructed SSE job...: " + this.hashCode() );
        sgr = new StructureGeneratorResult( 20 );
    }

    public void setDetectAromaticity( boolean detectAromaticity ) {

        this.detectAromaticity = detectAromaticity;
    }

    /*
     * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    public StructureGeneratorResult run( IProgressMonitor monitor ) {

        this.monitor = new MonitorWrapper( monitor );

        try {

        	monitor.beginTask( "Initializing", 1000 );
            CandidateFactory<IMolecule> factory =
                    new MoleculeCandidateFactory( initialContainer,
                                                  detectAromaticity );

            // Create a pipeline that applies cross-over then mutation.
            List<EvolutionaryOperator<IMolecule>> operators =
                    new LinkedList<EvolutionaryOperator<IMolecule>>();
            operators.add( new MoleculeMutation( detectAromaticity ) );
            operators.add( new MoleculeCrossover( detectAromaticity ) );
            EvolutionaryOperator<IMolecule> pipeline =
                    new EvolutionPipeline<IMolecule>( operators );

            FitnessEvaluator<IMolecule> fitnessEvaluator =
                    new MoleculeFitnessEvaluator( chiefJustice );
            SelectionStrategy<Object> selection = new RouletteWheelSelection();
            Random rng = new MersenneTwisterRNG();

            EvolutionEngine<IMolecule> engine =
                    new ConcurrentEvolutionEngine<IMolecule>( factory,
                                                              pipeline,
                                                              fitnessEvaluator,
                                                              selection, rng );
            engine.addEvolutionObserver( new MoleculeEvolutionObserver( this ) );
            TerminationCondition[] terminations = new TerminationCondition[2];
            terminations[0] = new Stagnation( 20, true );
            terminations[1] = userAbort;
            startTime = System.currentTimeMillis();
            IMolecule result = null;
            if(specification.getGeneratorSetting(StructureGeneratorSettingsPage.gaGeneratorName, "initialfile")!=null){
            	List<ICDKMolecule> mols = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager().loadMolecules(specification.getGeneratorSetting(StructureGeneratorSettingsPage.gaGeneratorName, "initialfile"));
            	List<IMolecule> seed = new ArrayList<IMolecule>();
            	int wrongseeds=0;
            	for(int i=0;i<mols.size();i++){
                	try{
	            		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mols.get(i).getAtomContainer());
	                    CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance()).addImplicitHydrogens(mols.get(i).getAtomContainer());
	            		if(PubchemStructureElucidationJob.checkForHCountValidity(specification, mols.get(i).getAtomContainer()))
	            			seed.add(DefaultChemObjectBuilder.getInstance().newMolecule(mols.get(i).getAtomContainer()));
	            		else
	            			wrongseeds++;
                	}catch(Exception ex){
                		ex.printStackTrace();
                		wrongseeds++;
                	}
            	}
            	if(wrongseeds>0){
            		System.out.println(wrongseeds+" of your "+mols.size()+" initial structures had a wrong hcount. We will not use them!");
            	}
            	if(seed.size()>0)
            		System.err.println("Seed "+chiefJustice.getScore(seed.get(0)));
            	result = engine.evolve( 10, 1, seed, terminations );
            }else{
            	result = engine.evolve( 10, 1, terminations );
            }
        } catch ( Exception exception ) {
            exception.printStackTrace();
            LogUtils.handleException( exception, logger,
                                      net.bioclipse.seneca.Activator.PLUGIN_ID );
        } finally {
            monitor.done();
        }

        return sgr;
    }

    public List<IJudge> getJudges() {

        return chiefJustice.getJudges();
    }

    public AnnealingEngineI getAnnealingEngine() {

        return annealingEngine;
    }

    public void addJudge( IJudge judge ) {

        chiefJustice.addJudge( judge );
    }

    double score = 0;

    public void stateChanged( State state ) {

        org.openscience.cdk.interfaces.IMolecule best =
                ((MoleculeState) state).molecule;
        best.setProperty( "Score", ((MoleculeState) state).score/chiefJustice.calcMaxScore() );
        best.setProperty( "Steps so far", ((MoleculeState) state).getStep() );
        sgr.structures.push( best );
        for ( int i = 0; i < scoreImprovedListeners.size(); i++ ) {
            scoreImprovedListeners.get( i ).betterScore( best );
        }
        score = ((MoleculeState) state).score/chiefJustice.calcMaxScore();
        if ( monitor.isCanceled() )
            userAbort.abort();
        this.monitor.subTask( "Best score: " + ((MoleculeState) state).score/chiefJustice.calcMaxScore()
                              + ", s="
                              + (System.currentTimeMillis() - startTime) / 1000
                              + ", #" + state.getStep() );
        this.monitor.worked( state.getStep() );

    }

    public void temperatureChange( double temp ) {

    	if(score!=0 || temp!=0){
	        for ( TemperatureAndScoreListener templistener : temperatureListeners ) {
	            templistener.change( temp, score );
	        }
    	}
    }

    public void addScoreImprovedListener( IScoreImprovedListener listener ) {

        scoreImprovedListeners.add( listener );

    }

    public void addTemperatureAndScoreListener(
                                                TemperatureAndScoreListener listener ) {

        temperatureListeners.add( listener );
    }

    public void setJobSpecification( SenecaJobSpecification specification ) {
        this.specification = specification;
    }

}