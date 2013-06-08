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

package compecon.engine;

import compecon.culture.sectors.financial.Bank;
import compecon.culture.sectors.financial.BankAccount;
import compecon.culture.sectors.financial.Currency;
import compecon.engine.dao.DAOFactory;
import compecon.engine.util.HibernateUtil;

public class BankAccountFactory {
	public static BankAccount newInstanceBankAccount(final Agent owner,
			boolean overdraftPossible, final Currency currency,
			final Bank managingBank) {
		BankAccount bankAccount = new BankAccount();

		bankAccount.setOwner(owner);
		bankAccount.setOverdraftPossible(overdraftPossible);
		bankAccount.setCurrency(currency);
		bankAccount.setManagingBank(managingBank);

		DAOFactory.getBankAccountDAO().save(bankAccount);
		HibernateUtil.flushSession();
		return bankAccount;
	}

	public static void deleteBankAccount(BankAccount bankAccount) {
		DAOFactory.getBankAccountDAO().delete(bankAccount);
	}
}