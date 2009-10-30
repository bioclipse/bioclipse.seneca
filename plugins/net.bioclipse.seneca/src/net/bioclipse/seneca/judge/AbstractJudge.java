/*
 *  Judge.java
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

import org.eclipse.core.runtime.IPath;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Base-class for evaluating the score of a particular structure with respect to
 * agreement to a given parameter. This class is to be subclassed and
 * customized.
 *
 * @author steinbeck
 * @created October 5, 2001
 */

public abstract class AbstractJudge implements IJudge {

	/*
	 * A name identifying the scope of the Judge
	 */
	public String name = "nop";

	/*
	 * Important property of a Judge. Tells a score summarizing entity if it is
	 * possible to calculate a maximum score for this judge This is important,
	 * for example, for letting a Simulated Annealing run converge to a maximum
	 * achivable score.
	 */
	boolean hasMaxScore = false;

	boolean ringSetRequired = false;
	double maxScore, scoreSum;
	String resultString;
	transient JudgeListener judgeListener = null;

	/*
	 * Should this Judge be used during the evaluation process?
	 */
	private boolean enabled = false;
	private boolean initialized = false;
	static boolean debug = false;
	static boolean report = true;
	private IPath datafile;

	/**
	 * Constructor for the Judge object
	 *
	 * @param name
	 *            Description of Parameter
	 */
	public AbstractJudge(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		fireChanged();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setInitialized(boolean)
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setJudgeListener(seneca.judges.JudgeListener)
	 */
	public void setJudgeListener(JudgeListener jl) {
		this.judgeListener = jl;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setRingSetRequired(boolean)
	 */
	public void setRingSetRequired(boolean ringSetRequired) {
		this.ringSetRequired = ringSetRequired;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setHasMaxScore(boolean)
	 */
	public void setHasMaxScore(boolean hasMaxScore) {
		this.hasMaxScore = hasMaxScore;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#setMaxScore(int)
	 */
	public void setMaxScore(int maxScore) {
		this.maxScore = maxScore;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#isInitialized()
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#getEnabled()
	 */
	public boolean getEnabled() {
		return this.enabled;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#isRingSetRequired()
	 */
	public boolean isRingSetRequired() {
		return this.ringSetRequired;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#hasMaxScore()
	 */
	public boolean hasMaxScore() {
		return this.hasMaxScore;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#getMaxScore()
	 */
	public double getMaxScore() {
		return this.maxScore;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#evaluate(org.openscience.cdk.AtomContainer)
	 */
	public abstract JudgeResult evaluate(IAtomContainer ac) throws Exception;

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#calcMaxScore()
	 */
	public abstract void calcMaxScore();

	/*
	 * (non-Javadoc)
	 *
	 * @see seneca.judges.IJudge#fireChanged()
	 */
	public void fireChanged() {
		if (judgeListener != null) {
			judgeListener.judgeDataChanged();
		}
	}
	

  public IPath getData() {
      return datafile;
  }

  public void setData( IPath data ) {
      this.datafile=data;
  }
  
  abstract public IJudge createJudge(IPath data)
	throws MissingInformationException;

}
