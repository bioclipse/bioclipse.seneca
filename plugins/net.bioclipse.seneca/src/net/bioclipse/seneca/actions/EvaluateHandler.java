/*******************************************************************************
 * Copyright (c) 2010  Stefan Kuhn <stefan.kuhn@ebi.ac.uk>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org—epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.seneca.actions;

import java.io.File;
import java.util.Iterator;

import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.judge.IJudge;
import net.bioclipse.seneca.judge.MissingInformationException;
import net.bioclipse.spectrum.Activator;
import net.bioclipse.ui.contentlabelproviders.FolderLabelProvider;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A handler for evaluatiing a molecule against the judges in a sjs.
 *
 */
public class EvaluateHandler extends AbstractHandler {

    private static final Logger logger = Logger.getLogger( EvaluateHandler.class );
	protected IStructuredSelection selectedFiles;

    /* (non-Javadoc)
     * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    public Object execute( ExecutionEvent event ) throws ExecutionException {

        ISelection sel =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getSelectionService().getSelection();
        if ( !sel.isEmpty() ) {
            if ( sel instanceof IStructuredSelection ) {
                try {
                    final IStructuredSelection ssel = (IStructuredSelection) sel;
                    
                    
                    final Shell dialog = new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
                    dialog.setText("Choose Seneca Job Specification");
                    GridLayout layout = new GridLayout();
                    dialog.setLayout( layout );
                    dialog.setMinimumSize(300,400);
                    layout.numColumns = 2;
                    layout.verticalSpacing = 9;
                    
            		TreeViewer treeViewer = new TreeViewer(dialog);
            		treeViewer.setContentProvider(new SjsFileContentProvider());
            		treeViewer.setLabelProvider(new DecoratingLabelProvider(
            				new FolderLabelProvider(), PlatformUI.getWorkbench()
            						.getDecoratorManager().getLabelDecorator()));
            		treeViewer.setUseHashlookup(true);

                    // Layout the tree viewer below the text field
                    GridData layoutData = new GridData();
                    layoutData.grabExcessHorizontalSpace = true;
                    layoutData.grabExcessVerticalSpace = true;
                    layoutData.horizontalAlignment = GridData.FILL;
                    layoutData.verticalAlignment = GridData.FILL;
                    layoutData.horizontalSpan = 3;
                    final Button ok = new Button(dialog, SWT.PUSH);
                    
                    treeViewer.getControl().setLayoutData( layoutData );
                    treeViewer.setInput( ResourcesPlugin.getWorkspace().getRoot()
                            .findMember( "." ) );
                    treeViewer.expandToLevel( 2 );
                    treeViewer
                            .addSelectionChangedListener( new ISelectionChangedListener() {

                                public void selectionChanged( SelectionChangedEvent event ) {
                                    ISelection sel = event.getSelection();
                                    if ( sel instanceof IStructuredSelection ) {
                                        selectedFiles = (IStructuredSelection) sel;
                                        try {
                                            if ( selectedFiles.size() == 1
                                            		&& selectedFiles.getFirstElement() instanceof IFile
                                                 && ((IFile)selectedFiles.getFirstElement()).getName().indexOf(".sjs")>-1 )
                                                ok.setEnabled(true);
                                        } catch ( Exception e ) {
                                            LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
                                        }
                                    }
                                }
                            } );
                    treeViewer.setSelection( new StructuredSelection( ResourcesPlugin
                            .getWorkspace().getRoot().findMember( "." ) ) );
                    ok.setText("OK");
                    ok.setEnabled(false);
                    Button cancel = new Button(dialog, SWT.PUSH);
                    cancel.setText("Cancel");
                    Listener listener = new Listener() {
                      public void handleEvent(Event event) {
                        if(event.widget == ok){
							try {
	                        	StringBuffer result = new StringBuffer();
	                        	ICDKMolecule mol = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager().loadMolecule((IFile)ssel.getFirstElement());
	                        	mol = (ICDKMolecule)net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager().removeExplicitHydrogens(mol);
	                        	mol = (ICDKMolecule)net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager().addImplicitHydrogens(mol);
	                        	SenecaJobSpecification sjs = null;
								sjs = net.bioclipse.seneca.Activator.getDefault().getJavaSenecaManager().getJobSpecification(((IFile)selectedFiles.getFirstElement()));
						        //Add judges
						        Iterator<String> judgeIDs = sjs.getJudges().iterator();
						        while (judgeIDs.hasNext()) {
						          String judgeID = judgeIDs.next();
						          Iterator<IJudge> judges = net.bioclipse.seneca.Activator.getDefault()
						              .getJudgeExtensions().iterator();

						          while (judges.hasNext()) {
						            IJudge factory = judges.next();
						            if (factory.getClass().getName().equals(judgeID)) {
						              try {
						                IJudge judge 
						                    = factory.createJudge(
						                          new Path( sjs.getJobDirectory()
						                                           .getFullPath().toOSString()
						                                    + File.separator 
						                                    + sjs.getJudgesData().get( judgeID)));
										if (judge.hasMaxScore()) {
											judge.calcMaxScore();
										}
						                if(judge.isLabelling())
						                	judge.labelStartStructure(mol.getAtomContainer());
						                try {
						                	result.append(judgeID+ ": "+judge.evaluate(mol.getAtomContainer()).score+"/"+judge.getMaxScore()+"\r\n");
										} catch (Exception e) {
											result.append(judgeID+ ": not possible with this structure\r\n");
											e.printStackTrace();
										}
						              } catch (MissingInformationException e) {
						                  throw new BioclipseException(e.getMessage(),e);
						              }
						            }
						          }
						        }
						        MessageDialog.openInformation(dialog, "Scoring Result", result.toString());
							} catch (Exception e) {
								LogUtils.handleException(e, logger, net.bioclipse.seneca.Activator.PLUGIN_ID);
							}
                        }
                        dialog.close();
                      }
                    };
                    ok.addListener(SWT.Selection, listener);
                    cancel.addListener(SWT.Selection, listener);
                    dialog.pack();
                    dialog.open();
                } catch ( Exception ex ) {
                    LogUtils.handleException( ex, logger, Activator.PLUGIN_ID );
                }
            }
        }
        return null;
    }
}
