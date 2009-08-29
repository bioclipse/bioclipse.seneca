package net.bioclipse.seneca.anneal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openscience.cdk.exception.CDKException;

public interface AnnealingEngineI {

	public void addTemperatureListener(TemperatureListener listener);

	public void run() throws CDKException;
	
	public void setAnnealerAdapter(AnnealerAdapterI adapter);

}