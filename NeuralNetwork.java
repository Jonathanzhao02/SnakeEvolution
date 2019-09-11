import java.util.*;

public class NeuralNetwork{
    Neuron[][] neurons;         //Hidden layer neurons array
    Neuron[] outputLayer;       //Output layer neurons array
    int layers;                 //Number of hidden layers
    int totalNeurons;           //Number of hidden neurons
    int totalInputs;            //Number of inputs
    int totalOutputs;           //Number of outputs
    int totalConnections = 0;   //Number of weight connections

    double[] currentWeights;    //Weights of neural network
    int index = 0;              //Tracks current weight index

    double[] neuralInputs;      //Tracks current inputs
    double[] neuralOutputs;     //Tracks current outputs
    double[] outputs;           //Overall network outputs

    //Initializer with architecture definition
    public NeuralNetwork(int layers, int neurons, int inputs, int outputs){
        this.layers = layers;
        this.totalNeurons = neurons;
        this.totalInputs = inputs;
        this.totalOutputs = outputs;

        if(layers < neurons){
            this.neurons = new Neuron[layers][];

            //Splits neurons as evenly as possible between layers
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

    //Calculates total connections in network
    public void calcTotalConnections(){
        totalConnections = 0;

        for(int i = 1; i < layers; i++){
            
            if(neurons.length > 1){
                totalConnections += neurons[i - 1].length * neurons[i].length;
            }

        }

        totalConnections += neurons[0].length * totalInputs;
        totalConnections += neurons[neurons.length - 1].length * totalOutputs;
    }

    //Sets weights of the network
    public void setWeights(double[] weights){

        try{
            index = 0;
            
            if(weights.length < getTotalSize()){
                System.out.println("Weights do not cover every connection");
            } else{

                //Hidden layer weights
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

                //Output layer weights
                for(int i = 0; i < outputLayer.length; i++){
                    currentWeights = new double[outputLayer[0].getConnections() + 2];

                    for(int j = 0; j < outputLayer[0].getConnections() + 2; j++){
                        currentWeights[j] = weights[index];
                        index++;
                    }

                    outputLayer[i].setWeights(currentWeights);
                }

            }

        } catch(Exception e){

        }

    }

    //Calculates output of neural network
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

    //Calculates output of neural network while filling array with layer outputs
    public double[] getOutputs(double[] inputs, double[][] layerOutputs){
        neuralInputs = inputs;
        layerOutputs[0] = inputs;
        outputs = new double[outputLayer.length];

        for(int i = 0; i < layers; i++){
            neuralOutputs = new double[neurons[i].length];

            for(int j = 0; j < neurons[i].length; j++){
                neuralOutputs[j] = neurons[i][j].calcOutput(neuralInputs);
            }

            layerOutputs[i + 1] = neuralOutputs;
            neuralInputs = neuralOutputs;
        }

        for(int i = 0; i < outputLayer.length; i++){
            outputs[i] = outputLayer[i].calcOutput(neuralInputs);
        }

        layerOutputs[layers + 1] = outputs;
        return outputs;
    }

    //Total size of array
    public int getTotalSize(){
        return totalConnections + 2 * (totalNeurons + totalOutputs);    //Includes biases and deprecated threshold values
    }

    //Self-explanatory methods
    public Neuron[][] getNeurons(){
        return neurons;
    }

    public Neuron[] getOutputNeurons(){
        return outputLayer;
    }
}

//Neuron in neural network
class Neuron{
    private double[] weights;   //Associated weights
    private double bias;        //Associated bias
    private int connections;    //Total number of connections to previous layer
    private double threshold;   //Deprecated as hell

    //Initializer with number of connections
    public Neuron(int inputs){
        this.connections = inputs;
    }

    //Sets weights of neuron
    public void setWeights(double[] weights){

        if(weights.length == connections + 2){
            this.weights = weights;
            this.bias = weights[connections];
            this.threshold = weights[connections + 1];
        } else{
            System.out.println("Assigned weights do not cover all connections");
        }

    }

    //Calculates output of neuron by multiplying inputs and weights, adding bias, and activation function
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

    //Self-explanatory methods
    public int getConnections(){return connections;}

    public double[] getWeights(){return weights;}
    
    public double getBias(){return bias;}
}