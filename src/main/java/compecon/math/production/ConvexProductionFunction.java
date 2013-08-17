/*
Copyright (C) 2013 u.wol@wwu.de 
 
This file is part of ComputationalEconomy.

ComputationalEconomy is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationalEconomy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationalEconomy. If not, see <http://www.gnu.org/licenses/>.
 */

package compecon.math.production;

import java.util.LinkedHashMap;
import java.util.Map;

import compecon.engine.jmx.Log;
import compecon.engine.util.MathUtil;
import compecon.materia.GoodType;
import compecon.math.IFunction;

public abstract class ConvexProductionFunction extends ProductionFunction {

	protected ConvexProductionFunction(IFunction<GoodType> delegate) {
		super(delegate);
	}

	public Map<GoodType, Double> calculateProfitMaximizingBundleOfProductionFactorsUnderBudgetRestriction(
			double priceOfProducedGoodType,
			Map<GoodType, Double> pricesOfProductionFactors,
			final double budget, final double maxOutput) {

		// order of exponents is preserved, so that important GoodTypes
		// will be bought first
		Map<GoodType, Double> bundleOfInputFactors = new LinkedHashMap<GoodType, Double>();

		// define estimated revenue per unit
		double estMarginalRevenue = priceOfProducedGoodType;

		// initialize
		for (GoodType goodType : this.getInputGoodTypes())
			bundleOfInputFactors.put(goodType, 0.0);

		// check for estimated revenue per unit
		if (MathUtil.equal(estMarginalRevenue, 0.0)) {
			Log.log("estMarginalRevenue = " + estMarginalRevenue
					+ " -> no production");
			return bundleOfInputFactors;
		}

		// check for budget
		if (MathUtil.equal(budget, 0)) {
			Log.log("budget is " + budget);
			return bundleOfInputFactors;
		}

		// maximize profit
		final int NUMBER_OF_ITERATIONS = this.getInputGoodTypes().size() * 20;

		double moneySpent = 0.0;
		double lastProfitableMarginalCost = 0;
		while (MathUtil.greater(budget, moneySpent)) {
			GoodType optimalInput = this
					.selectInputWithHighestMarginalOutputPerPrice(
							bundleOfInputFactors, pricesOfProductionFactors);

			double marginalCost = pricesOfProductionFactors.get(optimalInput)
					/ this.calculateMarginalOutput(bundleOfInputFactors,
							optimalInput);
			double priceOfGoodType = pricesOfProductionFactors
					.get(optimalInput);
			double amount = (budget / NUMBER_OF_ITERATIONS) / priceOfGoodType;
			bundleOfInputFactors.put(optimalInput,
					bundleOfInputFactors.get(optimalInput) + amount);
			double newOutput = this.calculateOutput(bundleOfInputFactors);

			if (!this.delegate
					.getNeedsAllInputFactorsNonZeroForPartialDerivate()
					|| this.hasAllInputFactorsNonZero(bundleOfInputFactors)) {
				if (!Double.isNaN(estMarginalRevenue)
						&& !Double.isInfinite(estMarginalRevenue)) {
					// a polypoly is assumed -> price = marginal revenue
					if (MathUtil.lesser(estMarginalRevenue, marginalCost)) {
						Log.log(MathUtil.round(lastProfitableMarginalCost)
								+ " deltaCost" + " <= "
								+ MathUtil.round(estMarginalRevenue)
								+ " deltaEstRevenue" + " < "
								+ MathUtil.round(marginalCost) + " deltaCost"
								+ " -> "
								+ bundleOfInputFactors.entrySet().toString());
						break;
					}
				}
			}

			if (maxOutput != -1 && MathUtil.greater(newOutput, maxOutput)) {
				bundleOfInputFactors.put(optimalInput,
						bundleOfInputFactors.get(optimalInput) - amount);
				Log.log("output " + newOutput + " > maxOutput " + maxOutput
						+ " -> " + bundleOfInputFactors.entrySet().toString());
				break;
			}

			lastProfitableMarginalCost = marginalCost;
			moneySpent += priceOfGoodType * amount;
		}

		return bundleOfInputFactors;
	}

	protected boolean hasAllInputFactorsNonZero(
			Map<GoodType, Double> bundleOfInputFactors) {
		for (Double amount : bundleOfInputFactors.values()) {
			if (amount == 0)
				return false;
		}

		return true;
	}
}