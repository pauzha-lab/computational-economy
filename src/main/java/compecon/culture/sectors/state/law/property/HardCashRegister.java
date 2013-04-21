/*
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

package compecon.culture.sectors.state.law.property;

import java.util.HashMap;

import compecon.culture.sectors.financial.Currency;
import compecon.engine.Agent;

public class HardCashRegister {

	private static HardCashRegister instance;

	private HashMap<Agent, HashMap<Currency, Double>> balances = new HashMap<Agent, HashMap<Currency, Double>>();

	private HardCashRegister() {
		super();
	}

	public static HardCashRegister getInstance() {
		if (instance == null)
			instance = new HardCashRegister();
		return instance;
	}

	public double getBalance(Agent agent, Currency currency) {
		this.assertAgentHasBalances(agent);

		HashMap<Currency, Double> balancesForIAgent = this.balances.get(agent);
		double balance = 0;
		if (balancesForIAgent.containsKey(currency))
			balance = balancesForIAgent.get(currency);

		return balance;
	}

	public double increment(Agent agent, Currency currency, double amount) {
		this.assertAgentHasBalances(agent);

		if (amount <= 0)
			throw new RuntimeException("amount is too small");
		double oldBalance = this.getBalance(agent, currency);
		double newBalance = oldBalance + amount;
		this.balances.get(agent).put(currency, newBalance);
		return newBalance;
	}

	public double decrement(Agent agent, Currency currency, double amount) {
		this.assertAgentHasBalances(agent);

		if (amount < 0)
			throw new RuntimeException("amount is negative");

		double oldBalance = this.getBalance(agent, currency);

		if (oldBalance < amount)
			throw new RuntimeException("not enough cash, amount is too large");

		double newBalance = oldBalance - amount;
		this.balances.get(agent).put(currency, newBalance);
		return newBalance;
	}

	private void assertAgentHasBalances(Agent agent) {
		if (!this.balances.containsKey(agent))
			this.balances.put(agent, new HashMap<Currency, Double>());
	}

	/*
	 * deregister
	 */
	public void deregister(Agent agent) {
		this.balances.remove(agent); // TODO transfer to other agent?
	}
}