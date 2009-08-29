package net.bioclipse.seneca.judge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.openscience.cdk.interfaces.IAtomContainer;

public interface IJudge {


	/**
	 * Create an instance of the class, and configure it.
	 *
	 * @param input the CML to create the judge from.
	 */
	public abstract IJudge createJudge(IPath data) throws MissingInformationException;


	/**
	 *  Sets the Name attribute of the Judge object
	 *
	 * @param  name  The new Name value
	 */
	public abstract void setName(String name);

	/**
	 *  Sets the Multiplicator attribute of the Judge object
	 *
	 * @param  multiplicator  The new Multiplicator value
	 */
	public abstract void setMultiplicator(int multiplicator);

	/**
	 *  Sets the Enabled attribute of the Judge object
	 *
	 * @param  enabled  The new Enabled value
	 */
	public abstract void setEnabled(boolean enabled);

	/**
	 *  Sets the Initialized attribute of the Judge object
	 *
	 * @param  initialized  The new Initialized value
	 */
	public abstract void setInitialized(boolean initialized);

	/**
	 *  Sets the JudgeListener attribute of the Judge object
	 *
	 * @param  jl  The new JudgeListener value
	 */
	public abstract void setJudgeListener(JudgeListener jl);

	/**
	 *  Sets the RingSetRequired attribute of the Judge object
	 *
	 * @param  ringSetRequired  The new RingSetRequired value
	 */
	public abstract void setRingSetRequired(boolean ringSetRequired);

	/**
	 *  Sets the HasMaxScore attribute of the Judge object
	 *
	 * @param  hasMaxScore  The new HasMaxScore value
	 */
	public abstract void setHasMaxScore(boolean hasMaxScore);

	/**
	 *  Sets the MaxScore attribute of the Judge object
	 *
	 * @param  maxScore  The new MaxScore value
	 */
	public abstract void setMaxScore(int maxScore);

	/**
	 *  Gets the Initialized attribute of the Judge object
	 *
	 * @return    The Initialized value
	 */
	public abstract boolean isInitialized();

	/**
	 *  Gets the Multiplicator attribute of the Judge object
	 *
	 * @return    The Multiplicator value
	 */
	public abstract int getMultiplicator();

	/**
	 *  Gets the Name attribute of the Judge object
	 *
	 * @return    The Name value
	 */
	public abstract String getName();

	/**
	 *  Gets the Enabled attribute of the Judge object
	 *
	 * @return    The Enabled value
	 */
	public abstract boolean getEnabled();

	/**
	 *  Gets the RingSetRequired attribute of the Judge object
	 *
	 * @return    The RingSetRequired value
	 */
	public abstract boolean isRingSetRequired();

	/**
	 *  Gets the HasMaxScore attribute of the Judge object
	 *
	 * @return    The HasMaxScore value
	 */
	public abstract boolean hasMaxScore();

	/**
	 *  Gets the MaxScore attribute of the Judge object
	 *
	 * @return    The MaxScore value
	 */
	public abstract double getMaxScore();

	/**
	 *  Description of the Method
	 *
	 * @param  ac                            Description of Parameter
	 * @return                               Description of the Returned Value
	 * @exception  JudgeEvaluationException  Description of Exception
	 */
	public abstract JudgeResult evaluate(IAtomContainer ac) throws Exception;

	/**
	 *  Description of the Method
	 */
	public abstract void calcMaxScore();

	/**
	 *  Description of the Method
	 */
	public abstract void fireChanged();

	public boolean[][][] getAssignment();
	
	public void setData(IPath data);
	
	public IPath getData();
	
	/**
	 * Gives a longer description of the judge to be displayed on judge page.
	 **/
	public String getDescription();
	
	/**
	 * Tells if this judge has all information needed in input
	 **/
	public boolean checkJudge(String data);
	
	/**
	 * This method is called by the editor if files are dropped onto the editor 
	 * section for that judge. The judge needs to check if data are correct (right 
	 * spectrum type etc.), perform any analyses (peak picking etc.) and return 
	 * the file where the processed data have been put into.
	 *  
	 * @param selection The selectoon the user dropped on the configuration page.
	 * @return The file where processed data are in.
	 */
	public IFile setData(ISelection selection, IFile sjsFile);

}