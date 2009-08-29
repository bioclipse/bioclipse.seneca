package net.bioclipse.seneca.editor;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

public class EditorModifyListener implements ModifyListener {

	private IDirtyablePage page;

	public EditorModifyListener(IDirtyablePage page) {
		this.page = page;
	}
	
	public void modifyText(ModifyEvent e) {
		page.setDirty(true);
	}

	
}
