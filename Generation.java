import java.util.*;
import java.io.*;

public class Generation implements Serializable{
	//Modifiables
	private double mutationChance = 0.4;		//Chance of mutation per genome
	private double mutationMagnitude = 0.4;		//Magnitude of mutation
	private double weightDecay = 0.99;			//Decay of weights per evolution
	private int selectionType = 0;				//Type of selection (0 = roulette, 1 = tournament)
	private int crossoverType = 0;				//Type of crossover (0 = random uniform, 1 = ordered one-point crossover)
	private double crossoverProbability = 1;	//Chance of crossover
	private double weightLimit = 1;				//Initial weight magnitude
	private float eliteProportion = 1 / 20;		//Proportion of generation to transfer over
	private double maxDistance = 0;				//Maximum distance between parents

	//Misc objects
	private int elite;						//Number of generation to transfer over
	private Child[] children;				//Children in current generation
	private long totalFitness = 0;			//Total fitness of children in generation
	private long highestFitness = 0;		//Highest fitness value over every trial
	static final long serialVersionUID = 0;	//Thing
	
	//Initializer for initial generation
	public Generation(int size, int weightSize){
		this.elite = Math.round((float) size * eliteProportion);

		//Generates new children
		children = new Child[size];
		
		for(int i = 0; i < size; i++){
			children[i] = new Child(weightSize, weightLimit);
		}
		
	}

	//Generates new generation from current generation
	public Generation genNext(){
		return new Generation(children.length, this);
	}
	
	//Initializer for child generation of parent generation
	public Generation(int size, Generation parents){
		parents.sort();
		parents.scale();
		this.elite = parents.getElite();
		children = new Child[size];
		Child p1;
		Child p2;

		//Generates new children
		for(int i = elite; i < size; i++){
			p1 = parents.getFitChild();
			p2 = parents.getFitChild(p1);
			
			while(p1 == p2 || childDistance(p1, p2) < 0.5 * parents.maxDistance){
				//System.out.println("Max: " + parents.maxDistance);
				//System.out.println("Calc: " + childDistance(p1, p2));
				p2 = parents.getFitChild(p1);
			}
			
			children[i] = generateChild(p1, p2);
		}

		//Pulls elites from previous generation
		for(int i = 0; i < elite; i++){
			children[i] = parents.getChildren()[i];
		}

	}
	
	//Sorts array by value using bubble sort
	public void sort(int[] array){
		int placeHolder;
		
		for(int i = 0; i < array.length - 1; i++){
			
			for(int j = 0; j < array.length - i - 1; j++){
				
				if(array[j] > array[j + 1]){
					placeHolder = array[j];
					array[j] = array[j + 1];
					array[j + 1] = placeHolder;
				}
				
			}
			
		}
		
	}

	//Checks if array contains a value
	public Boolean arrayContains(int[] array, int val){
		
		for(int i = 0; i < array.length; i++){
			
			if(array[i] == val){
				return true;
			}
			
		}
		
		return false;
	}

	//Sorts generation by fitness using bubble sort
	public void sort(){
		Child placeHolder;
		
		for(int i = 0; i < children.length - 1; i++){
			
			for(int j = 0; j < children.length - i - 1; j++){
				
				if(children[j].getFitness() < children[j + 1].getFitness()){
					placeHolder = children[j];
					children[j] = children[j + 1];
					children[j + 1] = placeHolder;
				}
				
			}
			
		}
		
	}

	//Scales children fitness so that every single one is positive
	public void scale(){
		long scaleFactor = children[children.length - 1].getFitness();

		for(int i = 0; i < children.length; i++){
			children[i].setFitness(children[i].getFitness() - scaleFactor + 1);
		}

	}

	//Calculates numerical difference between two children
	public double childDistance(Child p1, Child p2){
		double total = 0;

		for(int i = 0; i < p1.getWeights().length; i++){
			total += Math.abs(p1.getWeights()[i] - p2.getWeights()[i]);
		}

		total = total / (double) (p1.getWeights().length + weightLimit);
		return total;
	}

	//Calculates highest numerical difference in generation
	public void calcHighestDistance(){
		maxDistance = 0;
		double highest = 0;
		double calcDistance = 0;

		for(int i = 0; i < children.length - 1; i++){

			for(int j = children.length - i - 1; j > i; j--){
				calcDistance = childDistance(children[i], children[j]);

				if(calcDistance > highest){
					highest = calcDistance;
				}

			}

		}

		maxDistance = highest;
	}
		
	//Creates child from two parents
	public Child generateChild(Child p1, Child p2){
		double[] weights = cross(p1, p2);
		weights = mutate(weights);
		return new Child(weights);
	}
	
	//Crossover operator
	public double[] cross(Child p1, Child p2){
		Random randomizer = new Random();
		int length = p1.getWeights().length;
		double[] weights = new double[length];
		double pCross = randomizer.nextDouble();

		if(pCross <= crossoverProbability){

			switch(this.crossoverType){
				case 0:		//Kind-of random crossover
					int crossExtent = randomizer.nextInt(length);
					int[] indices = new int[crossExtent];
					int chosenIndex = 0;

					for(int i = 0; i < crossExtent; i++){

						do{
							chosenIndex = randomizer.nextInt(length);
						} while(arrayContains(indices, chosenIndex));

						indices[i] = chosenIndex;
					}

					for(int i = 0; i < length; i++){

						if(arrayContains(indices, chosenIndex)){
							weights[i] = p1.getWeights()[i];
						} else{
							weights[i] = p2.getWeights()[i];
						}

					}

					return weights;
				default:	//Ordered crossover
					int chosen = randomizer.nextInt(2);
					
					switch(chosen){
						case 0:
							
							for(int i = 0; i < length / 2; i++){
								weights[i] = p1.getWeights()[i];
							}

							for(int i = length / 2; i < length; i++){
								weights[i] = p2.getWeights()[i];
							}

						case 1:

							for(int i = 0; i < length / 2; i++){
								weights[i] = p2.getWeights()[i];
							}

							for(int i = length / 2; i < length; i++){
								weights[i] = p1.getWeights()[i];
							}

					}
					
					return weights;
			}

		}
		
		return p1.getWeights();
	}
	
	//Mutation operator
	public double[] mutate(double[] weights){
		Random randomizer = new Random();
		double chosen;
		double newNum;
		
		for(int i = 0; i < weights.length; i++){
			chosen = randomizer.nextDouble();
			newNum = randomizer.nextGaussian() * mutationMagnitude;
			
			if(chosen <= mutationChance){
				weights[i] += newNum;
			}
			
			weights[i] *= weightDecay;
		}
		
		return weights;
	}

	//Selection operator
	public Child getFitChild(){
		Random randomizer = new Random();
		
		switch(this.selectionType){
			case 0:		//Roulette wheel
				long chosen = new Double(Math.floor(randomizer.nextDouble() * (double) totalFitness)).longValue();
				long totalProb = 0;

				for(int i = 0; i < children.length; i++){
					
					if(chosen <= children[i].getFitness() + totalProb){
						return children[i];
					} else{
						totalProb += children[i].getFitness();
					}
					
				}
				
				return children[0];
			default:	//Poor tournament implementation (should create clusters and compare with one another)
				int selected = randomizer.nextInt(2);
				
				switch(selected){
					case 0:
						return children[0];
					case 1:
						return children[1];
				}
				
				return children[0];
		}
		
	}

	//Selection operator with restriction
	public Child getFitChild(Child chosenParent){
		Random randomizer = new Random();
		
		switch(this.selectionType){
			case 0:		//Roulette wheel
				long chosen = new Double(Math.floor(randomizer.nextDouble() * ((double) totalFitness - chosenParent.getFitness()))).longValue();
				long totalProb = 0;

				for(int i = 0; i < children.length; i++){
					
					if(children[i] != chosenParent){

						if(chosen <= children[i].getFitness() + totalProb){
							return children[i];
						} else{
							totalProb += children[i].getFitness();
						}
					
					}

				}
				
				return children[0];
			default:	//Poor tournament implementation (should create clusters and compare with one another)
				int selected = randomizer.nextInt(2);
				
				switch(selected){
					case 0:
						return children[0];
					case 1:
						return children[1];
				}
				
				return children[0];
		}
		
	}

	//Self-explanatory methods
	public void calcTotalFitness(){
		totalFitness = 0;

		for(Child c : children){
			totalFitness += c.getFitness();
		}

	}
	
	public void setTotalFitness(long totalFitness){
		this.totalFitness = totalFitness;
	}
	
	public Child getFittest(){
		Child fittest = children[0];

		for(int i = 1; i < children.length; i++){

			if(children[i].getFitness() > fittest.getFitness()){
				fittest = children[i];
			}

		}

		return fittest;
	}
	
	public Child[] getChildren(){
		return children;
	}
	
	public long getTotalFitness(){
		return totalFitness;
	}
	
	public double getMutationChance(){
		return mutationChance;
	}
	
	public int getSelectionType(){
		return selectionType;
	}
	
	public int getCrossoverType(){
		return crossoverType;
	}

	public long getAverageFitness(){
		return totalFitness / (long) children.length;
	}

	public double getWeightLimit(){
		return weightLimit;
	}

	public double getCrossoverProbability(){
		return crossoverProbability;
	}

	public int getElite(){
		return elite;
	}

	public double getMutationMagnitude(){
		return mutationMagnitude;
	}

	public double getWeightDecay(){
		return weightDecay;
	}

	public void setHighestFitness(long val){
		highestFitness = val;
	}

	public long getHighestFitness(){
		return highestFitness;
	}
}