/*******************************************************************************
 * Copyright (c) 2007-2008 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org�epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 * 
 * Contributors:
 *     shk3
 *     
 *******************************************************************************/
package net.bioclipse.spectrum.business.test;

import net.bioclipse.core.tests.AbstractManagerTest;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.seneca.Activator;
import net.bioclipse.seneca.business.ISenecaManager;

public class SenecaManagerPluginTest extends AbstractManagerTest{

    ISenecaManager senecamanager;

    //Do not use SPRING OSGI for this manager
    //since we are only testing the implementations of the manager methods
    public SenecaManagerPluginTest() {
        senecamanager = Activator.getDefault().getJavaSenecaManager();
    }
    
    public IBioclipseManager getManager() {
        return senecamanager;
    }

}
