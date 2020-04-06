import serviceUtils.CompositionSolution;
import serviceUtils.Service;
import serviceUtils.serviceGraph.ServiceGraph;

public class Main {
    public static void main(String[] args) {

        ServiceGraph graph = new ServiceGraph();

        String descriptionX = "Given certain location, take photo and get temperature.";
        String[] inputsX = {"location"};
        String[] outputsX = {"photo", "temperature"};
        Service serviceX = new Service(1, descriptionX, inputsX, outputsX);

        String descriptionA = "get the temperature somewhere";
        String[] inputsA = {"location"};
        String[] outputsA = {"temperature"};
        Service serviceA = new Service(2, descriptionA, inputsA, outputsA);

        String descriptionB = "get longitude and latitude";
        String[] inputsB = {"location"};
        String[] outputsB = {"longitude", "latitude"};
        Service serviceB = new Service(3, descriptionB, inputsB, outputsB);

        String descriptionC = "according to (latitude, longitude), take photo by satellite";
        String[] inputsC = {"latitude", "longitude"};
        String[] outputsC = {"photo"};
        Service serviceC = new Service(4, descriptionC, inputsC, outputsC);

        String descriptionD = "interfere 1";
        String[] inputsD = {"location"};
        String[] outputsD = {"message"};
        Service serviceD = new Service(5, descriptionD, inputsD, outputsD);

        String descriptionE = "interfere 2";
        String[] inputsE = {"pdf"};
        String[] outputsE = {"txt"};
        Service serviceE = new Service(6, descriptionE, inputsE, outputsE);

//        X = A // (B -> C)
        graph.addService(serviceX);
        graph.addService(serviceA);
        graph.addService(serviceB);
        graph.addService(serviceC);
        graph.addService(serviceD);
        graph.addService(serviceE);

        System.out.println("data nodes: " + graph.getDataNodeSet().size());
        System.out.println("service nodes: " + graph.getServiceNodeSet().size());
        CompositionSolution solution = graph.search(serviceX, 1.0, 5);
        System.out.println("isExistingService: " + solution.isExistingService);
        System.out.println(solution);
    }

}
