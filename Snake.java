import java.util.*;
import javafx.application.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;

//Color encoding when using matrix instead of JavaFX rectangles
/*
	0 = BLACK
	1 = WHITE
	2 = GRAY
	3 = GREEN
	4 = RED
*/

public class Snake{
	//Modifiables
	int steps = 0;					//Starting number of steps on a snake
	int maxSteps = 1200;			//Maximum number of steps a snake should take between food
	int stepRefresh = 1200;			//How many steps to "forgive" after a snake eats
	int cheese = 1;					//Minimum distance between border and food
	int foodBonus = 1;				//The length bonus provided by food
	int length = 3;					//The starting length
	double penaltyForgiveness = 1;	//Factor to scale penalty after eating food

	//Trackers
	Boolean alive = true;		//Tracks if snake is still alive
	Boolean started = false;	//Tracks if game has started
	Boolean isFood = false;		//Tracks if food is on the grid
	int posX;					//Tracks current X position on grid
	int posY;					//Tracks current Y position on grid
	int totalSteps = 0;			//Tracks total number of steps taken
	int gridSize;				//Tracks size of grid

	//Graphic game objects
	Rectangle rect;
	Rectangle[][] grid;

	//Background evolution objects
	int[][] numGrid;		//Grid for background evolution
	int numRect;			//Accompanying value of background evolution tile
	Boolean num = false;	//Tracks whether snake is used in graphic game or background evolution

	//Snake movement
	int changeX = 0;	//Current movement of snake along X-axis
	int changeY = 0;	//Current movement of snake along Y-axis

	//Food position
	int randX = 0;
	int randY = 0;

	//Misc objects
	TileGroup snakeTileGroup = new TileGroup();	//Associated snake tiles
	double[] penalties;							//References penalties array in main program
	Random intgen = new Random();				//Random generator for stuff
	
	//Initializer for snake using rectangles in JavaFX
	public Snake(Rectangle[][] grid, int gridSize, long seed, double[] penalties){
		intgen.setSeed(seed);
		this.grid = grid;
		this.gridSize = gridSize;
		this.penalties = penalties;
		rect = grid[gridSize / 2][gridSize / 2];
		posX = gridSize / 2;
		posY = gridSize / 2;
		
		if(rect != null){
			rect.setFill(Color.GRAY);
		}
		
		num = false;
	}

	//Initializer for snake using a matrix
	public Snake(int[][] numGrid, int gridSize, long seed, double[] penalties){
		intgen.setSeed(seed);
		this.numGrid = numGrid;
		this.gridSize = gridSize;
		this.penalties = penalties;
		numRect = numGrid[gridSize / 2][gridSize / 2];
		posX = gridSize / 2;
		posY = gridSize / 2;
		numGrid[gridSize / 2][gridSize / 2] = 2;
		num = true;
	}
	
	//Used to indicate to the snake what direction to travel
	public void updatePos(int x, int y){

		//Ensures the direction does not completely reverse, which would cause the snake to collide directly with itself
		if(x != -1 * changeX){
			changeX = x;
		}

		if(y != -1 * changeY){
			changeY = y;
		}
			
	}
	
	//Used when the snake head is located on a food tile
	public void eatFood(){
		//Indicates the snake has eaten the food and increments the length
		isFood = false;
		length += foodBonus;
		
		//Slightly increases the timeout of every snake tile to make it visually work
		snakeTileGroup.instances.forEach((e) -> {
			e.timeout += foodBonus;
		});
		
		if(!num){
			penalties[1] = penalties[1] * penaltyForgiveness;
		} else{
			penalties[0] = penalties[0] * penaltyForgiveness;
		}

	}
	
	//Used to generate food at specific positions on the grid
	public void genFood(){
		
		//Only runs if there is not already food on the grid
		if(!isFood){
			//The cheese value basically makes it so the food never spawns within a certain distance of the border
			randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
			randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
			
			if(!num){

				//Continues generating new numbers until it does not spawn on top of the snake and is not directly in front of the snake
				while(grid[randX][randY].getFill() == Color.GRAY || project(randX, changeX, posX) == true || project(randY, changeY, posY) == true){
					randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
					randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
				}
				
				//Finally, places the food
				grid[randX][randY].setFill(Color.GREEN);
			} else{

				//Same premise as above
				while(numGrid[randX][randY] == 2 || project(randX, changeX, posX) == true || project(randY, changeY, posY) == true){
					randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
					randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
				}
				
				numGrid[randX][randY] = 3;
			}

			//Indicates that there is food now on the grid
			isFood = true;
		}
		
	}

	//Essentially projects to check if the combination of the val and change will collide with the goal
	//In practice, used to ensure the food never spawns in front of the snake, as then it will hopefully require the snake to actively seek it
	public Boolean project(int val, int change, int goal){

		//Comment in to enable
		for(int i = 0; i < gridSize; i++){
			val += change;

			if(val == goal){
				return true;
			}

		}

		return false;
	}
	
	//Advances the game by one step
	public void step(){

		//Kind of deprecated, used to be used to determine whether a snake has gone in a circle
		if(!num){
			SnakeTile old = new SnakeTile(length, rect, this, snakeTileGroup);
		} else{
			SnakeTile old = new SnakeTile(length, numGrid, posX, posY, this, snakeTileGroup);
		}

		//Ensures there is food on the field
		genFood();
		
		//Essentially, if the snake has stopped moving, it's probably dead
		if(changeX == 0 && changeY == 0){
			alive = false;
		}

		//Updates the position according to the position
		posX += changeX;
		posY += changeY;
		
		//Only runs if the snake is inside the bounds of the grid
		if(posX >= 0 && posX < gridSize && posY >= 0 && posY < gridSize){

			//Gets the value of the current tile the snake is on
			if(!num){
				rect = grid[posX][posY];
			} else{
				numRect = numGrid[posX][posY];
			}
			
			//For every snake tile, AKA each body part of the snake, it decreases the timer by one so the body automatically disappears
			snakeTileGroup.instances.forEach((e) -> {
				
				if(e.parent == this){
					e.countdown();
				}
				
			});

			//Cleans up the snake tiles that must be deleted
			snakeTileGroup.toDelete.forEach((e) -> {

				if(snakeTileGroup.instances.contains(e)){
					snakeTileGroup.instances.remove(e);
				}

			});
			
			//Finally clears the array of snake tiles to be deleted
			snakeTileGroup.toDelete.clear();

			if(!num){

				//Runs if it is still alive, on a valid tile, and has not exceeded its limit
				if(alive && rect.getFill() != Color.WHITE && rect.getFill() != Color.GRAY && steps < maxSteps){
					//Just to keep track of the total steps a snake has taken
					totalSteps++;

					//If it is on a green tile, grow the snake and decrement the amount of steps they have taken, otherwise increase it
					if(rect.getFill() == Color.GREEN){
						eatFood();
						steps -= stepRefresh;
						if(steps < 0) steps = 0;
					} else{
						steps++;
					}
					
					//Makes it so, visually, the snake moves to the new position on the board
					rect.setFill(Color.GRAY);
				} else{
					//Runs if snake dies, just indicates its death and then cleans up the snake
					rect.setFill(Color.RED);
					alive = false;
					snakeTileGroup.instances.clear();
					snakeTileGroup.toDelete.clear();
				}

			} else{

				//Follows same premise as above, but with integers instead of colors
				if(alive && numRect != 1 && numRect != 2 && steps < maxSteps){
					totalSteps++;

					if(numRect == 3){
						eatFood();
						steps = 0;
					} else{
						steps++;
					}
					
					numGrid[posX][posY] = 2;
				} else{
					numGrid[posX][posY] = 4;
					alive = false;
					snakeTileGroup.instances.clear();
					snakeTileGroup.toDelete.clear();
				}

			}
				
		} else{
			//If it's outside of the bounds, undo previous changes and kill the snake
			posX -= changeX;
			posY -= changeY;
			alive = false;

			//Visual/numerical indicator the snake is dead
			if(!num){
				rect.setFill(Color.RED);
			} else{
				numGrid[posX][posY] = 4;
			}

			//Cleanup of snake
			snakeTileGroup.instances.clear();
			snakeTileGroup.toDelete.clear();
		}
		
	}
	
	//Returns a 24 double array as inputs for the neural network
	public double[] getSurrounding(){
		//Describes snake's surroundings, 3 slots dedicated to each direction to indicate distance from something, first slot is distance, other two are indicators
		double[] surroundings = new double[24];
		int checkX = 0;
		int checkY = 0;
		int stateX = 0;
		int stateY = 0;
		Paint currentColor;
		int currentVal;

		if(!num){

			//Repeats in all 8 directions (diagonals and cardinal)
			for(int i = 0; i < 8; i++){

				switch(i){
					case 0:
						stateX = 1;
						break;
					case 1:
						stateX = -1;
						break;
					case 2:
						stateX = 0;
						stateY = 1;
						break;
					case 3:
						stateY = -1;
						break;
					case 4:
						stateX = 1;
						stateY = 1;
						break;
					case 5:
						stateX = -1;
						break;
					case 6:
						stateY = -1;
						break;
					case 7:
						stateX = 1;
						break;
				}

				//Continues scanning along the snake's view until it detects something that is not the background color
				do{
					checkX++;
					checkY++;
					currentColor = grid[posX + stateX * checkX][posY + stateY * checkY].getFill();
				} while(currentColor == Color.BLACK);

				//Essentially what happens when snake detects a wall, further away means less important (lower value)
				if(stateX != 0){
					surroundings[i * 3] = 1 / (double) checkX;
				} else{
					surroundings[i * 3] = 1 / (double) checkY;
				}

				//If green, then it indicates so in the array, otherwise, indicates it is itself
				//This allows encoding of distance, and then what it is
				if(currentColor == Color.GREEN){
					surroundings[i * 3 + 2] = 1;
				} else if(currentColor == Color.GRAY){
					surroundings[i * 3 + 1] = 1;
				}

				checkX = 0;
				checkY = 0;
			}

		} else{

			//Follows same premise as above, but with color encoded as integers
			for(int i = 0; i < 8; i++){

				switch(i){
					case 0:
						stateX = 1;
						break;
					case 1:
						stateX = -1;
						break;
					case 2:
						stateX = 0;
						stateY = 1;
						break;
					case 3:
						stateY = -1;
						break;
					case 4:
						stateX = 1;
						stateY = 1;
						break;
					case 5:
						stateX = -1;
						break;
					case 6:
						stateY = -1;
						break;
					case 7:
						stateX = 1;
						break;
				}
	
				do{
					checkX++;
					checkY++;
					currentVal = numGrid[posX + stateX * checkX][posY + stateY * checkY];
				} while(currentVal == 0);
	
				if(stateX != 0){
					surroundings[i * 3] = 1 / (double) checkX;
				} else{
					surroundings[i * 3] = 1 / (double) checkY;
				}
	
				if(currentVal == 3){
					surroundings[i * 3 + 2] = 1;
				} else if(currentVal == 2){
					surroundings[i * 3 + 1] = 1;
				}
	
				checkX = 0;
				checkY = 0;
			}

		}

		return surroundings;
	}

	//Self-explanatory methods
	public void killSnake(){
		alive = false;
	}
	
	public Boolean isAlive(){return alive;}
	
	public Boolean hasStarted(){return started;}

	public int getTotalSteps(){return totalSteps;}

	public int getLength(){return length;}
}

//Contains the snake tiles used in each snake's game
class TileGroup{
	ArrayList<SnakeTile> instances = new ArrayList<SnakeTile>();
	ArrayList<SnakeTile> toDelete = new ArrayList<SnakeTile>();
}

//Represents the trailing snake body that follows a snake
class SnakeTile{
	int timeout;	//Time before tile disappears
	Rectangle rect;	//Accompanying rectangle for graphic game
	Snake parent;	//Associated snake

	int[][] grid;			//Game grid for background evolution
	int posX;				//X value of tracked tile
	int posY;				//Y value of tracked tile
	Boolean num = false;	//Tracks whether representing background evolution or graphic game

	TileGroup group;	//Group it belongs to
	
	//Initializer for graphic game
	public SnakeTile(int timeout, Rectangle rect, Snake parent, TileGroup group){
		this.timeout = timeout;
		this.rect = rect;
		this.parent = parent;
		this.group = group;
		group.instances.add(this);
		countdown();
		num = false;
	}

	//Initializer for background evolution
	public SnakeTile(int timeout, int[][] grid, int posX, int posY, Snake parent, TileGroup group){
		this.timeout = timeout;
		this.grid = grid;
		this.posX = posX;
		this.posY = posY;
		this.parent = parent;
		this.group = group;
		group.instances.add(this);
		countdown();
		num = true;
	}
	
	//Handles tile disappearing and countdown, if it reaches 0 the tile is reset to default values
	public void countdown(){
		
		if(timeout <= 0){

			if(!num){
				rect.setFill(Color.BLACK);
			} else{
				grid[posX][posY] = 0;
			}
			
			group.toDelete.add(this);
		} else{
			timeout--;
		}
		
	}
}