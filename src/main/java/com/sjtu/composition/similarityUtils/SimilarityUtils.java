package com.sjtu.composition.similarityUtils;

import com.sjtu.composition.serviceUtils.Parameter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimilarityUtils {

    // TODO: 是否需要排序？（ B+ 树？）
    private Map<Parameter, Map<Parameter, Double>> mockSimilarityMap = new ConcurrentHashMap<>();

    // TODO: 根据词向量模型获取相似度
    public double similarity(Parameter p1, Parameter p2) {
        return mockSimilarity(p1, p2);
    }

    private double mockSimilarity(Parameter p1, Parameter p2) {
        Map<Parameter, Double> map1 = mockSimilarityMap.computeIfAbsent(p1, v -> new ConcurrentHashMap<>());
        if (!map1.containsKey(p2)) {
            double similarity = p1.getName().equals(p2.getName()) ? 1.0 : 0.0;
            map1.putIfAbsent(p2, similarity);
            Map<Parameter, Double> map2 = mockSimilarityMap.computeIfAbsent(p2, v -> new ConcurrentHashMap<>());
            map2.putIfAbsent(p1, similarity);
            return similarity;
        } else {
            return map1.get(p2);
        }
    }
    //TODO: map.get(a).get(b) == map.get(b).get(a)
}
