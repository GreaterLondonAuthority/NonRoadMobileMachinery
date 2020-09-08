/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A class that holds and manages all the known (non-military) timezones
 */
public enum GeoTimeZone {

    ADT_AtlanticDaylightTime("ADT", "Atlantic Daylight Time", "Atlantic", -3),
    ADT_AtlanticDaylightTime1("ADT", "Atlantic Daylight Time", "North America", -3),
    AEDT_AustralianEasternDaylightTime("AEDT", "Australian Eastern Daylight Time", "Australia", 11),
    AEST_AustralianEasternStandardTime("AEST", "Australian Eastern Standard Time", "Australia", 10),
    AKDT_AlaskaDaylightTime("AKDT", "Alaska Daylight Time", "North America", -8),
    AKST_AlaskaStandardTime("AKST", "Alaska Standard Time", "North America", -9),
    ALMT_AlmaAtaTime("ALMT", "Alma-Ata Time", "Asia", 6),
    AMST_ArmeniaSummerTime("AMST", "Armenia Summer Time", "Asia", 5),
    AMST_AmazonSummerTime("AMST", "Amazon Summer Time", "South America", -3),
    AMT_ArmeniaTime("AMT", "Armenia Time", "Asia", 4),
    AMT_AmazonTime("AMT", "Amazon Time", "South America", -4),
    ANAST_AnadyrSummerTime("ANAST", "Anadyr Summer Time", "Asia", 12),
    ANAT_AnadyrTime("ANAT", "Anadyr Time", "Asia", 12),
    AQTT_AqtobeTime("AQTT", "Aqtobe Time", "Asia", 5),
    ART_ArgentinaTime("ART", "Argentina Time", "South America", -3),
    AST_ArabiaStandardTime("AST", "Arabia Standard Time", "Asia", 3),
    AST_AtlanticStandardTime("AST", "Atlantic Standard Time", "Atlantic", -4),
    AST_AtlanticStandardTime1("AST", "Atlantic Standard Time", "Caribbean", -4),
    AST_AtlanticStandardTime2("AST", "Atlantic Standard Time", "North America", -4),
    AWDT_AustralianWesternDaylightTime("AWDT", "Australian Western Daylight Time", "Australia", 9),
    AWST_AustralianWesternStandardTime("AWST", "Australian Western Standard Time", "Australia", 8),
    AZOST_AzoresSummerTime("AZOST", "Azores Summer Time", "Atlantic", 0),
    AZOT_AzoresTime("AZOT", "Azores Time", "Atlantic", -1),
    AZST_AzerbaijanSummerTime("AZST", "Azerbaijan Summer Time", "Asia", 5),
    AZT_AzerbaijanTime("AZT", "Azerbaijan Time", "Asia", 4),
    BNT_BruneiDarussalamTime("BNT", "Brunei Darussalam Time", "Asia", 8),
    BOT_BoliviaTime("BOT", "Bolivia Time", "South America", -4),
    BRST_BrasiliaSummerTime("BRST", "Brasilia Summer Time", "South America", -2),
    BRT_Brasliatime("BRT", "Brasília time", "South America", -3),
    BST_BangladeshStandardTime("BST", "Bangladesh Standard Time", "Asia", 6),
    BST_BritishSummerTime("BST", "British Summer Time", "Europe", 1),
    BTT_BhutanTime("BTT", "Bhutan Time", "Asia", 6),
    CAST_CaseyTime("CAST", "Casey Time", "Antarctica", 8),
    CAT_CentralAfricaTime("CAT", "Central Africa Time", "Africa", 2),
    CDT_CubaDaylightTime("CDT", "Cuba Daylight Time", "Caribbean", -4),
    CDT_CentralDaylightTime("CDT", "Central Daylight Time", "North America", -5),
    CEST_CentralEuropeanSummerTime("CEST", "Central European Summer Time", "Europe", 2),
    CET_CentralEuropeanTime("CET", "Central European Time", "Africa", 1),
    CET_CentralEuropeanTime1("CET", "Central European Time", "Europe", 1),
    CKT_CookIslandTime("CKT", "Cook Island Time", "Pacific", -10),
    CLST_ChileSummerTime("CLST", "Chile Summer Time", "South America", -3),
    CLT_ChileStandardTime("CLT", "Chile Standard Time", "South America", -4),
    COT_ColombiaTime("COT", "Colombia Time", "South America", -5),
    CST_ChinaStandardTime("CST", "China Standard Time", "Asia", 8),
    CST_CentralStandardTime("CST", "Central Standard Time", "Central America", -6),
    CST_CentralStandardTime1("CST", "Central Standard Time", "North America", -6),
    CST_CubaStandardTime("CST", "Cuba Standard Time", "Caribbean", -5),
    CVT_CapeVerdeTime("CVT", "Cape Verde Time", "Africa", -1),
    CXT_ChristmasIslandTime("CXT", "Christmas Island Time", "Australia", 7),
    ChST_ChamorroStandardTime("ChST", "Chamorro Standard Time", "Pacific", 10),
    DAVT_DavisTime("DAVT", "Davis Time", "Antarctica", 7),
    EASST_EasterIslandSummerTime("EASST", "Easter Island Summer Time", "Pacific", -5),
    EAST_EasterIslandStandardTime1("EAST", "Easter Island Standard Time", "Pacific", -6),
    EAT_EasternAfricaTime("EAT", "Eastern Africa Time", "Africa", 3),
    ECT_EcuadorTime("ECT", "Ecuador Time", "South America", -5),
    EDT_EasternDaylightTime("EDT", "Eastern Daylight Time", "Caribbean", -4),
    EDT_EasternDaylightTime1("EDT", "Eastern Daylight Time", "North America", -4),
    EDT_EasternDaylightTime2("EDT", "Eastern Daylight Time", "Pacific", 11),
    EEST_EasternEuropeanSummerTime("EEST", "Eastern European Summer Time", "Africa", 3),
    EEST_EasternEuropeanSummerTime1("EEST", "Eastern European Summer Time", "Asia", 3),
    EEST_EasternEuropeanSummerTime2("EEST", "Eastern European Summer Time", "Europe", 3),
    EET_EasternEuropeanTime("EET", "Eastern European Time", "Africa", 2),
    EET_EasternEuropeanTime1("EET", "Eastern European Time", "Asia", 2),
    EET_EasternEuropeanTime2("EET", "Eastern European Time", "Europe", 2),
    EGST_EasternGreenlandSummerTime("EGST", "Eastern Greenland Summer Time", "North America", 0),
    EGT_EastGreenlandTime("EGT", "East Greenland Time", "North America", -1),
    EST_EasternStandardTime("EST", "Eastern Standard Time", "Central America", -5),
    EST_EasternStandardTime1("EST", "Eastern Standard Time", "Caribbean", -5),
    EST_EasternStandardTime2("EST", "Eastern Standard Time", "North America", -5),
    ET_TiempodelEste("ET", "Tiempo del Este", "Central America", -5),
    ET_TiempodelEste1("ET", "Tiempo del Este", "Caribbean", -5),
    ET_TiempoDelEste2("ET", "Tiempo Del Este ", "North America", -5),
    FJST_FijiSummerTime("FJST", "Fiji Summer Time", "Pacific", 13),
    FJT_FijiTime("FJT", "Fiji Time", "Pacific", 12),
    FKST_FalklandIslandsSummerTime("FKST", "Falkland Islands Summer Time", "South America", -3),
    FKT_FalklandIslandTime("FKT", "Falkland Island Time", "South America", -4),
    FNT_FernandodeNoronhaTime("FNT", "Fernando de Noronha Time", "South America", -2),
    GALT_GalapagosTime("GALT", "Galapagos Time", "Pacific", -6),
    GAMT_GambierTime("GAMT", "Gambier Time", "Pacific", -9),
    GET_GeorgiaStandardTime("GET", "Georgia Standard Time", "Asia", 4),
    GFT_FrenchGuianaTime("GFT", "French Guiana Time", "South America", -3),
    GILT_GilbertIslandTime("GILT", "Gilbert Island Time", "Pacific", 12),
    GMT_GreenwichMeanTime("GMT", "Greenwich Mean Time", "Africa", 0),
    GMT_GreenwichMeanTime1("GMT", "Greenwich Mean Time", "Europe", 0),
    GST_GulfStandardTime("GST", "Gulf Standard Time", "Asia", 4),
    GYT_GuyanaTime("GYT", "Guyana Time", "South America", -4),
    HAA_HeureAvancedelAtlantique("HAA", "Heure Avancée de l'Atlantique", "Atlantic", -3),
    HAA_HeureAvancedelAtlantique1("HAA", "Heure Avancée de l'Atlantique", "North America", -3),
    HAC_HeureAvanceduCentre("HAC", "Heure Avancée du Centre", "North America", -5),
    HADT_HawaiiAleutianDaylightTime("HADT", "Hawaii-Aleutian Daylight Time", "North America", -9),
    HAE_HeureAvancedelEst("HAE", "Heure Avancée de l'Est ", "Caribbean", -4),
    HAE_HeureAvancedelEst1("HAE", "Heure Avancée de l'Est ", "North America", -4),
    HAP_HeureAvanceduPacifique("HAP", "Heure Avancée du Pacifique", "North America", -7),
    HAR_HeureAvancedesRocheuses("HAR", "Heure Avancée des Rocheuses", "North America", -6),
    HAST_HawaiiAleutianStandardTime("HAST", "Hawaii-Aleutian Standard Time", "North America", -10),
    HAY_HeureAvanceduYukon("HAY", "Heure Avancée du Yukon", "North America", -8),
    HKT_HongKongTime("HKT", "Hong Kong Time", "Asia", 8),
    HNA_HeureNormaledelAtlantique("HNA", "Heure Normale de l'Atlantique", "Atlantic", -4),
    HNA_HeureNormaledelAtlantique1("HNA", "Heure Normale de l'Atlantique", "Caribbean", -4),
    HNA_HeureNormaledelAtlantique2("HNA", "Heure Normale de l'Atlantique", "North America", -4),
    HNC_HeureNormaleduCentre("HNC", "Heure Normale du Centre", "Central America", -6),
    HNC_HeureNormaleduCentre1("HNC", "Heure Normale du Centre", "North America", -6),
    HNE_HeureNormaledelEst("HNE", "Heure Normale de l'Est", "Central America", -5),
    HNE_HeureNormaledelEst1("HNE", "Heure Normale de l'Est", "Caribbean", -5),
    HNE_HeureNormaledelEst2("HNE", "Heure Normale de l'Est", "North America", -5),
    HNP_HeureNormaleduPacifique("HNP", "Heure Normale du Pacifique", "North America", -8),
    HNR_HeureNormaledesRocheuses("HNR", "Heure Normale des Rocheuses", "North America", -7),
    HNY_HeureNormaleduYukon("HNY", "Heure Normale du Yukon", "North America", -9),
    HOVT_HovdTime("HOVT", "Hovd Time", "Asia", 7),
    ICT_IndochinaTime("ICT", "Indochina Time", "Asia", 7),
    IDT_IsraelDaylightTime("IDT", "Israel Daylight Time", "Asia", 3),
    IRKST_IrkutskSummerTime("IRKST", "Irkutsk Summer Time", "Asia", 9),
    IRKT_IrkutskTime("IRKT", "Irkutsk Time", "Asia", 9),
    IST_IrishStandardTime("IST", "Irish Standard Time", "Europe", 1),
    JST_JapanStandardTime("JST", "Japan Standard Time", "Asia", 9),
    KGT_KyrgyzstanTime("KGT", "Kyrgyzstan Time", "Asia", 6),
    KRAST_KrasnoyarskSummerTime("KRAST", "Krasnoyarsk Summer Time", "Asia", 8),
    KRAT_KrasnoyarskTime("KRAT", "Krasnoyarsk Time", "Asia", 8),
    KST_KoreaStandardTime("KST", "Korea Standard Time", "Asia", 9),
    KUYT_KuybyshevTime("KUYT", "Kuybyshev Time", "Europe", 4),
    LHDT_LordHoweDaylightTime("LHDT", "Lord Howe Daylight Time", "Australia", 11),
    LINT_LineIslandsTime("LINT", "Line Islands Time", "Pacific", 14),
    MAGST_MagadanSummerTime("MAGST", "Magadan Summer Time", "Asia", 12),
    MAGT_MagadanTime("MAGT", "Magadan Time", "Asia", 12),
    MAWT_MawsonTime("MAWT", "Mawson Time", "Antarctica", 5),
    MDT_MountainDaylightTime("MDT", "Mountain Daylight Time", "North America", -6),
    MESZ_MitteleuropischeSommerzeit("MESZ", "Mitteleuropäische Sommerzeit", "Europe", 2),
    MEZ_MitteleuropischeZeit("MEZ", "Mitteleuropäische Zeit", "Africa", 1),
    MHT_MarshallIslandsTime("MHT", "Marshall Islands Time", "Pacific", 12),
    MSD_MoscowDaylightTime("MSD", "Moscow Daylight Time", "Europe", 4),
    MSK_MoscowStandardTime("MSK", "Moscow Standard Time", "Europe", 4),
    MST_MountainStandardTime("MST", "Mountain Standard Time", "North America", -7),
    MUT_MauritiusTime("MUT", "Mauritius Time", "Africa", 4),
    MVT_MaldivesTime("MVT", "Maldives Time", "Asia", 5),
    MYT_MalaysiaTime("MYT", "Malaysia Time", "Asia", 8),
    NCT_NewCaledoniaTime("NCT", "New Caledonia Time", "Pacific", 11),
    NOVST_NovosibirskSummerTime("NOVST", "Novosibirsk Summer Time", "Asia", 7),
    NOVT_NovosibirskTime("NOVT", "Novosibirsk Time", "Asia", 6),
    NUT_NiueTime("NUT", "Niue Time", "Pacific", -11),
    NZDT_NewZealandDaylightTime("NZDT", "New Zealand Daylight Time", "Antarctica", 13),
    NZDT_NewZealandDaylightTime1("NZDT", "New Zealand Daylight Time", "Pacific", 13),
    NZST_NewZealandStandardTime("NZST", "New Zealand Standard Time", "Antarctica", 12),
    NZST_NewZealandStandardTime1("NZST", "New Zealand Standard Time", "Pacific", 12),
    OMSST_OmskSummerTime("OMSST", "Omsk Summer Time", "Asia", 7),
    OMST_OmskStandardTime("OMST", "Omsk Standard Time", "Asia", 7),
    PDT_PacificDaylightTime("PDT", "Pacific Daylight Time", "North America", -7),
    PET_PeruTime("PET", "Peru Time", "South America", -5),
    PETST_KamchatkaSummerTime("PETST", "Kamchatka Summer Time", "Asia", 12),
    PETT_KamchatkaTime("PETT", "Kamchatka Time", "Asia", 12),
    PGT_PapuaNewGuineaTime("PGT", "Papua New Guinea Time", "Pacific", 10),
    PHOT_PhoenixIslandTime("PHOT", "Phoenix Island Time", "Pacific", 13),
    PHT_PhilippineTime("PHT", "Philippine Time", "Asia", 8),
    PKT_PakistanStandardTime("PKT", "Pakistan Standard Time", "Asia", 5),
    PMDT_PierreMiquelonDaylightTime("PMDT", "Pierre & Miquelon Daylight Time", "North America", -2),
    PMST_PierreMiquelonStandardTime("PMST", "Pierre & Miquelon Standard Time", "North America", -3),
    PONT_PohnpeiStandardTime("PONT", "Pohnpei Standard Time", "Pacific", 11),
    PST_PacificStandardTime("PST", "Pacific Standard Time", "North America", -8),
    PST_PitcairnStandardTime("PST", "Pitcairn Standard Time", "Pacific", -8),
    PT_TiempodelPacfico("PT", "Tiempo del Pacífico", "North America", -8),
    PWT_PalauTime("PWT", "Palau Time", "Pacific", 9),
    PYST_ParaguaySummerTime("PYST", "Paraguay Summer Time", "South America", -3),
    PYT_ParaguayTime("PYT", "Paraguay Time", "South America", -4),
    RET_ReunionTime("RET", "Reunion Time", "Africa", 4),
    SAMT_SamaraTime("SAMT", "Samara Time", "Europe", 4),
    SAST_SouthAfricaStandardTime("SAST", "South Africa Standard Time", "Africa", 2),
    SBT_SolomonIslandsTime("SBT", "Solomon IslandsTime", "Pacific", 11),
    SCT_SeychellesTime("SCT", "Seychelles Time", "Africa", 4),
    SGT_SingaporeTime("SGT", "Singapore Time", "Asia", 8),
    SRT_SurinameTime("SRT", "Suriname Time", "South America", -3),
    SST_SamoaStandardTime("SST", "Samoa Standard Time", "Pacific", -11),
    TAHT_TahitiTime("TAHT", "Tahiti Time", "Pacific", -10),
    TJT_TajikistanTime("TJT", "Tajikistan Time", "Asia", 5),
    TKT_TokelauTime("TKT", "Tokelau Time", "Pacific", 13),
    TLT_EastTimorTime("TLT", "East Timor Time", "Asia", 9),
    TMT_TurkmenistanTime("TMT", "Turkmenistan Time", "Asia", 5),
    TVT_TuvaluTime("TVT", "Tuvalu Time", "Pacific", 12),
    ULAT_UlaanbaatarTime("ULAT", "Ulaanbaatar Time", "Asia", 8),
    UTC_UniversalTimeCoordinated("UTC", "Universal Time Coordinated", "Universal", 0),
    UYST_UruguaySummerTime("UYST", "Uruguay Summer Time", "South America", -2),
    UYT_UruguayTime("UYT", "Uruguay Time", "South America", -3),
    UZT_UzbekistanTime("UZT", "Uzbekistan Time", "Asia", 5),
    VLAST_VladivostokSummerTime("VLAST", "Vladivostok Summer Time", "Asia", 11),
    VLAT_VladivostokTime("VLAT", "Vladivostok Time", "Asia", 11),
    VUT_VanuatuTime("VUT", "Vanuatu Time", "Pacific", 11),
    WAST_WestAfricaSummerTime("WAST", "West Africa Summer Time", "Africa", 2),
    WAT_WestAfricaTime("WAT", "West Africa Time", "Africa", 1),
    WEST_WesternEuropeanSummerTime("WEST", "Western European Summer Time", "Africa", 1),
    WEST_WesternEuropeanSummerTime1("WEST", "Western European Summer Time", "Europe", 1),
    WESZ_WesteuropischeSommerzeit("WESZ", "Westeuropäische Sommerzeit", "Africa", 1),
    WET_WesternEuropeanTime("WET", "Western European Time", "Africa", 0),
    WET_WesternEuropeanTime1("WET", "Western European Time", "Europe", 0),
    WEZ_WesteuropischeZeit("WEZ", "Westeuropäische Zeit", "Europe", 0),
    WFT_WallisandFutunaTime("WFT", "Wallis and Futuna Time", "Pacific", 12),
    WGST_WesternGreenlandSummerTime("WGST", "Western Greenland Summer Time", "North America", -2),
    WGT_WestGreenlandTime("WGT", "West Greenland Time", "North America", -3),
    WIB_WesternIndonesianTime("WIB", "Western Indonesian Time", "Asia", 7),
    WIT_EasternIndonesianTime("WIT", "Eastern Indonesian Time", "Asia", 9),
    WITA_CentralIndonesianTime("WITA", "Central Indonesian Time", "Asia", 8),
    WST_WesternSaharaSummerTime("WST", "Western Sahara Summer Time", "Africa", 1),
    WST_WestSamoaTime("WST", "West Samoa Time", "Pacific", 13),
    WT_WesternSaharaStandardTime("WT", "Western Sahara Standard Time", "Africa", 0),
    YAKST_YakutskSummerTime("YAKST", "Yakutsk Summer Time", "Asia", 10),
    YAKT_YakutskTime("YAKT", "Yaku7tsk Time", "Asia", 10),
    YAPT_YapTime("YAPT", "Yap Time", "Pacific", 10),
    YEKST_YekaterinburgSummerTime("YEKST", "Yekaterinburg Summer Time", "Asia", 6),
    YEKT_YekaterinburgTime("YEKT", " Yekaterinburg Time", "Asia", 6);

    private String code;
    private String description;
    private String area;
    private int offset;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GeoTimeZone.class);

    private static List<GeoTimeZone> all;
    private static List<Map<String,String>> allDisplay;
    private static List<String> areas;
    private static Map<String, List<GeoTimeZone>> areaMap;

    /**
     * Create all the surrogate versions of the lists
     */
    static {
        all = Common.sortListObjects(Arrays.asList(values()), "getCode");
        Set<String> list = new HashSet<>();
        Map<String, List<GeoTimeZone>> areaMapTmp = new CaseInsensitiveMap();
        Map<String, Map<String,String>> displayMap = new CaseInsensitiveMap();
        for (GeoTimeZone tz : all) {
            list.add(tz.area);
            List<GeoTimeZone> areaList = areaMapTmp.get(tz.area);
            if (areaList==null) {
                areaList = new ArrayList<>();
                areaMapTmp.put(tz.area, areaList);
            }
            areaList.add(tz);
            String text;
            if (tz.offset<0)
                text = String.format("%s (UTC-%d) %s", Common.padRight(tz.code,"&nbsp;",4), Math.abs(tz.offset), tz.description);
            else if (tz.offset>0)
                text = String.format("%s (UTC+%d) %s", Common.padRight(tz.code,"&nbsp;",4), Math.abs(tz.offset), tz.description);
            else
                text = String.format("%s (UTC) %s", Common.padRight(tz.code,"&nbsp;",4), tz.description);
            displayMap.put(tz.code, Common.getMapFromPairs("text",text,"value",tz.code));
        }
        areas = Common.sortList(new ArrayList<>(list));
        areaMap = areaMapTmp;
        allDisplay = Common.sortList(new ArrayList<>(displayMap.values()),"value");
    }

    /**
     * Create an enumeration
     * @param code International code for the time zone
     * @param description Description of the timezone
     * @param area Area covered by the timezone
     * @param offset Offset from UTC in hours
     */
    GeoTimeZone(String code, String description, String area, int offset) {
        this.code = code;
        this.description = description;
        this.area = area;
        this.offset = offset;
    }

    /**
     * Returns all the available timezone codes
     * @return List of timezones
     */
    public static List<GeoTimeZone> getAll() {
        return all;
    }

    /**
     * Returns all the available timezone areas
     * @return List of timezones
     */
    public static List<String> getAreas() {
        return areas;
    }

    /**
     * Returns a list of timzones for the specific area
     * @param area Area to use
     * @return List of timezones
     */
    public static List<GeoTimeZone> get(String area) {
        return Common.isBlank(area)?null:areaMap.get(area);
    }

    /**
     * Returns a list of timezones suitable for display on a form
     * @return List of mapped values
     */
    public static List<Map<String,String>> getAllDisplay() {
        return allDisplay;
    }

    /**
     * Returns the international timzone code
     * @return Code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the description of the timezone
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the area that the timezone covers
     * @return Area
     */
    public String getArea() {
        return area;
    }

    /**
     * Returns the offset from UTC for the timezone
     * @return Offset in hours
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Checks if day light saving is active for the given timezone as of the given date time.
     *
     * @param destTZ  String representing the timezone to convert the date into to check for DST.
     * @param sourceTzDate  Source timezone Date to check for Day Light saving.
     */
    public static boolean checkIfTimezoneinDST(String destTZ, Date sourceTzDate) {
        boolean isDST = false;
        try {
            TimeZone tz = TimeZone.getTimeZone(destTZ);
            if (tz.inDaylightTime(sourceTzDate)) {
                isDST = true;
            }
        }
        catch (Exception e1) {
            logger.debug("Error while fetching if day light saving is active for timezone [" + destTZ + "] and date [" + sourceTzDate + ']', e1);
        }
        return isDST;
    }

    /**
     * Converts the specified dateTime string formatted in the specified inputDateFormat from
     * the specified sourceTZ time zone into the specified destTZ time zone and returns
     * the date formatted in the specified outputDateFormat.
     *
     * @param dateTime         String representing the date to convert.
     * @param inputDateFormat  The format in which the input date string is provided.
     * @param outputDateFormat The format in which the output date string needs to be returned.
     * @param sourceTZ         The original time zone of the provided date.
     * @param destTZ           The time zone to which to convert the input date.
     * @return A date string formatted in the specified outputDateFormat representing the
     *         result of converting the specified dateTime for the specified sourceTZ time
     *         zone to the specified destTZ time one.
     */
    public static String convertDateTimeZone(String dateTime, String inputDateFormat, String outputDateFormat, String sourceTZ, String destTZ) {
        SimpleDateFormat inputSdf = new SimpleDateFormat(inputDateFormat);
        SimpleDateFormat outputSdf = new SimpleDateFormat(outputDateFormat);
        String formattedTime = null;
        Date specifiedTime;

        try {
            inputSdf.setTimeZone(TimeZone.getTimeZone(sourceTZ));
            specifiedTime = inputSdf.parse(dateTime);
            outputSdf.setTimeZone(TimeZone.getTimeZone(destTZ));
            formattedTime = outputSdf.format(specifiedTime);

        } catch (Exception e1) {
            logger.debug("Cannot convert date [" + dateTime + "] using input date format [" +
                    inputDateFormat + "] and output date format [" + outputDateFormat +
                    "] from time zone [" + sourceTZ + "] to time zone [" + destTZ + "].", e1);
        }

        return formattedTime;
    }

}
