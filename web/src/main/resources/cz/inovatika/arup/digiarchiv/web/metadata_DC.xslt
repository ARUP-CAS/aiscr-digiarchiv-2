<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="mods"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:srw_dc="info:srw/schema/1/dc-schema"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:gml="https://www.opengis.net/gml/3.2" xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


        
        
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
        <xsl:variable name="base_url">http://base_url/id/</xsl:variable>
    <xsl:template match="/">
        <oai_dc:dc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
            <oai_dc:dc
                xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
                <xsl:apply-templates/>
            </oai_dc:dc>
        </oai_dc:dc>
        
    </xsl:template>
    
    <xsl:template match="//amcr:projekt">
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - projekt "{amcr:projekt/amcr:ident_cely} -->
        <dc:identifier><xsl:value-of select="amcr:ident_cely"/></dc:identifier> <!-- {amcr:projekt/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">projekt</dc:subject> <!-- "projekt" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:projekt/amcr:stav} -->
        <dc:type><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:pristupnost_pom/@id"/></dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:pristupnost_pom[@id]} -->
        <dc:type><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:typ_projektu/@id"/></dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:typ_projektu[@id]} -->
        <dc:coverage><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:okres/@id"/></dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:okres[@id]} -->
        <dc:description><xsl:value-of select="amcr:podnet"/></dc:description> <!-- {amcr:projekt/amcr:podnet} -->
        <dc:contributor><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:vedouci_projektu/@id"/></dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:vedouci_projektu[@id]} -->
        <dc:contributor><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:organizace/@id"/></dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:organizace[@id]} -->
        <dc:description><xsl:value-of select="amcr:uzivatelske_oznaceni"/></dc:description> <!-- {amcr:projekt/amcr:uzivatelske_oznaceni} -->
        <dc:description><xsl:value-of select="amcr:oznaceni_stavby"/></dc:description> <!-- {amcr:projekt/amcr:oznaceni_stavby} -->
        <dc:coverage><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:kulturni_pamatka/@id"/></dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:kulturni_pamatka[@id]} -->
        <dc:coverage><xsl:value-of select="amcr:datum_zahajeni"/></dc:coverage> <!-- {amcr:projekt/amcr:datum_zahajeni} -->
        <dc:coverage><xsl:value-of select="amcr:datum_ukonceni"/></dc:coverage> <!-- {amcr:projekt/amcr:datum_ukonceni} -->
        <dc:coverage><xsl:value-of select="$base_url"/><xsl:value-of select="amcr:chranene_udaje/amcr:hlavni_katastr/@id"/></dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:dalsi_katastr[@id]} -->
        </xsl:for-each>
        <dc:coverage><xsl:value-of select="amcr:chranene_udaje/amcr:lokalizace"/></dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:lokalizace} -->
        <dc:coverage><xsl:value-of select="amcr:chranene_udaje/amcr:parcelni_cislo"/></dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:parcelni_cislo} -->
        <dc:coverage><xsl:value-of select="amcr:chranene_udaje/amcr:geom_wkt"/></dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:geom_wkt} -->
        <dc:coverage>12345</dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_cislo} -->
        <dc:coverage>Hrad Hořovice</dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_popis} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/"" -->
        <dc:source>https://api.aiscr.cz/id/C-201901234</dc:source> <!-- [base_url]"/id/"{amcr:projekt/amcr:ident_cely} -->
        <dc:date>2023-02-01T16:58:50Z</dc:date> <!-- {amcr:projekt/amcr:historie/amcr:datum_zmeny} -->
        <dc:creator>https://api.aiscr.cz/id/U-123456</dc:creator> <!-- [base_url]"/id/"{amcr:projekt/amcr:historie/amcr:uzivatel[@id]} -->
        <dc:relation>http://fedora.aiscr.cz/rest/AMCR-test/C-201700002/file/39dc3dc9-aa6b-4034-becc-4f8cb1b94580</dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:soubor/amcr:path} -->
        <dc:relation>https://api.aiscr.cz/id/C-201901234A</dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:archeologicky_zaznam[@id]} -->
        <dc:relation>https://api.aiscr.cz/id/C-201901234-N00001</dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:samostatny_nalez[@id]} -->
        <dc:relation>https://api.aiscr.cz/id/C-TX-201901234</dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:dokument[@id]} -->
                    
    </xsl:template>
    
    <xsl:template name="clean">
     <xsl:copy>
             <xsl:apply-templates 
                  select="node()[boolean(normalize-space())]
                         |@*"/>
     </xsl:copy>
    </xsl:template>
</xsl:stylesheet>