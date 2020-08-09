import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class NettyController implements Initializable {
    private NettyNetwork network;
    private String clientName;

    @FXML
    TextField msgField;

    @FXML
    TextArea mainArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // NettyNetwork - принимает Callback из нескольких аргументов типа Object
        // обращение к этим аргументам происходит ввиде args[0] + выполняется -> - лямда выражение
        network = new NettyNetwork((args) -> {
            if (args[0] instanceof String) {
                // Загрузка файла на сервер
                String command = (String) args[0];
                if(command.startsWith("/")) {
                    if(command.startsWith("/name ")) {
                        String [] op = command.split(" ");
                        clientName = op[1];
                        final File dir1 = new File("./netty-client/src/main/resources/" + clientName + "/");
                        if(!dir1.exists()) {
                            dir1.mkdir();
                        }
                        return;
                    }
                    if(command.startsWith("/upload ")) {
                        String [] op = command.split(" ");
                        File file = new File("./netty-client/src/main/resources/" + clientName + "/" + op[1]);
                        if(file.exists()) {
                            network.sendMessage(file);
                        }
                    }
                }
                // Добавляем в mainArea команды отправленные серверу
                mainArea.appendText((String)args[0] + "\n");
            }
            else if (args[0] instanceof File){
                // Сохраняем файлы полученные от сервера
                File file = (File) args[0];
                try {
                    Files.copy(
                            new FileInputStream(file),
                            Paths.get("./netty-client/src/main/resources",clientName, "/", file.getName()),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mainArea.appendText("Файл скачан с сервера\n");
                }
            }
        });
    }

    public void sendCommand(ActionEvent actionEvent) {
        network.sendMessage(msgField.getText());
        msgField.clear();
        msgField.requestFocus(); // фокус на поле отправки комманд
    }

    public void exitAction(ActionEvent actionEvent) {
        network.close();
        Platform.exit();
    }
}
