package Player;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Klasa koja se koristi da bi se pomocu Java SOund API
 *
 */
public class AudioPlayer implements LineListener {

    private static final int SECONDS_IN_HOUR = 60 * 60;
    private static final int SECONDS_IN_MINUTE = 60;

    /**
     * Oznacava da li je zavrsilo sa sviranjem ili ne.
     */
    private boolean playCompleted;

    /**
     * Oznaka za to da li je zaustavljena ili pauzirana pjesma.
     */
    private boolean isStopped;

    private boolean isPaused;

    private Clip audioClip;

    /**
     * Ucitava audio fajl prije sviranja
     */
    public void load(String audioFilePath)
            throws UnsupportedAudioFileException, IOException,
            LineUnavailableException {
        File audioFile = new File(audioFilePath);

        //podesavanja za audio fajl koji se nalazi u audioFile
        AudioInputStream audioStream = AudioSystem
                .getAudioInputStream(audioFile);

        AudioFormat format = audioStream.getFormat();

        DataLine.Info info = new DataLine.Info(Clip.class, format);

        audioClip = (Clip) AudioSystem.getLine(info);

        audioClip.addLineListener(this);

        audioClip.open(audioStream);
    }
    
    // izracunava duzinu klipa u sekundama
    public long getClipSecondLength() {
        return audioClip.getMicrosecondLength() / 1000000;
    }

    //pretvara broj sekundi u HH:mm:ss format
    public String getClipLengthString() {
        String length;
        long hour;
        long minute;
        long seconds = audioClip.getMicrosecondLength()/ 1000000;

        System.out.println(seconds);

        hour = seconds / SECONDS_IN_HOUR;
        seconds %= SECONDS_IN_HOUR;
        length = ((hour < 10) ? ("0" + hour) : (hour)) + ":";
       
        minute = seconds / SECONDS_IN_MINUTE;
        seconds %= SECONDS_IN_MINUTE;

        length += ((minute < 10) ? ("0" + minute) : (minute)) + ":";
       
        length += ((seconds < 10) ? ("0" + seconds) : (seconds));

        return length;
    }

    /**
     * Pusta da svira zadati fajl.
     */
    void play() throws IOException {

        audioClip.start();

        playCompleted = false;
        isStopped = false;

        while (!playCompleted) {
            // ceka dok se zavrsi sviranje, 
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                
                if (isStopped) {
                    audioClip.stop();
                    break;
                }
                if (isPaused) {
                    audioClip.stop();
                } else {
                    System.out.println("!!!!");
                    audioClip.start();
                }
            }
        }

        audioClip.close();
    }

    /**
     * Kontrola sviranja (zaustavlja, pauzira, pusta).
     */
    public void stop() {
        isStopped = true;
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    /**
     * Slusa i ceka kada ce se zavrsiti ili zaustaviti.
     */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();
        if (type == LineEvent.Type.STOP) {
            System.out.println("STOP EVENT");
            if (isStopped || !isPaused) {
                playCompleted = true;
            }
        }
    }

    public Clip getAudioClip() {
        return audioClip;
    }
}
