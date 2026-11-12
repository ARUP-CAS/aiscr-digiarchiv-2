<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.2/"
    xmlns:amcr22="https://api.aiscr.cz/schema/amcr/2.2/"
    xmlns:amcr21="https://api.aiscr.cz/schema/amcr/2.1/"
    xmlns:amcr20="https://api.aiscr.cz/schema/amcr/2.0/">

    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="amcr:historie[amcr:typ_zmeny = 'KAT']/amcr:poznamka">
        <xsl:copy>HTTP/1.1 403 Forbidden</xsl:copy>
    </xsl:template>

</xsl:stylesheet>