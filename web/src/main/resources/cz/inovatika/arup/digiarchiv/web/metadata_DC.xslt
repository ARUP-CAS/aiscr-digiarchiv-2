<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="mods"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:srw_dc="info:srw/schema/1/dc-schema"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:gml="https://www.opengis.net/gml/3.2" xmlns:amcr="https://api.aiscr.cz/schema/amcr/2.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
    <xsl:variable name="base_url">http://base_url/</xsl:variable>
    <xsl:variable name="base_url_id"><xsl:value-of select="$base_url"/>id/</xsl:variable>
    <xsl:template match="/">
        <oai_dc:dc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
            <oai_dc:dc
                xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
                <xsl:apply-templates select="amcr:amcr" />
            </oai_dc:dc>
        </oai_dc:dc>
        
    </xsl:template>
    
    
    <!-- dokument -->
    <xsl:template  match="amcr:dokument">
        <dc:title xml:lang="cs">AMČR - dokument <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - projekt "{amcr:dokument/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:dokument/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">dokument</dc:subject> <!-- "projekt" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:dokument/amcr:stav} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:typ_dokumentu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:typ_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:material_originalu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:material_originalu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:rada">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:rada[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:posudek">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:posudek[@id]} -->
        </xsl:for-each>
        <dc:date>
            <xsl:value-of select="amcr:rok_vzniku"/>
        </dc:date> <!-- {amcr:dokument/amcr:rok_vzniku} -->
        <xsl:for-each select="amcr:autor">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:dokument/amcr:autor[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:dokument/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:jazyk_dokumentu">
            <dc:language>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:language> <!-- [base_url]"/id/"{amcr:dokument/amcr:jazyk_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ulozeni_originalu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:ulozeni_originalu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:oznaceni_originalu">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- [base_url]"/id/"{amcr:dokument/amcr:oznaceni_originalu} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:popis">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- [base_url]"/id/"{amcr:dokument/amcr:popis} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:poznamka">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- [base_url]"/id/"{amcr:dokument/amcr:poznamka} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:licence">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- [base_url]"/id/"{amcr:dokument/amcr:licence} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:osoba">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:osoba[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:extra_data">
            <xsl:if test="amcr:cislo_objektu">
            <dc:description>
                <xsl:value-of select="amcr:cislo_objektu"/>
            </dc:description> <!-- {amcr:dokument/amcr:extra_data/amcr:cislo_objektu} -->
            </xsl:if>
            <xsl:if test="amcr:format">
            <dc:format>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:format/@id"/>
            </dc:format> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:format[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:meritko">
            <dc:format>
                <xsl:value-of select="amcr:meritko"/>
            </dc:format> <!-- {amcr:dokument/amcr:extra_data/amcr:meritko} -->
            </xsl:if>
            <xsl:if test="amcr:vyska">
            <dc:format>
                <xsl:value-of select="amcr:vyska"/>
            </dc:format> <!-- {amcr:dokument/amcr:extra_data/amcr:vyska} -->
            </xsl:if>
            <xsl:if test="amcr:sirka">
            <dc:format>
                <xsl:value-of select="amcr:sirka"/>
            </dc:format> <!-- {amcr:dokument/amcr:extra_data/amcr:sirka} -->
            </xsl:if>
            <xsl:if test="amcr:zachovalost">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:zachovalost/@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:zachovalost[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:nahrada">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:nahrada/@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:nahrada[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:odkaz">
            <dc:relation>
                <xsl:value-of select="amcr:odkaz"/>
            </dc:relation> <!-- {amcr:dokument/amcr:extra_data/amcr:odkaz} -->
            </xsl:if>
            <xsl:if test="amcr:udalost_typ">
            <dc:coverage>
                <xsl:value-of select="amcr:udalost_typ"/>
            </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:datum_vzniku} -->
            </xsl:if>
            <xsl:if test="amcr:udalost_typ">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:udalost_typ/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:udalost_typ[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:udalost">
            <dc:description>
                <xsl:value-of select="amcr:udalost"/>
            </dc:description> <!-- {amcr:dokument/amcr:extra_data/amcr:udalost -->
            </xsl:if>
            <xsl:if test="amcr:zeme">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:zeme/@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:zeme[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:region">
            <dc:coverage>
                <xsl:value-of select="amcr:region"/>
            </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:region} -->
            </xsl:if>
            <xsl:if test="amcr:rok_od">
            <dc:coverage>
                <xsl:value-of select="amcr:rok_od"/>
            </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:rok_od} -->
            </xsl:if>
            <xsl:if test="amcr:rok_do">
            <dc:coverage>
                <xsl:value-of select="amcr:rok_do"/>
            </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:rok_do} -->
            </xsl:if>
            <xsl:if test="amcr:geom_wkt">
            <dc:coverage>
                <xsl:value-of select="amcr:geom_wkt"/>
            </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:geom_wkt} -->
            </xsl:if>
        </xsl:for-each>
        
        <xsl:for-each select="amcr:tvar">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:tvar/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:tvar/amcr:tvar[@id]} -->
        </xsl:for-each>
        
        <xsl:for-each select="amcr:dokument_cast">
            <xsl:for-each select="amcr:komponenta">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="amcr:obdobi/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:obdobi[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="amcr:areal/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:areal[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="amcr:aktivita/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:aktivita[@id]} -->
                <xsl:for-each select="amcr:nalez_objekt">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/>
                        <xsl:value-of select="amcr:druh/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_objekt/amcr:druh[@id]} -->
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/>
                        <xsl:value-of select="amcr:specifikace/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_objekt/amcr:specifikace[@id]} -->
                </xsl:for-each>
                <xsl:for-each select="amcr:nalez_predmet">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/>
                        <xsl:value-of select="amcr:druh/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_predmet/amcr:druh[@id]} -->
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/>
                        <xsl:value-of select="amcr:specifikace/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_predmet/amcr:specifikace[@id]} -->
                </xsl:for-each>
            </xsl:for-each>
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:dokument/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:dokument/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:dokument/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:let">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:dokument/amcr:let[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:soubor/amcr:path">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="."/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:dokument/amcr:soubor/amcr:path} -->
        </xsl:for-each>
        
        <xsl:for-each select="amcr:dokument_cast">
            <xsl:for-each select="amcr:archeologicky_zaznam">
                <dc:relation>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
                </dc:relation> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:archeologicky_zaznam[@id]} -->
            </xsl:for-each>
            <xsl:for-each select="amcr:projekt">
                <dc:relation>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
                </dc:relation> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:projekt[@id]} -->
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template match="amcr:projekt">
        <dc:title xml:lang="cs">AMČR - projekt <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - projekt "{amcr:projekt/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:projekt/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">projekt</dc:subject> <!-- "projekt" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:projekt/amcr:stav} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:pristupnost_pom[@id]} -->
        <xsl:for-each select="amcr:typ_projektu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:typ_projektu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:okres[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:podnet">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:projekt/amcr:podnet} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:vedouci_projektu">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:vedouci_projektu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:uzivatelske_oznaceni">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:projekt/amcr:uzivatelske_oznaceni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:oznaceni_stavby">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:projekt/amcr:oznaceni_stavby} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:kulturni_pamatka">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:kulturni_pamatka[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_zahajeni">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:datum_zahajeni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_ukonceni">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:datum_ukonceni} -->
        </xsl:for-each>
        <dc:coverage>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:chranene_udaje/amcr:hlavni_katastr/@id"/>
        </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:dalsi_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:lokalizace">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:lokalizace} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:parcelni_cislo">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:parcelni_cislo} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:geom_wkt">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:geom_wkt} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:kulturni_pamatka_cislo">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_cislo} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:kulturni_pamatka_popis">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_popis} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/"" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:projekt/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:projekt/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:projekt/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        
        <xsl:for-each select="amcr:soubor/amcr:path">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="."/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:soubor/amcr:path} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:archeologicky_zaznam">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:archeologicky_zaznam[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:samostatny_nalez">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:samostatny_nalez[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:dokument[@id]} -->
        </xsl:for-each>
                    
    </xsl:template>
    
    <xsl:template match="amcr:archeologicky_zaznam">
        <dc:title xml:lang="cs">AMČR - archeologický záznam <xsl:value-of select="amcr:ident_cely"/></dc:title>        <!-- AMČR - archeologický záznam {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">archeologický záznam</dc:subject> <!-- archeologický záznam -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- Stav: {amcr:archeologicky_zaznam/amcr:stav} -->
        <dc:type>
            <xsl:value-of select="$base_url"/>
            <xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:okres[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_vedouci">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_vedouci">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedouci_akce_ostatni">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:vedouci/@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedouci_akce_ostatni/amcr:vedouci[@id]} -->
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="amcr:organizace/@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedouci_akce_ostatni/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:datum_zahajeni">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:archeologicky_zaznam/amcr:akce/amcr:datum_zahajeni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:datum_ukonceni">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:archeologicky_zaznam/amcr:akce/amcr:datum_ukonceni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_typ">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedlejsi_typ">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedlejsi_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:typ_lokality">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:typ_lokality[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:druh">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:druh[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:zachovalost">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:zachovalost[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:jistota">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:jistota[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:komponenta">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./amcr:obdobi/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:obdobi[@id]} -->
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./amcr:areal/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:areal[@id]} -->
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./amcr:aktivita/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:aktivita[@id]} -->
            <xsl:for-each select="amcr:nalez_objekt">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:druh[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:specifikace/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:specifikace[@id]} -->
            </xsl:for-each>
            <xsl:for-each select="amcr:nalez_predmet">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:druh[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:specifikace/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:specifikace[@id]} -->
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:hlavni_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:dalsi_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:uzivatelske_oznaceni">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:uzivatelske_oznaceni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:chranene_udaje/amcr:lokalizace_okolnosti">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:archeologicky_zaznam/amcr:akce/amcr:chranene_udaje/amcr:lokalizace_okolnosti} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:chranene_udaje/amcr:souhrn_upresneni">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:archeologicky_zaznam/amcr:akce/amcr:chranene_udaje/amcr:souhrn_upresneni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:chranene_udaje/amcr:nazev">
            <dc:title>
                <xsl:value-of select="."/>
            </dc:title> <!-- {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:nazev} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:chranene_udaje/amcr:popis">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:popis} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:chranene_udaje/amcr:poznamka">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:poznamka} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:archeologicky_zaznam/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:pian">
            `<dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:pian[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:adb">
            `<dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:adb[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ext_odkaz/amcr:ext_zdroj">
            `<dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ext_odkaz/amcr:ext_zdroj[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument">
            `<dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument[@id]} -->
        </xsl:for-each>
            
    </xsl:template>
    
    <xsl:template match="amcr:let">
        <dc:title xml:lang="cs">AMČR - let <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - {amcr:let záznam {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:let/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">let</dc:subject> <!-- "let" -->
        <dc:coverage>
            <xsl:value-of select="amcr:datum"/>
        </dc:coverage> <!-- {amcr:let/amcr:datum} -->
  
        <xsl:for-each select="amcr:pozorovatel">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:pozorovatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:organizace[@id]} -->
        </xsl:for-each>
        
        <dc:description>
            <xsl:value-of select="amcr:ucel_letu"/>
        </dc:description> <!-- {amcr:let/amcr:ucel_letu} -->
        <xsl:for-each select="amcr:letiste_start">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:let/amcr:letiste_start[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:letiste_cil">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:let/amcr:letiste_cil[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:pocasi">
            <dc:description>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:description> <!-- [base_url]"/id/"{amcr:let/amcr:pocasi[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dohlednost">
            <dc:description>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:description> <!-- [base_url]"/id/"{amcr:let/amcr:dohlednost[@id]} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:let/amcr:ident_cely} -->
        <dc:date>
            <xsl:value-of select="amcr:datum"/>
        </dc:date> <!-- {amcr:let/amcr:datum} -->
        <dc:creator>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pozorovatel/@id"/>
        </dc:creator> <!-- [base_url]"/id/"{amcr:let/amcr:pozorovatel[@id]} -->
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:let/amcr:dokument[@id]} -->
        </xsl:for-each>
  
    </xsl:template>
    
    <xsl:template match="amcr:adb">
        <dc:title xml:lang="cs">AMČR - archeologický dokumentační bod <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - {amcr:let záznam {amcr:adb/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:adb/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">archeologický dokumentační bod</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:adb/amcr:stav_pom} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:adb/amcr:pristupnost_pom[@id]} -->
        <xsl:for-each select="amcr:typ_sondy">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:typ_sondy[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:podnet">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:tpodnet[@id]} -->
        </xsl:for-each>
        <dc:coverage><xsl:value-of select="amcr:sm5"/></dc:coverage> <!-- {amcr:adb/amcr:sm5} -->
        <xsl:for-each select="amcr:chranene_udaje">
            <dc:description><xsl:value-of select="amcr:uzivatelske_oznaceni_sondy"/></dc:description> <!-- {amcr:adb/amcr:chranene_udaje/amcr:uzivatelske_oznaceni_sondy} -->
            <dc:coverage><xsl:value-of select="amcr:trat"/></dc:coverage> <!-- {amcr:adb/amcr:chranene_udaje/amcr:trat} -->
            <dc:coverage><xsl:value-of select="amcr:cislo_popisne"/></dc:coverage> <!-- {amcr:adb/amcr:chranene_udaje/amcr:cislo_popisne} -->
            <dc:coverage><xsl:value-of select="amcr:parcelni_cislo"/></dc:coverage> <!-- {amcr:adb/amcr:chranene_udaje/amcr:parcelni_cislo} -->
            <dc:description><xsl:value-of select="amcr:poznamka"/></dc:description> <!-- {amcr:adb/amcr:chranene_udaje/amcr:poznamka} -->
            <xsl:for-each select="amcr:vyskovy_bod">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:typ/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:chranene_udaje/amcr:vyskovy_bod/amcr:typ[@id]} -->
                <dc:coverage>
                    <xsl:value-of select="$base_url_id"/>
                    <xsl:value-of select="./amcr:geom_wkt"/>
                </dc:coverage> <!-- {amcr:adb/amcr:chranene_udaje/amcr:vyskovy_bod/amcr:geom_wkt} -->
            </xsl:for-each>
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        
        <dc:source>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:adb/amcr:ident_cely} -->
        <dc:date>
            <xsl:value-of select="amcr:rok_popisu"/>
        </dc:date> <!-- {amcr:adb/amcr:rok_popisu} -->
        <dc:creator>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:autor_popisu/@id"/>
        </dc:creator> <!-- [base_url]"/id/"{amcr:let/amcr:autor_popisu[@id]} -->
        <dc:date>
            <xsl:value-of select="amcr:rok_revize"/>
        </dc:date> <!-- {amcr:adb/amcr:rok_revize} -->
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:let/amcr:dokument[@id]} -->
        </xsl:for-each>
        <dc:creator>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:autor_revize/@id"/>
        </dc:creator> <!-- [base_url]"/id/"{amcr:let/amcr:autor_revize[@id]} -->
        <dc:relation>
                      <xsl:value-of select="$base_url_id"/>
                      <xsl:value-of select="amcr:dokumentacni_jednotka/@id"/>
        </dc:relation> <!-- [base_url]"/id/"{amcr:adb/amcr:dokumentacni_jednotka[@id]} -->
  
    </xsl:template>
    
      <!-- ext_zdroj -->
      <xsl:template match="amcr:ext_zdroj">
        <dc:title xml:lang="cs">AMČR - externí zdroj <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - externí zdroj {amcr:ext_zdroj/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:ext_zdroj/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">externí zdroj</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:ext_zdroj/amcr:stav_pom} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:typ/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:typ[@id]} -->
        <xsl:for-each select="amcr:sysno">
            <dc:identifier>SYSNO: <xsl:value-of select="."/></dc:identifier> <!-- "SYSNO: "{amcr:ext_zdroj/amcr:sysno} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:autor">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:autor[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:nazev">
            <dc:title><xsl:value-of select="."/></dc:title> <!-- {amcr:ext_zdroj/amcr:nazev} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:edice_rada">
            <dc:title><xsl:value-of select="."/></dc:title> <!-- {amcr:ext_zdroj/amcr:edice_rada} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:rok_vydani_vzniku">
            <dc:date><xsl:value-of select="."/></dc:date> <!-- amcr:ext_zdroj/amcr:rok_vydani_vzniku} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:isbn">
            <dc:identifier>ISBN: <xsl:value-of select="."/></dc:identifier> <!-- "ISBN: "{amcr:ext_zdroj/amcr:isbn} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:issn">
            <dc:identifier>ISSN: <xsl:value-of select="."/></dc:identifier> <!-- "ISSN: "{amcr:ext_zdroj/amcr:issn} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:vydavatel">
            <dc:publisher><xsl:value-of select="."/></dc:publisher> <!-- amcr:ext_zdroj/amcr:vydavatel} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:editor">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:editor[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:sbornik_nazev">
            <dc:title><xsl:value-of select="."/></dc:title> <!-- amcr:ext_zdroj/amcr:sbornik_nazev} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:misto">
            <dc:description><xsl:value-of select="."/></dc:description> <!-- amcr:ext_zdroj/amcr:misto} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:casopis_denik_nazev">
            <dc:title><xsl:value-of select="."/></dc:title> <!-- amcr:ext_zdroj/amcr:casopis_denik_nazev} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_rd">
            <dc:date><xsl:value-of select="."/></dc:date> <!-- amcr:ext_zdroj/amcr:datum_rd} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:link">
            <dc:relation><xsl:value-of select="."/></dc:relation> <!-- amcr:ext_zdroj/amcr:link} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:typ_dokumentu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:typ_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/>
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:poznamka">
            <dc:description><xsl:value-of select="."/></dc:description> <!-- amcr:ext_zdroj/amcr:poznamka} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:ext_zdroj/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ext_odkaz">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:archeologicky_zaznam/@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:ext_odkaz/amcr:archeologicky_zaznam[@id]} -->
        </xsl:for-each>
        
      </xsl:template>

  <!-- pian -->
      <xsl:template match="amcr:pian">
        <dc:title xml:lang="cs">AMČR - prostorová jednotka <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - prostorová jednotka {amcr:pian/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:pian/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">PIAN</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:pian/amcr:stav_pom} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:pristupnost_pom[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:typ/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:typ[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:presnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:presnost[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:geom_system/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:geom_system[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje">
            <dc:coverage>
                <xsl:value-of select="amcr:zm10"/>
            </dc:coverage> <!-- {amcr:pian/amcr:chranene_udaje/amcr:zm10} -->
            <dc:coverage>
                <xsl:value-of select="amcr:geom_wkt"/>
            </dc:coverage> <!-- {amcr:pian/amcr:chranene_udaje/amcr:geom_wkt} -->
            <dc:coverage>
                <xsl:value-of select="amcr:geom_sjtsk_wkt"/>
            </dc:coverage> <!-- {amcr:pian/amcr:chranene_udaje/amcr:geom_sjtsk_wkt} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:pian/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:pian/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:pian/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:pian/amcr:dokumentacni_jednotka[@id]} -->
        </xsl:for-each>
        
        
    </xsl:template>
        
  
  <!-- samostatny_nalez -->
      <xsl:template match="amcr:samostatny_nalez">
        <dc:title xml:lang="cs">AMČR - samostatný nález <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - samostatný nález {amcr:samostatny_nalez/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:samostatny_nalez/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">samostatný nález</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:samostatny_nalez/amcr:stav_pom} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:evidencni_cislo">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:samostatny_nalez/amcr:evidencni_cislo} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:okres[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:okolnosti">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- {amcr:samostatny_nalez/amcr:okolnosti[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:obdobi">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- {amcr:samostatny_nalez/amcr:obdobi[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:druh_nalezu">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- {amcr:samostatny_nalez/amcr:druh_nalezu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:specifikace">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- {amcr:samostatny_nalez/amcr:specifikace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:poznamka">
            <dc:description>
                <xsl:value-of select="."/>
            </dc:description> <!-- {amcr:samostatny_nalez/amcr:poznamka} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:nalezce">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- {amcr:samostatny_nalez/amcr:nalezce[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_nalezu">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:datum_nalezu} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:predano_organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- {amcr:samostatny_nalez/amcr:predano_organizace[@id]} -->
        </xsl:for-each>
        <xsl:if test="amcr:chranene_udaje/amcr:katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:chranene_udaje/amcr:katastr/@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:chranene_udaje/amcr:katastr[@id]} -->
        </xsl:if>
        <xsl:if test="amcr:chranene_udaje/amcr:lokalizace">
            <dc:coverage>
                <xsl:value-of select="amcr:chranene_udaje/amcr:lokalizace"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:chranene_udaje/amcr:lokalizace} -->
        </xsl:if>
        <xsl:if test="amcr:chranene_udaje/amcr:geom_wkt">
            <dc:coverage>
                <xsl:value-of select="amcr:chranene_udaje/amcr:geom_wkt"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:chranene_udaje/amcr:geom_wkt} -->
        </xsl:if>
        <xsl:if test="amcr:chranene_udaje/amcr:geom_sjtsk_wkt">
            <dc:coverage>
                <xsl:value-of select="amcr:chranene_udaje/amcr:geom_sjtsk_wkt"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:chranene_udaje/amcr:geom_sjtsk_wkt} -->
        </xsl:if>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:samostatny_nalez/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:projekt">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:projekt[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:soubor">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:path"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:soubor/amcr:path} -->
        </xsl:for-each>
        
      </xsl:template>
      
      <!-- uzivatel -->
      <xsl:template match="amcr:uzivatel">
        <dc:title xml:lang="cs">AMČR - uživatel <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - uživatel {amcr:uzivatel/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:uzivatel/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">uživatel</dc:subject> 
        <dc:description><xsl:value-of select="amcr:jmeno"/></dc:description> <!-- {amcr:uzivatel/amcr:jmeno} -->
        <dc:description><xsl:value-of select="amcr:prijmeni"/></dc:description> <!-- {amcr:uzivatel/amcr:prijmeni} -->
        <dc:description><xsl:value-of select="amcr:email"/></dc:description> <!-- {amcr:uzivatel/amcr:email} -->
        <dc:subject>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:osoba/@id"/>
        </dc:subject> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:osoba[@id]} -->
        <dc:description><xsl:value-of select="amcr:telefon"/></dc:description> <!-- {amcr:uzivatel/amcr:telefon} -->
        <xsl:for-each select="amcr:skupina">
            <dc:type>
                <xsl:value-of select="."/>
            </dc:type> <!-- {amcr:uzivatel/amcr:skupina} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- {amcr:uzivatel/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:uzivatel/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        
        <xsl:for-each select="amcr:organizace">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:vedouci">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:spolupracovnik">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:spolupracovnik[@id]} -->
        </xsl:for-each>
        
      </xsl:template>
      
  
  <!-- heslo -->
      <xsl:template match="amcr:heslo">
        <dc:title xml:lang="cs">AMČR - heslo <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - heslo {amcr:heslo/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:heslo/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">heslo</dc:subject> <!-- "heslo" -->
        <dc:subject><xsl:value-of select="amcr:nazev_heslare"/></dc:subject> <!-- {amcr:heslo/amcr:nazev_heslare} -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:heslo"/></dc:title> <!-- AMČR - heslo {amcr:heslo/amcr:heslo} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:heslo_en"/></dc:title> <!-- AMČR - heslo {amcr:heslo/amcr:heslo_en} -->
        <dc:description xml:lang="cs"><xsl:value-of select="amcr:popis"/></dc:description> <!-- {amcr:heslo/amcr:popis} -->
        <dc:description xml:lang="en"><xsl:value-of select="amcr:popis_en"/></dc:description> <!-- {amcr:heslo/amcr:popis_en} -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:zkratka"/></dc:title> <!-- AMČR - heslo {amcr:heslo/amcr:zkratka} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:zkratka_en"/></dc:title> <!-- AMČR - heslo {amcr:heslo/amcr:zkratka_en} -->
        
        <xsl:for-each select="amcr:odkaz/amcr:uri">
            <dc:relation>
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- {amcr:heslo/amcr:odkaz/amcr:uri[@id]} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- {amcr:heslo/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:heslo/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:heslo/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:hierarchie_vyse/amcr:heslo_nadrazene">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:hierarchie_vyse/amcr:heslo_nadrazene[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:hierarchie_nize/amcr:heslo_nadrazene">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:hierarchie_nize/amcr:heslo_nadrazene[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument_typ_material_rada/amcr:dokument_typ">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:dokument_typ_material_rada/amcr:dokument_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument_typ_material_rada/amcr:dokument_material">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:dokument_typ_material_rada/amcr:dokument_material[@id]} -->
        </xsl:for-each>
      </xsl:template>
      

  <!-- ruian_kraj -->
      <xsl:template match="amcr:ruian_kraj">
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev"/></dc:title> <!-- {amcr:ruian_kraj/amcr:nazev} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:ruian_kraj/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">kraj</dc:subject> <!-- "kraj" -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:ruian_kraj/amcr:nazev_en} -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_wkt"/></dc:coverage> <!-- {amcr:ruian_kraj/amcr:definicni_bod_wkt} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_wkt"/></dc:coverage> <!-- {amcr:ruian_kraj/amcr:hranice_wkt} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:kod"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:ruian_kraj/amcr:kod -->
        <dc:source>https://www.cuzk.cz/ruian/</dc:source> <!-- "https://www.cuzk.cz/ruian/" -->
        <dc:creator>https://www.cuzk.cz/</dc:creator> <!-- "https://www.cuzk.cz/" -->
      </xsl:template>
      

  <!-- ruian_okres -->
      <xsl:template match="amcr:ruian_okres">
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev"/></dc:title> <!-- {amcr:ruian_okres/amcr:nazev} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:ruian_okres/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">okres</dc:subject> <!-- "okres" -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:ruian_okres/amcr:nazev_en} -->
        <dc:title><xsl:value-of select="amcr:spz"/></dc:title> <!-- {amcr:ruian_okres/amcr:spz} -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_wkt"/></dc:coverage> <!-- {amcr:ruian_okres/amcr:definicni_bod_wkt} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_wkt"/></dc:coverage> <!-- {amcr:ruian_okres/amcr:hranice_wkt} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:kod"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:ruian_okres/amcr:kod -->
        <dc:source>https://www.cuzk.cz/ruian/</dc:source> <!-- "https://www.cuzk.cz/ruian/" -->
        <dc:creator>https://www.cuzk.cz/</dc:creator> <!-- "https://www.cuzk.cz/" -->
        <dc:relation><xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:kraj/@id"/></dc:relation> <!-- [base_url]"/id/"{amcr:ruian_okres/amcr:kraj[@id]} -->
      </xsl:template>
      

  <!-- ruian_katastr -->
      <xsl:template match="amcr:ruian_katastr">
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev"/></dc:title> <!-- {amcr:ruian_katastr/amcr:nazev} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:ruian_katastr/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">katastrální území</dc:subject> <!-- "katastrální území" -->
        
        <dc:title><xsl:value-of select="amcr:spz"/></dc:title> <!-- {amcr:ruian_katastr/amcr:spz} -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_wkt"/></dc:coverage> <!-- {amcr:ruian_katastr/amcr:definicni_bod_wkt} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_wkt"/></dc:coverage> <!-- {amcr:ruian_katastr/amcr:hranice_wkt} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:kod"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:ruian_katastr/amcr:kod -->
        <dc:source>https://www.cuzk.cz/ruian/</dc:source> <!-- "https://www.cuzk.cz/ruian/" -->
        <dc:creator>https://www.cuzk.cz/</dc:creator> <!-- "https://www.cuzk.cz/" -->
        <dc:relation><xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:okres/@id"/></dc:relation> <!-- [base_url]"/id/"{amcr:ruian_katastr/amcr:okres[@id]} -->
        <dc:relation><xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pian/@id"/></dc:relation> <!-- [base_url]"/id/"{amcr:ruian_katastr/amcr:pian[@id]} -->
      </xsl:template>
      

  <!-- organizace -->
      <xsl:template match="amcr:organizace">
        <dc:title xml:lang="cs"> AMČR - organizace <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - organizace {amcr:organizace/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:organizace/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">organizace</dc:subject> <!-- "organizace" -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev"/></dc:title> <!-- {amcr:organizace/amcr:nazev} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:organizace/amcr:nazev_en} -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev_zkraceny"/></dc:title> <!-- {amcr:organizace/amcr:nazev_zkraceny} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_zkraceny_en"/></dc:title> <!-- {amcr:organizace/amcr:nazev_zkraceny_en} -->
        <dc:type><xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:typ_organizace/@id"/></dc:type> <!-- [base_url]"/id/"{amcr:organizace/amcr:typ_organizace[@id]} -->
        <dc:description><xsl:value-of select="amcr:email"/></dc:description> <!-- {amcr:organizace/amcr:email} -->
        <dc:description><xsl:value-of select="amcr:telefon"/></dc:description> <!-- {amcr:organizace/amcr:telefon} -->
        <dc:description><xsl:value-of select="amcr:adresa"/></dc:description> <!-- {amcr:organizace/amcr:adresa} -->
        <dc:identifier xml:lang="cs">IČO: <xsl:value-of select="amcr:ico"/></dc:identifier> <!-- {amcr:organizace/amcr:ico} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:organizace/amcr:ident_cely} -->
        <xsl:for-each select="amcr:soucast">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:organizace/amcr:soucast[@id]} -->
        </xsl:for-each>
      </xsl:template>

  <!-- osoba -->
      <xsl:template match="amcr:osoba">
        <dc:title xml:lang="cs">AMČR - osoba <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - osoba {amcr:osoba/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:osoba/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">osoba</dc:subject> 
        <dc:description><xsl:value-of select="amcr:jmeno"/></dc:description> <!-- {amcr:osoba/amcr:jmeno} -->
        <dc:description>
            <xsl:value-of select="amcr:prijmeni"/>
        </dc:description> <!-- {amcr:osoba/amcr:prijmeni} -->
        <dc:title><xsl:value-of select="amcr:vypis"/></dc:title> <!-- {amcr:osoba/amcr:vypis} -->
        <dc:title><xsl:value-of select="amcr:vypis_cely"/></dc:title> <!-- {amcr:osoba/amcr:vypis_cely} -->
        <dc:coverage><xsl:value-of select="amcr:rok_narozeni"/></dc:coverage> <!-- {amcr:osoba/amcr:rok_narozeni} -->
        <dc:coverage><xsl:value-of select="amcr:rok_umrti"/></dc:coverage> <!-- {amcr:osoba/amcr:rok_umrti} -->
        <dc:description><xsl:value-of select="amcr:rodne_prijmeni"/></dc:description> <!-- {amcr:osoba/amcr:rodne_prijmeni} -->
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:osoba/amcr:ident_cely} -->
      </xsl:template>
    
</xsl:stylesheet>