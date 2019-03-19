package de.upb.soot.frontends.asm;

import de.upb.soot.core.AbstractClass;
import de.upb.soot.core.ClassType;
import de.upb.soot.core.IMethod;
import de.upb.soot.core.Modifier;
import de.upb.soot.core.ResolvingLevel;
import de.upb.soot.core.SootClass;
import de.upb.soot.core.SootField;
import de.upb.soot.core.SootMethod;
import de.upb.soot.frontends.ClassSource;
import de.upb.soot.frontends.IClassSourceContent;
import de.upb.soot.frontends.ResolveException;
import de.upb.soot.signatures.DefaultSignatureFactory;
import de.upb.soot.signatures.FieldSignature;
import de.upb.soot.signatures.JavaClassSignature;
import de.upb.soot.signatures.MethodSignature;
import de.upb.soot.signatures.TypeSignature;
import de.upb.soot.views.IView;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

class AsmClassClassSourceContent extends org.objectweb.asm.tree.ClassNode implements IClassSourceContent {

  private final ClassSource classSource;

  public AsmClassClassSourceContent(@Nonnull ClassSource classSource) {
    super(AsmUtil.SUPPORTED_ASM_OPCODE);
    
    this.classSource = classSource;
    
    // FIXME: maybe delete class reading
    AsmUtil.initAsmClassSource(classSource, this);
  }

  @Override
  @Nonnull
  public AbstractClass resolveClass(
    @Nonnull ResolvingLevel level,
    @Nonnull IView view
  ) throws AsmFrontendException {
    
    JavaClassSignature cs = view.getSignatureFactory().getClassSignature(this.signature);
    SootClass.SootClassSurrogateBuilder builder;
    
    // FIXME: currently ugly because, the original class is always re-resolved but never copied...
    switch (level) {
      case DANGLING:
        builder = (SootClass.SootClassSurrogateBuilder) resolveDangling(view, cs);
        break;

      case HIERARCHY:
        builder = (SootClass.SootClassSurrogateBuilder) resolveHierarchy(view, cs);
        break;

      case SIGNATURES:
        builder = (SootClass.SootClassSurrogateBuilder) resolveSignature(view, cs);
        break;

      case BODIES:
        builder = (SootClass.SootClassSurrogateBuilder) resolveBody(view, cs);
        break;
        
      default:
        throw new AsmFrontendException("Unsupported resolving level \"" + level + "\".");
    }

    return builder.build();
  }

  // FIXME: Parameter `cs` is unused
  @Nonnull
  private SootClass.HierachyStep resolveDangling(
    @Nonnull IView view,
    @Nonnull JavaClassSignature cs) {

    return SootClass.surrogateBuilder().dangling(this.classSource, ClassType.Library);
  }

  @Nonnull
  private SootClass.SignatureStep resolveHierarchy(
    @Nonnull IView view,
    @Nonnull JavaClassSignature cs
  ) throws AsmFrontendException {
    
    SootClass sootClass = (SootClass) view.getClass(cs)
        .orElse(null);
  
    SootClass.HierachyStep danglingStep;

    if (sootClass == null || sootClass.resolvingLevel().isLowerThan(ResolvingLevel.DANGLING)) { // FIXME: [JMP] This expression is always `false`, because `DANGLING` is the lowest level.
      // FIXME: do the setting stuff again...
      danglingStep = resolveDangling(view, cs);
    } else {
      danglingStep = SootClass.fromExisting(sootClass);
    }
  
    // Add super class
    JavaClassSignature mySuperClass = DefaultSignatureFactory.getInstance().getClassSignature(AsmUtil.toQualifiedName(superName));
  
    // Add interfaces
    Set<JavaClassSignature> interfaces = new HashSet<>(AsmUtil.asmIdToSignature(this.interfaces));
  
    return danglingStep.hierachy(mySuperClass, interfaces, EnumSet.noneOf(Modifier.class), null);
  }

  @Nonnull 
  private SootClass.BodyStep resolveSignature(
    @Nonnull IView view,
    @Nonnull JavaClassSignature cs
  ) throws AsmFrontendException {
    
    SootClass.SignatureStep signatureStep;
  
    SootClass sootClass = (SootClass) view.getClass(cs)
        .orElseThrow(() -> new AsmFrontendException(String.format("Cannot resolve class %s", cs)));
    
    if (sootClass.resolvingLevel().isLowerThan(ResolvingLevel.HIERARCHY)) {
      signatureStep = resolveHierarchy(view, cs);
    } else {
      signatureStep = SootClass.fromExisting(sootClass);
    }

    // region Add fields
  
    Set<SootField> fields = new HashSet<>();
    
    // FIXME: add support for annotation
    for (FieldNode fieldNode : this.fields) {
      String fieldName = fieldNode.name;
      TypeSignature fieldType = AsmUtil.toJimpleType(fieldNode.desc);
      FieldSignature fieldSignature = view.getSignatureFactory().getFieldSignature(fieldName, sootClass.getSignature(), fieldType);
      EnumSet<Modifier> modifiers = AsmUtil.getModifiers(fieldNode.access);
      SootField sootField = new SootField(fieldSignature, modifiers);
      
      fields.add(sootField);
    }
    
    // endregion /Add fields/

    // region Add methods
  
    Set<IMethod> methods = new HashSet<>();
    
    for (MethodNode methodSource : this.methods) {

      if (!(methodSource instanceof AsmMethodSourceContent)) {
        throw new AsmFrontendException(String.format("Failed to create Method Signature %s", methodSource));
      }
      AsmMethodSourceContent asmClassClassSourceContent = (AsmMethodSourceContent) methodSource;

      List<JavaClassSignature> exceptions = new ArrayList<>();
      Iterable<JavaClassSignature> exceptionsSignatures = AsmUtil.asmIdToSignature(methodSource.exceptions);

      for (JavaClassSignature exceptionSig : exceptionsSignatures) {
        exceptions.add(exceptionSig);
      }
      String methodName = methodSource.name;
      EnumSet<Modifier> modifiers = AsmUtil.getModifiers(methodSource.access);
      List<TypeSignature> sigTypes = AsmUtil.toJimpleSignatureDesc(methodSource.desc);
      TypeSignature retType = sigTypes.remove(sigTypes.size() - 1);

      MethodSignature methodSignature =
          view.getSignatureFactory().getMethodSignature(methodName, cs, retType, sigTypes);
  
      SootMethod sootMethod =
          SootMethod.builder()
              .withSource(asmClassClassSourceContent)
              .withSignature(methodSignature)
              .withModifiers(modifiers)
              .withThrownExceptions(exceptions)
              .build();
      
      methods.add(sootMethod);
    }
    
    // endregion /Add methods/
    
    return signatureStep.signature(fields, methods);
  }
  
  @Override
  @Nonnull
  public Iterable<SootMethod> resolveMethods(@Nonnull JavaClassSignature signature) throws ResolveException {
    // region Add methods
  
    List<SootMethod> methods = new ArrayList<>(this.methods.size());
    
    for (MethodNode methodSource : this.methods) {

      if (!(methodSource instanceof AsmMethodSourceContent)) {
        throw new AsmFrontendException(String.format("Failed to create Method Signature %s", methodSource));
      }
      AsmMethodSourceContent asmClassClassSourceContent = (AsmMethodSourceContent) methodSource;

      List<JavaClassSignature> exceptions = new ArrayList<>();
      Iterable<JavaClassSignature> exceptionsSignatures = AsmUtil.asmIdToSignature(methodSource.exceptions);

      for (JavaClassSignature exceptionSig : exceptionsSignatures) {
        exceptions.add(exceptionSig);
      }
      String methodName = methodSource.name;
      EnumSet<Modifier> modifiers = AsmUtil.getModifiers(methodSource.access);
      List<TypeSignature> sigTypes = AsmUtil.toJimpleSignatureDesc(methodSource.desc);
      TypeSignature retType = sigTypes.remove(sigTypes.size() - 1);

      MethodSignature methodSignature =
          DefaultSignatureFactory.getInstance()
              .getMethodSignature(methodName, signature, retType, sigTypes);
  
      SootMethod sootMethod =
          SootMethod.builder()
              .withSource(asmClassClassSourceContent)
              .withSignature(methodSignature)
              .withModifiers(modifiers)
              .withThrownExceptions(exceptions)
              .build();
      
      methods.add(sootMethod);
    }
    
    // endregion /Add methods/
    
    return methods;
  }
  
  @Override
  @Nonnull 
  public Iterable<SootField> resolveFields(
      @Nonnull JavaClassSignature signature
  ) throws ResolveException {
    // region Add fields
  
    List<SootField> fields = new ArrayList<>(this.fields.size());
    
    // FIXME: add support for annotation
    for (FieldNode fieldNode : this.fields) {
      String fieldName = fieldNode.name;
      TypeSignature fieldType = AsmUtil.toJimpleType(fieldNode.desc);
      FieldSignature fieldSignature = 
          DefaultSignatureFactory.getInstance().getFieldSignature(fieldName, signature, fieldType);
      EnumSet<Modifier> modifiers = AsmUtil.getModifiers(fieldNode.access);
      SootField sootField = new SootField(fieldSignature, modifiers);
      
      fields.add(sootField);
    }
    
    // endregion /Add fields/
    
    return fields;
  }
  
  @Nonnull 
  private SootClass.Build resolveBody(
    @Nonnull IView view,
    @Nonnull JavaClassSignature cs
  ) throws AsmFrontendException {
    
    SootClass sootClass =
      (SootClass) view.getClass(cs)
        .orElseThrow(() -> new AsmFrontendException(String.format("Cannot resolve class %s", cs)));
    
    SootClass.BodyStep bodyStep =
      sootClass.resolvingLevel().isLowerThan(ResolvingLevel.SIGNATURES)
        ? resolveSignature(view, cs)
        : SootClass.fromExisting(sootClass);
    
    // TODO: resolve the method bodies
    return bodyStep.bodies("dummy");
  }

  @Override
  @Nonnull 
  public MethodVisitor visitMethod(
    int access,
    @Nonnull String name,
    @Nonnull String desc,
    @Nonnull String signature,
    @Nonnull String[] exceptions) {

    AsmMethodSourceContent mn = new AsmMethodSourceContent(access, name, desc, signature, exceptions);
    methods.add(mn);
    return mn;
  }
}
