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
package net.bioclipse.seneca.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.RandomGenerator;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;


/**
 * A molecule mutator for Watchmaker based on cdk RandomGenerator.
 *
 */
public class MoleculeMutation implements EvolutionaryOperator<IMolecule> {
    private boolean detectAromaticity;
    
    public MoleculeMutation(boolean detectAromaticity){
        this.detectAromaticity = detectAromaticity;
    }

    public List<IMolecule> apply( List<IMolecule> arg0, Random arg1 ) {
        List<IMolecule> result= new ArrayList<IMolecule>();
        for(IMolecule molecule : arg0){
            RandomGenerator rg=new RandomGenerator(molecule);
            IMolecule mol = rg.proposeStructure();
            MoleculeCandidateFactory.rearangeAtoms( mol );
            if(detectAromaticity)
                try {
                    CDKHueckelAromaticityDetector.detectAromaticity( mol );
                } catch ( CDKException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            result.add( mol );
        }
        return result;
    }

}
