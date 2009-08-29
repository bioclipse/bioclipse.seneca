/*******************************************************************************
 * Copyright (c) 2009 Stefan Kuhn <stefan.kuhn@ebi.ac.uk>
 *               
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package net.bioclipse.seneca;

import net.bioclipse.seneca.views.BestStructureView;
import net.bioclipse.seneca.wizard.NewSenecaJobWizard;
import net.bioclipse.spectrum.views.SpectrumCompareView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * The Seneca perspective.
 * 
 * @author shk3
 */
public class SenecaPerspective implements IPerspectiveFactory {

	IPageLayout storedLayout;

	public static final String ID_PERSPECTIVE =
		"net.bioclipse.seneca.SenecaPerspective";

    public static final String ID_NAVIGATOR =
        "net.bioclipse.navigator";

	public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.setEditorAreaVisible(true);
        layout.setFixed(false);
        layout.addPerspectiveShortcut(ID_PERSPECTIVE);

        //Add layouts for views
        IFolderLayout left_folder_layout =
            layout.createFolder(
                    "explorer",
                    IPageLayout.LEFT,
                    0.20f,
                    editorArea);

        IFolderLayout right_folder_layout =
            layout.createFolder(
                    "outline",
                    IPageLayout.RIGHT,
                    0.70f,
                    editorArea);

        IFolderLayout bottom_folder_layout =
            layout.createFolder(
                    "properties",
                    IPageLayout.BOTTOM,
                    0.70f,
                    editorArea);

        //Add views
        left_folder_layout.addView(ID_NAVIGATOR);
        bottom_folder_layout.addView(IPageLayout.ID_PROP_SHEET);
        bottom_folder_layout.addView(IPageLayout.ID_PROGRESS_VIEW);
        right_folder_layout.addView(IPageLayout.ID_OUTLINE);
        right_folder_layout.addView(BestStructureView.ID);

        // the below seems redundant, and actually done via the plugin.xml

        //Add NewWizards shortcuts
        layout.addNewWizardShortcut(NewSenecaJobWizard.ID);

        //Add ShowView shortcuts
        layout.addShowViewShortcut(ID_NAVIGATOR);
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}

	public IPageLayout getStoredLayout() {
		return storedLayout;
	}

}
