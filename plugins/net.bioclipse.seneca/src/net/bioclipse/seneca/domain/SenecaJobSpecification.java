/*******************************************************************************
 * Copyright (c) 2007 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Egon Willighagen
 ******************************************************************************/
package net.bioclipse.seneca.domain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.xmlcml.cml.base.CMLAttribute;

/**
 * Wrapper around an existing XOM resource that contains the job
 * specification for the Seneca CASE job.
 *
 * @author egonw
 */
public class SenecaJobSpecification {

	private static final String ATTRIB_NAME_ENABLED = "enabled";
	private static final String ATTRIB_VALUE_TRUE = "true";
	private static final String ATTRIB_VALUE_FALSE = "false";
	private static final String ATTRIB_NAME_WEIGHT = "weight";

	private static final String NAMESPACE = "http://cdk.sf.net/seneca/";
	private XPathContext context = new XPathContext("sjs", NAMESPACE);

	private Element root;
	private IContainer jobDirectory;

  /**
	 * A default specification will be created
	 */
	public SenecaJobSpecification() {
		this.root = new Element("senecaJob", NAMESPACE);
		Element title = new Element("title", NAMESPACE);
		title.appendChild("Example SENECA Job specification");
		this.root.appendChild(title);
	}

	public SenecaJobSpecification(Document doc, IContainer jobDirectory) {
		this(doc.getRootElement(), jobDirectory);
	}

	public SenecaJobSpecification(Element root, IContainer jobDirectory) {
		this.root = root;
		this.jobDirectory = jobDirectory;
	}

	public InputStream getSource() {
		return new ByteArrayInputStream(this.toString().getBytes());
	}

	public void setMolecularFormula(String mf) {
		Nodes result = root.query("./sjs:mf", context);
		if (result.size() > 0) {
			// remove previous stuff
			for (int i=0; i<result.size(); i++)
				root.removeChild(result.get(i));
		}
		Element mfElem = new Element("mf", NAMESPACE);
		mfElem.appendChild(mf);
		root.appendChild(mfElem);
	}

	public String getMolecularFormula() {
		Nodes result = root.query("./sjs:mf", context);
		if (result.size() > 0) {
			return result.get(0).getValue();
		}
		return "";
	}

	public void setDeptData(int hcount, int count) {
		Nodes result = root.query("./sjs:data", context);
		Element dataElem = null;
		if (result.size() > 0) {
			// ensure only one data element
			for (int i=0; i<result.size()-1; i++)
				root.removeChild(result.get(i));
			dataElem = (Element)result.get(0);
		} else {
			dataElem = new Element("data", NAMESPACE);
			root.appendChild(dataElem);
		}
		Attribute hcountAttribute = dataElem.getAttribute("CH"+hcount);
		if (hcountAttribute == null) {
			hcountAttribute = new Attribute("CH"+hcount, Integer.toString( count ));
			dataElem.addAttribute(hcountAttribute);
		} else {
			hcountAttribute.setValue(Integer.toString( count ));
		}
	}

	public int getDeptData(int hcount) {
		Nodes result = root.query("./sjs:data", context);
		if (result.size() > 0) {
			Attribute fileAttrib = ((Element)result.get(0)).getAttribute("CH"+hcount);
			if (fileAttrib != null) {
				return Integer.parseInt( fileAttrib.getValue() );
			}
		}
		return 0;
	}

  public void setJobTitle(String title) {
		Nodes result = root.query("./sjs:title", context);
		if (result.size() > 0) {
			// remove previous stuff
			for (int i=0; i<result.size(); i++)
				root.removeChild(result.get(i));
		}
		Element mfElem = new Element("title", NAMESPACE);
		mfElem.appendChild(title);
		root.appendChild(mfElem);
	}

	public String getJobTitle() {
		Nodes result = root.query("./sjs:title", context);
		if (result.size() > 0) {
			return result.get(0).getValue();
		}
		return "";
	}
	
	public boolean getDetectAromaticity(){
	    Nodes result = root.query("./sjs:detectAromaticity", context);
	    if (result.size() > 0) {
	      return result.get(0).getValue().equals( "true" );
	    }
	    return false;	    
	}
	
	public void setDetectAromaticity(boolean detectAromaticity){
	    Nodes result = root.query("./sjs:detectAromaticity", context);
	    if (result.size() > 0) {
	      // remove previous stuff
	      for (int i=0; i<result.size(); i++)
	        root.removeChild(result.get(i));
	    }
	    Element mfElem = new Element("detectAromaticity", NAMESPACE);
	    mfElem.appendChild(detectAromaticity ? "true" : "false");
	    root.appendChild(mfElem);
	}

	public void setJudgeEnabled(String id, boolean enabled) {
		Nodes result = root.query("./sjs:judge[./@id='" + id + "']", context);
		if (enabled) {
			if (result.size() > 0) {
				if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).equals(ATTRIB_VALUE_TRUE)) {
					// OK
				} else {
					// set enabled=true
					((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).setValue(ATTRIB_VALUE_TRUE);
				}
			} else {
				// add new element
				Element judgeElem = new Element("judge", NAMESPACE);
				judgeElem.addAttribute(new Attribute("id", id));
				judgeElem.addAttribute(new Attribute(ATTRIB_NAME_ENABLED, ATTRIB_VALUE_TRUE));
				root.appendChild(judgeElem);
			}
		} else { // !enabled
			if (result.size() > 0) {
				// remove previous stuff
				for (int i=0; i<result.size(); i++)
					root.removeChild(result.get(i));
			}
		}
	}

	public boolean getJudgeEnabled(String id) {
		Nodes result = root.query("./sjs:judge[./@id='" + id + "']", context);
		if (result.size() > 0) {
			if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).getValue().equals(ATTRIB_VALUE_TRUE)) {
				return true;
			}
		}
		return false;
	}


	 public List<String> getJudges() {
	     Nodes result = root.query("./sjs:judge", context);
	     List<String> judges = new ArrayList<String>();
	     for (int i=0; i<result.size(); i++) {
	       if (((Element)result.get(i)).getAttribute("id") != null) {
	         judges.add(((Element)result.get(i)).getAttribute("id").getValue());
	       }
	     }
	     return judges;
	 }
	   
	 public Map<String,IPath> getJudgesData() {
		Nodes result = root.query("./sjs:judge", context);
		Map<String, IPath> judgesData = new HashMap<String,IPath>();
		for (int i=0; i<result.size(); i++) {
			if (((Element)result.get(i)).getAttribute("data") != null) {
				judgesData.put(((Element)result.get(i)).getAttribute("id").getValue(),new Path(((Element)result.get(i)).getAttribute("data").getValue()));
			}
		}
		return judgesData;
	}
	 
	 public void setJudgeData(String id, String datafile){
	     Nodes result = root.query("./sjs:judge[./@id='" + id + "']", context);
	     for (int i=0; i<result.size(); i++) {
           CMLAttribute attr=new CMLAttribute("data",datafile);
           ((Element)result.get(i)).addAttribute( attr );
	     }
	 }

	public void setGeneratorEnabled(String id, boolean enabled) {
		Nodes result = root.query("./sjs:generator[./@id='" + id + "']", context);
		if (enabled) {
			if (result.size() > 0) {
				if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).equals(ATTRIB_VALUE_TRUE)) {
					// OK
				} else {
					// set enabled=true
					((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).setValue(ATTRIB_VALUE_TRUE);
				}
			} else {
				// add new element
				Element judgeElem = new Element("generator", NAMESPACE);
				judgeElem.addAttribute(new Attribute("id", id));
				judgeElem.addAttribute(new Attribute(ATTRIB_NAME_ENABLED, ATTRIB_VALUE_TRUE));
				root.appendChild(judgeElem);
			}
		} else { // !enabled
			if (result.size() > 0) {
				// remove previous stuff
				for (int i=0; i<result.size(); i++)
					root.removeChild(result.get(i));
			}
		}
	}

	public void setGeneratorSetting(String id, String field, String value) {
		Nodes generatorNodes = root.query("./sjs:generator[./@id='" + id + "']", context);
		Element generatorNode = null;
		if (generatorNodes.size() == 0) {
			// add new element
			generatorNode = new Element("generator", NAMESPACE);
			generatorNode.addAttribute(new Attribute("id", id));
			generatorNode.addAttribute(new Attribute(ATTRIB_NAME_ENABLED, ATTRIB_VALUE_FALSE));
			root.appendChild(generatorNode);
		} else {
			generatorNode = (Element)generatorNodes.get(0);
		}
		Nodes result = generatorNode.query("./sjs:" + field, context);
		if(value!=null){
			if (result.size() > 0) {
				Element settingNode = (Element)result.get(0);
				Attribute attr = settingNode.getAttribute("value");
				if (attr == null) {
					settingNode.addAttribute(new Attribute("value", value));
				} else if (!attr.getValue().equals(value)){
					attr.setValue(value);
				}
			} else {
				// add new element
				Element judgeElem = new Element(field, NAMESPACE);
				judgeElem.addAttribute(new Attribute("value", value));
				generatorNode.appendChild(judgeElem);
			}
		}else{
			if (result.size() > 0) {
				generatorNode.removeChild(result.get(0));
			}
		}
	}

	public String getGeneratorSetting(String id, String field) {
		Nodes result = root.query("./sjs:generator/sjs:" + field, context);
		if (result.size() > 0) {
			if (((Element)result.get(0)).getAttribute("value").getValue() != null) {
				return ((Element)result.get(0)).getAttribute("value").getValue();
			}
		}
		return null;
	}

	public boolean getGeneratorEnabled(String id) {
		Nodes result = root.query("./sjs:generator[./@id='" + id + "']", context);
		if (result.size() > 0) {
			if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_ENABLED).getValue().equals(ATTRIB_VALUE_TRUE)) {
				return true;
			}
		}
		return false;
	}

	public String getGenerator() {
		Nodes result = root.query("./sjs:generator", context);
		if (result.size() > 0) {
			if (((Element)result.get(0)).getAttribute("id").getValue() != null) {
				return ((Element)result.get(0)).getAttribute("id").getValue();
			}
		}
		return null;
	}


	public String toString() {
		return this.root.toXML();
	}
  
  public IContainer getJobDirectory() {
  
      return jobDirectory;
  }

  
	public int getWeight(String id) {
		Nodes result = root.query("./sjs:judge[./@id='" + id + "']", context);
		if (result.size() > 0) {
			if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_WEIGHT)!=null){
				return Integer.parseInt(((Element)result.get(0)).getAttribute(ATTRIB_NAME_WEIGHT).getValue());
			}
		}
		return 1;	
	}

	public void setWeight(String id, int weight) {
		Nodes result = root.query("./sjs:judge[./@id='" + id + "']", context);
		if(result.size()>0){
			if (((Element)result.get(0)).getAttribute(ATTRIB_NAME_WEIGHT)!=null){
				((Element)result.get(0)).getAttribute(ATTRIB_NAME_WEIGHT).setValue(Integer.toString(weight));
			}else{
				((Element)result.get(0)).addAttribute(new Attribute(ATTRIB_NAME_WEIGHT,Integer.toString(weight)));
			}
		}
	}
}
