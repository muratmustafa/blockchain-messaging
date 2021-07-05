package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import p2p.common.Const;

public class Main extends Application{
	/*
	 * Port no. at which users and miner communicate
	 * Change port no. if you get error like Bind Problem: Address already in use
	 * Users and miner should be connected to same Wi-Fi Network
	 */
	public static final int port = Const.PORT;

	public static Controller controllerHandle;


	@Override
	public void start(Stage primaryStage) throws Exception{

		FXMLLoader loader=new FXMLLoader(getClass().getResource("/main.fxml"));

		//FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
		Parent root = loader.load();

		primaryStage.setTitle("Blockchain Messenger");
		primaryStage.setScene(new Scene(root, 650, 400));
		primaryStage.setResizable(true);
		primaryStage.show();
	}

	public static void main(String[] args) throws Exception {
		//Miner Mode
		//Miner pro = new Miner("pro",port, controllerHandle);
		//pro.start();

		//User Mode
		launch(args);
		System.exit(0);

	}
}
