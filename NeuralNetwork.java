import java.util.*;

public class NeuralNetwork{
    Neuron[][] neurons;
    Neuron[] outputLayer;
    int layers;
    int totalNeurons;
    int totalInputs;
    int totalOutputs;
    int totalConnections = 0;

    double[] currentWeights;
    int index = 0;

    double[] neuralInputs;
    double[] neuralOutputs;
    double[] outputs;

    public NeuralNetwork(int layers, int neurons, int inputs, int outputs){
        this.layers = layers;
        this.totalNeurons = neurons;
        this.totalInputs = inputs;
        this.totalOutputs = outputs;

        if(layers < neurons){
            this.neurons = new Neuron[layers][];

            for(int i = 0; i < layers; i++){
                this.neurons[i] = new Neuron[neurons / (layers - i) + neurons % (layers - i)];
                neurons -= this.neurons[i].length;

                for(int j = 0; j < this.neurons[i].length; j++){

                    if(i == 0){
                        this.neurons[i][j] = new Neuron(inputs);
                    } else{
                        this.neurons[i][j] = new Neuron(this.neurons[i - 1].length);
                    }

                    //System.out.println("Connections in Neuron " + j + " at layer " + i + ": " + this.neurons[i][j].getConnections());
                }
    
            }

            this.outputLayer = new Neuron[outputs];

            for(int i = 0; i < outputs; i++){
                this.outputLayer[i] = new Neuron(this.neurons[this.neurons.length - 1].length);
            }

            calcTotalConnections();
            //System.out.println(this.totalConnections);
        } else{
            System.out.println("Cannot create network with more layers than neurons");
        }

    }

    public void calcTotalConnections(){

        for(int i = 1; i < layers; i++){
            
            if(neurons.length > 1){
                totalConnections += neurons[i - 1].length * neurons[i].length;
            }

        }

        totalConnections += neurons[0].length * totalInputs;
        totalConnections += neurons[neurons.length - 1].length * totalOutputs;
    }

    public void setWeights(double[] weights){
        index = 0;
        
        if(weights.length < totalConnections + totalNeurons){
            System.out.println("Weights do not cover every connection");
        } else{

            for(int i = 0; i < layers; i++){

                for(int j = 0; j < neurons[i].length; j++){
                    currentWeights = new double[neurons[i][0].getConnections() + 2];

                    for(int k = 0; k < neurons[i][0].getConnections() + 2; k++){
                        currentWeights[k] = weights[index];
                        index++;
                    }

                    neurons[i][j].setWeights(currentWeights);
                }

            }

            for(int i = 0; i < outputLayer.length; i++){
                currentWeights = new double[outputLayer[0].getConnections() + 2];

                for(int j = 0; j < outputLayer[0].getConnections() + 2; j++){
                    currentWeights[j] = weights[index];
                    index++;
                }

                outputLayer[i].setWeights(currentWeights);
            }

        }

    }

    public double[] getOutputs(double[] inputs){
        neuralInputs = inputs;
        outputs = new double[outputLayer.length];

        for(int i = 0; i < layers; i++){
            neuralOutputs = new double[neurons[i].length];

            for(int j = 0; j < neurons[i].length; j++){
                neuralOutputs[j] = neurons[i][j].calcOutput(neuralInputs);
            }

            neuralInputs = neuralOutputs;
        }

        for(int i = 0; i < outputLayer.length; i++){
            outputs[i] = outputLayer[i].calcOutput(neuralInputs);
        }

        return outputs;
    }

    public int getTotalSize(){
        return totalConnections + 2 * (totalNeurons + totalOutputs);
    }
}

class Neuron{
    private double[] weights;
    private double bias;
    private double threshold;
    private int connections;

    public Neuron(int inputs){
        this.connections = inputs;
    }

    public void setWeights(double[] weights){

        if(weights.length == connections + 2){
            this.weights = weights;
            this.bias = weights[connections];
            this.threshold = weights[connections + 1];
        } else{
            System.out.println("Assigned weights do not cover all connections");
        }

    }

    public double calcOutput(double[] inputs){
        double sum = 0;
        
        for(int i = 0; i < connections; i++){
            sum += inputs[i] * weights[i];
        }

        sum += bias;

        if(sum > 0){    //Rectifier functionality
            return sum;
        } else{
            return 0;
        }

        //return (1/(1 + Math.pow(Math.E, sum))); //Sigmoid functionality

        /*if(sum > threshold){    //Perceptron functionality
            return 1;
        } else{
            return 0;
        }*/

        //return sum;   //Raw functionality
    }

    public int getConnections(){return connections;}
}