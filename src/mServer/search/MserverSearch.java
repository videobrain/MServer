/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mServer.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import mSearch.Config;
import mSearch.MSearch;
import mSearch.tool.Log;
import mServer.daten.MserverSearchTask;
import mServer.tool.MserverDaten;
import mServer.tool.MserverKonstanten;
import mServer.tool.MserverLog;

public class MserverSearch {

    MSearch mSearch;

    public MserverSearch() {
        this.mSearch = null;
        Config.dirFilme = MserverDaten.getVerzeichnisFilme();
    }

    public boolean filmeSuchen(MserverSearchTask aktSearchTask) {
        boolean ret = true;
        try {
            // ===========================================
            // den nächsten Suchlauf starten
            MserverLog.systemMeldung("");
            MserverLog.systemMeldung("-----------------------------------");
            MserverLog.systemMeldung("Filmsuche starten");
            mSearch = new MSearch();

            // was und wie
            Config.senderLoadHow = aktSearchTask.loadHow();
            Config.updateFilmliste = aktSearchTask.updateFilmliste();
            Config.nurSenderLaden = arrLesen(aktSearchTask.arr[MserverSearchTask.SUCHEN_SENDER_NR].trim());
            Config.orgFilmlisteErstellen = aktSearchTask.orgListeAnlegen();
            Config.orgFilmliste = MserverDaten.system[MserverKonstanten.SYSTEM_FILMLISTE_ORG_NR];

            // live-steams
            Config.importLive = MserverDaten.system[MserverKonstanten.SYSTEM_IMPORT_LIVE_NR];

            // und noch evtl. ein paar Imports von Filmlisten anderer Server
            Config.importUrl_1__anhaengen = MserverDaten.system[MserverKonstanten.SYSTEM_IMPORT_URL_1_NR];
            Config.importUrl_2__anhaengen = MserverDaten.system[MserverKonstanten.SYSTEM_IMPORT_URL_2_NR];

            // für die alte Filmliste
            Config.importOld = MserverDaten.system[MserverKonstanten.SYSTEM_IMPORT_OLD_NR];

            // Rest
            Config.setUserAgent(MserverDaten.getUserAgent());
            Config.proxyUrl = MserverDaten.system[MserverKonstanten.SYSTEM_PROXY_URL_NR];
            Config.proxyPort = MserverDaten.getProxyPort();
            Config.debug = MserverDaten.debug;

            Log.setLogfile(MserverDaten.getLogDatei(MserverKonstanten.LOG_FILE_NAME_MSEARCH));

            Thread t = new Thread(mSearch);
            t.start();
            MserverLog.systemMeldung("Filme suchen gestartet");
            // ===========================================
            // warten auf das Ende
            //int warten = aktSearchTask.allesLaden() == true ? MvSKonstanten.WARTEZEIT_ALLES_LADEN : MvSKonstanten.WARTEZEIT_UPDATE_LADEN;
            int warten = aktSearchTask.getWaitTime()/*Minuten*/;
            MserverLog.systemMeldung("Max Laufzeit[Min]: " + warten);
            MserverLog.systemMeldung("-----------------------------------");

            warten = 1000 * 60 * warten;
            t.join(warten);

            // ===========================================
            // erst mal schauen ob noch was läuft
            if (t != null) {
                if (t.isAlive()) {
                    MserverLog.fehlerMeldung(915147623, MserverSearch.class.getName(), "Der letzte Suchlauf läuft noch");
                    if (mSearch != null) {
                        MserverLog.systemMeldung("");
                        MserverLog.systemMeldung("");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("und wird jetzt gestoppt");
                        MserverLog.systemMeldung("Zeit: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("");
                        //und jetzt STOPPEN!!!!!!!!
                        mSearch.stop();
                    }

                    t.join(20 * 60 * 1000); // 20 Minuten warten, das Erstellen/Komprimieren der Liste dauert
                    if (t.isAlive()) {
                        MserverLog.systemMeldung("");
                        MserverLog.systemMeldung("");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("und noch gekillt");
                        MserverLog.systemMeldung("Zeit: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("================================");
                        MserverLog.systemMeldung("");
                        ret = false;
                    }
                    //jetzt ist Schicht im Schacht
                    t.stop();
                }
            }
        } catch (Exception ex) {
            MserverLog.fehlerMeldung(636987308, MserverSearch.class.getName(), "filmeSuchen", ex);
        }
        int l = mSearch.getListeFilme().size();
        MserverLog.systemMeldung("");
        MserverLog.systemMeldung("");
        MserverLog.systemMeldung("================================");
        MserverLog.systemMeldung("Filmliste Anzahl Filme: " + l);
        if (l < 10_000) {
            //dann hat was nicht gepasst
            MserverLog.systemMeldung("   Fehler!!");
            MserverLog.systemMeldung("================================");
            ret = false;
        } else {
            MserverLog.systemMeldung("   dann ist alles OK");
            MserverLog.systemMeldung("================================");

        }
        MserverLog.systemMeldung("filmeSuchen beendet");
        mSearch = null;
        return ret;
    }

    private String[] arrLesen(String s) {
        ArrayList<String> arr = new ArrayList<>();
        String tmp = "";
        s = s.trim();
        if (s.equals("")) {
            return null;
        }
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == ',') {
                if (!tmp.equals("")) {
                    arr.add(tmp);
                }
                tmp = "";
            } else {
                tmp += s.charAt(i);
            }
        }
        if (!tmp.equals("")) {
            arr.add(tmp);
        }
        return arr.toArray(new String[]{});
    }
}
