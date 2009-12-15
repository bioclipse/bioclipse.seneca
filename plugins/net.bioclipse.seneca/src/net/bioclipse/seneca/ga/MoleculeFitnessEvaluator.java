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

import java.util.List;

import net.bioclipse.seneca.judge.ChiefJustice;

import org.openscience.cdk.interfaces.IMolecule;
import org.uncommons.watchmaker.framework.FitnessEvaluator;


/**
 * A molecule fitness evaluator for Watchmaker based on a ChiefJustice.
 *
 */
public class MoleculeFitnessEvaluator implements FitnessEvaluator<IMolecule> {
    
    ChiefJustice justice;
    
    public MoleculeFitnessEvaluator(ChiefJustice justice){
        this.justice = justice;
    }
    

    public double getFitness( IMolecule arg0, List<? extends IMolecule> arg1 ) {
    	justice.label(arg0);
        return justice.getScore( arg0).score;
    }

    public boolean isNatural() {
        return true;
    }
}
