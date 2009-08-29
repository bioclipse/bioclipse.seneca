package net.bioclipse.seneca.business;


import net.bioclipse.seneca.Activator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;

public class SenecaManagerFactory implements IExecutableExtension,
		IExecutableExtensionFactory {

	private Object senecaManager;

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {

		senecaManager = Activator.getDefault().getJavaScriptSenecaManager();
		if (senecaManager == null) {
			senecaManager = new Object();
		}
	}

	public Object create() throws CoreException {
		return senecaManager;
	}
}
