import java.util.*;
import java.io.*;

public class Child implements Serializable{
	private double[] weights;
	private long fitness;
	final static long serialVersionUID = 0;
	
	public Child(int size, double weightLimit){
		Random doubleGen = new Random();
		double num;
		weights = new double[size];
		
		for(int i = 0; i < size; i++){
			num = doubleGen.nextDouble() * weightLimit * 2 - weightLimit;
			weights[i] = num;
		}
		
	}
	
	public Child(double[] weights){
		this.weights = weights;
	}
	
	/*public int calcFitness(){
		fitness = 0;
	
		//REWRITE FITNESS FUNCTION
		
		return fitness;
	}*/
	
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