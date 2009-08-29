package net.bioclipse.seneca.anneal;

import org.openscience.cdk.interfaces.IMolecule;

public class MoleculeState implements State {
	
	public enum Acceptance { ACCEPT, REJECT, UNKNOWN };
	
	public final IMolecule molecule;
	
	public final Acceptance acceptance;
	
	public final int stepIndex;
	
	public double score=0;
	
	public MoleculeState(IMolecule molecule, Acceptance acceptance, int stepIndex, double score) {
		this.molecule = molecule;
		this.acceptance = acceptance;
		this.stepIndex = stepIndex;
		this.score = score;
	}
	
	public int getStep() {
		return this.stepIndex;
	}

}
