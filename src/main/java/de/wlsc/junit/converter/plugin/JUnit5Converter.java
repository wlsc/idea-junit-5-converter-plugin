package de.wlsc.junit.converter.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import de.wlsc.junit.converter.plugin.visitor.JUnit4Visitor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JUnit5Converter {
  INSTANCE(new JUnit4Visitor());

  private static final Logger LOGGER = LoggerFactory.getLogger(JUnit5Converter.class);
  private final JUnit4Visitor jUnit4Visitor;

  JUnit5Converter(final JUnit4Visitor jUnit4Visitor) {
    this.jUnit4Visitor = jUnit4Visitor;
  }

  boolean isFileNotWritable(final VirtualFile data) {
    return data == null || !data.exists() || !data.isWritable();
  }

  void convertToJunit5(final Path path) {
    try {
      CompilationUnit unit = StaticJavaParser.parse(path);
      unit.accept(jUnit4Visitor, null);

      Files.write(path, unit.toString().getBytes(UTF_8));

    } catch (IOException e) {
      LOGGER.error("Cannot read/write file", e);
    }
  }

  void refreshProject(final AnActionEvent actionEvent) {
    Project project = actionEvent.getProject();
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
