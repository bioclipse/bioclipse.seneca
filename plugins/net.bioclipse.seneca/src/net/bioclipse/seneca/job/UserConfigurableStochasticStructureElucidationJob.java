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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.StructureGeneratorSettingsPage;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.ScoreSummary;
import net.bioclipse.seneca.structgen.ConvergenceAnnealingEngine;
import net.bioclipse.seneca.structgen.IAnnealingEngine;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.structgen.RandomGenerator;
import org.openscience.cdk.structgen.SingleStructureRandomGenerator;
import org.openscience.cdk.tools.FormatStringBuffer;

/**
 * @author Egon Willighagen
 */
public class UserConfigurableStochasticStructureElucidationJob implements ICASEJob {

  private static final Logger logger
    = Logger.getLogger(UserConfigurableStochasticStructureElucidationJob.class);

  private ChiefJustice chiefJustice = new ChiefJustice();

  /**
   * The selection to run this computation on, never <code>null</code>.
   */
  private IStructuredSelection selection = StructuredSelection.EMPTY;

  StructureGeneratorResult sgr = null;

  private SenecaJobSpecification specification = null;
  IAtomContainer initialContainer = null;
  IAnnealingEngine annealingEngine;
  
  private List<IScoreImprovedListener> scoreImprovedListeners = new ArrayList<IScoreImprovedListener>();
  private List<TemperatureAndScoreListener> temperatureListeners = new ArrayList<TemperatureAndScoreListener>();

  private boolean detectAromaticity;

  public UserConfigurableStochasticStructureElucidationJob(IAtomContainer initialAtomContainer) {
    //super("Structure Elucidation");
    this.initialContainer = initialAtomContainer;
    System.out.println("Constructed SSE job...: " + this.hashCode());
    sgr = new StructureGeneratorResult(20);
    annealingEngine = new ConvergenceAnnealingEngine();
  }

  public void setDetectAromaticity(boolean detectAromaticity){
      this.detectAromaticity = detectAromaticity;
  }

  /*
   * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
   * IProgressMonitor)
   */
  public StructureGeneratorResult run(IProgressMonitor monitor) {
    long overallStartTime = System.currentTimeMillis();

    IProgressMonitor richMonitor = monitor;
    
    try {
      int stepsDone = 0;
      richMonitor.beginTask("Initializing", 10000);

      richMonitor.subTask("Setting up first structures...");
      SingleStructureRandomGenerator ssrg = new SingleStructureRandomGenerator();

      logger.info("Analyzing given MF: " + specification.getMolecularFormula());
      logger.debug("SAStochasticGenerator.execute()");
      logger.debug(initialContainer.getBondCount());

      ssrg.setAtomContainer(initialContainer);
      IMolecule mol = ssrg.generate();
      if(detectAromaticity)
          CDKHueckelAromaticityDetector.detectAromaticity( mol );
      StringWriter writer = new StringWriter();
      MDLV2000Writer mdlWriter = new MDLV2000Writer(writer);
      mdlWriter.write(mol);
      System.out.println(writer.toString());

      logger.debug("AtomCount: " + mol.getAtomCount());
      logger.debug("Starting structure generated");

      RandomGenerator randomGent = new RandomGenerator(mol);

      logger.debug("RandomGenerator initialized");

      if (richMonitor.isCanceled())
        return null;

      richMonitor.subTask("Initializing annealing engine...");

      annealingEngine.initAnnealing(randomGent, chiefJustice, monitor);
      annealingEngine.setMaxPlateauSteps(Long.parseLong(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "maxPlateauSteps")));
      annealingEngine.setConvergenceStopCount(annealingEngine.getMaxPlateauSteps()*10);
      annealingEngine.setMaxUphillSteps(Long.parseLong(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "maxUphillSteps")));
      annealingEngine.setConvergenceStopCount(Long.parseLong(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "convergenceStopCount")));
      annealingEngine.setInitializationCycles(Integer.parseInt(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "initializationCycles")));
      annealingEngine.setCoolingRate(Double.parseDouble(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "coolingRate")));
      annealingEngine.setAcceptanceProbability(Double.parseDouble(specification.getGeneratorSetting(StructureGeneratorSettingsPage.generatorNameUserConfigurable, "acceptanceProbability")));
      
      logger.debug("Annealing engine initialized");

      ScoreSummary recentScore = new ScoreSummary((long) 0, "nop", chiefJustice.calcMaxScore());
      ScoreSummary lastScore = new ScoreSummary((long) 0, "nop", chiefJustice.calcMaxScore());
      ScoreSummary bestScore = new ScoreSummary((long) 0, "nop", chiefJustice.calcMaxScore());

      if (richMonitor.isCanceled())
        return null;

      logger.debug("Starting CASE...");
      monitor.setTaskName("Computing");

      long startTime = System.currentTimeMillis();
      do {
        IMolecule result = randomGent.proposeStructure();
        if(detectAromaticity)
            CDKHueckelAromaticityDetector.detectAromaticity( result );

        try {
          recentScore = chiefJustice.getScore(result);

          logger.debug("Recent score: " + recentScore);

          if (annealingEngine.isAccepted(
              recentScore.score, lastScore.score)) {
            randomGent.acceptStructure();
            lastScore = recentScore;

            logger.debug("accepted, now lastscore = " + lastScore);

          } else {
            logger.debug("not accepted");
          }

          if (recentScore.score > bestScore.score) {
            bestScore = recentScore;

            logger.debug("New best score: "
                + (bestScore.score/chiefJustice.calcMaxScore()) + " after #" + stepsDone);

            logger.debug("Best score raised to: " + bestScore);

            annealingEngine.setConvergenceCount((long) 0);
          }

          if (recentScore.score >= bestScore.score) {
            logger.debug("Result AC: " + result);
            result.setProperty("Score", (recentScore.score/chiefJustice.calcMaxScore()));
            result.setProperty("Steps so far", stepsDone);
            result.setProperty("Temperature", annealingEngine.getTemperature());

            sgr.structures.push(result);
            for(int i=0;i<scoreImprovedListeners.size();i++){
                scoreImprovedListeners.get( i ).betterScore( result );
            }
          }
          logger.debug("Current best score: "+ (bestScore.score/chiefJustice.calcMaxScore()));

          annealingEngine.cool();

          logger.debug("new temperature: "
              + annealingEngine.getTemperature());

          if (stepsDone % 10 == 0) {
            richMonitor.subTask("Best score: "
                + (bestScore.score/chiefJustice.calcMaxScore())
                + ", T="
                + new FormatStringBuffer("%.3f")
                    .format(annealingEngine
                        .getTemperature()) + ", s="
                + (System.currentTimeMillis() - startTime)
                / 1000 + ", #" + stepsDone);
          }
        } catch (Exception exc) {
          exc.printStackTrace();
        }

        monitor.worked(1);
        stepsDone++;
        for(TemperatureAndScoreListener templistener : temperatureListeners){
            templistener.change( annealingEngine.getTemperature()/((ConvergenceAnnealingEngine)annealingEngine).getStart_kT(), bestScore.score/chiefJustice.calcMaxScore() );
        }
//        Thread.sleep(5);
      } while (!annealingEngine.isFinished() && !monitor.isCanceled());

      long totalTime = (System.currentTimeMillis() - overallStartTime)/1000;
      logger.debug("Best result achieved: " + bestScore);
      logger.debug("End of SAStochasticGenerator run");
      logger.debug("Time consumed: " + totalTime + " s");

      /*int counter = 1;
      while (sgr.structures.size() > 0) {
        IMolecule nextMolecule = (IMolecule)sgr.structures.pop();
        createChildResourceInSenecaFolder(
         counter, nextMolecule,
         jobTitle
        );

        counter++;
      }*/

      // BioclipseConsole.writeToConsole(selection.toString());

    } catch (Exception exception) {
      // logger.debug("An exception occured: " + exception.getMessage());
      exception.printStackTrace();
    } finally {
      richMonitor.done();
    }

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

    List<ISpectrum> spectra = new ArrayList<ISpectrum>();
    for (Object o : selection.toArray()) {

      if (o instanceof ISpectrum) {
        spectra.add((ISpectrum) o);
      }
    }

    return spectra.toArray(new ISpectrum[spectra.size()]);
  }

  public List<IJudge> getJudges() {
    return chiefJustice.getJudges();
  }

  public IAnnealingEngine getAnnealingEngine() {
    return annealingEngine;
  }

  public void addJudge(IJudge judge) {
    chiefJustice.addJudge(judge);
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
}