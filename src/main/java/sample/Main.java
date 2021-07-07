package sample;

import dao.node.Miner;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import p2p.common.Const;

public class Main extends Application{

	public static Controller controllerHandle;

	@Override
	public void start(Stage primaryStage) throws Exception{

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
		Parent root = loader.load();
		controllerHandle = loader.getController();

		primaryStage.setTitle("Blockchain Messenger");
		primaryStage.setScene(new Scene(root, 650, 400));
		primaryStage.setResizable(true);
		primaryStage.show();
	}

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();

		/*Miner Mode*/
		//Miner pro = new Miner();
		//pro.start();

		/*User Mode*/
		launch(args);
		System.exit(0);

	}
}
