package io.renren.utils;

import io.renren.entity.ColumnEntity;
import io.renren.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author ： chenshun
 * @email sunlightcs@gmail.com
 * @date : 2016年12月19日 下午11:40:24
 */
public class GenUtils {

    /**
     * 获取默认生成的模板
     *
     * @return
     */
    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();

        // domain
        templates.add("custemplate/backend/server/Dao.xml.vm");
        templates.add("custemplate/backend/server/Dao.java.vm");
        templates.add("custemplate/backend/server/Service.java.vm");
        templates.add("custemplate/backend/server/ServiceImpl.java.vm");
        //templates.add("custemplate/backend/server/Controller.java.vm");
        templates.add("custemplate/backend/server/Bean.java.vm");
        // templates.add("custemplate/backend/server/Provider.java.vm");

        // base
        //templates.add("custemplate/backend/facade/Dto.java.vm");
        //templates.add("custemplate/backend/facade/Facade.java.vm");

        // ui
       /* templates.add("custemplate/ui/List.js.vm");
        templates.add("custemplate/ui/List.less.vm");
        templates.add("custemplate/ui/model.js.vm");
        templates.add("custemplate/ui/EditModel.js.vm");
        templates.add("custemplate/ui/service.js.vm");
        templates.add("custemplate/ui/table.js.vm");*/

        return templates;
    }

    /**
     * 生成代码
     *
     * @param generatorConfig 生成代码配置
     * @param table           表信息
     * @param columns         列信息
     * @param zip             输出zip
     */
    public static void generatorCode(Map<String, Object> generatorConfig, Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        VelocityContext context = getContext(generatorConfig, table, columns);
        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            renderTemplate(context, template, zip);
        }
    }

    /**
     * 生成代码
     *
     * @param template        模板名称
     * @param generatorConfig 生成代码配置
     * @param table           表信息
     * @param columns         列信息
     * @param zip             输出zip
     */
    public static void generatorCode(String template, Map<String, Object> generatorConfig, Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        VelocityContext context = getContext(generatorConfig, table, columns);
        //渲染模板
        renderTemplate(context, template, zip);
    }

    /**
     * 获取访问路径
     *
     * @param tableName
     * @return
     */
    public static String getRestPath(String tableName) {
        Configuration config = getConfig();
        //表名转换成Java类名小写
        String className = tableToJava(tableName, config.getString("tablePrefix")).toLowerCase();
        String restPrePath = config.getString("restPrePath");
        // String moduleName = config.getString("moduleName");
        return restPrePath + "/" + className;
    }

    /**
     * 获取context
     *
     * @param generatorConfig 生成代码配置
     * @param table           表信息
     * @param columns         列信息
     * @return
     */
    private static VelocityContext getContext(Map<String, Object> generatorConfig, Map<String, String> table, List<Map<String, String>> columns) {
        //配置信息
        Configuration config = getConfig();
        Configuration mybatisConfig = getMyBatisConfig();
        Configuration javaTypeConfig = getJavaTypeConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            String dataType = mybatisConfig.getString(column.get("dataType"), "unknowType");
            columnEntity.setDataType(dataType);
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));
            Object len = column.get("columnLength");
            columnEntity.setColumnLength(len.toString());
            columnEntity.setNullable(column.get("isNullable"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(column.get("dataType"), "unknowType");
            columnEntity.setAttrType(attrType);
            columnEntity.setJavaType(javaTypeConfig.getString(attrType, attrType));
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("package", config.getString("package"));
        // 模块名
        String moduleName = (String) generatorConfig.get("moduleName");
        if (StringUtils.isBlank(moduleName)) {
            moduleName = config.getString("moduleName");
        }
        moduleName = moduleName.replaceAll("/", ".");
        String moduleFileName = moduleName.replaceAll("\\.", "/");
        map.put("moduleName", moduleName);
        map.put("moduleFileName", moduleFileName);
        map.put("restPrePath", config.getString("restPrePath"));

        // 请求地址
        String restPath = (String) generatorConfig.get("restPath");
        if (StringUtils.isBlank(restPath)) {
            restPath = getRestPath(table.get("tableName"));
        }
        map.put("restPath", restPath);

        map.put("appName", config.getString("appName"));
        map.put("author", config.getString("author"));
        map.put("version", config.getString("version"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        return new VelocityContext(map);
    }

    /**
     * 渲染模板并输出到zip
     *
     * @param context  环境变量
     * @param template 模板
     * @param zip      输出zip
     */
    private static void renderTemplate(VelocityContext context, String template, ZipOutputStream zip) {
        //渲染模板
        StringWriter sw = new StringWriter();
        Template tpl = Velocity.getTemplate(template, "UTF-8");
        tpl.merge(context, sw);
        try {

            //添加到zip
            zip.putNextEntry(new ZipEntry(getFileName(template,
                    context.internalGet("className").toString(),
                    context.internalGet("package").toString(),
                    context.internalGet("appName").toString(),
                    context.internalGet("moduleFileName").toString(),
                    context.internalGet("classname").toString())));
            IOUtils.write(sw.toString(), zip, "UTF-8");
            IOUtils.closeQuietly(sw);
            zip.closeEntry();
        } catch (IOException e) {
            throw new RRException("渲染模板失败，表名：" + context.internalGet("tableName").toString(), e);
        }
    }

    /**
     * 列名转换成Java属性名
     *
     * @param columnName 列名
     * @return
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     *
     * @param tableName   表名
     * @param tablePrefix 表前缀
     * @return
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取mybatis配置信息
     */
    public static Configuration getMyBatisConfig() {
        try {
            return new PropertiesConfiguration("mybatis.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取java类型对应
     */
    public static Configuration getJavaTypeConfig() {
        try {
            return new PropertiesConfiguration("javatype.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     *
     * @param template    模板路径
     * @param className   类名称（首字母大写）
     * @param packageName 包名
     * @param appName     应用名
     * @param moduleName  模块名
     * @param classname   类名（首字母小写）
     * @return
     */
    public static String getFileName(String template, String className, String packageName, String appName, String moduleName, String classname) {
        String rootPath = appName;

        // common
        String backCommonPath = "src" + File.separator + "main" + File.separator + "java" + File.separator + "com"
                + File.separator + "yiautos" + File.separator + appName;

        // api路径
        String apiPath = rootPath + File.separator + "api" + File.separator + backCommonPath;

        // domain
        String domainPath = rootPath + File.separator + "domain" + File.separator + backCommonPath;

        // base路径
        String basePath = rootPath + File.separator + "base" + File.separator + backCommonPath;


        // bean对象
        if (template.contains("Bean.java.vm")) {
            return domainPath + File.separator + "entity" + File.separator + className + ".java";
        }

        // dao对象
        if (template.contains("Dao.java.vm")) {
            return domainPath + File.separator + "dao" + File.separator + className + "Dao.java";
        }

        // xml对象
        if (template.contains("Dao.xml.vm")) {
            return domainPath + File.separator + "dao" + File.separator + "xml" + File.separator + className + "Dao.xml";
        }

        // service对象
        if (template.contains("Service.java.vm")) {
            return domainPath + File.separator + "service" + File.separator + className + "Service.java";
        }

        // serviceImpl对象
        if (template.contains("ServiceImpl.java.vm")) {
            return domainPath + File.separator + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }

        // controller对象
       /* if (template.contains("Controller.java.vm")) {
            return apiPath + File.separator + "controller" + File.separator + className + "Controller.java";
        }*/


        // dto对象
        if (template.contains("Dto.java.vm")) {
            return basePath + File.separator + "dto" + File.separator + moduleName
                    + File.separator + className + "DTO.java";
        }

       /* // facade对象
        if (template.contains("Facade.java.vm")) {
            return backFacadePath + File.separator + "facade" + File.separator + moduleName
                    + File.separator + "I" + className + "Facade.java";
        }*/

        // model
        /*if (template.contains("model.js.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator + "src"
                    + File.separator + "model" + File.separator  + classname + ".js";
        }

        // service
        if (template.contains("service.js.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator + "src"
                    + File.separator + "service" + File.separator  + classname + ".js";
        }

        // list
        if (template.contains("List.js.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator + "src"
                    + File.separator + "page" + File.separator + className
                    + File.separator + className + "List.js";
        }

        if (template.contains("List.less.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator + "src"
                    +  File.separator + "page" + File.separator + className
                    + File.separator + className + "List.less";
        }

        // EditModel
        if (template.contains("EditModel.js.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator + "src"
                    + File.separator + "page" + File.separator + className
                    + File.separator + "components" + File.separator  + "EditorModal.js";
        }

        // table
        if (template.contains("table.js.vm" )) {
            return rootPath + File.separator + "hoox-" + appName + "-ui" + File.separator +
                    "src"  + File.separator + "page" + File.separator + className
                    + File.separator + "components" + File.separator  + "ShTable.js";
        }
*/
        if (template.contains("README.vm")) {
            return rootPath + File.separator + "README.md";
        }

        return null;
    }
}
