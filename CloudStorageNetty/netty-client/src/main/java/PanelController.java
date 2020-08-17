import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PanelController implements Initializable {
    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField pathFiled;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Инициализация данных столбцов таблицы
        // TableColumn<Что храним, Как выглядит> | FileInfo преобразуем в строку
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>(); // Столбец хранит тип файла
        // Задаютися хранимых значения в стобце
        // param - это одна запись в таблице (param.getValue() вернет FileInfo у него узнали тип, затем имя)
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24); // ширина столбца с типом файла

        // Аналогичным образом создаются остальные столбцы
        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        fileNameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        // FileInfo - в Long возращая размер файла из метода - getSize()
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        // Настройка сортировки для fileSizeColumn - по числу и с разделителями
        // И как выглядит ячейка в столбце
        // Идет перебор по колонкам
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                // Long item - значение ячейки, boolean empty - пустая ли ячейка
                protected void updateItem(Long item, boolean empty) {
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        // format("%,d") - берем число и добавляем разделитель
                        String text = String.format("%,d bytes", item);
                        if (item == -1) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        // Изменение формата даты и добавление колонки с датой изменения
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(160);

        // Добваление колонок
        filesTable.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        // Выбор диска
        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getClickCount() == 2){
                    // pathFiled.getText() - вернет текущий путь, через resolve добавили кусок пути - имя выбранного файла из таблицы
                    Path path = Paths.get(pathFiled.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if(Files.isDirectory(path)){
                        updateList(path);
                    }
                }
            }
        });

        //updateList(Paths.get("."));
    }

    public void updateList(Path path) {
        try {
            // Путь к директории приписали в текстовом поле | normalize() - убирает лишние символы
            pathFiled.setText(path.toString()); // path.normalize().toAbsolutePath().toString()

            // getItems - вернет список элекментов таблицы
            filesTable.getItems().clear();
            // Получили поток путей и добавили в таблицу
            // Перебираем поток через map и приводим к типу FileInfo
            // Собираем приведенные данные в лист через collect
            // Лист отдаем в таблицу
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort(); // сортирует по возрастанию
        } catch (IOException e){
            // Всплывающее окно с сообщением и кнопкой OK
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "По покакой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait(); // Показать кнопку
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathFiled.getText()).getParent();
        if(upperPath != null) {
            // Нельзя подняться выше своей рабочей папки пользователя
            if(!upperPath.getFileName().toString().equals("resources")){
                updateList(upperPath);
            }
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        // actionEvent.getSource() - возвращает источник события (ComboBox)
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedFilename(){
        if(!filesTable.isFocused()){
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public String getFilePath(){
        if(!filesTable.isFocused()){
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilePath().toString();
    }

    public String getCurrentPath(){
        return pathFiled.getText();
    }
}
