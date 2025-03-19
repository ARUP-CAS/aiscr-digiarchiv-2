<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:gml="http://www.opengis.net/gml/3.2"
    xmlns:amcr_old="https://api.aiscr.cz/schema/amcr/2.0/"
    xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.1/" exclude-result-prefixes="amcr_old">

    <xsl:template match="amcr_old:amcr">
        <amcr:amcr xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:gml="http://www.opengis.net/gml/3.2"
            xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.1/" xsi:schemaLocation="https://api.aiscr.cz/schema/amcr/2.1/ https://api.aiscr.cz/schema/amcr/2.1/amcr.xsd">
            <xsl:apply-templates select="@*|node()"/>
        </amcr:amcr>
    </xsl:template>

    <xsl:template match="amcr_old:*">
        <xsl:element name="amcr:{local-name()}">
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

    <xsl:template match="amcr_old:projekt/amcr_old:pristupnost_pom">
        <amcr:pristupnost_pom>
            <xsl:apply-templates select="@*|node()"/>
        </amcr:pristupnost_pom>
        <amcr:geom_system>5514</amcr:geom_system>
    </xsl:template>

    <xsl:template match="amcr_old:extra_data">
        <amcr:extra_data>
            <xsl:choose>
                <!-- Pokud existuje geom_gml nebo geom_wkt, přidej geom_system před první z nich -->
                <xsl:when test="amcr_old:geom_gml or amcr_old:geom_wkt">
                    <xsl:variable name="first_geom_element" select="(amcr_old:geom_gml | amcr_old:geom_wkt)[1]" />
                    <xsl:for-each select="*">
                        <xsl:choose>
                            <!-- Před prvním geom_gml nebo geom_wkt vlož geom_system -->
                            <xsl:when test="generate-id(.) = generate-id($first_geom_element)">
                                <amcr:geom_system>4326</amcr:geom_system>
                                <xsl:element name="amcr:{local-name()}">
                                    <xsl:apply-templates select="@*|node()"/>
                                </xsl:element>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="amcr:{local-name()}">
                                    <xsl:apply-templates select="@*|node()"/>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:when>
                <!-- Pokud neexistuje geom_gml ani geom_wkt, přidej geom_system na konec -->
                <xsl:otherwise>
                    <xsl:apply-templates select="@*|node()"/>
                    <amcr:geom_system>4326</amcr:geom_system>
                </xsl:otherwise>
            </xsl:choose>
        </amcr:extra_data>
    </xsl:template>

    <!-- Odstraneni vybranych elementu -->
    <xsl:template match="amcr_old:pian/amcr_old:geom_updated_at"/>
    <xsl:template match="amcr_old:pian/amcr_old:geom_sjtsk_updated_at"/>
    <xsl:template match="amcr_old:samostatny_nalez/amcr_old:geom_updated_at"/>
    <xsl:template match="amcr_old:samostatny_nalez/amcr_old:geom_sjtsk_updated_at"/>

</xsl:stylesheet>
