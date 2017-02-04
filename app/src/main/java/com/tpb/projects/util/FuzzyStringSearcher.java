/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by theo on 03/02/17.
 *
 * Possible algorithms
 * https://en.wikipedia.org/wiki/Bitap_algorithm
 * Uses Levenshtein distance on substrings
 * First computes a set of bitmasks containing one bit for each element of the pattern
 *
 * Rabin-Karp algorithm
 * https://en.wikipedia.org/wiki/Rabin%E2%80%93Karp_algorithm
 * Uses hashing to find any one of a set of pattern strings in a atext
 *
 * Knuth-Morris-Pratt algorithm
 * https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm
 *
 * Boyer-Moore string search
 * https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
 *
 *
 */

/*
Bitap algorithm fuzzy search
To perform fuzzy searching, we need a 2d bit array
Instead of having a single array which changes over the length of the text, we have k arrays
R_i holds a representation of the prefixes of pattern that match any suffix of the current string with i or fewer errors.
An error may be insertion, deletion, or substitution


 */

public class FuzzyStringSearcher {
    private static final String TAG = FuzzyStringSearcher.class.getSimpleName();

    private ArrayList<String> items;
    private int[] queryMask = new int[65536];

    private static FuzzyStringSearcher instance;

    private FuzzyStringSearcher(ArrayList<String> items) {
        this.items = items;
    }

    public static FuzzyStringSearcher getInstance(ArrayList<String> items) {
        if(instance == null) {
            instance = new FuzzyStringSearcher(items);
        } else {
            instance.setItems(items);
        }
        return instance;
    }

    public void setItems(ArrayList<String> items) {
        this.items = items;
    }

    public ArrayList<Integer> search(String query) {
        final ArrayList<Integer> positions = new ArrayList<>();
        final ArrayList<Integer> ranks = new ArrayList<>();
        Log.i(TAG, "search: For " + query);
        int index, rank;
        for(int i = 0; i < items.size(); i++) {
            index = findIndex(items.get(i), query, 1);
            if(index >= 0) {
                rank = rank(index, query, items.get(i));
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

        if(query.isEmpty()) return 0;
        if(m > 31) return -1;

        R = new int[k + 1];
        for(i = 0; i <= k; ++i) {
            R[i] = ~1; //Bitwise complement of 1
        }
        Arrays.fill(queryMask, ~0);

        for(i = 0; i < m; ++i) {
            queryMask[query.charAt(i)] &= ~(1 << i);
        }
        for(i = 0; i < s.length(); ++i) {
            int oldRd1 = R[0];
            R[0] |= queryMask[s.charAt(i)];
            R[0] <<= 1;

            for (d = 1; d <= k; ++d) {
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

    private int rank(int pos, String query, String text) {
        int rank = -pos;
        for(int i = pos; i < text.length() && i < query.length(); i++) {
            if(text.charAt(i) == query.charAt(i - pos)) rank++;
        }
        return rank;
    }

}
