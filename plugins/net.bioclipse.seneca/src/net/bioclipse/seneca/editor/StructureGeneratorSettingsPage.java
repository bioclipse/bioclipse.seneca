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

import net.bioclipse.seneca.anneal.AdaptiveAnnealingEngine;
import net.bioclipse.seneca.domain.SenecaJobSpecification;
import net.bioclipse.seneca.structgen.ConvergenceAnnealingEngine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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

public class StructureGeneratorSettingsPage extends FormPage implements IDirtyablePage {

	public boolean isDirty = false;

	// items for the pubchem generator
	private Text classNamePubchem;
	private Button enabledPubchem;

	// items for the deterministic generator
	private Text classNameDeter;
	private Button enabledDeter;

	// items for the adaptive annealing stochastic generator
	private Text classNameStoch;
	private Button enabledStoch;

	 // items for the stochastic generator
  private Text classNameStochUserSettings;
  private Button enabledStochUserSettings;
  
  // items for the ga stochastic generator
  private Text classNameGA;
  private Button enabledGA;
  
  public final static long MAXPLATEAUSTEPS = 1500;
	public final static long MAXUPHILLSTEPS = 150;
	public final static long CONVERGENCESTOPCOUNT = 4500;
	public final static double COOLINGRATE = 0.95;
	public final static int INITIALIZATIONCYCLES = 200;

	// items for the annealing engine settings
	private Text numberOfSteps;
	
  // items for the configurabel annealing engine settings
  private Text acceptanceProb;
  private Text initCycles;
  private Text coolingRate;
  private Text convergenceStopCount;
  private Text maxUphillSteps;
  private Text maxPlateauSteps;

  //the generators
  public static final String generatorName = "org.openscience.cdk.structgen.RandomGenerator";
  public static final String generatorNameUserConfigurable = "org.openscience.cdk.structgen.UserConfigurableRandomGenerator";
  public static final String gaGeneratorName = "org.openscience.seneca.job.GAStructureElucidationJob";
  public static final String generatorNameDeteministic = "org.openscience.cdk.structgen.deterministic.GENMDeterministicGenerator";
  public static final String generatorNamePubchem = "org.openscience.cdk.structgen.PubchemDeterministicGenerator";

	public StructureGeneratorSettingsPage(FormEditor editor) {
		super(editor, "structgen", "Generators");
	}

	protected void createFormContent(IManagedForm managedForm) {

		// set up the specification
		SenecaJobSpecification specification =
			((SenecaJobEditor)this.getEditor()).getSpecification();

		final ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText("Structure Generator Settings");
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		form.getBody().setLayout(layout);

		// no extension point for the two structure generators

		// The pubchem generator
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
		section.setText("PubChem Generator");
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		toolkit.createLabel(sectionClient, "Java Class:");
		this.classNamePubchem = toolkit.createText(sectionClient, generatorNamePubchem);
		this.classNamePubchem.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.classNamePubchem.setEditable(false);
		this.enabledPubchem = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
		this.enabledPubchem.addSelectionListener(new EnableGeneratorListener());
		this.enabledPubchem.setSelection(specification.getGeneratorEnabled(generatorNamePubchem));
		section.setClient(sectionClient);

		// The deterministic generator
		section = toolkit.createSection(form.getBody(),
			Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
			Section.EXPANDED
		);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		section.setText("Deterministic Generator");
		sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		toolkit.createLabel(sectionClient, "Java Class:");
		this.classNameDeter = toolkit.createText(sectionClient, generatorNameDeteministic);
		this.classNameDeter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.classNameDeter.setEditable(false);
		this.enabledDeter = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
		this.enabledDeter.addSelectionListener(new EnableGeneratorListener());
		this.enabledDeter.setSelection(specification.getGeneratorEnabled(generatorNameDeteministic));
		section.setClient(sectionClient);

		// The stochastic generator
		section = toolkit.createSection(form.getBody(),
			Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
			Section.EXPANDED
		);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		section.setText("Stochastic Generator");
		sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		toolkit.createLabel(sectionClient, "Java Class:");
		this.classNameStoch = toolkit.createText(sectionClient, generatorName);
		this.classNameStoch.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		this.classNameStoch.setEditable(false);
		this.enabledStoch = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
		this.enabledStoch.addSelectionListener(new EnableGeneratorListener());
		this.enabledStoch.setSelection(specification.getGeneratorEnabled(generatorName));
		section.setClient(sectionClient);

		// should add more options here
		Section settingsSection = toolkit.createSection(form.getBody(),
			Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
			Section.EXPANDED
		);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = 2;
		settingsSection.setLayoutData(td);
		settingsSection.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		settingsSection.setText("Annealing Engine Settings");
		Composite settingsSectionClient = toolkit.createComposite(settingsSection);
		settingsSectionClient.setLayout(new GridLayout());
		this.numberOfSteps = createSettingField(toolkit, settingsSectionClient, this.numberOfSteps,
			"Number of Steps :", generatorName, "numberSteps",
			Integer.toString(AdaptiveAnnealingEngine.DEFAULT_EVALSMAX)
		);
		Button resetParamsButton = toolkit.createButton(settingsSectionClient, "Reset", SWT.PUSH);
		resetParamsButton.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(SelectionEvent e) {
		        numberOfSteps.setText(Integer.toString(AdaptiveAnnealingEngine.DEFAULT_EVALSMAX));
		    }
		});

		settingsSection.setClient(settingsSectionClient);

    // The stochastic generator
    section = toolkit.createSection(form.getBody(),
      Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
      Section.EXPANDED
    );
    td = new TableWrapData(TableWrapData.FILL);
    td.colspan = 2;
    section.setLayoutData(td);
    section.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        form.reflow(true);
      }
    });
    section.setText("Stochastic Generator (user configurable)");
    sectionClient = toolkit.createComposite(section);
    sectionClient.setLayout(new GridLayout());
    toolkit.createLabel(sectionClient, "Java Class:");
    this.classNameStochUserSettings = toolkit.createText(sectionClient, generatorNameUserConfigurable);
    this.classNameStochUserSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    this.classNameStochUserSettings.setEditable(false);
    this.enabledStochUserSettings = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
    this.enabledStochUserSettings.addSelectionListener(new EnableGeneratorListener());
    this.enabledStochUserSettings.setSelection(specification.getGeneratorEnabled(generatorNameUserConfigurable));
    section.setClient(sectionClient);

    // should add more options here
    settingsSection = toolkit.createSection(form.getBody(),
      Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
      Section.EXPANDED
    );
    td = new TableWrapData(TableWrapData.FILL);
    td.colspan = 2;
    settingsSection.setLayoutData(td);
    settingsSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        form.reflow(true);
      }
    });
    settingsSection.setText("Annealing Engine Settings");
    settingsSectionClient = toolkit.createComposite(settingsSection);
    settingsSectionClient.setLayout(new GridLayout());
    this.acceptanceProb = createSettingField(toolkit, settingsSectionClient, this.acceptanceProb,
      "Acceptance Probability :", generatorNameUserConfigurable, "acceptanceProbability",
      Double.toString(ConvergenceAnnealingEngine.INITIALACCEPTANCEPROBABILITY)
    );
    this.maxPlateauSteps = createSettingField(toolkit, settingsSectionClient, this.maxPlateauSteps,
      "Maximum Plateau Steps :", generatorNameUserConfigurable, "maxPlateauSteps",
      Long.toString(ConvergenceAnnealingEngine.MAXPLATEAUSTEPS)
    );
    this.maxUphillSteps = createSettingField(toolkit, settingsSectionClient, this.maxUphillSteps,
      "Maximum Uphill Steps :", generatorNameUserConfigurable, "maxUphillSteps",
      Long.toString(ConvergenceAnnealingEngine.MAXUPHILLSTEPS)
    );
    this.convergenceStopCount = createSettingField(toolkit, settingsSectionClient, this.convergenceStopCount,
      "Convergence Stop Count :", generatorNameUserConfigurable, "convergenceStopCount",
      Long.toString(ConvergenceAnnealingEngine.CONVERGENCESTOPCOUNT)
    );
    this.coolingRate = createSettingField(toolkit, settingsSectionClient, this.coolingRate,
      "Cooling Rate :", generatorNameUserConfigurable, "coolingRate",
      Double.toString(ConvergenceAnnealingEngine.COOLINGRATE)
    );
    this.initCycles = createSettingField(toolkit, settingsSectionClient, this.initCycles,
      "Number of Initialization Cycles :", generatorNameUserConfigurable, "initializationCycles",
      Integer.toString(ConvergenceAnnealingEngine.INITIALIZATIONCYCLES)
    );
    resetParamsButton = toolkit.createButton(settingsSectionClient, "Reset", SWT.PUSH);
    resetParamsButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
            acceptanceProb.setText(Double.toString(ConvergenceAnnealingEngine.INITIALACCEPTANCEPROBABILITY));
            maxPlateauSteps.setText(Long.toString(ConvergenceAnnealingEngine.MAXPLATEAUSTEPS));
            maxUphillSteps.setText(Long.toString(ConvergenceAnnealingEngine.MAXUPHILLSTEPS));
            convergenceStopCount.setText(Long.toString(ConvergenceAnnealingEngine.CONVERGENCESTOPCOUNT));
            coolingRate.setText(Double.toString(ConvergenceAnnealingEngine.COOLINGRATE));
            initCycles.setText(Integer.toString(ConvergenceAnnealingEngine.INITIALIZATIONCYCLES));
        }
    });

    settingsSection.setClient(settingsSectionClient);

    // The GA generator
    section = toolkit.createSection(form.getBody(),
      Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
      Section.EXPANDED
    );
    td = new TableWrapData(TableWrapData.FILL);
    td.colspan = 2;
    section.setLayoutData(td);
    section.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        form.reflow(true);
      }
    });
    section.setText("Genetic algorithm (GA) generator");
    sectionClient = toolkit.createComposite(section);
    sectionClient.setLayout(new GridLayout());
    toolkit.createLabel(sectionClient, "Java Class:");
    this.classNameGA = toolkit.createText(sectionClient, gaGeneratorName);
    this.classNameGA.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    this.classNameGA.setEditable(false);
    this.enabledGA = toolkit.createButton(sectionClient, "Enable", SWT.CHECK);
    this.enabledGA.addSelectionListener(new EnableGeneratorListener());
    this.enabledGA.setSelection(specification.getGeneratorEnabled(gaGeneratorName));
    section.setClient(sectionClient);

    // should add more options here
    settingsSection = toolkit.createSection(form.getBody(),
      Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE |
      Section.EXPANDED
    );
    td = new TableWrapData(TableWrapData.FILL);
    td.colspan = 2;
    settingsSection.setLayoutData(td);
    settingsSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        form.reflow(true);
      }
    });
    settingsSection.setText("GA Engine Settings");
    settingsSectionClient = toolkit.createComposite(settingsSection);
    settingsSectionClient.setLayout(new GridLayout());
    /*this.acceptanceProb = createSettingField(toolkit, settingsSectionClient, this.acceptanceProb,
      "Acceptance Probability :", generatorName, "acceptanceProbability",
      Double.toString(ConvergenceAnnealingEngine.INITIALACCEPTANCEPROBABILITY)
    );
    this.maxPlateauSteps = createSettingField(toolkit, settingsSectionClient, this.maxPlateauSteps,
      "Maximum Plateau Steps :", generatorName, "maxPlateauSteps",
      Long.toString(ConvergenceAnnealingEngine.MAXPLATEAUSTEPS)
    );
    this.maxUphillSteps = createSettingField(toolkit, settingsSectionClient, this.maxUphillSteps,
      "Maximum Uphill Steps :", generatorName, "maxUphillSteps",
      Long.toString(ConvergenceAnnealingEngine.MAXUPHILLSTEPS)
    );
    this.convergenceStopCount = createSettingField(toolkit, settingsSectionClient, this.convergenceStopCount,
      "Convergence Stop Count :", generatorName, "convergenceStopCount",
      Long.toString(ConvergenceAnnealingEngine.CONVERGENCESTOPCOUNT)
    );
    this.coolingRate = createSettingField(toolkit, settingsSectionClient, this.coolingRate,
      "Cooling Rate :", generatorName, "coolingRate",
      Double.toString(ConvergenceAnnealingEngine.COOLINGRATE)
    );
    this.initCycles = createSettingField(toolkit, settingsSectionClient, this.initCycles,
      "Number of Initialization Cycles :", generatorName, "initializationCycles",
      Integer.toString(ConvergenceAnnealingEngine.INITIALIZATIONCYCLES)
    );*/
    resetParamsButton = toolkit.createButton(settingsSectionClient, "Reset", SWT.PUSH);
    resetParamsButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
            /*acceptanceProb.setText(Double.toString(ConvergenceAnnealingEngine.INITIALACCEPTANCEPROBABILITY));
            maxPlateauSteps.setText(Long.toString(ConvergenceAnnealingEngine.MAXPLATEAUSTEPS));
            maxUphillSteps.setText(Long.toString(ConvergenceAnnealingEngine.MAXUPHILLSTEPS));
            convergenceStopCount.setText(Long.toString(ConvergenceAnnealingEngine.CONVERGENCESTOPCOUNT));
            coolingRate.setText(Double.toString(ConvergenceAnnealingEngine.COOLINGRATE));
            initCycles.setText(Integer.toString(ConvergenceAnnealingEngine.INITIALIZATIONCYCLES));*/
        }
    });

    settingsSection.setClient(settingsSectionClient);
	}

	private Text createSettingField(FormToolkit toolkit, Composite settingsSectionClient,
					Text fieldSWTWidget,
					String label, String generatorName, String field, String defaultValue) {
		SenecaJobSpecification specification =
			((SenecaJobEditor)this.getEditor()).getSpecification();
		toolkit.createLabel(settingsSectionClient, label);
		String value = specification.getGeneratorSetting(generatorName, field);
		fieldSWTWidget = toolkit.createText(settingsSectionClient,
			value != null ? value : defaultValue,
			SWT.BORDER
		);
		GridData gData = new GridData(GridData.FILL_HORIZONTAL);
		gData.horizontalSpan = 1;
		fieldSWTWidget.setLayoutData(gData);
		fieldSWTWidget.addModifyListener(new EditorModifyListener(this));
		return fieldSWTWidget;
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		if(this.getManagedForm()!=null)
		    this.getManagedForm().dirtyStateChanged();
		this.getEditor().editorDirtyStateChanged();
	}

	public void doSave(IProgressMonitor monitor) {
		SenecaJobSpecification specification =
			((SenecaJobEditor)this.getEditor()).getSpecification();
		if (specification == null) {
			// then the page has not been initialized yet, and can't content can't have been
			// changed.
			return;
		}

		//only if page has been shown, ui is acutally built
		if(classNameDeter!=null){
		    specification.setGeneratorEnabled(classNamePubchem.getText(), enabledPubchem.getSelection());

		    specification.setGeneratorEnabled(classNameDeter.getText(), enabledDeter.getSelection());
		    
		    specification.setGeneratorEnabled(classNameStoch.getText(), enabledStoch.getSelection());
    		specification.setGeneratorSetting(generatorName, "numberSteps", numberOfSteps.getText());
    		specification.setGeneratorEnabled(classNameStochUserSettings.getText(), enabledStochUserSettings.getSelection());

	        specification.setGeneratorSetting(generatorNameUserConfigurable, "acceptanceProbability", acceptanceProb.getText());
	        specification.setGeneratorSetting(generatorNameUserConfigurable, "coolingRate", coolingRate.getText());
	        specification.setGeneratorSetting(generatorNameUserConfigurable, "initializationCycles", initCycles.getText());
	        specification.setGeneratorSetting(generatorNameUserConfigurable, "convergenceStopCount", convergenceStopCount.getText());
	        specification.setGeneratorSetting(generatorNameUserConfigurable, "maxPlateauSteps", maxPlateauSteps.getText());
	        specification.setGeneratorSetting(generatorNameUserConfigurable, "maxUphillSteps", maxUphillSteps.getText());
        
	        specification.setGeneratorEnabled( classNameGA.getText(), enabledGA.getSelection() );
		}

		this.setDirty(false);
	}


	class EnableGeneratorListener implements SelectionListener {

		EnableGeneratorListener() {
		}

		public void widgetDefaultSelected(SelectionEvent e) {}

		public void widgetSelected(SelectionEvent e) {
			// make sure only one generator is enabled!
			if (e.getSource() == enabledStoch) {
				enabledPubchem.setSelection(!enabledPubchem.getSelection());
				enabledDeter.setSelection(!enabledStoch.getSelection());
				enabledStochUserSettings.setSelection(!enabledStoch.getSelection());
				enabledGA.setSelection(!enabledStoch.getSelection());
			} else if (e.getSource() == enabledDeter) {
				enabledPubchem.setSelection(!enabledPubchem.getSelection());
				enabledStoch.setSelection(!enabledDeter.getSelection());
				enabledStochUserSettings.setSelection(!enabledDeter.getSelection());
				enabledGA.setSelection(!enabledDeter.getSelection());
			} else if (e.getSource() == enabledStochUserSettings) {
				enabledPubchem.setSelection(!enabledPubchem.getSelection());
				enabledStoch.setSelection(!enabledStochUserSettings.getSelection());
				enabledDeter.setSelection(!enabledStochUserSettings.getSelection());
				enabledGA.setSelection(!enabledStochUserSettings.getSelection());
			} else if (e.getSource() == enabledGA) {
				enabledPubchem.setSelection(!enabledGA.getSelection());
		        enabledStoch.setSelection(!enabledGA.getSelection());
		        enabledStochUserSettings.setSelection(!enabledGA.getSelection());
		        enabledDeter.setSelection(!enabledGA.getSelection());
			} else if (e.getSource() == enabledPubchem) {
		        enabledStoch.setSelection(!enabledPubchem.getSelection());
		        enabledStochUserSettings.setSelection(!enabledPubchem.getSelection());
		        enabledDeter.setSelection(!enabledPubchem.getSelection());
				enabledGA.setSelection(!enabledPubchem.getSelection());
		    }
			setDirty(true);
		}
	}

}
