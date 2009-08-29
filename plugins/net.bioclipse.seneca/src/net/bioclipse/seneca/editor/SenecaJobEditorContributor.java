/*****************************************************************************
 * Copyright (c) 2008 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *****************************************************************************/
package net.bioclipse.seneca.editor;

import net.bioclipse.seneca.Activator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

public class SenecaJobEditorContributor extends
		MultiPageEditorActionBarContributor {
	
	private IAction runAction;
	
	public SenecaJobEditorContributor() {
		super();
    runAction=new Action(){
        @Override
        public void run() {

          ((SenecaJobEditor)getActiveEditorPart()).runJob();
          
        }
      };
      runAction.setText("Run Seneca Job");
      runAction.setToolTipText("Run Seneca Job");
      runAction.setImageDescriptor(Activator.getImageDescriptor("icons/play.gif"));
	}
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Separator());
		toolBarManager.add(runAction);
		super.contributeToToolBar(toolBarManager);
	}


	private IEditorPart activeEditorPart;

	@Override
	public void setActivePage(IEditorPart activeEditor) {
	}
	
	public void contributeToMenu(IMenuManager manager) {
		super.contributeToMenu(manager);
	}
	
	public IEditorPart getActiveEditorPart() {
		return activeEditorPart;
	}
	
	public void setActiveEditor(IEditorPart part) {
		if (!(activeEditorPart == part)) {
			this.activeEditorPart = part;
		}
	}
}

