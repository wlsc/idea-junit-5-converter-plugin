package de.wlsc.junit.converter.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class ConvertAction extends AnAction {

  private static final ImmutableMap<String, String> JUST_REPLACERS = ImmutableMap.<String, String>builder()
      // imports
      .put("org.junit.Test", "org.junit.jupiter.api.Test")
      .put("org.junit.Before", "org.junit.jupiter.api.BeforeEach")
      .put("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
      .put("org.junit.After", "org.junit.jupiter.api.AfterEach")
      .put("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
      // annotations
      .put("Before", "BeforeEach")
      .put("BeforeClass", "BeforeAll")
      .put("After", "AfterEach")
      .put("AfterClass", "AfterAll")
      .build();

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {

    VirtualFile data = e.getData(CommonDataKeys.VIRTUAL_FILE);

    if (!isFileWritable(data)) {
      Messages.showErrorDialog("Selected folder cannot be accessed", "Conversion Failed");
      return;
    }
    try {
      Files.walk(Paths.get(data.getPath()))
          .filter(Files::isRegularFile)
          .forEach(this::convertToJunit5);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    Messages.showInfoMessage("Selected folder was converted!", "Conversion Successful");
    refreshProject(e);
  }

  private boolean isFileWritable(final VirtualFile data) {
    return data != null && data.exists() && data.isWritable();
  }

  private void convertToJunit5(final Path path) {

    CompilationUnit unit;
    try {
      unit = StaticJavaParser.parse(path);

      replaceImports(unit);
      replaceAnnotations(unit);

      Files.write(path, unit.toString().getBytes(UTF_8));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void replaceImports(final CompilationUnit compilationUnit) {
    for (ImportDeclaration importDeclaration : compilationUnit.findAll(ImportDeclaration.class)) {
      Optional.ofNullable(JUST_REPLACERS.get(importDeclaration.getNameAsString()))
          .ifPresent(replaceWith -> importDeclaration.replace(new ImportDeclaration(replaceWith, false, false)));
    }
  }

  private void replaceAnnotations(final CompilationUnit compilationUnit) {
    for (AnnotationExpr annotationExpr : compilationUnit.findAll(AnnotationExpr.class)) {
      Optional.ofNullable(JUST_REPLACERS.get(annotationExpr.getNameAsString()))
          .ifPresent(replaceWith -> annotationExpr.replace(new MarkerAnnotationExpr(new Name(replaceWith))));
    }
  }

  private void refreshProject(final AnActionEvent e) {
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
