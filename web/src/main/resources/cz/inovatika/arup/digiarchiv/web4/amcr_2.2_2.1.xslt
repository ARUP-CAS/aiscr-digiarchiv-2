<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:amcr_old="https://api.aiscr.cz/schema/amcr/2.2/"
    xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.1/" exclude-result-prefixes="amcr_old">

    <xsl:template match="amcr_old:amcr">
        <amcr:amcr xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:gml="http://www.opengis.net/gml/3.2"
            xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.1/" xsi:schemaLocation="https://api.aiscr.cz/schema/amcr/2.1/ https://api.aiscr.cz/schema/amcr/2.1/amcr.xsd">
            <xsl:apply-templates select="@*|node()"/>
        </amcr:amcr>
    </xsl:template>

    <xsl:template match="amcr_old:*">
        <xsl:element name="amcr:{local-name()}" >
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{name()}" namespace="{namespace-uri()}">
            <xsl:apply-templates select="@*|node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:if test="not(name()='xsi:schemaLocation')">
            <xsl:copy/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:copy/>
    </xsl:template>

    <!-- Odstraneni vybranych elementu -->
    <xsl:template match="amcr_old:ruian_kraj/amcr_old:email"/>

</xsl:stylesheet>
