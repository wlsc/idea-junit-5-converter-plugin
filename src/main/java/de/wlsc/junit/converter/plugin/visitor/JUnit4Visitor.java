package de.wlsc.junit.converter.plugin.visitor;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JUnit4Visitor extends VoidVisitorAdapter<Void> {

  private static final ImmutableMap<String, String> MAPPERS = ImmutableMap.<String, String>builder()
      // imports
      .put("org.junit.Test", "org.junit.jupiter.api.Test")
      .put("org.junit.Before", "org.junit.jupiter.api.BeforeEach")
      .put("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
      .put("org.junit.After", "org.junit.jupiter.api.AfterEach")
      .put("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
      .put("org.junit.Ignore", "org.junit.jupiter.api.Disabled")
      .put("org.junit.Assume", "org.junit.jupiter.api.Assumptions")
      .put("org.junit.Assume.assumeTrue", "org.junit.jupiter.api.Assumptions.assumeTrue")
      .put("org.junit.Assume.assumeFalse", "org.junit.jupiter.api.Assumptions.assumeFalse")
      .put("org.junit.Assert", "org.junit.jupiter.api.Assertions")
      .put("org.junit.Assert.assertTrue", "org.junit.jupiter.api.Assertions.assertTrue")
      .put("org.junit.Assert.assertFalse", "org.junit.jupiter.api.Assertions.assertFalse")
      .put("org.junit.Assert.assertEquals", "org.junit.jupiter.api.Assertions.assertEquals")
      .put("org.junit.Assert.assertNotEquals", "org.junit.jupiter.api.Assertions.assertNotEquals")
      .put("org.junit.Assert.assertArrayEquals", "org.junit.jupiter.api.Assertions.assertArrayEquals")
      .put("org.junit.Assert.assertNotNull", "org.junit.jupiter.api.Assertions.assertNotNull")
      .put("org.junit.Assert.assertNull", "org.junit.jupiter.api.Assertions.assertNull")
      .put("org.junit.Assert.assertSame", "org.junit.jupiter.api.Assertions.assertSame")
      .put("org.junit.Assert.assertNotSame", "org.junit.jupiter.api.Assertions.assertNotSame")
      // spring specific
      .put("org.springframework.test.context.junit4.SpringRunner",
          "org.springframework.test.context.junit.jupiter.SpringExtension")
      .put("org.springframework.test.context.junit4.SpringJUnit4ClassRunner",
          "org.springframework.test.context.junit.jupiter.SpringExtension")
      // mockito specific
      .put("org.mockito.junit.MockitoJUnitRunner", "org.mockito.junit.jupiter.MockitoExtension")
      // annotations
      .put("Before", "BeforeEach")
      .put("BeforeClass", "BeforeAll")
      .put("After", "AfterEach")
      .put("AfterClass", "AfterAll")
      .build();

  @Override
  public void visit(final ImportDeclaration importDeclaration, final Void arg) {
    replaceImportIfPresent(importDeclaration);
    super.visit(importDeclaration, arg);
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration classOrInterfaceDeclaration, final Void arg) {
    generateDisplayNameAnnotationIfNotExist(classOrInterfaceDeclaration);
    super.visit(classOrInterfaceDeclaration, arg);
  }

  @Override
  public void visit(final MarkerAnnotationExpr markerAnnotationExpr, final Void arg) {
    replaceAnnotationNameIfPresent(markerAnnotationExpr);
    replaceIgnoreIfPresent(markerAnnotationExpr);
    super.visit(markerAnnotationExpr, arg);
  }

  @Override
  public void visit(final SingleMemberAnnotationExpr singleMemberAnnotationExpr, final Void arg) {
    replaceMockitoRunner(singleMemberAnnotationExpr);
    replaceSpringRunner(singleMemberAnnotationExpr);
    replaceAnnotationNameIfPresent(singleMemberAnnotationExpr);
    replaceIgnoreWithParameterIfPresent(singleMemberAnnotationExpr);
    super.visit(singleMemberAnnotationExpr, arg);
  }

  @Override
  public void visit(final NormalAnnotationExpr normalAnnotationExpr, final Void arg) {
    replaceAnnotationNameIfPresent(normalAnnotationExpr);
    replaceTestIfPresent(normalAnnotationExpr);
    super.visit(normalAnnotationExpr, arg);
  }

  @Override
  public void visit(final MethodCallExpr methodCallExpr, final Void arg) {
    moveMessageArgumentIfPresent(methodCallExpr);
    super.visit(methodCallExpr, arg);
  }

  @Override
  public void visit(final MethodDeclaration methodDeclaration, final Void arg) {
    generateDisplayNameIfTestMethod(methodDeclaration);
    super.visit(methodDeclaration, arg);
  }

  private void generateDisplayNameAnnotationIfNotExist(final ClassOrInterfaceDeclaration classDeclaration) {
    if (classDeclaration.getAnnotationByName("DisplayName").isPresent()) {
      return;
    }

    SingleMemberAnnotationExpr displayName = createCapitalizedDisplayNameBy(classDeclaration.getNameAsString());
    classDeclaration.addAnnotation(displayName);

    classDeclaration.findCompilationUnit()
        .ifPresent(unit -> unit.addImport("org.junit.jupiter.api.DisplayName"));
  }

  private void replaceMockitoRunner(final SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
    replaceRunWithAnnotation(singleMemberAnnotationExpr, "MockitoExtension.class",
        "MockitoJUnitRunner"::equals);
  }

  private void replaceSpringRunner(final SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
    replaceRunWithAnnotation(singleMemberAnnotationExpr, "SpringExtension.class",
        className -> "SpringRunner".equals(className) || "SpringJUnit4ClassRunner".equals(className));
  }

  private void replaceRunWithAnnotation(final SingleMemberAnnotationExpr annotationExpr,
      final String newClassValue, final Predicate<String> predicate) {

    Stream.of(annotationExpr)
        .filter(expr -> "RunWith".equals(expr.getNameAsString()))
        .filter(expr -> predicate.test(expr.getMemberValue().asClassExpr().getType().asString()))
        .map(expr -> newClassValue)
        .map(NameExpr::new)
        .map(clazzName -> new SingleMemberAnnotationExpr(new Name("ExtendWith"), clazzName))
        .forEach(annotationExpr::replace);

    annotationExpr.findCompilationUnit()
        .ifPresent(unit -> unit.addImport("org.junit.jupiter.api.extension.ExtendWith"));
  }

  private void generateDisplayNameIfTestMethod(final MethodDeclaration methodDeclaration) {
    if (methodDeclaration.getAnnotationByName("DisplayName").isPresent() ||
        (!methodDeclaration.getAnnotationByName("Test").isPresent() &&
            !methodDeclaration.getAnnotationByName("ParameterizedTest").isPresent())) {
      return;
    }

    String methodName = methodDeclaration.getName().asString();
    methodDeclaration.addAnnotation(createCapitalizedDisplayNameBy(methodName));

    methodDeclaration.findCompilationUnit()
        .ifPresent(unit -> unit.addImport("org.junit.jupiter.api.DisplayName"));
  }

  private SingleMemberAnnotationExpr createCapitalizedDisplayNameBy(final String methodName) {

    String[] words = splitByCharacterTypeCamelCase(methodName);
    words[0] = capitalizeFirstCharNormalizeOthers(words[0]);
    String displayNameText = join(words, ' ');

    return new SingleMemberAnnotationExpr(new Name("DisplayName"),
        new StringLiteralExpr(displayNameText));
  }

  private String capitalizeFirstCharNormalizeOthers(final String word) {
    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
  }

  private void moveMessageArgumentIfPresent(final MethodCallExpr methodCallExpr) {

    if (methodCallExpr.getArguments().size() < 1) {
      return;
    }

    String methodName = methodCallExpr.getNameAsString();

    if ("assumeTrue".equals(methodName) || "assumeFalse".equals(methodName)) {
      pushFirstAsLastArgument(methodCallExpr, "Assumptions");
    }

    if ("assertTrue".equals(methodName) || "assertFalse".equals(methodName) || "assertNotNull".equals(methodName) ||
        "assertArrayEquals".equals(methodName) || "assertEquals".equals(methodName) ||
        "assertNotEquals".equals(methodName)) {
      pushFirstAsLastArgument(methodCallExpr, "Assertions");
    }
  }

  private void pushFirstAsLastArgument(final MethodCallExpr methodCallExpr, final String newPrefixName) {
    NodeList<Expression> arguments = methodCallExpr.getArguments();

    if (arguments.get(0).isStringLiteralExpr()) {
      arguments.add(arguments.get(0));
      arguments.remove(0);
    }

    methodCallExpr.getScope()
        .ifPresent(expression -> methodCallExpr.setScope(new NameExpr(newPrefixName)));
  }

  public void replaceIgnoreIfPresent(final MarkerAnnotationExpr markerAnnotationExpr) {
    Stream.of(markerAnnotationExpr)
        .filter(expr -> "Ignore".equals(expr.getNameAsString()))
        .map(expr -> new Name("Disabled"))
        .map(MarkerAnnotationExpr::new)
        .forEach(markerAnnotationExpr::replace);
  }

  private void replaceIgnoreWithParameterIfPresent(final SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
    Stream.of(singleMemberAnnotationExpr)
        .filter(expr -> "Ignore".equals(expr.getNameAsString()))
        .map(SingleMemberAnnotationExpr::getMemberValue)
        .map(Expression::asStringLiteralExpr)
        .map(LiteralStringValueExpr::getValue)
        .map(StringLiteralExpr::new)
        .map(value -> new SingleMemberAnnotationExpr(new Name("Disabled"), value))
        .forEach(singleMemberAnnotationExpr::replace);
  }

  private void replaceTestIfPresent(final NormalAnnotationExpr normalAnnotationExpr) {
    if (!"Test".equals(normalAnnotationExpr.getNameAsString())) {
      return;
    }
    normalAnnotationExpr.getParentNode().ifPresent(node -> {
      for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
        for (Node childNode : node.getChildNodes()) {
          if (childNode instanceof BlockStmt) {
            String identifier = pair.getName().asString();
            if ("timeout".equals(identifier)) {
              wrapWithAssertTimeout((BlockStmt) childNode, pair.getValue());
            }
            if ("expected".equals(identifier)) {
              wrapWithExpected((BlockStmt) childNode, pair.getValue());
            }
          }
        }
      }
    });
    normalAnnotationExpr.replace(new MarkerAnnotationExpr(new Name("Test")));
  }

  private void replaceImportIfPresent(final ImportDeclaration importDeclaration) {
    Optional.ofNullable(MAPPERS.get(importDeclaration.getNameAsString()))
        .map(name -> new ImportDeclaration(name, importDeclaration.isStatic(), false))
        .ifPresent(importDeclaration::replace);
  }

  private void replaceAnnotationNameIfPresent(final AnnotationExpr annotationExpr) {
    Optional.ofNullable(MAPPERS.get(annotationExpr.getNameAsString()))
        .map(Name::new)
        .map(MarkerAnnotationExpr::new)
        .ifPresent(annotationExpr::replace);
  }

  private void wrapWithAssertTimeout(final BlockStmt oldBlockStmt, final Expression annotationValue) {

    BlockStmt previousBlockStmt = new BlockStmt(oldBlockStmt.getStatements());
    MethodCallExpr assertTimeout = new MethodCallExpr("assertTimeout",
        new MethodCallExpr("ofMillis", new LongLiteralExpr(annotationValue.asLongLiteralExpr().asLong())),
        new LambdaExpr(new NodeList<>(), previousBlockStmt));
    Statement timeoutStatement = new ExpressionStmt(assertTimeout);
    oldBlockStmt.setStatements(new NodeList<>(timeoutStatement));

    oldBlockStmt.findCompilationUnit().ifPresent(unit -> {
      unit.addImport("java.time.Duration.ofMillis", true, false);
      unit.addImport("org.junit.jupiter.api.Assertions.assertTimeout", true, false);
    });
  }

  private void wrapWithExpected(final BlockStmt oldBlockStmt, final Expression annotationValue) {

    BlockStmt previousBlockStmt = new BlockStmt(oldBlockStmt.getStatements());
    MethodCallExpr assertTimeout = new MethodCallExpr("assertThrows",
        new ClassExpr(annotationValue.asClassExpr().getType()),
        new LambdaExpr(new NodeList<>(), previousBlockStmt));
    Statement timeoutStatement = new ExpressionStmt(assertTimeout);
    oldBlockStmt.setStatements(new NodeList<>(timeoutStatement));

    oldBlockStmt.findCompilationUnit()
        .ifPresent(unit -> unit.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false));
  }
}
