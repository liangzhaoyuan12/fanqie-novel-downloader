package com.lzy12;
import com.google.gson.Gson;
import org.jsoup.nodes.Document;
import org.jsoup.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.awt.*;
import java.io.*;


import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
public class Main {
    private static final String bookBaseUrl="https://fanqienovel.com/reader/";
    private static final String rootBaseUrl="https://fanqienovel.com/page/";
    private static final String rootUrl="https://fanqienovel.com";
    private static final String testChapterID="7039248554300277287";
    private static String cookie="";
    private static Map<String,String> config=new HashMap<>();
    private static final File cookieJson=new File("date/cookie.json");
    private static final File configJson=new File("date/config.json");
    private static final Scanner scanner=new Scanner(System.in);
    public static void main(String[] args) throws InterruptedException, IOException {
        initialization();//初始化，已获得cookie，已获得config文件的Map，并检测了是否使用代理
        mainMenu();
    }
    public static void writeCookieToJson(String cookie){
        try {
            cookieJsonStruct cj = new cookieJsonStruct(cookie);
            Gson gson = new Gson();
            String json = gson.toJson(cj);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(cookieJson));
            bufferedWriter.write(json);
            bufferedWriter.flush();
            bufferedWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static String readCookieJson() throws IOException{
        BufferedReader bufferedReader=new BufferedReader(new FileReader(cookieJson));
        Gson gson=new Gson();
        cookieJsonStruct cookieJsonStruct=gson.fromJson(bufferedReader,com.lzy12.cookieJsonStruct.class);
        cookie=cookieJsonStruct.getCookie();
        bufferedReader.close();
        return cookie;
    }
    public static void initialization(){
        System.out.println("初始化");

        try {
            if(!cookieJson.getParentFile().exists()){
                cookieJson.getParentFile().mkdir();
            }
            if(cookieJson.exists()){
                System.out.println("cookie在本地已存在，正在测试cookie是否可用");
                try {
                    cookie=readCookieJson();
                    if(isCookieAvailable(testChapterID,cookie)){
                        System.out.println("cookie可用，将继续使用本地的cookie");
                    }
                    else{
                        System.out.println("本地cookie已不可用，将重新获取cookie");
                        cookie=getCookie(testChapterID);
                        writeCookieToJson(cookie);
                    }
                } catch (IOException e) {
                    System.out.println("读取cookie文件发生错误");
                }
            }
            if(!cookieJson.exists()){
                System.out.println("正在获取cookie");
                cookie=getCookie(testChapterID);
                writeCookieToJson(cookie);
            }
            if(configJson.exists()){
                System.out.println("config在本地已存在,正在读取config文件的数据");
                config=readConfigJson();
            }
            if(!configJson.exists()){
                System.out.println("config文件不存在，将重新创建config文件并写入默认值");
                Map<String,String> map=new HashMap<>();
                writeConfigToJson(map); //传入空的Map使其使用默认值
                config=readConfigJson();
            }
        }catch (Exception e){
            System.out.println("创建cookie和config的json文件失败，自行检查权限");
        }
        if((!config.get("proxyHost").isEmpty())&&(!config.get("proxyPort").isEmpty())){ //只有在代理host和代理port都为非空时才启用
            System.out.println("检测到已设置网络代理，将检测网络代理是否可用");
            if(isProxyAvailable(config.get("proxyHost"),Integer.valueOf(config.get("proxyPort")))){
                System.out.println("代理可用，正在将代理配置至程序全局");
                // 设置 HTTP 代理
                System.setProperty("http.proxyHost", config.get("proxyHost"));
                System.setProperty("http.proxyPort", config.get("proxyPort"));

                // 设置 HTTPS 代理
                System.setProperty("https.proxyHost", config.get("proxyHost"));
                System.setProperty("https.proxyPort", config.get("proxyPort"));
            }
            else{
                System.out.println("代理设置不可用，请重新检查代理设置");
                System.out.println("本次运行将不使用代理，如果需要使用代理请更改为有效的网络代理后重启程序");
            }
        }
    }
    public static boolean isCookieAvailable(String chapterID,String cookie){
        return chapterToString(chapterID, cookie).length() >= 200;
    }
    public static  String getCookie(String testUrl) {
        long bas = 1000000000000000000L;
        Random random = new Random();
        String cookie = "";

        long start = random.nextLong(bas * 6, bas * 8 + 1);
        for (long i = start; i < bas * 9; i++) {
            try {
                // 模拟随机延迟，范围在 50 到 150 毫秒
                int delay = random.nextInt(50, 151);
                Thread.sleep(delay);
                if(isCookieAvailable(testChapterID, String.valueOf(i))){
                    cookie= String.valueOf(i);
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return cookie;
    }
    public static String chapterToString(String chapterID,String cookie){
        try{
            Document document= Jsoup.connect(bookBaseUrl+chapterID)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .cookie("novel_web_id",cookie)
                    .get();
            document.outputSettings().charset("UTF-8");
            Elements elements=document.select("div.muye-reader-content.noselect > div > p");
            String result="";
            for(Element element : elements) {
                result += element.text() +'\n';
            }
            return strInterpreter(result, 0);
        }catch (IOException e){
            e.printStackTrace();
            return "error";
        }
    }
    public static Map<String,String> getChapters(String bookID){
        Map<String,String> result=new LinkedHashMap<>();
        try {
            Document document=Jsoup.connect(rootBaseUrl+bookID)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();
            Elements elements=document.select("div.chapter > div > a");
            System.out.println("标签数量："+elements.size());
            for(Element element:elements){
                result.put(element.text(), element.attr("href"));   //第一个值是章节数量和名字，第二个值是章节相对应的域名相对目录
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    public static String getTitle(String bookID){
        String title= "";
        try{
            Document document=Jsoup.connect(rootBaseUrl+bookID)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();
            Elements elements=document.select("div.info-name > h1");
            Element element=elements.first();
            title=element.text();
        }catch (Exception e){
            e.printStackTrace();
        }
        return title;
    }
    public static String getAuthor(String bookID){
        String author="";
        try{
            Document document=Jsoup.connect(rootBaseUrl+bookID)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();
            Elements elements=document.select("span.author-name-text");
            Element element=elements.first();
            author=element.text();
        }catch (Exception e){
            e.printStackTrace();
        }
        return author;
    }
    public static String getStatus(String bookID){
        String status="";
        try{
            Document document=Jsoup.connect(rootBaseUrl+bookID)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .get();
            Elements elements=document.select("span.info-label-yellow");
            Element element=elements.first();
            status=element.text();
        }catch (Exception e){
            e.printStackTrace();
        }
        return status;
    }
    public static void writeTotal(String bookID,String path) throws InterruptedException, IOException {
        String title=getTitle(bookID);
        String author=getAuthor(bookID);
        String status=getStatus(bookID);
        String fileName=title+".txt";
        String Path=""; //Path是文件的最终的输出路径
        Map<String,String> chapterAndContent=getChapters(bookID);
        if(path.isEmpty())
            Path+=fileName;
        else if(path.charAt(path.length()-1)!='/'||path.charAt(path.length()-1)!='\\'){
            Path+=path+'/'+fileName;
        }else{
            Path+=path+fileName;
        }
        File file=new File(Path);
        if(file.getParentFile()==null){}
        else if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        // 创建一个 FileOutputStream 对象，用于将数据写入文件
        FileOutputStream fos = new FileOutputStream(file);
        // 创建一个 OutputStreamWriter 对象，并指定字符编码为 config中存储的encoding
        OutputStreamWriter osw = new OutputStreamWriter(fos,config.get("encoding"));
        // 创建一个 BufferedWriter 对象，用于缓冲写入的数据
        BufferedWriter bufferedWriter=new BufferedWriter(osw);
        System.out.println(title);
        System.out.println(author);
        System.out.println(status);
        int chapterNumber=chapterAndContent.size();
        int currentNumber=0;
        System.out.println("共"+chapterNumber+"章");
        bufferedWriter.write(title+'\n'+"作者："+author+'\n');   //写入标题和作者
        for(Map.Entry<String,String> entry:chapterAndContent.entrySet()){
            bufferedWriter.write('\n'+entry.getKey()+'\n');  //写入章节名
            String content=chapterToString(entry.getValue().substring(8), cookie); //value的案例：/reader/6982739457606681118,从第9位开始算，也就输索引为8
            bufferedWriter.write(content);
            currentNumber++;
            System.out.println("进度："+currentNumber+'/'+chapterNumber+"    "+entry.getKey()+"    "+title);
        }
        System.out.println("正在保存");
        bufferedWriter.flush();
        bufferedWriter.close();

    }
    public static Map<String,String> readConfigJson() throws FileNotFoundException {
        Map<String,String> result=new HashMap<>();
        BufferedReader bufferedReader=new BufferedReader(new FileReader(configJson));
        Gson gson=new Gson();
        configJsonStruct configJsonStruct=gson.fromJson(bufferedReader,com.lzy12.configJsonStruct.class);
        result.put("savePath",configJsonStruct.getSavePath());
        result.put("encoding",configJsonStruct.getEncoding());
        result.put("mail",configJsonStruct.getMail());
        result.put("password",configJsonStruct.getPassword());
        result.put("smtp",configJsonStruct.getSmtp());
        result.put("mailPort", String.valueOf(configJsonStruct.getMailPort()));
        result.put("proxyHost",configJsonStruct.getProxyHost());
        result.put("proxyPort",String.valueOf(configJsonStruct.getProxyPort()));
        return result;
    }
    //如果要启动默认设置，请传入一个空的Map
    public static void writeConfigToJson(Map<String,String> configMap)  {
        try {
            Gson gson = new Gson();
            configJsonStruct configJsonStruct = new configJsonStruct();
            if (!configMap.isEmpty()) {
                String savePath = configMap.get("savePath");
                String encoding = configMap.get("encoding");
                String mail = configMap.get("mail");
                String password = configMap.get("password");
                String smtp = configMap.get("smtp");
                String mailPort = configMap.get("mailPort");
                String proxyHost = configMap.get("proxyHost");
                String proxyPort = configMap.get("proxyPort");
                configJsonStruct.setSavePath(savePath);
                configJsonStruct.setEncoding(encoding);
                configJsonStruct.setMail(mail);
                configJsonStruct.setPassword(password);
                configJsonStruct.setSmtp(smtp);
                configJsonStruct.setMailPort(Integer.valueOf(mailPort));
                configJsonStruct.setProxyHost(proxyHost);
                configJsonStruct.setProxyPort(Integer.valueOf(proxyPort));
            }
            String json = gson.toJson(configJsonStruct);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configJson));
            bufferedWriter.write(json);
            bufferedWriter.flush();
            bufferedWriter.close();
            System.out.println("写入config成功");
        }catch (Exception e){
            System.out.println("写入config失败");
        }
    }
    public static boolean isProxyAvailable(String host,int port){
        /*
         * 这里将百度作为代理host和port的检测网站，超时时间设置为5s（5000ms），超时则返回false
         * */
        int timeout=5000;
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            URL testURL=new URL("https://www.baidu.com");
            URLConnection connection= testURL.openConnection(proxy);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.connect();
            return true;
        }catch (IOException e){
            return false;
        }
    }
    //程序主菜单
    public static void mainMenu() {
        System.out.println("本程序开源，项目地址：\ngithub: https://github.com/liangzhaoyuan12/fanqie-novel-downloader\ngitee: https://gitee.com/liangzhaoyuan12/fanqie-novel-downloader");
        System.out.println("Developer: liangzhaoyuan12");
        System.out.println("请勿滥用此程序，勿用于商业用途");
        System.out.println("输入数字选择使用程序的功能");
        System.out.println("1.单本小说下载\n2.批量下载\n3.下载后发送至邮箱\n4.设置\n5.启用多线程批量下载（测试）\n6.关闭");
        int option=scanner.nextInt();
        scanner.nextLine();
        switch (option){
            case 1:
                System.out.println("请输入小说ID");
                try {
                    writeTotal(scanner.next(), config.get("savePath"));
                }catch (Exception e){
                    System.out.println("下载发生致命错误");
                }
                break;
            case 2:
                betch();
                break;
            case 3:
                sendMail();
                break;
            case 4:
                setting();
                break;
            case 5:
                betchAsync();
                break;
            case 6:
                System.exit(0);
                break;
        }
        mainMenu(); //链式调用
    }
    public static void setting(){
        System.out.println("输入数字以选择功能");
        System.out.println("1.更改保存路径\n2.更改文字编码\n3.设置邮箱\n4.设置代理\n5.返回至主菜单");
        switch (scanner.nextInt()){
            case 1:
                scanner.nextLine();
                System.out.println("请输入保存路径，如果不输入为空则下载至程序所在的目录");
                String path= scanner.nextLine();
                config.put("savePath",path);
                writeConfigToJson(config);
                break;
            case 2:
                System.out.println("请输入期望的文字编码：(输入数字)\n1.UTF-8\n2.GBK");
                switch (scanner.nextInt()){
                    case 1:
                        scanner.nextLine();
                        config.put("encoding","UTF-8");
                        break;
                    case 2:
                        scanner.nextLine();
                        config.put("encoding","GBK");
                        break;
                    default:
                        scanner.nextLine();
                        System.out.println("输入有误，请检查输入");
                        break;
                }
                writeConfigToJson(config);
                break;
            case 3:
                System.out.println("请输入你的邮箱");
                config.put("mail", scanner.next());
                System.out.println("请输入你的邮箱密码/授权码");
                config.put("password", scanner.next());
                System.out.println("请输入你的邮箱的smtp邮箱发送服务器地址");
                config.put("smtp", scanner.next());
                scanner.nextLine();
                System.out.println("请输入发送邮件的端口(默认587，不输入则为默认)，请不要随意改动");
                String mailPort= scanner.nextLine();
                if(!mailPort.isEmpty()){
                    config.put("mailPort",mailPort);
                }
                writeConfigToJson(config);
                break;
            case 4:
                System.out.println("请输入你的代理Host/IP");
                config.put("proxyHost", scanner.next());
                System.out.println("请输入你的代理的Host/IP的远程端口");
                config.put("proxyPort", scanner.next());
                writeConfigToJson(config);
                if((!config.get("proxyHost").isEmpty())&&(!(config.get("proxyPort") =="0"))) { //只有在代理host和代理port都为非空时才启用
                    System.out.println("检测到已设置网络代理，将检测网络代理是否可用");
                    if (isProxyAvailable(config.get("proxyHost"), Integer.valueOf(config.get("proxyPort")))) {
                        System.out.println("代理可用，正在将代理配置至程序全局");
                        // 设置 HTTP 代理
                        System.setProperty("http.proxyHost", config.get("proxyHost"));
                        System.setProperty("http.proxyPort", config.get("proxyPort"));

                        // 设置 HTTPS 代理
                        System.setProperty("https.proxyHost", config.get("proxyHost"));
                        System.setProperty("https.proxyPort", config.get("proxyPort"));
                    } else {
                        System.out.println("代理设置不可用，请重新检查代理设置");
                        System.out.println("本次运行将不使用代理，如果需要使用代理请更改为有效的网络代理后重启程序");
                        config.put("proxyHost", "");
                        config.put("proxyPort", "0");
                        writeConfigToJson(config);
                    }
                }
                break;
            case 5:
                break;
            default:
                System.out.println("输入错误，返回至主菜单");
                break;
        }
    }
    public static void betch(){
        betch(config.get("savePath"));
    }
    public static void betch(String path){
        File file=new File("idList.txt");
        System.out.println("请在打开的txt文件内输入小说的id，一行一个");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("创建idList.txt文件失败\n可尝试手动在当前目录创建idList.txt");
            }
        }
        if(Desktop.isDesktopSupported()){
            Desktop desktop=Desktop.getDesktop();
            try {
                desktop.open(file);
            } catch (IOException e) {
                System.out.println("打开idList.txt文件失败\n可尝试手动打开idList.txt");
            }
        }else {
            System.out.println("当前系统桌面不支持java.awt.Desktop\n请手动打开当前目录下的idList.txt");
        }
        System.out.println("输入完成后，回车继续");
        scanner.nextLine();
        System.out.println("开始下载");
        try {
            BufferedReader bufferedReader=new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine())!=null){
                writeTotal(line,path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void betchAsync(){
        betchAsync(config.get("savePath"));
    }
    public static void betchAsync(String path){
        File file=new File("idList.txt");
        System.out.println("请设置允许同时下载数量的最大限制数量(输入数字)\n请合理设置，避免被服务器拉黑\n建议在本程序设置内设置网络代理服务");
        int n= scanner.nextInt();
        scanner.nextLine();
        ThreadPoolExecutor threadPool=new ThreadPoolExecutor(n,n,0L,TimeUnit.MILLISECONDS,new LinkedBlockingDeque<>());
        System.out.println("请在打开的txt文件内输入小说的id，一行一个");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("创建idList.txt文件失败\n可尝试手动在当前目录创建idList.txt");
            }
        }
        if(Desktop.isDesktopSupported()){
            Desktop desktop=Desktop.getDesktop();
            try {
                desktop.open(file);
            } catch (IOException e) {
                System.out.println("打开idList.txt文件失败\n可尝试手动打开idList.txt");
            }
        }else {
            System.out.println("当前系统桌面不支持java.awt.Desktop\n请手动打开当前目录下的idList.txt");
        }
        System.out.println("输入完成后，回车继续");
        scanner.nextLine();
        System.out.println("开始下载");
        try {
            BufferedReader bufferedReader=new BufferedReader(new FileReader(file));
            String line;
            while ((line=bufferedReader.readLine())!=null){
                String finalLine = line;
                threadPool.submit(
                        () ->{
                            try {
                                writeTotal(finalLine,path);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
            System.out.println("所有线程均已执行完毕");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean sendMail(){
        if(config.get("mail").isEmpty()||config.get("smtp").isEmpty()||config.get("mailPort").isEmpty()){
            System.out.println("邮箱设置未设置或存在问题");
            System.out.println("将返回主菜单");
            return false;
        }
        System.out.println("请输入收件人邮箱");
        String recipient= scanner.next();
        // 获取系统属性
        Properties properties = new Properties();

        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", config.get("smtp"));
        properties.put("mail.smtp.port",config.get("mailPort")); // 通常使用587端口
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // 启用TLS
        properties.put("mail.smtp.ssl.enable", "true"); //启用ssl
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.get("mail"), config.get("password")); // 发件人邮箱和密码
            }
        });
        System.out.println("是否开启多线程\n1.开启\n2.不开启");
        switch (scanner.nextInt()){
            case 1:
                scanner.nextLine();
                System.out.println("开启多线程下载");
                betchAsync("mailOutput");
                break;
            case 2:
                scanner.nextLine();
                System.out.println("开启单线程下载");
                betch("mailOutput");
                break;
            default:
                scanner.nextLine();
                System.out.println("输入错误");
                return false;
//                break;
        }
        System.out.println("所有任务下载完成，现在执行发送邮件任务");
        File folder=new File("mailOutput");
        File[] files=folder.listFiles();
        try {
            int count=0;
            for (File file : files) {
                // 创建默认的MimeMessage对象
                MimeMessage message = new MimeMessage(session);

                // 设置发件人
                message.setFrom(new InternetAddress(config.get("mail")));

                // 设置收件人
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

                // 设置邮件主题
                message.setSubject("文本发送");

                // 创建一个多部分消息
                Multipart multipart = new MimeMultipart();

                // 创建邮件正文部分
                BodyPart messageBodyPart = new MimeBodyPart();
                // 设置邮件正文内容
                messageBodyPart.setText("这是一封带有附件的邮件。");
                // 将正文部分添加到多部分消息中
                multipart.addBodyPart(messageBodyPart);

                // 创建附件部分
                messageBodyPart = new MimeBodyPart();
                // 附件文件路径，存在于for循环
                // 设置附件文件
                ((MimeBodyPart) messageBodyPart).attachFile(file);
                // 将附件部分添加到多部分消息中
                multipart.addBodyPart(messageBodyPart);

                // 将多部分消息设置到邮件消息中
                message.setContent(multipart);

                // 发送邮件
                Transport.send(message);
                count++;
                System.out.println("成功发送第"+count+"条邮件");
            }
            System.out.println("所有内容发送完成");
            deleteFolder(folder);    //发送完自动删除
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }
    public static void deleteFolder(File folder){
        if(folder.isFile()){
            System.out.println("这个是一个文件，不适宜调用此函数");
            return;
        }
        File[] files=folder.listFiles();
        if (files != null) {
            for(File file:files){
                if(file.isDirectory()){
                    deleteFolder(file); //递归
                }else{
                    file.delete();
                }
            }
        }
    }
    private static final int[][] CODE = {{58344, 58715}, {58345, 58716}};
    private static final List<List<String>> charset = Arrays.asList(
            Arrays.asList("D", "在", "主", "特", "家", "军", "然", "表", "场", "4", "要", "只", "v", "和", "?", "6", "别", "还", "g", "现", "儿", "岁", "?", "?", "此", "象", "月", "3", "出", "战", "工", "相", "o", "男", "直", "失", "世", "F", "都", "平", "文", "什", "V", "O", "将", "真", "T", "那", "当", "?", "会", "立", "些", "u", "是", "十", "张", "学", "气", "大", "爱", "两", "命", "全", "后", "东", "性", "通", "被", "1", "它", "乐", "接", "而", "感", "车", "山", "公", "了", "常", "以", "何", "可", "话", "先", "p", "i", "叫", "轻", "M", "士", "w", "着", "变", "尔", "快", "l", "个", "说", "少", "色", "里", "安", "花", "远", "7", "难", "师", "放", "t", "报", "认", "面", "道", "S", "?", "克", "地", "度", "I", "好", "机", "U", "民", "写", "把", "万", "同", "水", "新", "没", "书", "电", "吃", "像", "斯", "5", "为", "y", "白", "几", "日", "教", "看", "但", "第", "加", "候", "作", "上", "拉", "住", "有", "法", "r", "事", "应", "位", "利", "你", "声", "身", "国", "问", "马", "女", "他", "Y", "比", "父", "x", "A", "H", "N", "s", "X", "边", "美", "对", "所", "金", "活", "回", "意", "到", "z", "从", "j", "知", "又", "内", "因", "点", "Q", "三", "定", "8", "R", "b", "正", "或", "夫", "向", "德", "听", "更", "?", "得", "告", "并", "本", "q", "过", "记", "L", "让", "打", "f", "人", "就", "者", "去", "原", "满", "体", "做", "经", "K", "走", "如", "孩", "c", "G", "给", "使", "物", "?", "最", "笑", "部", "?", "员", "等", "受", "k", "行", "一", "条", "果", "动", "光", "门", "头", "见", "往", "自", "解", "成", "处", "天", "能", "于", "名", "其", "发", "总", "母", "的", "死", "手", "入", "路", "进", "心", "来", "h", "时", "力", "多", "开", "已", "许", "d", "至", "由", "很", "界", "n", "小", "与", "Z", "想", "代", "么", "分", "生", "口", "再", "妈", "望", "次", "西", "风", "种", "带", "J", "?", "实", "情", "才", "这", "?", "E", "我", "神", "格", "长", "觉", "间", "年", "眼", "无", "不", "亲", "关", "结", "0", "友", "信", "下", "却", "重", "己", "老", "2", "音", "字", "m", "呢", "明", "之", "前", "高", "P", "B", "目", "太", "e", "9", "起", "稜", "她", "也", "W", "用", "方", "子", "英", "每", "理", "便", "四", "数", "期", "中", "C", "外", "样", "a", "海", "们", "任"),
            Arrays.asList("s", "?", "作", "口", "在", "他", "能", "并", "B", "士", "4", "U", "克", "才", "正", "们", "字", "声", "高", "全", "尔", "活", "者", "动", "其", "主", "报", "多", "望", "放", "h", "w", "次", "年", "?", "中", "3", "特", "于", "十", "入", "要", "男", "同", "G", "面", "分", "方", "K", "什", "再", "教", "本", "己", "结", "1", "等", "世", "N", "?", "说", "g", "u", "期", "Z", "外", "美", "M", "行", "给", "9", "文", "将", "两", "许", "张", "友", "0", "英", "应", "向", "像", "此", "白", "安", "少", "何", "打", "气", "常", "定", "间", "花", "见", "孩", "它", "直", "风", "数", "使", "道", "第", "水", "已", "女", "山", "解", "d", "P", "的", "通", "关", "性", "叫", "儿", "L", "妈", "问", "回", "神", "来", "S", "", "四", "望", "前", "国", "些", "O", "v", "l", "A", "心", "平", "自", "无", "军", "光", "代", "是", "好", "却", "c", "得", "种", "就", "意", "先", "立", "z", "子", "过", "Y", "j", "表", "", "么", "所", "接", "了", "名", "金", "受", "J", "满", "眼", "没", "部", "那", "m", "每", "车", "度", "可", "R", "斯", "经", "现", "门", "明", "V", "如", "走", "命", "y", "6", "E", "战", "很", "上", "f", "月", "西", "7", "长", "夫", "想", "话", "变", "海", "机", "x", "到", "W", "一", "成", "生", "信", "笑", "但", "父", "开", "内", "东", "马", "日", "小", "而", "后", "带", "以", "三", "几", "为", "认", "X", "死", "员", "目", "位", "之", "学", "远", "人", "音", "呢", "我", "q", "乐", "象", "重", "对", "个", "被", "别", "F", "也", "书", "稜", "D", "写", "还", "因", "家", "发", "时", "i", "或", "住", "德", "当", "o", "l", "比", "觉", "然", "吃", "去", "公", "a", "老", "亲", "情", "体", "太", "b", "万", "C", "电", "理", "?", "失", "力", "更", "拉", "物", "着", "原", "她", "工", "实", "色", "感", "记", "看", "出", "相", "路", "大", "你", "候", "2", "和", "?", "与", "p", "样", "新", "只", "便", "最", "不", "进", "T", "r", "做", "格", "母", "总", "爱", "身", "师", "轻", "知", "往", "加", "从", "?", "天", "e", "H", "?", "听", "场", "由", "快", "边", "让", "把", "任", "8", "条", "头", "事", "至", "起", "点", "真", "手", "这", "难", "都", "界", "用", "法", "n", "处", "下", "又", "Q", "告", "地", "5", "k", "t", "岁", "有", "会", "果", "利", "民")
    );

    // 解码主函数
    public static String strInterpreter(String n, int mode) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            int uni = n.charAt(i);
            if (CODE[mode][0] <= uni && uni <= CODE[mode][1]) {
                s.append(interpreter(uni, mode));
            } else {
                s.append(n.charAt(i));
            }
        }
        return s.toString();
    }

    // 单个字符解码函数
    private static String interpreter(int uni, int mode) {
        int bias = uni - CODE[mode][0];
        if (bias < 0 || bias >= charset.get(mode).size() || charset.get(mode).get(bias).equals("?")) {
            return String.valueOf((char) uni);
        }
        return charset.get(mode).get(bias);
    }
}
class cookieJsonStruct{
    private String novel_web_id;

    public cookieJsonStruct(String cookie) {
        this.novel_web_id = cookie;
    }
    public String getCookie() {
        return novel_web_id;
    }

    public void setCookie(String cookie) {
        this.novel_web_id = cookie;
    }
}
class configJsonStruct{
    private String savePath="";    //如果空则为保存在当前目录


    private String encoding="UTF-8";    //选择编码为UTF-8或GBK
    private String mail="";    //这里设置发送邮件的邮箱
    private String password="";    //这里设置邮箱的授权码或者密码
    private String smtp="";    //这里设置邮箱发送的服务器地址
    private int mailPort=587;   //设置发送邮件的端口，默认为587，非必要不改动
    private String proxyHost="";
    private int proxyPort=0;
    public configJsonStruct(){
        //启用默认设置
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmtp() {
        return smtp;
    }

    public void setSmtp(String smtp) {
        this.smtp = smtp;
    }

    public int getMailPort() {
        return mailPort;
    }

    public void setMailPort(int mailPort) {
        this.mailPort = mailPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
}
