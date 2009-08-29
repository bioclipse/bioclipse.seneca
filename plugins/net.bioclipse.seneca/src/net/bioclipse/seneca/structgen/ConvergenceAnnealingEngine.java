/*
 *  $RCSfile: ConvergenceAnnealingEngine.java,v $
 *  $Author: steinbeck $
 *  $Date: 2004/02/16 09:50:54 $
 *  $Revision: 1.10 $
 *
 *  Copyright (C) 1997 - 2001  Christoph Steinbeck
 *
 *  Contact: c.steinbeck@uni-koeln.de
 *
 *  This software is published and distributed under artistic license.
 *  The intent of this license is to state the conditions under which this Package
 *  may be copied, such that the Copyright Holder maintains some semblance
 *  of artistic control over the development of the package, while giving the
 *  users of the package the right to use and distribute the Package in a
 *  more-or-less customary fashion, plus the right to make reasonable modifications.
 *
 *  THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES,
 *  INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  The complete text of the license can be found in a file called LICENSE
 *  accompanying this package.
 */
package net.bioclipse.seneca.structgen;

import net.bioclipse.seneca.judge.ChiefJustice;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.RandomGenerator;

/**
 * The AnnealingEngine controls the course of the tempererature during a
 * Simulated Annealing run according to the data stored in a given
 * AnnealingSchedule configuration object.
 *
 *@author steinbeck
 *@created July 7, 2001
 */
public class ConvergenceAnnealingEngine implements IAnnealingEngine {

	/**
	 *  The maximum number of steps for each Plateau (Markov Chain)
	 */
	public final static long MAXPLATEAUSTEPS = 1500;

	/**
	 *  Number of uphill moves to be made before leaving this Plateau
	 */
	public final static long MAXUPHILLSTEPS = 150;

	/**
	 *  Value for the convergenceCounter to be reached as a stop criterion
	 */
	public final static long CONVERGENCESTOPCOUNT = 4500;

	/**
	 *  The cooling rate. Factor with which to multiply the current temperature in
	 *  order to calculate the new one.
	 */
	public final static double COOLINGRATE = 0.95;

	/**
	 *  The initial acceptance probability used for initializing the schedule
	 */
	public final static double INITIALACCEPTANCEPROBABILITY = 0.8;

	/**
	 *  The number of cycles used in initialization
	 */
	public final static int INITIALIZATIONCYCLES = 200;

	/**
	 * The maximum number of steps for each Plateau (Markov Chain)
	 */
	long maxPlateauSteps = MAXPLATEAUSTEPS;

	/**
	 * The number of steps for each Plateau (Markov Chain)
	 */
	long plateauStepCounter = 0;

	/**
	 * Number of uphill made since last reset of this counter
	 */
	long uphill = 0;

	/**
	 * Number of uphill moves to be made before leaving this Plateau
	 */
	long maxUphillSteps = MAXUPHILLSTEPS;

	/**
	 * The best score so far. Used for resetting the convergenceCounter
	 */
	long bestScore = 0;

	/**
	 * Counter that counts the unsuccessful steps (steps without a change in the
	 * score)
	 */
	long convergenceCounter = 0;

	/**
	 * Value for the convergenceCounter to be reached as a stop criterion
	 */
	long convergenceStopCount = CONVERGENCESTOPCOUNT;

	/**
	 * The starting temperature (multiplied with k for convenience)
	 */
	double start_kT = 0;

	/**
	 * The current temperature (multiplied with k for convenience)
	 */
	double current_kT = 0;

	/**
	 * The cooling rate. Factor with which to multiply the current temperature
	 * in order to calculate the new one.
	 */
	double coolingRate = COOLINGRATE;

	/**
	 * The counter for the annealing steps (surprise!)
	 */
	long annealingStepCounter;

	/**
	 * False as long as the calculation is not converged
	 */
	boolean isConverged = false;

	/**
	 * The initial acceptance probability used for initializing the schedule
	 */
	double initialAcceptanceProbability = INITIALACCEPTANCEPROBABILITY;

	/**
	 * A central scoring facility for the initialization
	 */
	ChiefJustice chiefJustice = null;

	/**
	 * A source of structures for the initialization
	 */
	RandomGenerator randomGent = null;

	/**
	 * The number of cycles used in initialization
	 */
	int initCycles = INITIALIZATIONCYCLES;

	/**
	 * The number of total iterations done so far
	 */
	long iterations = 0;

	long reportsteps = 1000;

	boolean debug = false;
	boolean report = true;

	// private static final Logger logger =
	// Bc_senecaPlugin.getLogManager().getLogger
	// (ConvergenceAnnealingEngine.class.toString());

	/**
	 * Constructs a new AnnealingEngine object
	 */
	public ConvergenceAnnealingEngine() {
	}

	/**
	 * Sets the InitialAcceptanceProbability attribute of the
	 * ConvergenceAnnealingEngine object
	 *
	 *@param initialAcceptanceProbability
	 *            The new InitialAcceptanceProbability value
	 */
	public void setInitialAcceptanceProbability(
			double initialAcceptanceProbability) {
		this.initialAcceptanceProbability = initialAcceptanceProbability;
	}

	/**
	 * Sets the RandomGent attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param randomGent
	 *            The new RandomGent value
	 */
	public void setRandomGent(RandomGenerator randomGent) {
		this.randomGent = randomGent;
	}

	/**
	 * Sets the Iterations attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param iterations
	 *            The new Iterations value
	 */
	public void setIterations(long iterations) {
		this.iterations = iterations;
	}

	/**
	 * Sets the convergenceCounter attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@param convergenceCounter
	 *            The new convergenceCounter value
	 */
	public void setConvergenceCounter(long convergenceCounter) {
		this.convergenceCounter = convergenceCounter;
	}

	/**
	 * Gets the convergenceCounter attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@return The convergenceCounter value
	 */
	public long getConvergenceCounter() {
		return convergenceCounter;
	}

	/**
	 * Sets the ChiefJustice attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param chiefJustice
	 *            The new ChiefJustice value
	 */
	public void setChiefJustice(ChiefJustice chiefJustice) {
		this.chiefJustice = chiefJustice;
	}

	/**
	 * Sets the MaxPlateauSteps attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@param mps
	 *            The new MaxPlateauSteps value
	 */
	public void setMaxPlateauSteps(long mps) {
		this.maxPlateauSteps = mps;
	}

	/**
	 * Sets the MaxUphillSteps attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@param mus
	 *            The new MaxUphillSteps value
	 */
	public void setMaxUphillSteps(long mus) {
		this.maxUphillSteps = mus;
	}

	/**
	 * Sets the ConvergenceStopCount attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@param csc
	 *            The new ConvergenceStopCount value
	 */
	public void setConvergenceStopCount(long csc) {
		this.convergenceStopCount = csc;
	}

	/**
	 * Sets the Start_kT attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param skt
	 *            The new Start_kT value
	 */
	public void setStart_kT(double skt) {
		this.start_kT = skt;
	}

	/**
	 * Sets the Current Temperature attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@param ckt
	 *            The new Current_kT value
	 */
	public void setCurrent_kT(double ckt) {
		this.current_kT = ckt;
	}

	/**
	 * Sets the CoolingRate attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param cr
	 *            The new CoolingRate value
	 */
	public void setCoolingRate(double cr) {
		this.coolingRate = cr;
	}

	/**
	 * Gets the RandomGent attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The RandomGent value
	 */
	public RandomGenerator getRandomGent() {
		return randomGent;
	}

	/**
	 * Gets the ChiefJustice attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The ChiefJustice value
	 */
	public ChiefJustice getChiefJustice() {
		return chiefJustice;
	}

	/**
	 * Gets the Iterations attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The Iterations value
	 */
	public long getIterations() {
		return iterations;
	}

	/**
	 * Gets the InitialAcceptanceProbability attribute of the
	 * ConvergenceAnnealingEngine object
	 *
	 *@return The InitialAcceptanceProbability value
	 */
	public double getInitialAcceptanceProbability() {
		return initialAcceptanceProbability;
	}

	/**
	 * Gets the CoolingRate attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The CoolingRate value
	 */
	public double getCoolingRate() {
		return coolingRate;
	}

	/**
	 * Gets the Temperature attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The Temperature value
	 */
	public double getTemperature() {
		if (debug) {
			// logger.debug("ConvergenceAnnealingEngine->getCurrent_kT");
		}
		return current_kT;
	}

	/**
	 * Gets the Finished attribute of the ConvergenceAnnealingEngine object
	 *
	 *@return The Finished value
	 */
	public boolean isFinished() {
		if (isConverged) {
			// logger.debug("Annealing run finished.");
			return true;
		}
		return false;
	}

	/**
	 * Gets the MaxPlateauSteps attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@return The MaxPlateauSteps value
	 */
	public long getMaxPlateauSteps() {
		return this.maxPlateauSteps;
	}

	/**
	 * Gets the MaxUphillSteps attribute of the ConvergenceAnnealingEngine
	 * object
	 *
	 *@return The MaxUphillSteps value
	 */
	public long getMaxUphillSteps() {
		return this.maxUphillSteps;
	}

	/**
	 * Gets the Accepted attribute of the ConvergenceAnnealingEngine object
	 *
	 *@param recentScore
	 *            Description of Parameter
	 *@param lastScore
	 *            Description of Parameter
	 *@return The Accepted value
	 */
	public boolean isAccepted(double recentScore, double lastScore) {
		double deltaE = lastScore - recentScore;
		// logger.debug("LastScore, RecentScore: " + lastScore + ", " +
		// recentScore);
		double rnd = Math.random();
		double exp = ((double) deltaE / (double) current_kT);
		if (deltaE <= 0) {
			// logger.debug("Accepted better or equal result: deltaE = " +
			// deltaE);
			uphill++;
			return true;
		} else {
			if (rnd < Math.exp(-exp)) {
				// logger.debug("Accepted stochastic update: rnd = " + rnd);
				// logger.debug("Math.exp(-exp) = " + Math.exp(-exp));
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Cool down according to the given annealing schedule
	 */
	public void cool() {
		// logger.debug("Cooling... convergenceCounter: " + convergenceCounter);
		if (plateauStepCounter > maxPlateauSteps || uphill > maxUphillSteps) {
			current_kT *= coolingRate;
			plateauStepCounter = 0;
			uphill = 0;
		} else if (((double) iterations / (double) reportsteps) == (int) ((double) iterations / (double) reportsteps)) {
			// logger.debug("iterations: " + iterations +
			// "; convergenceCounter: " + convergenceCounter + "; current_kT: "
			// + current_kT);
		}
		plateauStepCounter++;
		convergenceCounter++;
		// logger.debug("convergence in step: " + (convergenceStopCount -
		// convergenceCounter));
		if (convergenceCounter > convergenceStopCount || current_kT < 1.0) {
			// logger.debug("Convergence criterion reached!");
			// logger.debug("convergenceCounter: " + convergenceCounter + "/" +
			// convergenceStopCount);
			// logger.debug("current_kT: " + current_kT);
			// logger.debug("iterations: " + iterations);

			isConverged = true;
		}
		iterations++;
	}

	/**
	 * Initializes the annealing procedure each time a new instance of
	 * AnnealingSchedule is popped out of the schedules ArrayList.
	 *
	 *@param randomGent
	 *            Description of the Parameter
	 *@param chiefJustice
	 *            Description of the Parameter
	 */
	public void initAnnealing(RandomGenerator randomGent,
			ChiefJustice chiefJustice, IProgressMonitor monitor) {
		// BioclipseConsole.writeToConsole(
		// "Starting initialization of annealing engine");
		IMolecule mol = null;
		double lastScore = 0;
		double recentScore = 0;
		long changes = 0;
		// BioclipseConsole.writeToConsole("Number of Init Cycles: " +
		// initCycles);
		int counter = 0;
		monitor.subTask("Generator staring structures...");
		for (int f = 0; f < initCycles; f++) {
			if (monitor.isCanceled())
				return;

			// logger.debug("Proposing structure...");
			monitor.worked(1);
			mol = randomGent.proposeStructure();
			// logger.debug("  accepting structure...");
			randomGent.acceptStructure();
			if (counter == 25) {
				// logger.debug(".");
				counter = 0;
			}
			// logger.debug("  detecting aromaticity...");
			try {
				CDKHueckelAromaticityDetector.detectAromaticity(mol);
				// logger.debug("  scoring molecule...");
				recentScore = chiefJustice.getScore(mol).score;
				// logger.debug("  score: " + recentScore);
				//this annealing engine expected sores to be 0 to 1000, but
				//since they are 0-1 now, we calculate *1000 here
				changes += Math.abs(lastScore*1000 - recentScore*1000);
				lastScore = recentScore;
			} catch (Exception exc) {
				// logger.error("Error while detecting aromaticity: " +
				// exc.getMessage());
			}
			counter++;
		}
		changes = (long) (changes / initCycles);
		setStart_kT(-changes / Math.log(getInitialAcceptanceProbability()));
		setCurrent_kT(start_kT);
		// BioclipseConsole.writeToConsole("Starting temperature set to: " +
		// start_kT);
		// BioclipseConsole.writeToConsole("ConvergenceStopCount is: " +
		// convergenceStopCount);
		// BioclipseConsole.writeToConsole("Done SA initialization.");
	}

	public long getConvergenceStopCount() {
		return this.convergenceStopCount;
	}

	public void setConvergenceCount(long convergenceCounter) {
		this.convergenceCounter = convergenceCounter;
	}

	public double getAcceptanceProbability() {
		return this.initialAcceptanceProbability;
	}

	public void setAcceptanceProbability(double prob) {
		this.initialAcceptanceProbability = prob;
	}

	public double getInitializationCycles() {
		return this.initCycles;
	}

	public void setInitializationCycles(int count) {
		this.initCycles = count;
	}

}
