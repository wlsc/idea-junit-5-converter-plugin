package de.wlsc.junit.converter.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public enum JUnit5Converter {
  INSTANCE;

  private static final ImmutableMap<String, String> NON_STATIC_REPLACERS = ImmutableMap.<String, String>builder()
      // imports
      .put("org.junit.Test", "org.junit.jupiter.api.Test")
      .put("org.junit.Before", "org.junit.jupiter.api.BeforeEach")
      .put("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
      .put("org.junit.After", "org.junit.jupiter.api.AfterEach")
      .put("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
      .put("org.junit.Ignore", "org.junit.jupiter.api.Disabled")
      // annotations
      .put("Before", "BeforeEach")
      .put("BeforeClass", "BeforeAll")
      .put("After", "AfterEach")
      .put("AfterClass", "AfterAll")
      .build();

  private static final ImmutableMap<String, String> STATIC_REPLACERS = ImmutableMap.<String, String>builder()
      // imports
      .put("org.junit.Assert.assertThat", "org.hamcrest.MatcherAssert.assertThat")
      .build();

  boolean isFileNotWritable(final VirtualFile data) {
    return data == null || !data.exists() || !data.isWritable();
  }

  void convertToJunit5(final Path path) {

    try {
      CompilationUnit unit = StaticJavaParser.parse(path);

      replaceImports(unit);
      replaceAnnotations(unit);

      Files.write(path, unit.toString().getBytes(UTF_8));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void replaceImports(final CompilationUnit compilationUnit) {
    for (ImportDeclaration importDeclaration : compilationUnit.findAll(ImportDeclaration.class)) {
      replaceIfPresent(importDeclaration, NON_STATIC_REPLACERS, false);
      replaceIfPresent(importDeclaration, STATIC_REPLACERS, true);
    }
  }

  private void replaceIfPresent(final ImportDeclaration importDeclaration,
      final ImmutableMap<String, String> staticReplacers,
      final boolean isStatic) {
    Optional.ofNullable(staticReplacers.get(importDeclaration.getNameAsString()))
        .map(name -> new ImportDeclaration(name, isStatic, false))
        .ifPresent(importDeclaration::replace);
  }

  private void replaceAnnotations(final CompilationUnit compilationUnit) {
    for (AnnotationExpr annotationExpr : compilationUnit.findAll(AnnotationExpr.class)) {

      String name = annotationExpr.getNameAsString();

      Optional.ofNullable(NON_STATIC_REPLACERS.get(name))
          .map(Name::new)
          .map(MarkerAnnotationExpr::new)
          .ifPresent(annotationExpr::replace);

      if ("Ignore".equals(name)) {
        replaceIgnore(annotationExpr);
      }

      if ("Test".equals(name)) {
        replaceTest(compilationUnit, annotationExpr);
      }
    }
  }

  private void replaceIgnore(final AnnotationExpr annotationExpr) {
    if (annotationExpr instanceof MarkerAnnotationExpr) {
      annotationExpr.replace(new MarkerAnnotationExpr(new Name("Disabled")));
    }
    if (annotationExpr instanceof SingleMemberAnnotationExpr) {
      StringLiteralExpr newValue = new StringLiteralExpr(
          ((SingleMemberAnnotationExpr) annotationExpr).getMemberValue().asStringLiteralExpr().getValue());
      annotationExpr.replace(new SingleMemberAnnotationExpr(new Name("Disabled"), newValue));
    }
  }

  private void replaceTest(final CompilationUnit compilationUnit, final AnnotationExpr annotationExpr) {

    if (annotationExpr instanceof NormalAnnotationExpr) {
      NormalAnnotationExpr normalAnnotationExpr = (NormalAnnotationExpr) annotationExpr;
      for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
        normalAnnotationExpr.getParentNode().ifPresent(node -> {
          for (Node childNode : node.getChildNodes()) {
            if (childNode instanceof BlockStmt) {
              String identifier = pair.getName().asString();
              if ("timeout".equals(identifier)) {
                wrapWithAssertTimeout(compilationUnit, (BlockStmt) childNode, pair.getValue());
              }
              if ("expected".equals(identifier)) {
                wrapWithExpected(compilationUnit, (BlockStmt) childNode, pair.getValue());
              }
            }
          }
        });
      }
      normalAnnotationExpr.replace(new MarkerAnnotationExpr(new Name("Test")));
    }
  }

  private void wrapWithAssertTimeout(final CompilationUnit compilationUnit, final BlockStmt oldBlockStmt,
      final Expression annotationValue) {

    BlockStmt previousBlockStmt = new BlockStmt(oldBlockStmt.getStatements());
    MethodCallExpr assertTimeout = new MethodCallExpr("assertTimeout",
        new MethodCallExpr("ofMillis", new LongLiteralExpr(annotationValue.asLongLiteralExpr().asLong())),
        new LambdaExpr(new NodeList<>(), previousBlockStmt));
    Statement timeoutStatement = new ExpressionStmt(assertTimeout);
    oldBlockStmt.setStatements(new NodeList<>(timeoutStatement));

    compilationUnit.addImport("java.time.Duration.ofMillis", true, false);
    compilationUnit.addImport("org.junit.jupiter.api.Assertions.assertTimeout", true, false);
  }

  private void wrapWithExpected(final CompilationUnit compilationUnit, final BlockStmt oldBlockStmt,
      final Expression annotationValue) {

    BlockStmt previousBlockStmt = new BlockStmt(oldBlockStmt.getStatements());
    MethodCallExpr assertTimeout = new MethodCallExpr("assertThrows",
        new ClassExpr(annotationValue.asClassExpr().getType()),
        new LambdaExpr(new NodeList<>(), previousBlockStmt));
    Statement timeoutStatement = new ExpressionStmt(assertTimeout);
    oldBlockStmt.setStatements(new NodeList<>(timeoutStatement));

    compilationUnit.addImport("org.junit.jupiter.api.Assertions.assertThrows", true, false);
  }

  void refreshProject(final AnActionEvent e) {
    Project project = e.getProject();
    if (isNull(project)) {
      return;
    }
    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    if (isNull(projectDir)) {
      return;
    }
    projectDir.refresh(false, true);
  }
}
