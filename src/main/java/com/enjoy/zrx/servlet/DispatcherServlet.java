package com.enjoy.zrx.servlet;

import com.enjoy.zrx.annotation.*;
import com.enjoy.zrx.controller.ZrxController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {
    //保存全限定类名
    List<String> classNames = new ArrayList<String>();

    //保存容器中的所有Bean
    Map<String, Object> beans = new HashMap<String, Object>();

    //用于保存路径和方法的关系 eg: url -> method
    Map<String, Object> handerMap = new HashMap<String, Object>();



    //tomcat启动的时候实例化,将所有的bean，放到一个map中
    public void init(ServletConfig config){
        //扫描包,得到一个全限定类名list
        basePackegeScan("com.enjoy");


        //根据全限定类名来实例化类
        doInstance();

        
        //属性注入
        doAutowired();

        //建立映射关系
        doUrlMapping(); //  zrx/query   -->  method
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res){
        this.doPost(req,res);
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res){
        //拿到uri
        String uri = req.getRequestURI(); // eg: /zrx-mvc/zrx/query

        //将uri中的前面的多部分切除， eg：  /zrx-mvc/zrx/query  -->  /zrx/query
        String context = req.getContextPath();  // context = "/zrx-mvc"
        String path = uri.replace(context,""); // path = "/zrx/query";

        //拿到需要执行的方法
        Method method = (Method)handerMap.get(path);

        //拿到要调用的controller
        ZrxController controller =  (ZrxController)beans.get("/"+path.split("/")[1]);

        //拿到执行的所有参数，这个hand方法应用了策略模式
        Object[] args = hand(req,res,method);

        //根据参数调用controller进行执行
        try {
            method.invoke(controller,args);
        } catch (Exception e) {
        }
    }



    //使用策略模式得到参数
    private Object[] hand(HttpServletRequest req, HttpServletResponse res, Method method) {
        //拿到当前待执行的方法有哪些参数
        Class<?>[] paramClazzs = method.getParameterTypes();

        //根据参数的个数，new 一个参数的数组，将方法里的所有参数复制到args来
        Object[] args = new Object[paramClazzs.length];

        int args_i = 0;
        int index = 0;
        for(Class<?> paramClazz : paramClazzs){
            if (ServletRequest.class.isAssignableFrom(paramClazz)){
                args[args_i++] = req;
            }
            if (ServletResponse.class.isAssignableFrom(paramClazz)){
                args[args_i++] = res;
            }
            //从0 - 3判断有没有RequestParam注解，很明显paramClazz为0和1时，不是
            //当为2和3时为@RequestParam，需要解析
            Annotation[] paramAns = method.getParameterAnnotations()[index];
            if (paramAns.length > 0){
                for (Annotation paramAn : paramAns){
                    if (EnjoyRequestParam.class.isAssignableFrom(paramAn.getClass())){
                        EnjoyRequestParam rp = (EnjoyRequestParam)paramAn;

                        //找到注解里的name 和 age
                        args[args_i++] = req.getParameter(rp.value());
                    }
                }
            }
            index++;
        }
        return args;
    }


    private void doUrlMapping() {

        //同样遍历ioc容器，只需要处理控制类即可
        for (Map.Entry<String, Object> entry : beans.entrySet()) {

            //得到ioc容器中的对象
            Object instance = entry.getValue();

            //根据对象获取Class类
            Class<?> clazz = instance.getClass();

            //如果是控制类则处理，否则continue
            if (clazz.isAnnotationPresent(EnjoyController.class)) {
                //控制类,先拿类路径，再拿方法的路径，拼起来

                EnjoyRequestMapping mappingClass = clazz.getAnnotation(EnjoyRequestMapping.class);

                //拿到类上的路径
                String classPath = mappingClass.value();  //eg: zrx

                Method[] methods = clazz.getMethods();
                //遍历，可能有多个方法，然后有的方法没有加注解,需要判断哪些方法上加了注解
                for(Method method : methods){
                    //方法上有RequestMapping注解
                    if (method.isAnnotationPresent(EnjoyRequestMapping.class)){
                        EnjoyRequestMapping mappingMethod = method.getAnnotation(EnjoyRequestMapping.class);
                        //拿到方法上请求的注解值
                        String methodPath = mappingMethod.value(); //eg: /query

                        //将请求路径拼起来
                        String requestPath = classPath + methodPath; // eg: /zrx/query
                        handerMap.put(requestPath,method);
                    }else {
                        continue;
                    }
                }
            }else {
                continue;
            }
        }
    }

    private void doAutowired() {
        //先得到clazz，然后得到method，添加
        for (Map.Entry<String, Object> entry : beans.entrySet()){

            //得到ioc容器中的对象
            Object instance = entry.getValue();

            //根据对象获取Class类
            Class<?> clazz = instance.getClass();

            //根据不同的类来注入
            if (clazz.isAnnotationPresent(EnjoyController.class)){
                //控制类

                Field[] fields = clazz.getDeclaredFields();

                for(Field field : fields){

                    //判断这个注解类的成员变量下面是否还有注解
                    if (field.isAnnotationPresent(EnjoyAutowired.class)){
                        //成员变量有注解AutoWired
                        EnjoyAutowired auto = field.getAnnotation(EnjoyAutowired.class);

                        String key = auto.value();//这个key类似 ZrxServiceImpl

                        //拿到一个实例
                        Object bean = beans.get(key);


                        //注入
                        field.setAccessible(true); //打开权限
                        try {
                            field.set(instance,bean);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }else {
                        continue;
                    }
                }
            }else {
                continue;
            }
        }
    }

    private void doInstance() {
        for(String className : classNames){
            // 把com.enjoy....ZrxService.class中的.class去除
            String cn = className.replace(".class","");

            //实例化对象
            try {
                Class<?> clazz= Class.forName(cn);

                //判断各种类是什么类
                if (clazz.isAnnotationPresent(EnjoyController.class)){
                    //是控制类

                    Object instance = clazz.newInstance();

                    EnjoyRequestMapping mapping = clazz.getAnnotation(EnjoyRequestMapping.class);
                    String key = mapping.value();

                    //创造IOC的map
                    beans.put(key, instance);
                }else if(clazz.isAnnotationPresent(EnjoyService.class)){
                    //是服务类
                    Object instance = clazz.newInstance();

                    EnjoyService service = clazz.getAnnotation(EnjoyService.class);
                    String key = service.value();

                    //创造IOC的map
                    beans.put(key, instance);
                }else{
                    continue;
                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

        }
    }

    private void basePackegeScan(String basePackege) {
        //扫描编译好的类路径  ...class
        URL url = this.getClass().getClassLoader().getResource("/"+basePackege.replaceAll("\\.","/"));

        //拿到路径后， url = E:/WORK/COM/ENJOY
        String fileStr = url.getFile();
        File file = new File(fileStr);

        String[] filesStr = file.list();
        for (String path : filesStr){
            File filePath = new File(fileStr+path);
            if (filePath.isDirectory()){
                //是文件夹,递归
                basePackegeScan(basePackege + "." + path);
            }else{
                //拿到com.enjoy....ZrxService.class
                classNames.add(basePackege+"."+filePath.getName());
            }
        }
    }
}

