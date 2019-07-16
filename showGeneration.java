import java.util.*;
import java.io.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.stage.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.input.*;
import javafx.event.*;
import javafx.animation.*;
import javafx.util.*;

public class showGeneration extends Application{

	String filePath = "../generations/";

	Label score;
	Label generation;
	GridPane gameView;
	Rectangle[][] gameGrid;
	Snake current;
	double[] surroundings;
	Timeline updateGame;
	Boolean isFood = false;
	Generation currentGeneration;
	int genCount = 0;
	Child currentChild;
	int childIndex = 0;
	int length;
	NeuralNetwork NN;
	int move;
	double[] result;
	int duration;
	Scanner in = new Scanner(System.in);
	int genSize;
	double max;
	long fitness;

	FileOutputStream fileOut;
	ObjectOutputStream os;
	FileInputStream fileIn;
	ObjectInputStream oi;

	int longest = 0;
	Generation prevGeneration;

	public void save(String s){

		try{
			fileOut = new FileOutputStream(filePath + s + genCount + ".gen");
			os = new ObjectOutputStream(fileOut);
			os.writeObject(currentGeneration);
			os.close();
			fileOut.close();
			System.out.println("Successfully serialized current generation");
		} catch(Exception e){
			System.out.println("Could not serialize current generation");
			e.printStackTrace();
		}

	}

	public void load(String fileName){

		try{

			if(!fileName.equals("null")){
				fileIn = new FileInputStream(filePath + fileName);
				oi = new ObjectInputStream(fileIn);
				currentGeneration = (Generation) oi.readObject();

				if(updateGame != null){
					updateGame.stop();
				}

				childIndex = 0;
				refreshScene();
				genSize = currentGeneration.getChildren().length;
				currentGeneration.sort();
				startGame();

				oi.close();
				fileIn.close();
				System.out.println("Successfully loaded generation");
			}

		} catch(Exception e){
			System.out.println("Could not load generation");
			e.printStackTrace();
		}

	}
	
	public void printContents(){

		try{
			String[] files = new File(filePath).list();

			for(String f : files){

				if(f.substring(f.length() - 4, f.length()).equals(".gen")){
					System.out.println(f);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

	public static void main(String[] args){
		launch(args);
	}
	
	public void start(Stage mainStage){
		createScene(mainStage);
	}
	
	public void createScene(Stage mainStage){
		System.out.println("Please enter a grid size");
		length = in.nextInt();
		gameGrid = new Rectangle[length][length];
		BorderPane rootNode = new BorderPane();
		VBox infoView = new VBox();
		gameView = new GridPane();
		score = new Label();
		generation = new Label();
		infoView.getChildren().addAll(score, generation);
		infoView.setAlignment(Pos.CENTER_LEFT);
		infoView.setSpacing(20);
		
		for(int i = 0; i < length; i++){
			
			for(int j = 0; j < length; j++){
				gameGrid[i][j] = new Rectangle(10, 10);
				gameView.add(gameGrid[i][j], i, j);
				gameGrid[i][j].setFill(Color.BLACK);
			}
			
		}
		
		for(int i = 0; i < length; i++){
			gameGrid[length - 1][i].setFill(Color.WHITE);
			gameGrid[0][i].setFill(Color.WHITE);
			gameGrid[i][length - 1].setFill(Color.WHITE);
			gameGrid[i][0].setFill(Color.WHITE);
		}
		
		rootNode.setCenter(gameView);
		rootNode.setBottom(infoView);
		Scene main = new Scene(rootNode, 400, 400);
		main.setOnKeyPressed((event) -> {
			switch(event.getCode()){
				case SPACE:
					current.killSnake();
					break;
				default:
					break;
			}
			
			event.consume();
		});
		
		mainStage.setScene(main);
		mainStage.setMaximized(true);
		mainStage.show();
		NN = new NeuralNetwork(2, 36, 24, 4);
		System.out.println("Please enter duration in ms between steps");
		duration = in.nextInt();
		printContents();
		System.out.println("Enter name of requested file");
		in.nextLine();
		load(in.nextLine());
	}
	
	public void startGame(){
		startGame(currentGeneration.getChildren()[childIndex]);
	}
	
	public void startGame(Child currentCh){
		currentChild = currentCh;
		NN.setWeights(currentCh.getWeights());
		current = new Snake(gameGrid, length);

		updateGame = new Timeline(new KeyFrame(Duration.millis(duration), new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e){

				if(current.isAlive()){
					surroundings = current.getSurrounding();
					result = NN.getOutputs(surroundings);
					max = result[0];
					move = 0;

					for(int i = 1; i < result.length; i++){

						if(result.length > 1){
							
							if(result[i] > max){
								max = result[i];
								move = i;
							}

						}

					}

					switch(move){
						case 0:
							current.updatePos(0, 1);
							break;
						case 1:
							current.updatePos(0, -1);
							break;
						case 2:
							current.updatePos(1, 0);
							break;
						case 3:
							current.updatePos(-1, 0);
							break;
					}

					current.step();

					if(current.getLength() - 3 < 8){
						fitness = (long) Math.pow(2, current.getLength() - 3) * current.getTotalSteps() * current.getTotalSteps();
					} else{
						fitness = (long) Math.pow(2, 10) * (current.getLength() - 10) * current.getTotalSteps() * current.getTotalSteps();
					}
					
					score.setText("Score: " + current.getLength() + " Fitness: " + fitness);
				} else{
					refreshScene();
					currentChild.setFitness(fitness);
					currentGeneration.calcTotalFitness();
					childIndex++;
					
					if(current.getLength() > longest){
						longest = current.getLength();
					}

					if(prevGeneration != null){
						generation.setText(" Child " + childIndex + " Best: " + currentGeneration.getFittest().getFitness() + " Longest: " + longest + " Mean: " + currentGeneration.getAverageFitness() + " Difference: " + (currentGeneration.getAverageFitness() - prevGeneration.getAverageFitness()));
					} else{
						generation.setText(" Child " + childIndex + " Best: " + currentGeneration.getFittest().getFitness() + " Longest: " + longest + " Mean: " + currentGeneration.getAverageFitness());
					}
					
					if(childIndex < genSize){
						startGame(currentGeneration.getChildren()[childIndex]);
					} else{
						System.out.println("Generation finished");
					}
					
					updateGame.stop();
				}
				
				e.consume();
			}
		}));
		
		updateGame.setCycleCount(Timeline.INDEFINITE);
		updateGame.play();
	}
	
	public void refreshScene(){
		
		for(int i = 0; i < length; i++){
			
			for(int j = 0; j < length; j++){

				if(gameGrid[i][j].getFill() != Color.BLACK){
					gameGrid[i][j].setFill(Color.BLACK);
				}

			}
			
		}
		
		for(int i = 0; i < length; i++){
			gameGrid[length - 1][i].setFill(Color.WHITE);
			gameGrid[0][i].setFill(Color.WHITE);
			gameGrid[i][length - 1].setFill(Color.WHITE);
			gameGrid[i][0].setFill(Color.WHITE);
		}
		
	}
}