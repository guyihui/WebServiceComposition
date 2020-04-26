package com.sjtu.composition.similarityUtils;

public class SimilarityUtils {

    // TODO: 根据词向量模型获取相似度
    public static double calculateSimilarity(String s1, String s2) {
        return mockSimilarity(s1, s2);
    }

    private static double mockSimilarity(String s1, String s2) {
        double similarity;
        return s1.equals(s2) ? 1.0 : 0.0;
    }
}
