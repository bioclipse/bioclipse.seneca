package net.bioclipse.seneca;

import java.util.ArrayList;
import java.util.List;

import net.bioclipse.core.util.LogUtils;
import net.bioclipse.seneca.business.IJavaScriptSenecaManager;
import net.bioclipse.seneca.business.IJavaSenecaManager;
import net.bioclipse.seneca.business.ISenecaManager;
import net.bioclipse.seneca.judge.IJudge;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends AbstractUIPlugin {

	private static final Logger logger = Logger.getLogger(Activator.class);

	public static final String PLUGIN_ID = "net.bioclipse.seneca";

	private static List<IJudge> judgeExtensions = null;

	private static Activator plugin;

	private ServiceTracker finderTracker;
  private ServiceTracker jsFinderTracker;

	public Activator() {}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

    finderTracker = new ServiceTracker( context, 
                                        IJavaSenecaManager.class.getName(), 
                                        null );
    
    finderTracker.open();
    jsFinderTracker = new ServiceTracker( context, 
                                        IJavaScriptSenecaManager.class.getName(), 
                                        null );
    jsFinderTracker.open();
		getJudgeExtensions();
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		finderTracker.close();
		plugin = null;
	}

	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	 public IJavaSenecaManager getJavaSenecaManager() {
		 	IJavaSenecaManager manager = null;
	        try {
	            manager = (IJavaSenecaManager)
	            	finderTracker.waitForService(1000);
	        } catch (InterruptedException e) {
	            LogUtils.debugTrace(logger, e);
	        }
	        if (manager == null) {
	            throw new IllegalStateException(
	            		"Could not get the Seneca manager");
	        }
	        return manager;
	    }
	 
   public IJavaScriptSenecaManager getJavaScriptSenecaManager() {
       IJavaScriptSenecaManager manager = null;
       try {
           manager = (IJavaScriptSenecaManager) jsFinderTracker.waitForService(1000*10);
       } catch (InterruptedException e) {
           LogUtils.debugTrace(logger, e);
       }
       if(manager == null) {
           throw new IllegalStateException("Could not get the CDK manager");
       }
       return manager;
   }


	public List<IJudge> getJudgeExtensions() {
		if (judgeExtensions == null) {
			judgeExtensions = new ArrayList<IJudge>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint
				= registry.getExtensionPoint(
						"net.bioclipse.seneca.SenecaJudge");

			if (extensionPoint == null) {
				logger.error("Could not find Judge extension Point.");
				return judgeExtensions;
			}

			IExtension[] extensions = extensionPoint.getExtensions();

			for (int i = 0; i < extensions.length; i++) {

				IConfigurationElement[] configelements
					= extensions[i].getConfigurationElements();
				for (int j = 0; j < configelements.length; j++) {
					IJudge creator = null;
					try {
						creator = (IJudge)
							configelements[j].
								createExecutableExtension("class");
					} catch (Exception e) {
					    e.printStackTrace();
						logger.debug(
								"Failed to instantiate factory: "
								+ configelements[j].getAttribute("class")
								+ " in type: "
								+ extensionPoint.getUniqueIdentifier()
								+ " in plugin: "
								+ configelements[j].getDeclaringExtension()
									.getExtensionPointUniqueIdentifier()
								+ e);
					}
					if (creator != null) {
						judgeExtensions.add(creator);
					}
				}
			}
		}
		return judgeExtensions;
	}

}
