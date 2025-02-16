package game.dataManagement;

import game.MAIN;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class NewGameLoader {
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_PURPLE = "\u001B[35m";

    public NewGameLoader(){
        overwriteNpcJSON(MAIN.npcFilePath);
        overwriteGamesaveJSON(MAIN.gameSaveFilePath);
    }
    public NewGameLoader(String npcFilePath,String gameSaveFilePath){
        overwriteNpcJSON(npcFilePath);
        overwriteGamesaveJSON(gameSaveFilePath);
    }


    private void overwriteNpcJSON(String destinationPath) {
        try {
            File originalFile = new File(MAIN.npcTemplatePath);
            File destinationFile = new File(destinationPath);
            FileChannel src = new FileInputStream(originalFile).getChannel();
            FileChannel dest = new FileOutputStream(destinationFile).getChannel();
            dest.transferFrom(src, 0, src.size());

            System.out.println(ANSI_GREEN + "game.NpcController2: NPC JSON erfolgreich aus Template überschrieben" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_PURPLE + "NpcController: Fehler beim Überschreiben der NPC JSON aus Template:" + ANSI_RESET);
            e.printStackTrace();

        }
    }
    private void overwriteGamesaveJSON(String destinationPath) {
        try {
            File originalFile = new File(MAIN.gameSaveTemplateFilePath);
            File destinationFile = new File(destinationPath);
            FileChannel src = new FileInputStream(originalFile).getChannel();
            FileChannel dest = new FileOutputStream(destinationFile).getChannel();
            dest.transferFrom(src, 0, src.size());

            System.out.println(ANSI_GREEN + "game.NpcController2: GAMESAVE JSON erfolgreich aus Template überschrieben" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_PURPLE + "NpcController: Fehler beim Überschreiben der GAMESAVE JSON aus Template:" + ANSI_RESET);
            e.printStackTrace();
        }
    }


}
