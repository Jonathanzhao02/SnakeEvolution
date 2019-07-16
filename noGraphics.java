import java.util.*;
import java.io.*;
import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class noGraphics extends Application{

	String filePath = "../generations/";

	int[][] gameNumGrid;
	Snake current;
	double[] surroundings;
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
	long highestFitness = 0;
	long highestMean = 0;
	long highestMedian = 0;
	long currentFitness = 0;
	int trials = 50;
	int currentTrial = 0;
	int longestLength = 0;
	int genLongestLength = 0;

	FileOutputStream fileOut;
	ObjectOutputStream os;

	FileInputStream fileIn;
	ObjectInputStream oi;
	
	public void save(){

		try{
			fileOut = new FileOutputStream(filePath + genCount + ".gen");
			os = new ObjectOutputStream(fileOut);
			os.writeObject(currentGeneration);
			os.close();
			fileOut.close();
		} catch(Exception e){
			System.out.println("Could not serialize current generation");
			e.printStackTrace();
		}

	}

	public void save(String word){

		try{
			fileOut = new FileOutputStream(filePath + word + genCount + ".gen");
			os = new ObjectOutputStream(fileOut);
			os.writeObject(currentGeneration);
			os.close();
			fileOut.close();
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
				childIndex = 0;
				refreshScene();
				genSize = currentGeneration.getChildren().length;
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
		gameNumGrid = new int[length][length];
		
		for(int i = 0; i < length; i++){
			gameNumGrid[length - 1][i] = 1;
			gameNumGrid[0][i] = 1;
			gameNumGrid[i][length - 1] = 1;
			gameNumGrid[i][0] = 1;
		}
		
		NN = new NeuralNetwork(4, 72, 24, 4);

		System.out.println("Please enter generation size");
		genSize = in.nextInt();

		currentGeneration = new Generation(genSize, NN.getTotalSize(), 1);

		try{
			printContents();
			System.out.println("Enter name of requested file (null if no file wanted)");
			in.nextLine();
			load(in.nextLine());
		} catch(Exception e){
			e.printStackTrace();
		}

		startGame(currentGeneration);
	}
	
	public void startGame(Generation currentGen){
		currentGeneration = currentGen;

		do{
			startGame(currentGeneration.getChildren()[childIndex]);
		} while(true);
		
	}
	
	public void startGame(Child currentCh){
		currentChild = currentCh;
		NN.setWeights(currentCh.getWeights());
		current = new Snake(gameNumGrid, length);

		while(current.isAlive()){
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
		}

		refreshScene();

		if(current.getLength() - 3 < 8){
			fitness = (long) Math.pow(2, current.getLength() - 3) * current.getTotalSteps() * current.getTotalSteps();
		} else{
			fitness = (long) Math.pow(2, 10) * (current.getLength() - 10) * current.getTotalSteps() * current.getTotalSteps();
		}
		
		if(current.getLength() > longestLength){
			longestLength = current.getLength();
		}

		currentTrial++;
		
		if(currentTrial >= trials){
			currentChild.setFitness(currentFitness / trials);
			currentTrial = 0;
			currentFitness = 0;
			System.out.println("Child " + childIndex + " completed");
			childIndex++;

			if(childIndex >= genSize){
				currentGeneration.calcTotalFitness();
				System.out.println("Gen " + genCount + " Best: " + currentGeneration.getFittest().getFitness() + " Mean: " + currentGeneration.getAverageFitness() + " Median: " + currentGeneration.getChildren()[genSize / 2].getFitness() + " Longest: " + longestLength);
				
				if(genCount > 0){

					if(currentGeneration.getFittest().getFitness() > highestFitness){
						highestFitness = currentGeneration.getFittest().getFitness();
						save("fit" + highestFitness + "_");
					} else if(currentGeneration.getAverageFitness() > highestMean){
						highestMean = currentGeneration.getAverageFitness();
						save("mean" + highestMean + "_");
					} else if(currentGeneration.getChildren()[genSize / 2].getFitness() > highestMedian){
						highestMedian = currentGeneration.getChildren()[genSize / 2].getFitness();
						save("median" + highestMedian + "_");
					} else if(longestLength > genLongestLength){
						genLongestLength = longestLength;
						save("length" + genLongestLength + "_");
					}

					if(genCount % 10 == 0){
						save("ng");
					}

				}

				longestLength = 0;
				genCount++;
				System.out.println("Creating new generation");
				currentGeneration = new Generation(genSize, currentGeneration);
				save("new");
				System.out.println("Generation created");
				childIndex = 0;
			}
			
		} else{
			currentFitness += fitness;
			startGame(currentChild);
		}

	}
	
	public int calcIndexBase2(double[] array){
		int sum = 0;
		
		for(int i = 0; i < array.length; i++){
			sum += array[i] * Math.pow(2, i);
		}
		
		return sum;
	}
	
	public void refreshScene(){
		gameNumGrid = new int[length][length];
		
		for(int i = 0; i < length; i++){
			gameNumGrid[length - 1][i] = 1;
			gameNumGrid[0][i] = 1;
			gameNumGrid[i][length - 1] = 1;
			gameNumGrid[i][0] = 1;
		}
		
	}
}