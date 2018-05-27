package com.itranswarp.contract;

public interface ERC20Contract {

	long totalSupply();

	long balanceOf(String tokenOwner);

	void transfer(String from, String to, long value);

	void batchTransfer(String from, String[] tos, long value);
}
