package game.dialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ea.Bild;
import ea.Farbe;
import ea.Knoten;
import ea.Text;
import game.dataManagement.GameSaver;
import game.MAIN;
import game.character.NPC;
import game.character.NpcController;
import game.screen.ComputerScreen;
import game.screen.EndScreen;
import game.screen.FadeScreen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class DialogController extends Knoten {
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_PURPLE = "\u001B[35m";

    private final String dialogLinesPath = "./Assets/Files/Dialoge.json";
    private final String dialogPacketsPath = "./Assets/Files/DialogPackets.json";


    //GLOBAL STUFF;
    private String globalTemporalPosition;


    //Mode booleans
    private boolean active = false;
    private boolean waitingForInput = false;

    //sub-Objects
    private final NpcController NPC_Controller2;
    private final GameSaver gameSaver;
    private final EndScreen endScreen;
    private final ComputerScreen computerScreen;
    private final FadeScreen fadeScreen;

    //JSON GSON
    private Map<String, DialogLine> dialogLines; //für die Json mit den DialogZeilen
    private Map<String, Map<String, List<DialogPacket>>> dialogPackets; //für die Json mit den DialogPackets

    //VISIBLE STUFF;
    private String defaultPath = "./Assets/Dialoge/";
    private Text displayTextObject;
    private Text displayTextNpcName;
    private Bild displayDialogBackgroundLeft;
    private Bild displayDialogBackgroundRight;
    private Bild displayArrowLeft;
    private Bild displayArrowRight;

    //Text Stuff
    private final int textPosY = 675;
    private final int defaultTextSize = 28;
    private final int maxTextWidth = 800;
    private final int nameTextPosY = 625;

    //DIALOG LINE STUFF;
    private String currentDialogCode;
    private String lastDialogCode;
    private boolean lastLineHadChoice = false;

    //Dialog Weg selection
    private int selection = 0;

    //playingLastLine
    private boolean playingLastLine = false;


    //lastLine
    private Map<String, String> lastLines = new HashMap<String, String>() {
    }; //<NAME, INHALT>

    //GESCHIHCTER DIE ANGEZEIGT WERDEN
    private Map<String, Bild> npcFaces = new HashMap<String, Bild>() {
    }; //<NAME, INHALT>

    private final int faceLocationX = 100;
    private final int faceLocationY = 620;
    private final int selfFaceLocationX = MAIN.x - faceLocationX;

    boolean waiting;
    Date lastDialogTime;


    public DialogController(NpcController NPC_C2, GameSaver gs, EndScreen eS, ComputerScreen cS, FadeScreen fS) {
        this.NPC_Controller2 = NPC_C2;
        this.gameSaver = gs;
        this.endScreen = eS;
        this.computerScreen = cS;
        this.fadeScreen = fS;

        waiting=false;

        //initialisert
        readJSON_DialogLines();
        readJSON_DialogPackets();

        addDisplayObjects();

        lastDialogTime=new Date();

        globalTemporalPosition = gameSaver.getTemporalPosition();
        NPC_Controller2.updateNpcPositions(globalTemporalPosition);
    }

    /**
     * Fügt alle sichtbaren Elemente zum Knoten zu und regelt Bildimporte
     */
    private void addDisplayObjects() {

        //beide Pfeile


        //beide Dialog blasen
        try { //Bilder mit try catch
            displayDialogBackgroundLeft = new Bild(0, 0, defaultPath + "DialogFensterLeft.png");
            displayDialogBackgroundRight = new Bild(0, 0, defaultPath + "DialogFensterRight.png");
            displayDialogBackgroundLeft.sichtbarSetzen(false);
            displayDialogBackgroundRight.sichtbarSetzen(false);
            this.add(displayDialogBackgroundLeft, displayDialogBackgroundRight);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.DialogController5: FEHLER beim Importieren der Bilder" + ANSI_RESET);
        }
        try { //Bilder mit try catch
            displayArrowLeft = new Bild(0, 0, defaultPath + "arrowLeft.png");
            displayArrowRight = new Bild(0, 0, defaultPath + "arrowRight.png"); //eigentlich beide bei 0,0
            displayArrowLeft.sichtbarSetzen(false);
            displayArrowRight.sichtbarSetzen(false);
            this.add(displayArrowLeft, displayArrowRight);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.DialogController5: FEHLER beim Importieren der Bilder" + ANSI_RESET);
        }

        //NPC Faces
        HashMap<String, NPC> NpcMap = NPC_Controller2.getNPCs();
        Bild selfFace = new Bild(selfFaceLocationX, faceLocationY, MAIN.playerStillImgPath);
        npcFaces.put("self", selfFace);
        this.add(selfFace);
        for (String name : NpcMap.keySet()) {
            try {
                Bild tempImg = new Bild(faceLocationX, faceLocationY, MAIN.npcFacesPath + name + ".png");
                this.add(tempImg);
                tempImg.sichtbarSetzen(false);
                npcFaces.put(name, tempImg);
                System.out.println("game.DialogController5: Neues Gesicht hinzugefügt mit dem name: " + name);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("game.DialogController5: FEHLER beim Importieren der Gesicht-Bilder");
            }
        }

        //content text field
        displayTextObject = new Text(MAIN.x / 2, textPosY, "DEFAULT TEXT");
        displayTextObject.mittelpunktSetzen(MAIN.x / 2, textPosY);
        displayTextObject.farbeSetzen(new Farbe(0, 0, 0));
        displayTextObject.groesseSetzen(defaultTextSize);
        displayTextObject.setzeFont("Monospaced");
        this.add(displayTextObject);

        //NPC name text field
        displayTextNpcName = new Text(MAIN.x / 2, textPosY, "DEFAULT NPC NAME");
        displayTextNpcName.mittelpunktSetzen(MAIN.x / 2, nameTextPosY);
        displayTextNpcName.farbeSetzen(new Farbe(0, 0, 0));
        displayTextNpcName.groesseSetzen(defaultTextSize);
        displayTextNpcName.setzeSchriftart(1);
        displayTextNpcName.setzeFont("Monospaced");
        this.add(displayTextNpcName);
        hideWindow();
    }


    /**
     * Main Methode die auch von game.SPIEL aufgerufen wird.
     *
     * @param npcID String ID des NPCs
     */
    public void startDialog(String npcID) { //Voraussetztung Kollision mit NPC und activ=false;

        selection = 0;
        playingLastLine = false;
        waitingForInput = true;
        active = true;
        if (isDialogPacketPlayable(npcID)) {
            //start eines neuen Dialogpacketes
            DialogController.DialogPacket element = getPlayableDialogPacket(npcID);
            currentDialogCode = element.code;

            lastDialogCode = currentDialogCode;
            lastLineHadChoice = !dialogLines.get(currentDialogCode).hasNoChoice();
            if (lastLineHadChoice) {
                updateArrows();
            } else {
                hideAllArrows();
            }


            showWindow();
            displayDialogLine(currentDialogCode);
        } else { //Es wird kein Dialogpacket für diesen NPC gefunden worden
            System.out.println("game.old.DialogController4: WÜRDE JZ LASTLINE SPIELEN MACHT ES ABER AUS TESTGRÜNDEN NOCH NICHT");
            playLastLine(npcID);
        }

        waiting =true;
    }

    public void highLightReadyNpcs() {
        NPC_Controller2.disguiseAllNPCs();
        for (String name : getHighlightedNpcNames()) {
            NPC_Controller2.highLightNpcsByName(name);
        }
    }

    public List<String> getHighlightedNpcNames() {
        List<String> nameList = new ArrayList<String>() {
        };
        //nameList = null;
        for (String name : NPC_Controller2.getNPCs().keySet()) {
            if (isDialogPacketPlayable(name)) {
                nameList.add(name);

            }
        }
        return nameList;
    }

    public List<NPC> getHighlightedNpcObjects() {
        List<NPC> npcList = new ArrayList<NPC>() {
        };
        //nameList = null;
        for (String name : NPC_Controller2.getNPCs().keySet()) {
            if (isDialogPacketPlayable(name)) {
                npcList.add(NPC_Controller2.getNPCs().get(name));
            }
        }
        return npcList;
    }


    public void setNpcFace(String npcName) {
        hideAllFaces();
        if (npcName.equals("self")) {
            //System.out.println("SELF SPRICHT");
        }
        //System.out.println("Der NPC mit dem Namen = " + npcName + " wird neben dem Fenster als FACE abgebildet");
        npcFaces.get(npcName).sichtbarSetzen(true);
    }

    public void hideAllFaces() {
        for (String name : npcFaces.keySet()) {
            npcFaces.get(name).sichtbarSetzen(false);
        }
    }

    private void saveLastLines() {
        //System.out.println("game.DialogController5: Die LastLines der NPCs werden im NPC_Controller gespeichert(NPCs-NEW.json)");
        for (String npcName : lastLines.keySet()) {
            if (npcName.equals("self")) {
                //überspringe diese Line
            } else {
                String code = lastLines.get(npcName);
                NPC_Controller2.setNpcLastLine(npcName, code);
            }

        }
        lastLines.clear();
    }

    private void endDialog() {

        active = false;
        waitingForInput = false;

        //not needed anymore bc of Input question to start dialog
        //NPC_Controller2.resetToLastQuietPos();

        NPC_Controller2.updateNpcPositions(globalTemporalPosition);
        currentDialogCode = null;
        hideWindow();
        saveLastLines();
        highLightReadyNpcs();
        lineSpecialAction(globalTemporalPosition);

    }

    private void playLastLine(String npcID) {
        playingLastLine = true;
        System.out.println("game.DialogController5: playLastLine() aufgerufen");
        DialogLine lastLine = dialogLines.get(NPC_Controller2.getNpcLastLine(npcID));
        if (lastLine == null) {
            updateTextContent("FEHLER DER NPC HAT KEINE LASTLINE");
            try{Thread.sleep(50);}
            catch(Exception e){
                e.printStackTrace();
            }
            endDialog();
        } else {
            displayDialogLine(NPC_Controller2.getNpcLastLine(npcID));
        }
    }

    public void nextLine() {
        if (!playingLastLine) {
            System.out.println("game.DialogController5: Es wird die Zeile mit den Code: " + currentDialogCode + " abgespeichert!");
            gameSaver.addLine(currentDialogCode); //speicher alle Abgespielen Lines in der JSON
            lastDialogCode = currentDialogCode;
            lastLineHadChoice = !dialogLines.get(lastDialogCode).hasNoChoice();
            if (lastLineHadChoice) {
                updateArrows();
            } else {
                hideAllArrows();
            }

            DialogLine currentLine = dialogLines.get(currentDialogCode);
            if (currentLine.hasNextTime()) {
                globalTemporalPosition = currentLine.nextTime;
                System.out.println("Der Dialog wird jz beendet mit und es wird zur Zeit: " + currentDialogCode + " gewechselt!");
                endDialog();
            } else {
                if (currentLine.hasNoChoice()) {
                    System.out.println("Bei dem Dialog wird mit der 1.Wahl weitergemacht weile er keine Wahl hat");
                    currentDialogCode = currentLine.wahl1;
                } else if (selection == 0) {
                    //hat nic bei wahl2 dirnstehen
                    System.out.println("Bei dem Dialog wird mit der 1.Wahl weitergemacht bei slection: " + selection);
                    currentDialogCode = currentLine.wahl1;
                } else if (selection == 1) {
                    System.out.println("Bei dem Dialog wird mit der 2.Wahl weitergemacht bei slection: " + selection);
                    //wenn es eine 2.Wahl gibt und wahl != 1
                    currentDialogCode = currentLine.wahl2;
                }
                displayDialogLine(currentDialogCode);
            }
        } else { //isPlayingLastLine
            endDialog();
        }

    }

    private Stream<DialogPacket> getPlayableDialogs(String npcID) {
        return dialogPackets
                .getOrDefault(globalTemporalPosition, Collections.emptyMap())
                .getOrDefault(npcID, Collections.emptyList())
                .stream()
                .filter(
                        packet ->
                                gameSaver.getItems().containsAll(packet.requiredItems) &&
                                        gameSaver.getLines().containsAll(packet.requiredLines) &&
                                        inverseContains(gameSaver.getLines(), packet.forbiddenLines)

                );
    }

    private DialogController.DialogPacket getPlayableDialogPacket(String npcID) {
        return getPlayableDialogs(npcID)
                .findFirst()
                .orElse(null);
    }

    public boolean isDialogPacketPlayable(String npcID) {
        return getPlayableDialogs(npcID)
                .findAny()
                .isPresent();
    }

    public void showWindow() {
        displayTextObject.sichtbarSetzen(true);
        displayTextNpcName.sichtbarSetzen(true);
        displayDialogBackgroundLeft.sichtbarSetzen(true);
        displayDialogBackgroundRight.sichtbarSetzen(true);
    }

    public void hideWindow() {
        hideAllFaces();
        displayTextObject.sichtbarSetzen(false);
        displayTextNpcName.sichtbarSetzen(false);
        displayDialogBackgroundLeft.sichtbarSetzen(false);
        displayDialogBackgroundRight.sichtbarSetzen(false);
        hideAllArrows();
    }

    public void lineSpecialAction(String timeCode) {
        System.out.println("game.DialogController5: lineSpecialAction() aufegerufen mit der Zeit: " + timeCode);
        switch (timeCode) {

            case ("Ende(felix)"):
            case ("Ende (Posts gelöscht)"):

                //e.g. show PC screen
                endScreen.playEnding(false);
                break;

            case ("Ende(Tim)"):
            case ("Tag 5 Ende(Acc gelöscht)"):

                endScreen.playEnding(true);
                break;

            case ("Tag 1 Abschnitt 3"):
                computerScreen.viewPost1();
                break;

            case ("Tag 3 Abschnitt 2"):
                computerScreen.viewPost2();
                break;

            case ("Tag 2 Abschnitt 1"):
            case ("Tag 3 Abschnitt 1"):
            case ("Tag 4 Abschnitt 1"):
            case ("Tag 5 Abschnitt 1"):

                fadeScreen.startBlackFade();
                break;

            default:
                break;

        }
    }


    public void displayDialogLine(String lineCode) {
        DialogLine dL = dialogLines.get(lineCode);
        setNpcFace(dL.name);
        setDialogWindowDir(dL.isSelf());
        updatePartnerName(dL.name, dL.isSelf());
        updateTextContent(dL.inhalt);
        System.out.println("game.DialogController5: Zeigt jetzt die Zeile: " + lineCode + " an!");
        lastLines.put(dL.name, lineCode); //self wird auch mitgespeicher und später rausgenommen
    }

    public void updatePartnerName(String name, boolean isSelf) {
        if (isSelf) {
            String inhalt = "Antwort:";
            displayTextNpcName.inhaltSetzen(inhalt);
            displayTextNpcName.mittelpunktSetzen(MAIN.x /2, nameTextPosY);
            displayTextNpcName.sichtbarSetzen(true);
        } else {
            String displayName = NPC_Controller2.getNPCs().get(name).displayName;
            if (displayName.equals("")) {
                displayTextNpcName.sichtbarSetzen(false);
            } else {
                String inhalt = displayName + ":";
                displayTextNpcName.inhaltSetzen(inhalt);
                displayTextNpcName.mittelpunktSetzen(MAIN.x /2, nameTextPosY);
                displayTextNpcName.sichtbarSetzen(true);
            }
        }
    }

    public void updateTextContent(String inhalt) {
        displayTextObject.sichtbarSetzen(true);
        displayTextObject.inhaltSetzen(inhalt);
        displayTextObject.groesseSetzen(defaultTextSize);
        while (displayTextObject.getBreite() > maxTextWidth) {
            displayTextObject.groesseSetzen(displayTextObject.groesse() - 1);
        }
        displayTextObject.mittelpunktSetzen(MAIN.x / 2, textPosY);
    }


    public void setDialogWindowDir(boolean isSelf) {
        displayDialogBackgroundLeft.sichtbarSetzen(false);
        displayDialogBackgroundRight.sichtbarSetzen(false);
        if (!isSelf) {
            //links wenn nicht selber
            displayDialogBackgroundLeft.sichtbarSetzen(true);
            //System.out.println("DialogController: DISPLAY: es wird das nach Links angezeigt");
        } else {
            //rechts wenn selber
            displayDialogBackgroundRight.sichtbarSetzen(true);
            //System.out.println("DialogController: DISPLAY: es wird das nach Rechts angezeigt");
        }
    }

    public void updateArrows() {
        if (isLastLineHadChoice()) {
            hideAllArrows();
            if (selection == 0) { //selection steht links, also nach rechts ein switch möglich
                displayArrowRight.sichtbarSetzen(true);
            } else {
                displayArrowLeft.sichtbarSetzen(true);
            }
        } else {
            System.out.println("game.DialogController5: Komischer Fehler in updateArrows()");
        }
    }

    public void hideAllArrows() {
        displayArrowLeft.sichtbarSetzen(false);
        displayArrowRight.sichtbarSetzen(false);
    }

    public void input(String dir) {
        if (isWaitingForInput()) {
            switch (dir) {
                case "links":
                    if (lastLineHadChoice) {
                        selection--;
                        if (selection < 0) {
                            selection = 0;
                        }
                        currentDialogCode = dialogLines.get(lastDialogCode).wahl1;
                        displayDialogLine(currentDialogCode);
                        updateArrows();
                    } else {
                        selection = 0;
                    }

                    //displayNextDialogLine();

                    break;

                case "rechts":
                    if (lastLineHadChoice) {
                        selection++;
                        if (selection > 1) {
                            selection = 1;
                        }
                        currentDialogCode = dialogLines.get(lastDialogCode).wahl2;
                        displayDialogLine(currentDialogCode);
                        updateArrows();
                    } else {
                        selection = 0;
                    }


                    //displayNextDialogLine();

                    break;

                case "enter":
                    nextLine();
                    break;

                default:
                    System.out.println(ANSI_PURPLE + "game.old.DialogController4: FEHLER Kein valider Input" + ANSI_RESET);

            }
        } else {
            System.out.println(ANSI_PURPLE + "game.old.DialogController4: FEHLER: wartet nicht auf Input" + ANSI_RESET);
        }
    }


    public String getGlobalTemporalPosition() {
        return globalTemporalPosition;
    }

    public boolean isWaitingForInput() {
        return waitingForInput;
    }

    public String getCurrentDialogCode() {
        return currentDialogCode;
    }

    public boolean isLastLineHadChoice() {
        return lastLineHadChoice;
    }

    public static <T> boolean inverseContains(List<T> a, List<T> b) {
        if (b == null) {
            //nur ForbiddenItems leer
            return true;
        } else if (a == null) {
            return false;
        } else {
            return a.stream().noneMatch(b::contains);
        }

    }

    public boolean isActive() {
        return active;
    }

    public int getSelection() {
        return selection;
    }

    //JSON SACHEN UND DEREN KLASSEN
    private void readJSON_DialogLines() {
        Gson gson = new Gson();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dialogLinesPath));

            Type MapType = new TypeToken<Map<String, DialogLine>>() {
            }.getType();
            dialogLines = gson.fromJson(bufferedReader, MapType);
            System.out.println();
            System.out.println(ANSI_GREEN + "game.DialogController5: JSON(" + dialogLinesPath + ")  erfolgreich gelesen" + ANSI_RESET);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.DialogController5: Ein Fehler beim Lesen der Json Datei(" + dialogLinesPath + " ). Entweder Pfad flasch, oder JSON Struktur." + ANSI_RESET);

        }

    }

    private void readJSON_DialogPackets() {
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(dialogPacketsPath));

            Type MapType = new TypeToken<Map<String, Map<String, List<DialogPacket>>>>() {
            }.getType();
            dialogPackets = gson.fromJson(bufferedReader, MapType);
            System.out.println(ANSI_GREEN + "game.DialogController5: JSON(" + dialogPacketsPath + ")  erfolgreich gelesen" + ANSI_RESET);
            //System.out.println("ANTWORT: " + dialogPackets.get("01").get("11").NpcID);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(ANSI_PURPLE + "game.DialogController5: FEHLEE beim Lesen der Json Datei(" + dialogPacketsPath + " ). Entweder Pfad flasch, oder JSON Struktur." + ANSI_RESET);

        }
    }

    /**
     * JF:
     * Das ist die Klasse die als Muster zum Auslesen des JSON (mit GSON) dient.
     * Alle Methoden hierdrinn sind also nur für eine Textzeile im allgemeinen verwendbar und selten brauchbar.
     * Eigentlich muss in dieser Klasse nicht geändert werden
     */
    public class DialogLine {

        String inhalt; //Text der Dialog Zeile
        String name; //NPC bei dem der Dialog abgespielt wird
        String wahl1; // Code der Ersten Wahl
        String wahl2; // Code der Zweiten Wahl

        String nextTime; //nexter Zeitabschnitt, leer wenn nicht letzter

        @Override
        public String toString() {
            return "DialogLine{" +
                    "inhalt='" + inhalt + '\'' +
                    ", name='" + name + '\'' +
                    ", wahl1='" + wahl1 + '\'' +
                    ", wahl2='" + wahl2 + '\'' +
                    ", nextTime='" + nextTime + '\'' +
                    '}';
        }

        public boolean isSelf() {
            return (name.equals("self"));
        }

        public boolean hasNextTime() {
            //System.out.println("HAS NEXT TIME AUFGERUFEN: ANTWORT: " + !nextTime.equals(""));
            return (!nextTime.equals(""));
        }

        public boolean hasNoChoice() {
            return (wahl2.equals(""));
        }

    }

    public class DialogPacket {
        //key Time also "01!

        ArrayList<String> requiredItems;
        ArrayList<String> requiredLines;
        ArrayList<String> forbiddenLines;

        //NpcPosition npcPos;
        String code; // erster Code des Dialogs


    }

    public class NpcPosition {
        private String name;
        private float posX;
        private float posY;
        private int houseN;


        public NpcPosition(String name, int x, int y, int hn) {
            this.name = name;
            this.posX = x;
            this.posY = y;
            this.houseN = hn;
        }

        public float getPosX() {
            return posX;
        }

        public float getPosY() {
            return posY;
        }

        public int getHouseN() {
            return houseN;
        }

        public String getName() {
            return name;
        }

        /**
         * Don't use for now!
         *
         * @return
         */
        public boolean isInHouse() {
            if (houseN > -1) {
                return true;

            } else {
                //hier landet man auch mit falschen Eingaben!!
                return false;
            }
        }
    }


}

