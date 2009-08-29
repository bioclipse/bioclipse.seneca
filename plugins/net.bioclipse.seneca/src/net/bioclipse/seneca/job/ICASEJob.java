/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Egon Willighagen
 ******************************************************************************/
package net.bioclipse.seneca.job;

import java.util.List;

import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.util.StructureGeneratorResult;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class defines the API with which the wizard can set job parameters.
 *
 * @author egonw
 */
public interface ICASEJob {

	public void setJobSpecification(SenecaJobSpecification specification);

	public List<IJudge> getJudges();

	public void addJudge(IJudge judge);
	
	public StructureGeneratorResult run(IProgressMonitor monitor);
	
	public void addScoreImprovedListener(IScoreImprovedListener listener);
	
	public void addTemperatureAndScoreListener(TemperatureAndScoreListener listener);
	
	public void setDetectAromaticity(boolean detectAromaticity);

}
