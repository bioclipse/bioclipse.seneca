package net.bioclipse.seneca.wizard;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.editor.StructureGeneratorSettingsPage;
import net.bioclipse.seneca.judge.IJudge;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Wizard page that checks the SJS input.
 *
 * @author egonw
 */
public class SJSCheckWizardPage extends WizardPage implements
 	IWizardPage {

 private CheckJobWizard wizard;

 private SenecaJobSpecification jobSpec;

 private Tree tree;
 private Map<TreeItem, CheckItem> checksMap;

 public SJSCheckWizardPage(CheckJobWizard parent, SenecaJobSpecification specification) {
 	super("Problems in your job specification", "The following problems exist with your job specifictation", null);
 	this.wizard = parent;
 	this.jobSpec = specification;
 	checksMap = new HashMap<TreeItem, CheckItem>();
 	setPageComplete(false);
 }

 public void createControl(Composite parent) {
 	final Composite control = new Composite(parent, SWT.NONE);
 	GridLayout gridLayout = new GridLayout();
 	gridLayout.numColumns = 2;
 	control.setLayout(gridLayout);

 	tree = new Tree(control, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
 	GridData gridData = new GridData(GridData.FILL_BOTH);
 	gridData.horizontalSpan = 2;
 	gridData.heightHint = 200;
 	tree.setLayoutData(gridData);
 	tree.setHeaderVisible(true);
 	TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
 	column1.setText("Task");
 	column1.setWidth(300);
 	TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
 	column2.setText("State");
 	column2.setWidth(50);

 	// add a listener
 	tree.addSelectionListener(new SelectionAdapter() {
 		public void widgetSelected(SelectionEvent e) {
 			CheckItem item = checksMap.get((TreeItem) e.item);
 			if (item != null && item.getError() != null) {
 				setErrorMessage(item.getError());
 			} else {
 				setErrorMessage(null);
 			}
 		}

 		public void widgetDefaultSelected(SelectionEvent e) {
 			System.out.println("default selected: "
 					+ ((TreeItem) e.item).getText());
 		}
	});
 	setControl(control);
  checkForRequiredData(tree);
  checkJudges(tree);
  configureEngine(tree);
  setErrorMessage(null);
  setPageComplete(true);
 }

 private void checkJudges( Tree parent ) {
     CheckItem item = new CheckItem(parent, "Checking judges...", "");
     Iterator<String> judgeIDs = jobSpec.getJudges().iterator();
     if(!judgeIDs.hasNext()){
         CheckItem generatorItem = new CheckItem(item, "Judge count", "checking");
         generatorItem.setState("failed");
         generatorItem.setError("No judge is specified. At least one is needed.");
         item.setExpanded(true);
         item.setState("failed");
     }
         
     while (judgeIDs.hasNext()) {
       String judgeID = judgeIDs.next();
       Iterator<IJudge> judges = Activator.getDefault()
           .getJudgeExtensions().iterator();
       while (judges.hasNext()) {
         IJudge factory = judges.next();
         if (factory.getClass().getName().equals(judgeID)) {
             if(jobSpec.getJudgesData().get( judgeID)!=null){
                 //TODO output message
                 factory.checkJudge(jobSpec.getJobDirectory().getFullPath().toOSString()+File.separator+jobSpec.getJudgesData().get( judgeID));
             }else{
                 CheckItem generatorItem = new CheckItem(item, "Judge "+judgeID, "checking");
                 generatorItem.setState("failed");
                 generatorItem.setError("Does not have data");
                 item.setExpanded(true);
                 item.setState("failed");                 
             }
         }
       }
     }
   }


 public boolean canFlipToNextPage() {
 	return false;
 }


 private void checkForRequiredData(Tree parent) {
     CheckItem item = new CheckItem(parent, "Checking formula data...", "");

     //TODO check formula
     if (jobSpec.getMolecularFormula().length() > 0 ) {
       item.setState( "valid" );
     } else {
       item.setState( "no" );
       item.setError( "No valid formula given" );
       item.setExpanded( true );
     }
 }


 private void configureEngine(Tree parent) {
     CheckItem item = new CheckItem(parent, "Checking generators...", "");
     
     String generatorID = jobSpec.getGenerator();
     if (generatorID == null || (!StructureGeneratorSettingsPage.gaGeneratorName
           .equals(generatorID) && !StructureGeneratorSettingsPage.generatorName
           .equals(generatorID) && !StructureGeneratorSettingsPage.generatorNameDeteministic
           .equals(generatorID) && !StructureGeneratorSettingsPage.generatorNameUserConfigurable
           .equals(generatorID))) {
           CheckItem generatorItem = new CheckItem(item, "Valid generators", "checking");
           generatorItem.setState("failed");
           generatorItem.setError("No valid generator, must be one from the generator page");
           item.setExpanded(true);
           item.setState("failed");
           return;
     }
     item.setState("valid");
 }

 /**
  * Should really be a subclass of TreeItem, as it functions as such, but
  * that is not allowed.
  *
  * @author egonw
  */
 class CheckItem {
 	private CheckItem parent;
 	private String taskText;
 	private TreeItem treeItem;
 	private String errorMessage = null;

 	public CheckItem(CheckItem item, String taskText, String taskState) {
 		this(new TreeItem(item.treeItem, SWT.NONE), taskText, taskState);
 		this.parent = item;
 	}

 	public CheckItem(Tree tree, String taskText, String taskState) {
 		this(new TreeItem(tree, SWT.NONE), taskText, taskState);
 		parent = null;
 	}

 	private CheckItem(TreeItem item, String taskText, String taskState) {
 		this.treeItem = item;
 		this.treeItem.setText(new String[] { taskText, taskState });
 		this.taskText = taskText;
 		checksMap.put(this.treeItem, this);
 	}

 	public void setState(String taskState) {
 		this.treeItem.setText(new String[] { taskText, taskState });
 		if ("failed".equals(taskState) && parent != null) {
 			parent.setState(taskState);
 		}
 	}

 	public void setExpanded(boolean bool) {
 		this.treeItem.setExpanded(bool);
 		if (parent != null)
 			parent.setExpanded(bool);
 	}

 	public void setError(String errorMessage) {
 		this.errorMessage = errorMessage;
 	}

 	public String getError() {
 		return this.errorMessage;
 	}
 }

}