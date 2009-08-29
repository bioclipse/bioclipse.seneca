package net.bioclipse.seneca.job;

import org.openscience.cdk.interfaces.IMolecule;


public interface IScoreImprovedListener {
    
    public void betterScore(IMolecule mol);

}
