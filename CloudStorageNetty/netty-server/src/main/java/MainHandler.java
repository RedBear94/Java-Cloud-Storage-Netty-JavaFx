import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

// Обработчик (Inbound - на вход)
public class MainHandler extends ChannelInboundHandlerAdapter {

    private String clientName;
    private static int cnt = 0;
    private String serverStoragePath = "./netty-server/src/main/resources/";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Вызывется один раз при подключении клиента
        cnt++;
        clientName = "user#" + cnt;
        System.out.println("Client " + clientName + " connected");

        final File dir1 = new File(serverStoragePath + clientName + "/");
        if(!dir1.exists()) {
            dir1.mkdir();
        }
        ctx.writeAndFlush("/name " + clientName);
        ctx.writeAndFlush("Здравствуйте " + clientName + " вы подключились к серверу");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Полученение сообщений от клиента
        // получение и отправка данных происходит ввиде ByteBuf для первого и последнего handler-ра при отсутвии
        // кодирования и декодирования (в данном случае обрабатываются объекты получаемые от клиента)
        if (msg instanceof String) {
            System.out.println("message from client " + clientName + ": " + msg);
            ctx.writeAndFlush(msg);

            // Выполнение команд полученных от клиента
            String command = (String) msg;
            if(command.startsWith("/")) {
                if(command.startsWith("/download ")) {
                    downloadFile(ctx, command);
                } else if(command.equals("/info")) {
                    ctx.writeAndFlush("Клиент " + clientName + " запросил список файлов у сервера");
                    sendInfo(ctx, serverStoragePath + clientName + "/");
                } else if(command.startsWith("/delete ")){
                    deleteFile(ctx, command);
                }
                return;
            }

            ctx.writeAndFlush("Отправлена несущестующая команда");
        } else if (msg instanceof File){
            getFile(ctx, (File) msg);
        }
    }

    private void deleteFile(ChannelHandlerContext ctx, String command) {
        String [] op = command.split(" ");
        File file = new File(serverStoragePath + clientName + "/" + op[1]);
        if(file.exists()) {
            if(file.isFile()){
                file.delete();
                ctx.writeAndFlush("Файл был удалён");
            } else {
                deleteDirectory(file);
                ctx.writeAndFlush("Директория и всё её содержимое было удалено");
            }
        }
        else {
            ctx.writeAndFlush("Такого файла не существует");
        }
    }

    private void deleteDirectory(final File file) {
        if(file.isDirectory()) {
            String[] files = file.list();
            if ((null != files) && (files.length != 0)) {
                for (final String filename : files) {
                    deleteDirectory(new File(file.getAbsolutePath() + "/" + filename));
                }
            }
        }
        file.delete();
    }

    private void getFile(ChannelHandlerContext ctx, File msg) throws IOException {
        // Получение файлов с клиента
        File file = msg;

        // Создание/Дублирование путей файла с клиента
        String whereSaveFilePath = file.getPath();
        whereSaveFilePath = whereSaveFilePath.split(clientName, 2)[1]; // /dir1/4.txt
        String [] pathParts = whereSaveFilePath.split(Pattern.quote(File.separator), 0);
        createAllFileDirectories(pathParts, ctx);

        if (file.isFile()) {
            Files.copy(new FileInputStream(file),
                    Paths.get(serverStoragePath, clientName, whereSaveFilePath),
                    StandardCopyOption.REPLACE_EXISTING);
            ctx.writeAndFlush("Файл " + file.getName() + " загружен на сервер");
        } else if(file.isDirectory()){
            final File dir1 = new File(serverStoragePath + clientName + whereSaveFilePath);
            if(!dir1.exists()) {
                dir1.mkdir();
            }
            ctx.writeAndFlush("Директория создана");
        }
    }

    private void downloadFile(ChannelHandlerContext ctx, String command) {
        String [] op = command.split(" ");
        File file = new File(serverStoragePath + clientName + "/" + op[1]);
        if(file.exists()) {
            if (file.isFile()){
                ctx.writeAndFlush(file);
            } else if(file.isDirectory()){
                sendAllFileInDirectory(file.getPath(), ctx);
            }
        } else {
            ctx.writeAndFlush("Такого файла или директории не существует");
        }
    }

    private void sendAllFileInDirectory(String path, ChannelHandlerContext ctx) {
        File file = new File(path);
        for ( File f : file.listFiles() ){
            if ( f.isFile() ) {
                ctx.writeAndFlush(f);
            } else {
                sendAllFileInDirectory(f.getPath(), ctx);
            }
        }
    }

    private void sendInfo(ChannelHandlerContext ctx, String path) {
        File dir = new File(path); //path указывает на директорию
        for ( File file : dir.listFiles() ){
            if ( file.isFile() ) {
                ctx.writeAndFlush("[file] " + file.getPath().split("resources" + Pattern.quote(File.separator), 2)[1]);
            } else {
                ctx.writeAndFlush("[dir] " + file.getPath().split("resources" + Pattern.quote(File.separator), 2)[1]);
                sendInfo(ctx, path + file.getName() + "/");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush("Произошла ошибка, вы отключены от сервера");
        System.out.println("На клиенте " + clientName + " произошло исключение");
        ctx.close(); // отключение клиента при ошибке
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Вызов метода при отключении клиента от канала
        System.out.println("Client disconnected");
    }

    private void createAllFileDirectories(String[] pathParts, ChannelHandlerContext ctx) {
        for(int i = 0; i < pathParts.length - 1; i++){
            final File dir1 = new File(serverStoragePath + clientName + "/" + pathParts[i]);
            if(!dir1.exists()) {
                dir1.mkdir();
                ctx.writeAndFlush("Директория создана");
            }
        }
    }
}
