package de.upb.swt.soot.java.core;

import com.google.common.graph.ImmutableGraph;
import de.upb.swt.soot.core.frontend.MethodSource;
import de.upb.swt.soot.core.frontend.OverridingMethodSource;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.Body;
import de.upb.swt.soot.core.model.Modifier;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.types.ClassType;
import java.util.Collections;
import java.util.function.Function;
import javax.annotation.Nonnull;

public class JavaSootMethod extends SootMethod {
  @Nonnull protected static final String CONSTRUCTOR_NAME = "<init>";
  @Nonnull protected static final String STATIC_INITIALIZER_NAME = "<clinit>";
  @Nonnull private final Iterable<AnnotationType> annotations;

  public JavaSootMethod(
      @Nonnull MethodSource source,
      @Nonnull MethodSignature methodSignature,
      @Nonnull Iterable<Modifier> modifiers,
      @Nonnull Iterable<ClassType> thrownExceptions,
      @Nonnull Iterable<AnnotationType> annotations) {
    super(source, methodSignature, modifiers, thrownExceptions);
    this.annotations = annotations;
  }

  /**
   * @return yes, if this function is a constructor. Please not that &lt;clinit&gt; methods are not
   *     treated as constructors in this methodRef.
   */
  public boolean isConstructor() {
    return this.getSignature().getName().equals(CONSTRUCTOR_NAME);
  }

  /** @return yes, if this function is a static initializer. */
  public boolean isStaticInitializer() {
    return this.getSignature().getName().equals(STATIC_INITIALIZER_NAME);
  }

  @Nonnull
  public Iterable<AnnotationType> getAnnotations() {
    return annotations;
  }

  @Nonnull
  @Override
  public JavaSootMethod withOverridingMethodSource(
      @Nonnull Function<OverridingMethodSource, OverridingMethodSource> overrider) {
    return new JavaSootMethod(
        overrider.apply(new OverridingMethodSource(methodSource)),
        getSignature(),
        getModifiers(),
        exceptions,
        getAnnotations());
  }

  @Nonnull
  @Override
  public JavaSootMethod withSource(@Nonnull MethodSource source) {
    return new JavaSootMethod(source, getSignature(), getModifiers(), exceptions, getAnnotations());
  }

  @Nonnull
  @Override
  public JavaSootMethod withModifiers(@Nonnull Iterable<Modifier> modifiers) {
    return new JavaSootMethod(
        methodSource, getSignature(), modifiers, getExceptionSignatures(), getAnnotations());
  }

  @Nonnull
  @Override
  public JavaSootMethod withThrownExceptions(@Nonnull Iterable<ClassType> thrownExceptions) {
    return new JavaSootMethod(
        methodSource, getSignature(), getModifiers(), thrownExceptions, getAnnotations());
  }

  @Nonnull
  public JavaSootMethod withAnnotations(@Nonnull Iterable<AnnotationType> annotations) {
    return new JavaSootMethod(
        methodSource, getSignature(), getModifiers(), getExceptionSignatures(), annotations);
  }

  @Nonnull
  @Override
  public JavaSootMethod withBody(@Nonnull Body body) {
    return new JavaSootMethod(
        new OverridingMethodSource(methodSource).withBody(body),
        getSignature(),
        getModifiers(),
        exceptions,
        getAnnotations());
  }

  /** @see OverridingMethodSource#withBodyStmts(Function) */
  @Nonnull
  @Override
  public JavaSootMethod withBodyStmts(
      @Nonnull Function<ImmutableGraph<Stmt>, ImmutableGraph<Stmt>> stmtModifier) {
    return new JavaSootMethod(
        new OverridingMethodSource(methodSource).withBodyStmts(stmtModifier),
        getSignature(),
        getModifiers(),
        exceptions,
        getAnnotations());
  }

  @Nonnull
  public static AnnotationOrSignatureStep builder() {
    return new JavaSootMethodBuilder();
  }

  public interface AnnotationOrSignatureStep extends MethodSourceStep {
    BuildStep withAnnotation(@Nonnull Iterable<AnnotationType> annotations);
  }

  /**
   * Defines a {@link JavaSootField.JavaSootFieldBuilder} to provide a fluent API.
   *
   * @author Markus Schmidt
   */
  public static class JavaSootMethodBuilder extends SootMethodBuilder
      implements AnnotationOrSignatureStep {

    private Iterable<AnnotationType> annotations = null;

    @Nonnull
    public Iterable<AnnotationType> getAnnotations() {
      return annotations != null ? annotations : Collections.emptyList();
    }

    @Override
    @Nonnull
    public BuildStep withAnnotation(@Nonnull Iterable<AnnotationType> annotations) {
      this.annotations = annotations;
      return this;
    }

    @Override
    @Nonnull
    public JavaSootMethod build() {
      return new JavaSootMethod(
          getSource(), getSignature(), getModifiers(), getThrownExceptions(), getAnnotations());
    }
  }
}
