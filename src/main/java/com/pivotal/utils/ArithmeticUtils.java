/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 *
 */
public class ArithmeticUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ArithmeticUtils.class);

    private static BigDecimal TWO = new BigDecimal(2);
    private static BigDecimal TEN = new BigDecimal(10);
    private static BigDecimal LOGTEN = BigDecimal.valueOf(Math.log(10));
    private static BigDecimal LNTWO = BigDecimal.valueOf(Math.log(2));
    private static BigDecimal MAXSIMPLE = new BigDecimal(Double.MAX_VALUE);


    /**
     * Log calculation for BigDecimals
     *
     * @param v    value
     * @param base base of the logarithm
     * @param mc   Math Context to use - { @link java.math.MathContext }
     *
     * @return result of the logarithm calculation
     */
    public static BigDecimal log(BigDecimal v, double base, MathContext mc) {
        return log(v).divide(log(new BigDecimal(base)), mc);
    }

    /**
     * Method that calculates natural logarithm for BigDecimal.
     * If number is smaller than Double.MAX_VALUE, then Math.log is used,
     * otherwise BigDecimals with v.divide(TWO).add(LNTWO).
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    public static BigDecimal log(BigDecimal v) {
        logger.debug(String.format("Input Value - %s",v));
        if (v.compareTo(MAXSIMPLE) > 0) {
            return v.divide(TWO).add(LNTWO);
        }
        else {
            return BigDecimal.valueOf(Math.log(v.doubleValue()));
        }
    }

    /**
     * Method which calculates natural logarithm for BigInteger.
     * BigDecimals are used.
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    public static BigInteger log(BigInteger v) {
        return log(new BigDecimal(v)).toBigInteger();
    }

    /**
     * Method which calculates logarithm base2 for BigDecimal.
     * If number is smaller than Double.MAX_VALUE, then Math.log(x)/Math.log(2) is used,
     * otherwise BigDecimal with log(v).divide(log(LNTWO)).
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    public static BigDecimal log2(BigDecimal v) {
        logger.debug(String.format("Input Value - %s",v));
        if (v.compareTo(MAXSIMPLE) > 0) {
            return log(v).divide(log(LNTWO));
        }
        else {
            return BigDecimal.valueOf(Math.log(v.doubleValue()) / Math.log(2));
        }
    }

    /**
     * Method which calculates logarithm base2 for BigInteger using BigDecimal.
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    @SuppressWarnings("unused")
    public static BigInteger log2(BigInteger v) {
        return log2(new BigDecimal(v)).toBigInteger();
    }

    /**
     * Method which calculates logarithm base10 for BigDecimal.
     * If number is smaller than Double.MAX_VALUE, then Math.log is used,
     * otherwise BigDecimal with v.divide(TEN).add(LOGTEN).
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    public static BigDecimal log10(BigDecimal v) {
        logger.debug(String.format("Input Value - %s",v));
        if (v.compareTo(MAXSIMPLE) > 0) {
            return v.divide(TEN).add(LOGTEN);
        }
        else {
            return BigDecimal.valueOf(Math.log10(v.doubleValue()));
        }
    }

    /**
     * Method which calculates logarithm base10 for BigInteger using BigDecimal.
     *
     * @param v value
     *
     * @return result of the logarithm calculation
     */
    public static BigInteger log10(BigInteger v) {
        return new BigInteger(Integer.toString(v.toString().length() - 1));
    }

    /**
     * Adds two big decimals
     *
     * @param val1 Left Operand
     * @param val2 Right Operand
     *
     * @return Result of the addition
     */
    public static BigDecimal sum(BigDecimal val1, BigDecimal val2) {
        return val1.add(val2);
    }

    /**
     * Subtracts two big decimals
     *
     * @param val1 Left Operand
     * @param val2 Right Operand
     *
     * @return Result of the subtraction
     */
    public static BigDecimal subtract(BigDecimal val1, BigDecimal val2) {
        return val1.subtract(val2);
    }

    /**
     * Divides two big decimals
     *
     * @param val1  Dividend
     * @param val2  Divisor
     * @param scale Scale for the division
     * @param mode  Rounding mode to use - { @link java.math.RoundingMode }
     *
     * @return Result of the division
     */
    public static BigDecimal divide(BigDecimal val1, BigDecimal val2, int scale, RoundingMode mode) {
        BigDecimal quota = val1.divide(val2, scale, mode);
        return rescale(quota, scale, mode);
    }

    /**
     * multiplies two big decimals
     *
     * @param val1 Left Operand
     * @param val2 Right Operand
     *
     * @return Result of the multiplication
     */
    public static BigDecimal multiply(BigDecimal val1, BigDecimal val2) {
        return val1.multiply(val2);
    }

    /**
     * Calculates a power of a big decimal
     *
     * @param base     base
     * @param exponent exponent
     *
     * @return Result of the power operation
     */
    public static BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        return base.pow(exponent.intValue());
    }

    /**
     * Calculates a power of a big decimal
     *
     * @param base     base
     * @param exponent exponent
     * @param mc       Math Context to use - { @link java.math.MathContext }
     *
     * @return Result of the power operation
     */
    public static BigDecimal pow(double base, BigDecimal exponent, MathContext mc) {//, int scale, RoundingMode mode){
        double dPow = Math.pow(base, exponent.doubleValue());
        return new BigDecimal(dPow, mc);
    }

    /**
     * Calculates a Exponential of a big decimal
     *
     * @param exponent exponent
     *
     * @return Result of the Exponential operation
     */
    public static BigDecimal exp(BigDecimal exponent) {
        return BigDecimal.valueOf(Math.pow(Math.E, exponent.doubleValue()));
    }

    /**
     * Applies a new scale to a big decimal
     *
     * @param n     Big decimal to apply the scale to
     * @param scale scale value
     * @param mode  Rounding mode to use - { @link java.math.RoundingMode }
     *
     * @return BigDecimal with the new scale
     */
    public static BigDecimal rescale(BigDecimal n, int scale, RoundingMode mode) {
        return (n.setScale(scale, mode));
    }
}
