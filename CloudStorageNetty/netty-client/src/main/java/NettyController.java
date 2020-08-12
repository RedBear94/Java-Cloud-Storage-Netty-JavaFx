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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class NettyController implements Initializable {
    private static NettyNetwork network;
    private String clientName;
    private final String clientStoragePath = "./netty-client/src/main/resources/";

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
                        createUserDirectory(command);
                        return;
                    }
                    if(command.startsWith("/upload ")) {
                        sendFile(command);
                    }
                    if(command.startsWith("/local_info")) {
                        mainArea.appendText((String)args[0] + "\n");
                        printLocalFiles(clientStoragePath + clientName);
                        return;
                    }
                }
                // Добавляем в mainArea команды отправленные серверу
                mainArea.appendText((String)args[0] + "\n");
            }
            else if (args[0] instanceof File){
                getFile(args[0]);
            }
        });
    }

    private void printLocalFiles(String path) {
        File dir = new File(path);
        if(dir.exists() && dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile()) {
                    mainArea.appendText("[file] " + file.getPath().split("resources" +
                            Pattern.quote(File.separator), 2)[1] + "\n");
                } else {
                    mainArea.appendText("[dir] " + file.getPath().split("resources" +
                            Pattern.quote(File.separator), 2)[1] + "\n");
                    printLocalFiles(path + file.getName() + "/");
                }
            }
        }
    }

    private void getFile(Object arg) {
        File file = (File) arg;

        // Создание/Дублирование путей файла с сервера
        String whereSaveFilePath = file.getPath();
        whereSaveFilePath = whereSaveFilePath.split(clientName, 2)[1];
        String [] pathParts = whereSaveFilePath.split(Pattern.quote(File.separator), 0);
        createAllFileDirectories(pathParts);

        if (file.isFile()) {
            try {
                Files.copy(new FileInputStream(file),
                        Paths.get(clientStoragePath, clientName, whereSaveFilePath),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainArea.appendText("Файл " + file.getName() + " скачан с сервера\n");
        } else if(file.isDirectory()){
            final File dir1 = new File(clientStoragePath + clientName + whereSaveFilePath);
            if(!dir1.exists()) {
                dir1.mkdir();
            }
            mainArea.appendText("Директория " + file.getName() +" скачена с сервера\n");
        }
    }

    private void createAllFileDirectories(String[] pathParts) {
        for(int i = 0; i < pathParts.length - 1; i++){
            final File dir1 = new File(clientStoragePath + clientName + "/" + pathParts[i]);
            if(!dir1.exists()) {dir1.mkdir();}
        }
    }

    private void sendFile(String command) {
        String [] op = command.split(" ");
        File file = new File(clientStoragePath + clientName + "/" + op[1]);
        if(file.exists()) {
            if (file.isFile()){
                network.sendMessage(file);
            } else if(file.isDirectory()){
                sendAllFileInDirectory(file.getPath());
            }
        } else {
            mainArea.appendText("Файл или директория не существует\n");
        }
    }

    private void sendAllFileInDirectory(String path) {
        File file = new File(path);
        for ( File f : file.listFiles() ){
            if ( f.isFile() ) {
                network.sendMessage(f);
            } else {
                sendAllFileInDirectory(f.getPath());
            }
        }
    }


    private void createUserDirectory(String command) {
        String [] op = command.split(" ");
        clientName = op[1];
        final File dir1 = new File(clientStoragePath + clientName + "/");
        if(!dir1.exists()) {
            dir1.mkdir();
        }
        return;
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

    public static void stop(){
        network.close();
        Platform.exit();
    }
}
