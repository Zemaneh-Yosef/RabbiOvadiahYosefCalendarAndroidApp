package com.ej.rovadiahyosefcalendar.classes;

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

    ChaiTablesCountries(String label) {
        this.label = label;
    }
}
