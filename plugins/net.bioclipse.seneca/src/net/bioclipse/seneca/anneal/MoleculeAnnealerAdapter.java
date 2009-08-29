package net.bioclipse.seneca.anneal;

import java.util.ArrayList;

import net.bioclipse.seneca.anneal.MoleculeState.Acceptance;
import net.bioclipse.seneca.judge.ChiefJustice;
import net.bioclipse.seneca.judge.IJudge;

import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.RandomGenerator;

public class MoleculeAnnealerAdapter implements AnnealerAdapterI {
	
	private ChiefJustice judge; 
	
	private final ArrayList<StateListener> stateListeners;
	private RandomGenerator randomGenerator;
	
	private double bestCost;
	private double currentCost;
	private double nextCost;
	
	private IMolecule best;
	private IMolecule current;
	private IMolecule next;
	
	private int stepIndex;
	private int bestStepIndex;
	
	private boolean isCancelled;
	private boolean detectAromaticity;
	
	public MoleculeAnnealerAdapter(IMolecule startingMolecule, ChiefJustice judge, boolean detectAromaticity) {
		this.judge = judge;
		this.detectAromaticity = detectAromaticity;
		this.stateListeners = new ArrayList<StateListener>();
		
		this.randomGenerator = new RandomGenerator(startingMolecule);
		
		this.current = startingMolecule;
		this.next = null;
		this.best = current;
		this.bestCost = this.currentCost = this.nextCost = 0.0;
		
		this.stepIndex = 0;
		this.bestStepIndex = 0;
		
		this.isCancelled = false;
	}
	
	public void setCancelled(boolean cancelled) {
	    this.isCancelled = cancelled;
	}
	
	public boolean isCancelled() {
	    return this.isCancelled;
	}
	
	public IMolecule getBest() {
		return this.best;
	}
	
	public int getBestStepIndex() {
		return this.bestStepIndex;
	}
	
	public IMolecule getCurrent() {
		return this.current;
	}

	public void addStateListener(StateListener listener) {
		this.stateListeners.add(listener);
	}

	public boolean costDecreasing() {
//		System.out.println("current cost: "+ this.currentCost);
//		System.out.println("previous cost: "+ this.nextCost);
		return this.nextCost < this.currentCost;
	}

	public double costDifference() {
		return this.currentCost - this.nextCost;
	}
	
	private double cost(IMolecule mol) {
		// the score is in the range [0-judge.calcMaxScore()], so the cost must be judge.calcMaxScore()-score.
    try {
        return judge.calcMaxScore() - this.judge.getScore(mol).score;
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
	}

	public void initialState() throws CDKException {
		// bit pointless.
		this.current = this.randomGenerator.getMolecule();
		if(detectAromaticity)
		    CDKHueckelAromaticityDetector.detectAromaticity( this.current );
		this.currentCost = cost(this.current);
		this.bestCost = this.currentCost;
	}

	public void nextState() throws CDKException {
		this.next = this.randomGenerator.proposeStructure();
    if(detectAromaticity)
        CDKHueckelAromaticityDetector.detectAromaticity( this.next );
		this.nextCost = cost(this.next);
		this.stepIndex++;
		fireStateEvent(new MoleculeState(current, Acceptance.UNKNOWN, stepIndex,judge.calcMaxScore()-nextCost));
	}

	public void accept() {
		this.current = this.next;
		this.currentCost = this.nextCost;
		if (this.currentCost < this.bestCost) {
			//System.out.println("best > current, storing" + this.bestCost + " " + this.currentCost);
			this.best = this.current;
			this.bestCost = currentCost;
			this.bestStepIndex = this.stepIndex;
	    fireStateEvent(new MoleculeState(current, Acceptance.ACCEPT, stepIndex,judge.calcMaxScore()-currentCost));
		} else {
			//System.out.println("best !> current, !storing" + this.bestCost + " " + this.currentCost);
		}
		this.randomGenerator.acceptStructure(); 
		
	}

	public void reject() {
		fireStateEvent(new MoleculeState(next, Acceptance.REJECT, stepIndex,0));
	}
	
	private void fireStateEvent(State state) {
		for (StateListener listener : this.stateListeners) {
			listener.stateChanged(state);
		}
	}

  public double getBestCost() {      
      return bestCost;
  }
}
