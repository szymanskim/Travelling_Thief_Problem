import java.util.ArrayList;
import java.util.Arrays;

public class Individual {
    private int[] route;
    private int[] packingPlan;
    private double[][] distances;
    private int[][] items;
    private Integer[][] groupedItems;
    private ArrayList<double[]> gainOfItems;
    private double maxSpeed;
    private double coefficient;
    private double rentingRatio;
    private double basicTime;
    private int capacity;
    private double fitness;

    public Individual(int[] route, double[][] distances, int[][] items,
                      Integer[][] groupedItems, double maxSpeed, double coefficient, double rentingRatio, int capacity) {
        this.route = route;
        this.distances = distances;
        this.groupedItems = groupedItems;
        this.items = items;
        gainOfItems = new ArrayList<>();
        this.maxSpeed = maxSpeed;
        this.coefficient = coefficient;
        this.rentingRatio = rentingRatio;
        basicTime = countTime(0, maxSpeed);
        this.capacity = capacity;
        packingPlan = new int[items.length];

        countGain();
        setPackingPlan();
        countFitness();
    }

    private void setPackingPlan() {
        int ind = 0;
        int currentWeight = 0;
        while(currentWeight < capacity && ind < gainOfItems.size()) {
            int rowNumber = (int)gainOfItems.get(ind)[0];
            if(items[rowNumber][2] + currentWeight <= capacity) {
                packingPlan[rowNumber] = 1;
                currentWeight += items[rowNumber][2];
            }
            ind++;
        }
    }

    private double countTime(int startIndex, double currentSpeed) {
        int endIndex = route.length - 1;
        return countTime(startIndex, endIndex, currentSpeed);
    }

    private double countTime(int startIndex, int endIndex, double currentSpeed) {
        return countRoad(startIndex, endIndex) / currentSpeed;
    }

    private double countRoad(int startIndex) {
        return countRoad(startIndex, route.length - 1);
    }

    private double countRoad(int startIndex, int endIndex) {
        double completeDistance = 0;
        if(endIndex == route.length - 1) {
            for(int i = startIndex; i < endIndex - 1; ) {
                completeDistance += distances[route[i]][route[++i]];
            }
            completeDistance += distances[route.length - 2][0];
        }
        else {
            for(int i = startIndex; i < endIndex; i++) {
                completeDistance += distances[route[i]][route[i + 1]];
            }
        }

        return completeDistance;
    }

    public double countSpeed(double currentWeight) {
        return maxSpeed - (currentWeight * coefficient);
    }
    //for each item gain is counted as a v - R(t - tb), where v - value of one item, R - renting ratio,
    // t - time of carrying this one item with no more items from the point it was placed to,
    //tb - basic time of travel from the chosen point with empty knapsack
    private void countGain() {
        for(int i = 0; i < items.length; i++) {
            int ind = -1;//index of a city of an item 'i' in a route
            for(int j = 0; j < route.length; j++) {//finding index of city number
                if(route[j] + 1 == items[i][3]) {
                    ind = j;
                    break;
                }
            }
            gainOfItems.add(new double[] {i, (((items[i][1] -
                    - rentingRatio * (countTime(ind, countSpeed(items[i][2])) - countTime(ind, maxSpeed)))
                    / capacity))});
        }
        gainOfItems.sort((double[] o1, double[] o2) ->
                o2[1] - o1[1] < 0 ? -1 : o2[1] > 0 ? 1 : 0);
    }

    public double countFitness() {
        double wage = 0;
        double weight = 0;
        double time = 0;
        for(int currentPosition = 0; currentPosition < route.length - 1; ) {
                Integer[] currentCity = groupedItems[currentPosition];
                for(int j = 0; j < currentCity.length; j++) {
                    if(packingPlan[currentCity[j]] == 1) {
                        wage += items[currentCity[j]][1];
                        weight += items[currentCity[j]][2];
                    }
                }
            time += countTime(currentPosition, ++currentPosition, countSpeed(weight));
        }
        fitness = wage - (rentingRatio * time);
        return fitness;
    }

    public double countFitnessForRoute() {
        double distance = 0;
        for(int i = 0; i < route.length - 1; ) {
            distance += distances[route[i]][route[++i]];
        }
        return distance;
    }

    public int[] getRoute() {
        return route;
    }

    public double getFitness() { return fitness; }

    public String toString() {
        String result = "[";
        for(int i = 0; i < route.length - 1; i++) {
            result += route[i] + ", ";
        }
        result += route[route.length - 1] + "]";
        return result;
    }
}
