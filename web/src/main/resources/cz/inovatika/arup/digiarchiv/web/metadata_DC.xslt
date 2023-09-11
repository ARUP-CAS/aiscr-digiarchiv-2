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
        <dc:title xml:lang="cs">AMČR - projekt <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - projekt "{amcr:projekt/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:projekt/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">projekt</dc:subject> <!-- "projekt" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:projekt/amcr:stav} -->
        <dc:type>
            <xsl:value-of select="$base_url"/>
            <xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:pristupnost_pom[@id]} -->
        <xsl:for-each select="amcr:typ_projektu">
            <dc:type>
                <xsl:value-of select="$base_url"/>
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
                <xsl:value-of select="$base_url"/>/id/
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
            <xsl:value-of select="$base_url"/>/id/
            <xsl:value-of select="amcr:chranene_udaje/amcr:hlavni_katastr/@id"/>
        </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>/id/
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
            <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:projekt/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:projekt/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:projekt/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        
        <xsl:for-each select="amcr:soubor/amcr:path">
            <dc:relation>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="."/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:soubor/amcr:path} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:archeologicky_zaznam">
            <dc:relation>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:archeologicky_zaznam[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:samostatny_nalez">
            <dc:relation>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:samostatny_nalez[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:projekt/amcr:dokument[@id]} -->
        </xsl:for-each>
                    
    </xsl:template>
    
    <xsl:template match="//amcr:archeologicky_zaznam">
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
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_vedouci">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedouci_akce_ostatni">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="amcr:vedouci/@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedouci_akce_ostatni/amcr:vedouci[@id]} -->
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
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
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedlejsi_typ">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedlejsi_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:typ_lokality">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:typ_lokality[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:druh">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:druh[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:zachovalost">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:zachovalost[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:jistota">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:jistota[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:komponenta">
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./amcr:obdobi/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:obdobi[@id]} -->
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./amcr:areal/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:areal[@id]} -->
            <dc:subject>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./amcr:aktivita/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:aktivita[@id]} -->
            <xsl:for-each select="amcr:nalez_objekt">
                <dc:subject>
                    <xsl:value-of select="$base_url"/>/id/
                    <xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:druh[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url"/>/id/
                    <xsl:value-of select="./amcr:specifikace/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:specifikace[@id]} -->
            </xsl:for-each>
            <xsl:for-each select="amcr:nalez_predmet">
                <dc:subject>
                    <xsl:value-of select="$base_url"/>/id/
                    <xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:druh[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url"/>/id/
                    <xsl:value-of select="./amcr:specifikace/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:specifikace[@id]} -->
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:hlavni_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>/id/
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
            <xsl:value-of select="$base_url"/>/id/
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <xsl:for-each select="amcr:historie">
            <dc:date>
                <xsl:value-of select="./amcr:datum_zmeny"/>
            </dc:date> <!-- {amcr:archeologicky_zaznam/amcr:historie/amcr:datum_zmeny} -->
            <dc:creator>
                <xsl:value-of select="$base_url"/>/id/<xsl:value-of select="./amcr:uzivatel/@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:historie/amcr:uzivatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:pian">
            `<dc:relation>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:pian[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:adb">
            `<dc:relation>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:adb[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ext_odkaz/amcr:ext_zdroj">
            `<dc:relation>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ext_odkaz/amcr:ext_zdroj[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument">
            `<dc:relation>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument[@id]} -->
        </xsl:for-each>
            
    </xsl:template>
    
    
    
    <xsl:template match="//amcr:let">
        <dc:title xml:lang="cs">AMČR - let <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - {amcr:let záznam {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:let/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">let</dc:subject> <!-- "let" -->
        <dc:coverage>
            <xsl:value-of select="amcr:datum"/>
        </dc:coverage> <!-- {amcr:let/amcr:datum} -->
  
        <xsl:for-each select="aamcr:pozorovatel">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:pozorovatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:organizace[@id]} -->
        </xsl:for-each>
        
        <dc:description>
            <xsl:value-of select="amcr:ucel_letu"/>
        </dc:description> <!-- {amcr:let/amcr:ucel_letu} -->
        <xsl:for-each select="amcr:letiste_start">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:let/amcr:letiste_start[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:letiste_cil">
            <dc:coverage>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:let/amcr:letiste_cil[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:pocasi">
            <dc:description>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:description> <!-- [base_url]"/id/"{amcr:let/amcr:pocasi[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dohlednost">
            <dc:description>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:description> <!-- [base_url]"/id/"{amcr:let/amcr:dohlednost[@id]} -->
        </xsl:for-each>
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url"/>/id/
            <xsl:value-of select="ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:let/amcr:ident_cely} -->
        <dc:date>
            <xsl:value-of select="amcr:datum"/>
        </dc:date> <!-- {amcr:let/amcr:datum} -->
        <dc:creator>
            <xsl:value-of select="$base_url"/>/id/
            <xsl:value-of select="amcr:pozorovatel/@id"/>
        </dc:creator> <!-- [base_url]"/id/"{amcr:let/amcr:pozorovatel[@id]} -->
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url"/>/id/
                <xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:let/amcr:dokument[@id]} -->
        </xsl:for-each>
  
  
    </xsl:template>
    
    <xsl:template name="clean">
        <xsl:copy>
            <xsl:apply-templates 
                select="node()[boolean(normalize-space())]
                         |@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>