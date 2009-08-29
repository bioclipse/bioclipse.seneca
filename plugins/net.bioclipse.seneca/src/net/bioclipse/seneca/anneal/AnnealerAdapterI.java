package net.bioclipse.seneca.anneal;

import org.openscience.cdk.exception.CDKException;

public interface AnnealerAdapterI {
    
    /**
     * Cancel the run.
     * 
     * @param value true to cancel.
     */
    public void setCancelled(boolean value);
    
    /**
     * Check the status of the run, through the adapter
     * 
     * @return true if the run has been cancelled
     */
    public boolean isCancelled();
    
	/**
	 * Add a listener to the list
	 * 
	 * @param listener
	 */
	public void addStateListener(StateListener listener);

	/**
	 * Generate an initial state (internally).
	 * @throws CDKException 
	 */
	public void initialState() throws CDKException;

	/**
	 * Generate a new state internally, and store it for acceptance/rejection.
	 * @throws CDKException 
	 */
	public void nextState() throws CDKException;

	/**
	 * @return true if the cost of the next state is 
	 * less than the cost of the current.
	 */
	public boolean costDecreasing();

	/**
	 * Accept the next state.
	 */
	public void accept();

	/**
	 * Reject the next state, go back to current. 
	 */
	public void reject();

	/**
	 * @return the difference between the cost of the current and next states.
	 */
	public double costDifference();
	
	/**
	 * 
	 * @return The cost of the best structure so far
	 */
	public double getBestCost();
	
	/**
	 * 
	 * @return The step in which the best result so far was achieved.
	 */
	public int getBestStepIndex();
	
}