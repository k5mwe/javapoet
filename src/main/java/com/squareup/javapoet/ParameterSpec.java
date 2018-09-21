/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/** A generated parameter declaration. */
public final class ParameterSpec extends Initializable<ParameterSpec> {
	transient public String name;
	transient public List<AnnotationSpec> annotations;
	transient public Set<Modifier> modifiers;
	transient public TypeName type;

	private ParameterSpec(Builder builder) {
		initialize(builder);
	}

	@Override
	public void initialize(Initializer<ParameterSpec> initializer) {
		Builder builder = (Builder) initializer;
		this.name = checkNotNull(builder.name, "name == null");
		this.annotations = Util.immutableList(builder.annotations);
		this.modifiers = Util.immutableSet(builder.modifiers);
		this.type = checkNotNull(builder.type, "type == null");
		super.initialize(builder);
		//		hashCode = null;
	}

	public boolean hasModifier(Modifier modifier) {
		ensureInitialized();
		return modifiers.contains(modifier);
	}

	public TypeName getType() {
		ensureInitialized();
		return type;
	}

	void emit(CodeWriter codeWriter, boolean varargs) throws IOException {
		ensureInitialized();
		codeWriter.emitAnnotations(annotations, true);
		codeWriter.emitModifiers(modifiers);
		if (varargs) {
			codeWriter.emit("$T... $L", TypeName.arrayComponent(type), name);
		} else {
			codeWriter.emit("$T $L", type, name);
		}
	}

	//	@Override public boolean equals(Object o) {
	//		if (this == o) return true;
	//		if (o == null) return false;
	//		if (getClass() != o.getClass()) return false;
	//		return toBuilder().equals(((ParameterSpec)o).toBuilder());
	//	}
	//
	//	Integer hashCode = null;
	//
	//	@Override
	//	public int hashCode() {
	//		ensureInitialized();
	//		if (hashCode == null) {
	//			hashCode = toString().hashCode();
	//		}
	//		return hashCode;
	//	}

	@Override public String toString() {
		StringWriter out = new StringWriter();
		try {
			CodeWriter codeWriter = new CodeWriter(out);
			emit(codeWriter, false);
			return out.toString();
		} catch (IOException e) {
			throw new AssertionError();
		}
	}

	public static ParameterSpec get(VariableElement element) {
		TypeName type = TypeName.get(element.asType());
		String name = element.getSimpleName().toString();
		return ParameterSpec.builder(type, name)
				.addModifiers(element.getModifiers())
				.build();
	}

	static List<ParameterSpec> parametersOf(ExecutableElement method) {
		List<ParameterSpec> result = new ArrayList<>();
		for (VariableElement parameter : method.getParameters()) {
			result.add(ParameterSpec.get(parameter));
		}
		return result;
	}

	public static Builder builder(TypeName type, String name, Modifier... modifiers) {
		checkNotNull(type, "type == null");
		checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
		return new Builder(type, name)
				.addModifiers(modifiers);
	}

	public static Builder builder(Type type, String name, Modifier... modifiers) {
		return builder(TypeName.get(type), name, modifiers);
	}

	public Builder toBuilder() {
		return toBuilder(type, name);
	}

	Builder toBuilder(TypeName type, String name) {
		Builder builder = new Builder(type, name);
		builder.annotations.addAll(annotations);
		builder.modifiers.addAll(modifiers);
		return builder;
	}

	public static final class Builder implements Initializer<ParameterSpec> {
		protected TypeName type;
		protected String name;

		protected Set<AnnotationSpec> annotations = new HashSet<>();
		protected Set<Modifier> modifiers = new HashSet<>();

		private Builder(TypeName type, String name) {
			this.type = type;
			this.name = name;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
			result = prime * result + ((modifiers == null) ? 0 : modifiers.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			return true;
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

		public ParameterSpec build() {
			return new ParameterSpec(this);
		}

		@Override
		public String getName() {
			return String.valueOf(name);
		}
	}
}
