import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class Evolution {

    private int dimension;
    private int capacity;
    private double minSpeed;
    private double maxSpeed;
    private double rentingRatio;
    private double[][] cities;
    private double[][] distances;
    private int[][] items;
    private Integer[][] groupedItems;

    private int popSize;
    private int numOfGeners;
    private double crossProb;
    private double mutProb;
    private int tournamentSize;

    private double coefficient;

    private ArrayList<Individual> population = new ArrayList<>();

    public double maxOfAll = Double.MIN_VALUE;

    public Evolution(String definitionFile, int popSize, int numOfGeners, int tournamentSize, double crossProb, double mutProb) {
        Loader loader = new Loader(definitionFile);
        loader.readFile();
        dimension = loader.getDimension();
        capacity = loader.getCapacity();
        minSpeed = loader.getMinSpeed();
        maxSpeed = loader.getMaxSpeed();
        rentingRatio = loader.getRentingRatio();
        cities = loader.getCities();
        items = loader.getItems();
        distances = new double[dimension][dimension];
        groupedItems = new Integer[dimension][];
        createDistancesArray();
        createGroupedItemsArray();

        coefficient = (maxSpeed - minSpeed) / capacity;

        this.popSize = popSize;
        this.numOfGeners = numOfGeners;
        this.tournamentSize = tournamentSize;
        this.crossProb = crossProb;
        this.mutProb = mutProb;
    }

    public void createDistancesArray() {
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                double distance;
                if (i == j) {
                    distance = 0;
                } else {
                    distance = Math.sqrt(Math.abs(cities[i][1] - cities[j][1])
                            + Math.abs(cities[i][2] - cities[j][2]));
                }
                distances[i][j] = distance;
                distances[j][i] = distance;//redundant
                //mozna zmienic tablicę na niesymetryczna, zeby zajmowala mniej miejsca
            }
        }
    }

    public void createGroupedItemsArray() {
        ArrayList<Integer>[] groupedItemsList = new ArrayList[dimension];
        for(int i = 0; i < dimension; i++) {
            groupedItemsList[i] = new ArrayList<>();
        }
        for(int i = 0; i < items.length; i++) {
            groupedItemsList[items[i][3] - 1].add(items[i][0] - 1);
        }
        for(int i = 0; i < dimension; i++) {
            groupedItems[i] = new Integer[groupedItemsList[i].size()];
            for (int j = 0; j < groupedItemsList[i].size(); j++) {
                groupedItems[i][j] = groupedItemsList[i].get(j);
            }
        }
    }

    public void initialize() {
        for (int i = 0; i < popSize; i++) {
            population.add(generateRandomInd());
        }
    }

    public Individual generateRandomInd() {
        int[] route = new int[dimension + 1];
        ArrayList<Integer> routeList = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            routeList.add(i);
        }
        Collections.shuffle(routeList);
        for (int i = 0; i < dimension; i++) {
            route[i] = routeList.get(i);
        }
        route[dimension] = route[0];
        Individual ind = new Individual(route, distances, items, groupedItems, maxSpeed, coefficient, rentingRatio, capacity);

        return ind;
    }


    public String evolve(int generCounter) {
        initialize();
        ArrayList<Individual> nextGeneration = new ArrayList<>();
        for(int i = 0; i < popSize - 1; i++) {
            int[][] children = crossingOver(tournament(), tournament());
            nextGeneration.add(new Individual(mutation(children[0]), distances, items, groupedItems, maxSpeed,
                    coefficient, rentingRatio, capacity));
            if(nextGeneration.size() < popSize) {
                nextGeneration.add(new Individual(mutation(children[1]), distances, items, groupedItems, maxSpeed,
                        coefficient, rentingRatio, capacity));
                i++;
            }
        }
        population = nextGeneration;
        return statistics(generCounter);
    }

    public int[] tournament() {

        double bestFitness = Double.MIN_VALUE;
        int rand = new Random().nextInt(popSize);
        Individual best = population.get(rand);//never remembered, just initialization
        for(int i = 0; i < tournamentSize; i++) {
            Individual current = population.get(new Random().nextInt(popSize));
            double fitness = current.countFitness();
//            double fitness = current.countFitnessForRoute();
            if(fitness > bestFitness) {
                bestFitness = fitness;
                best = current;
            }
        }
        return best.getRoute();
    }

    public double countDistance(int[] route) {
        double distance = 0;
        for(int i = 0; i < route.length - 2; ) {
            distance += distances[i][++i];
        }
        distance += distances[route.length - 2][0];
        return distance;
    }

    public int[][] crossingOver(int[] parent1, int[] parent2) {
        int[] child1 = new int[parent1.length];
        int[] child2 = new int[parent1.length];
        if(Math.random() < crossProb) {
            int crossPoint = new Random().nextInt(parent1.length);
            for(int i = 0; i < crossPoint; i++) {
                child1[i] = parent1[i];
                child2[i] = parent2[i];
            }
            //rest for parent1
            boolean used = false;
            int from = 0;
            for(int empty = crossPoint; empty < child1.length - 1;) {
                for(int j = 0; j < empty; j++) {
                    if(child1[j] == parent2[from]){
                        used = true;
                        break;
                    }
                }
                if(!used) {
                    child1[empty] = parent2[from];
                    empty++;
                }
                used = false;
                from++;
            }
            child1[parent1.length - 1] = child1[0];

            //rest for parent2
            used = false;
            from = 0;
            for(int empty = crossPoint; empty < child2.length - 1;) {
                for(int j = 0; j < empty; j++) {
                    if(child2[j] == parent1[from]){
                        used = true;
                        break;
                    }
                }
                if(!used) {
                    child2[empty] = parent1[from];
                    empty++;
                }
                used = false;
                from++;
            }
            child2[parent2.length - 1] = child2[0];
        }
        else {
            child1 = parent1;
            child2 = parent2;
        }
        return new int[][] {child1, child2};
    }

    public int[] mutation(int[] route) {
        for(int i = 0; i < route.length - 2; i++) {
            if(Math.random() < mutProb) {
                int swapIndex = new Random().nextInt(route.length - 1);
                int temp = route[i];
                route[i] = route[swapIndex];
                route[swapIndex] = temp;
            }
            route[route.length - 1] = route[0];
        }
        return route;
    }

    public int getNumOfGeners() {
        return numOfGeners;
    }

    public String statistics(int pop_number) {
        double minFitness = 2.147483647E9D;
        double maxFitness = 0.0D;
        double avgDuration = 0.0D;


        for(int i = 0; i < popSize; i++) {
            Individual ind = population.get(i);
            double fitness = ind.getFitness();
            if (fitness < minFitness) {
                minFitness = fitness;
            }

            if (fitness > maxFitness) {
                maxFitness = fitness;
            }

            avgDuration += fitness;
        }

        if (this.maxOfAll < maxFitness) {
            this.maxOfAll = maxFitness;
        }

        return pop_number + ", " + minFitness + "," + maxFitness + "," + avgDuration / (double)popSize;
    }
}
