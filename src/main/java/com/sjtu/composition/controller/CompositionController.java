package com.sjtu.composition.controller;

import com.sjtu.composition.graph.CompositionSolution;
import com.sjtu.composition.graph.executionPath.ExecutionPath;
import com.sjtu.composition.graph.serviceGraph.ServiceGraph;
import com.sjtu.composition.graph.serviceGraph.ServiceNode;
import com.sjtu.composition.serviceUtils.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.*;

@RestController
public class CompositionController {

    private static final ServiceGraph graph = new ServiceGraph();

    @PostConstruct
    public void initGraph() {

        String descriptionX = "Given certain location, take photo and get temperature.";
        String[] inputsX = {"定位"};
        String[] outputsX = {"照片", "温度"};
        Service serviceX = new Service(0, descriptionX, inputsX, outputsX);

        String descriptionA = "get the temperature somewhere";
        String[] inputsA = {"定位"};
        String[] outputsA = {"温度"};
        Service serviceA = new Service(1, descriptionA, inputsA, outputsA);

        String descriptionB = "get longitude and latitude";
        String[] inputsB = {"定位"};
        String[] outputsB = {"经度", "纬度"};
        Service serviceB = new Service(2, descriptionB, inputsB, outputsB);

        String descriptionC = "according to (latitude, longitude), take photo by satellite";
        String[] inputsC = {"纬度", "经度"};
        String[] outputsC = {"照片"};
        Service serviceC = new Service(3, descriptionC, inputsC, outputsC);

        String descriptionD = "get longitude";
        String[] inputsD = {"定位"};
        String[] outputsD = {"经度"};
        Service serviceD = new Service(4, descriptionD, inputsD, outputsD);

        String descriptionE = "get timezone";
        String[] inputsE = {"经度"};
        String[] outputsE = {"时区"};
        Service serviceE = new Service(5, descriptionE, inputsE, outputsE);

        graph.addService(serviceX);
        graph.addService(serviceA);
        graph.addService(serviceB);
        graph.addService(serviceC);
        graph.addService(serviceD);
        graph.addService(serviceE);
        System.out.println(graph);

    }

    @GetMapping("/")
    public Set<Service> getGraph() {
        Collection<ServiceNode> serviceNodes = graph.getServiceNodeMap().values();
        Set<Service> serviceSet = new HashSet<>();
        for (ServiceNode node : serviceNodes) {
            serviceSet.add(node.getService());
        }
        return serviceSet;
    }

    @GetMapping("/substitution")
    public String substitutionIntroduction() {
        return "/substitution/{serviceId}?{?}={?}";
    }

    @GetMapping("/substitution/{serviceId}")
    public Object[] substitute(@PathVariable("serviceId") int serviceId,
                               @RequestParam(value = "x", defaultValue = "0") int s) {
        Service serviceX = graph.getServiceNodeMap().get(serviceId).getService();

        CompositionSolution solutionX = graph.search(serviceX, 0.99999999, 5);
        System.out.println(solutionX);
        Set<ExecutionPath> pathsX = solutionX.extractExecutionPaths(3);
        System.out.println("Path count = " + pathsX.size());

        Object[] argsX = {"location{0}"};
        List<Object> argsListX = Arrays.asList(argsX);
        Object[] resultX;
        for (ExecutionPath path : pathsX) {
            System.out.println();
            if (path.isAvailable()) {
                resultX = path.run(argsListX);
                System.out.println(Arrays.toString(resultX));
            }
        }
        System.out.println("\n______ test run serviceX ______");
        return serviceX.run(argsX);
    }

}
