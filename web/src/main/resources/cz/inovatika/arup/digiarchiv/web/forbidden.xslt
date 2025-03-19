<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/"
                xsi:schemaLocation="https://api.aiscr.cz/schema/amcr/2.0/ https://api.aiscr.cz/schema/amcr/2.0/amcr.xsd"
>
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="/">
        <amcr:amcr xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:gml="http://www.opengis.net/gml/3.2"
            xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.1/" xsi:schemaLocation="https://api.aiscr.cz/schema/amcr/2.0/ https://api.aiscr.cz/schema/amcr/2.0/amcr.xsd">
            <xsl:apply-templates select="@* | node()"/>
        </amcr:amcr>
    </xsl:template>
    
    <xsl:template match="@* | node()">
          HTTP/1.1 403 Forbidden
    </xsl:template>
</xsl:stylesheet>