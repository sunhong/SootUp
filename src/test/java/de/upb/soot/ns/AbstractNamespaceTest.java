package de.upb.soot.ns;

import java.util.Collection;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;

import de.upb.soot.ns.classprovider.ClassSource;
import de.upb.soot.ns.classprovider.IClassProvider;
import de.upb.soot.signatures.ClassSignature;
import de.upb.soot.signatures.SignatureFactory;

/**
 * @author Manuel Benz created on 07.06.18
 */
public abstract class AbstractNamespaceTest {

  protected static final int MIN_CLASSES_FOUND = 20;
  private SignatureFactory signatureFactory;
  private IClassProvider classProvider;

  @Before
  public void setUp() {
    signatureFactory = createSignatureFactory();
    classProvider = createClassProvider();
  }

  protected SignatureFactory getSignatureFactory() {
    return signatureFactory;
  }

  protected IClassProvider getClassProvider() {
    return classProvider;
  }

  protected SignatureFactory createSignatureFactory() {
    return new SignatureFactory() {
    };
  }

  protected IClassProvider createClassProvider() {
    return new DummyClassProvider(getSignatureFactory());
  }

  protected void testClassReceival(AbstractNamespace ns, ClassSignature sig, int minClassesFound) {
    final Optional<ClassSource> clazz = ns.getClassSource(sig);
    Assert.assertTrue(clazz.isPresent());
    Assert.assertEquals(sig, clazz.get().getClassSignature());

    final Collection<ClassSource> classSources = ns.getClassSources();
    Assert.assertNotNull(classSources);
    Assert.assertFalse(classSources.isEmpty());
    Assert.assertTrue(classSources.size() >= minClassesFound);
  }
}