/*******************************************************************************
 * Copyright (c) 2007 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Egon Willighagen - core API and implementation
 *******************************************************************************/
package net.bioclipse.seneca.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.bioclipse.cdk.domain.CDKMolecule;
import net.bioclipse.chemoinformatics.wizards.WizardHelper;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.jobs.BioclipseJobUpdateHook;
import net.bioclipse.jobs.BioclipseUIJob;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.anneal.TemperatureListener;
import net.bioclipse.seneca.business.IJavaSenecaManager;
import net.bioclipse.seneca.business.ISenecaManager;
import net.bioclipse.seneca.business.IFinishListener;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.views.BestStructureView;
import net.bioclipse.seneca.wizard.CheckJobWizard;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openscience.cdk.DefaultChemObjectBuilder;

public class SenecaJobEditor extends FormEditor implements IFinishListener, TemperatureAndScoreListener {

	private FormPage page1;
	private FormPage page2;
	private FormPage page3;
	private SenecaJobSpecification specification;
	private IFile inputFile;
	private static Logger logger = Logger.getLogger(SenecaJobEditor.class);
  final List<IMolecule> result = new ArrayList<IMolecule>();


	 public void init(IEditorSite site, IEditorInput editorInput)
	    throws PartInitException {
		 if (!(editorInput instanceof IFileEditorInput))
			 throw new PartInitException(
					 "Invalid Input: Must be IFileEditorInput");
		 super.init(site, editorInput);
		 Object file = editorInput.getAdapter(IFile.class);
		 if (!(file instanceof IFile)) {
			 throw new PartInitException(
					 "Invalid editor input: Does not provide an IFile");
		 }

		 this.inputFile = (IFile) file;
		 ISenecaManager manager =
				(ISenecaManager)Activator.getDefault().getJavaSenecaManager();
		 try {
            this.specification = manager.getJobSpecification(inputFile);
        } catch ( BioclipseException e ) {
            throw new PartInitException("Problems reading file",e);
        }
		 setPartName( inputFile.getName() );
	 }

	 
    public IFile getInputFile() {
    
        return inputFile;
    }

    public SenecaJobSpecification getSpecification() {
		 if (this.specification == null) {
			 System.err.println("specification null");
		 }
		 return this.specification;
	 }

	protected void addPages() {
		try {
			page1 = new MetadataPage(this);
			addPage(page1);
			page2 = new StructureGeneratorSettingsPage(this);
			addPage(page2);
			page3 = new JudgePage(this);
			addPage(page3);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public void doSave(IProgressMonitor monitor) {
		page1.doSave(monitor);
		page2.doSave(monitor);
		page3.doSave(monitor);

		ISenecaManager manager =
			(ISenecaManager)Activator.getDefault().getJavaSenecaManager();
		try {
            manager.saveSenecaJob(this.inputFile, this.specification);
    } catch ( CoreException e ) {
        LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
    }

		this.editorDirtyStateChanged();
	}

	public void doSaveAs() {

	}

	public boolean isSaveAsAllowed() {
		return false;
	}


    public void runJob() {
        //we do a save first since this updates the job specification
        doSave( new NullProgressMonitor() );
        boolean valid = Activator.getDefault().getJavaSenecaManager()
                .validateJob( specification );
        if(valid){
            if(specification.getGenerator().equals( StructureGeneratorSettingsPage.generatorName )){
                BestStructureView view = (BestStructureView)getSite().getPage().findView(BestStructureView.ID);
                if(view!=null)
                	view.setMaxSteps(Integer.parseInt( 
                                 specification.getGeneratorSetting( 
                                     StructureGeneratorSettingsPage.generatorName, 
                                     "numberSteps")));
            }
            IJavaSenecaManager man = Activator.getDefault().getJavaSenecaManager();
            man.addFinishListener( this );
            man.addTempeatureAndScoreListener( this );
            BestStructureView view = (BestStructureView)getSite().getPage().findView(BestStructureView.ID);
            if(view!=null)
                view.reset();
            man.executeJob( specification,
            new BioclipseJobUpdateHook<IMolecule>(specification.getJobTitle()){
                public void partialReturn( IMolecule chunk ) {
                    result.add( chunk );
                    BestStructureView view = (BestStructureView)getSite().getPage().findView(BestStructureView.ID);
                    if(view!=null)
                        view.setBestStructure( DefaultChemObjectBuilder.getInstance().newMolecule( ((CDKMolecule) chunk ).getAtomContainer() ));
                }
            });
        }else{
            CheckJobWizard predwiz=new CheckJobWizard(specification);
            WizardDialog wd=new WizardDialog(new Shell(),predwiz);
            wd.open();
        }
    }
    
    private void saveSDFile(List<IMolecule> mols, String jobTitle) throws BioclipseException, InvocationTargetException {
          // get an sdf file 
        IStructuredSelection virtualselection = 
            new StructuredSelection(
                    net.bioclipse.core.Activator.getVirtualProject());
        String filename = WizardHelper.
                            findUnusedFileName(
                                virtualselection, "Seneca_" + jobTitle, ".sdf");
          final IFile sdfile = 
              net.bioclipse.core.Activator.getVirtualProject().getFile(filename);
          
          // save the molecules to the file
          net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager()
          .saveSDFile( sdfile, mols, new BioclipseUIJob<Void>() {
              @Override
              public void runInUI() {
                  net.bioclipse.ui.business.Activator
                     .getDefault().getUIManager().open( sdfile );
              }
              
          });
    }


    public void finished() {
        try {
            saveSDFile( result, specification.getJobTitle() );
        } catch ( Exception e ) {
            LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
        }
    }


    public void change( double temp , double score) {
        BestStructureView view = (BestStructureView)getSite().getPage().findView(BestStructureView.ID);
        if(view!=null)
            view.addTemperatureValue( temp, score );
    }

}

