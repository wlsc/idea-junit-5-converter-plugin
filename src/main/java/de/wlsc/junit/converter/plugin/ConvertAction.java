package de.wlsc.junit.converter.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;

public class ConvertAction extends AnAction {

  private static final JUnit5Converter CONVERTER = new JUnit5Converter();

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {

    VirtualFile data = e.getData(CommonDataKeys.VIRTUAL_FILE);

    if (CONVERTER.isFileNotWritable(data)) {
      Messages.showErrorDialog("Selected folder cannot be accessed", "Conversion Failed");
      return;
    }
    try {
      Files.walk(Paths.get(data.getPath()))
          .filter(Files::isRegularFile)
          .forEach(CONVERTER::convertToJunit5);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    Messages.showInfoMessage("Selected folder was converted!", "Conversion Successful");
    CONVERTER.refreshProject(e);
  }
}
