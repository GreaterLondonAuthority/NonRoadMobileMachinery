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
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Exposes Utility methods for Thermodynamics calculations.
 * This was specifically created to expose methods for a Refrigeration system.
 * It will have calculations for Enthalpy, Entropy, ... [ADD Indication for all the available calculations]
 */
public class SmartsenseUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SmartsenseUtils.class);

    /**
     * Possible element states. Liquid or Vapor
     */
    public static enum ElementState {
        Liquid, Vapor
    }

    /**
     * Enumeration of the efficiency values for the compressor
     */
    public static enum ElementEfficiency {
        Motor(0.1), Mechanical(0.1), Indic(0.15), Heatloss(0.07);

        private double efficiency;

        public double getEfficiency() {
            return efficiency;
        }

        private ElementEfficiency(double efficiency) {
            this.efficiency = efficiency;
        }
    }

    /**
     * Enumeration with the available Refrigerant types ( R22, R407C, R410a ).
     * All the specific constants for each refrigerant are available in the the enum.
     *
     * @see <a href="https://pivotal.atlassian.net/wiki/pages/viewpage.action?spaceKey=SS&title=SmartSense">Smartsense Description</a>
     */
    public static enum RefrigerantType {
        R22, R407C, R410a, R134a, R417A, R438A;

        /**
         * Returns the constants required for saturation temperature calculations.
         * It has constants for both the liquid and vapor saturation. The choice on which to get comes from the given parameter
         *
         * @param elementState Element state (Liquid or Vapor) - @link ElementState
         *
         * @return The necessary constants to calculate the saturated liquid or vapor temperature
         */
        public Map<String, BigDecimal> getTemperatureSatConstansts(ElementState elementState) {

            Map<String, BigDecimal> res = new HashMap<>();
            switch (this) {
                case R22:
                    res.put("a1", new BigDecimal("14.4636291036899"));
                    res.put("a2", new BigDecimal("-2071.28282053782"));
                    res.put("a3", new BigDecimal("250.912637617554"));
                    break;
                case R407C:
                    res.put("a1", elementState == ElementState.Liquid ? new BigDecimal("14.5625001177579") : new BigDecimal("14.89479"));
                    res.put("a2", elementState == ElementState.Liquid ? new BigDecimal("-2077.38300847436") : new BigDecimal("-2181.30628"));
                    res.put("a3", elementState == ElementState.Liquid ? new BigDecimal("252.333881664967") : new BigDecimal("248.4435"));
                    break;
                case R410a:
                    res.put("a1", elementState == ElementState.Liquid ? new BigDecimal("14.7908328800257") : new BigDecimal("14.7774827974991"));
                    res.put("a2", elementState == ElementState.Liquid ? new BigDecimal("-2044.84836460141") : new BigDecimal("-2039.12046340924"));
                    res.put("a3", elementState == ElementState.Liquid ? new BigDecimal("252.344798586178") : new BigDecimal("251.969054071597"));
                    break;
                case R134a:
                    res.put("a1", new BigDecimal("14.577142913501"));
                    res.put("a2", new BigDecimal("-2165.22712697685"));
                    res.put("a3", new BigDecimal("243.230921541177"));
                    break;
                case R417A:
                    res.put("a1", elementState == ElementState.Liquid ? new BigDecimal("14.4550438806832") : new BigDecimal("14.4956132522849"));
                    res.put("a2", elementState == ElementState.Liquid ? new BigDecimal("-2081.5206818852") : new BigDecimal("-2038.60171135397"));
                    res.put("a3", elementState == ElementState.Liquid ? new BigDecimal("250.489317507094") : new BigDecimal("240.225384884734"));
                    break;
                case R438A:
                    res.put("a1", elementState == ElementState.Liquid ? new BigDecimal("14.4109673537775") : new BigDecimal("14.5965877082386"));
                    res.put("a2", elementState == ElementState.Liquid ? new BigDecimal("-2019.08729484226") : new BigDecimal("-2052.31546590627"));
                    res.put("a3", elementState == ElementState.Liquid ? new BigDecimal("248.331692134603") : new BigDecimal("241.53947863034"));
                    break;
            }
            return res;
        }

        /**
         * Returns the constants required for enthalpy calculations.
         * It has constants for both the liquid and vapor enthalpy. The choice on which to get comes from the given parameter
         *
         * @param elementState Element state (Liquid or Vapor) - @link ElementState
         *
         * @return The necessary constants to calculate the enthalpy on liquid or vapor
         */
        public Map<String, BigDecimal> getEnthalpyConstansts(ElementState elementState) {

            Map<String, BigDecimal> res = new HashMap<>();
            switch (this) {
                case R407C:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(200));
                        res.put("a2", BigDecimal.valueOf(1.40550));
                        res.put("a3", BigDecimal.valueOf(0.00188473));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.60587), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(414.760898331593));
                        res.put("a2", BigDecimal.valueOf(0.557485744415949));
                        res.put("a3", BigDecimal.valueOf(-0.0028695499953349));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.2516793392), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    }
                    break;
                case R410a:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(200.22026221321));
                        res.put("a2", BigDecimal.valueOf(1.57421840818231));
                        res.put("a3", BigDecimal.valueOf(0.00165638335236148));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.07799178081605), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.50929840716415), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.1032905044931), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(6.90364373767801), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(422.493317072957));
                        res.put("a2", BigDecimal.valueOf(0.280008539581349));
                        res.put("a3", BigDecimal.valueOf(-0.00297440514021662));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.25332806464717), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.0840708012068), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-3.82747876030056), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.01914423665586), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    break;
                case R22:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(199.909437381285));
                        res.put("a2", BigDecimal.valueOf(1.18817351214751));
                        res.put("a3", BigDecimal.valueOf(0.00170310870441469));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.03425424526091), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.38538237637785), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.14406440427397), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.3366221723008), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(405.187132750104));
                        res.put("a2", BigDecimal.valueOf(0.346684815521948));
                        res.put("a3", BigDecimal.valueOf(-0.00218460415432788));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(6.79296067994317), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.86501484413254), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-3.03523458655899), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-3.26332827317684), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    break;
                case R134a:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(199.77771319516));
                        res.put("a2", BigDecimal.valueOf(1.36139047774145));
                        res.put("a3", BigDecimal.valueOf(0.00213068220562354));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-6.51037746912179), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.09645388163342), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.73149258859739), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.48554699455676), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(399.140663906571));
                        res.put("a2", BigDecimal.valueOf(0.556056823637838));
                        res.put("a3", BigDecimal.valueOf(-0.00199606080590857));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(9.73207242406858), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(3.40417405554134), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.87807340760732), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-4.06135881327442), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    break;
                case R417A:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(199.98702840619));
                        res.put("a2", BigDecimal.valueOf(1.36358441637664));
                        res.put("a3", BigDecimal.valueOf(0.00184366435730339));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.36004347079917), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.36903121923529), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(4.3331704407564), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(3.79117618430742), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(370.838210597247));
                        res.put("a2", BigDecimal.valueOf(0.531687660599065));
                        res.put("a3", BigDecimal.valueOf(-0.00167582420565011));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.5164395842115), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.54443655873799), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.63690908488821), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-4.90785728566302), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    break;
                case R438A:
                    if (elementState == ElementState.Liquid) {
                        res.put("a1", BigDecimal.valueOf(200.027274813126));
                        res.put("a2", BigDecimal.valueOf(1.37327367115882));
                        res.put("a3", BigDecimal.valueOf(0.0016590463220354));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(5.74111408355271), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(3.30356031646969), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.53567887343553), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.70468812094544), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    else {
                        res.put("a1", BigDecimal.valueOf(381.129299466233));
                        res.put("a2", BigDecimal.valueOf(0.519751320045561));
                        res.put("a3", BigDecimal.valueOf(-0.00156482769464959));
                        res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.23043786207646), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                        res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.9948184256013), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                        res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-3.42769529585786), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-9), new MathContext(20))));
                        res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.30498277784995), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-11), new MathContext(20))));
                    }
                    break;
            }
            return res;
        }

        /**
         * Returns the constants required for enthalpy in superheat calculations.
         *
         * @return The necessary constants to calculate the enthalpy in superheat
         */
        public Map<String, BigDecimal> getSuperheatEnthalpyConstansts() {

            Map<String, BigDecimal> res = new HashMap<>();
            switch (this) {
                case R407C:
                    res.put("a1", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.91677108276923), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-3), new MathContext(20))));
                    res.put("a2", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.99839678144973), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a3", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(6.21487966054492), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.93667999932017), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.47838499513105), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.49353533067174), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-10), new MathContext(20))));
                    break;
                case R410a:
                    res.put("a1", BigDecimal.valueOf(423.32721913606));
                    res.put("a2", BigDecimal.valueOf(0.673694991803988));
                    res.put("a3", BigDecimal.valueOf(0.00984004151249526));
                    res.put("a4", BigDecimal.valueOf(-1.85530478497243));
                    res.put("a5", BigDecimal.valueOf(-0.00452722780699017));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.74432164229809), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                    res.put("a7", BigDecimal.valueOf(-0.000856731255843455));
                    res.put("a8", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.59199294856224), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a9", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-4.44868109073127), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a10", BigDecimal.valueOf(-0.00517096551777797));
                    break;
                case R22:
                    res.put("a1", BigDecimal.valueOf(660.972776992172));
                    res.put("a2", BigDecimal.valueOf(329.721192314922));
                    res.put("a3", BigDecimal.valueOf(476.345827462378));
                    res.put("a4", BigDecimal.valueOf(169.144908934477));
                    res.put("a5", BigDecimal.valueOf(497.164232271644));
                    break;
                case R134a:
                    res.put("a1", BigDecimal.valueOf(396.346782336104));
                    res.put("a2", BigDecimal.valueOf(3.55764629443453));
                    res.put("a3", BigDecimal.valueOf(-2.45150562050595));
                    res.put("a4", BigDecimal.valueOf(0.00640335502146483));
                    res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.07383790354203), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a6", BigDecimal.valueOf(-0.00769428842278624));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.24425031618032), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    break;
                case R417A:
                    res.put("a1", BigDecimal.valueOf(130.687516270565));
                    res.put("a2", BigDecimal.valueOf(568.982111146835));
                    res.put("a3", BigDecimal.valueOf(282.405131099392));
                    res.put("a4", BigDecimal.valueOf(-294.128494901518));
                    res.put("a5", BigDecimal.valueOf(140.342137832035));
                    break;
                case R438A:
                    res.put("a1", BigDecimal.valueOf(381.644382881473));
                    res.put("a2", BigDecimal.valueOf(2.81156693304374));
                    res.put("a3", BigDecimal.valueOf(-1.79825905040222));
                    res.put("a4", BigDecimal.valueOf(-0.00530554358638419));
                    res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.5677001124594), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a6", BigDecimal.valueOf(0.00479429928751899));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-8.52227825173769), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a8", BigDecimal.valueOf(-0.00621747132134811));
                    break;
            }
            return res;
        }

        /**
         * Returns the constants required for entropy in superheat calculations.
         *
         * @return The necessary constants to calculate the entropy in superheat
         */
        public Map<String, BigDecimal> getSuperheatEntropyConstansts() {

            Map<String, BigDecimal> res = new HashMap<>();
            switch (this) {
                case R407C:
                    res.put("a1", BigDecimal.valueOf(1.79162652111688));
                    res.put("a2", BigDecimal.valueOf(0.0037907474044221));
                    res.put("a3", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-6.07284565576814), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-4.32364573898471), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a5", BigDecimal.valueOf(0.0130028471688682));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.18859484544963), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a7", BigDecimal.valueOf(0.00263540407071766));
                    res.put("a8", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-3.52047472564001), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a9", BigDecimal.valueOf(0.00550916089153417));
                    break;
                case R410a:
                    res.put("a1", BigDecimal.valueOf(1.821042080036));
                    res.put("a2", BigDecimal.valueOf(0.00651947623769754));
                    res.put("a3", BigDecimal.valueOf(-0.00146401766406458));
                    res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-4.01938855793706), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a5", BigDecimal.valueOf(0.00163262181863572));
                    res.put("a6", BigDecimal.valueOf(0.000175175188118859));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-2.48064604734636), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    break;
                case R22:
                    res.put("a1", BigDecimal.valueOf(1.74205447111843));
                    res.put("a2", BigDecimal.valueOf(0.00288475402130497));
                    res.put("a3", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-6.11092227480972), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.25578063318701), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a5", BigDecimal.valueOf(-0.00137077870320787));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(8.88922516367549), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.3645829938642), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a8", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.41754558504799), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-10), new MathContext(20))));
                    res.put("a9", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.70472411052581), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-12), new MathContext(20))));
                    break;
                case R134a:
                    res.put("a1", BigDecimal.valueOf(1.72828657823017));
                    res.put("a2", BigDecimal.valueOf(0.0411885792026927));
                    res.put("a3", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(7.62488926815731), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a4", BigDecimal.valueOf(0.00974649135222149));
                    res.put("a5", BigDecimal.valueOf(-0.000138594181212413));
                    res.put("a6", BigDecimal.valueOf(0.0218362205022333));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(9.53598345760956), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a8", BigDecimal.valueOf(0.00584854787531734));
                    res.put("a9", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-7.97184063916368), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    break;
                case R417A:
                    res.put("a1", BigDecimal.valueOf(1.63164644100862));
                    res.put("a2", BigDecimal.valueOf(0.0642684592856046));
                    res.put("a3", BigDecimal.valueOf(0.00789118413773831));
                    res.put("a4", BigDecimal.valueOf(-0.000190688950552284));
                    res.put("a5", BigDecimal.valueOf(0.0371940234945991));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-6.16834449412183), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a7", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(9.10348237767386), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a8", BigDecimal.valueOf(0.00500519037544093));
                    res.put("a9", BigDecimal.valueOf(-0.000115408772097636));
                    break;
                case R438A:
                    res.put("a1", BigDecimal.valueOf(1.75780444298264));
                    res.put("a2", BigDecimal.valueOf(0.00362539848022433));
                    res.put("a3", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.27123449044086), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-5), new MathContext(20))));
                    res.put("a4", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.04020631420628), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-7), new MathContext(20))));
                    res.put("a5", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.20023621183709), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-10), new MathContext(20))));
                    res.put("a6", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(1.05950119448249), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-12), new MathContext(20))));
                    res.put("a7", BigDecimal.valueOf(-0.000556287056037614));
                    res.put("a8", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(8.16972050960633), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-6), new MathContext(20))));
                    res.put("a9", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-5.96439321515583), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-8), new MathContext(20))));
                    res.put("a10", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(2.12567894086469), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-10), new MathContext(20))));
                    res.put("a11", com.pivotal.utils.ArithmeticUtils.multiply(BigDecimal.valueOf(-1.972247314911), com.pivotal.utils.ArithmeticUtils.pow(10, BigDecimal.valueOf(-12), new MathContext(20))));
                    break;
            }
            return res;
        }

        /**
         * Returns the constants required for the enthalpy from entropy calculations
         *
         * @return The necessary constants to calculate the enthalpy from entropy
         */
        public Map<String, BigDecimal> getSuperheatEnthalpyFromEntropyConstansts() {

            Map<String, BigDecimal> res = new HashMap<>();
            switch (this) {
                case R407C:
                    res.put("a1", BigDecimal.valueOf(204.090994674653));
                    res.put("a2", BigDecimal.valueOf(23.4662466339122));
                    res.put("a3", BigDecimal.valueOf(0.11361617200666));
                    res.put("a4", BigDecimal.valueOf(2.54294676703401));
                    break;
                case R410a:
                    res.put("a1", BigDecimal.valueOf(224.211920197762));
                    res.put("a2", BigDecimal.valueOf(16.3071186944362));
                    res.put("a3", BigDecimal.valueOf(0.13587647814201));
                    res.put("a4", BigDecimal.valueOf(2.64804395157217));
                    break;
                case R22:
                    res.put("a1", BigDecimal.valueOf(5932926.45715449));
                    res.put("a2", BigDecimal.valueOf(-10196076.9428671));
                    res.put("a3", BigDecimal.valueOf(1285764.44318034));
                    res.put("a4", BigDecimal.valueOf(-69489.3624743342));
                    res.put("a5", BigDecimal.valueOf(26724260.4447282));
                    res.put("a6", BigDecimal.valueOf(-31426.0340910902));
                    res.put("a7", BigDecimal.valueOf(3198.7605880901));
                    res.put("a8", BigDecimal.valueOf(-149.440334542599));
                    res.put("a9", BigDecimal.valueOf(158964.804806478));
                    res.put("a10", BigDecimal.valueOf(-37676.8306714738));
                    break;
                case R134a:
                    res.put("a1", BigDecimal.valueOf(51.1075603322566));
                    res.put("a2", BigDecimal.valueOf(-4.58926992304856));
                    res.put("a3", BigDecimal.valueOf(0.0959892564858774));
                    res.put("a4", BigDecimal.valueOf(111.141153511452));
                    res.put("a5", BigDecimal.valueOf(-0.0537223357642793));
                    res.put("a6", BigDecimal.valueOf(0.00142744129898492));
                    res.put("a7", BigDecimal.valueOf(-0.162852210068193));
                    res.put("a8", BigDecimal.valueOf(0.0727000654796499));
                    res.put("a9", BigDecimal.valueOf(-0.0238258152601469));
                    break;
                case R417A:
                    res.put("a1", BigDecimal.valueOf(-1496.23989512919));
                    res.put("a2", BigDecimal.valueOf(-51.1453388226719));
                    res.put("a3", BigDecimal.valueOf(0.781737273906807));
                    res.put("a4", BigDecimal.valueOf(-0.817930099402256));
                    res.put("a5", BigDecimal.valueOf(2916.72215738443));
                    res.put("a6", BigDecimal.valueOf(-0.751502017145999));
                    res.put("a7", BigDecimal.valueOf(11.0436068691294));
                    res.put("a8", BigDecimal.valueOf(-2.26095376301941));
                    res.put("a9", BigDecimal.valueOf(-0.210252520163665));
                    break;
                case R438A:
                    res.put("a1", BigDecimal.valueOf(169.146405115881));
                    res.put("a2", BigDecimal.valueOf(3.3968367661523));
                    res.put("a3", BigDecimal.valueOf(0.579515868624095));
                    res.put("a4", BigDecimal.valueOf(-0.0572450096551183));
                    res.put("a5", BigDecimal.valueOf(8.68541565253575));
                    res.put("a6", BigDecimal.valueOf(-0.0098555546009738));
                    res.put("a7", BigDecimal.valueOf(-0.000710292259628238));
                    res.put("a8", BigDecimal.valueOf(-0.671170416584113));
                    break;
            }
            return res;
        }

    }

    /**
     * Generic constants for the calculations
     */
    public static enum Constants {
        SHR(0.95), AirDensity(1.2), SpecificHeat(1.01);

        private double value;

        public double getValue() {
            return value;
        }

        private Constants(double value) {
            this.value = value;
        }
    }

    /**
     * Get the Refrigerant Type by name. The search is case insensitive
     *
     * @param refrigerantName refrigerant code. r22,r407c or r410a.
     *
     * @return The matching refrigerant type or null if no matching code is found.
     */
    public static RefrigerantType getRefrigerantByName(String refrigerantName) {
        RefrigerantType res = null;
        if (refrigerantName != null) {
            if (refrigerantName.equalsIgnoreCase("r22"))
                res = RefrigerantType.R22;
            else if (refrigerantName.equalsIgnoreCase("r407c"))
                res = RefrigerantType.R407C;
            else if (refrigerantName.equalsIgnoreCase("r410a"))
                res = RefrigerantType.R410a;
            else if (refrigerantName.equalsIgnoreCase("r134a"))
                res = RefrigerantType.R134a;
            else if (refrigerantName.equalsIgnoreCase("r417a"))
                res = RefrigerantType.R417A;
            else if (refrigerantName.equalsIgnoreCase("r438a"))
                res = RefrigerantType.R438A;
        }
        return res;
    }

    /**
     * Get the Element State Type by name. The search is case insensitive
     *
     * @param stateName State code. Liquid or Vapor
     *
     * @return Element state or null if no matching code is found.
     */
    @SuppressWarnings("unused")
    public static ElementState getStateByName(String stateName) {
        ElementState res = null;
        if (stateName != null) {
            if (stateName.equalsIgnoreCase("liquid"))
                res = ElementState.Liquid;
            else if (stateName.equalsIgnoreCase("vapor"))
                res = ElementState.Vapor;
        }
        return res;
    }


    /**
     * Calculates the Cycle Coefficient of Performance.
     *
     * @param coolingCapacity Cooling capacity
     * @param compressorPower Compressor power reading
     *
     * @return value of the Coefficient of Performance
     */
    @SuppressWarnings("unused")
    public static double calculateCycleCOP(double coolingCapacity, double compressorPower) {
        return (coolingCapacity / compressorPower);
    }

    /**
     * Calculates the Evaporator Temperature Delta
     *
     * @param tempAirOn  Reading for the Air On Temperature sensor
     * @param tempAirOff Reading for the Air Off Temperature sensor
     *
     * @return The evaporator Temperature Delta in degrees Celsius
     */
    @SuppressWarnings("unused")
    public static double calculateEvaportorDt(double tempAirOn, double tempAirOff) {
        return tempAirOn - tempAirOff;
    }

    /**
     * Calculates the air flow of the refrigeration System
     *
     * @param coolingCapacity Cooling capacity
     * @param deltaT          Evaporator Temperature Delta
     *
     * @return Air flow value
     */
    @SuppressWarnings("unused")
    public static double calculateAirFlow(double coolingCapacity, double deltaT) {
        double shr = Constants.SHR.getValue(),
                p = Constants.AirDensity.getValue(),
                cp = Constants.SpecificHeat.getValue();
        //return value as m3/hr - protect against 0 division when deltaT is 0
        return deltaT==0 ? 0 : ((coolingCapacity * shr) / (p * cp * deltaT) * 60 * 60);
    }

    /**
     * Calculates the Cooling Capacity.
     * Cooling capacity is the measure of a cooling system's ability to remove heat.
     *
     * @param massFlowRate   Mass Flow rate
     * @param enthalpyPoint2 Enthalpy in point 2
     * @param enthalpyPoint4 Enthalpy in point 4
     *
     * @return Cooling Capacity
     */
    public static double calculateCoolingCapacity(double massFlowRate, double enthalpyPoint2, double enthalpyPoint4) {
        return Math.round(massFlowRate * (enthalpyPoint2 - enthalpyPoint4));
    }

    /**
     * Calculates the unit's coefficient of performance.
     * This is the ratio of heat extraction to energy input.
     *
     * @param coolingCapacity Cooling capacity
     * @param totalPower      Total power used in the chiller
     *
     * @return Unit COP value
     */
    @SuppressWarnings("unused")
    public static double calculateUnitCOP(double coolingCapacity, double totalPower) {
        return coolingCapacity / totalPower;
    }

    /**
     * Calculates the Mass flow Rate
     *
     * @param inputPower     Total power reading
     * @param enthalpyPoint3 Enthalpy in point 3
     * @param enthalpyPoint2 Enthalpy in point 2
     *
     * @return Mass Flow Rate
     */
    public static double calculateMassFlowRate(double inputPower, double enthalpyPoint3, double enthalpyPoint2) {
        double motorEff = inputPower * ElementEfficiency.Motor.getEfficiency(),
                mechEff = (inputPower - motorEff) * ElementEfficiency.Mechanical.getEfficiency(),
                indEff = (inputPower - motorEff - mechEff) * ElementEfficiency.Indic.getEfficiency(),
                heatLoss = (inputPower - motorEff - mechEff - indEff) * ElementEfficiency.Heatloss.getEfficiency();

        if ((enthalpyPoint3 - enthalpyPoint2) < 7) {
            return 0;
        }
        else {
            return (inputPower - motorEff - mechEff - indEff - heatLoss) / (enthalpyPoint3 - enthalpyPoint2);
        }
    }


    /**
     * Compressor Coefficient
     *
     * @param enthalpy3       Enthalpy in point 3
     * @param enthalpy2       Enthalpy in point 2
     * @param massFlowRate    Mass Flow rate value
     * @param compressorPower Compressor power reading
     *
     * @return Compressor Coefficient
     */
    public static double calculateCompressorCoefficient(double enthalpy3, double enthalpy2, double massFlowRate, double compressorPower) {
        return (((enthalpy3 - enthalpy2) * massFlowRate) / compressorPower * 100);
    }


    /**
     * Converts a value in gauge pressure (BarG) to absolute pressure (kPa)
     *
     * @param gaugePressureVal Gauge pressure value in BarG
     *
     * @return A Conversion of the given value to absolute pressure in kPa
     */
    public static double convertGaugePresureToAbsolute(double gaugePressureVal) {
        double res = gaugePressureVal * 100 + 101.325;
        logger.debug("Converted %f BarG to %f KPa", gaugePressureVal, res);
        return res;
    }

    /**
     * Finds the temperature from a given pressure for a specific refrigeration type and elementState.
     * It will use Antoine Equation to estimate the temperature - T = ( B / ( A - log(P) ) ) - C. Refer to http://en.wikipedia.org/wiki/Antoine_equation#Sources_for_Antoine_equation_parameters
     *
     * @param pressureVal     Pressure val in absolute units (PSIG)
     * @param refrigerantType Type of refrigerant. The required constants will be associated with it
     * @param state           Element state (Liquid or Vapor)
     *
     * @return The estimated temperature for the given pressure
     */
    public static BigDecimal getTemperatureFromPressure(double pressureVal, RefrigerantType refrigerantType, ElementState state) {
        double convertedPressure = SmartsenseUtils.convertGaugePresureToAbsolute(pressureVal);
        Map<String, BigDecimal> constants = refrigerantType.getTemperatureSatConstansts(state);
        BigDecimal divisor = com.pivotal.utils.ArithmeticUtils.log(BigDecimal.valueOf(convertedPressure)).subtract(constants.get("a1"));
        return constants.get("a2").divide(divisor, 10, BigDecimal.ROUND_HALF_EVEN).subtract(constants.get("a3"));
    }

    /**
     * Calculates enthalpy in a given state  for a given temperature using a specific refrigerant
     *
     * @param temperature     Temperature to get the enthalpy from
     * @param refrigerantType Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     * @param state           Element state (Liquid or Vapor) - @link ElementState
     *
     * @return Enthalpy value
     */
    public static BigDecimal calculateEnthalpy(double temperature, RefrigerantType refrigerantType, ElementState state) {
        Map<String, BigDecimal> constants = refrigerantType.getEnthalpyConstansts(state);
        BigDecimal bigTemperature = BigDecimal.valueOf(temperature);

        BigDecimal tmp = constants.get("a1");
        tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), bigTemperature));
        tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(bigTemperature, BigDecimal.valueOf(2))));
        tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(bigTemperature, BigDecimal.valueOf(3))));
        // Some of the refrigerant may include 3 more constants for the enthalpy calculation.
        if (constants.containsKey("a5") && constants.containsKey("a6") && constants.containsKey("a7")) {
            tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), com.pivotal.utils.ArithmeticUtils.pow(bigTemperature, BigDecimal.valueOf(4))));
            tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(bigTemperature, BigDecimal.valueOf(5))));
            tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(bigTemperature, BigDecimal.valueOf(6))));
        }
        return tmp;
    }


    /**
     * Calculates enthalpy in superheat for a given temperature using a specific refrigerant
     *
     * @param satTemperature            Saturation temperature
     * @param superHeatSteamTemperature Superheated steam temperature
     * @param refrigerantType           Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Enthalpy in superheat value
     */
    public static BigDecimal calculateSuperHeatEnthalpy(double satTemperature, double superHeatSteamTemperature, RefrigerantType refrigerantType) {
        BigDecimal res;
        Map<String, BigDecimal> constants = refrigerantType.getSuperheatEnthalpyConstansts();
        BigDecimal bigSatTemperature = BigDecimal.valueOf(satTemperature);
        BigDecimal bigHeatSteamTemperature = BigDecimal.valueOf(superHeatSteamTemperature);
        BigDecimal superheatDelta = bigHeatSteamTemperature.subtract(bigSatTemperature);
        BigDecimal tmp = null;
        switch (refrigerantType) {
            case R407C:
                BigDecimal vapEnt = SmartsenseUtils.calculateEnthalpy(satTemperature, refrigerantType, ElementState.Vapor);
                tmp = BigDecimal.valueOf(1);
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a1"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), superheatDelta), BigDecimal.valueOf(satTemperature)));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                tmp = tmp.multiply(vapEnt);
                break;
            case R410a:
                BigDecimal den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), superheatDelta));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(3))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a10"), bigSatTemperature));

                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(3))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R22:
                tmp = BigDecimal.valueOf(1);
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.pow(superheatDelta.subtract(constants.get("a2")).divide(constants.get("a3"), 20, BigDecimal.ROUND_HALF_EVEN), BigDecimal.valueOf(2)));
                tmp = com.pivotal.utils.ArithmeticUtils.multiply(
                        tmp,
                        BigDecimal.valueOf(1).add(
                                com.pivotal.utils.ArithmeticUtils.pow(
                                        bigSatTemperature.subtract(constants.get("a4")).divide(
                                                constants.get("a5"),
                                                20,
                                                BigDecimal.ROUND_HALF_EVEN),
                                        BigDecimal.valueOf(2)
                                )
                        )
                );
                tmp = constants.get("a1").divide(tmp, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R134a:
                BigDecimal top = constants.get("a1");
                top = top.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                top = top.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), bigSatTemperature));

                BigDecimal bottom = BigDecimal.valueOf(1);
                bottom = bottom.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), superheatDelta));
                bottom = bottom.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                bottom = bottom.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), bigSatTemperature));
                bottom = bottom.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));

                tmp = top.divide(bottom, 20, BigDecimal.ROUND_HALF_EVEN);
                break;

            case R417A:
                BigDecimal leftSide = superheatDelta.subtract(constants.get("a3"));
                leftSide = leftSide.divide(constants.get("a4"), 20, BigDecimal.ROUND_HALF_EVEN);
                leftSide = leftSide.pow(2);
                leftSide = leftSide.add(BigDecimal.valueOf(1));

                BigDecimal rightSide = bigSatTemperature.subtract(constants.get("a5"));
                rightSide = rightSide.divide(constants.get("a4"), 20, BigDecimal.ROUND_HALF_EVEN);
                rightSide = rightSide.pow(2);
                rightSide = rightSide.add(BigDecimal.valueOf(1));

                bottom = leftSide.multiply(rightSide);

                tmp = constants.get("a1");
                tmp = tmp.add(constants.get("a2").divide(bottom));
                break;
            case R438A:
                top = constants.get("a1");
                top = top.add(constants.get("a2").multiply(superheatDelta));
                top = top.add(constants.get("a3").multiply(bigSatTemperature));
                top = top.add(constants.get("a4").multiply(bigSatTemperature.pow(2)));
                top = top.add(constants.get("a5").multiply(bigSatTemperature.pow(3)));

                bottom = BigDecimal.valueOf(1);
                bottom = bottom.add(constants.get("a6").multiply(superheatDelta));
                bottom = bottom.add(constants.get("a7").multiply(superheatDelta.pow(2)));
                bottom = bottom.add(constants.get("a8").multiply(bigSatTemperature));

                tmp = top.divide(bottom, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
        }
        res = tmp;
        return res;
    }


    /**
     * Calculated Entropy in Super Heat
     *
     * @param satTemperature            Saturation temperature
     * @param superHeatSteamTemperature Steam temperature reading
     * @param refrigerantType           Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Entropy in superheat value
     */
    public static BigDecimal calculateSuperHeatEntropy(double satTemperature, double superHeatSteamTemperature, RefrigerantType refrigerantType) {
        Map<String, BigDecimal> constants = refrigerantType.getSuperheatEntropyConstansts();
        BigDecimal bigSatTemperature = BigDecimal.valueOf(satTemperature);
        BigDecimal bigHeatSteamTemperature = BigDecimal.valueOf(superHeatSteamTemperature);
        BigDecimal superheatDelta = bigHeatSteamTemperature.subtract(bigSatTemperature);
        BigDecimal tmp = null;
        BigDecimal den;
        switch (refrigerantType) {
            case R407C:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(3))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), bigSatTemperature));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), superheatDelta));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R410a:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), superheatDelta));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), bigSatTemperature));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R22:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(3))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(3))));
                // This doesn't match the old PHP equations (it's subtracted there), but does match the equations from JianMei. See SS-203
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(4))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(5))));
                break;
            case R134a:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));

                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), superheatDelta));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), bigSatTemperature));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R417A:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), superheatDelta));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), bigSatTemperature));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));

                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), superheatDelta));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(superheatDelta, BigDecimal.valueOf(3))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), bigSatTemperature));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(bigSatTemperature, BigDecimal.valueOf(2))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R438A:
                tmp = constants.get("a1");
                tmp = tmp.add(constants.get("a2").multiply(superheatDelta));
                tmp = tmp.add(constants.get("a3").multiply(superheatDelta.pow(2)));
                tmp = tmp.add(constants.get("a4").multiply(superheatDelta.pow(3)));
                tmp = tmp.add(constants.get("a5").multiply(superheatDelta.pow(4)));
                tmp = tmp.add(constants.get("a6").multiply(superheatDelta.pow(5)));
                tmp = tmp.add(constants.get("a7").multiply(bigSatTemperature));
                tmp = tmp.add(constants.get("a8").multiply(bigSatTemperature).pow(2));
                tmp = tmp.add(constants.get("a9").multiply(bigSatTemperature).pow(3));
                tmp = tmp.add(constants.get("a10").multiply(bigSatTemperature).pow(4));
                tmp = tmp.add(constants.get("a11").multiply(bigSatTemperature).pow(5));
                break;
        }
        return tmp;
    }


    /**
     * Calculates Enthalpy in Superheat from discharge pressure and entropy at suction
     *
     * @param pressure        Discharge Pressure reading
     * @param entropy         Entropy value at suction
     * @param refrigerantType Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Enthalpy in superheat
     */
    public static BigDecimal calculateSuperHeatEnthalpyFromEntropy(double pressure, double entropy, RefrigerantType refrigerantType) {
        Map<String, BigDecimal> constants = refrigerantType.getSuperheatEnthalpyFromEntropyConstansts();
        BigDecimal tmp = null;
        BigDecimal den;
        BigDecimal logP = com.pivotal.utils.ArithmeticUtils.log(BigDecimal.valueOf(convertGaugePresureToAbsolute(pressure)));
        BigDecimal bigEntropy = BigDecimal.valueOf(entropy);
        switch (refrigerantType) {
            case R407C:
            case R410a:
                tmp = constants.get("a2");
                //Convert to absolute pressure
                tmp = com.pivotal.utils.ArithmeticUtils.multiply(tmp, com.pivotal.utils.ArithmeticUtils.pow(convertGaugePresureToAbsolute(pressure), constants.get("a3"), new MathContext(20)));
                tmp = tmp.multiply(com.pivotal.utils.ArithmeticUtils.pow(entropy, constants.get("a4"), new MathContext(20)));
                tmp = tmp.add(constants.get("a1"));
                break;
            case R22:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), logP));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(3))));
                tmp = tmp.subtract(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), bigEntropy));

                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), logP));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(3))));
                // This doesn't match the old PHP equations (it's subtracted there), but does match the equations from JianMei. See SS-203
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), bigEntropy));
                // This doesn't match the old PHP equations (it's subtracted there), but does match the equations from JianMei. See SS-203
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a10"), com.pivotal.utils.ArithmeticUtils.pow(bigEntropy, BigDecimal.valueOf(2))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;

            case R134a:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), logP));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), bigEntropy));

                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), logP));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), bigEntropy));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(bigEntropy, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(bigEntropy, BigDecimal.valueOf(3))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R417A:
                tmp = constants.get("a1");
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a2"), logP));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a3"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(2))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a4"), com.pivotal.utils.ArithmeticUtils.pow(logP, BigDecimal.valueOf(3))));
                tmp = tmp.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a5"), bigEntropy));

                den = BigDecimal.valueOf(1);
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a6"), logP));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a7"), bigEntropy));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a8"), com.pivotal.utils.ArithmeticUtils.pow(bigEntropy, BigDecimal.valueOf(2))));
                den = den.add(com.pivotal.utils.ArithmeticUtils.multiply(constants.get("a9"), com.pivotal.utils.ArithmeticUtils.pow(bigEntropy, BigDecimal.valueOf(3))));
                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
            case R438A:
                tmp = constants.get("a1");
                tmp = tmp.add(constants.get("a2").multiply(logP));
                tmp = tmp.add(constants.get("a3").multiply(logP.pow(2)));
                tmp = tmp.add(constants.get("a4").multiply(logP.pow(3)));
                tmp = tmp.add(constants.get("a5").multiply(com.pivotal.utils.ArithmeticUtils.log(bigEntropy)));

                den = BigDecimal.valueOf(1);
                den = den.add(constants.get("a6").multiply(logP));
                den = den.add(constants.get("a7").multiply(logP.pow(2)));
                den = den.add(constants.get("a8").multiply(com.pivotal.utils.ArithmeticUtils.log(bigEntropy)));

                tmp = tmp.divide(den, 20, BigDecimal.ROUND_HALF_EVEN);
                break;
        }
        return tmp;
    }


    /**
     * Convenience method to calculate the Flow rate from the required sensor readings
     *
     * @param suctionPressure      Suction Pressure reading
     * @param suctionTemperature   Suction Temperature reading
     * @param dischargePressure    Discharge Pressure reading
     * @param dischargeTemperature Discharge Temperature reading
     * @param compressorPower      Compressor power reading
     * @param refrigerantType      Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Flow Rate Value
     */
    public static double getSmartSenseFlowRateFromReadings(double suctionPressure,
                                                           double suctionTemperature,
                                                           double dischargePressure,
                                                           double dischargeTemperature,
                                                           double compressorPower,
                                                           RefrigerantType refrigerantType
    ) {
        BigDecimal suctionTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(suctionPressure, refrigerantType, ElementState.Vapor);
        BigDecimal enthalpy2 = SmartsenseUtils.calculateSuperHeatEnthalpy(suctionTempFromPressure.doubleValue(), suctionTemperature, refrigerantType);

        BigDecimal dischargeTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(dischargePressure, refrigerantType, ElementState.Vapor);
        BigDecimal enthalpy3 = SmartsenseUtils.calculateSuperHeatEnthalpy(dischargeTempFromPressure.doubleValue(), dischargeTemperature, refrigerantType);

        return SmartsenseUtils.calculateMassFlowRate(compressorPower, enthalpy3.doubleValue(), enthalpy2.doubleValue());
    }


    /**
     * Convenience method to calculate the Cooling Load from the required sensor readings
     *
     * @param suctionPressure      Suction Pressure reading
     * @param suctionTemperature   Suction Temperature reading
     * @param condenserTemperature Condenser Temperature reading
     * @param massFlowRate         Mass Flow rate value
     * @param refrigerantType      Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Cooling Load for a circuit
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseCoolingLoadFromReadings(double suctionPressure,
                                                              double suctionTemperature,
                                                              double condenserTemperature,
                                                              double massFlowRate,
                                                              RefrigerantType refrigerantType
    ) {

        BigDecimal suctionTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(suctionPressure, refrigerantType, ElementState.Vapor);
        BigDecimal enthalpy2 = SmartsenseUtils.calculateSuperHeatEnthalpy(suctionTempFromPressure.doubleValue(), suctionTemperature, refrigerantType);

        BigDecimal enthalpy4 = SmartsenseUtils.calculateEnthalpy(condenserTemperature, refrigerantType, ElementState.Liquid);

        return SmartsenseUtils.calculateCoolingCapacity(massFlowRate, enthalpy2.doubleValue(), enthalpy4.doubleValue());

    }

    /**
     * Convenience method to calculate the Cycle Coefficient of performance
     *
     * @param coolingCapacity Cooling capacity of the circuit
     * @param compressorPower Compressor power reading
     *
     * @return Cycle coefficient of performance
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseCycleCOPFromReadings(double coolingCapacity,
                                                           double compressorPower
    ) {

        return SmartsenseUtils.calculateCycleCOP(coolingCapacity, compressorPower);
    }

    /**
     * Convenience method to calculate the Compressor Coefficient from the required sensor readings
     *
     * @param suctionPressure    Suction Pressure reading
     * @param suctionTemperature Suction Temperature reading
     * @param dischargePressure  Discharge Pressure reading
     * @param massFlowRate       Mass Flow rate value
     * @param compressorPower    Compressor power reading
     * @param refrigerantType    Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Compressor Coefficient value
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseCompressorCoefficientFromReadings(double suctionPressure,
                                                                        double suctionTemperature,
                                                                        double dischargePressure,
                                                                        double massFlowRate,
                                                                        double compressorPower,
                                                                        RefrigerantType refrigerantType
    ) {

        BigDecimal suctionTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(suctionPressure, refrigerantType, ElementState.Vapor);

        BigDecimal superHeatEntropy = SmartsenseUtils.calculateSuperHeatEntropy(suctionTempFromPressure.doubleValue(), suctionTemperature, refrigerantType);
        BigDecimal enthalpySuperheat = SmartsenseUtils.calculateSuperHeatEnthalpyFromEntropy(dischargePressure, superHeatEntropy.doubleValue(), refrigerantType);
        BigDecimal enthalpy2 = SmartsenseUtils.calculateSuperHeatEnthalpy(suctionTempFromPressure.doubleValue(), suctionTemperature, refrigerantType);

        return SmartsenseUtils.calculateCompressorCoefficient(enthalpySuperheat.doubleValue(), enthalpy2.doubleValue(), massFlowRate, compressorPower);
    }


    /**
     * Convenience method to calculate the Subcool Enthalpy value from the required sensor readings
     *
     * @param condenserTemperature Condenser Temperature reading
     * @param refrigerantType      Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Subcool Enthalpy value
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseSubCoolEnthalpy(double condenserTemperature,
                                                      RefrigerantType refrigerantType
    ) {
        return SmartsenseUtils.calculateEnthalpy(condenserTemperature, refrigerantType, ElementState.Liquid).doubleValue();
    }

    /**
     * Convenience method to calculate the Superheat Enthalpy value from the required sensor readings
     *
     * @param temperature     Temperature reading
     * @param pressure        Pressure reading
     * @param refrigerantType Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Superheat Enthalpy value
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseSuperHeatEnthalpy(double temperature,
                                                        double pressure,
                                                        RefrigerantType refrigerantType
    ) {

        BigDecimal suctionTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(pressure, refrigerantType, ElementState.Vapor);
        return SmartsenseUtils.calculateSuperHeatEnthalpy(suctionTempFromPressure.doubleValue(), temperature, refrigerantType).doubleValue();
    }

    /**
     * Convenience method to calculate the Subcooling value from the required sensor readings
     *
     * @param dischargePressure    Discharge Pressure reading
     * @param condenserTemperature Condenser Temperature reading
     * @param refrigerantType      Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Subcooling value
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseSubCooling(double dischargePressure,
                                                 double condenserTemperature,
                                                 RefrigerantType refrigerantType
    ) {

        BigDecimal tempFromPressure = SmartsenseUtils.getTemperatureFromPressure(dischargePressure, refrigerantType, ElementState.Liquid);
        return tempFromPressure.doubleValue() - condenserTemperature;
    }


    /**
     * Convenience method to calculate the Superheat value from the required sensor readings
     *
     * @param suctionPressure    Discharge Pressure reading
     * @param suctionTemperature Condenser Temperature reading
     * @param refrigerantType    Type of refrigerant. The required constants will be associated with it - @link RefrigerantType
     *
     * @return Superheat value
     */
    @SuppressWarnings("unused")
    public static double getSmartSenseSuperheat(double suctionPressure,
                                                double suctionTemperature,
                                                RefrigerantType refrigerantType
    ) {

        BigDecimal suctionTempFromPressure = SmartsenseUtils.getTemperatureFromPressure(suctionPressure, refrigerantType, ElementState.Vapor);
        return suctionTemperature - suctionTempFromPressure.doubleValue();
    }


    public static void main(String[] args) {
//
//        double t1 = SmartsenseUtils.getTemperatureFromPressure(4.9, RefrigerantType.R407C, ElementState.Vapor);
//        System.out.println("AKI 1 - " + t1);
//
//        System.out.println("AKI Enth - " + SmartsenseUtils.calculateSuperHeatEnthalpy(t1,23.6, RefrigerantType.R407C));
//
//
//        double t2 = SmartsenseUtils.getTemperatureFromPressure(21.8, RefrigerantType.R407C, ElementState.Vapor);
//        System.out.println("AKI 12 - " + t2);
//
//        System.out.println("AKI Enth2 - " + SmartsenseUtils.calculateSuperHeatEnthalpy(t2,86.1, RefrigerantType.R407C));
//
//        System.out.println("AKI Res - " +
//                SmartsenseUtils.calculateMassFlowRate(8.3,
//                        SmartsenseUtils.calculateSuperHeatEnthalpy(t2,86.1, RefrigerantType.R407C),
//                        SmartsenseUtils.calculateSuperHeatEnthalpy(t1,23.6, RefrigerantType.R407C))
//                );


        //
//
//        System.out.println("AKI - " + SmartsenseUtils.getTemperatureFromPressure(4860, RefrigerantType.R407C, ElementState.Liquid));
//        System.out.println("AKI - " + SmartsenseUtils.getTemperatureFromPressure(4860, RefrigerantType.R407C, ElementState.Vapor));
//
//        System.out.println("AKI - " + SmartsenseUtils.getTemperatureFromPressure(4860, RefrigerantType.R410a, ElementState.Liquid));
//        System.out.println("AKI - " + SmartsenseUtils.getTemperatureFromPressure(4860, RefrigerantType.R410a, ElementState.Vapor));

//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R407C, ElementState.Liquid));
//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R410a, ElementState.Liquid));
//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R22, ElementState.Liquid));
//
//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R407C, ElementState.Vapor));
//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R410a, ElementState.Vapor));
//        System.out.println("AKI - " + SmartsenseUtils.calculateEnthalpy(41, RefrigerantType.R22, ElementState.Vapor));
//

//        System.out.println("AKI - " + SmartsenseUtils.calculateSuperHeatEnthalpy(290,230, RefrigerantType.R407C));
//        System.out.println("AKI - " + SmartsenseUtils.calculateSuperHeatEnthalpy(290,230, RefrigerantType.R410a));
//        System.out.println("AKI - " + SmartsenseUtils.calculateSuperHeatEnthalpy(290,230, RefrigerantType.R22));
//
//        System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEntropy(290, 230, RefrigerantType.R407C));
//                System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEntropy(290, 230, RefrigerantType.R410a));
//                System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEntropy(290, 230, RefrigerantType.R22));

//                System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEnthalpyFromEntropy(100, 1000, RefrigerantType.R407C));
//                        System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEnthalpyFromEntropy(100, 1000, RefrigerantType.R410a));
//                        System.out.println("AKI r - " + SmartsenseUtils.calculateSuperHeatEnthalpyFromEntropy(100, 1000, RefrigerantType.R22));
//
//
//        double result = 5932926.45715449 - (10196076.9428671 * Math.log(convertGaugePresureToAbsolute(100))) + (1285764.44318034 * Math.pow(Math.log(convertGaugePresureToAbsolute(100)), 2));
//                                            result -= (69489.3624743342 * Math.pow(Math.log(convertGaugePresureToAbsolute(100)), 3)) + (26724260.4447282 * 1000);
//                                            double den = 1 - (31426.0340910902 * Math.log(convertGaugePresureToAbsolute(100))) + (3198.7605880901 * Math.pow(Math.log(convertGaugePresureToAbsolute(100)), 2));
//                                            den -= (149.440334542599 * Math.pow(Math.log(convertGaugePresureToAbsolute(100)), 3)) + (158964.804806478 * 1000) - (37676.8306714738 * Math.pow(1000, 2));
//                                            result /= den;
//
//        System.out.println("Teste - " + result);

        double result = 200 + (1.4055 * 23.6) + (0.00188473 * Math.pow(23.6, 2)) + (0.0000160587 * Math.pow(23.6, 3));
//double result = 200.22026221321 + (1.57421840818231 * 23.6) + (0.00165638335236148 * Math.pow(23.6,2));
//        result -= ((2.07799178081605*Math.pow(10,-5)) * Math.pow(23.6,3));
//        result += ((2.50929840716415*Math.pow(10,-7)) * Math.pow(23.6,4));
//        result += ((1.1032905044931*Math.pow(10,-8)) * Math.pow(23.6,5));
//        result += ((6.90364373767801*Math.pow(10,-11)) * Math.pow(23.6,6));

        System.out.println("Teste - " + result);


        double vaporVal = (-2077.38300847436 / (Math.log(SmartsenseUtils.convertGaugePresureToAbsolute(4.9)) - 14.5625001177579)) - 252.3338847436;
        double liqVal = (-2181.30628 / (Math.log(SmartsenseUtils.convertGaugePresureToAbsolute(4.9)) - 14.89479)) - 248.4435;
        System.out.println("Teste2 - " + vaporVal);
        System.out.println("Teste3 - " + liqVal);


    }


}
