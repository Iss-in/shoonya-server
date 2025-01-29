package com.shoonya.trade_server.lib;

import java.util.*;
import java.math.*;

import lombok.Getter;
import org.apache.commons.math3.distribution.NormalDistribution;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.stereotype.Component;

@Component

public class Mibian {

    private static final NormalDistribution normalDistribution = new NormalDistribution();

    public static double impliedVolatility(Class<?> cls, double[] args, Double callPrice, Double putPrice, double high, double low) {
        double target = callPrice != null ? callPrice : putPrice;
        double restimate;
        try {
            if (callPrice != null) {
                restimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                        .newInstance(args, high / 100, true).getClass().getField("callPrice").get(null);
                if (restimate < target) {
                    return high;
                }
                if (args[0] > args[1] + callPrice) {
                    return 0.001;
                }
            }
            if (putPrice != null) {
                restimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                        .newInstance(args, high / 100, true).getClass().getField("putPrice").get(null);
                if (restimate < target) {
                    return high;
                }
                if (args[1] > args[0] + putPrice) {
                    return 0.001;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        int decimals = String.valueOf(target).split("\\.")[1].length();
        for (int i = 0; i < 10000; i++) {
            double mid = (high + low) / 2;
            if (mid < 0.00001) {
                mid = 0.00001;
            }
            double estimate;
            try {
                if (callPrice != null) {
                    estimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                            .newInstance(args, mid / 100, true).getClass().getField("callPrice").get(null);
                } else {
                    estimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                            .newInstance(args, mid / 100, true).getClass().getField("putPrice").get(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
            if (Math.round(estimate * Math.pow(10, decimals)) == Math.round(target * Math.pow(10, decimals))) {
                return mid;
            } else if (estimate > target) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return -1;
    }

    public static class GK {
        public double underlyingPrice, strikePrice, domesticRate, foreignRate, daysToExpiration;
        public Double callPrice, putPrice, callDelta, putDelta, callDelta2, putDelta2, callTheta, putTheta, callRhoD, putRhoD, callRhoF, putRhoF, vega, gamma, impliedVolatility, putCallParity;

        private double volatility, _a_, _d1_, _d2_;

        public GK(double[] args, Double volatility, Double callPrice, Double putPrice, Boolean performance) {
            this.underlyingPrice = args[0];
            this.strikePrice = args[1];
            this.domesticRate = args[2] / 100;
            this.foreignRate = args[3] / 100;
            this.daysToExpiration = args[4] / 365;

            if (volatility != null) {
                this.volatility = volatility / 100;
                this._a_ = this.volatility * Math.sqrt(this.daysToExpiration);
                this._d1_ = (Math.log(this.underlyingPrice / this.strikePrice) + (this.domesticRate - this.foreignRate + (this.volatility * this.volatility) / 2) * this.daysToExpiration) / this._a_;
                this._d2_ = this._d1_ - this._a_;
                if (performance) {
                    double[] prices = _price();
                    this.callPrice = prices[0];
                    this.putPrice = prices[1];
                } else {
                    double[] prices = _price();
                    this.callPrice = prices[0];
                    this.putPrice = prices[1];
                    double[] deltas = _delta();
                    this.callDelta = deltas[0];
                    this.putDelta = deltas[1];
                    double[] delta2s = _delta2();
                    this.callDelta2 = delta2s[0];
                    this.putDelta2 = delta2s[1];
                    double[] thetas = _theta();
                    this.callTheta = thetas[0];
                    this.putTheta = thetas[1];
                    double[] rhoDs = _rhod();
                    this.callRhoD = rhoDs[0];
                    this.putRhoD = rhoDs[1];
                    double[] rhoFs = _rhof();
                    this.callRhoF = rhoFs[0];
                    this.putRhoF = rhoFs[1];
                    this.vega = _vega();
                    this.gamma = _gamma();
                }
            }
            if (callPrice != null) {
                this.callPrice = callPrice;
                this.impliedVolatility = impliedVolatility(GK.class, args, this.callPrice, null, 500.0, 0.0);
            }
            if (putPrice != null && callPrice == null) {
                this.putPrice = putPrice;
                this.impliedVolatility = impliedVolatility(GK.class, args, null, this.putPrice, 500.0, 0.0);
            }
            if (callPrice != null && putPrice != null) {
                this.callPrice = callPrice;
                this.putPrice = putPrice;
                this.putCallParity = _parity();
            }
        }

        private double[] _price() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = Math.max(0.0, this.underlyingPrice - this.strikePrice);
                put = Math.max(0.0, this.strikePrice - this.underlyingPrice);
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                call = Math.exp(-this.foreignRate * this.daysToExpiration) * this.underlyingPrice * normalDistribution.cumulativeProbability(this._d1_) - Math.exp(-this.domesticRate * this.daysToExpiration) * this.strikePrice * normalDistribution.cumulativeProbability(this._d2_);
                put = Math.exp(-this.domesticRate * this.daysToExpiration) * this.strikePrice * normalDistribution.cumulativeProbability(-this._d2_) - Math.exp(-this.foreignRate * this.daysToExpiration) * this.underlyingPrice * normalDistribution.cumulativeProbability(-this._d1_);
            }
            return new double[]{call, put};
        }

        private double[] _delta() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = this.underlyingPrice > this.strikePrice ? 1.0 : 0.0;
                put = this.underlyingPrice < this.strikePrice ? -1.0 : 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                double _b_ = Math.exp(-this.foreignRate * this.daysToExpiration);
                call = normalDistribution.cumulativeProbability(this._d1_) * _b_;
                put = -normalDistribution.cumulativeProbability(-this._d1_) * _b_;
            }
            return new double[]{call, put};
        }

        private double[] _delta2() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = this.underlyingPrice > this.strikePrice ? -1.0 : 0.0;
                put = this.underlyingPrice < this.strikePrice ? 1.0 : 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                double _b_ = Math.exp(-this.domesticRate * this.daysToExpiration);
                call = -normalDistribution.cumulativeProbability(this._d2_) * _b_;
                put = normalDistribution.cumulativeProbability(-this._d2_) * _b_;
            }
            return new double[]{call, put};
        }

        private double _vega() {
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                return 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                return this.underlyingPrice * Math.exp(-this.foreignRate * this.daysToExpiration) * normalDistribution.density(this._d1_) * Math.sqrt(this.daysToExpiration);
            }
        }

        private double[] _theta() {
            double call, put;
            double _b_ = Math.exp(-this.foreignRate * this.daysToExpiration);
            call = -this.underlyingPrice * _b_ * normalDistribution.density(this._d1_) * this.volatility / (2 * Math.sqrt(this.daysToExpiration)) + this.foreignRate * this.underlyingPrice * _b_ * normalDistribution.cumulativeProbability(this._d1_) - this.domesticRate * this.strikePrice * _b_ * normalDistribution.cumulativeProbability(this._d2_);
            put = -this.underlyingPrice * _b_ * normalDistribution.density(this._d1_) * this.volatility / (2 * Math.sqrt(this.daysToExpiration)) - this.foreignRate * this.underlyingPrice * _b_ * normalDistribution.cumulativeProbability(-this._d1_) + this.domesticRate * this.strikePrice * _b_ * normalDistribution.cumulativeProbability(-this._d2_);
            return new double[]{call / 365, put / 365};
        }

        private double[] _rhod() {
            double call, put;
            call = this.strikePrice * this.daysToExpiration * Math.exp(-this.domesticRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(this._d2_) / 100;
            put = -this.strikePrice * this.daysToExpiration * Math.exp(-this.domesticRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(-this._d2_) / 100;
            return new double[]{call, put};
        }

        private double[] _rhof() {
            double call, put;
            call = -this.underlyingPrice * this.daysToExpiration * Math.exp(-this.foreignRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(this._d1_) / 100;
            put = this.underlyingPrice * this.daysToExpiration * Math.exp(-this.foreignRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(-this._d1_) / 100;
            return new double[]{call, put};
        }

        private double _gamma() {
            return normalDistribution.density(this._d1_) * Math.exp(-this.foreignRate * this.daysToExpiration) / (this.underlyingPrice * this._a_);
        }

        private double _parity() {
            return this.callPrice - this.putPrice - (this.underlyingPrice / Math.pow(1 + this.foreignRate, this.daysToExpiration)) + (this.strikePrice / Math.pow(1 + this.domesticRate, this.daysToExpiration));
        }
    }

    @Getter
    public static class BS {

        private final NormalDistribution normalDistribution = new NormalDistribution();

        private double underlyingPrice, strikePrice, interestRate, daysToExpiration;
        private Double callPrice, putPrice, callDelta, putDelta, callDelta2, putDelta2, callTheta, putTheta, callRho, putRho, vega, gamma, impliedVolatility, putCallParity;

        private double volatility, _a_, _d1_, _d2_;

        public BS(double[] args, Double volatility, Double callPrice, Double putPrice, Boolean performance) {
            this.underlyingPrice = args[0];
            this.strikePrice = args[1];
            this.interestRate = args[2] / 100;
            this.daysToExpiration = args[3] / 365;

            if (volatility != null) {
                this.volatility = volatility / 100;
                this._a_ = this.volatility * Math.sqrt(this.daysToExpiration);
                this._d1_ = (Math.log(this.underlyingPrice / this.strikePrice) + (this.interestRate + (this.volatility * this.volatility) / 2) * this.daysToExpiration) / this._a_;
                this._d2_ = this._d1_ - this._a_;
                if (performance != null) {
                    double[] prices = _price();
                    this.callPrice = prices[0];
                    this.putPrice = prices[1];
                } else {
                    double[] prices = _price();
                    this.callPrice = prices[0];
                    this.putPrice = prices[1];
                    double[] deltas = _delta();
                    this.callDelta = deltas[0];
                    this.putDelta = deltas[1];
                    double[] delta2s = _delta2();
                    this.callDelta2 = delta2s[0];
                    this.putDelta2 = delta2s[1];
                    double[] thetas = _theta();
                    this.callTheta = thetas[0];
                    this.putTheta = thetas[1];
                    double[] rhos = _rho();
                    this.callRho = rhos[0];
                    this.putRho = rhos[1];
                    this.vega = _vega();
                    this.gamma = _gamma();
                }
            }
            if (callPrice != null) {
                this.callPrice = callPrice;
                this.impliedVolatility = impliedVolatility(BS.class, args, this.callPrice, null, 500.0, 0.0);
            }
            if (putPrice != null && callPrice == null) {
                this.putPrice = putPrice;
                this.impliedVolatility = impliedVolatility(BS.class, args, null, this.putPrice, 500.0, 0.0);
            }
            if (callPrice != null && putPrice != null) {
                this.callPrice = callPrice;
                this.putPrice = putPrice;
                this.putCallParity = _parity();
            }
        }

        private double[] _price() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = Math.max(0.0, this.underlyingPrice - this.strikePrice);
                put = Math.max(0.0, this.strikePrice - this.underlyingPrice);
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                call = this.underlyingPrice * normalDistribution.cumulativeProbability(this._d1_) - this.strikePrice * Math.exp(-this.interestRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(this._d2_);
                put = this.strikePrice * Math.exp(-this.interestRate * this.daysToExpiration) * normalDistribution.cumulativeProbability(-this._d2_) - this.underlyingPrice * normalDistribution.cumulativeProbability(-this._d1_);
            }
            return new double[]{call, put};
        }

        private double[] _delta() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = this.underlyingPrice > this.strikePrice ? 1.0 : 0.0;
                put = this.underlyingPrice < this.strikePrice ? -1.0 : 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                call = normalDistribution.cumulativeProbability(this._d1_);
                put = -normalDistribution.cumulativeProbability(-this._d1_);
            }
            return new double[]{call, put};
        }

        private double[] _delta2() {
            double call, put;
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                call = this.underlyingPrice > this.strikePrice ? -1.0 : 0.0;
                put = this.underlyingPrice < this.strikePrice ? 1.0 : 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                double _b_ = Math.exp(-this.interestRate * this.daysToExpiration);
                call = -normalDistribution.cumulativeProbability(this._d2_) * _b_;
                put = normalDistribution.cumulativeProbability(-this._d2_) * _b_;
            }
            return new double[]{call, put};
        }

        private double _vega() {
            if (this.volatility == 0 || this.daysToExpiration == 0) {
                return 0.0;
            } else if (this.strikePrice == 0) {
                throw new ArithmeticException("The strike price cannot be zero");
            } else {
                return this.underlyingPrice * normalDistribution.density(this._d1_) * Math.sqrt(this.daysToExpiration) / 100;
            }
        }

        private double[] _theta() {
            double call, put;
            double _b_ = Math.exp(-this.interestRate * this.daysToExpiration);
            call = -this.underlyingPrice * normalDistribution.density(this._d1_) * this.volatility / (2 * Math.sqrt(this.daysToExpiration)) - this.interestRate * this.strikePrice * _b_ * normalDistribution.cumulativeProbability(this._d2_);
            put = -this.underlyingPrice * normalDistribution.density(this._d1_) * this.volatility / (2 * Math.sqrt(this.daysToExpiration)) + this.interestRate * this.strikePrice * _b_ * normalDistribution.cumulativeProbability(-this._d2_);
            return new double[]{call / 365, put / 365};
        }

        private double[] _rho() {
            double call, put;
            double _b_ = Math.exp(-this.interestRate * this.daysToExpiration);
            call = this.strikePrice * this.daysToExpiration * _b_ * normalDistribution.cumulativeProbability(this._d2_) / 100;
            put = -this.strikePrice * this.daysToExpiration * _b_ * normalDistribution.cumulativeProbability(-this._d2_) / 100;
            return new double[]{call, put};
        }

        private double _gamma() {
            return normalDistribution.density(this._d1_) / (this.underlyingPrice * this._a_);
        }

        private double _parity() {
            return this.callPrice - this.putPrice - this.underlyingPrice + (this.strikePrice / Math.pow(1 + this.interestRate, this.daysToExpiration));
        }

        public static double impliedVolatility(Class<?> cls, double[] args, Double callPrice, Double putPrice, double high, double low) {
            double target = callPrice != null ? callPrice : putPrice;
            double restimate;
            try {
                if (callPrice != null) {
                    restimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                            .newInstance(args, high / 100, true).getClass().getField("callPrice").get(null);
                    if (restimate < target) {
                        return high;
                    }
                    if (args[0] > args[1] + callPrice) {
                        return 0.001;
                    }
                }
                if (putPrice != null) {
                    restimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                            .newInstance(args, high / 100, true).getClass().getField("putPrice").get(null);
                    if (restimate < target) {
                        return high;
                    }
                    if (args[1] > args[0] + putPrice) {
                        return 0.001;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            int decimals = String.valueOf(target).split("\\.")[1].length();
            for (int i = 0; i < 10000; i++) {
                double mid = (high + low) / 2;
                if (mid < 0.00001) {
                    mid = 0.00001;
                }
                double estimate;
                try {
                    if (callPrice != null) {
                        estimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                                .newInstance(args, mid / 100, true).getClass().getField("callPrice").get(null);
                    } else {
                        estimate = (double) cls.getConstructor(double[].class, Double.TYPE, Boolean.TYPE)
                                .newInstance(args, mid / 100, true).getClass().getField("putPrice").get(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
                if (Math.round(estimate * Math.pow(10, decimals)) == Math.round(target * Math.pow(10, decimals))) {
                    return mid;
                } else if (estimate > target) {
                    high = mid;
                } else {
                    low = mid;
                }
            }
            return -1;
        }
    }
}