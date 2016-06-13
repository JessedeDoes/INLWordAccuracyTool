package org.impact.evaluation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AggregateReport extends PageReport
{
	boolean hasPages = false;
	
	public boolean filter(PageReport p)
	{
		return false;
	}
	
	@XmlElement 
	public String getFilter()
	{
		return "";
	}
	
	public void incorporate(PageReport p)
	{
		if (filter(p))
		{
			super.incorporate(p);
			// Will be assigned metadata for the first page included
			if (!hasPages)
			{
				this.metadata = p.metadata;
				this.id = p.id;
			}
			hasPages = true;
		}
	}
}
