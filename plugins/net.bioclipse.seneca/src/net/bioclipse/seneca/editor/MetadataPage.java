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

import net.bioclipse.seneca.domain.SenecaJobSpecification;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class MetadataPage extends FormPage implements IDirtyablePage {

	private Text mfData;
	private Text jobTitle;
	private Text[] dataFile = new Text[4];
	public boolean isDirty = false;
	private Button detectAromaticity;

	public MetadataPage(FormEditor editor) {
		super(editor, "metadata", "metadata");
	}

	protected void createFormContent(IManagedForm managedForm) {

		// set up the resource
		SenecaJobSpecification specification = ((SenecaJobEditor) this
				.getEditor()).getSpecification();

		final ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("General Information");
		GridLayout layout = new GridLayout(3, false);
		form.getBody().setLayout(layout);

		toolkit.createLabel(form.getBody(), "Job Title:");
		jobTitle = toolkit.createText(form.getBody(), specification
				.getJobTitle(), SWT.BORDER);
		GridData gData = new GridData(GridData.FILL_HORIZONTAL);
		gData.horizontalSpan = 2;
		jobTitle.setLayoutData(gData);
		jobTitle.addModifyListener(new EditorModifyListener(this));

		toolkit.createLabel(form.getBody(), "Molecular Formula:");
		mfData = toolkit.createText(form.getBody(), specification
				.getMolecularFormula(), SWT.BORDER);
		mfData.setLayoutData(gData);
		mfData.addModifyListener(new EditorModifyListener(this));

		toolkit.createLabel(form.getBody(), "Carbon hydrogen count:");
		for(int i=0;i<4;i++){
		  toolkit.createLabel(form.getBody(), "CH"+i, SWT.BORDER);
    	dataFile[i] = toolkit.createText(form.getBody(), Integer
    	    .toString( specification.getDeptData( i ) ), SWT.BORDER);
    	gData = new GridData(SWT.LEFT);
    	gData.horizontalSpan = 1;
    	dataFile[i].setLayoutData(gData);
    	dataFile[i].addModifyListener(new EditorModifyListener(this));
    	if(i<4-1)
    	    toolkit.createComposite( form.getBody() );
    }
    toolkit.createLabel(form.getBody(), "Do aromaticity detection during run");
    detectAromaticity = toolkit.createButton( form.getBody(), "", SWT.CHECK );
    detectAromaticity.addSelectionListener( new SelectionListener(){

        public void widgetDefaultSelected( SelectionEvent e ) {

            setDirty( true );
            
        }

        public void widgetSelected( SelectionEvent e ) {

            setDirty( true );
            
        }
        
    });
    detectAromaticity.setSelection( specification.getDetectAromaticity() );
	}

	public void doSave(IProgressMonitor monitor) {
		SenecaJobSpecification specification =
			((SenecaJobEditor) this.getEditor()).getSpecification();
		if (specification == null) {
			// then the page has not been initialized yet,
			// and content can't have changed.
			return;
		}

		specification.setMolecularFormula(mfData.getText());
		specification.setJobTitle(jobTitle.getText());
		for(int i=0;i<4;i++){
		    specification.setDeptData(i, Integer.parseInt( dataFile[i].getText() ));
		}
		specification.setDetectAromaticity( detectAromaticity.getSelection() );
		this.setDirty(false);
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		this.getManagedForm().dirtyStateChanged();
		this.getEditor().editorDirtyStateChanged();
	}

}
