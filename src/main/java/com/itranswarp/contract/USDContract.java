package com.itranswarp.contract;

import java.util.HashMap;
import java.util.Map;

public class USDContract implements ERC20Contract {

	long total;
	Map<String, Long> balance;

	public USDContract() {
		this.total = 9_000_000_000L;
		this.balance = new HashMap<>();
		this.balance.put("0x123456", this.total);
	}

	@Override
	public long totalSupply() {
		return this.total;
	}

	@Override
	public long balanceOf(String tokenOwner) {
		return this.balance.getOrDefault(tokenOwner, 0L);
	}

	@Override
	public void transfer(String from, String to, long value) {
		check(value > 0, "Value must be positive.");
		long fromBalance = balanceOf(from);
		check(fromBalance >= value, "Balance of msg.sender must be greater or equal than value.");
		setBalance(from, fromBalance - value);
		setBalance(to, balanceOf(to) + value);
	}

	@Override
	public void batchTransfer(String from, String[] tos, long value) {
		check(value > 0, "Value must be positive.");
		check(tos.length > 0, "Tos must be not empty.");
		// without overflow check:
		long sum = tos.length * value;
		long fromBalance = balanceOf(from);
		check(fromBalance >= sum, "Balance of msg.sender must be enough.");
		for (String to : tos) {
			setBalance(to, balanceOf(to) + value);
		}
		setBalance(from, fromBalance - sum);
	}

	private void setBalance(String name, long value) {
		balance.put(name, value);
	}

	private void check(boolean exp, String message) {
		if (!exp) {
			throw new RuntimeException(message);
		}
	}
}
