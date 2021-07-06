package sample;

import dao.node.Node;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import util.PBFT;

import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private Node n;

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
    public TextArea chat;

    @FXML
    void getKeys(ActionEvent ae) throws NoSuchAlgorithmException, IOException, InterruptedException {	
        String uName = user.getText();
        n = Node.getInstance();
        n.setIndex(0);
        n.setUserName(uName);
        logs.setText("NODE CREATED");

        if (new PBFT().pubView()) {
            n.start();
            Thread.sleep(500);
            //n.broadcastPublicKey();
            get.setDisable(true);
        }else{
            System.exit(-1);
        }

    }    
    
    @FXML
    void sendMsg(ActionEvent ae) throws Exception {
        String recName = rec.getText();
        String msg = screen.getText();
        n.createMessage(msg, recName);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setLOG(String text)
    {
        logs.setText(logs.getText() + " " + text);
    }

    public void setChat(String text) {
        chat.setText(text);
    }
}
