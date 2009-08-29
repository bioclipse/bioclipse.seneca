package net.bioclipse.seneca.wizard;

import net.bioclipse.seneca.domain.SenecaJobSpecification;

import org.eclipse.jface.wizard.Wizard;


public class CheckJobWizard extends Wizard {
    
    private SenecaJobSpecification specification;
    
    public CheckJobWizard(SenecaJobSpecification specification){
        this.specification = specification;
    }

    @Override
    public boolean performFinish() {
        return true;
    }
    
    public void addPages(){
        SJSCheckWizardPage checkpage = new SJSCheckWizardPage(this, specification);
        addPage(checkpage);
    }

}
