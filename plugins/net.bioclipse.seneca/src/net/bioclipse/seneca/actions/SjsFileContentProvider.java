/*****************************************************************************
 * Copyright (c) 2008 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *****************************************************************************/
package net.bioclipse.seneca.actions;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.bioclipse.chemoinformatics.util.ChemoinformaticUtils;
import net.bioclipse.core.util.LogUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
/** 
 * A class implementing ITreeContentProvider and only returning child elements 
 * which are molecule files. This can be used to build TreeViewers for browsing 
 * for molecules.
 *
 */
public class SjsFileContentProvider implements ITreeContentProvider {
    private static final Logger logger 
        = Logger.getLogger(SjsFileContentProvider.class);
    public SjsFileContentProvider() {
    }
    public void dispose() {
    }
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }
    public Object[] getChildren(Object parentElement) {
        ArrayList<IResource> childElements = new ArrayList<IResource>();
        if ( parentElement instanceof IContainer 
             && ( (IContainer)parentElement ).isAccessible() ) {
            IContainer container = (IContainer)parentElement;
            try {
                for ( int i=0 ; i < container.members().length ; i++ ) {
                    IResource resource = container.members()[i];
                    if ( resource instanceof IFile && isSjs( (IFile ) resource )) {
                        childElements.add(resource);
                    }else if ( resource instanceof IContainer 
                         && resource.isAccessible() 
                         && containsMolecules((IContainer)resource)) {
                            childElements.add(resource);
                    }
                }
            } 
            catch (CoreException e) {
                LogUtils.handleException(e,logger, net.bioclipse.chemoinformatics.Activator.PLUGIN_ID);
            } catch ( IOException e ) {
                LogUtils.handleException(e,logger, net.bioclipse.chemoinformatics.Activator.PLUGIN_ID);
            }
        }
        return childElements.toArray();
    }
    private boolean containsMolecules( IContainer container ) throws CoreException, IOException {
        //we first test all the files, that should be fast
        for(int i=0;i<container.members().length;i++){
            if(container.members()[i] instanceof IFile && isSjs( (IFile )container.members()[i])){
                    return true;
            }
        }
        //if none is a molecule, we need to recursivly check child folders
        for(int i=0;i<container.members().length;i++){
            if(container.members()[i] instanceof IContainer){
                if(containsMolecules( (IContainer )container.members()[i]))
                    return true;
            }
        }
        return false;
    }
    public Object getParent(Object element) {
        if( element instanceof IFolder)
            return ( (IFolder)element ).getParent();
        else
            return null;
    }
    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }
    
    public static boolean isSjs(IFile file) throws CoreException, IOException{
        if(!file.exists())
            return false;
        if(file.getName().endsWith(".sjs"))
        	return true;
        else
        	return false;
   }
}