/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.squareup.javapoet;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A generated constructor or method declaration. */
public final class MethodSpec extends Initializable<MethodSpec> {
	static final String CONSTRUCTOR = "<init>";

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodSpec.class);

	transient public String name;
	transient public CodeBlock javadoc;
	transient public List<AnnotationSpec> annotations;
	transient public Set<Modifier> modifiers;
	transient public List<TypeVariableName> typeVariables;
	transient public TypeName returnType;
	transient public List<ParameterSpec> parameters;
	transient public boolean varargs;
	transient public List<TypeName> exceptions;
	transient public CodeBlock code;
	transient public CodeBlock defaultValue;

	private MethodSpec(Builder builder) {
		initialize(builder);
	}

	@Override
	public final void initialize(Initializer<MethodSpec> aBuilder) {
		Builder builder = (Builder) aBuilder;
		CodeBlock code = builder.code.build();
		checkArgument(code.isEmpty() || !builder.modifiers.contains(Modifier.ABSTRACT),
				"abstract method %s cannot have code", builder.name);
		checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
				"last parameter of varargs method %s must be an array", builder.name);

		this.name = checkNotNull(builder.name, "name == null");
		this.javadoc = builder.javadoc.build();
		this.annotations = Util.immutableList(builder.annotations);
		this.modifiers = Util.immutableSet(builder.modifiers);
		this.typeVariables = Util.immutableList(builder.typeVariables);
		this.returnType = builder.returnType;
		this.parameters = Util.immutableList(builder.parameters);
		this.varargs = builder.varargs;
		this.exceptions = Util.immutableList(builder.exceptions);
		this.defaultValue = builder.defaultValue;
		this.code = code;
		super.initialize(builder);
	}

	private boolean lastParameterIsArray(List<ParameterSpec> parameters) {
		return !parameters.isEmpty()
				&& TypeName.arrayComponent(parameters.get(parameters.size() - 1).getType()) != null;
	}

	void emit(CodeWriter codeWriter, String enclosingName, Set<Modifier> implicitModifiers)
			throws IOException {
		ensureInitialized();
		codeWriter.emitJavadoc(javadoc);
		codeWriter.emitAnnotations(annotations, false);
		codeWriter.emitModifiers(modifiers, implicitModifiers);

		if (!typeVariables.isEmpty()) {
			codeWriter.emitTypeVariables(typeVariables);
			codeWriter.emit(" ");
		}

		if (isConstructor()) {
			codeWriter.emit("$L(", enclosingName);
		} else {
			codeWriter.emit("$T $L(", returnType, name);
		}

		boolean firstParameter = true;
		for (Iterator<ParameterSpec> i = parameters.iterator(); i.hasNext();) {
			ParameterSpec parameter = i.next();
			if (!firstParameter)
				codeWriter.emit(",").emitWrappingSpace();
			parameter.emit(codeWriter, !i.hasNext() && varargs);
			firstParameter = false;
		}

		codeWriter.emit(")");

		if (defaultValue != null && !defaultValue.isEmpty()) {
			codeWriter.emit(" default ");
			codeWriter.emit(defaultValue);
		}

		if (!exceptions.isEmpty()) {
			codeWriter.emitWrappingSpace().emit("throws");
			boolean firstException = true;
			for (TypeName exception : exceptions) {
				if (!firstException)
					codeWriter.emit(",");
				codeWriter.emitWrappingSpace().emit("$T", exception);
				firstException = false;
			}
		}

		if (hasModifier(Modifier.ABSTRACT)) {
			codeWriter.emit(";\n");
		} else if (hasModifier(Modifier.NATIVE)) {
			// Code is allowed to support stuff like GWT JSNI.
			codeWriter.emit(code);
			codeWriter.emit(";\n");
		} else {
			codeWriter.emit(" {\n");

			codeWriter.indent();
			codeWriter.emit(code);
			codeWriter.unindent();

			codeWriter.emit("}\n");
		}
	}

	public boolean hasModifier(Modifier modifier) {
		ensureInitialized();
		return modifiers.contains(modifier);
	}

	public boolean isConstructor() {
		ensureInitialized();
		return name.equals(CONSTRUCTOR);
	}

	//	@Override
	//	public boolean equals(Object o) {
	//		if (this == o)
	//			return true;
	//		if (o == null)
	//			return false;
	//		ensureInitialized();
	//		if (getClass() != o.getClass())
	//			return false;
	//		return toBuilder().equals(((MethodSpec)o).toBuilder());
	//	}

	//	@Override
	//	public int hashCode() {
	//		ensureInitialized();
	//		if (hashCode == null) {
	//			hashCode = toBuilder().hashCode();
	//		}
	//		return hashCode;
	//	}

	@Override
	public String toString() {
		StringWriter out = new StringWriter();
		try {
			CodeWriter codeWriter = new CodeWriter(out);
			emit(codeWriter, "Constructor", Collections.<Modifier>emptySet());
			return out.toString();
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	public static Builder methodBuilder(String name) {
		return new Builder(name);
	}

	public static Builder constructorBuilder() {
		return new Builder(CONSTRUCTOR);
	}

	/**
	 * Returns a new method spec builder that overrides {@code method}.
	 *
	 * <p>
	 * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
	 * throws declarations. An {@link Override} annotation will be added.
	 *
	 * <p>
	 * Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
	 * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
	 */
	public static Builder overriding(ExecutableElement method) {
		checkNotNull(method, "method == null");

		Set<Modifier> modifiers = method.getModifiers();
		if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL)
				|| modifiers.contains(Modifier.STATIC)) {
			throw new IllegalArgumentException(
					"cannot override method with modifiers: " + modifiers);
		}

		String methodName = method.getSimpleName().toString();
		MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

		methodBuilder.addAnnotation(Override.class);

		modifiers = new LinkedHashSet<>(modifiers);
		modifiers.remove(Modifier.ABSTRACT);
		modifiers.remove(Util.DEFAULT); // LinkedHashSet permits null as element for Java 7
		methodBuilder.addModifiers(modifiers);

		for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
			TypeVariable var = (TypeVariable) typeParameterElement.asType();
			methodBuilder.addTypeVariable(TypeVariableName.get(var));
		}

		methodBuilder.returns(TypeName.get(method.getReturnType()));
		methodBuilder.addParameters(ParameterSpec.parametersOf(method));
		methodBuilder.varargs(method.isVarArgs());

		for (TypeMirror thrownType : method.getThrownTypes()) {
			methodBuilder.addException(TypeName.get(thrownType));
		}

		return methodBuilder;
	}

	/**
	 * Returns a new method spec builder that overrides {@code method} as a member of {@code
	 * enclosing}. This will resolve type parameters: for example overriding
	 * {@link Comparable#compareTo} in a type that implements {@code Comparable<Movie>}, the
	 * {@code T} parameter will be resolved to {@code Movie}.
	 *
	 * <p>
	 * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
	 * throws declarations. An {@link Override} annotation will be added.
	 *
	 * <p>
	 * Note that in JavaPoet 1.2 through 1.7 this method retained annotations from the method and
	 * parameters of the overridden method. Since JavaPoet 1.8 annotations must be added separately.
	 */
	public static Builder overriding(ExecutableElement method, DeclaredType enclosing,
			Types types) {
		ExecutableType executableType = (ExecutableType) types.asMemberOf(enclosing, method);
		List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
		TypeMirror resolvedReturnType = executableType.getReturnType();

		Builder builder = overriding(method);
		builder.returns(TypeName.get(resolvedReturnType));
		for (int i = 0, size = builder.parameters.size(); i < size; i++) {
			ParameterSpec parameter = builder.parameters.get(i);
			TypeName type = TypeName.get(resolvedParameterTypes.get(i));
			builder.parameters.set(i, parameter.toBuilder(type, parameter.name).build());
		}

		return builder;
	}

	public Builder toBuilder() {
		Builder builder = new Builder(name);
		builder.javadoc.add(javadoc);
		builder.annotations.addAll(annotations);
		builder.modifiers.addAll(modifiers);
		builder.typeVariables.addAll(typeVariables);
		builder.returnType = returnType;
		builder.parameters.addAll(parameters);
		builder.exceptions.addAll(exceptions);
		builder.code.add(code);
		builder.varargs = varargs;
		builder.defaultValue = defaultValue;
		return builder;
	}

	public static final class Builder implements Initializer<MethodSpec> {
		private final String name;

		private final CodeBlock.Builder javadoc = CodeBlock.builder();
		private final Set<AnnotationSpec> annotations = new HashSet<>();
		private final Set<Modifier> modifiers = new HashSet<>();
		private Set<TypeVariableName> typeVariables = new HashSet<>();
		private TypeName returnType;
		private final List<ParameterSpec> parameters = new ArrayList<>();
		private final Set<TypeName> exceptions = new LinkedHashSet<>();
		private final CodeBlock.Builder code = CodeBlock.builder();
		private boolean varargs;
		private CodeBlock defaultValue;

		private Builder(String name) {
			checkNotNull(name, "name == null");
			checkArgument(name.equals(CONSTRUCTOR) || SourceVersion.isName(name),
					"not a valid name: %s", name);
			this.name = name;
			this.returnType = name.equals(CONSTRUCTOR) ? null : TypeName.VOID;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
			result = prime * result + ((code == null) ? 0 : code.hashCode());
			result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
			result = prime * result + ((exceptions == null) ? 0 : exceptions.hashCode());
			result = prime * result + ((javadoc == null) ? 0 : javadoc.hashCode());
			result = prime * result + ((modifiers == null) ? 0 : modifiers.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
			result = prime * result + ((typeVariables == null) ? 0 : typeVariables.hashCode());
			result = prime * result + (varargs ? 1231 : 1237);
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Builder)) {
				return false;
			}
			Builder other = (Builder) obj;
			if (annotations == null) {
				if (other.annotations != null) {
					return false;
				}
			} else if (!annotations.equals(other.annotations)) {
				return false;
			}
			if (code == null) {
				if (other.code != null) {
					return false;
				}
			} else if (!code.equals(other.code)) {
				return false;
			}
			if (defaultValue == null) {
				if (other.defaultValue != null) {
					return false;
				}
			} else if (!defaultValue.equals(other.defaultValue)) {
				return false;
			}
			if (exceptions == null) {
				if (other.exceptions != null) {
					return false;
				}
			} else if (!exceptions.equals(other.exceptions)) {
				return false;
			}
			if (javadoc == null) {
				if (other.javadoc != null) {
					return false;
				}
			} else if (!javadoc.equals(other.javadoc)) {
				return false;
			}
			if (modifiers == null) {
				if (other.modifiers != null) {
					return false;
				}
			} else if (!modifiers.equals(other.modifiers)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (parameters == null) {
				if (other.parameters != null) {
					return false;
				}
			} else if (!parameters.equals(other.parameters)) {
				return false;
			}
			if (returnType == null) {
				if (other.returnType != null) {
					return false;
				}
			} else if (!returnType.equals(other.returnType)) {
				return false;
			}
			if (typeVariables == null) {
				if (other.typeVariables != null) {
					return false;
				}
			} else if (!typeVariables.equals(other.typeVariables)) {
				return false;
			}
			if (varargs != other.varargs) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("name:").append(this.name)
			.append(',')
			.append("returnType:").append(returnType)
			.append(',')
			.append("nAnnotations:").append(annotations.size())
			.append(',')
			.append("nModifiers:").append(modifiers.size())
			.append(',')
			.append("nTypeVariables:").append(typeVariables.size())
			.append(',')
			.append("nParameters:").append(parameters.size())
			.append(',')
			.append("nExceptions:").append(exceptions.size());
			return sb.toString();
		}

		public Builder addJavadoc(String format, Object... args) {
			javadoc.add(format, args);
			return this;
		}

		public Builder addJavadoc(CodeBlock block) {
			javadoc.add(block);
			return this;
		}

		public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
			checkArgument(annotationSpecs != null, "annotationSpecs == null");
			for (AnnotationSpec annotationSpec : annotationSpecs) {
				this.annotations.add(annotationSpec);
			}
			return this;
		}

		public Builder addAnnotation(AnnotationSpec annotationSpec) {
			this.annotations.add(annotationSpec);
			return this;
		}

		public Builder addAnnotation(ClassName annotation) {
			this.annotations.add(AnnotationSpec.builder(annotation).build());
			return this;
		}

		public Builder addAnnotation(Class<?> annotation) {
			return addAnnotation(ClassName.get(annotation));
		}

		public Builder addModifiers(Modifier... modifiers) {
			checkNotNull(modifiers, "modifiers == null");
			Collections.addAll(this.modifiers, modifiers);
			return this;
		}

		public Builder addModifiers(Iterable<Modifier> modifiers) {
			checkNotNull(modifiers, "modifiers == null");
			for (Modifier modifier : modifiers) {
				this.modifiers.add(modifier);
			}
			return this;
		}

		public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
			checkArgument(typeVariables != null, "typeVariables == null");
			for (TypeVariableName typeVariable : typeVariables) {
				this.typeVariables.add(typeVariable);
			}
			return this;
		}

		public Builder addTypeVariable(TypeVariableName typeVariable) {
			typeVariables.add(typeVariable);
			return this;
		}

		public Builder returns(TypeName returnType) {
			checkState(!name.equals(CONSTRUCTOR), "constructor cannot have return type.");
			this.returnType = returnType;
			return this;
		}

		public Builder returns(Type returnType) {
			return returns(TypeName.get(returnType));
		}

		public Builder addParameters(Iterable<ParameterSpec> parameterSpecs) {
			checkArgument(parameterSpecs != null, "parameterSpecs == null");
			for (ParameterSpec parameterSpec : parameterSpecs) {
				this.parameters.add(parameterSpec);
			}
			return this;
		}

		public Builder addParameter(ParameterSpec parameterSpec) {
			this.parameters.add(parameterSpec);
			return this;
		}

		public Builder addParameter(TypeName type, String name, Modifier... modifiers) {
			return addParameter(ParameterSpec.builder(type, name, modifiers).build());
		}

		public Builder addParameter(Type type, String name, Modifier... modifiers) {
			return addParameter(TypeName.get(type), name, modifiers);
		}

		public Builder varargs() {
			return varargs(true);
		}

		public Builder varargs(boolean varargs) {
			this.varargs = varargs;
			return this;
		}

		public Builder addExceptions(Iterable<? extends TypeName> exceptions) {
			checkArgument(exceptions != null, "exceptions == null");
			for (TypeName exception : exceptions) {
				this.exceptions.add(exception);
			}
			return this;
		}

		public Builder addException(TypeName exception) {
			this.exceptions.add(exception);
			return this;
		}

		public Builder addException(Type exception) {
			return addException(TypeName.get(exception));
		}

		public Builder addCode(String format, Object... args) {
			code.add(format, args);
			return this;
		}

		public Builder addNamedCode(String format, Map<String, ?> args) {
			code.addNamed(format, args);
			return this;
		}

		public Builder addCode(CodeBlock codeBlock) {
			code.add(codeBlock);
			return this;
		}

		public Builder addComment(String format, Object... args) {
			code.add("// " + format + "\n", args);
			return this;
		}

		public Builder defaultValue(String format, Object... args) {
			return defaultValue(CodeBlock.of(format, args));
		}

		public Builder defaultValue(CodeBlock codeBlock) {
			checkState(this.defaultValue == null, "defaultValue was already set");
			this.defaultValue = checkNotNull(codeBlock, "codeBlock == null");
			return this;
		}

		/**
		 * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
		 *        Shouldn't contain braces or newline characters.
		 */
		public Builder beginControlFlow(String controlFlow, Object... args) {
			code.beginControlFlow(controlFlow, args);
			return this;
		}

		/**
		 * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)"
		 *        . Shouldn't contain braces or newline characters.
		 */
		public Builder nextControlFlow(String controlFlow, Object... args) {
			code.nextControlFlow(controlFlow, args);
			return this;
		}

		public Builder endControlFlow() {
			code.endControlFlow();
			return this;
		}

		/**
		 * @param controlFlow the optional control flow construct and its code, such as
		 *        "while(foo == 20)". Only used for "do/while" control flows.
		 */
		public Builder endControlFlow(String controlFlow, Object... args) {
			code.endControlFlow(controlFlow, args);
			return this;
		}

		public Builder addStatement(String format, Object... args) {
			code.addStatement(format, args);
			return this;
		}

		public MethodSpec build() {
			LOGGER.debug("build using {}", this);
			return new MethodSpec(this);
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
