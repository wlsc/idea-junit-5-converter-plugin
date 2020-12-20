package de.wlsc.junit.converter.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertAction extends AnAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertAction.class);

  @Override
  public void actionPerformed(@NotNull final AnActionEvent actionEvent) {

    VirtualFile data = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
    Project project = Objects.requireNonNull(actionEvent.getProject(), "Project must be not null");
    PsiManager manager = PsiManager.getInstance(project);
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    if (JUnit5Converter.INSTANCE.isFileNotWritable(data)) {
      Messages.showErrorDialog("Selected folder cannot be accessed", "Conversion Failed");
      return;
    }
    try {
      Files.walk(Paths.get(data.getPath()))
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().toLowerCase().endsWith("test.java"))
          .peek(JUnit5Converter.INSTANCE::convertToJunit5)
          .map(Path::toUri)
          .map(this::getUrl)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(VfsUtil::findFileByURL)
          .filter(Objects::nonNull)
          .map(manager::findFile)
          .filter(Objects::nonNull)
          .forEach(file -> ApplicationManager.getApplication().invokeLater(() -> {
            documentManager.commitAllDocumentsUnderProgress();
            WriteCommandAction.runWriteCommandAction(project, () -> {
              codeStyleManager.reformat(file);
            });
          }));
    } catch (Exception ex) {
      LOGGER.error("Cannot apply JUnit 5 conversion", ex);
    }

    Messages.showInfoMessage("Selected folder/file was converted!", "Conversion Successful");
    JUnit5Converter.INSTANCE.refreshProject(actionEvent);
  }

  @NotNull
  private Optional<URL> getUrl(final URI uri) {
    try {
      return Optional.of(uri.toURL());
    } catch (MalformedURLException e) {
      LOGGER.error("Cannot get URL from URI");
    }
    return Optional.empty();
  }
}
