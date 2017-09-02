package Player;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.filechooser.FileFilter;
 

/**
 * Glavni dio programa, izgled i glavna logika. 
 * NAPOMENA: Moze da svira samo *.wav fajlove
 */
public class SwingAudioPlayer extends JFrame implements ActionListener {

    // sve potrebne klase da bi moglo da radi.
    private AudioPlayer player = new AudioPlayer();
    private Thread playbackThread;
    private PlayingTimer timer;

    //prati da li svira i da li je zaustavljeno.
    private boolean isPlaying = false;
    private boolean isPause = false;

    private String audioFilePath;
    private String lastOpenPath;

    private JLabel labelFileName = new JLabel("Playing File:");
    private JLabel labelTimeCounter = new JLabel("00:00:00");
    private JLabel labelDuration = new JLabel("00:00:00");

    private JButton buttonOpen = new JButton("Open");
    private JButton buttonPlay = new JButton("Play");
    private JButton buttonPause = new JButton("Pause");

    // Slajder ili ona linija koja prati dokle je dosla pjesma
    private final JSlider sliderTime = new JSlider();

    /** Konstruktor
    * u konstruktoru se postavljaju mjesta svega sto ima na ekranu
    * i dodaje na Prozor koji se pojavljuje.
    */
    public SwingAudioPlayer() {
        setTitle("Java Audio Player");
        
        Font mainFont = new Font("Sans", Font.BOLD, 14);
        
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        buttonOpen.setFont(mainFont);

        buttonPlay.setFont(mainFont);
        buttonPlay.setEnabled(false);

        buttonPause.setFont(mainFont);
        buttonPause.setEnabled(false);

        labelTimeCounter.setFont(mainFont);
        labelDuration.setFont(mainFont);

        sliderTime.setPreferredSize(new Dimension(400, 20));
        sliderTime.setEnabled(false);
        sliderTime.setValue(0);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        add(labelFileName, constraints);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        add(labelTimeCounter, constraints);

        constraints.gridx = 1;
        add(sliderTime, constraints);

        constraints.gridx = 2;
        add(labelDuration, constraints);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        panelButtons.add(buttonOpen);
        panelButtons.add(buttonPlay);
        panelButtons.add(buttonPause);

        constraints.gridwidth = 3;
        constraints.gridx = 0;
        constraints.gridy = 2;
        add(panelButtons, constraints);

        buttonOpen.addActionListener(this);
        buttonPlay.addActionListener(this);
        buttonPause.addActionListener(this);

        pack();
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Kontorlise dogadjaje pritisaka na dugmad.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof JButton) {
            JButton button = (JButton) source;
            if (button == buttonOpen) {
                openFile();
            } else if (button == buttonPlay) {
                if (!isPlaying) {
                    playBack();
                } else {
                    stopPlaying();
                }
            } else if (button == buttonPause) {
                if (!isPause) {
                    pausePlaying();
                } else {
                    resumePlaying();
                }
            }
        }
    }

    // metoda za otvaranje menija za biranje pjesme koju cemo pustiti
    private void openFile() {
        // Prozor za biranje.
        JFileChooser fileChooser = null;

        // ukoliko je prije otvarano nesto i put do foldera nije prazan
        if (lastOpenPath != null && !lastOpenPath.equals("")) {
            fileChooser = new JFileChooser(lastOpenPath);
        } else {
            fileChooser = new JFileChooser();
        }

        FileFilter wavFilter = new FileFilter() {
            @Override
            public String getDescription() {
                return "Sound file (*.WAV)";
            }

            //kada se otvori fileChooser ukoliko pritisnemo na foder, da ga otvori
            // a ukoliko nije folder da vrati da li je to nas trazeni oblik (*.wav)
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    return file.getName().toLowerCase().endsWith(".wav"); 
                }
            }
        };

        fileChooser.setFileFilter(wavFilter);
        fileChooser.setDialogTitle("Open Audio File");
        fileChooser.setAcceptAllFileFilterUsed(false);

        int userChoice = fileChooser.showOpenDialog(this);
        
        //Ukoliko je fajl izabran ispravno
        if (userChoice == JFileChooser.APPROVE_OPTION) {
            audioFilePath = fileChooser.getSelectedFile().getAbsolutePath();
            lastOpenPath = fileChooser.getSelectedFile().getParent();
            if (isPlaying || isPause) {
                stopPlaying();
                while (player.getAudioClip().isRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            playBack();
        }
    }

    /**
     * Metoda za pustanje muzike.
     */
    private void playBack() {
        timer = new PlayingTimer(labelTimeCounter, sliderTime);
        timer.start();
        isPlaying = true;
        playbackThread = new Thread(() -> {
            try {
                
                buttonPlay.setText("Stop");
                buttonPlay.setEnabled(true);
                
                buttonPause.setText("Pause");
                buttonPause.setEnabled(true);
                
                player.load(audioFilePath);
                timer.setAudioClip(player.getAudioClip());
                labelFileName.setText("Playing File: " + audioFilePath);
                sliderTime.setMaximum((int) player.getClipSecondLength());
                
                labelDuration.setText(player.getClipLengthString());
                player.play();
                
                resetControls();
                
            } catch (UnsupportedAudioFileException ex) {
                JOptionPane.showMessageDialog(SwingAudioPlayer.this,
                        "The audio format is unsupported!", "Error", JOptionPane.ERROR_MESSAGE);
                resetControls();
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(SwingAudioPlayer.this,
                        "Could not play the audio file because line is unavailable!", "Error", JOptionPane.ERROR_MESSAGE);
                resetControls();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(SwingAudioPlayer.this,
                        "I/O error while playing the audio file!", "Error", JOptionPane.ERROR_MESSAGE);
                resetControls();
            }
        });

        playbackThread.start();
    }

    private void stopPlaying() {
        isPause = false;
        buttonPause.setText("Pause");
        buttonPause.setEnabled(false);
        timer.reset();
        timer.interrupt();
        player.stop();
        playbackThread.interrupt();
    }

    private void pausePlaying() {
        buttonPause.setText("Resume");
        isPause = true;
        player.pause();
        timer.pauseTimer();
        playbackThread.interrupt();
    }

    private void resumePlaying() {
        buttonPause.setText("Pause");
        isPause = false;
        player.resume();
        timer.resumeTimer();
        playbackThread.interrupt();
    }

    private void resetControls() {
        timer.reset();
        timer.interrupt();

        buttonPlay.setText("Play");

        buttonPause.setEnabled(false);

        isPlaying = false;
    }
}
