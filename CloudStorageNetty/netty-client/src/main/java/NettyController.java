import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


import java.net.URL;
import java.util.ResourceBundle;

public class NettyController implements Initializable {
    private NettyNetwork network;

    @FXML
    TextField msgField;

    @FXML
    TextArea mainArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        network = new NettyNetwork((args) -> {
            if (args[0] instanceof String) {
                mainArea.appendText((String)args[0] + "\n"); // Добавляем в mainArea команды отправленные серверу
            }
        });
    }

    public void sendCommand(ActionEvent actionEvent) {
        network.sendMessage(msgField.getText());
        msgField.clear();
        msgField.requestFocus(); // фокус на поле отправки комманд
    }

    public void exitAction(ActionEvent actionEvent) {

    }
}
