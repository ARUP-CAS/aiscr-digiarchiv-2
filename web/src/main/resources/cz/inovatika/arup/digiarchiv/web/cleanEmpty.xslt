<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="mods"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:srw_dc="info:srw/schema/1/dc-schema"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:gml="https://www.opengis.net/gml/3.2" xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


        
        
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
        <xsl:variable name="base_url">http://base_url/id/</xsl:variable>
    <xsl:template match="*">
        <xsl:copy>
             <xsl:apply-templates 
                  select="node()[boolean(normalize-space())]
                         |@*"/>
     </xsl:copy>
        
    </xsl:template>
    
</xsl:stylesheet>