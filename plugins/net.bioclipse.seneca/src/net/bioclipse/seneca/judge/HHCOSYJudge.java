/* HHCOSYJudge.java
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Gets the AllPairsShortestPath matrix for a given structure and checks if all
 * of the HHCOSY rules are fullfilled. HHCOSY rules are given as a 3D matrix of
 * size [n][n][n] where n is the number of atoms in the structure. A value != 0
 * at [x][y][y] indicates that there was a HHCOSY crosspeak between the signals
 * of heavyatom x and y. In the third dimension alternatives to y are noted to
 * handle ambigous assignments.
 */

public class HHCOSYJudge extends TwoDSpectrumJudge {

	public HHCOSYJudge() {
		super("HHCOSYJudge");
		setScore(100, 0);
	}

	public IJudge createJudge(IPath data) throws MissingInformationException {
		IJudge judge = new HHCOSYJudge();
		//TODO use data
		//judge.configure(input);
		judge.setEnabled(true);
		return judge;
	}

    public String getDescription() {

        // TODO Auto-generated method stub
        return null;
    }

    public boolean checkJudge( String data ) {

        // TODO Auto-generated method stub
        return false;
    }

    public IFile setData( ISelection selection, IFile sjsFile ) {

        // TODO Auto-generated method stub
        return null;
    }

	public boolean isLabelling() {
		// TODO Auto-generated method stub
		return false;
	}

	public void labelStartStructure(IAtomContainer startStructure) {
		// TODO Auto-generated method stub
		
	}
}
