package org.openscience.cdk.structgen.pubchem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Retrieves all structures from pubchem for a formula.
 * Usage: List<IMolecule> result = PubchemStructureGenerator.doDownload("C10H16");
 * 
 * This class has been adapted from the PowerUserGatewayRequest by Martin Scholz.
 * 
 *	<a rel="license" href="http://creativecommons.org/licenses/by/3.0/us/">
 *	<img alt="Creative Commons License" style="border-width:0" src="http://creativecommons.org/images/public/somerights20.png" />
 *	</a>
 *	<br />This work is licensed under a 
 *	<a rel="license" href="http://creativecommons.org/licenses/by/3.0/us/">Creative Commons Attribution 3.0 United States License</a>.
 *	<hr>
 *
 * @see http://pubchem.ncbi.nlm.nih.gov/ 
 * @see http://depth-first.com/articles/2007/06/04/hacking-pubchem-power-user-gateway
 * @see http://depth-first.com/articles/2007/06/11/hacking-pubchem-learning-to-speak-pug
 * 
 * @author <a href=mailto:mscholz@ucdavis.edu>&nbsp;Martin Scholz&nbsp;</a>
 * @version $Revision: 1.21 $
 *
 */
public class PubchemStructureGenerator {
    public static final String COMPRESSION_NONE = "none";
    public static final String COMPRESSION_GZIP = "gzip";
    public static final String COMPRESSION_BZIP2 = "bzip2";


    private Document requestDocument;
    private Document responseDocument;

    private String requestid;

    private String downloadFormat = "sdf";

    private String compression = COMPRESSION_NONE;
    
    private static final String url = "http://pubchem.ncbi.nlm.nih.gov/pug/pug.cgi";
    private static final String ID_xpath = "//*/PCT-Waiting_reqid";
    
    private String webenv = null;

    public static int worked=0;
    
    public static List<IMolecule> doDownload(String formula, IProgressMonitor monitor) throws TransformerConfigurationException, ParserConfigurationException, IOException, SAXException, FactoryConfigurationError, TransformerFactoryConfigurationError, TransformerException, NodeNotAvailableException{
        PubchemStructureGenerator request = new PubchemStructureGenerator();
        request.submitInitalRequest(formula);
        worked=0;

        while(!request.getStatus().isFinished()){
        	if(monitor.isCanceled())
        		return null;        	
            // looping and waiting
            request.refresh();
            worked++;
            monitor.worked(worked);
        }

        request.submitDownloadRequest();
        
        while(!request.getStatusDownload().isFinished()){
        	if(monitor.isCanceled())
        		return null;        	
            // looping and waiting
            request.refresh();
            worked++;
            monitor.worked(worked);
        }

        URLConnection uc = request.getResponseURL().openConnection();
        String contentType = uc.getContentType();
        int contentLength = uc.getContentLength();
        if (contentType.startsWith("text/") || contentLength == -1) {
            throw new IOException("This is not a binary file.");
        }
        InputStream raw = uc.getInputStream();
        InputStream in = new BufferedInputStream(raw);
        
        List<IMolecule> list = new ArrayList<IMolecule>();
        IteratingMDLReader reader = new IteratingMDLReader(in, DefaultChemObjectBuilder.getInstance());
        while(reader.hasNext()){
        	list.add((IMolecule)reader.next());
        }
        return list;
    }
    
    private void submitDownloadRequest()  throws ParserConfigurationException, IOException, SAXException, FactoryConfigurationError, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        // create the request
        this.initDownloadRequestDocument(webenv);

        // submit it
        this.request();
		
	}
	/**
     * creata a new instance of the request. <pre>The request is <b>not</b> sent.
     * @param pctIDs the pubchem IDs 
     */
    public PubchemStructureGenerator () {
        debug("creating request");
    }


    /**
     * 
     * @throws Exception 
     * @since JAVA5
     * 
     */
    private static Node xpathJDK5(String pathString, Document doc) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node node = ((Node)xpath.evaluate(pathString, doc, XPathConstants.NODE));
            return node;
        } catch (DOMException e) {
            throw new Exception(e.toString());
        } catch (XPathExpressionException e) {
            throw new Exception(e.toString());
        }
    }

    /**
     * creating a new request and checking for an update. The {@link #responseDocument} will be updated and overwirtten by the new {@link Document} received 
     * after the {@link #refresh()}. <p>
     * Before the actual refresh is committed / submitted there is a 3000 pause. 
     */
    public final void refresh() throws ParserConfigurationException, TransformerConfigurationException, IOException, SAXException, FactoryConfigurationError, TransformerFactoryConfigurationError, TransformerException {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        this.initRefreshRequestDocument();

        // submit the actual request.
        this.request();
    }


    public final void submitInitalRequest(String formula) throws ParserConfigurationException, IOException, SAXException, FactoryConfigurationError, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        // create the request
        this.initInitalRequestDocument(formula);

        // submit it
        this.request();
    }


    /**
     * submit the {@link #requestDocument} ({@link Document}). 
     */
    private final void request() throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError, DOMException, TransformerException {
        debug("submit request");
        URL server = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)server.openConnection();
        //connection.connect();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        connection.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF8");
        out.write(getXMLString(this.requestDocument));
        out.close(); 

        
        this.responseDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());

        try {
            // finding the request ID. - the request ID is only received in the inital request and not in the updates.
            this.requestid = xpath(ID_xpath,responseDocument).getFirstChild().getNodeValue();
        } catch (Exception e) {
            // ignore
            // debug("can not determine new ID");
        }
    }


    private final void initRefreshRequestDocument() throws ParserConfigurationException {
        // build a new request
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();

        String[] tagNames = new String[]{
                "PCT-Data_input", 
                "PCT-InputData", 
                "PCT-InputData_request", 
                "PCT-Request"
        };


        // create a root object
        doc.appendChild(doc.createElement("PCT-Data"));
        Element element = doc.getDocumentElement();

        for (int i = 0; i < tagNames.length; i++) {
            String tagname = tagNames[i];
            Element child = doc.createElement(tagname);

            element.appendChild(child);
            element = child;
        }



        Element reqid = doc.createElement("PCT-Request_reqid");
        reqid.appendChild(doc.createTextNode(this.getRequestID()));

        Element type = doc.createElement("PCT-Request_type");
        type.setAttribute("value","status");

        element.appendChild(reqid);
        element.appendChild(type);


        this.requestDocument = doc;
    }

    
    /** 
     * init the first request. Build the document submitted to the PUG. <pre>in case you want to make modifications to the Request (add additional parameters, 
     */
    protected void initDownloadRequestDocument(String id) throws ParserConfigurationException {
        debug("init request");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();

        String[] tagNames = new String[]{
                "PCT-Data_input", 
                "PCT-InputData", 
                "PCT-InputData_download", 
                "PCT-Download", 
                "PCT-Download_uids", 
                "PCT-QueryUids", 
                "PCT-QueryUids_entrez", 
                "PCT-Entrez"
        };


        // create a root object
        doc.appendChild(doc.createElement("PCT-Data"));
        Element element = doc.getDocumentElement();

        for (int i = 0; i < tagNames.length; i++) {
            String tagname = tagNames[i];
            Element child = doc.createElement(tagname);

            element.appendChild(child);
            element = child;
        }

        Element pccompound = doc.createElement("PCT-Entrez_db");
        pccompound.appendChild(doc.createTextNode("pccompound"));
        element.appendChild(pccompound);

        pccompound = doc.createElement("PCT-Entrez_query-key");
        pccompound.appendChild(doc.createTextNode("1"));
        element.appendChild(pccompound);

        pccompound = doc.createElement("PCT-Entrez_webenv");
        pccompound.appendChild(doc.createTextNode(id));
        element.appendChild(pccompound);



        try {
            Node tmp = xpath("//*/PCT-Download", doc);


            Element format = doc.createElement("PCT-Download_format");
            format.setAttribute("value",this.getDownloadFormat());
            tmp.appendChild(format);

            Element compress = doc.createElement("PCT-Download_compression");
            compress.setAttribute("value",this.getCompression());
            tmp.appendChild(compress);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        this.requestDocument = doc;
    }

    /** 
     * init the first request. Build the document submitted to the PUG. <pre>in case you want to make modifications to the Request (add additional parameters, 
     */
    protected void initInitalRequestDocument(String formula) throws ParserConfigurationException {
        debug("init request");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();

        
        
        String[] tagNames = new String[]{
                "PCT-Data_input", 
                "PCT-InputData", 
                "PCT-InputData_query", 
                "PCT-Query", 
                "PCT-Query_type", 
                "PCT-QueryType", 
                "PCT-QueryType_css", 
                "PCT-QueryCompoundCS" 
        };


        // create a root object
        doc.appendChild(doc.createElement("PCT-Data"));
        Element element = doc.getDocumentElement();

        for (int i = 0; i < tagNames.length; i++) {
            String tagname = tagNames[i];
            Element child = doc.createElement(tagname);

            element.appendChild(child);
            element = child;
        }


        Element pccompound = doc.createElement("PCT-QueryCompoundCS_query");
        Element pccompound2 = doc.createElement("PCT-QueryCompoundCS_query_data");
        pccompound.appendChild(pccompound2);
        pccompound2.appendChild(doc.createTextNode(formula));
        element.appendChild(pccompound);

        pccompound = doc.createElement("PCT-QueryCompoundCS_type");
        pccompound2 = doc.createElement("PCT-QueryCompoundCS_type_formula");
        pccompound.appendChild(pccompound2);
        Element pccompound3 = doc.createElement("PCT-CSMolFormula");
        pccompound2.appendChild(pccompound3);
        element.appendChild(pccompound);

        pccompound = doc.createElement("PCT-QueryCompoundCS_results");
        pccompound.appendChild(doc.createTextNode("2000000"));
        element.appendChild(pccompound);

        this.requestDocument = doc;
    }


    /** 
     * @return status of my request. <code>{@link Status#isFinished()}</code> tells you if the result is ready or not.
     */
    public final Status getStatusDownload() {
        try {
            this.getResponseURL();
            return new Status(true, null);
        } catch (NodeNotAvailableException e) {
        	return new Status(false, e);
        }
    }
    
    /** 
     * @return status of my request. <code>{@link Status#isFinished()}</code> tells you if the result is ready or not.
     */
    public final Status getStatus() {
        try {
            this.getEntrezwebenv();
            return new Status(true, null);
        } catch (NodeNotAvailableException e) {
        	return new Status(false, e);
        }
    }


    /**
     * @return the id for the request received from the PowerUserGateway.<pre>Until the request is submitted the id is invalid.</pre>
     */
    public final String getRequestID() {
        return this.requestid;
    }


    /**
     * @return url on the ncbi servers pointing to the resultfile.
     * @throws NodeNotAvailableException
     */
    public final URL getResponseURL() throws NodeNotAvailableException{
        try {
            return new URL(xpath("//*/PCT-Download-URL_url", this.responseDocument).getFirstChild().getNodeValue());
        } catch (Exception e){
            throw new NodeNotAvailableException(e);
        }
    }
    
    public final String getEntrezwebenv() throws NodeNotAvailableException{
        try {
            webenv = xpath("//*/PCT-Entrez_webenv", this.responseDocument).getFirstChild().getNodeValue();
            return webenv;
        } catch (Exception e){
            throw new NodeNotAvailableException(e);
        }
    }


    public final String getCompression() {
        return compression;
    }


    public final String getDownloadFormat() {
        return downloadFormat;
    }


    /**
     * convert an XML {@link Document} into a {@link String}.
     * @return String containg the document.
     */
    private static String getXMLString(Document requestDocument){
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource source = new DOMSource(requestDocument);
            StringWriter stringWriter =new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);
            transformer.transform(source, streamResult);
            return stringWriter.toString();
        } catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }    

    private static void debug(String string) {
        //System.out.println(string);
    }


    class NodeNotAvailableException extends Exception {
        public NodeNotAvailableException(Exception e) {
            super(e);
        }
    }

    /**
     * Status.
     * @author scholz
     */
    class Status {

        private boolean status;
        private String msg;

        Status(boolean b, Object object) {
            this.status = b;
            if (msg != null){
                this.msg = object.toString();
            } else {
                this.msg = "--";
            }
        }

        public boolean isFinished() {
            return status;
        }

        public String getMessage(){
            return msg;
        }
    }


    public final void setCompression(String compression) {
        this.compression = compression;
    }


    public final void setDownloadFormat(String downloadFormat) {
        this.downloadFormat = downloadFormat;
    }


    public static Node xpath(String pathString, final Document doc) throws Exception {
        Node node = xpathJDK5(pathString, doc);
        //Node node = xpathJDK1_4(pathString, doc);
        return node;
    }
}
