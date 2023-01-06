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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;
import static com.squareup.javapoet.Util.checkState;

/** A generated field declaration. */
public final class FieldSpec extends Initializable<FieldSpec> {
  transient public TypeName type;
  transient public String name;
  transient public CodeBlock javadoc;
  transient public List<AnnotationSpec> annotations;
  transient public Set<Modifier> modifiers;
  transient public CodeBlock initializer;

  private FieldSpec(Builder builder) {
	initialize(builder);
  }
  
  @Override
  public void initialize(Initializer<FieldSpec> aBuilder) {
	Builder builder = (Builder) aBuilder;
    this.type = checkNotNull(builder.type, "type == null");
    this.name = checkNotNull(builder.name, "name == null");
    this.javadoc = builder.javadoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.initializer = (builder.initializer == null)
        ? CodeBlock.builder().build()
        : builder.initializer;
    super.initialize(builder);
  }

  public boolean hasModifier(Modifier modifier) {
	ensureInitialized();
    return modifiers.contains(modifier);
  }

  void emit(CodeWriter codeWriter, Set<Modifier> implicitModifiers) throws IOException {
	ensureInitialized();
    codeWriter.emitJavadoc(javadoc);
    codeWriter.emitAnnotations(annotations, false);
    codeWriter.emitModifiers(modifiers, implicitModifiers);
    codeWriter.emit("$T $L", type, name);
    if (!initializer.isEmpty()) {
      codeWriter.emit(" = ");
      codeWriter.emit(initializer);
    }
    codeWriter.emit(";\n");
  }

//  @Override public boolean equals(Object o) {
//    if (this == o) return true;
//    if (o == null) return false;
//    if (getClass() != o.getClass()) return false;
//    return toString().equals(o.toString());
//  }
//
//  @Override public int hashCode() {
//    return toString().hashCode();
//  }

  @Override public String toString() {
    StringBuilder out = new StringBuilder();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, Collections.emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
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
    Builder builder = new Builder(type, name);
    builder.javadoc.add(javadoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.initializer = initializer.isEmpty() ? null : initializer;
    return builder;
  }

  public static final class Builder implements Initializer<FieldSpec> {
    private final TypeName type;
    private final String name;

    private final CodeBlock.Builder javadoc = CodeBlock.builder();
    private CodeBlock initializer = null;

    public final Set<AnnotationSpec> annotations = new HashSet<>();
    public final Set<Modifier> modifiers = new HashSet<>();

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
 	    result = prime * result + ((initializer == null) ? 0 : initializer.hashCode());
 	    result = prime * result + ((javadoc == null) ? 0 : javadoc.hashCode());
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
 	    if (initializer == null) {
 	        if (other.initializer != null) {
 	            return false;
 	        }
 	    } else if (!initializer.equals(other.initializer)) {
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
 	    if (type == null) {
 	        if (other.type != null) {
 	            return false;
 	        }
 	    } else if (!type.equals(other.type)) {
 	        return false;
 	    }
 	    return true;
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
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder initializer(String format, Object... args) {
      return initializer(CodeBlock.of(format, args));
    }

    public Builder initializer(CodeBlock codeBlock) {
      checkState(this.initializer == null, "initializer was already set");
      this.initializer = checkNotNull(codeBlock, "codeBlock == null");
      return this;
    }

    public FieldSpec build() {
      return new FieldSpec(this);
    }
    
    @Override
 	public String getName() {
 	    return name;
 	}
  }
}
