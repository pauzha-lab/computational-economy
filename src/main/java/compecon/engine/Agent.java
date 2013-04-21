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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import compecon.culture.markets.PrimaryMarket;
import compecon.culture.sectors.financial.Bank;
import compecon.culture.sectors.financial.BankAccount;
import compecon.culture.sectors.financial.Currency;
import compecon.culture.sectors.state.law.bookkeeping.BalanceSheet;
import compecon.culture.sectors.state.law.property.HardCashRegister;
import compecon.culture.sectors.state.law.property.IPropertyOwner;
import compecon.culture.sectors.state.law.property.PropertyRegister;
import compecon.engine.time.ITimeSystemEvent;
import compecon.engine.time.TimeSystem;
import compecon.engine.time.calendar.HourType;
import compecon.nature.materia.GoodType;

@Entity
@Table(name = "Agent")
@org.hibernate.annotations.Table(appliesTo = "Agent", indexes = { @Index(name = "IDX_DTYPE", columnNames = { "DTYPE" }) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Agent implements IPropertyOwner {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	protected int id;

	@Column(name = "isDeconstructed")
	protected boolean isDeconstructed = false;

	@Transient
	protected Set<ITimeSystemEvent> timeSystemEvents = new HashSet<ITimeSystemEvent>();

	@ElementCollection
	@CollectionTable(name = "Agent_BankPassword", joinColumns = @JoinColumn(name = "agent_id"))
	@MapKeyJoinColumn(name = "bank_id")
	@Column(name = "bankPassword")
	// CREATE INDEX bank_id ON TABLE Agent
	protected Map<Bank, String> bankPasswords = new HashMap<Bank, String>();

	@ManyToOne
	@JoinColumn(name = "primaryBank_id")
	@Index(name = "IDX_PRIMARYBANK_ID")
	protected Bank primaryBank;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "transactionsBankAccount_id")
	@Index(name = "IDX_TRANSACTIONSBANKACCOUNT_ID")
	// bank account for basic daily transactions
	protected BankAccount transactionsBankAccount;

	@Transient
	protected final HourType BALANCE_SHEET_PUBLICATION_HOUR_TYPE = HourType.HOUR_23;

	@Transient
	private final String agentTypeName = this.getClass().getSimpleName();

	public void initialize() {
		Log.agent_onConstruct(this);
	}

	/*
	 * Accessors
	 */

	public int getId() {
		return id;
	}

	public boolean isDeconstructed() {
		return isDeconstructed;
	}

	public Set<ITimeSystemEvent> getTimeSystemEvents() {
		return timeSystemEvents;
	}

	public Map<Bank, String> getBankPasswords() {
		return bankPasswords;
	}

	public Bank getPrimaryBank() {
		return primaryBank;
	}

	public BankAccount getTransactionsBankAccount() {
		return transactionsBankAccount;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setDeconstructed(boolean isDeconstructed) {
		this.isDeconstructed = isDeconstructed;
	}

	public void setTimeSystemEvents(Set<ITimeSystemEvent> timeSystemEvents) {
		this.timeSystemEvents = timeSystemEvents;
	}

	public void setBankPasswords(Map<Bank, String> bankPasswords) {
		this.bankPasswords = bankPasswords;
	}

	public void setPrimaryBank(Bank primaryBank) {
		this.primaryBank = primaryBank;
	}

	public void setTransactionsBankAccount(BankAccount transactionsBankAccount) {
		this.transactionsBankAccount = transactionsBankAccount;
	}

	/*
	 * Assertions
	 */

	@Transient
	protected void assertTransactionsBankAccount() {
		// initialize bank account
		if (this.primaryBank == null) {
			this.primaryBank = AgentFactory
					.getRandomInstanceCreditBank(Currency.EURO);
			String bankPassword = this.primaryBank.openCustomerAccount(this);
			this.bankPasswords.put(this.primaryBank, bankPassword);
		}
		if (this.transactionsBankAccount == null) {
			this.transactionsBankAccount = this.primaryBank.openBankAccount(
					this, Currency.EURO,
					this.bankPasswords.get(this.primaryBank));
		}
	}

	/*
	 * Business logic
	 */

	/**
	 * deregisters the agent from all referencing objects
	 */
	@Transient
	protected void deconstruct() {
		Log.agent_onDeconstruct(this);
		this.isDeconstructed = true;

		// deregister from TimeSystem
		for (ITimeSystemEvent timeSystemEvent : this.timeSystemEvents)
			TimeSystem.getInstance().removeEvent(timeSystemEvent);

		// remove selling offers from primary market
		PrimaryMarket.getInstance().removeAllSellingOffers(this);

		// deregister from PropertyRegister
		PropertyRegister.getInstance().deregister(this);

		// deregister from CashRegister
		HardCashRegister.getInstance().deregister(this);

		// deregister from CreditBank
		this.primaryBank.closeCustomerAccount(this,
				this.bankPasswords.get(this.primaryBank));
		this.primaryBank = null;
		this.transactionsBankAccount = null;
		this.timeSystemEvents = null;
		this.bankPasswords = null;
	}

	@Transient
	public BalanceSheet issueBasicBalanceSheet() {
		this.assertTransactionsBankAccount();

		Currency referenceCurrency = Agent.this.transactionsBankAccount
				.getCurrency();

		if (referenceCurrency == null)
			throw new RuntimeException("referenceCurrency is "
					+ referenceCurrency);

		BalanceSheet balanceSheet = new BalanceSheet(referenceCurrency);

		// hard cash, TODO convert other currencies
		balanceSheet.hardCash = HardCashRegister.getInstance().getBalance(
				Agent.this, referenceCurrency);

		// bank deposits
		if (Agent.this.transactionsBankAccount.getBalance() > 0)
			balanceSheet.cash += Agent.this.transactionsBankAccount
					.getBalance();
		else
			balanceSheet.loans += -1
					* Agent.this.transactionsBankAccount.getBalance();

		// inventory
		for (GoodType goodType : GoodType.values()) {
			balanceSheet.inventory.put(goodType, PropertyRegister.getInstance()
					.getBalance(Agent.this, goodType));
		}

		return balanceSheet;
	}

	@Override
	public String toString() {
		if (this.id != 0)
			return this.agentTypeName + " (ID " + this.id + ")";
		return this.agentTypeName;
	}
}