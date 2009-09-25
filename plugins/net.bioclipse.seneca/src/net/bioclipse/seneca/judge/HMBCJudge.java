/* HMBCJudge.java
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

import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;

import JSX.ObjIn;

/**
 * Gets the AllPairsShortestPath matrix for a given structure and checks if all
 * of the HMBC rules are fullfilled. HMBC rules are given as a 3D matrix of size
 * [n][n][n] where n is the number of atoms in the structure. A value != 0 at
 * [x][y][y] indicates that there was a HMBC crosspeak between the signals of
 * heavyatom x and y. In the third dimension alternatives to y are noted to
 * handle ambigous assignments.
 */

public class HMBCJudge extends TwoDSpectrumJudge {

	public HMBCJudge() {
		super("HMBCJudge");
		setScore(100, 0);
		setScore(100, 1);
		setScore(5, 2);
	}


	public IJudge createJudge(IPath data) throws MissingInformationException {
		//IJudge judge = new HMBCJudge();
		this.setData( data );
		
		try {
			IWorkspaceRoot root=ResourcesPlugin.getWorkspace().getRoot();
	    Reader reader = new InputStreamReader(root.getFile(data).getContents());
	    ObjIn in;
			in = new ObjIn(reader);
	    HMBCJudge obj = (HMBCJudge)in.readObject();
	    
		this.setScores(obj.scores);
		this.setAssignment(obj.assignment);
		this.init();
		this.setEnabled(true);
		return this;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MissingInformationException(e.getMessage());
		}
	}

    public String getDescription() {
    	return "A simple 2d HMBC judge. Configuration is in a jsx file right now";
    }

    public boolean checkJudge( String data ) {

        // TODO implement a check
        return true;
    }

    public IFile setData( ISelection selection, IFile sjsFile ) {

        // TODO Auto-generated method stub
        return null;
    }

}
