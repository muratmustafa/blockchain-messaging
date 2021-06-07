package sample;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller implements Initializable {

    private User u;

    @FXML
    private TextField user;

    @FXML
    private TextArea screen;
    
    @FXML
    private Button get;

    @FXML
    private TextField rec;

    @FXML
    private Button send;
    
    @FXML
    private Button check;

    @FXML
    public TextArea logs;

    @FXML
    public static TextArea chat;

    @FXML
    void getKeys(ActionEvent ae) throws NoSuchAlgorithmException, IOException, InterruptedException {	
        String uName=user.getText();
        u = new User(uName, Main.port, this);
        logs.setText("USER CREATED");
        u.start();
        Thread.sleep(500);
        u.broadcastPublicKey();
        get.setDisable(true);
    }    
    
    @FXML
    void sendMsg(ActionEvent ae) throws Exception {
        String recName = rec.getText();
        String msg = screen.getText();
        u.createMessage(msg, recName);
    }
    
    @FXML
    void displayAllMsgs(ActionEvent ae) throws Exception {
        screen.setText(u.printMyMessages());
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setLOG(String text)
    {
        logs.setText(logs.getText() + " " + text);
    }
}
