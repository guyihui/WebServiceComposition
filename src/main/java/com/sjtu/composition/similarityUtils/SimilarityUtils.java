package com.sjtu.composition.similarityUtils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.sjtu.composition.serviceUtils.Parameter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimilarityUtils {

    // TODO: 是否需要排序？（ B+ 树？）
    private Map<Parameter, Map<Parameter, Double>> mockSimilarityMap = new ConcurrentHashMap<>();

    // TODO: 根据词向量模型获取相似度
    public double similarity(Parameter p1, Parameter p2) throws Exception {
        return paramSimilarity(p1, p2);
    }

    private double paramSimilarity(Parameter p1, Parameter p2) throws Exception {
        Map<Parameter, Double> map1 = mockSimilarityMap.computeIfAbsent(p1, v -> new ConcurrentHashMap<>());
        if (!map1.containsKey(p2)) {
            double similarity = this.textSimilarity(p1.getDescription(), p2.getDescription());
            map1.putIfAbsent(p2, similarity);
            Map<Parameter, Double> map2 = mockSimilarityMap.computeIfAbsent(p2, v -> new ConcurrentHashMap<>());
            map2.putIfAbsent(p1, similarity);
            return similarity;
        } else {
            return map1.get(p2);
        }
    }

    /**
     * 预训练词向量
     */
    private static final String WORD_VECTOR_PATH = "E:\\BaiduNetdiskDownload\\500000-small.txt";
    private static final int MAX_VECTOR_COUNT = 500000;
    private Map<String, List<Double>> wordVectorMap = new HashMap<>(500000);
    private Integer dimension;

    @PostConstruct
    public boolean loadPreTrainedWordVector() {
        try {
            Scanner scanner = new Scanner(new File(WORD_VECTOR_PATH));
            String[] scale = scanner.nextLine().split(" ");
            System.out.println("scale:" + Arrays.toString(scale));
            int size = Integer.valueOf(scale[0]);
            dimension = Integer.valueOf(scale[1]);

            for (int i = 0; i < Math.min(size, MAX_VECTOR_COUNT); i++) {
                if (!scanner.hasNextLine()) {
                    System.out.println(i + "<{" + size + "," + MAX_VECTOR_COUNT + "}");
                    break;
                }
                String line = scanner.nextLine();
                String[] split = line.split(" ");
                String word = split[0];
                List<Double> vector = new ArrayList<>(dimension);
                for (int j = 1; j <= dimension; j++) {
                    vector.add(Double.valueOf(split[j]));
                }
                wordVectorMap.put(word, vector);
            }

            System.out.println("vector count = " + wordVectorMap.size());
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

    }

    private double textSimilarity(String text1, String text2) throws Exception {
        List<Double> textVector1 = text2vector(text1);
        List<Double> textVector2 = text2vector(text2);
        double squareSum1 = 0;
        double squareSum2 = 0;
        double x = 0;
        for (int i = 0; i < dimension; i++) {
            x += textVector1.get(i) * textVector2.get(i);
            squareSum1 += Math.pow(textVector1.get(i), 2);
            squareSum2 += Math.pow(textVector2.get(i), 2);
        }
        return x / Math.sqrt(squareSum1 * squareSum2);
    }

    private List<Double> text2vector(String text) throws Exception {
        List<Term> termList = HanLP.segment(text);
        List<List<Double>> wordVectors = new ArrayList<>();
        for (Term term : termList) {
            if (wordVectorMap.containsKey(term.word)) {
                wordVectors.add(wordVectorMap.get(term.word));
            } else {
                // TODO: Bi-MM 双向最大匹配
                throw new Exception("no such word:" + term.word);
            }
        }
        List<Double> textVector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            double d = 0;
            for (List<Double> wordVector : wordVectors) {
                d += wordVector.get(i);
            }
            d /= wordVectors.size();
            textVector.add(d);
        }
        return textVector;
    }

}
