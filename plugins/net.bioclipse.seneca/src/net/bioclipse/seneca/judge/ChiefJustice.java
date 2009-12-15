/*
 *  ChiefJustice.java
 *
 *  Copyright (C) 1997, 1998, 1999, 2000  Christoph Steinbeck
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
package net.bioclipse.seneca.judge;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IMolecule;

/**
 * Administers and controls all the Judges involved in a CASE run
 *
 * @author steinbeck
 * @created September 10, 2001
 */
public class ChiefJustice {

	List<IJudge> judges = null;
	static boolean debug = false;
	static boolean report = true;
	boolean isInitialized = false;

	/**
	 * Constructor for the ChiefJustice object
	 */
	public ChiefJustice() {
		this(new ArrayList<IJudge>());
	}

	/**
	 * Constructor for the ChiefJustice object
	 *
	 * @param judges
	 *            Description of Parameter
	 */
	public ChiefJustice(List<IJudge> judges) {
		this.judges = judges;
	}

	/**
	 * initializes the judges
	 */
	public void initJudges() {
		for (int f = 0; f < judges.size(); f++) {
			IJudge judge = (IJudge) judges.get(f);
			if (judge.getEnabled()) {
				if (judge.hasMaxScore()) {
					judge.calcMaxScore();
				}
			}
		}
		isInitialized = true;
	}

	/**
	 * Sets the Judges attribute of the ChiefJustice object
	 *
	 * @param judges
	 *            The new Judges value
	 */
	public void setJudges(List<IJudge> judges) {
		this.judges = judges;
	}

	/**
	 * Gets the Score attribute of the ChiefJustice object
	 *
	 * @param ac
	 *            Description of Parameter
	 * @return The Score value
	 * @exception JudgeEvaluationException
	 *                Description of Exception
	 */
	public ScoreSummary getScore(IMolecule molecule)  {
		if (!isInitialized)
			initJudges();
		double score = 0;
		String description = "";
		double maxScore = 0;
		for (int f = 0; f < judges.size(); f++) {
			IJudge judge = (IJudge) judges.get(f);
			if (judge.getEnabled()) {
				try {
					JudgeResult ser = judge.evaluate(molecule);
					maxScore += ser.maxScore;
					score += ser.score;
					if (debug) {
						System.out.println("Score from Judge " + judge.getName()
								+ ": " + ser.score);
					}
					description += ser.scoreDescription + "\n";
				} catch (Exception e) {
					// TODO Auto-generated catch block
					// FIXME : do we want to catch here?
					e.printStackTrace();
				}
			}
		}
		if (score < 0) {
			score = 0;
		}
		ScoreSummary scsy = new ScoreSummary(score, description);
		scsy.maxScore = maxScore;
		return scsy;
	}
	
   public double calcMaxScore() {
       double maxScore = 0;
       for (int f = 0; f < judges.size(); f++) {
         IJudge judge = (IJudge) judges.get(f);
         if (judge.getEnabled()) {
             maxScore += judge.getMaxScore();
         }
       }
       return maxScore;
   }

	/**
	 * Gets the Judges attribute of the ChiefJustice object
	 *
	 * @return The Judges value
	 */
	public List<IJudge> getJudges() {
		return judges;
	}

	/**
	 * Adds a feature to the Judge attribute of the ChiefJustice object
	 *
	 * @param judge
	 *            The feature to be added to the Judge attribute
	 */
	public void addJudge(IJudge judge) {
		this.judges.add(judge);
		this.isInitialized = false;
	}

	/**
	 * Description of the Method
	 *
	 * @param judge
	 *            Description of Parameter
	 */
	public void removeJudge(IJudge judge) {
		this.judges.remove(judge);
	}

	public void label(IMolecule arg0) {
		for(int i=0;i<judges.size();i++){
            if(judges.get(i).isLabelling()){
            	judges.get(i).labelStartStructure(arg0);
			}
		}
	}
}
