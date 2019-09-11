import java.util.*;
import java.io.*;

public class Child implements Serializable{
	private double[] weights;				//Associated weights of child
	private long fitness;					//Associated fitness
	public long highestTrialSeed;			//Seed of highest game
	final static long serialVersionUID = 0;	//Thing
	
	//Initializer that generates weights
	public Child(int size, double weightLimit){
		Random doubleGen = new Random();
		double num;
		weights = new double[size];
		
		for(int i = 0; i < size; i++){
			num = doubleGen.nextGaussian() * weightLimit;
			weights[i] = num;
		}
		
	}
	
	//Initializer that uses old weights
	public Child(double[] weights){
		this.weights = weights;
	}
	
	//Self-explanatory methods
	public void setFitness(long fitness){
		this.fitness = fitness;
	}
	
	public long getFitness(){
		return fitness;
	}
	
	public double[] getWeights(){
		return weights;
	}
}