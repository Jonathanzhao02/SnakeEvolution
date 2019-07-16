import java.util.*;
import javafx.application.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;

/*
	0 = BLACK
	1 = WHITE
	2 = GRAY
	3 = GREEN
	4 = RED
*/

public class Snake{
	Boolean alive = true;
	int posX;
	int posY;
	Rectangle rect;
	Rectangle[][] grid;
	int changeX;
	int changeY;
	Boolean started = false;
	Boolean isFood = false;
	
	int steps = 0;			//Modifiables
	int maxSteps = 800;
	int stepRefresh = 800;
	int cheese = 1;
	int length = 3;
	
	int gridSize;
	int totalSteps = 0;
	Random intgen = new Random();

	int randX;
	int randY;

	int[][] numGrid;
	int numRect;
	Boolean num = false;
	
	public Snake(Rectangle[][] grid, int gridSize){
		this.grid = grid;
		this.gridSize = gridSize;
		rect = grid[gridSize / 2][gridSize / 2];
		posX = gridSize / 2;
		posY = gridSize / 2;
		
		if(rect != null){
			rect.setFill(Color.GRAY);
		}
		
		num = false;
	}

	public Snake(int[][] numGrid, int gridSize){
		this.numGrid = numGrid;
		this.gridSize = gridSize;
		numRect = numGrid[gridSize / 2][gridSize / 2];
		posX = gridSize / 2;
		posY = gridSize / 2;
		numGrid[gridSize / 2][gridSize / 2] = 2;
		num = true;
	}
	
	public void updatePos(int x, int y){
		changeX = x;
		changeY = y;
	}
	
	public void eatFood(){
		int foodBonus = 1;
		isFood = false;
		length += foodBonus;
		
		SnakeTile.instances.forEach((e) -> {
			e.timeout += foodBonus;
		});
		
	}
	
	public void genFood(){
		
		if(!isFood){
			randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
			randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
			
			if(!num){

				while(grid[randX][randY].getFill() == Color.GRAY || project(randX, changeX, posX) == true || project(randY, changeY, posY) == true){
					randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
					randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
				}
				
				grid[randX][randY].setFill(Color.GREEN);
			} else{

				while(numGrid[randX][randY] == 2 || project(randX, changeX, posX) == true || project(randY, changeY, posY) == true){
					randX = intgen.nextInt(gridSize - 2 * cheese) + cheese;
					randY = intgen.nextInt(gridSize - 2 * cheese) + cheese;
				}
				
				numGrid[randX][randY] = 3;
			}

			isFood = true;
		}
		
	}

	public Boolean project(int val, int change, int goal){

		for(int i = 0; i < gridSize; i++){
			val += change;

			if(val == goal){
				return true;
			}

		}

		return false;
	}
	
	public void step(){

		if(!num){
			SnakeTile old = new SnakeTile(length, rect, this);
		} else{
			SnakeTile old = new SnakeTile(length, numGrid, posX, posY, this);
		}

		genFood();
		
		if(changeX == 0 && changeY == 0){
			alive = false;
		}

		posX += changeX;
		posY += changeY;
		
		if(posX >= 0 && posX < gridSize && posY >= 0 && posY < gridSize){

			if(!num){
				rect = grid[posX][posY];
			} else{
				numRect = numGrid[posX][posY];
			}
			
			SnakeTile.instances.forEach((e) -> {
				
				if(e.parent == this){
					e.countdown();
				}
				
			});

			SnakeTile.toDelete.forEach((e) -> {

				if(SnakeTile.instances.contains(e)){
					SnakeTile.instances.remove(e);
				}

			});
			
			SnakeTile.toDelete.clear();

			if(!num){

				if(alive && rect.getFill() != Color.WHITE && rect.getFill() != Color.GRAY && steps < maxSteps){
					totalSteps++;

					if(rect.getFill() == Color.GREEN){
						eatFood();
						steps -= stepRefresh;
						if(steps < 0) steps = 0;
					} else{
						steps++;
					}
					
					rect.setFill(Color.GRAY);
				} else{
					rect.setFill(Color.RED);
					alive = false;
					SnakeTile.instances.clear();
					SnakeTile.toDelete.clear();
				}

			} else{

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
					SnakeTile.instances.clear();
					SnakeTile.toDelete.clear();
				}

			}
				
		} else{
			posX -= changeX;
			posY -= changeY;
			alive = false;

			if(!num){
				rect.setFill(Color.RED);
			} else{
				numGrid[posX][posY] = 4;
			}

			SnakeTile.instances.clear();
			SnakeTile.toDelete.clear();
		}
		
	}
	
	public double[] getSurrounding(){
		double[] surroundings = new double[24];
		int checkX = 0;
		int checkY = 0;
		int stateX = 0;
		int stateY = 0;
		Paint currentColor;
		int currentVal;

		if(!num){

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
					currentColor = grid[posX + stateX * checkX][posY + stateY * checkY].getFill();
				} while(currentColor == Color.BLACK);

				if(stateX != 0){
					surroundings[i * 3] = 1 / (double) checkX;
				} else{
					surroundings[i * 3] = 1 / (double) checkY;
				}

				if(currentColor == Color.GREEN){
					surroundings[i * 3 + 2] = 1;
				} else if(currentColor == Color.GRAY){
					surroundings[i * 3 + 1] = 1;
				}

				checkX = 0;
				checkY = 0;
			}

		} else{

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

	public void killSnake(){
		alive = false;
	}
	
	public Boolean isAlive(){return alive;}
	
	public Boolean hasStarted(){return started;}

	public int getTotalSteps(){return totalSteps;}

	public int getLength(){return length;}
}

class SnakeTile{
	static ArrayList<SnakeTile> instances = new ArrayList<SnakeTile>();
	static ArrayList<SnakeTile> toDelete = new ArrayList<SnakeTile>();
	int timeout;
	Rectangle rect;
	Snake parent;

	int[][] grid;
	int posX;
	int posY;
	Boolean num = false;
	
	public SnakeTile(int timeout, Rectangle rect, Snake parent){
		this.timeout = timeout;
		this.rect = rect;
		this.parent = parent;
		instances.add(this);
		countdown();
		num = false;
	}

	public SnakeTile(int timeout, int[][] grid, int posX, int posY, Snake parent){
		this.timeout = timeout;
		this.grid = grid;
		this.posX = posX;
		this.posY = posY;
		this.parent = parent;
		instances.add(this);
		countdown();
		num = true;
	}
	
	public void countdown(){
		
		if(timeout <= 0){

			if(!num){
				rect.setFill(Color.BLACK);
			} else{
				grid[posX][posY] = 0;
			}
			
			toDelete.add(this);
		} else{
			timeout--;
		}
		
	}
}