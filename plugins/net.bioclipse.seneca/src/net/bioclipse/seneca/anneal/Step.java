package net.bioclipse.seneca.anneal;

import org.openscience.cdk.interfaces.IMolecule;


public class Step {
	
	public final int index;
	public final IMolecule mol;
	public final String smiles;
	public final double score;
//	public final Spectrum spectrum;
	public final MoleculeState.Acceptance acceptance;
	
	public Step(int index, 
			//IMolecule mol, String smiles, Spectrum spectrum,
	        IMolecule mol, String smiles, 
			double score, MoleculeState.Acceptance acceptance) {
		this.index = index; 
		this.mol = mol;
		this.smiles = smiles;
//		this.spectrum = spectrum;
		this.score = score;
		this.acceptance = acceptance;
	}
	
	public String toString() {
//		return String.format("%s\t%2.2f\t%s\t%s\t%s",
	    return String.format("%s\t%2.2f\t%s\t%s",
//				this.index, this.score, this.acceptance, this.smiles, this.spectrum.toSimpleString());
	            this.index, this.score, this.acceptance, this.smiles);
	}

}
