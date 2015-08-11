package com.unit16.conf.checks;

import com.unit16.conf.C;
import com.unit16.conf.C.Checker;

public class ValidPort extends Checker.I<Integer> {

	public ValidPort() {
		super("Valid port in range [1,65536]");
	}

	@Override
	public boolean validate(Integer value, C<Integer> def) {
		if(value == null || (value > 0 && value <= 65536)) {
			return true;
		}
		this.setError("Port is not in valid port range [1,65536]");
		return false;
	}
	
}
