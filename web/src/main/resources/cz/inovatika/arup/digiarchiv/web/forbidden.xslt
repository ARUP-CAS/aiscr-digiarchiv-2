<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:amcr21="https://api.aiscr.cz/schema/amcr/2.1/"
    xmlns:amcr20="https://api.aiscr.cz/schema/amcr/2.0/">

    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

    <!-- Match the root element and apply templates to all child nodes -->
    <xsl:template match="/">
        <xsl:apply-templates select="*" />
    </xsl:template>

    <!-- Match amcr:amcr for both schema versions -->
    <xsl:template match="amcr21:amcr | amcr20:amcr">
        <xsl:copy>
            <!-- Copy all attributes -->
            <xsl:copy-of select="@*" />
            <!-- Insert the forbidden message without extra lines -->
            <xsl:text>HTTP/1.1 403 Forbidden</xsl:text>
        </xsl:copy>
    </xsl:template>

    <!-- Remove all other content -->
    <xsl:template match="node() | @*"></xsl:template>

</xsl:stylesheet>
