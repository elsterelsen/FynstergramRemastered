import ea.Sound;

public class SoundController {

    //Main Stuff
    private String mainPath = "./Assets/Audio/";

    //Dateien
    private Sound sadMusic;
    private Sound doorSound;


    public SoundController(){
        sadMusic = new Sound(mainPath + "musik1.mp3");
        sadMusic.loop();
        doorSound = new Sound(mainPath + "door.wav");
    }

    public void playDoorSound(){
        doorSound.play();
    }
}
