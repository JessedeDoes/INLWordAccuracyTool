package org.impact.evaluation;
import java.util.List;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class Error 
{
	@XmlElement
	List<Word> ocr = new ArrayList<Word>();
	@XmlElement
    List<Word> truth = new ArrayList<Word>();
	@XmlAttribute
	String regionId = null;
}
