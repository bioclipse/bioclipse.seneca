package net.bioclipse.seneca.business;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import net.bioclipse.cdk.domain.CDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.jobs.IReturner;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.StructureGeneratorSettingsPage;
import net.bioclipse.seneca.editor.TemperatureAndScoreListener;
import net.bioclipse.seneca.job.GAStructureElucidationJob;
import net.bioclipse.seneca.job.ICASEJob;
import net.bioclipse.seneca.job.IScoreImprovedListener;
import net.bioclipse.seneca.job.StochasticStructureElucidationJob;
import net.bioclipse.seneca.job.UserConfigurableStochasticStructureElucidationJob;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.MissingInformationException;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SenecaManager implements IBioclipseManager {

	private IFinishListener finishlistener;
	private TemperatureAndScoreListener temperaturelistener;

    public String getManagerName() {
		return "seneca";
	}

	/**
	 * Load the specification from an sjs file.
	 *
	 * @param file the IFile to read from
	 * @return
	 */
	public SenecaJobSpecification getJobSpecification(IFile file) throws BioclipseException{
		Builder parser = new Builder();
		try {
			Document doc = parser.build(file.getContents());
			return new SenecaJobSpecification(doc,file.getParent());
		} catch (ValidityException e) {
			throw new BioclipseException(e.getMessage(), e);
		} catch (ParsingException e) {
	      throw new BioclipseException(e.getMessage(), e);
		} catch (IOException e) {
	      throw new BioclipseException(e.getMessage(), e);
		} catch (CoreException e) {
	      throw new BioclipseException(e.getMessage(), e);
		}
	}

	/**
	 * Create a new specification in an sjs file.
	 *
	 * @param filename the name of the file to create
	 */
	public boolean createSenecaJob(IContainer parent, String jobTitle) throws BioclipseException, CoreException{
	  IFolder projectFolder = parent.getFolder( new Path(jobTitle));
	  if(projectFolder.exists())
	      throw new BioclipseException("A directory "+parent.getFullPath()+" already exists");
	  projectFolder.create( false, true, new NullProgressMonitor() );
	  
		SenecaJobSpecification specification
			= new SenecaJobSpecification();
		specification.setJobTitle( jobTitle );
		String extension = "sjs";

		IFile file = projectFolder.getFile(new Path(jobTitle+"."+extension));
		boolean successful = net.bioclipse.seneca.Activator.getDefault().getJavaSenecaManager().saveSenecaJob(file, specification);
		// open this file
		if (successful) {
			net.bioclipse.ui.business.Activator.
				getDefault().getUIManager().open(file);
			return true;
		}
		return false;
	}

	public boolean saveSenecaJob(IFile file,
				SenecaJobSpecification specification, IProgressMonitor monitor) throws CoreException{

		InputStream source = specification.getSource();
		boolean force = true;
		boolean keepHistory = false;
		if (file.exists()) {
			file.setContents(source, force, keepHistory, monitor);
		} else {
			file.create(source, force, monitor);
		}
   	return true;
	}
	
	public void executeJob( SenecaJobSpecification jobSpec,
	                                   IReturner<IMolecule> returner,
	                                   IProgressMonitor monitor) 
	                       throws BioclipseException{
	    String generatorID = jobSpec.getGenerator();
	    ICASEJob job = null;
        //We take start start structure from the formula and use 
        //the DEPT information if any
        IAtomContainer startStructure 
            = DefaultChemObjectBuilder.getInstance().newAtomContainer();
        IMolecularFormula formula 
            = MolecularFormulaManipulator.getMolecularFormula( 
                  jobSpec.getMolecularFormula(),
                  startStructure.getBuilder() );
        for(int i=0;i<4;i++){
            int atomcount = jobSpec.getDeptData( i );
            for(int k=0;k<atomcount;k++){
              IAtom atom = startStructure.getBuilder().newAtom( "C" );
              atom.setHydrogenCount( i );
              startStructure.addAtom( atom );
              formula.removeIsotope( 
                  startStructure.getBuilder().newIsotope( "C" ) );
              for(int l=0;l<i;l++){
                  formula.removeIsotope( 
                      startStructure.getBuilder().newIsotope( "H" ) );
              }
            }
        }
        IAtomContainer residue 
            = MolecularFormulaManipulator.getAtomContainer( formula );
        for(IAtom atom : residue.atoms())
            atom.setHydrogenCount( 0 );
        startStructure.add( residue );
	    if (generatorID != null) {
	      if (StructureGeneratorSettingsPage.generatorName
	          .equals(generatorID)) {
  	        job = new StochasticStructureElucidationJob( 
  	                  startStructure, 
  	                  Integer.parseInt( 
  	                      jobSpec.getGeneratorSetting( 
  	                          StructureGeneratorSettingsPage.generatorName, 
  	                          "numberSteps")));
	      } else if (StructureGeneratorSettingsPage.generatorNameUserConfigurable
                .equals(generatorID)) {
            job = new UserConfigurableStochasticStructureElucidationJob( 
                      startStructure);
        } 
	      else if (("org.openscience.cdk.structgen.deterministic." +
	      		     "GENMDeterministicGenerator").equals(generatorID)) {
  	        //job = new DeterministicStructureElucidationJob(jobSpec
  	        //    .getJobTitle());
	      }else if(StructureGeneratorSettingsPage.
	              gaGeneratorName.equals( generatorID )){
            job = new GAStructureElucidationJob( 
                      jobSpec.getJobTitle(),  
                      startStructure);
	      } else {
	          throw new BioclipseException("The generator type '" + generatorID
	                                       + "' is not recognized.");
	      }
        job.setJobSpecification( jobSpec );
        job.setDetectAromaticity(jobSpec.getDetectAromaticity());
        //Add judges
        Iterator<String> judgeIDs = jobSpec.getJudges().iterator();
        while (judgeIDs.hasNext()) {
          String judgeID = judgeIDs.next();
          Iterator<IJudge> judges = Activator.getDefault()
              .getJudgeExtensions().iterator();

          while (judges.hasNext()) {
            IJudge factory = judges.next();
            if (factory.getClass().getName().equals(judgeID)) {
              try {
                IJudge judge 
                    = factory.createJudge(
                          new Path( jobSpec.getJobDirectory()
                                           .getFullPath().toOSString()
                                    + File.separator 
                                    + jobSpec.getJudgesData().get( judgeID)));
                judge.setEnabled( true );
                if(judge.isLabelling())
                	judge.labelStartStructure(startStructure);
                job.addJudge( judge );
              } catch (MissingInformationException e) {
                  throw new BioclipseException(e.getMessage(),e);
              }
            }
          }
        }
        job.addScoreImprovedListener( new MyScoreImprovedListener(returner) );
        if(temperaturelistener!=null)
            job.addTemperatureAndScoreListener(temperaturelistener);
        job.run( monitor);
        finishlistener.finished();
	 	  }else{
	      throw new BioclipseException("No generator specified");
	    }
	}
	
	private class MyScoreImprovedListener implements IScoreImprovedListener{

	      IReturner<IMolecule> returner=null;
	      
	      public MyScoreImprovedListener(IReturner<IMolecule> returner){
	          this.returner = returner;
	      }
        public void betterScore( org.openscience.cdk.interfaces.IMolecule mol ) {
            returner.partialReturn( new CDKMolecule(mol) );
        }
	    
	}
	
	public boolean validateJob(SenecaJobSpecification sjs){
	    boolean canFinish =  checkForRequiredData(sjs);
      if (canFinish)
        canFinish = canFinish && configureEngine(sjs);
      if (canFinish)
        canFinish = canFinish && checkJudges(sjs);
      return canFinish;
	}
	
	 private boolean checkJudges(SenecaJobSpecification jobSpec) {
	     Iterator<String> judgeIDs = jobSpec.getJudges().iterator();
	     if(!judgeIDs.hasNext())
	         return false;
	     boolean ok=true;
	     while (judgeIDs.hasNext()) {
	       String judgeID = judgeIDs.next();
	       Iterator<IJudge> judges = Activator.getDefault()
	           .getJudgeExtensions().iterator();
	       while (judges.hasNext()) {
	         IJudge factory = judges.next();
	         if (factory.getClass().getName().equals(judgeID)) {
	             if(jobSpec.getJudgesData().get( judgeID)!=null)
	                 ok = ok && factory.checkJudge(jobSpec.getJobDirectory().getFullPath().toOSString()+File.separator+jobSpec.getJudgesData().get( judgeID));
	             else
	                 ok=false;
	         }
	       }
	     }
	     return ok;
	   }
	
  private boolean checkForRequiredData(SenecaJobSpecification jobSpec) {
      boolean result = true;

      if (jobSpec.getMolecularFormula().length() > 0) {
        result = result && true;
      } else {
        result = result && false;
      }
      return result;
  }

  private boolean configureEngine(SenecaJobSpecification jobSpec) {
      
      String generatorID = jobSpec.getGenerator();
      if (generatorID != null) {
        if (!StructureGeneratorSettingsPage.gaGeneratorName
            .equals(generatorID) && 
            !StructureGeneratorSettingsPage.generatorName
            .equals(generatorID) &&
            !StructureGeneratorSettingsPage.generatorNameUserConfigurable
            .equals(generatorID) &&  
            !StructureGeneratorSettingsPage.generatorNameDeteministic
            .equals(generatorID)) 
            return false;
        else
            return true;
      }else{
          return false;
      }
  }
  
  public void addFinishListener(IFinishListener listener){
      finishlistener=listener;
  }
  public void addTempeatureAndScoreListener(TemperatureAndScoreListener listener){
      temperaturelistener=listener;
  }
}
