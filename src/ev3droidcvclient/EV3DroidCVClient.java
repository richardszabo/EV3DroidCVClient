package ev3droidcvclient;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;

/**
 * Based on EV3ClientTest.
 * @author rics
 */
public class EV3DroidCVClient {

    private Socket socket;
    private DataInputStream in;
    private final EV3 ev3 = (EV3) BrickFinder.getLocal();
    private final TextLCD lcd = ev3.getTextLCD();
    private final Keys keys = ev3.getKeys();
    private final RegulatedMotor leftMotor = new EV3LargeRegulatedMotor(MotorPort.A);
    private final RegulatedMotor rightMotor = new EV3LargeRegulatedMotor(MotorPort.B);
    String btIPPrefix = "10.0.1.";
    String wifiIPPrefix = "192.168.0.";
    boolean isBluetooth = true;
    int ipDomain = 0;
    int ipEnd = 0;
    final static int MODE_ROW = 0;
    final static int IP_ROW = 2;
    final static int STATUS_ROW = 4;
    final static int VALUE_POS = 10;
    final static int BASE_SPEED = 300;
    
    String getIP() {
        return (isBluetooth ? btIPPrefix : wifiIPPrefix) + ipEnd;
    }

    void drawRow(String string, int row) {
        lcd.clear(row);
        lcd.drawString(string, 0, row);        
    }
        
    void setMode(boolean isBluetooth) {
        ipDomain = (isBluetooth ? 0 : 100);
        drawRow(isBluetooth? "Bluetooth" : "Wi-Fi", MODE_ROW);                
        drawRow(getIP(), IP_ROW);
    }
        
    void init () {
        drawRow("IP:", IP_ROW-1);
        drawRow("Status:", STATUS_ROW-1);
        for(;;) {
            drawRow("Setting IP", STATUS_ROW);
            setMode(isBluetooth);
            int but = Button.waitForAnyPress();
            if( (but & Button.ID_ESCAPE) != 0 ) {
                System.exit(0);
            }
            if( (but & Button.ID_ENTER) != 0 ) {
                if( connect() ) {                
                    try {
                        run();
                    } catch (IOException e) {
                        drawRow("Disconnected",STATUS_ROW);
                        drawRow("E:" + e.getMessage(), STATUS_ROW+1);
                    }
                }
            } else if( (but & Button.ID_UP) != 0 ) {
                ipEnd = Math.min(ipEnd+1,99 + ipDomain); 
            } else if( (but & Button.ID_DOWN) != 0 ) {
                ipEnd = Math.max(ipEnd-1,ipDomain); 
            } else if( (but & Button.ID_LEFT) != 0 || (but & Button.ID_RIGHT) != 0) {
                isBluetooth = !isBluetooth;
                ipEnd = (isBluetooth ? 0 : 100);                
            } 
        }
    }
    
    boolean connect () {
        try {
            drawRow("Connecting...", STATUS_ROW);
            socket = new Socket(getIP(), 1234);
            in = new DataInputStream(socket.getInputStream());
            drawRow("Connected", STATUS_ROW);
            return true;
        } catch (IOException e) {
            drawRow("E:" + e.getMessage(), STATUS_ROW);
            keys.waitForAnyPress();
        }        
        return false;
    }

    boolean disconnect () {
        try {
            drawRow("Disconnecting", STATUS_ROW);
            socket.close();
            drawRow("Disconnected", STATUS_ROW);
            return true;
        } catch (IOException e) {
            drawRow("E:" + e.getMessage(), STATUS_ROW);
            keys.waitForAnyPress();
        }        
        return false;
    }
    
    void run() throws IOException {
        leftMotor.synchronizeWith(new RegulatedMotor[]{rightMotor});
        boolean finish = false;
        while (!finish) {
            double x = in.readDouble();            
            leftMotor.startSynchronization();
            drawRow("dir: " + String.format("%6.2f", x), STATUS_ROW+1);
            if( x < - 50 ) {
                leftMotor.stop(); 
                rightMotor.stop(); 
            } else {
                leftMotor.setSpeed((int)(BASE_SPEED * (1 + 0.8 * x))); 
                leftMotor.forward();
                rightMotor.setSpeed((int)(BASE_SPEED * (1 - 0.8 * x)));
                rightMotor.forward();
            }
            leftMotor.endSynchronization();
            if( Button.readButtons() != 0 ) {
                leftMotor.startSynchronization();
                leftMotor.stop(); 
                rightMotor.stop(); 
                leftMotor.endSynchronization();
                disconnect();
                finish = true;
            }
        }
    }
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        EV3DroidCVClient edcc = new EV3DroidCVClient();
        edcc.init();
    }
    
}
