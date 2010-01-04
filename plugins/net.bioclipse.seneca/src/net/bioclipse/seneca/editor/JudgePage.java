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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.judge.IJudge;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class JudgePage extends FormPage implements IDirtyablePage {

	public boolean isDirty = false;
	private static Logger logger = Logger.getLogger(JudgePage.class);
	private SenecaJobEditor senecaeditor;
	private SenecaJobSpecification specification;
	private final Map<String,IJudge> judges = new HashMap<String,IJudge>();

	public JudgePage(FormEditor editor) {
		super(editor, "judges", "Scoring Functions");
		this.senecaeditor = (SenecaJobEditor)editor;
	}

	protected void createFormContent(IManagedForm managedForm) {

		// set up the resource
		specification =
			((SenecaJobEditor)this.getEditor()).getSpecification();

		final ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Judge Selection");
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		form.getBody().setLayout(layout);

		Iterator<IJudge> judgeFactories =
			Activator.getDefault().getJudgeExtensions().iterator();
		while (judgeFactories.hasNext()) {
			final IJudge factory = judgeFactories.next();
			Section section = toolkit.createSection(form.getBody(),
				Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
				Section.EXPANDED
			);
			TableWrapData td = new TableWrapData(TableWrapData.FILL);
			td.colspan = 2;
			section.setLayoutData(td);
			section.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					form.reflow(true);
				}
			});
			String judgeName = factory.getClass().getName();
			factory.setEnabled( specification.getJudgeEnabled(judgeName) );
			factory.setData( specification.getJudgesData().get( judgeName ) );
			judges.put( factory.getName(), factory );
			section.setText(factory.getName());
			section.setToolTipText(factory.getName());
			Composite sectionClient = toolkit.createComposite(section);
			GridData layoutData = new GridData(SWT.LEFT, SWT.BOTTOM,true,true);
			layoutData.widthHint=500;
			sectionClient.setLayoutData( layoutData );
			sectionClient.setLayout(new GridLayout());
			Label description = toolkit.createLabel( sectionClient, factory.getDescription());
			final Button button = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
			button.addSelectionListener(new EnableJudgeListener(factory));
			button.setSelection(specification.getJudgeEnabled(judgeName));
			Label spinnerlabel = toolkit.createLabel( sectionClient, "Weight:");
			final Spinner weightSpinner = new Spinner(sectionClient, SWT.NONE);
			weightSpinner.setMinimum(0);
			weightSpinner.setIncrement(1);
			weightSpinner.setMaximum(100);
			weightSpinner.setSelection(specification.getWeight(judgeName));
			weightSpinner.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e) {
					judges.get(button.getParent().getParent().getToolTipText()).setWeight( Integer.parseInt(weightSpinner.getText()) );
					setDirty(true);
				}
			});
			toolkit.adapt(weightSpinner);
			final Label filelabel = toolkit.createLabel( sectionClient,"Drop spectrum file here                ");
			button.addSelectionListener( new SelectionListener(){
		          public void widgetDefaultSelected( SelectionEvent e ) {
		              filelabel.setEnabled( button.getSelection() );
		              filelabel.setText( "Drop spectrum file here                " );
		              judges.get(button.getParent().getParent().getToolTipText()).setEnabled( button.getSelection() );
		          }
		          public void widgetSelected( SelectionEvent e ) {
		              filelabel.setEnabled( button.getSelection() );
		              filelabel.setText( "Drop spectrum file here                " );
		              judges.get(button.getParent().getParent().getToolTipText()).setEnabled( button.getSelection() );
		          }
		      });
		      filelabel.setLayoutData( layoutData );
		      if(!button.getSelection())
		          filelabel.setEnabled( false );
		      if(specification.getJudgesData().get( judgeName )!=null)
		          filelabel.setText( "Data file: "+specification.getJudgesData().get( judgeName ) );
		      // Create the drop target
		      DropTarget target = new DropTarget(filelabel, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
		      Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance()};
		      target.setTransfer(types);
		      target.addDropListener(new DropTargetAdapter() {
		        public void dragEnter(DropTargetEvent event) {
		          if (event.detail == DND.DROP_DEFAULT) {
		            event.detail = (event.operations & DND.DROP_COPY) != 0 ? DND.DROP_COPY : DND.DROP_NONE;
		          }
		        }
		
		        public void dragOver(DropTargetEvent event) {
		           event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
		        }
		        public void drop(DropTargetEvent event) {
		            DropTarget target = (DropTarget) event.widget;
		            Label label = (Label) target.getControl();
		            Object data =  event.data;
		            if (data instanceof IStructuredSelection){
		                IJudge judge = judges.get(label.getParent().getParent().getToolTipText());
		                IFile processedFile = judge.setData( (IStructuredSelection) data, senecaeditor.getInputFile());
		                if(processedFile!=null){
		                  label.setText( "Data file: "+processedFile.getName() );
		                  factory.setData( new Path(processedFile.getName()) );
		                  setDirty(true);
		                }
		            }
		            label.redraw();
		        }
		      });
			section.setClient(sectionClient);
		}
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		this.getManagedForm().dirtyStateChanged();
		this.getEditor().editorDirtyStateChanged();
	}

	public void doSave(IProgressMonitor monitor) {
		if (specification == null) {
			// then the page has not been initialised yet,
			// and content can't have been changed.
			return;
		}

		Iterator<IJudge> judgeFactories =
			Activator.getDefault().getJudgeExtensions().iterator();
		while (judgeFactories.hasNext()) {
		  IJudge judge = judges.get(judgeFactories.next().getName());
			specification.setJudgeEnabled(
					judge.getClass().getName(), judge.getEnabled());
			specification.setWeight(
					judge.getClass().getName(), judge.getWeight());
			if(judge.getEnabled() && judge.getData()!=null)			    
			    specification.setJudgeData( judge.getClass().getName(), judge.getData().toFile().getName() );
			else
				specification.setJudgeEnabled(judge.getClass().getName(),false);
		}

		this.setDirty(false);
	}

	class EnableJudgeListener implements SelectionListener {
		IJudge judge;

		EnableJudgeListener(IJudge factory) {
			judge = factory;
		}

		public void widgetDefaultSelected(SelectionEvent e) {}

		public void widgetSelected(SelectionEvent e) {
			judge.setEnabled(((Button)e.getSource()).getSelection());
			setDirty(true);
		}
	}
}
