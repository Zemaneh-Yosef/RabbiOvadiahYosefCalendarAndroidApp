package com.ej.rovadiahyosefcalendar.classes;

/**
 * This class is used to store the countries and their corresponding names for the Chai table links.
 * @author E.J.
 * @version 1.0
 * @see ChaiTablesOptionsList
 */
public enum ChaiTablesCountries {

    ARGENTINA("Argentina"), 
    AUSTRALIA("Australia"), 
    AUSTRIA("Austria"),
    BELGIUM("Belgium"),
    BRAZIL("Brazil"),
    BULGARIA("Bulgaria"),
    CANADA("Canada"),
    CHILE("Chile"),
    CHINA("China"),
    COLOMBIA("Colombia"),
    CZECH_REPUBLIC("Czech-Republic"),
    DENMARK("Denmark"), 
    ERETZ_YISROEL_CITIES("Eretz_Yisroel"), //becomes Eretz_Yisroel in the link
    ERETZ_YISROEL_NEIGHBORHOODS("Israel"), //becomes Israel in the link
    FRANCE("France"),
    GERMANY("Germany"),
    GREECE("Greece"),
    HUNGARY("Hungary"),  
    ITALY("Italy"), 
    MEXICO("Mexico"), 
    NETHERLANDS("Netherlands"), 
    PANAMA("Panama"), 
    POLAND("Poland"), 
    ROMANIA("Romania"), 
    RUSSIA("Russia"), 
    SOUTH_AFRICA("South-Africa"),
    SPAIN("Spain"), 
    SWITZERLAND("Switzerland"), 
    TURKEY("Turkey"), 
    UK_AND_IRELAND("England"), //becomes England in the link
    UKRAINE("Ukraine"), 
    URUGUAY("Uruguay"), 
    USA("USA"), 
    VENEZUELA("Venezuela");

    public final String label;

    /**
     * Constructor for the enum.
     * @param label The label of the country in the link (e.g. "Argentina" for the link "http://chaitables.com/cgi-bin/ChaiTables.cgi/?cgi_TableType=Chai&cgi_country=Argentina").
     */
    ChaiTablesCountries(String label) {
        this.label = label;
    }
}
