package com.squareup.javapoet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			if (LOGGER.isTraceEnabled()) LOGGER.trace("{} already initialized", initializer.getName());
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
			@SuppressWarnings("unchecked")
			Initializer<T> otherInitializer = ((Initializable<T>) other).initializer;
			return (initializer.equals(otherInitializer));
		}
		return false;
	}


}
