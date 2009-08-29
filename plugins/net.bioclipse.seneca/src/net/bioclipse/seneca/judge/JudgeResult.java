/* JudgeResult.java
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

/**
 * Instances of this class are returned by Judges. In addition to the score the
 * do also return the maximum possible score in this run and the property value
 * based on which the score has been calculated
 */

public class JudgeResult {

	/** The score calculated for the current structure **/
	public double score;

	/** The maximum reachable score - This is certainly not always defined. **/
	public double maxScore;

	/**
	 * The score is often based on particular property, e. g. the Wiener Number
	 * of the current structure. This field allows passing back a value for this
	 * property
	 **/
	public long propertyValue;

	/**
	 * A Human-readable description like '10 of 20 HMBC signals satisfied'. This
	 * can for instance be used as the title for a structure drawing.
	 **/
	public String scoreDescription;

	public JudgeResult(JudgeResult other) {
		this.maxScore = other.maxScore;
		this.score = other.score;
		this.propertyValue = other.propertyValue;
		this.scoreDescription = new String(other.scoreDescription);
	}

	public JudgeResult(double maxScore, double score, long propertyValue,
			String scoreDescription) {
		this.maxScore = maxScore;
		this.score = score;
		this.propertyValue = propertyValue;
		this.scoreDescription = scoreDescription;
	}
}