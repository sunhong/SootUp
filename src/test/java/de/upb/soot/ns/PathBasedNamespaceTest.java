package de.upb.soot.ns;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import de.upb.soot.ns.classprovider.ClassSource;
import de.upb.soot.signatures.ClassSignature;

/** @author Manuel Benz created on 06.06.18 */
public class PathBasedNamespaceTest extends AbstractNamespaceTest {

  @Test(expected = IllegalArgumentException.class)
  public void failsOnFile() {
    // TODO adapt to new testing folder structure
    PathBasedNamespace.createForClassContainer(getClassProvider(),
        Paths.get("target/test-classes/de/upb/soot/ns/PathBasedNamespaceTest.class"));
  }

  public void classNotFound() {
    // TODO adapt to new testing folder structure
    Path baseDir = Paths.get("target/test-classes/");
    PathBasedNamespace pathBasedNamespace = PathBasedNamespace.createForClassContainer(getClassProvider(), baseDir);
    final ClassSignature sig = getSignatureFactory().getClassSignature("NotExisting", "de.upb.soot.ns");
    final Optional<ClassSource> classSource = pathBasedNamespace.getClassSource(sig);
    Assert.assertFalse(classSource.isPresent());
  }

  @Test
  public void testFolder() {
    // TODO adapt to new testing folder structure
    Path baseDir = Paths.get("target/classes/");
    PathBasedNamespace pathBasedNamespace = PathBasedNamespace.createForClassContainer(getClassProvider(), baseDir);
    final ClassSignature sig = getSignatureFactory().getClassSignature("PathBasedNamespace", "de.upb.soot.ns");
    testClassReceival(pathBasedNamespace, sig, MIN_CLASSES_FOUND);
  }

  @Test
  public void testJar() {
    // TODO adapt to new testing folder structure
    Path jar = Paths.get("target/test-classes/de/upb/soot/ns/Soot-4.0-SNAPSHOT.jar");
    PathBasedNamespace pathBasedNamespace = PathBasedNamespace.createForClassContainer(getClassProvider(), jar);
    final ClassSignature sig = getSignatureFactory().getClassSignature("PathBasedNamespace", "de.upb.soot.ns");
    testClassReceival(pathBasedNamespace, sig, MIN_CLASSES_FOUND);
  }
}
