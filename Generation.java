import java.util.*;
import java.io.*;

public class Generation implements Serializable{
	private double mutationChance;
	private Child[] children;
	private long totalFitness = 0;
	private int selectionType;
	private int crossoverType;
	private double crossoverProbability;
	private double weightLimit;
	private int elite;
	static final long serialVersionUID = 0;
	
	public Generation(int size, int weightSize, double weightLimit){
		this.selectionType = 0;
		this.crossoverType = 0;
		this.mutationChance = 0.1;
		this.crossoverProbability = 100;
		this.elite = size / 20;	//MUST BE < size / 2
		this.weightLimit = weightLimit;
		children = new Child[size];
		
		for(int i = 0; i < size; i++){
			children[i] = new Child(weightSize, weightLimit);
		}
		
	}

	public Generation genNext(){
		return new Generation(children.length, this);
	}
	
	public Generation(int size, Generation parents){
		parents.sort();
		this.mutationChance = parents.getMutationChance();
		this.selectionType = parents.getSelectionType();
		this.crossoverType = parents.getCrossoverType();
		this.weightLimit = parents.getWeightLimit();
		this.crossoverProbability = parents.getCrossoverProbability();
		this.elite = parents.getElite();
		children = new Child[size];
		Child p1;
		Child p2;

		for(int i = elite * 2; i < size; i++){
			p1 = parents.getFitChild();
			p2 = parents.getFitChild();
			
			while(p1 == p2){
				p2 = parents.getFitChild(p1);
			}
			
			children[i] = generateChild(p1, p2);
		}

		for(int i = 0; i < elite * 2; i++){
			
			if(i < elite){
				children[i] = parents.getChildren()[i];
			} else{
				children[i] = new Child(mutate(parents.getChildren()[i - elite / 2].getWeights()));
			}
			
		}

	}
	
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
	
	public Child generateChild(Child p1, Child p2){
		double[] weights = cross(p1, p2);
		weights = mutate(weights);
		return new Child(weights);
	}
	
	public double[] cross(Child p1, Child p2){
		Random randomizer = new Random();
		int length = p1.getWeights().length;
		double[] weights = new double[length];
		double pCross = randomizer.nextDouble();

		if(pCross <= crossoverProbability / 100){

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
	
	public Boolean arrayContains(int[] array, int val){
		
		for(int i = 0; i < array.length; i++){
			
			if(array[i] == val){
				return true;
			}
			
		}
		
		return false;
	}
	
	public double[] mutate(double[] weights){
		Random randomizer = new Random();
		double chosen;
		double newNum;
		
		for(int i = 0; i < weights.length; i++){
			chosen = randomizer.nextDouble();
			newNum = randomizer.nextDouble() * weightLimit * 2 - weightLimit;
			
			if(chosen <= mutationChance){
				weights[i] = newNum;
			}
			
		}
		
		return weights;
	}

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
	
	public Child getFitChild(){
		Random randomizer = new Random();
		
		switch(this.selectionType){
			case 0:		//Roulette wheel
				long chosen = Double.doubleToLongBits(Math.ceil(randomizer.nextDouble() * (double) totalFitness));
				long totalProb = 0;

				for(int i = 0; i < children.length; i++){
					
					if(chosen <= children[i].getFitness() + totalProb){
						return children[i];
					} else{
						totalProb += children[i].getFitness();
					}
					
				}
				
				return children[0];
			default:	//Tournament
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

	public Child getFitChild(Child chosenParent){
		Random randomizer = new Random();
		
		switch(this.selectionType){
			case 0:		//Roulette wheel
				long chosen = Double.doubleToLongBits(Math.ceil(randomizer.nextDouble() * (double) totalFitness));
				long totalProb = 0;

				for(int i = 0; i < children.length; i++){
					
					if(chosen <= children[i].getFitness() + totalProb){
						return children[i];
					} else{
						totalProb += children[i].getFitness();
					}
					
				}
				
				if(children[0] != chosenParent){
					return children[0];
				} else{
					return children[1];
				}

			default:	//Tournament
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
}