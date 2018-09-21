package com.squareup.javapoet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turrettech.p3cobol.P3Compiler;

public abstract class Initializable<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Initializable.class);

	transient private boolean isInitialized;
	private Initializer<T> initializer;

	public Initializable<T> ensureInitialized() {
		if (!isInitialized) {
			try {
				initialize(initializer);
				LOGGER.debug("initialized {}", initializer.getName());
			} catch (Exception e) {
				LOGGER.error("exception during initialization: ", e);
			}
		} else {
			if (P3Compiler.LOW_LEVEL_LOGGING) LOGGER.trace("{} already initialized", initializer.getName());
		}
		return this;
	}

	public void initialize(Initializer<T> builder) {
		this.initializer = builder;
		isInitialized = true;
	}

	@Override
	public int hashCode() {
		return initializer.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Initializable) {
			Initializer<T> otherInitializer = ((Initializable) other).initializer;
			return (initializer.equals(otherInitializer));
		}
		return false;
	}


}
