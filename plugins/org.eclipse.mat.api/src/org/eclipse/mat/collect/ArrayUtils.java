/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.collect;


public class ArrayUtils
{

    /**
     * Sorts the keys in an increasing order. Elements key[i] and
     * values[i] are always swapped together in the corresponding arrays. 
     * 
     * A mixture of several sorting algorithms is used:
     * 
     * A radix sort performs better on the numeric data we sort, but requires 
     * additional storage to perform the sorting. Therefore only the not-very-large
     * parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the overhead of the 
     * radix sort is also bigger 
     * 
     * Works correctly only with positive keys!
     *  
     */
    public static void sort(int[] keys, int[] values)
    {
        hybridsort(keys, values, 0, keys.length - 1);
    }

    /**
     * Sorts the keys in an decreasing order. Elements key[i] and
     * values[i] are always swapped together in the corresponding arrays. 
     * 
     * A mixture of several sorting algorithms is used:
     * 
     * A radix sort performs better on the numeric data we sort, but requires 
     * additional storage to perform the sorting. Therefore only the not-very-large
     * parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the overhead of the 
     * radix sort is also bigger 
     * 
     * Works correctly only with positive keys!
     *  
     */
    public static void sortDesc(long[] keys, int[] values)
    {
        hybridsortDesc(keys, values, null, null, 0, keys.length - 1);
    }

    
    /**
     * Sorts the keys in an decreasing order. Elements key[i] and
     * values[i] are always swapped together in the corresponding arrays. 
     * 
     * A mixture of several sorting algorithms is used:
     * 
     * A radix sort performs better on the numeric data we sort, but requires 
     * additional storage to perform the sorting. Therefore only the not-very-large
     * parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the overhead of the 
     * radix sort is also bigger 
     * 
     * Works correctly only with positive keys!
     * 
     * This version of the method allows the temporarily needed arrays for the radix sort
     * to be provided externally - tempa and tempb. This saves unnecessary array creation
     * and cleanup
     *  
     */
    public static void sortDesc(long[] a, int[] b, long[] tmpa, int[] tmpb)
    {
        hybridsortDesc(a, b, tmpa, tmpb, 0, a.length - 1);
    }

    /**
     * Sorts a range from the keys in an increasing order. Elements key[i] and
     * values[i] are always swapped together in the corresponding arrays. 
     * 
     * A mixture of several sorting algorithms is used:
     * 
     * A radix sort performs better on the numeric data we sort, but requires 
     * additional storage to perform the sorting. Therefore only the not-very-large
     * parts produced by a quick sort are sorted with radix sort.
     * An insertion sort is used to sort the smallest arrays, where the the overhead of the 
     * radix sort is also bigger 
     * 
     * Works correctly only with positive keys!
     *  
     */
    public static void sort(int[] keys, int[] values, int offset, int length)
    {
        hybridsort(keys, values, offset, offset + length - 1);
    }
    

    /* *********************************************
     * 
     * PRIVATE METHODS IMPLEMENTING THE SORTING 
     * 
     * ********************************************/

    private static void swap(int keys[], int values[], int a, int b)
    {
        // swap the keys
        int tmp = keys[a];
        keys[a] = keys[b];
        keys[b] = tmp;

        // swap the values
        tmp = values[a];
        values[a] = values[b];
        values[b] = tmp;
    }

    private static void swap(long keys[], int values[], int a, int b)
    {
        // swap the keys
        long tmpKey = keys[a];
        keys[a] = keys[b];
        keys[b] = tmpKey;

        // swap the values
        int tmpValue = values[a];
        values[a] = values[b];
        values[b] = tmpValue;
    }

    private static int median(int x[], int pos1, int pos2, int pos3)
    {
        int v1 = x[pos1];
        int v2 = x[pos2];
        int v3 = x[pos3];
        
        if (v1 < v2)
            if (v2 <= v3)
                return pos2;
            else
                return v1 < v3 ? pos3 : pos1; 
        
        /* else -> v1 > v2 */
        if (v1 <= v3)
            return pos1;
        else
            return v2 < v3 ? pos3 : pos2;
    }

    private static int median(long x[], int pos1, int pos2, int pos3)
    {
        long v1 = x[pos1];
        long v2 = x[pos2];
        long v3 = x[pos3];
        
        if (v1 < v2)
            if (v2 <= v3)
                return pos2;
            else
                return v1 < v3 ? pos3 : pos1; 
        
        /* else -> v1 > v2 */
        if (v1 <= v3)
            return pos1;
        else
            return v2 < v3 ? pos3 : pos2;    
    }

    private static int split(int[] keys, int[] values, int left, int right)
    {
        // just take the median of the middle key and the two border keys
        int splittingIdx = median(keys, left, right, left + ((right - left) >> 1));
        int splittingValue = keys[splittingIdx];

        // move splitting element first
        swap(keys, values, left, splittingIdx);

        int i = left;
        for (int j = left + 1; j <= right; j++)
        {
            if (keys[j] < splittingValue)
            {
                i++;
                swap(keys, values, i, j);
            }
        }
        swap(keys, values, left, i);

        return i;
    }

    private static int splitDesc(long[] keys, int[] values, int left, int right)
    {
        // just take the median of the middle key and the two border keys
        int splittingIdx = median(keys, left, right, left + ((right - left) >> 1));
        long splittingValue = keys[splittingIdx];

        // move splitting element first
        swap(keys, values, left, splittingIdx);

        int i = left;
        for (int j = left + 1; j <= right; j++)
        {
            if (keys[j] > splittingValue)
            {
                i++;
                swap(keys, values, i, j);
            }
        }
        swap(keys, values, left, i);

        return i;
    }

    private static void hybridsort(int[] keys, int[] values, int left, int right)
    {
        if (right - left >= 1)
        {
            if (right - left < 5000000)
            {
                radixsort(keys, values, left, right - left + 1);
            }
            else
            {
                int i = split(keys, values, left, right);
                hybridsort(keys, values, left, i - 1);
                hybridsort(keys, values, i + 1, right);
            }
        }
    }

    private static void hybridsortDesc(long[] keys, int[] values, long[] tmpKeys, int[] tmpValues, int left, int right)
    {
        if (right - left >= 1)
        {
            if (right - left < 5000000)
            {
                // use insert sort on the small ones
                // to avoid the loop in radix sort
                if (right - left < 12)
                {
                    for (int i = left; i <= right; i++)
                        for (int j = i; j > left && keys[j - 1] < keys[j]; j--)
                            swap(keys, values, j, j - 1);
                    return;
                }
                radixsortDesc(keys, values, tmpKeys, tmpValues, left, right - left + 1);
            }
            else
            {
                int i = splitDesc(keys, values, left, right);
                hybridsortDesc(keys, values, tmpKeys, tmpValues, left, i - 1);
                hybridsortDesc(keys, values, tmpKeys, tmpValues, i + 1, right);
            }
        }
    }

    private static void radixsort(int[] keys, int[] values, int offset, int length)
    {
        int[] tempKeys = new int[length];
        int[] tempValues = new int[length];
        countsort(keys, tempKeys, values, tempValues, offset, 0, length, 0);
        countsort(tempKeys, keys, tempValues, values, 0, offset, length, 1);
        countsort(keys, tempKeys, values, tempValues, offset, 0, length, 2);
        countsort(tempKeys, keys, tempValues, values, 0, offset, length, 3);
    }

    private static void radixsortDesc(long[] keys, int[] values, long[] tempKeys, int[] tempValues, int offset,
                    int length)
    {
        if (tempKeys == null)
            tempKeys = new long[length];
        if (tempValues == null)
            tempValues = new int[length];
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 0);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 1);
        countsortDesc(keys, tempKeys, values, tempValues, offset, 0, length, 2);
        countsortDesc(tempKeys, keys, tempValues, values, 0, offset, length, 3);
    }

    private static void countsort(int[] srcKeys, int[] destKeys, int[] srcValues, int[] destValues, int srcOffset,
                    int trgOffset, int length, int sortByte)
    {
        int[] count = new int[256];
        int[] index = new int[256];

        int shiftBits = 8 * sortByte;
        int srcEnd = srcOffset + length;

        for (int i = srcOffset; i < srcEnd; i++)
            count[(int) ((srcKeys[i] >> (shiftBits)) & 0xff)]++;

        /* index[0] = 0 */
        for (int i = 1; i < 256; i++)
            index[i] = index[i - 1] + count[i - 1];

        for (int i = srcOffset; i < srcEnd; i++)
        {
            int idx = (int) ((srcKeys[i] >> (shiftBits)) & 0xff);
            destValues[trgOffset + index[idx]] = srcValues[i];
            destKeys[trgOffset + index[idx]++] = srcKeys[i];
        }
    }

    private static void countsortDesc(long[] srcKeys, long[] destKeys, int[] srcValues, int[] destValues,
                    int srcOffset, int trgOffset, int length, int sortByte)
    {
        int[] count = new int[256];
        int[] index = new int[256];

        int shiftBits = 8 * sortByte;
        int srcEnd = srcOffset + length;

        for (int i = srcOffset; i < srcEnd; i++)
            count[(int) ((srcKeys[i] >> (shiftBits)) & 0xff)]++;

        /* index[255] = 0 */
        for (int i = 254; i >= 0; i--)
            index[i] = index[i + 1] + count[i + 1];

        for (int i = srcOffset; i < srcEnd; i++)
        {
            int idx = (int) ((srcKeys[i] >> (shiftBits)) & 0xff);
            destValues[trgOffset + index[idx]] = srcValues[i];
            destKeys[trgOffset + index[idx]++] = srcKeys[i];
        }
    }

}
