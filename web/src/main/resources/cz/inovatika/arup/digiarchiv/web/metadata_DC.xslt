<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                xmlns:dc="http://purl.org/dc/elements/1.1/" 
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
                xmlns:gml="http://www.opengis.net/gml/3.2" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                exclude-result-prefixes="amcr"
                xmlns:amcr="##AMCRVERSION##"
>
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />
    <xsl:param name="base_url">https://api.aiscr.cz/</xsl:param>
    <xsl:variable name="base_url_id"><xsl:value-of select="$base_url"/>/id/</xsl:variable>
    <xsl:template match="/" >
        <oai_dc:dc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                   xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" 
                   xmlns:dc="http://purl.org/dc/elements/1.1/" 
                   xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
            <xsl:apply-templates select="amcr:amcr" />
        </oai_dc:dc>
    </xsl:template>
    
    
<!-- projekt -->
    <xsl:template match="amcr:projekt">
        <dc:title xml:lang="cs">AMČR - projekt <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - projekt "{amcr:projekt/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:projekt/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">projekt</dc:subject> <!-- "projekt" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:projekt/amcr:stav} -->
        <dc:description xml:lang="cs">Podnět: <xsl:value-of select="amcr:podnet"/></dc:description> <!-- Podnět: {amcr:projekt/amcr:podnet} -->
        <xsl:if test="amcr:uzivatelske_oznaceni"><dc:description xml:lang="cs">Označení projektu: <xsl:value-of select="amcr:uzivatelske_oznaceni"/></dc:description></xsl:if> <!-- Označení projektu: {amcr:projekt/amcr:uzivatelske_oznaceni} -->
        <xsl:if test="amcr:oznaceni_stavby"><dc:description xml:lang="cs">Označení stavby: <xsl:value-of select="amcr:oznaceni_stavby"/></dc:description></xsl:if> <!-- Označení stavby: {amcr:projekt/amcr:oznaceni_stavby} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:pristupnost_pom[@id]} -->
        <xsl:for-each select="amcr:typ_projektu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:projekt/amcr:typ_projektu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:okres[@id]} -->
        </xsl:for-each>
        <dc:coverage>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:chranene_udaje/amcr:hlavni_katastr/@id"/>
        </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:chranene_udaje/amcr:dalsi_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:lokalizace">
            <dc:coverage xml:lang="cs">Lokalizace: <xsl:value-of select="."/></dc:coverage> <!-- Lokalizace: {amcr:projekt/amcr:chranene_udaje/amcr:lokalizace} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:parcelni_cislo">
            <dc:coverage xml:lang="cs">Parcelní číslo: <xsl:value-of select="."/></dc:coverage> <!-- Parcelní číslo: {amcr:projekt/amcr:chranene_udaje/amcr:parcelni_cislo} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:vedouci_projektu">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:vedouci_projektu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:projekt/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:kulturni_pamatka">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:projekt/amcr:kulturni_pamatka[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:kulturni_pamatka_cislo">
            <dc:coverage xml:lang="cs">Číslo ÚSKP: <xsl:value-of select="."/></dc:coverage> <!-- Číslo ÚSKP: {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_cislo} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:kulturni_pamatka_popis">
            <dc:coverage xml:lang="cs">Název ÚSKP: <xsl:value-of select="."/></dc:coverage> <!-- Název ÚSKP: {amcr:projekt/amcr:chranene_udaje/amcr:kulturni_pamatka_popis} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_zahajeni">
            <dc:coverage xml:lang="cs">Datum zahájení: <xsl:value-of select="."/> <!-- Datum zahájení: {amcr:projekt/amcr:datum_zahajeni} --></dc:coverage>
        </xsl:for-each>
        <xsl:for-each select="amcr:datum_ukonceni">
            <dc:coverage xml:lang="cs">Datum ukončení: <xsl:value-of select="."/> <!-- Datum ukončení: {amcr:projekt/amcr:datum_ukonceni} --></dc:coverage>        
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:geom_gml">
            <dc:coverage>
                <xsl:value-of select="."/>
            </dc:coverage> <!-- {amcr:projekt/amcr:chranene_udaje/amcr:geom_gml} -->
        </xsl:for-each>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/"" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:projekt/amcr:ident_cely} -->
        
        <xsl:for-each select="amcr:soubor">
            <dc:relation>
                <xsl:value-of select="./amcr:url"/>
            </dc:relation> <!-- {amcr:projekt/amcr:soubor/amcr:url} -->
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


<!-- archeologicky_zaznam -->
    <xsl:template match="amcr:archeologicky_zaznam">
        <dc:title xml:lang="cs">AMČR - archeologický záznam <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - archeologický záznam {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <xsl:for-each select="amcr:lokalita/amcr:chranene_udaje/amcr:nazev">
            <dc:title>
                <xsl:value-of select="."/>
            </dc:title> <!-- {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:nazev} -->
        </xsl:for-each>
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:archeologicky_zaznam/amcr:ident_cely} -->
        <xsl:for-each select="amcr:lokalita/amcr:igsn">
            <dc:identifier>doi:<xsl:value-of select="."/></dc:identifier> <!-- doi:{amcr:archeologicky_zaznam/amcr:lokalita/amcr:igsn} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">archeologický záznam</dc:subject> <!-- archeologický záznam -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- Stav: {amcr:archeologicky_zaznam/amcr:stav} -->
        <xsl:if test="amcr:chranene_udaje/amcr:uzivatelske_oznaceni"><dc:description xml:lang="cs">Označení: <xsl:value-of select="amcr:chranene_udaje/amcr:uzivatelske_oznaceni"/></dc:description></xsl:if> <!-- Označení: {amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:uzivatelske_oznaceni} -->
        <xsl:if test="amcr:akce/amcr:chranene_udaje/amcr:lokalizace_okolnosti"><dc:description xml:lang="cs">Lokalizace/okolnosti: <xsl:value-of select="amcr:akce/amcr:chranene_udaje/amcr:lokalizace_okolnosti"/></dc:description></xsl:if> <!-- Lokalizace/okolnosti: {amcr:archeologicky_zaznam/amcr:akce/amcr:chranene_udaje/amcr:lokalizace_okolnosti} -->
        <xsl:if test="amcr:akce/amcr:chranene_udaje/amcr:souhrn_upresneni"><dc:description xml:lang="cs">Souhrn/upřesnění: <xsl:value-of select="amcr:akce/amcr:chranene_udaje/amcr:souhrn_upresneni"/></dc:description></xsl:if> <!-- Souhrn/upřesnění: {amcr:archeologicky_zaznam/amcr:akce/amcr:chranene_udaje/amcr:souhrn_upresneni} -->
        <xsl:if test="amcr:lokalita/amcr:chranene_udaje/amcr:popis"><dc:description xml:lang="cs">Popis: <xsl:value-of select="amcr:lokalita/amcr:chranene_udaje/amcr:popis"/></dc:description></xsl:if> <!-- Popis: {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:popis} -->
        <xsl:if test="amcr:lokalita/amcr:chranene_udaje/amcr:poznamka"><dc:description xml:lang="cs">Poznámka: <xsl:value-of select="amcr:lokalita/amcr:chranene_udaje/amcr:poznamka"/></dc:description></xsl:if> <!-- Poznámka: {amcr:archeologicky_zaznam/amcr:lokalita/amcr:chranene_udaje/amcr:poznamka} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:okres[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:hlavni_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:hlavni_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:chranene_udaje/amcr:dalsi_katastr">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:chranene_udaje/amcr:dalsi_katastr[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_vedouci">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_vedouci[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:organizace">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedouci_akce_ostatni">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:vedouci/@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedouci_akce_ostatni/amcr:vedouci[@id]} -->
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:organizace/@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedouci_akce_ostatni/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:datum_zahajeni">
            <dc:coverage xml:lang="cs">Datum zahájení: <xsl:value-of select="."/></dc:coverage> <!-- Datum zahájení: {amcr:archeologicky_zaznam/amcr:akce/amcr:datum_zahajeni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:datum_ukonceni">
            <dc:coverage xml:lang="cs">Datum ukončení: <xsl:value-of select="."/></dc:coverage> <!-- Datum ukončení: {amcr:archeologicky_zaznam/amcr:akce/amcr:datum_ukonceni} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:hlavni_typ">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:hlavni_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:akce/amcr:vedlejsi_typ">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:vedlejsi_typ[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:typ_lokality">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:lokalita/amcr:typ_lokality[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:druh">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:lokalita/amcr:druh[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:zachovalost">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:lokalita/amcr:zachovalost[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:lokalita/amcr:jistota">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:lokalita/amcr:jistota[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:komponenta">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:obdobi/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:obdobi[@id]} -->
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:areal/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:areal[@id]} -->
            <xsl:for-each select="amcr:aktivita">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:aktivita[@id]} -->
            </xsl:for-each>
            <xsl:for-each select="amcr:nalez_objekt">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:druh[@id]} -->
                <xsl:if test="amcr:specifikace">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:specifikace/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_objekt/amcr:specifikace[@id]} -->
                </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="amcr:nalez_predmet">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:druh/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:druh[@id]} -->
                <xsl:if test="amcr:specifikace">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:specifikace/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:komponenta/amcr:nalez_predmet/amcr:specifikace[@id]} -->
                </xsl:if>
            </xsl:for-each>
        </xsl:for-each>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ident_cely} -->

        <xsl:for-each select="amcr:akce/amcr:projekt">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:akce/amcr:projekt[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:pian">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:pian[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokumentacni_jednotka/amcr:adb">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokumentacni_jednotka/amcr:adb[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ext_odkaz/amcr:ext_zdroj">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:ext_odkaz/amcr:ext_zdroj[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument[@id]} -->
        </xsl:for-each>
    </xsl:template>


<!-- let -->
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
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:pozorovatel[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:let/amcr:organizace[@id]} -->
        </xsl:for-each>        
        <dc:description xml:lang="cs">Letiště - start: <xsl:value-of select="amcr:letiste_start"/></dc:description> <!-- Letiště - start: {amcr:let/amcr:letiste_start} -->
        <dc:description xml:lang="cs">Letiště - cíl: <xsl:value-of select="amcr:letiste_cil"/></dc:description> <!-- Letiště - cíl: {amcr:let/amcr:letiste_cil} -->
        <xsl:if test="amcr:ucel_letu"><dc:description xml:lang="cs">Účel letu: <xsl:value-of select="amcr:ucel_letu"/></dc:description></xsl:if> <!-- Účel letu: {amcr:let/amcr:ucel_letu} -->
        <xsl:if test="amcr:pocasi"><dc:description xml:lang="cs">Počasí: <xsl:value-of select="amcr:pocasi"/></dc:description></xsl:if> <!-- Počasí: {amcr:let/amcr:pocasi} -->
        <xsl:if test="amcr:dohlednost"><dc:description xml:lang="cs">Dohlednost: <xsl:value-of select="amcr:dohlednost"/></dc:description></xsl:if> <!-- Dohlednost: {amcr:let/amcr:dohlednost} -->

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:let/amcr:ident_cely} -->

        <xsl:for-each select="amcr:dokument">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:let/amcr:dokument[@id]} -->
        </xsl:for-each>  
    </xsl:template>


<!-- adb -->
    <xsl:template match="amcr:adb">
        <dc:title xml:lang="cs">AMČR - archeologický dokumentační bod <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - {amcr:let záznam {amcr:adb/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:adb/amcr:ident_cely} -->
        <dc:subject xml:lang="cs">archeologický dokumentační bod</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav_pom"/></dc:description> <!-- "Stav: "{amcr:adb/amcr:stav_pom} -->
        <xsl:if test="amcr:chranene_udaje/amcr:uzivatelske_oznaceni_sondy"><dc:description xml:lang="cs">Označení sondy: <xsl:value-of select="amcr:chranene_udaje/amcr:uzivatelske_oznaceni_sondy"/></dc:description></xsl:if> <!-- Označení sondy: {amcr:adb/amcr:chranene_udaje/amcr:uzivatelske_oznaceni_sondy} -->
        <xsl:if test="amcr:chranene_udaje/amcr:poznamka"><dc:description xml:lang="cs">Poznámka: <xsl:value-of select="amcr:chranene_udaje/amcr:poznamka"/></dc:description></xsl:if> <!-- {amcr:adb/amcr:chranene_udaje/amcr:poznamka} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:adb/amcr:pristupnost_pom[@id]} -->
        <xsl:for-each select="amcr:typ_sondy">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:typ_sondy[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:podnet">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:tpodnet[@id]} -->
        </xsl:for-each>
        <dc:coverage xml:lang="cs">Mapa SM5: <xsl:value-of select="amcr:sm5"/></dc:coverage> <!-- Mapa SM5: {amcr:adb/amcr:sm5} -->
            <xsl:if test="amcr:chranene_udaje/amcr:trat"><dc:coverage xml:lang="cs">Ulice/trať: <xsl:value-of select="amcr:chranene_udaje/amcr:trat"/></dc:coverage></xsl:if> <!-- Ulice/trať: {amcr:adb/amcr:chranene_udaje/amcr:trat} -->
            <xsl:if test="amcr:chranene_udaje/amcr:cislo_popisne"><dc:coverage xml:lang="cs">Číslo popisné: <xsl:value-of select="amcr:chranene_udaje/amcr:cislo_popisne"/></dc:coverage></xsl:if> <!-- Číslo popisné: {amcr:adb/amcr:chranene_udaje/amcr:cislo_popisne} -->
            <xsl:if test="amcr:chranene_udaje/amcr:parcelni_cislo"><dc:coverage xml:lang="cs">Parcelní číslo: <xsl:value-of select="amcr:chranene_udaje/amcr:parcelni_cislo"/></dc:coverage></xsl:if> <!-- Parcelní číslo: {amcr:adb/amcr:chranene_udaje/amcr:parcelni_cislo} -->
        <xsl:for-each select="amcr:chranene_udaje/amcr:vyskovy_bod">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./amcr:typ/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:adb/amcr:chranene_udaje/amcr:vyskovy_bod/amcr:typ[@id]} -->
            <dc:coverage>
                <xsl:value-of select="./amcr:geom_gml"/>
            </dc:coverage> <!-- {amcr:adb/amcr:chranene_udaje/amcr:vyskovy_bod/amcr:geom_gml} -->
        </xsl:for-each>
        <dc:date>
            <xsl:value-of select="amcr:rok_popisu"/>
        </dc:date> <!-- {amcr:adb/amcr:rok_popisu} -->
        <dc:creator>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:autor_popisu/@id"/>
        </dc:creator> <!-- [base_url]"/id/"{amcr:let/amcr:autor_popisu[@id]} -->

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:adb/amcr:ident_cely} -->

        <dc:relation>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:dokumentacni_jednotka/@id"/>
        </dc:relation> <!-- [base_url]"/id/"{amcr:adb/amcr:dokumentacni_jednotka[@id]} -->
    </xsl:template>


<!-- dokument -->
    <xsl:template match="amcr:dokument">
        <dc:title xml:lang="cs">AMČR - dokument <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- "AMČR - dokument "{amcr:dokument/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:dokument/amcr:ident_cely} -->
        <xsl:for-each select="amcr:doi">
            <dc:identifier>doi:<xsl:value-of select="."/></dc:identifier> <!-- doi:{amcr:dokument/amcr:doi} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">dokument</dc:subject> <!-- "dokument" -->
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:dokument/amcr:stav} -->
        <xsl:if test="amcr:oznaceni_originalu"><dc:description xml:lang="cs">Označení: <xsl:value-of select="amcr:oznaceni_originalu"/></dc:description></xsl:if> <!-- Označení: {amcr:dokument/amcr:oznaceni_originalu} -->
        <xsl:if test="amcr:popis"><dc:description xml:lang="cs">Popis: <xsl:value-of select="amcr:popis"/></dc:description></xsl:if> <!-- Popis: {amcr:dokument/amcr:popis} -->
        <xsl:if test="amcr:poznamka"><dc:description xml:lang="cs">Poznámka: <xsl:value-of select="amcr:poznamka"/></dc:description></xsl:if> <!-- Poznámka: {amcr:dokument/amcr:poznamka} -->
        <xsl:if test="amcr:extra_data/amcr:cislo_objektu"><dc:description xml:lang="cs">Objekt/kontext: <xsl:value-of select="amcr:extra_data/amcr:cislo_objektu"/></dc:description></xsl:if> <!-- Objekt/kontext: {amcr:dokument/amcr:extra_data/amcr:cislo_objektu} -->
        <xsl:if test="amcr:extra_data/amcr:udalost"><dc:description xml:lang="cs">Událost: <xsl:value-of select="amcr:extra_data/amcr:udalost"/></dc:description></xsl:if> <!-- {amcr:dokument/amcr:extra_data/amcr:udalost -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:popis"><dc:description xml:lang="cs">Popis akce: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:popis"/></dc:description></xsl:if> <!-- Popis akce: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:popis} -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:lokalizace"><dc:description xml:lang="cs">Lokalizace akce: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:lokalizace"/></dc:description></xsl:if> <!-- Lokalizace akce: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:lokalizace} -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:poznamka"><dc:description xml:lang="cs">Poznámka k akci: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:poznamka"/></dc:description></xsl:if> <!-- Poznámka k akci: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:poznamka} -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:rok_zahajeni"><dc:description xml:lang="cs">Rok zahájení akce: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:rok_zahajeni"/></dc:description></xsl:if> <!-- Rok zahájení akce: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:rok_zahajeni} -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:rok_ukonceni"><dc:description xml:lang="cs">Rok ukončení akce: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:rok_ukonceni"/></dc:description></xsl:if> <!-- Rok ukončení akce: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:rok_ukonceni} -->
        <xsl:if test="amcr:dokument_cast/amcr:neident_akce/amcr:pian"><dc:description xml:lang="cs">Související PIAN: <xsl:value-of select="amcr:dokument_cast/amcr:neident_akce/amcr:pian"/></dc:description></xsl:if> <!-- Související PIAN: {amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:pian} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:typ_dokumentu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:typ_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:material_originalu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:material_originalu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:rada">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:rada[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:posudek">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:posudek[@id]} -->
        </xsl:for-each>
        <dc:date>
            <xsl:value-of select="amcr:rok_vzniku"/>
        </dc:date> <!-- {amcr:dokument/amcr:rok_vzniku} -->
        <xsl:for-each select="amcr:extra_data/amcr:datum_vzniku">
            <dc:date>
                <xsl:value-of select="."/>
            </dc:date>
        </xsl:for-each> <!-- {amcr:dokument/amcr:extra_data/amcr:datum_vzniku} -->
        <xsl:for-each select="amcr:autor">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:dokument/amcr:autor[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:dokument/amcr:organizace[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:jazyk_dokumentu">
            <dc:language>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:language> <!-- [base_url]"/id/"{amcr:dokument/amcr:jazyk_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:ulozeni_originalu">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:ulozeni_originalu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:licence">
            <dc:type>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:licence[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:osoba">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:osoba[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:extra_data">
            <xsl:if test="amcr:format">
                <dc:format>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:format/@id"/>
                </dc:format> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:format[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:meritko">
                <dc:format>
                    <xsl:value-of select="amcr:meritko"/>
                </dc:format> <!-- {amcr:dokument/amcr:extra_data/amcr:meritko} -->
            </xsl:if>
            <xsl:if test="amcr:zachovalost">
                <dc:type>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:zachovalost/@id"/>
                </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:zachovalost[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:nahrada">
                <dc:type>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:nahrada/@id"/>
                </dc:type> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:nahrada[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:odkaz">
                <dc:relation>
                    <xsl:value-of select="amcr:odkaz"/>
                </dc:relation> <!-- {amcr:dokument/amcr:extra_data/amcr:odkaz} -->
            </xsl:if>
            <xsl:if test="amcr:udalost_typ">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:udalost_typ/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:udalost_typ[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:zeme">
                <dc:coverage>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:zeme/@id"/>
                </dc:coverage> <!-- [base_url]"/id/"{amcr:dokument/amcr:extra_data/amcr:zeme[@id]} -->
            </xsl:if>
            <xsl:if test="amcr:region">
                <dc:coverage>
                    <xsl:value-of select="amcr:region"/>
                </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:region} -->
            </xsl:if>
            <xsl:if test="amcr:rok_od">
                <dc:coverage>Rok od: <xsl:value-of select="amcr:rok_od"/></dc:coverage> <!-- Rok od: {amcr:dokument/amcr:extra_data/amcr:rok_od} -->
            </xsl:if>
            <xsl:if test="amcr:rok_do">
                <dc:coverage>Rok do: <xsl:value-of select="amcr:rok_do"/></dc:coverage> <!-- Rok do: {amcr:dokument/amcr:extra_data/amcr:rok_do} -->
            </xsl:if>
            <xsl:if test="amcr:geom_gml">
                <dc:coverage>
                    <xsl:value-of select="amcr:geom_gml"/>
                </dc:coverage> <!-- {amcr:dokument/amcr:extra_data/amcr:geom_gml} -->
            </xsl:if>
        </xsl:for-each>        
        <xsl:for-each select="amcr:tvar">
            <dc:subject>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:tvar/@id"/>
            </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:tvar/amcr:tvar[@id]} -->
        </xsl:for-each>        
        <xsl:for-each select="amcr:dokument_cast">
            <xsl:for-each select="amcr:komponenta">
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:obdobi/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:obdobi[@id]} -->
                <dc:subject>
                    <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:areal/@id"/>
                </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:areal[@id]} -->
                <xsl:for-each select="amcr:aktivita">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:aktivita[@id]} -->
                </xsl:for-each>
                <xsl:for-each select="amcr:nalez_objekt">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:druh/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_objekt/amcr:druh[@id]} -->
                    <xsl:if test="amcr:specifikace">
                        <dc:subject>
                            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:specifikace/@id"/>
                        </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_objekt/amcr:specifikace[@id]} -->
                    </xsl:if>
                </xsl:for-each>
                <xsl:for-each select="amcr:nalez_predmet">
                    <dc:subject>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:druh/@id"/>
                    </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_predmet/amcr:druh[@id]} -->
                    <xsl:if test="amcr:specifikace">
                        <dc:subject>
                            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:specifikace/@id"/>
                        </dc:subject> <!-- [base_url]"/id/"{amcr:dokument/amcr:dokument_cast/amcr:komponenta/amcr:nalez_predmet/amcr:specifikace[@id]} -->
                    </xsl:if>
                </xsl:for-each>
            </xsl:for-each>
            <xsl:for-each select="amcr:neident_akce">
                <xsl:if test="amcr:okres">
                    <dc:coverage>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:okres/@id"/>
                    </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:okres[@id]} -->
                </xsl:if>
                <xsl:if test="amcr:katastr">
                    <dc:coverage>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:katastr/@id"/>
                    </dc:coverage> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:katastr[@id]} -->
                </xsl:if>
                <xsl:if test="amcr:vedouci">
                    <dc:contributor>
                        <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:vedouci/@id"/>
                    </dc:contributor> <!-- [base_url]"/id/"{amcr:archeologicky_zaznam/amcr:dokument_cast/amcr:neident_akce/amcr:vedouci[@id]} -->
                </xsl:if>
            </xsl:for-each>
        </xsl:for-each>
        
        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:dokument/amcr:ident_cely} -->

        <xsl:for-each select="amcr:let">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:dokument/amcr:let[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:soubor">
            <dc:relation>
                <xsl:value-of select="./amcr:url"/>
            </dc:relation> <!-- {amcr:dokument/amcr:soubor/amcr:url} -->
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


<!-- ext_zdroj -->
      <xsl:template match="amcr:ext_zdroj">
        <dc:title xml:lang="cs">AMČR - externí zdroj <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - externí zdroj {amcr:ext_zdroj/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:ext_zdroj/amcr:ident_cely} -->
        <xsl:for-each select="amcr:doi">
            <dc:identifier>doi:<xsl:value-of select="."/></dc:identifier> <!-- doi:{amcr:ext_zdroj/amcr:doi} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">externí zdroj</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:ext_zdroj/amcr:stav_pom} -->
        <xsl:if test="amcr:misto"><dc:description xml:lang="cs">Místo vydání: <xsl:value-of select="amcr:misto"/></dc:description></xsl:if> <!-- Místo vydání: {amcr:ext_zdroj/amcr:misto} -->
        <xsl:if test="amcr:poznamka"><dc:description xml:lang="cs">Poznámka: <xsl:value-of select="amcr:poznamka"/></dc:description></xsl:if> <!-- Poznámka: {amcr:ext_zdroj/amcr:poznamka} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:typ/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:typ[@id]} -->
        <xsl:for-each select="amcr:autor">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:autor[@id]} -->
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
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:editor[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:sbornik_nazev">
            <dc:title><xsl:value-of select="."/></dc:title> <!-- amcr:ext_zdroj/amcr:sbornik_nazev} -->
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
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:type> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:typ_dokumentu[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:organizace">
            <dc:contributor>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:contributor> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:organizace[@id]} -->
        </xsl:for-each>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:ext_zdroj/amcr:ident_cely} -->

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
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost_pom/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:pristupnost_pom[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:typ/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:typ[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:presnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:presnost[@id]} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:geom_system/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:pian/amcr:geom_system[@id]} -->
        <xsl:for-each select="amcr:chranene_udaje">
            <dc:coverage xml:lang="cs">Mapa ZM10: <xsl:value-of select="amcr:zm10"/></dc:coverage> <!-- Mapa ZM10: {amcr:pian/amcr:chranene_udaje/amcr:zm10} -->
            <dc:coverage>
                <xsl:value-of select="amcr:geom_gml"/>
            </dc:coverage> <!-- {amcr:pian/amcr:chranene_udaje/amcr:geom_gml} -->
            <dc:coverage>
                <xsl:value-of select="amcr:geom_sjtsk_gml"/>
            </dc:coverage> <!-- {amcr:pian/amcr:chranene_udaje/amcr:geom_sjtsk_gml} -->
        </xsl:for-each>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:pian/amcr:ident_cely} -->

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
        <xsl:for-each select="amcr:igsn">
            <dc:identifier>doi:<xsl:value-of select="."/></dc:identifier> <!-- doi:{amcr:samostatny_nalez/amcr:igsn} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">samostatný nález</dc:subject> 
        <dc:description xml:lang="cs">Stav: <xsl:value-of select="amcr:stav"/></dc:description> <!-- "Stav: "{amcr:samostatny_nalez/amcr:stav_pom} -->
        <xsl:if test="amcr:evidencni_cislo"><dc:description xml:lang="cs">Evideční číslo: <xsl:value-of select="amcr:evidencni_cislo"/></dc:description></xsl:if> <!-- Evideční číslo: {amcr:samostatny_nalez/amcr:evidencni_cislo} -->
        <xsl:if test="amcr:poznamka"><dc:description xml:lang="cs">Poznámka: <xsl:value-of select="amcr:poznamka"/></dc:description></xsl:if> <!-- Poznámka: {amcr:samostatny_nalez/amcr:poznamka} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:pristupnost/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:pristupnost[@id]} -->
        <xsl:for-each select="amcr:okres">
            <dc:coverage>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:okres[@id]} -->
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
        <xsl:if test="amcr:chranene_udaje/amcr:geom_gml">
            <dc:coverage>
                <xsl:value-of select="amcr:chranene_udaje/amcr:geom_gml"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:chranene_udaje/amcr:geom_gml} -->
        </xsl:if>
        <xsl:if test="amcr:chranene_udaje/amcr:geom_sjtsk_gml">
            <dc:coverage>
                <xsl:value-of select="amcr:chranene_udaje/amcr:geom_sjtsk_gml"/>
            </dc:coverage> <!-- {amcr:samostatny_nalez/amcr:chranene_udaje/amcr:geom_sjtsk_gml} -->
        </xsl:if>
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
        <xsl:for-each select="amcr:nalezce">
            <dc:creator>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:creator> <!-- {amcr:samostatny_nalez/amcr:nalezce[@id]} -->
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

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->        
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:ident_cely} -->

        <xsl:for-each select="amcr:projekt">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:samostatny_nalez/amcr:projekt[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:soubor">
            <dc:relation>
                <xsl:value-of select="./amcr:url"/>
            </dc:relation> <!-- {amcr:samostatny_nalez/amcr:soubor/amcr:url} -->
        </xsl:for-each>
      </xsl:template>


<!-- uzivatel -->
      <xsl:template match="amcr:uzivatel">
        <dc:title xml:lang="cs">AMČR - uživatel <xsl:value-of select="amcr:ident_cely"/></dc:title> <!-- AMČR - uživatel {amcr:uzivatel/amcr:ident_cely} -->
        <dc:identifier>
            <xsl:value-of select="amcr:ident_cely"/>
        </dc:identifier> <!-- {amcr:uzivatel/amcr:ident_cely} -->
        <xsl:for-each select="amcr:orcid">
            <dc:identifier><xsl:value-of select="."/></dc:identifier> <!-- {amcr:uzivatel/amcr:orcid} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">uživatel</dc:subject>
        <dc:title><xsl:value-of select="amcr:prijmeni"/>, <xsl:value-of select="amcr:jmeno"/></dc:title>
        <dc:description xml:lang="cs">Email: <xsl:value-of select="amcr:email"/></dc:description> <!-- Email: {amcr:uzivatel/amcr:email} -->
        <xsl:if test="amcr:telefon"><dc:description xml:lang="cs">Telefon: <xsl:value-of select="amcr:telefon"/></dc:description></xsl:if> <!-- Telefon: {amcr:uzivatel/amcr:telefon} -->
        <xsl:for-each select="amcr:skupina">
            <dc:type xml:lang="cs">
                <xsl:value-of select="."/>
            </dc:type> <!-- {amcr:uzivatel/amcr:skupina} -->
        </xsl:for-each>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:ident_cely} -->

        <xsl:for-each select="amcr:osoba">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:osoba[@id]} -->
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
        <xsl:for-each select="amcr:spoluprace_podrizeni/amcr:spolupracovnik">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:spoluprace_podrizeni/amcr:spolupracovnik[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:spoluprace_nadrizeni/amcr:vedouci">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:uzivatel/amcr:spoluprace_nadrizeni/amcr:vedouci[@id]} -->
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

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:heslo/amcr:ident_cely} -->

        <xsl:for-each select="amcr:odkaz/amcr:uri">
            <dc:relation>
                <xsl:value-of select="."/>
            </dc:relation> <!-- {amcr:heslo/amcr:odkaz/amcr:uri} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:hierarchie_vyse/amcr:heslo_nadrazene">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:hierarchie_vyse/amcr:heslo_nadrazene[@id]} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:hierarchie_nize/amcr:heslo_podrazene">
            <dc:relation>
                <xsl:value-of select="$base_url_id"/><xsl:value-of select="./@id"/>
            </dc:relation> <!-- [base_url]"/id/"{amcr:heslo/amcr:hierarchie_nize/amcr:heslo_podrazene[@id]} -->
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
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:ruian_kraj/amcr:nazev_en} -->
        <dc:identifier>
            <xsl:value-of select="amcr:kod"/>
        </dc:identifier> <!-- {amcr:ruian_kraj/amcr:kod} -->
        <dc:subject xml:lang="cs">kraj</dc:subject> <!-- "kraj" -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_gml"/></dc:coverage> <!-- {amcr:ruian_kraj/amcr:definicni_bod_gml} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_gml"/></dc:coverage> <!-- {amcr:ruian_kraj/amcr:hranice_gml} -->

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
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:ruian_okres/amcr:nazev_en} -->
        <dc:identifier>
            <xsl:value-of select="amcr:kod"/>
        </dc:identifier> <!-- {amcr:ruian_okres/amcr:kod} -->
        <dc:subject xml:lang="cs">okres</dc:subject> <!-- "okres" -->
        <dc:title><xsl:value-of select="amcr:spz"/></dc:title> <!-- {amcr:ruian_okres/amcr:spz} -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_gml"/></dc:coverage> <!-- {amcr:ruian_okres/amcr:definicni_bod_gml} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_gml"/></dc:coverage> <!-- {amcr:ruian_okres/amcr:hranice_gml} -->

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
            <xsl:value-of select="amcr:kod"/>
        </dc:identifier> <!-- {amcr:ruian_katastr/amcr:kod} -->
        <dc:subject xml:lang="cs">katastrální území</dc:subject> <!-- "katastrální území" -->
        <dc:coverage><xsl:value-of select="amcr:definicni_bod_gml"/></dc:coverage> <!-- {amcr:ruian_katastr/amcr:definicni_bod_gml} -->
        <dc:coverage><xsl:value-of select="amcr:hranice_gml"/></dc:coverage> <!-- {amcr:ruian_katastr/amcr:hranice_gml} -->

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
        <xsl:for-each select="amcr:ror">
            <dc:identifier><xsl:value-of select="."/></dc:identifier> <!-- {amcr:organizace/amcr:ror} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">organizace</dc:subject> <!-- "organizace" -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev"/></dc:title> <!-- {amcr:organizace/amcr:nazev} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_en"/></dc:title> <!-- {amcr:organizace/amcr:nazev_en} -->
        <dc:title xml:lang="cs"><xsl:value-of select="amcr:nazev_zkraceny"/></dc:title> <!-- {amcr:organizace/amcr:nazev_zkraceny} -->
        <dc:title xml:lang="en"><xsl:value-of select="amcr:nazev_zkraceny_en"/></dc:title> <!-- {amcr:organizace/amcr:nazev_zkraceny_en} -->
        <dc:type>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:typ_organizace/@id"/>
        </dc:type> <!-- [base_url]"/id/"{amcr:organizace/amcr:typ_organizace[@id]} -->
        <xsl:if test="amcr:adresa"><dc:description xml:lang="cs">Adresa: <xsl:value-of select="amcr:adresa"/></dc:description></xsl:if> <!-- Adresa: {amcr:organizace/amcr:adresa} -->
        <xsl:if test="amcr:email"><dc:description xml:lang="cs">Email: <xsl:value-of select="amcr:email"/></dc:description></xsl:if> <!-- Email: {amcr:organizace/amcr:email} -->
        <xsl:if test="amcr:telefon"><dc:description xml:lang="cs">Telefon: <xsl:value-of select="amcr:telefon"/></dc:description></xsl:if> <!-- Telefon: {amcr:organizace/amcr:telefon} -->
        <xsl:if test="amcr:ico"><dc:identifier xml:lang="cs">IČO: <xsl:value-of select="amcr:ico"/></dc:identifier></xsl:if> <!-- {amcr:organizace/amcr:ico} -->

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
        <xsl:for-each select="amcr:orcid">
            <dc:identifier><xsl:value-of select="."/></dc:identifier> <!-- {amcr:osoba/amcr:orcid} -->
        </xsl:for-each>
        <xsl:for-each select="amcr:wikidata">
            <dc:identifier><xsl:value-of select="."/></dc:identifier> <!-- {amcr:osoba/amcr:wikidata} -->
        </xsl:for-each>
        <dc:subject xml:lang="cs">osoba</dc:subject>
        <dc:title><xsl:value-of select="amcr:vypis_cely"/></dc:title> <!-- {amcr:osoba/amcr:vypis_cely} -->
        <dc:coverage><xsl:value-of select="amcr:rok_narozeni"/>-<xsl:value-of select="amcr:rok_umrti"/></dc:coverage> <!-- {amcr:osoba/amcr:rok_narozeni}-{amcr:osoba/amcr:rok_umrti} -->
        <xsl:if test="amcr:rodne_prijmeni">
            <dc:description xml:lang="cs">Rodné příjmení: <xsl:value-of select="amcr:rodne_prijmeni"/></dc:description> <!-- Rodné příjmení: {amcr:osoba/amcr:rodne_prijmeni} -->
        </xsl:if>

        <dc:format>application/xml</dc:format> <!-- "application/xml" -->
        <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/</dc:rights> <!-- "https://creativecommons.org/licenses/by-nc/4.0/" -->
        <dc:publisher>https://www.aiscr.cz/</dc:publisher> <!-- "https://www.aiscr.cz/" -->
        <dc:source>
            <xsl:value-of select="$base_url_id"/><xsl:value-of select="amcr:ident_cely"/>
        </dc:source> <!-- [base_url]"/id/"{amcr:osoba/amcr:ident_cely} -->
      </xsl:template>


<!-- HTTP/1.1 403 Forbidden -->
      <xsl:template match="amcr:amcr[.=' HTTP/1.1 403 Forbidden ']">
          <dc:type>HTTP/1.1 403 Forbidden</dc:type>
      </xsl:template>

</xsl:stylesheet>
