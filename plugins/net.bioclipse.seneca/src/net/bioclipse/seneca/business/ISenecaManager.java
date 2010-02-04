package net.bioclipse.seneca.business;

import java.util.List;

import net.bioclipse.core.PublishedClass;
import net.bioclipse.core.PublishedMethod;
import net.bioclipse.core.Recorded;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.jobs.BioclipseJob;
import net.bioclipse.jobs.BioclipseJobUpdateHook;
import net.bioclipse.jobs.BioclipseUIJob;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.judge.JudgeResult;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

@PublishedClass("Contains Seneca methods")
public interface ISenecaManager extends IBioclipseManager {

	/**
	 * Load the specification from an sjs file.
	 *
	 * @param file the IFile to read from
	 * @return
	 */
	@Recorded
	@PublishedMethod( params = "IFile file",
			methodSummary = "Loads a job specification from a file. ")
	public SenecaJobSpecification getJobSpecification(IFile file) throws BioclipseException;

  /**
   * Load the specification from an sjs file.
   *
   * @param file the path to the file to read from, relative to workspace.
   * @return
   */
  @Recorded
  @PublishedMethod( params = "String file",
      methodSummary = "Loads a job specification from a file. ")
  public SenecaJobSpecification getJobSpecification(String file);

  /**
	 * Create a new specification in an sjs file.
	 *
	 * @param filename the name of the file to create
	 */
	@Recorded
	public boolean createSenecaJob(IContainer parent, String jobTitle) throws BioclipseException, CoreException;

	/**
	 * Save the Specification to an sjs file.
	 *
	 * @param file the file to save to
	 * @param specification the job specification
	 */
	@Recorded
	public boolean saveSenecaJob(IFile file,
			SenecaJobSpecification specification) throws CoreException;

  @Recorded
  @PublishedMethod( params = "SenecaJobSpecification sjs",
                    methodSummary = "Validates the job specification. ")
  public boolean validateJob(SenecaJobSpecification sjs);

  
  @Recorded
  @PublishedMethod( params = "SenecaJobSpecification sjs",
                    methodSummary = "Executes an elucidation job and return the " +
                        "structures which improved score during the job. Scores " +
                        "and step count are set as properties on the molecules. ")
  public List<IMolecule> executeJob(SenecaJobSpecification sjs);

  @Recorded
  public BioclipseJob<IMolecule> executeJob(SenecaJobSpecification sjs, BioclipseJobUpdateHook<IMolecule> hook);
  
  @Recorded
  public void executeJob(SenecaJobSpecification sjs, BioclipseUIJob<List<IMolecule>> uiJob);

  @Recorded
  @PublishedMethod( params = "SenecaJobSpecification sjs, IMolecule structure",
                    methodSummary = "Evaluates a structure against the scoring " +
                        "functions defined in a SenecaJobSpecification. Only " +
                        "the scoring part of sjs is used. ")
  public JudgeResult evaluateStructure(SenecaJobSpecification sjs, IMolecule structure);

  @Recorded
  @PublishedMethod( params = "SenecaJobSpecification sjs, List<IMolecule> structure",
          methodSummary = "Evaluates a list of structure against the scoring " +
          	"functions defined in a SenecaJobSpecification. Only " +
          	"the scoring part of sjs is used. ")
  public List<JudgeResult> evaluateStructures(SenecaJobSpecification sjs, List<IMolecule> structure);
}
