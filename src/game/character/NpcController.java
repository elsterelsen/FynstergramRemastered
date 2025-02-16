package game.character;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ea.Knoten;
import ea.Punkt;
import game.dialog.DialogController;
import game.dataManagement.GameSaver;
import game.MAIN;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class NpcController extends Knoten {
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_PURPLE = "\u001B[35m";

    //Pfade
    private final String npcTemplatePath = MAIN.npcTemplatePath;
    private final String npcFilePath;
    private final String npcPositionPath = MAIN.npcPositionPath;


    //JSON GSON
    private HashMap<String, NPC> NPCs; //für die Json
    private Map<String, Map<String, DialogController.NpcPosition>> npcPositions; //für die Json mit den NPC Daten

    //for Position Reset
    private float lastQuietX = 0; //letzte Pos an der kein Dialog abgespielt wurde
    private float lastQuietY = 0;

    //für die anzeige
    //private int currentHouseNumber = -1; //muss eig als Map kommen und darf am anfang nicht unbedingt -1 sein;

    //Konstruktor Init //besonders JSONs Lesen ist langsam(?)
    public boolean initDone = false;


    private Player AP;
    private GameSaver gamesaver;

    public NpcController(Player mAP, GameSaver gs,String npcFilePath) {
        AP = mAP;
        this.npcFilePath=npcFilePath;
        this.gamesaver = gs;
        //resetJSON();

        readJSON();
        readNpcPositionJSON();
        //System.out.println("TESTETSTET POSX VON NAME: " + npcPositions.get("Tag 1 Abschnitt 1 (Start Zeit)").get("name").getPosX());
        addAllNPCs();

        //leaveHouse(); //muss am anfang außen sein
        updateNpcPositions(gamesaver.getTemporalPosition());
        /*
        NPCs.get("01").verschieben(100,0);
        saveJSON();

        */
        initDone = true;
    }

    public void startNewGame() {
        System.out.println("game.NpcController2");
        readJSON();
        readNpcPositionJSON();
        leaveHouse();
    }


    public boolean checkForCollision(Player AP) {
        boolean coll = false;
        //geht alle Npcs durch und schaut nach collision, aber nicht nach einer expliziten
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            if (element.schneidetNEU(AP) && element.sichtbar()) { //wenn der Spieler mit einem SICHTBAREN Npc kollidiert, dann weiter:
                //System.out.println("Spieler geschnitten und er ist sichtbar?? : " + element.sichtbar());
                coll = true;
            }
        }
        if (!coll) {
            lastQuietX = AP.getPosX();
            lastQuietY = AP.getPosY();
            //System.out.println("game.NpcController2; Der SPieler Kollidiert nicht, also wird die Pos gespeichert x = "  + lastQuietX);
        }
        return coll;
    }


    public String getCollidingNPC(Player AP) { //gibt den NPC KEY zurück mit dem geschnitten wurde

        String returnKey = null;
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            if (element.schneidetNEU(AP) && element.sichtbar()) { //wenn der Spieler mit einem SICHTBAREN Npc kollidiert, dann weiter:
                returnKey = key; // = current key
            }
        }
        if (returnKey == null) {
            System.out.println("game.NpcController2: Komisches Verhalten: Es wird nach expliziter Kollision gefragt, aber es gibt keine. Das kann für massive weitere Fehler sorgen");
            return null;
        } else {
            return returnKey; //gibt letzte Kollsion zurück
        }
    }

    public String getNpcLastLine(String npcID) {
        System.out.println("NPC_Controller: Es wird nach dem DialogCode für den Spieler mit der ID-> gefragt: " + npcID);
        System.out.println("NPC_Controller: Das game.NPC2 Objekt dazu sieht so aus: " + NPCs.get(npcID).toString());
        return NPCs.get(npcID).lastLine;
    }

    public void resetToLastQuietPos() {
        AP.positionSetzen(lastQuietX, lastQuietY);
    }


    public Punkt getLastQuietPos() {
        return new Punkt(lastQuietX, lastQuietY);
    }

    /**
     * Verschiebt die NPCs dieses Hauses an die entsprechende Position
     *
     * @param houseN  NouseNummer
     * @param offsetX Relle Postion der oberen ecke des Bildes
     * @param offsetY Relle Postion der oberen ecke des Bildes
     */
    public void enterHouse(int houseN, int offsetX, int offsetY) {
        System.out.println("NpcController: enterhouse wird aufgerufen()");
        // wird in Map geregelt gamesaver.setHouseNumber(houseN);
        //currentHouseNumber = houseN;
        hideAllNPCs();  //alle ausblenden
        //System.out.println("Der NPC Controller betritt auch ein Haus mit der Nummer: " + houseN);

        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            if (element.getHouseNumber() == houseN) {
                System.out.println("Der NPC names( " + element.name + " )ist im Haus sichtbar");
                //System.out.println("Er ist an der relativen Posiiton x:" + element.getRelativPosX() + " und y:" + element.getRelativPosY());
                this.add(element);
                element.sichtbarSetzen(true);
                element.positionSetzen(offsetX + (int) element.getRelativPosX(), offsetY + (int) element.getRelativPosY());
                System.out.println("Er ist an der relativen Posititon x:" + element.getRelativPosX() + " und y:" + element.getRelativPosY());
                System.out.println("Er ist an der absoluten Posititon x:" + element.getPosX() + " und y:" + element.getPosY());
                System.out.println(element);
            }
        }
    }

    public void setNpcLastLine(String name, String lineCode) {
        System.out.println("game.NpcController2: für den NPC:" + name + "wird der Dialog mit dem Code: " + lineCode + " gespeichert");
        NPC npc = NPCs.get(name);
        npc.setLastLine(lineCode);
        saveJSON();
    }

    public void highLightNpcs(Set keySet) {
        for (String key : NPCs.keySet()) {  //geht die JSON durch und macht alle aus(false)
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            element.setHighlightState(false);
        }
        if (!(keySet == null)) {
            for (Object npcName : keySet) {
                System.out.println("game.NpcController2: Der Spieler mit dem Namen:" + npcName + " wird jetzt gehighlightet!");
                NPCs.get(npcName).setHighlightState(true);
            }
        } else {
            System.out.println("game.NpcController2: FEHLER: EIN DER HIGHLIGHT KEYSET GRUPPE IST LEER. DAS LIEGT WAHRSCHEINLICH DARAN, DASS ES KEINE WEITEREN DIALOGPACKETE FÜTR DIE NEUE ZEIT GIBT!");
        }

    }

    public void highLightNpcsByName(String name) {
        System.out.println("Der NPC mit dem Namen (" + name  +") wird gehighlightet");
        if (name == null) {
            System.out.println("game.NpcController2: FEHLER: EIN DER HIGHLIGHT NAME  IST LEER. (DAS LIEGT Vielleicht DARAN, DASS ES KEINE WEITEREN DIALOGPACKETE FÜTR DIE NEUE ZEIT GIBT!)");
        } else {
            NPCs.get(name).setHighlightState(true);
        }
    }

    public void disguiseAllNPCs() {
        for (String key : NPCs.keySet()) {  //geht die JSON durch und macht alle aus(false)
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            element.setHighlightState(false);
        }
    }

    public void leaveHouse() {
        hideAllNPCs();

        //System.out.println("game.NpcController2: Haus wird verlassen");
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            if (!element.isInHouse()) {
                //System.out.println("Npc_Controller2: game.character.Player ID -> sichtbar: " + element.name);
                element.sichtbarSetzen(true);
            }
        }
    }

    public void updateNpcVisibility() {
        int currentHouseNumber = gamesaver.getHouseNumber();
        hideAllNPCs();
        //System.out.println("game.NpcController2: Haus wird verlassen");
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            if (element.getHouseNumber() == currentHouseNumber) {
                //System.out.println("Npc_Controller2: game.character.Player ID -> sichtbar: " + element.name);
                element.sichtbarSetzen(true);
            }
        }
    }


    private void hideAllNPCs() {
        System.out.println("NpcController4: HideAllNPCs() aufgerufen");
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal als "element" zur Verfügung
            element.sichtbarSetzen(false);
        }
    }

    private void addAllNPCs() {
        for (String key : NPCs.keySet()) {  //geht die JSON durch und
            NPC element = NPCs.get(key);   //stellt jedes Element der Map einmal in "element" zur Verfügung
            this.add(element);
            //System.out.println("Elemente : " + element.name);
        }
    }

    /**
     * Speichert die aktuellen Daten in der Java Map "NPCs" in der JSON
     */
    private void saveJSON() {
        try {
            Writer writer = new FileWriter(npcFilePath);
            Gson gson = new GsonBuilder() //man könnte noch den gleichen gson von dem readJson nehmen, aber geht auch so
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            String json = gson.toJson(NPCs); //kriegt json inhalt zum schreiben gleich
            writer.write(json);
            writer.close(); //wichtig

            System.out.println(ANSI_GREEN + "game.NpcController2: JSON(./Assets/Files/NPCs_NEW.json) erfolgreich gespeichert" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_PURPLE + "game.NpcController2: Ein Fehler beim SCHREIBEN der Json Datei:" + ANSI_RESET);
            System.out.println(e);
        }

    }

    public void updateNpcPositions(String timePos) {
        System.out.println("game.NpcController2: updateNpcPositions aufgerufen---------------------------------");
        Map<String, DialogController.NpcPosition> positionsAtTime = npcPositions.get(timePos);
        if (!(positionsAtTime == null)) {
            for (String key : positionsAtTime.keySet()) {  //geht die JSON durch
                try {
                    DialogController.NpcPosition posObject = positionsAtTime.get(key);
                    String name = key;

                    NPC npc = NPCs.get(name);
                    if (!(npc == null)) {
                        System.out.println("game.NpcController2: Die Position des NPCs mir dem Name (" + name + ") wird geupdatet");
                        //setzte Position und Hausnummer des NPCs mit dem entsprechenden Namen.
                            npc.setRelativPos((int)posObject.getPosX(), (int) posObject.getPosY()); //dann sind die relativen Pos = die absoluten

                        npc.positionSetzen(posObject.getPosX(), posObject.getPosY());
                        npc.setHouseNumber(posObject.getHouseN());

                        System.out.println("game.NpcController2: Er befindet sich jz um Haus: " + npc.getHouseNumber() + " und x:" + npc.getPosX() + "| y:" + npc.getPosY());
                    } else {
                        System.out.println("NpcController: FEHLER: NPC mit dem name (" + name + ") existiert nicht in der ursprünglichen JSON mit den NPCs");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("NpcController: FEHLER beim update der Positionen der NPCs");
                }

            }
            updateNpcVisibility();
        } else {
            System.out.println("NpcController: Für diese Zeit gibt es kein NPC Pos Update");
        }


    }

    public HashMap<String, NPC> getNPCs() {
        return NPCs;
    }

    /**
     * Liest die JSON(NPCs-Temple.json) Datei und schreibt ihren Inhalt in den Json(NPCs_NEW.json).
     */

    private void readJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(NPC.class, new NPC.Deserializer())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(npcFilePath));
            Type MapType = new TypeToken<HashMap<String, NPC>>() {
            }.getType();


            NPCs = gson.fromJson(bufferedReader, MapType);
            System.out.println(ANSI_GREEN + "game.NpcController2: JSON(" + npcFilePath + ") erfolgreich gelesen" + ANSI_RESET);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.NpcController2: Ein Fehler beim Lesen der JSON(" + npcFilePath + ") Datei. Entweder Pfad flasch, oder JSON Struktur." + ANSI_RESET);

        }
    }


    private void readNpcPositionJSON() {
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(npcPositionPath));

            Type MapType = new TypeToken<Map<String, Map<String, DialogController.NpcPosition>>>() {
            }.getType();
            npcPositions = gson.fromJson(bufferedReader, MapType);
            System.out.println(ANSI_GREEN + "game.NpcController2: JSON(" + npcPositionPath + ")  erfolgreich gelesen" + ANSI_RESET);
            //System.out.println("ANTWORT: " + dialogPackets.get("01").get("11").NpcID);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.NpcController2: Ein Fehler beim Lesen der Json Datei(" + npcPositionPath + " ). Entweder Pfad flasch, oder JSON Struktur." + ANSI_RESET);

        }

    }
    public NPC getNPC2(String npcID){
        if(!NPCs.containsKey(npcID)){return null;}
        return NPCs.get(npcID);
    }
}