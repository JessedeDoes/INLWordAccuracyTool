<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">


<xsl:strip-space elements="*"/>


<xsl:template match="Word"/>

<xsl:template match="Glyph">Jaja</xsl:template>
<xsl:template match="//Glyph[not (ancestor::Word)]">
Kaboem!!!
</xsl:template>
</xsl:stylesheet>
