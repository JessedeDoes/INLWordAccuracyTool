<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">


<xsl:strip-space elements="*"/>

<xsl:template match="pageEvaluator">
		<html>
			<head>
				<title>
					<xsl:value-of select="./ocrLocation" />
					:
				</title>
				<style type="text/css">
	.multicol {
	-moz-column-count: 4;
	-moz-column-gap: 1em;
	-moz-column-rule: 1px solid black;
	-webkit-column-count: 3;
	-webkit-column-gap: 1em;
	-webkit-column-rule: 1px solid black;
	}

	.pageListxx {
	-moz-column-count: 3;
	-moz-column-gap: 1em;
	-moz-column-rule: 1px solid black;
	-webkit-column-count: 3;
	-webkit-column-gap: 1em;
	-webkit-column-rule: 1px solid black;
	}

	.pageReport {
	-moz-column-count: 2;
	-moz-column-gap: 1em;
	-moz-column-rule: 1px solid black;
	-webkit-column-count: 3;
	-webkit-column-gap: 1em;
	-webkit-column-rule: 1px solid black;
	}

	td { background-color: #fefeee }
	body { font-family: Calibri; background-color: lightblue }
</style>
				<script type="text/JavaScript">
					function toggle(x)
					{
					var y = document.getElementById(x);
					if (y.style.display=='none')
					y.style.display='block';
					else
					y.style.display='none';
					}
	</script>
			</head>
			<body>
				<h1>
					<xsl:value-of select="./ocrLocation" />
					:
				</h1>
				<h2>
					Precision:
					<xsl:value-of select="./precision" />
					,
					Recall:
					<xsl:value-of select="./recall" />
				</h2>
				<xsl:call-template name="barchart_period_with_compare"/>
				<xsl:variable name="nTitles">
				<xsl:value-of select="count(.//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')])"/>
				</xsl:variable>
				<xsl:call-template name="barchart_title">
				<xsl:with-param name="start">1</xsl:with-param>
				<xsl:with-param name="end">10</xsl:with-param>
				</xsl:call-template>
				<xsl:if test="$nTitles &gt;= 10">
				<xsl:call-template name="barchart_title">
				<xsl:with-param name="start">10</xsl:with-param>
				<xsl:with-param name="end">20</xsl:with-param>
				</xsl:call-template>
				</xsl:if>
				<xsl:if test="$nTitles &gt;= 20">
				<xsl:call-template name="barchart_title">
				<xsl:with-param name="start">20</xsl:with-param>
				<xsl:with-param name="end">30</xsl:with-param>
				</xsl:call-template>
				</xsl:if>
				<xsl:if test="$nTitles &gt;= 30">
				<xsl:call-template name="barchart_title">
				<xsl:with-param name="start">30</xsl:with-param>
				<xsl:with-param name="end">40</xsl:with-param>
				</xsl:call-template>
				</xsl:if>

	<div>
		<i>Titles:</i> <!-- pity the dates are missing.... -->
		<ol>
			<xsl:for-each
				select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')]">
				<xsl:sort select="filter" />
				<li>
					<xsl:value-of select="substring(./filter,7)" />, <xsl:value-of select=".//keyValuePair[./key='year']/value"/>
				</li>
			</xsl:for-each>
		</ol>
	</div>
				<div style="background-color: lightgrey; margin-left: 2em">
					<table>
						<xsl:apply-templates
							select="*[not(self::pageReport) and not (self::aggregateReports) and not (self::realWordErrors)]" />
					</table>
				</div>
				<xsl:apply-templates select="./realWordErrors" />
				<div class='aggregates'>
					<a href="javascript:toggle('aggregates')">Show detailed results per title and per age</a>
					<div id='aggregates' style='display:none; margin-left:2em'>
						<xsl:apply-templates select="./aggregateReports" />
					</div>
				</div>
				<div class='pageList'>
					<xsl:apply-templates select="./pageReport" />
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="realWordErrors" priority='100'>
		<xsl:if test=".//frequency">
			<div>
				<a href="javascript:toggle('realWordErrors')">Show in-dictionary errors</a>
				<div style="width:800px; display:none; border-style:solid" id='realWordErrors'>
					<div class='multicol'>
						<xsl:apply-templates />
					</div>
				</div>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="typeFrequencyList">
		<xsl:if test="./frequency &gt; 1">
			<xsl:apply-templates select="type" />
			:
			<xsl:apply-templates select="frequency" />
			<br />
		</xsl:if>
	</xsl:template>

	<xsl:template match="pageReport">
		<div>
			<b style="color: #888888">
				&#x2012;
				<xsl:value-of select="@id" />
			</b>
			<div style="margin-left:2em">
				<div>
					<xsl:value-of select=".//keyValuePair[./key='title']/value" />
					,
					<xsl:value-of select=".//keyValuePair[./key='year']/value" />
				</div>
				<i>
					(p=
					<xsl:value-of select="format-number(./precision,'##%')" />
					, r=
					<xsl:value-of select="format-number(./recall,'##%')" />
					)
				</i>
				<a>
					<xsl:attribute name="href">javascript:toggle('<xsl:value-of
						select="@id" />');</xsl:attribute>
					(show details)
				</a>
				<a>
					<xsl:attribute name="href">file:/<xsl:value-of
						select="./imageFilename" /></xsl:attribute>
					(show image locally)
				</a>
				<a>
					<xsl:attribute name="href">http://www.prima.cse.salford.ac.uk:8080/impact-dataset/private/docview.php?Did=<xsl:value-of
						select="@id" /></xsl:attribute>
					(show page information on PRIMA)
				</a>
				<div class='pageReport'
					style="margin-left: 3em; display:none; background-color: lightgrey">
					<xsl:attribute name="id"><xsl:value-of
						select="@id" /></xsl:attribute>
					<table>
						<xsl:apply-templates select="*[not(self::error)]" />
					</table>
					<div>
						<div>
							<xsl:attribute name="id"><xsl:value-of
								select="@id" />.errors</xsl:attribute>
							<table border="yes">
								<tr>
									<th>OCR</th>
									<th>Truth</th>
								</tr>
								<xsl:apply-templates select="./error" />
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	</xsl:template>

	<xsl:template match="error">
		<tr>
			<td>
				<xsl:apply-templates select="./ocr" />
			</td>
			<td>
				<xsl:apply-templates select="./truth" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="truth|ocr">
		<span>
			<xsl:attribute name="style">
		<xsl:choose>
			<xsl:when test="@inDictionary='true'">color:green;</xsl:when>
		</xsl:choose>
		<xsl:if test="@outOfOrder='true'">text-decoration:underline</xsl:if>
	</xsl:attribute>
			<xsl:if test="./tokenizedText=''">
				<font color='blue'>
					<xsl:value-of select="./text" />
				</font>
				<xsl:text> </xsl:text>
			</xsl:if>
			<xsl:value-of select="./tokenizedText" />
			<xsl:text> </xsl:text>
		</span>
	</xsl:template>

	<xsl:template match="keyValuePair" priority="100">
		<tr>
			<td>
				<b>
					<xsl:value-of select="./key" />
				</b>
			</td>
			<td>
				<xsl:value-of select="./value" />
			</td>
		</tr>
	</xsl:template>


	<xsl:template match="aggregateReports[./nPages>0]">
		<div class='aggregateReport'>
			<b>
				Filter:
				<xsl:value-of select="./filter" />
			</b>
			<xsl:call-template name="piechart"/>
			<div style="margin-left: 2em">
				<table>
					<xsl:apply-templates />
				</table>
			</div>
		</div>
	</xsl:template>

	<xsl:template match="aggregateReports[./nPages='0']"
		priority="100" />

	<xsl:template
		match="pageEvaluator/*[not(self::pageReport) and not(self::aggregateReports)]">
		<!-- <b><xsl:value-of select="name(.)"/></b>=<xsl:value-of select=".//text()"/><br/> -->
		<tr>
			<td>
				<b>
					<xsl:value-of select="name(.)" />
				</b>
			</td>
			<td>
				<xsl:value-of select=".//text()" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="aggregateReports/*|pageReport/*[not(self::error)]">
		<tr>
			<td>
				<b>
					<xsl:value-of select="name(.)" />
				</b>
			</td>
			<td>
				<xsl:value-of select=".//text()" />
			</td>
		</tr>
	</xsl:template>


	<xsl:template name="barchart_period">
	<xsl:variable name="itemWidth" select="50"/>
		<xsl:variable name="itemSpacing" select="10"/>
		
		<xsl:variable name="nItems"><xsl:value-of select="count(.//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')])"/></xsl:variable>
		<xsl:variable name="w"><xsl:value-of select="$nItems * ($itemWidth + $itemSpacing) + $itemSpacing + 30"/></xsl:variable>	
		<div>
			<div><b>Results (word recall) per period</b></div>
			<xsl:variable name="data">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')]">
					<xsl:value-of select="100 * ./recall"></xsl:value-of>
					<xsl:if test="position() &lt; last()">
						,
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="labels">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')]">
					<xsl:value-of select="substring(./filter,8,9)"></xsl:value-of>
					<xsl:if test="position() &lt; last()">
						|
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<!-- bar_width_or_scale, space_between_bars , space_between_groups -->
			<xsl:variable name="chbh">
				50,10,10
			</xsl:variable>
			<xsl:variable name="chm">N,000000,0,-1,11</xsl:variable>
			<img>
				<xsl:attribute name="src">https://chart.googleapis.com/chart?cht=bvs&amp;chxt=x,y&amp;chbh=<xsl:value-of
					select="$chbh" />&amp;chs=<xsl:value-of select="$w"/>x300&amp;chd=t:<xsl:value-of
					select="$data" />&amp;chm=<xsl:value-of select="$chm"/>&amp;chl=<xsl:value-of 
					select="$labels" /></xsl:attribute>
			</img>
		</div>
	</xsl:template>
	
	<xsl:template name="barchart_title">
	<xsl:param name="start">0</xsl:param>
	<xsl:param name="end">10</xsl:param>
	<xsl:variable name="other"><xsl:value-of select="//compareTo"/></xsl:variable>
		<div> 
		<xsl:variable name="itemWidth" select="30"/>
		<xsl:variable name="itemSpacing" select="10"/>
		
		<xsl:variable name="nItems"><xsl:value-of select="count(.//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')][position() &gt;= $start and position() &lt; $end])"/></xsl:variable>
		<xsl:variable name="lastIndex">
		<xsl:choose>
		<xsl:when test="$nItems &gt; $end"><xsl:value-of select="$end -1"/></xsl:when>
		<xsl:otherwise><xsl:value-of select="$start + $nItems -1"/></xsl:otherwise>
		</xsl:choose>
		</xsl:variable>
		
		
		<xsl:variable name="w"><xsl:value-of select="2 * $nItems * ($itemWidth + $itemSpacing) + $itemSpacing + 30"/></xsl:variable>	
			<div><b>Results (word recall) per title, <xsl:value-of select="$start"/> - <xsl:value-of select="$lastIndex"/></b></div>
			
			<xsl:variable name="data">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')]">
					<xsl:sort select="filter"/> <!--  does not work. not in data. -->
					<xsl:if test="position() &gt;= $start and position() &lt; $end">
					<xsl:value-of select="100 * ./recall"></xsl:value-of>
					<xsl:if test="position() &lt; last() and position() &lt; ($end - 1)">
						,
					</xsl:if>
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			
			<xsl:variable name="data2">
				<xsl:for-each
					select="document($other)//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')]">
					<xsl:sort select="filter"/> <!--  does not work. not in data. -->
					<xsl:if test="position() &gt;= $start and position() &lt; $end">
					<xsl:value-of select="100 * ./recall"></xsl:value-of>
					<xsl:if test="position() &lt; last() and position() &lt; ($end - 1)">
						,
					</xsl:if>
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			
			<xsl:variable name="labels">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'title=')]">
					<xsl:sort select="filter"/>
					<xsl:if test="position() &gt;= $start and position() &lt; $end">
					<xsl:value-of select="substring(./filter,7,9)"></xsl:value-of>
					<xsl:if test="position() &lt; last() and position() &lt; ($end - 1)">
						|
					</xsl:if>
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<!-- bar_width_or_scale, space_between_bars , space_between_groups -->
			<xsl:variable name="chbh">
				<xsl:value-of select="$itemWidth"/>,<xsl:value-of select="$itemSpacing"/>,10
			</xsl:variable>
			<!--
			<xsl:variable name="chm">N,000000,0,-1,11</xsl:variable>
			-->
			<xsl:variable name="chm">N,000000,0,,11<xsl:if test="$other != ''">|N,000000,1,,11</xsl:if></xsl:variable>
			<xsl:variable name="src">https://chart.googleapis.com/chart?cht=bvg&amp;chco=5555aa,aaaaff&amp;chxt=x,y&amp;chbh=<xsl:value-of
					select="$chbh" />&amp;chs=<xsl:value-of select="$w"/>x300&amp;chd=t:<xsl:value-of
					select="$data" /><xsl:if test="$other != ''">|<xsl:value-of 
							select="$data2"/></xsl:if>&amp;chl=<xsl:value-of select="$labels" />&amp;chm=<xsl:value-of select="$chm"/>
			</xsl:variable>
			<xsl:variable name="srcNoSpace"><xsl:value-of select="replace($src,'\s', '')"/></xsl:variable>
			<!-- <xsl:value-of select="$srcNoSpace"/> -->
			<img><xsl:attribute name="src"><xsl:value-of select="$srcNoSpace"/></xsl:attribute></img>
			<!-- 
			<img>
				<xsl:attribute name="src">https://chart.googleapis.com/chart?cht=bvg&amp;chco=5555aa,aaaaff&amp;chxt=x,y&amp;chbh=<xsl:value-of
					select="$chbh" />&amp;chs=<xsl:value-of select="$w"/>x300&amp;chd=t:<xsl:value-of
					select="$data" /><xsl:if test="$other != ''">|<xsl:value-of 
							select="$data2"/></xsl:if>&amp;chl=<xsl:value-of select="$labels" />&amp;chm=<xsl:value-of select="$chm"/></xsl:attribute>
			</img>
			-->
		</div>
	</xsl:template>


	<xsl:template name="piechart">
		<div>
			<xsl:variable name="nok">
				<xsl:value-of select="./nWrong div ./nIncludedWords" />
			</xsl:variable>
			<xsl:variable name="ok">
				<xsl:value-of select="(./nIncludedWords - ./nWrong) div ./nIncludedWords" />
			</xsl:variable>
			<img>
				<xsl:attribute name="src">https://chart.googleapis.com/chart?cht=p3&amp;chs=250x100&amp;chd=t:<xsl:value-of
					select="$ok" />,<xsl:value-of select="$nok" />&amp;chl=Correct|Wrong</xsl:attribute>
			</img>
		</div>
	</xsl:template>
	
<xsl:template name="barchart_period_with_compare">
		<xsl:param name="other"><xsl:value-of select="//compareTo"/></xsl:param>
	<xsl:variable name="itemWidth" select="50"/>
		<xsl:variable name="itemSpacing" select="10"/>
		
		<xsl:variable name="nItems"><xsl:value-of select="count(.//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')])"/></xsl:variable>
		<xsl:variable name="w"><xsl:value-of select="2*$nItems * ($itemWidth + $itemSpacing) + $itemSpacing + 30"/></xsl:variable>	
		<div>
			<div><b>Results (word recall) per period</b></div>
			<xsl:variable name="data">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')]">
					<xsl:value-of select="100 * ./recall"></xsl:value-of>
					<xsl:if test="position() &lt; last()">
						,
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="data2">
				<xsl:for-each
					select="document($other)//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')]">
					<xsl:value-of select="100 * ./recall"></xsl:value-of><xsl:if test="position() &lt; last()">,</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="labels">
				<xsl:for-each
					select=".//aggregateReports[./nPages &gt; 0 and contains(./filter,'period=')]">
					<xsl:value-of select="substring(./filter,8,9)"></xsl:value-of><xsl:if test="position() &lt; last()">|</xsl:if>
			</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="chbh">50,10,10</xsl:variable>
			
			<xsl:variable name="chm">N,000000,0,,11<xsl:if test="$other != ''">|N,000000,1,,11</xsl:if></xsl:variable>
			<xsl:variable name="chartSrc">
			https://chart.googleapis.com/chart?cht=bvg&amp;chco=5555aa,aaaaff&amp;chxt=x,y&amp;chs=<xsl:value-of select="$w"/>x300&amp;chd=t:<xsl:value-of
						select="$data" /><xsl:if test="$other != ''">|<xsl:value-of 
							select="$data2"/></xsl:if>&amp;chm=<xsl:value-of select="$chm"/>&amp;chl=<xsl:value-of 
					select="$labels" />&amp;chbh=<xsl:value-of
					select="$chbh" />
			</xsl:variable>
			
			<xsl:variable name="srcNoSpace"><xsl:value-of select="replace($chartSrc,'\s', '')"/></xsl:variable>
			<!-- bar_width_or_scale, space_between_bars , space_between_groups -->
			<!--  
			<xsl:value-of select="$srcNoSpace"/>
			<img>
				<xsl:attribute name="src">https://chart.googleapis.com/chart?cht=bvg&amp;chco=5555aa,aaaaff&amp;chxt=x,y&amp;chs=<xsl:value-of select="$w"/>x300&amp;chd=t:<xsl:value-of
						select="$data" /><xsl:if test="$other != ''">|<xsl:value-of 
							select="$data2"/></xsl:if>&amp;chm=<xsl:value-of select="$chm"/>&amp;chl=<xsl:value-of 
					select="$labels" />&amp;chbh=<xsl:value-of
					select="$chbh" /></xsl:attribute>
			</img>
			-->
			<img><xsl:attribute name="src"><xsl:value-of select="$srcNoSpace"/></xsl:attribute></img>
		</div>
	</xsl:template>
	
	
	<xsl:template match="externalReport/*" priority='100'>
	<b><xsl:value-of select="name()" /></b>:
				<xsl:value-of select=".//text()" />
	</xsl:template>
		
	
	<xsl:template match="externalReport/missedWords[position()=1]" priority='200'>
		<b>Missed words</b>: <xsl:value-of select=".//text()" />
	</xsl:template>
	
   <xsl:template match="externalReport/missedWords[position() != 1]" priority='200'>
		<xsl:value-of select=".//text()"/>
	</xsl:template>


	<xsl:template match="externalReport" priority="100">
	<div>NCSR Evaluation:
	<xsl:for-each select="child::*"> 
				<xsl:apply-templates select="."/>
				<xsl:if test="position() &lt; last()">, </xsl:if>
	</xsl:for-each>
	</div>
	</xsl:template>
</xsl:stylesheet>
