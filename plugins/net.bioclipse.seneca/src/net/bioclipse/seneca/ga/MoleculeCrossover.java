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

import org.openscience.cdk.Molecule;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.stochastic.operator.CrossoverMachine;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;


/**
 * A molecule crossover for use in Watchmaker, based on cdk CrossoverMachine
 * 
 */
public class MoleculeCrossover extends AbstractCrossover<IMolecule> {

    private CrossoverMachine cm = new CrossoverMachine();
    private boolean detectAromaticity;
    
    public MoleculeCrossover(boolean detectAromaticity) {
        super(1);
        this.detectAromaticity = detectAromaticity;
    }

    @Override
    protected List<IMolecule> mate( IMolecule arg0, IMolecule arg1, int arg2,
                                   Random arg3 ) {
        List<IAtomContainer> l;
        try {
            l = cm.doCrossover( arg0, arg1 );
            List<IMolecule> result =new ArrayList<IMolecule>();
            if(detectAromaticity){
                CDKHueckelAromaticityDetector.detectAromaticity( l.get( 0 ) );
                CDKHueckelAromaticityDetector.detectAromaticity( l.get( 1 ) );
            }
            result.add( new Molecule(l.get( 0 ) ));
            result.add( new Molecule(l.get( 1 ) ));
            return result;
        } catch ( CDKException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            List<IMolecule> result = new ArrayList<IMolecule>();
            result.add( arg0 );
            result.add( arg1 );
            return result;
        }
    }

}
