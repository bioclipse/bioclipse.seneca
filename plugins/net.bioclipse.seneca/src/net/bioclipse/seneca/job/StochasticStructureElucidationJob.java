/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: Rob Schellhorn
 ******************************************************************************/

package net.bioclipse.seneca.job;

import java.util.ArrayList;
import java.util.List;

import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.anneal.AdaptiveAnnealingEngine;
import net.bioclipse.seneca.anneal.AnnealerAdapterI;
import net.bioclipse.seneca.anneal.AnnealingEngineI;
import net.bioclipse.seneca.anneal.MoleculeAnnealerAdapter;
import net.bioclipse.seneca.anneal.MoleculeState;
import net.bioclipse.seneca.anneal.State;
import net.bioclipse.seneca.anneal.StateListener;
import net.bioclipse.seneca.anneal.TemperatureListener;
import net.bioclipse.seneca.anneal.MoleculeState.Acceptance;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.SingleStructureRandomGenerator;
import org.openscience.cdk.tools.FormatStringBuffer;

/**
 * @author Egon Willighagen
 */
public class StochasticStructureElucidationJob implements StateListener,
        TemperatureListener, ICASEJob {

    private static final Logger               logger                 =
                                                                             Logger
                                                                                     .getLogger( StochasticStructureElucidationJob.class );

    private ChiefJustice                      chiefJustice           =
                                                                             new ChiefJustice();

    private StructureGeneratorResult          sgr                    = null;

    private SenecaJobSpecification            specification          = null;

    private IAtomContainer                    initialContainer       = null;

    private AnnealingEngineI                  annealingEngine;

    private MoleculeAnnealerAdapter           aa;

    private MonitorWrapper                    monitor;

    private long                              startTime;

    private double                            temperature            = 0;

    private int                               numberOfSteps;

    private List<IScoreImprovedListener>      scoreImprovedListeners =
                                                                             new ArrayList<IScoreImprovedListener>();

    private List<TemperatureAndScoreListener> temperatureListeners   =
                                                                             new ArrayList<TemperatureAndScoreListener>();

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

    public StochasticStructureElucidationJob(
            IAtomContainer initialAtomContainer, int numberOfSteps) {

        this.initialContainer = initialAtomContainer;
        System.out.println( "Constructed SSE job...: " + this.hashCode() );
        sgr = new StructureGeneratorResult( 20 );
        this.numberOfSteps = numberOfSteps;
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
            monitor.beginTask( "Initializing", numberOfSteps );

            monitor.subTask( "Setting up first structures..." );
            SingleStructureRandomGenerator ssrg =
                    new SingleStructureRandomGenerator();

            logger.info( "Analyzing given MF: "
                         + specification.getMolecularFormula() );
            logger.debug( "SAStochasticGenerator.execute()" );
            logger.debug( initialContainer.getBondCount() );

            ssrg.setAtomContainer( initialContainer );
            IMolecule mol = ssrg.generate();

            logger.debug( "AtomCount: " + mol.getAtomCount() );
            logger.debug( "Starting structure generated" );

            logger.debug( "RandomGenerator initialized" );

            if ( monitor.isCanceled() )
                return null;

            monitor.subTask( "Initializing annealing engine..." );

            chiefJustice.initJudges();

            aa =
                    new MoleculeAnnealerAdapter( mol, chiefJustice,
                                                 detectAromaticity );
            aa.addStateListener( this );
            startTime = System.currentTimeMillis();

            annealingEngine = new AdaptiveAnnealingEngine( aa, numberOfSteps );
            annealingEngine.addTemperatureListener( this );
            annealingEngine.run();

        } catch ( Exception exception ) {
            exception.printStackTrace();
            LogUtils.handleException( exception, logger, Activator.PLUGIN_ID );
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

    public void stateChanged( State state ) {

        MoleculeState moleculeState = (MoleculeState) state;
        if ( moleculeState.acceptance == Acceptance.ACCEPT ) {
            org.openscience.cdk.interfaces.IMolecule best = aa.getBest();
            best.setProperty( "Score", 1 - aa.getBestCost() );
            best.setProperty( "Steps so far", aa.getBestStepIndex() );
            best.setProperty( "Temperature", this.temperature );
            sgr.structures.push( best );
            for ( int i = 0; i < scoreImprovedListeners.size(); i++ ) {
                scoreImprovedListeners.get( i ).betterScore( best );
            }
        } else if ( moleculeState.acceptance == Acceptance.UNKNOWN ) {
            this.monitor.subTask( "Best score: "
                                  + (1 - aa.getBestCost())
                                  + ", T="
                                  + new FormatStringBuffer( "%.3f" )
                                          .format( this.temperature ) + ", s="
                                  + (System.currentTimeMillis() - startTime)
                                  / 1000 + ", #" + state.getStep() );
            this.monitor.worked( state.getStep() );
        }
        if ( monitor.isCanceled() )
            aa.setCancelled( true );

    }

    public void temperatureChange( double temp ) {

        this.temperature = temp;
        for ( TemperatureAndScoreListener templistener : temperatureListeners ) {
            templistener.change( temp, 1 - aa.getBestCost() );
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