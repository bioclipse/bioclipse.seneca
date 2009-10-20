package net.bioclipse.seneca.judge;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import net.bioclipse.chemoinformatics.wizards.WizardHelper;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.spectrum.domain.IJumboSpectrum;
import net.bioclipse.spectrum.domain.JumboSpectrum;
import net.bioclipse.spectrum.editor.MetadataUtils;
import net.bioclipse.spectrum.editor.SpectrumEditor;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLElements;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLCml;
import org.xmlcml.cml.element.CMLMetadata;
import org.xmlcml.cml.element.CMLMetadataList;
import org.xmlcml.cml.element.CMLPeak;
import org.xmlcml.cml.element.CMLSpectrum;

import spok.utils.SpectrumUtils;

/**
 * This is an abstract class for all sort of pure 13C judges. The class does 
 * configuration, data processing etc. Implementing classes need to provide 
 * evaluate, calcMaxScore and getDescription methods.
 *
 */
public abstract class Abstract13CJudge extends AbstractJudge {

    private static Logger logger = Logger.getLogger(Abstract13CJudge.class);
	protected double[] carbonShifts;

	/**
	 * Constructor for the Judge object
	 *
	 * @param name
	 *            Description of Parameter
	 */
	public Abstract13CJudge(String name) {
		super(name);
	}


	/* (non-Javadoc)
	 * @see net.bioclipse.seneca.judge.AbstractJudge#createJudge(org.eclipse.core.runtime.IPath)
	 */
	@Override
	public IJudge createJudge(IPath data) throws MissingInformationException {
		try {
			this.setData( data );
			CMLBuilder builder = new CMLBuilder();
			Document doc =  builder.buildEnsureCML(ResourcesPlugin.getWorkspace().getRoot().getFile( this.getData()).getContents());
			SpectrumUtils.namespaceThemAll( doc.getRootElement().getChildElements() );
			doc.getRootElement().setNamespaceURI(CMLUtil.CML_NS);
			Element element = builder.parseString(doc.toXML());
			if(element instanceof CMLCml)
			    this.configure((CMLCml)element);
			else if(element instanceof CMLSpectrum){
			    CMLCml cmlcml=new CMLCml();
			    cmlcml.appendChild( element );
			    this.configure(cmlcml);
			}            
			this.setEnabled(super.getEnabled());
			return this;
		} catch (IOException e) {
			throw new MissingInformationException("Could not read the cmlString.");
		} catch (ParsingException e) {
			throw new MissingInformationException(
					"Could not parse the cmlString; " + e.getMessage()
			);
		} catch ( CoreException e ) {
			throw new MissingInformationException(e.getMessage());
		}
	}

    /* (non-Javadoc)
     * @see net.bioclipse.seneca.judge.IJudge#checkJudge(java.lang.String)
     */
    public boolean checkJudge( String data ) {
        CMLBuilder builder = new CMLBuilder();
        try {
            Document doc =  builder.buildEnsureCML(ResourcesPlugin.getWorkspace().getRoot().getFile( new Path(data)).getContents());
            SpectrumUtils.namespaceThemAll( doc.getRootElement().getChildElements() );
            doc.getRootElement().setNamespaceURI(CMLUtil.CML_NS);
            Element element = builder.parseString(doc.toXML());
            if(element instanceof CMLCml)
                configure((CMLCml)element);
            else if(element instanceof CMLSpectrum){
                CMLCml cmlcml=new CMLCml();
                cmlcml.appendChild( element );
                configure(cmlcml);
            }            
        } catch (Exception e) {
            return false;
        }
        return true;
    }

	private void configure(CMLElement input) throws MissingInformationException {
		if (!(input instanceof CMLCml)) {
			throw new MissingInformationException("Root element must be <cml>!");
		}
		CMLCml root = (CMLCml) input;

		String CML_NAMESPACE = "http://www.xml-cml.org/schema";
		XPathContext context = new XPathContext("cml", CML_NAMESPACE);
		Nodes result = root
				.query(
						"./cml:spectrum[./cml:metadataList/cml:metadata/@content=\"13C\"]",
						context);
		if (result.size() == 0) {
			throw new MissingInformationException(
					"No 13C NMR spectrum is defined!");
		}

		CMLSpectrum cmlSpect = (CMLSpectrum) result.get(0);
		if (cmlSpect.getPeakListElements() == null) {
			throw new MissingInformationException("No peaks are defined!");
		}
		CMLElements<CMLPeak> peaks = cmlSpect.getPeakListElements().get(0)
				.getPeakElements();
		carbonShifts = new double[peaks.size()];
		for (int peakNo = 0; peakNo < carbonShifts.length; peakNo++) {
			CMLPeak peak = peaks.get(peakNo);
			carbonShifts[peakNo] = peak.getXValue();
		}

	}
	
	/* (non-Javadoc)
	 * @see net.bioclipse.seneca.judge.IJudge#isLabelling()
	 */
	public boolean isLabelling() {
		return false;
	}

	/* (non-Javadoc)
	 * @see net.bioclipse.seneca.judge.IJudge#labelStartStructure(org.openscience.cdk.interfaces.IAtomContainer)
	 */
	public void labelStartStructure(IAtomContainer startStructure) {
	}

    /* (non-Javadoc)
     * @see net.bioclipse.seneca.judge.IJudge#setData(org.eclipse.jface.viewers.ISelection, org.eclipse.core.resources.IFile)
     */
    public IFile setData( ISelection selection, IFile sjsFile ) {
        IStructuredSelection ssel = (IStructuredSelection) selection;
        if(ssel.size()>1){
            MessageBox mb = new MessageBox(new Shell(), SWT.ICON_WARNING);
            mb.setText("Multiple Files");
            mb.setMessage("Only one file can be dropped on here!");
            mb.open();
            return null;
        }else{
            if (ssel.getFirstElement() instanceof IFile) {
                IFile file = (IFile) ssel.getFirstElement();
                IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
                InputStream stream;
                try {
                    boolean peakPickingDone=false;
                    stream = file.getContents();
                    IContentType contentType = contentTypeManager.findContentTypeFor(stream, file.getName());
                    if(contentType.getId().equals( "net.bioclipse.contenttypes.jcampdx" ) ||  contentType.getId().equals( "net.bioclipse.contenttypes.cml.singleSpectrum")){
                        IJumboSpectrum spectrum=net.bioclipse.spectrum
                            .Activator.getDefault()
                            .getJavaSpectrumManager()
                            .loadSpectrum( file );
                        IJumboSpectrum newspectrum=null;
                        if(spectrum.getJumboObject().getPeakListElements()!=null && spectrum.getJumboObject().getPeakListElements().size()>0){
                            CMLSpectrum cmlspec=new CMLSpectrum();
                            cmlspec.appendChild( spectrum.getJumboObject().getPeakListElements().get( 0 ) );
                            newspectrum = new JumboSpectrum(cmlspec);
                        }else if(spectrum.getJumboObject().getPeakListElements().size()==0){
                            if(spectrum.getJumboObject().getSpectrumDataElements().size()>0){
                              MessageBox mb = new MessageBox(new Shell(), SWT.ICON_INFORMATION);
                              mb.setText("Peak picking necessary");
                              mb.setMessage("This spectrum has no peaks, but continuous data. We will perform a peak picking on it!");
                              mb.open();
                              newspectrum = net.bioclipse.spectrum
                                  .Activator.getDefault()
                                  .getJavaSpectrumManager()
                                  .pickPeaks( spectrum );
                              peakPickingDone=true;
                            }else{
                                MessageBox mb = new MessageBox(new Shell(), SWT.ERROR);
                                mb.setText("No data in here");
                                mb.setMessage("It looks like this spectrum has neither peak nor continuous data. We cannot use this!");
                                mb.open();
                                return null;
                            }
                        }
                        //make this a 13c spectrum (should also go in judge
                        CMLElements<CMLMetadataList> mlists = newspectrum.getJumboObject().getMetadataListElements();
                        Iterator<CMLMetadataList> it = mlists.iterator();
                        String type="";
                        while (it.hasNext()) {
                          CMLMetadataList mlist = it.next();
                            List<CMLMetadata> freq = MetadataUtils.getMetadataDescendantsByName(mlist.getMetadataDescendants(),"jcampdx:OBSERVENUCLEUS");
                            if (freq != null && freq.size() > 0) {
                                type = freq.get( 0 ).getDictRef();
                            }
                        }
                        if(!type.equals( "13C" )){
                            if(newspectrum.getJumboObject().getMetadataListElements().size()==0)
                                newspectrum.getJumboObject().addMetadataList( new  CMLMetadataList() );
                            CMLMetadata metadata = new CMLMetadata();
                            metadata.setDictRef( "jcampdx:OBSERVENUCLEUS" );
                            metadata.setContent( "13C" );
                            newspectrum.getJumboObject().getMetadataListElements().get( 0 ).addMetadata( metadata );
                        }
                        //if the file is somewhere else or a peak picking was done, we make a new file
                        IFile newFile;
                        if(file.getParent()!=sjsFile.getParent() || peakPickingDone){
                            IContainer folder = sjsFile.getParent();
                            String newFileName;
                            if(file.getParent()==sjsFile.getParent())
                                newFileName=file.getName().substring( 0, file.getName().length()-1-file.getFileExtension().length() )+"peaks";
                            else
                                newFileName=file.getName().substring( 0, file.getName().length()-1-file.getFileExtension().length() );
                            IStructuredSelection projectFolder = 
                                new StructuredSelection(
                                        folder);
                            String filename = WizardHelper.
                            findUnusedFileName(
                                projectFolder, newFileName, ".cml");
                            newFile = folder.getFile( new Path(filename));
                            net.bioclipse.spectrum.Activator
                                .getDefault().getJavaSpectrumManager()
                                .saveSpectrum(
                                    newspectrum, newFile,
                                    SpectrumEditor.CML_TYPE
                                );
                        }else{
                            newFile = file;
                        }
                        return newFile;
                    }else{
                        MessageBox mb = new MessageBox(new Shell(), SWT.ICON_WARNING);
                        mb.setText("Not a spectrum file");
                        mb.setMessage("Only a spectrum file (JCAMP or CML) can be dropped on here!");
                        mb.open();
                        return null;
                    }
                } catch ( Exception e ) {
                    LogUtils.handleException( e, logger );
                    return null;
                }
            }else{
                MessageBox mb = new MessageBox(new Shell(), SWT.ICON_WARNING);
                mb.setText("Not a file");
                mb.setMessage("Only a file (not directory etc.) can be dropped on here!");
                mb.open();
                return null;
            }
        }
    }
}
