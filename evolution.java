import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.chart.*;
import javafx.stage.*;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.scene.input.*;
import javafx.geometry.*;
import javafx.event.*;
import javafx.animation.*;
import javafx.util.*;

public class evolution extends Application{
    //Modifiables
    int genSize = 500;                                      //Population size
    int length = 55;                                        //Size of game grid
    int duration = 40;                                      //Milliseconds between frame updates
    double movePenalty = 2.5;                               //Penalty for each move away from food
    int trials = 20;                                        //Number of games to play per child
    double colorVibrancy = 0.5;                             //How vibrant connection colors are
    Color backgroundColor = new Color(0.05, 0.05, 0.05, 1); //Background color of application
    double distX = 150;                                     //Horizontal distance between neurons
    double distY = 30;                                      //Vertical distance between neurons
    double radius = 10;                                     //Neuron radius
    double historyHeight = 600;                             //History text container height
    
    //Neural network architecture (also modifiable)
    int layers = 4;             //Total number of layers (hidden layers + 2)
    int neuronsPerLayer = 18;   //Number of neurons in each hidden layer
    int inputs = 24;            //Number of inputs
    int outputs = 4;            //Number of outputs

    //File I/O objects
	FileOutputStream fileOut;
	ObjectOutputStream os;
	FileInputStream fileIn;
    ObjectInputStream oi;
    String filePath = "~/Downloads/Generations";

    //JavaFX graphic objects
    VBox infoView;                                      //Contains information about current graphic game
    Label score;                                        //Contains the current snake length
    Label fitnessScore;                                 //Contains the current snake fitness
    Label generation;                                   //Generation counter
    Label speed;                                        //Indicates current speed
    GridPane gameView;                                  //Contains the game itself
    Rectangle[][] gameGrid;                             //Data representation of the game grid
    Timeline updateGame;                                //Timeline thread that handles frame updates
    Pane neuralNet;                                     //Contains graphic representation of neural network
    Label evolutionHistory;                             //Contains history of background evolution
    ScrollPane historyContainer;                        //Just container for evolutionHistory
    LineChart<Number, Number> fitnessGraph;             //Contains chart used to graph fitness
    NumberAxis xAxis;                                   //Contains X-axis of graph
    NumberAxis yAxis;                                   //Contains Y-axis of graph
    XYChart.Series<Number, Number> maxFitnessHistory;   //Contains max fitness data for graph
    Circle[][] neurons = new Circle[layers][];          //Contains every circle that represents a neuron
    Line[][][] connections = new Line[layers - 1][][];  //Contains every line that represents a connection

    //Variables for graphic game
    int shownGencount = 0;                              //Tracks which generation the current snake being shown corresponds to
    double[][] gLayerOutputs = new double[layers][];    //Tracks outputs of each neuron in neural network
    Boolean currentlyPlaying = false;                   //Tracks if a game is already going on onscreen
    double rateModifier = 1;                            //Tracks how quickly to update frames
    double gMax;                                        //Tracks current max value from the neural network
    Snake gSnake;                                       //Snake used in graphic game
    NeuralNetwork gNN;                                  //Neural network used in graphic game
    double[] gSurroundings;                             //Inputs of gSnake to neural network
    double[] gResult;                                   //The outputs from the neural network
    int gMove;                                          //The move determined by the neural network
    long gFitness;                                      //The fitness of the graphic game snake

    //Variables for threads
    volatile Boolean resetting = false;                                             //Tracks if evolution is currently being reset
    Thread evolutionThread;                                                         //References thread used for background evolution
    LinkedBlockingQueue<Child> fittestMembers = new LinkedBlockingQueue<Child>();   //Queue used to communicate between threads
    Thread queueThread;                                                             //References thread used for dequeuing
    Runnable queueRunnable = () -> {                                                //Actual runnable used in dequeuing

        try{
            //Waits until a child is available from fittestMembers queue
            Child c = fittestMembers.take();

            //Runs on main thread
            Platform.runLater(() -> {

                //Checks that no game is currently in progress and evolution is not being reset
                if(!currentlyPlaying && !resetting){
                    showSnake(c);
                }

            });

        } catch(Exception e){
            
        }

    };

    //Variables for background evolution
    double max;                             //Tracks current max value of neural network
    long currentFitness = 0;                //Tracks sum of fitnesses of current child from individual games
    long fitness;                           //Tracks fitness of current child in current game
	int[][] gameNumGrid;                    //Data representation of the background evolution's game grid
    Snake current;                          //Snake used in background evolution
    NeuralNetwork NN;                       //Neural network used in background evolution
    double[] surroundings;                  //Inputs to neural network
    double[] result;                        //Outputs of neural network
    int move;                               //Move determined by neural network
    
    //Variables for generation
	int genCount = 0;               //Tracks how many generations have been created
    int childIndex = 0;             //Tracks how many children have been evaluated
    int currentTrial = 0;           //Tracks how many games have passed in the current evaluation
    Generation currentGeneration;   //References the current generation being evaluated
    Child currentChild;             //References current child being evaluated
    
    //Variables for keeping track of highest scores
	long highestFitness = 0;        //Tracks highest overall fitness of child
	long highestMean = 0;           //Tracks highest overall mean fitness
    long highestMedian = 0;         //Tracks highest overall median fitness
    int genLongestLength = 0;       //Tracks highest overall length
	int longestLength = 0;          //Tracks highest length in generation
    long highestTrialFitness = 0;   //Tracks highest trial fitness of each child

    //Miscellaneous objects
    Scanner in = new Scanner(System.in);    //Scanner for inputs on program start
    long[] seeds = new long[trials];        //Contains seeds so games can be replicated
    double[] penalties = new double[2];     //Tracks movement penalties from moving the wrong way (0 is background evolution, 1 is graphic game)
    
    //Just resets variables to default values
    public void resetVariables(){
        shownGencount = -1; //-1 to avoid displaying "1" if thread is interrupted when this is iterated
        genCount = 0;
        childIndex = 0;
        highestFitness = 0;
        highestMean = 0;
        highestMedian = 0;
        currentFitness = 0;
        currentTrial = 0;
        longestLength = 0;
        genLongestLength = 0;
        highestTrialFitness = 0;
        seeds = new long[trials];
        fitness = 0;
        penalties = new double[2];
        evolutionHistory.setText("");
    }

    //Saves with default name
	public void save(){
		save("");
	}

    //Saves current generation in file path with ".gen" extension
	public void save(String word){

		try{
			fileOut = new FileOutputStream(filePath + word + genCount + ".gen");
			os = new ObjectOutputStream(fileOut);
			os.writeObject(currentGeneration);
			os.close();
			fileOut.close();
		} catch(Exception e){
			//System.out.println("Could not serialize current generation");
			//e.printStackTrace();
		}

	}

    //Loads generation from ".gen" file and starts evolution
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
			//e.printStackTrace();
		}

	}

    //Prints contents of file path with ".gen" extension
	public void printContents(){

		try{
			String[] files = new File(filePath).list();

			for(String f : files){

                //Checks if last 4 letters are ".gen"
				if(f.substring(f.length() - 4, f.length()).equals(".gen")){
					System.out.println(f);
				}

			}

		} catch (Exception e) {
			//e.printStackTrace();
		}
	
    }
    
    //Prints current game state of background evolution
    public void printGameState(){

        for(int i = 0; i < length; i++){
            System.out.println();

            for(int j = 0; j < length; j++){
                System.out.print(" " + gameNumGrid[i][j]);
            }

        }

        System.out.println();
    }

    //Determines whether snake is currently moving away from the food
    public double calcPenalty(Snake s){
        double result = 0;

        if(s.posY >= s.randY && s.changeY == 1){
            result = movePenalty;
        } else if(s.posY <= s.randY && s.changeY == -1){
            result = movePenalty;
        } else if(s.posX >= s.randX && s.changeX == 1){
            result = movePenalty;
        } else if(s.posX <= s.randX && s.changeX == -1){
            result = movePenalty;
        }

        return result;
    }

    //Launches upon application launch
	public static void main(String[] args){
		launch(args);
	}

    //Sets up evolution
	public void start(Stage mainStage){
		createScene(mainStage);
	}
    
    //Draws the black and white network and fills the neurons and connections arrays
    public void drawNetwork(){
        Circle newCircle;   //References current neuron being drawn
        Line newLine;       //Refernces current connection being drawn

        //Input layer
        neurons[0] = new Circle[inputs];

        for(int i = 0; i < inputs; i++){
            newCircle = new Circle(0, (i + 1) * distY, radius, Color.TRANSPARENT);
            newCircle.setStroke(Color.BLACK);
            newCircle.setStrokeWidth(2);
            neurons[0][i] = newCircle;
        }

        //Hidden layers
        for(int i = 0; i < layers - 2; i++){
            neurons[i + 1] = new Circle[neuronsPerLayer];

            for(int j = 0; j < neuronsPerLayer; j++){
                newCircle = new Circle(distX * (i + 1), (j + 1 + (inputs - neuronsPerLayer) / 2) * distY, radius, Color.WHITE);
                newCircle.setStroke(Color.BLACK);
                newCircle.setStrokeWidth(2);
                neurons[i + 1][j] = newCircle;
            }

        }

        //Output layer
        neurons[layers - 1] = new Circle[outputs];

        for(int i = 0; i < outputs; i++){
            newCircle = new Circle(distX * (layers - 1), distY * (i + 1 + (inputs - outputs) / 2), radius, Color.WHITE);
            newCircle.setStroke(Color.BLACK);
            newCircle.setStrokeWidth(2);
            neurons[layers - 1][i] = newCircle;
        }

        //Input layer - hidden layer connections
        connections[0] = new Line[inputs][neuronsPerLayer];

        for(int i = 0; i < inputs; i++){

            for(int j = 0; j < neuronsPerLayer; j++){
                newLine = new Line(neurons[0][i].getCenterX(), neurons[0][i].getCenterY(), neurons[1][j].getCenterX(), neurons[1][j].getCenterY());
                connections[0][i][j] = newLine;
            }

        }

        //Hidden layer - hidden layer connections
        for(int i = 0; i < layers - 3; i++){
            connections[i + 1] = new Line[neuronsPerLayer][neuronsPerLayer];

            for(int j = 0; j < neuronsPerLayer; j++){
                
                for(int k = 0; k < neuronsPerLayer; k++){
                    newLine = new Line(neurons[i + 1][j].getCenterX(), neurons[i + 1][j].getCenterY(), neurons[i + 2][k].getCenterX(), neurons[i + 2][k].getCenterY());
                    connections[i + 1][j][k] = newLine;
                }

            }

        }

        //Hidden layer - output layer connections
        connections[layers - 2] = new Line[neuronsPerLayer][outputs];

        for(int i = 0; i < neuronsPerLayer; i++){

            for(int j = 0; j < outputs; j++){
                newLine = new Line(neurons[layers - 2][i].getCenterX(), neurons[layers - 2][i].getCenterY(), neurons[layers - 1][j].getCenterX(), neurons[layers - 1][j].getCenterY());
                connections[layers - 2][i][j] = newLine;
            }

        }

        //Adds connections to view
        for(int i = 0; i < layers - 1; i++){
            
            for(int j = 0; j < connections[i].length; j++){

                for(int k = 0; k < connections[i][j].length; k++){
                    neuralNet.getChildren().add(connections[i][j][k]);
                }

            }

        }

        //Adds neurons to view
        for(int i = 0; i < layers; i++){
            
            for(int j = 0; j < neurons[i].length; j++){
                neuralNet.getChildren().add(neurons[i][j]);
            }

        }

    }

    //Colors network according to weight and bias values
    public void colorNetwork(Child c){
        double maxVal = 0;                  //Tracks current maximum value among weights
        double[] weights = c.getWeights();  //Refernces weights of current child

        //Calculates highest weight value of child
        for(int i = 0; i < weights.length; i++){

            if(Math.abs(weights[i]) > maxVal){
                maxVal = Math.abs(weights[i]);
            }

        }

        Neuron[][] networkNeurons = gNN.getNeurons();       //References hidden layer neurons in neural network
        Neuron[] networkOutputs = gNN.getOutputNeurons();   //Refernces output layer in neural network

        double[] currentWeights;    //References current weights being used
        double currentBias;         //References current bias value being used

        Color newColor; //Tracks current color being assigned to connection/neuron

        //Hidden layers
        for(int i = 0; i < networkNeurons.length; i++){

            //Hidden neurons
            for(int j = 0; j < networkNeurons[i].length; j++){
                currentWeights = networkNeurons[i][j].getWeights();
                currentBias = networkNeurons[i][j].getBias();

                //Hidden neuron weights
                for(int k = 0; k < currentWeights.length - 2; k++){

                    //Blue if positive, red if negative
                    if(currentWeights[k] > 0){
                        newColor = new Color(0, 0, colorVibrancy, Math.abs(currentWeights[k]) / maxVal);
                    } else{
                        newColor = new Color(colorVibrancy, 0, 0, Math.abs(currentWeights[k]) / maxVal);
                    }

                    connections[i][k][j].setStroke(newColor);
                }

                //Blue if positive, red if negative
                if(currentBias > 0){
                    newColor = new Color(0, 0, colorVibrancy, Math.abs(currentBias) / maxVal);
                } else{
                    newColor = new Color(colorVibrancy, 0, 0, Math.abs(currentBias) / maxVal);
                }

                neurons[i + 1][j].setStroke(newColor);
            }

        }

        //Output neurons
        for(int i = 0; i < networkOutputs.length; i++){
            currentWeights = networkOutputs[i].getWeights();
            currentBias = networkOutputs[i].getBias();

            //Output neuron weights
            for(int j = 0; j < currentWeights.length - 2; j++){

                //Blue if positive, red if negative
                if(currentWeights[j] > 0){
                    newColor = new Color(0, 0, colorVibrancy, Math.abs(currentWeights[j]) / maxVal);
                } else{
                    newColor = new Color(colorVibrancy, 0, 0, Math.abs(currentWeights[j]) / maxVal);
                }

                connections[layers - 2][j][i].setStroke(newColor);
            }

            //Blue if positive, red if negative
            if(currentBias > 0){
                newColor = new Color(0, 0, colorVibrancy, Math.abs(currentBias) / maxVal);
            } else{
                newColor = new Color(colorVibrancy, 0, 0, Math.abs(currentBias) / maxVal);
            }

            neurons[layers - 1][i].setStroke(newColor);
        }

    }

    //Updates active neurons in network using network outputs
    public void drawRealtime(double[][] networkOutputs){
        double maxVal = 0;  //Tracks current maximum output from each layer
        Color newColor;     //Tracks current color being assigned to neuron

        //Iterates over network layers
        for(int i = 0; i < networkOutputs.length; i++){
            //Calculates maximum output from each layer
            maxVal = 0;

            for(int j = 0; j < networkOutputs[i].length; j++){

                if(networkOutputs[i][j] > maxVal){
                    maxVal = networkOutputs[i][j];
                }

            }

            //Iterates over neurons in each layer
            for(int j = 0; j < networkOutputs[i].length; j++){
                //White if active, black if inactive
                newColor = new Color(networkOutputs[i][j] / maxVal, networkOutputs[i][j] / maxVal, networkOutputs[i][j] / maxVal, 1);
                neurons[i][j].setFill(newColor);
            }

        }

    }

    //Sends output to evolutionHistory label, which acts as application output
    public void print(String s){

        //Runs on main thread
        Platform.runLater(() -> {
            evolutionHistory.setText(evolutionHistory.getText() + "\n" + s);
        });

    }

    public void drawHistory(VBox v){
        evolutionHistory = new Label();
        evolutionHistory.setWrapText(true);
        evolutionHistory.heightProperty().addListener(observable -> historyContainer.setVvalue(1D));
        evolutionHistory.setFont(new Font("Monaco", 8));
        evolutionHistory.setTextFill(Color.WHITE);
        evolutionHistory.setPrefWidth(200);
        evolutionHistory.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        evolutionHistory.setPadding(new Insets(5));
        evolutionHistory.setMinHeight(historyHeight);

        historyContainer = new ScrollPane();
        historyContainer.setContent(evolutionHistory);
        historyContainer.setPrefWidth(200);
        historyContainer.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        historyContainer.setHbarPolicy(ScrollBarPolicy.NEVER);
        historyContainer.setFitToWidth(true);
        historyContainer.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        historyContainer.setPrefHeight(historyHeight);

        infoView = new VBox();
        generation = new Label();
        generation.setFont(new Font("Monaco", 20));
        generation.setTextFill(Color.WHITE);
        score = new Label();
        score.setFont(new Font("Monaco", 18));
        score.setTextFill(Color.WHITE);
        fitnessScore = new Label();
        fitnessScore.setFont(new Font("Monaco", 10));
        fitnessScore.setTextFill(Color.WHITE);
        speed = new Label();
        speed.setFont(new Font("Monaco", 10));
        speed.setTextFill(Color.WHITE);
		infoView.getChildren().addAll(generation, score, fitnessScore, speed);
		infoView.setAlignment(Pos.CENTER_LEFT);
        infoView.setSpacing(10);

        v.getChildren().addAll(historyContainer, infoView);
        v.setSpacing(50);
        v.setPadding(new Insets(0, 100, 0, 0));
    }

    //Sets up application window graphics and all that jazz
    public void formGrid(Stage mainStage){
		gameGrid = new Rectangle[length][length];
        BorderPane rootNode = new BorderPane();
        neuralNet = new Pane();
        drawNetwork();
        neuralNet.setPadding(new Insets(20));
		gameView = new GridPane();
		
		for(int i = 0; i < length; i++){
			
			for(int j = 0; j < length; j++){
				gameGrid[i][j] = new Rectangle(10, 10);
				gameView.add(gameGrid[i][j], j, i);
				gameGrid[i][j].setFill(Color.BLACK);
			}
			
		}
		
		for(int i = 0; i < length; i++){
			gameGrid[length - 1][i].setFill(Color.WHITE);
			gameGrid[0][i].setFill(Color.WHITE);
			gameGrid[i][length - 1].setFill(Color.WHITE);
			gameGrid[i][0].setFill(Color.WHITE);
		}
        
        xAxis = new NumberAxis();
        xAxis.setLabel("Generation");
        xAxis.setTickUnit(0);
        yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        fitnessGraph = new LineChart<Number, Number>(xAxis, yAxis);
        //fitnessGraph.setTitle("Fitness over time");
        maxFitnessHistory = new XYChart.Series<Number, Number>();
        maxFitnessHistory.setName("Max");
        fitnessGraph.getData().add(maxFitnessHistory);
        fitnessGraph.setPrefWidth(length * 10);
        fitnessGraph.setMaxWidth(length * 10);

        VBox quickFix2 = new VBox();
        drawHistory(quickFix2);
        VBox quickFix = new VBox(gameView, fitnessGraph);
		rootNode.setCenter(quickFix);
        rootNode.setRight(neuralNet);
        rootNode.setLeft(quickFix2);
        rootNode.setBackground(new Background(new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
        Scene main = new Scene(rootNode, 400, 400);
        setKeyBindings(main);
		mainStage.setScene(main);
		mainStage.setMaximized(true);
        mainStage.show();
    }
    
    //Clears graph data
    public void clearGraph(){
        maxFitnessHistory.getData().clear();
    }

    //Sets up key bindings to control application
    public void setKeyBindings(Scene main){
        main.setOnKeyPressed((event) -> {
			switch(event.getCode()){
                case SPACE: //Kills current graphic snake

                    if(gSnake != null){
                        gSnake.killSnake();
                    }
                    
                    break;
                case W:     //Speeds up game
                    rateModifier *= 2;

                    if(rateModifier > 2048){
                        rateModifier = 2048;
                    }

                    setSpeed(rateModifier);
                    break;
                case S:     //Slows down game
                    rateModifier /= 2;

                    if(rateModifier < 0.015625){
                        rateModifier = 0.015625;
                    }

                    setSpeed(rateModifier);
                    break;
                case R:     //Resets evolution by cleaning up a bunch of processes
                    resetting = true;
                    updateGame.stop();
                    queueThread.interrupt();
                    gSnake.killSnake();
                    current.killSnake();
                    currentlyPlaying = false;
                    gRefreshScene();
                    refreshScene();
                    fittestMembers.clear();

                    Platform.runLater(() -> {
                        resetVariables();
                        clearGraph();
                        currentGeneration = new Generation(genSize, NN.getTotalSize());
                        resetting = false;
                        startGame(currentGeneration);
                    });

                    break;
				default:
					break;
			}
			
			event.consume();
		});
    }

    //Sets up background evolution and other required objects
	public void createScene(Stage mainStage){
		gameNumGrid = new int[length][length];
		
		for(int i = 0; i < length; i++){
			gameNumGrid[length - 1][i] = 1;
			gameNumGrid[0][i] = 1;
			gameNumGrid[i][length - 1] = 1;
			gameNumGrid[i][0] = 1;
		}
        
        formGrid(mainStage);

        gNN = new NeuralNetwork(layers - 2, (layers - 2) * neuronsPerLayer, inputs, outputs);
        NN = new NeuralNetwork(layers - 2, (layers - 2) * neuronsPerLayer, inputs, outputs);

        //Uncomment to allow choosing generation size
		//System.out.println("Please enter generation size");
		//genSize = in.nextInt();

		currentGeneration = new Generation(genSize, NN.getTotalSize());

        //Uncomment to allow loading from file
		/*try{
			printContents();
			System.out.println("Enter name of requested file (null if no file wanted)");
			in.nextLine();
			load(in.nextLine());
		} catch(Exception e){
			//e.printStackTrace();
		}*/

		startGame(currentGeneration);
	}

    //Creates and starts background evolution thread and queue thread
	public void startGame(Generation currentGen){
        currentGeneration = currentGen;
        evolutionThread = new Thread(() -> {

            //Loops until evolution is reset
            do{

                if(currentGeneration != null){
                    startGame(currentGeneration.getChildren()[childIndex]);
                }

            } while(!resetting);

        });

        evolutionThread.start();
        queueThread = new Thread(queueRunnable);
        queueThread.start();
    }
    
    //Graphs results on main thread
    public void graph(int genCount, Generation gen){

        Platform.runLater(() -> {
            maxFitnessHistory.getData().add(new XYChart.Data<Number, Number>(genCount, gen.getFittest().getFitness()));
        });

    }

    //Handles background evolution
	public void startGame(Child currentCh){
        currentChild = currentCh;
        
        if(currentCh != null){
            NN.setWeights(currentCh.getWeights());
            current = new Snake(gameNumGrid, length, seeds[currentTrial], penalties);

            while(current.isAlive()){
                surroundings = current.getSurrounding();
                result = NN.getOutputs(surroundings);
                max = result[0];
                move = 0;

                //Calculates maximum of network outputs
                for(int i = 1; i < result.length; i++){

                    if(result.length > 1){

                        if(result[i] > max){
                            max = result[i];
                            move = i;
                        }

                    }

                }

                //Updates snake's movement accordingly
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

                penalties[0] += calcPenalty(current);
                current.step();
            }

            //Post-game stuff
            refreshScene();

            //Calculates fitness
            if(current.getLength() - 3 < 8){
                fitness = (long) Math.pow(2, current.getLength() - 3) * (current.getTotalSteps() - (long) penalties[0]);
                //fitness = (long) Math.pow(2, current.getLength() - 3);
            } else{
                fitness = (long) Math.pow(2, 9) * (current.getLength() - 10) * (current.getTotalSteps() - (long) penalties[0]);
                //fitness = (long) Math.pow(2, 9) * (current.getLength() - 10);
            }
            
            //Compares current game to others
            if(current.getLength() > longestLength){
                longestLength = current.getLength();
            }

            if(fitness > highestTrialFitness){
                highestTrialFitness = fitness;
                currentCh.highestTrialSeed = seeds[currentTrial];
            }

            penalties[0] = 0;
            currentTrial++;
            
            //Runs after child is completely done being evaluated
            if(currentTrial >= trials){
                currentChild.setFitness(currentFitness / trials);
                currentTrial = 0;
                currentFitness = 0;
                highestTrialFitness = 0;
                childIndex++;

                //Runs after entire generation is done being evaluated
                if(childIndex >= genSize){
                    currentGeneration.calcTotalFitness();
                    //currentGeneration.calcHighestDistance(); //Comment out to get rid of pseudo-exploration
                    print("\nGen " + genCount + "\nBest: " + currentGeneration.getFittest().getFitness() + "\nLongest: " + longestLength);
                    graph(genCount, currentGeneration);
                    
                    //Determines whether current generation is "worth saving"
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

                    //Add fittest member of current generation to queue
                    try{
                        fittestMembers.put(currentGeneration.getFittest());
                    } catch(Exception e){
                        
                    }

                    longestLength = 0;
                    genCount++;
                    currentGeneration = new Generation(genSize, currentGeneration);
                    save("new");
                    childIndex = 0;
                    generateSeeds();
                }
                
            //Runs if current child is still being evaluated
            } else{
                currentFitness += fitness;
                startGame(currentChild);
            }
        
        }

	}

    //Generates seeds for generation trials
    public void generateSeeds(){
        Random rand = new Random();

        for(int i = 0; i < trials; i++){
            seeds[i] = rand.nextLong();
        }

    }

    //Updates speed label and sets rate of timeline
    public void setSpeed(double rate){
        speed.setText("Speed: " + rate + "x");
        updateGame.setRate(rateModifier);
    }

    //Handles showing the snake in application window
    public void showSnake(Child currentCh){

        if(currentCh != null && !currentlyPlaying){
            currentlyPlaying = true;
            gNN.setWeights(currentCh.getWeights());
            colorNetwork(currentCh);
            gSnake = new Snake(gameGrid, length, currentCh.highestTrialSeed, penalties);

            updateGame = new Timeline(new KeyFrame(Duration.millis(duration), new EventHandler<ActionEvent>(){
                @Override
                public void handle(ActionEvent e){

                    //Runs while snake is still alive
                    if(gSnake.isAlive()){
                        gSurroundings = gSnake.getSurrounding();
                        gResult = gNN.getOutputs(gSurroundings, gLayerOutputs);
                        gMax = gResult[0];
                        gMove = 0;

                        //Calculates maximum output of neural network
                        for(int i = 1; i < gResult.length; i++){

                            if(gResult.length > 1){
                                
                                if(gResult[i] > gMax){
                                    gMax = gResult[i];
                                    gMove = i;
                                }

                            }

                        }

                        //Updates snake's movements accordingly
                        switch(gMove){
                            case 0: //RIGHT
                                gSnake.updatePos(0, 1);
                                break;
                            case 1: //LEFT
                                gSnake.updatePos(0, -1);
                                break;
                            case 2: //DOWN
                                gSnake.updatePos(1, 0);
                                break;
                            case 3: //UP
                                gSnake.updatePos(-1, 0);
                                break;
                        }

                        penalties[1] += calcPenalty(gSnake);
                        drawRealtime(gLayerOutputs);
                        gSnake.step();

                        //Fitness calculation
                        if(gSnake.getLength() - 3 < 8){
                            gFitness = (long) Math.pow(2, gSnake.getLength() - 3) * (gSnake.getTotalSteps() - (long) penalties[1]);
                        } else{
                            gFitness = (long) Math.pow(2, 10) * (gSnake.getLength() - 10) * (gSnake.getTotalSteps() - (long) penalties[1]);
                        }
                        
                        //Label updates
                        if (shownGencount < 0) shownGencount = 0;
                        generation.setText("Gen: " + shownGencount + " / " + (genCount - 1));
                        score.setText("Length: " + gSnake.getLength());
                        fitnessScore.setText("Fitness: " + gFitness);
                    } else{
                        currentlyPlaying = false;
                        shownGencount++;
                        gRefreshScene();
                        penalties[1] = 0;
                        queueThread = new Thread(queueRunnable);
                        queueThread.start();
                        updateGame.stop();
                    }
                    
                    e.consume();
                }
            }));
            
            setSpeed(rateModifier);
            updateGame.setCycleCount(Timeline.INDEFINITE);
            updateGame.play();
        //Runs if something bad happens, like this method is entered while a game is in progress or the child does not exist
        } else {
            System.out.println("Something went wrong!");
            print("Something went wrong!");
            currentlyPlaying = false;
        }

    }
    
    //Refreshes grid for background evolution
	public void refreshScene(){
		gameNumGrid = new int[length][length];
		
		for(int i = 0; i < length; i++){
			gameNumGrid[length - 1][i] = 1;
			gameNumGrid[0][i] = 1;
			gameNumGrid[i][length - 1] = 1;
			gameNumGrid[i][0] = 1;
		}
		
    }
    
    //Refreshes grid for graphic game
    public void gRefreshScene(){
		
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