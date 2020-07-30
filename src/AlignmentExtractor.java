import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

class Pair<T1, T2> {
    public T1 first;
    public T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}

public class AlignmentExtractor {
    public static void main(String[] args) throws IOException {
        HashMap<String, Integer> sen2IDDict = new HashMap<>();
        HashMap<String, Integer> word2IDDict = new HashMap<>();
        ArrayList<String> sentences = new ArrayList<>();
        ArrayList<String> words = new ArrayList<>();
        String NULL = "_NULL_";
        words.add(NULL);
        int nullID = 0;
        word2IDDict.put(NULL, nullID);

        HashMap<Integer, ArrayList<Pair<Integer, HashMap<Integer, Integer>>>> senAlignDict = new HashMap<>();
        HashMap<Integer, Integer> srcWordCounter = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Double>> src2dstProb = new HashMap<>();
        BufferedReader parReader = new BufferedReader(new FileReader(args[0]));
        BufferedReader alignReader = new BufferedReader(new FileReader(args[1]));
        String parLine = null;
        String aLine = null;
        int lineNum = 0;
        while (((parLine = parReader.readLine()) != null) && ((aLine = alignReader.readLine()) != null)) {
            lineNum++;
            HashMap<Integer, Integer> alignDict = new HashMap<>();
            String[] aSplit = aLine.trim().split(" ");
            for (String spl : aSplit) {
                String[] strSpl = spl.trim().split("-");
                if (strSpl.length != 2) continue;
                int srcIndex = Integer.parseInt(strSpl[0]);
                int dstIndex = Integer.parseInt(strSpl[1]);
                alignDict.put(srcIndex, dstIndex);
            }

            String[] line = parLine.trim().split(" \\|\\|\\| ");
            String srcLine = line[0].trim();
            String dstLine = line[1].trim();
            if (!sen2IDDict.containsKey(srcLine)) {
                sen2IDDict.put(srcLine, sen2IDDict.size());
                sentences.add(srcLine);
            }
            if (!sen2IDDict.containsKey(dstLine)) {
                sen2IDDict.put(dstLine, sen2IDDict.size());
                sentences.add(dstLine);
            }
            int srcSenID = sen2IDDict.get(srcLine);
            int dstSenID = sen2IDDict.get(dstLine);

            if (!senAlignDict.containsKey(srcSenID)) {
                senAlignDict.put(srcSenID, new ArrayList<>());
            }

            senAlignDict.get(srcSenID).add(new Pair<>(dstSenID, alignDict));

            String[] srcWords = srcLine.split(" ");
            int[] srcIds = new int[srcWords.length];
            for (int si = 0; si < srcWords.length; si++) {
                if (!word2IDDict.containsKey(srcWords[si])) {
                    word2IDDict.put(srcWords[si], word2IDDict.size());
                    words.add(srcWords[si]);
                }
                srcIds[si] = word2IDDict.get(srcWords[si]);
            }
            String[] dstWords = dstLine.split(" ");
            int[] dstIds = new int[dstWords.length];
            for (int ti = 0; ti < dstWords.length; ti++) {
                if (!word2IDDict.containsKey(dstWords[ti])) {
                    word2IDDict.put(dstWords[ti], word2IDDict.size());
                    words.add(dstWords[ti]);
                }
                dstIds[ti] = word2IDDict.get(dstWords[ti]);
            }

            for (int si = 0; si < srcIds.length; si++) {
                if (!srcWordCounter.containsKey(srcIds[si])) {
                    srcWordCounter.put(srcIds[si], 1);
                    src2dstProb.put(srcIds[si], new HashMap<>());
                    src2dstProb.get(srcIds[si]).put(nullID, 0.0);
                } else {
                    srcWordCounter.put(srcIds[si], srcWordCounter.get(srcIds[si]) + 1);
                }

                if (alignDict.containsKey(si)) {
                    int ti = alignDict.get(si);

                    if (!src2dstProb.get(srcIds[si]).containsKey(dstWords[ti])) {
                        src2dstProb.get(srcIds[si]).put(dstIds[ti], 1.);
                    } else {
                        src2dstProb.get(srcIds[si]).put(dstIds[ti], src2dstProb.get(srcIds[si]).get(dstIds[ti]) + 1);
                    }
                } else {
                    src2dstProb.get(srcIds[si]).put(nullID, src2dstProb.get(srcIds[si]).get(nullID) + 1);
                }
            }
            if (lineNum % 1000 == 0) {
                System.out.print(lineNum + "\r");
            }
        }
        System.out.println("\nCalculating probabilities...");
        BufferedWriter probWriter = new BufferedWriter(new FileWriter(args[2]));
        int di = 0;
        for (int srcWord : srcWordCounter.keySet()) {
            int srcCount = srcWordCounter.get(srcWord);
            for (int dstWord : src2dstProb.get(srcWord).keySet()) {
                double prob = src2dstProb.get(srcWord).get(dstWord) / srcCount;
                src2dstProb.get(srcWord).put(dstWord, prob);
                probWriter.write(words.get(srcWord) + "\t" + words.get(dstWord) + "\t" + prob + "\n");
            }
            di++;
            if (di % 100 == 0) {
                System.out.print(di + "/" + srcWordCounter.size() + "\r");
            }
        }
        System.out.print(di + "/" + srcWordCounter.size() + "\n");
        System.out.println("Calculating sentence probabilities");
        BufferedWriter alignWriter = new BufferedWriter(new FileWriter(args[3]));
        int sc = 0;
        for (int srcSenId : senAlignDict.keySet()) {
            double maxProb = -100000.0;
            String srcSentence = sentences.get(srcSenId);
            String[] srcWords = srcSentence.split(" ");
            int[] srcIds = new int[srcWords.length];
            for (int si = 0; si < srcWords.length; si++) {
                if (!word2IDDict.containsKey(srcWords[si])) {
                    word2IDDict.put(srcWords[si], word2IDDict.size());
                    words.add(srcWords[si]);
                }
                srcIds[si] = word2IDDict.get(srcWords[si]);
            }

            String bestSentence = "";

            for (Pair<Integer, HashMap<Integer, Integer>> pair : senAlignDict.get(srcSenId)) {
                int dstSenID = pair.first;
                HashMap<Integer, Integer> alignDict = pair.second;
                String dstSentence = sentences.get(dstSenID);
                String[] dstWords = dstSentence.split(" ");
                int[] dstIds = new int[dstWords.length];
                for (int ti = 0; ti < dstWords.length; ti++) {
                    if (!word2IDDict.containsKey(dstWords[ti])) {
                        word2IDDict.put(dstWords[ti], word2IDDict.size());
                        words.add(dstWords[ti]);
                    }
                    dstIds[ti] = word2IDDict.get(dstWords[ti]);
                }

                double prob = 1.0;

                if (alignDict.size() == 0) {
                    prob = 0.0;
                } else {
                    for (int si = 0; si < srcWords.length; si++) {
                        if (alignDict.containsKey(si)) {
                            int ti = alignDict.get(si);
                            double tProb = src2dstProb.get(srcIds[si]).get(dstIds[ti]);
                            prob *= tProb;
                        }
                    }
                    prob = Math.pow(prob, 1.0 / alignDict.size()) / (1 + srcWords.length - alignDict.size());
                }
                if (prob > maxProb) {
                    maxProb = prob;
                    bestSentence = dstSentence;
                }
            }
            String output = srcSentence + " ||| " + bestSentence + "\t" + maxProb + "\n";
            sc++;
            alignWriter.write(output);
            if (sc % 100 == 0) {
                System.out.print(sc + "/" + senAlignDict.size() + "\r");
            }
        }
        System.out.print(sc + "/" + senAlignDict.size() + "\n");
        alignWriter.close();
        probWriter.close();
        System.out.println("Done!");
    }
}
