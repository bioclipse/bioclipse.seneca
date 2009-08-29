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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.Assert;
import net.bioclipse.core.MockIFile;
import net.bioclipse.core.ResourcePathTransformer;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.ISpectrum;
import net.bioclipse.core.tests.AbstractManagerTest;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.seneca.business.ISenecaManager;
import net.bioclipse.seneca.business.SenecaManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.junit.Test;

public class SenecaManagerPluginTest extends AbstractManagerTest{

    ISenecaManager senecamanager;

    //Do not use SPRING OSGI for this manager
    //since we are only testing the implementations of the manager methods
    public SenecaManagerPluginTest() {
        senecamanager = new SenecaManager();
    }
    
    public IBioclipseManager getManager() {
        return senecamanager;
    }

    @Test
    public void testLoadSpectrum_String() throws IOException, 
                                          BioclipseException, 
                                          CoreException, URISyntaxException {

        URI uri = getClass().getResource("/testFiles/aug07.dx").toURI();
        URL url=FileLocator.toFileURL(uri.toURL());
        String path=url.getFile();
        ISpectrum spectrum = senecamanager.loadSpectrum( path);
        Assert.assertEquals(0,((IJumboSpectrum)spectrum).getJumboObject().getPeakListElements().size());
        Assert.assertEquals(1,((IJumboSpectrum)spectrum).getJumboObject().getSpectrumDataElements().size());
        uri = getClass().getResource("/testFiles/spectrum3.xml").toURI();
        url=FileLocator.toFileURL(uri.toURL());
        path=url.getFile();
        spectrum = senecamanager.loadSpectrum( path);
        Assert.assertEquals(0,((IJumboSpectrum)spectrum).getJumboObject().getPeakListElements().size());
        Assert.assertEquals(1,((IJumboSpectrum)spectrum).getJumboObject().getSpectrumDataElements().size());
    }
    
    @Test
    public void testSaveSpectrum_JumboSpectrum_IFile_String() throws URISyntaxException, IOException, BioclipseException, CoreException{
        URI uri = getClass().getResource("/testFiles/aug07.dx").toURI();
        URL url=FileLocator.toFileURL(uri.toURL());
        String path=url.getFile();
        ISpectrum spectrum = senecamanager.loadSpectrum( path);
        IFile target=new MockIFile();
        senecamanager.saveSpectrum((JumboSpectrum)spectrum, target, SpectrumEditor.JCAMP_TYPE);
        byte[] bytes=new byte[3];
        target.getContents().read(bytes);
        Assert.assertEquals(35, bytes[0]);
        Assert.assertEquals(35, bytes[1]);
        Assert.assertEquals(84, bytes[2]);
    }
    
    @Test
    public void testSaveSpectrum_IJumboSpectrum_String_String() throws URISyntaxException, IOException, BioclipseException, CoreException{
        URI uri = getClass().getResource("/testFiles/spectrum3.xml").toURI();
        URL url = FileLocator.toFileURL(uri.toURL());
        String path = url.getFile();
        JumboSpectrum spectrum = senecamanager.loadSpectrum( path);
        senecamanager.saveSpectrum(spectrum, "/Virtual/testSaveSpectrum.jdx", SpectrumEditor.JCAMP_TYPE);
  	    byte[] bytes=new byte[1000];
        IFile file= ResourcePathTransformer.getInstance().transform("/Virtual/testSaveSpectrum.jdx");
        file.getContents().read(bytes);
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<bytes.length;i++){
             sb.append((char)bytes[i]);
        }
        assertEquals(0, sb.toString().indexOf( "##TITLE=" ));
    }
}
