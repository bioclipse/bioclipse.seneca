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

import net.bioclipse.seneca.anneal.MoleculeState;
import net.bioclipse.seneca.anneal.MoleculeState.Acceptance;
import net.bioclipse.seneca.job.GAStructureElucidationJob;

import org.openscience.cdk.interfaces.IMolecule;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.PopulationData;


/**
 * A molecule evolution observer for Watchmaker.
 *
 */
public class MoleculeEvolutionObserver implements
        EvolutionObserver<IMolecule> {
    GAStructureElucidationJob site;
    double bestscoresofar=0;
    
    public MoleculeEvolutionObserver(GAStructureElucidationJob site){
        this.site = site;
    }

    public void populationUpdate( PopulationData<? extends IMolecule> data ) {
        site.temperatureChange( 0 );
        if(data.getBestCandidateFitness()>bestscoresofar){
            bestscoresofar = data.getBestCandidateFitness();
            MoleculeState moleculeState = new MoleculeState(data.getBestCandidate(),Acceptance.ACCEPT,data.getGenerationNumber(), data.getBestCandidateFitness());
            site.stateChanged( moleculeState );
        }
    }

}
