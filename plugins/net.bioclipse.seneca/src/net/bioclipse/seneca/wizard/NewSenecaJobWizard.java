/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Egon Willighagen - core API and implementation
 *******************************************************************************/
package net.bioclipse.seneca.wizard;

import net.bioclipse.chemoinformatics.wizards.WizardHelper;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.business.ISenecaManager;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewSenecaJobWizard extends Wizard implements INewWizard {

    private static Logger logger = Logger.getLogger(NewSenecaJobWizard.class);
    private WizardNewFileCreationPage selectFilePage;
    public static final String ID = "net.bioclipse.plugins.wizards.NewSenecaJobResourceWizard";
    /**
     * Constructor for JCPWizard.
     */
    public NewSenecaJobWizard() {
      super();
      setWindowTitle("Create a new structure elucidation job");
      setNeedsProgressMonitor(true);
    }
	
	 /**
   * Adding the pages to the wizard.
   */

  public void addPages() {
      ISelection sel=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
      if(sel instanceof IStructuredSelection){
          selectFilePage = new WizardNewFileCreationPage("newfile",(IStructuredSelection) sel);
          selectFilePage.setFileName(WizardHelper.findUnusedFileName((IStructuredSelection)sel, "unnamed", ""));
      }else{
          selectFilePage = new WizardNewFileCreationPage("newfile",StructuredSelection.EMPTY);
      }        
      selectFilePage.setTitle("Choose name and location for new CASE project");
      selectFilePage.setDescription("Extension will be set to .sjs");
      addPage(selectFilePage);
  }

	public boolean performFinish() {
		// make a default job specification
		ISenecaManager manager =
			(ISenecaManager)Activator.getDefault().getJavaSenecaManager();
		try {
            return manager.createSenecaJob((IContainer)ResourcesPlugin.getWorkspace().getRoot().findMember( selectFilePage.getContainerFullPath()),selectFilePage.getFileName());
        } catch ( Exception e ) {
            LogUtils.handleException( e, logger );
            return false;
        }
	}

  public void init( IWorkbench workbench, IStructuredSelection selection ) {
  }
}
