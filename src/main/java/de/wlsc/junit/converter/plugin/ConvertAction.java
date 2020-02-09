package de.wlsc.junit.converter.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertAction extends AnAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertAction.class);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {

    VirtualFile data = e.getData(CommonDataKeys.VIRTUAL_FILE);

    if (JUnit5Converter.INSTANCE.isFileNotWritable(data)) {
      Messages.showErrorDialog("Selected folder cannot be accessed", "Conversion Failed");
      return;
    }
    try {
      Files.walk(Paths.get(data.getPath()))
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().toLowerCase().endsWith("test.java"))
          .forEach(JUnit5Converter.INSTANCE::convertToJunit5);
    } catch (Exception ex) {
      LOGGER.error("Cannot apply JUnit 5 conversion");
    }

    Messages.showInfoMessage("Selected folder was converted!", "Conversion Successful");
    JUnit5Converter.INSTANCE.refreshProject(e);
  }
}
