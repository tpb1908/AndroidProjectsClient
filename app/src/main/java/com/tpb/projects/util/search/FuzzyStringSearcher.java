package com.tpb.projects.util.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by theo on 03/02/17.
 */

/*
Bitap algorithm fuzzy search
To perform fuzzy searching, we need a 2d bit array
Instead of having a single array which changes over the length of the text, we have k arrays
R_i holds a representation of the prefixes of pattern that match any suffix of the current string with i or fewer errors.
An error may be insertion, deletion, or substitution
 */

public class FuzzyStringSearcher {
    private List<String> mItems = new ArrayList<>();
    private final int[] queryMask = new int[65536];

    private static FuzzyStringSearcher instance;

    public FuzzyStringSearcher() {

    }

    private FuzzyStringSearcher(List<String> items) {
        mItems = items;
    }

    public static FuzzyStringSearcher getInstance(List<String> items) {
        if(instance == null) {
            instance = new FuzzyStringSearcher(items);
        } else {
            instance.setItems(items);
        }
        return instance;
    }

    public void setItems(List<String> items) {
        mItems = items;
    }

    public List<Integer> search(String query) {
        final List<Integer> positions = new ArrayList<>();
        final List<Integer> ranks = new ArrayList<>();
        int index, rank;
        for(int i = 0; i < mItems.size(); i++) {
            index = findIndex(mItems.get(i), query, 1);
            if(index >= 0) {
                rank = index;
                boolean added = false;
                for(int j = 0; j < ranks.size(); j++) {
                    if(rank > ranks.get(j)) {
                        added = true;
                        ranks.add(j, rank);
                        positions.add(j, i);
                        break;
                    }
                }
                if(!added) {
                    ranks.add(rank);
                    positions.add(i);
                }
            }
        }
        return positions;
    }

    private int findIndex(String s, String query, int k) {
        int result = -1;
        int m = query.length();
        int[] R;
        int i, d;

        if(query.isEmpty()) return -1;
        if(m > 31) return -1;

        R = new int[k + 1];
        for(i = 0; i <= k; ++i) {
            R[i] = ~1; //Bitwise complement of 1
        }
        Arrays.fill(queryMask, ~0); //Fill the mask

        for(i = 0; i < m; ++i) {
            queryMask[query.charAt(i)] &= ~(1 << i);
        }
        for(i = 0; i < s.length(); ++i) {
            int oldRd1 = R[0];
            R[0] |= queryMask[s.charAt(i)];
            R[0] <<= 1;

            for(d = 1; d <= k; ++d) {
                int tmp = R[d];

                R[d] = (oldRd1 & (R[d] | queryMask[s.charAt(i)])) << 1;
                oldRd1 = tmp;
            }

            if(0 == (R[k] & (1 << m))) {
                result = (i - m) + 1;
                break;
            }
        }
        return result;
    }


}
