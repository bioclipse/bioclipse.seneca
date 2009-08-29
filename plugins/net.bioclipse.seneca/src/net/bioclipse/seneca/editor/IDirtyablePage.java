package net.bioclipse.seneca.editor;

import org.eclipse.ui.forms.editor.IFormPage;

public interface IDirtyablePage extends IFormPage {

	public void setDirty(boolean isDirty);
	
}
