<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:amcr_old="https://api.aiscr.cz/schema/amcr/2.1/"
    xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/" exclude-result-prefixes="amcr_old">

    <xsl:template match="amcr_old:amcr">
        <amcr:amcr xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:gml="http://www.opengis.net/gml/3.2"
            xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/" xsi:schemaLocation="https://api.aiscr.cz/schema/amcr/2.0/ https://api.aiscr.cz/schema/amcr/2.0/amcr.xsd">
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
    <xsl:template match="amcr_old:projekt/amcr_old:geom_system"/>
    <xsl:template match="amcr_old:projekt/amcr_old:chranene_udaje/amcr_old:geom_sjtsk_gml"/>
    <xsl:template match="amcr_old:projekt/amcr_old:chranene_udaje/amcr_old:geom_sjtsk_wkt"/>
    <xsl:template match="amcr_old:dokument/amcr_old:doi"/>
    <xsl:template match="amcr_old:dokument/amcr_old:extra_data/amcr_old:geom_system"/>
    <xsl:template match="amcr_old:dokument//amcr_old:extra_data/amcr_old:geom_sjtsk_gml"/>
    <xsl:template match="amcr_old:dokument/amcr_old:extra_data/amcr_old:geom_sjtsk_wkt"/>
    <xsl:template match="amcr_old:lokalita/amcr_old:igsn"/>
    <xsl:template match="amcr_old:ext_zdroj/amcr_old:doi"/>
    <xsl:template match="amcr_old:samostatny_nalez/amcr_old:igsn"/>
    <xsl:template match="amcr_old:uzivatel/amcr_old:orcid"/>
    <xsl:template match="amcr_old:organizace/amcr_old:ror"/>
    <xsl:template match="amcr_old:osoba/amcr_old:orcid"/>
    <xsl:template match="amcr_old:osoba/amcr_old:wikidata"/>
    <xsl:template match="amcr_old:heslo/amcr_old:odkaz/amcr_old:scheme_uri"/>

</xsl:stylesheet>
