import serviceUtils.CompositionSolution;
import serviceUtils.Service;
import serviceUtils.executionPath.ExecutionPath;
import serviceUtils.serviceGraph.ServiceGraph;

import java.util.Set;

public class Main {
    public static void main(String[] args) {

        ServiceGraph graph = new ServiceGraph();

        String descriptionX = "Given certain location, take photo and get temperature.";
        String[] inputsX = {"location"};
        String[] outputsX = {"photo", "temperature"};
        Service serviceX = new Service(0, descriptionX, inputsX, outputsX);

        String descriptionA = "get the temperature somewhere";
        String[] inputsA = {"location"};
        String[] outputsA = {"temperature"};
        Service serviceA = new Service(1, descriptionA, inputsA, outputsA);

        String descriptionB = "get longitude and latitude";
        String[] inputsB = {"location"};
        String[] outputsB = {"longitude", "latitude"};
        Service serviceB = new Service(2, descriptionB, inputsB, outputsB);

        String descriptionC = "according to (latitude, longitude), take photo by satellite";
        String[] inputsC = {"latitude", "longitude"};
        String[] outputsC = {"photo"};
        Service serviceC = new Service(3, descriptionC, inputsC, outputsC);

        String descriptionD = "get longitude";
        String[] inputsD = {"location"};
        String[] outputsD = {"longitude"};
        Service serviceD = new Service(4, descriptionD, inputsD, outputsD);

        String descriptionE = "get timezone";
        String[] inputsE = {"longitude"};
        String[] outputsE = {"UTC timezone"};
        Service serviceE = new Service(5, descriptionE, inputsE, outputsE);

//        X = A // (B -> C)
        graph.addService(serviceX);
        graph.addService(serviceA);
        graph.addService(serviceB);
        graph.addService(serviceC);
        graph.addService(serviceD);
        graph.addService(serviceE);

        System.out.println(graph);
        CompositionSolution solution = graph.search(serviceX, 1.0, 5);
        System.out.println(solution);
        Set<ExecutionPath> paths = solution.extractExecutionPaths();
        System.out.println(paths);
    }

}
