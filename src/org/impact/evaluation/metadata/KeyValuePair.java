package org.impact.evaluation.metadata;
/*
 * This class is only used in JAXB marshaling to XML 
 */
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class KeyValuePair 
{
	@XmlElement
	String key;
	@XmlElement
	String value;
	
	public KeyValuePair()
	{
		
	}
	
	public KeyValuePair(String key, String value)
	{
		this.key=key;
		this.value=value;
	}
}
