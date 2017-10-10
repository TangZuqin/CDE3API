package app.controllers;

import app.models.*;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;


import javax.print.DocFlavor;
import java.io.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static jdk.internal.dynalink.support.NameCodec.decode;

public class CRFSvrController extends BasicController {
    public void index() {
        respond("This is CRFSvr");
    }

    @POST
    public void delete() throws Exception {

        String ids =param("ids");
        if(ids.isEmpty()){
            output(20001,"参数异常");
        }
        Template.delete("ID in ("+ ids + ")");
        TplElement.delete("TEMPLATE_ID in ("+ ids + ")");
        output(0,"删除成功");
    }

    @POST
    public void newAllTemplates() throws Exception {
        long total = 0;
        String query = "";
        Object[] params =new Object[3];
        query = "NAME like ? or ID = ? or SUBJECT_ID = ?";
        params[0]= "%"+param("keyword")+"%";
        params[1]= param("ID");
        params[2]= param("SUBJECT_ID");
        total = Template.count(query,params);
        int pagesize = 10;
        if(null != param("pagesize")) pagesize=Integer.parseInt(param("pagesize"));
        int offset = 0;
        if(null != param("offset")) offset=(Integer.parseInt(param("p"))-1)*pagesize;
        List resList = Template.where(query,params).offset(offset).limit(pagesize).orderBy("ID DESC");
        Object[] templates= toArray(resList);
        ArrayList newNodes = new ArrayList();
        for (Object template:templates) {
            TreeMap node = (TreeMap) template;
            long TEMPLATE_NUM = TplElement.count("TEMPLATE_ID = ?",node.get("ID"));
            node.put("TEMPLATE_NUM",TEMPLATE_NUM);
            Subject subject = Subject.findById(node.get("SUBJECT_ID"));
            String SUBJECT_NAME = subject.get("SUBJECT_NAME").toString();
            String AGENCY_ID = subject.get("AGENCY_ID").toString();
            String AGENCY_NAME = Agency.findById(AGENCY_ID).get("AGENCY_NAME").toString();
            node.put("SUBJECT_NAME",SUBJECT_NAME);
            node.put("AGENCY_NAME",AGENCY_NAME);
            node.put("AGENCY_ID",AGENCY_ID);
            node.remove("HTMLSTR");
            node.remove("LOGICJSON");
            newNodes.add(node);
        }
        Map data = new HashMap();
        data.put("total",total);
        data.put("rows",newNodes);
        if(null !=param("isBoostrap")){
            output(0, newNodes, Math.ceil(total/pagesize));
        }else{
            toJSON(data);
        }
    }

    @POST
    public void newInsertOrUpdate() throws Exception {
        Map params = params1st();
        if(param("NAME").isEmpty()) output(20002, "名字不能为空");
        DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String VERSION_DATE = format.format(param("VERSION_DATE"));
        params.remove("VERSION_DATE");
        params.put("VERSION_DATE",VERSION_DATE);
        String ID = param("ID");
        if (ID==null || Integer.parseInt(ID)==0) {
            params.remove("ID");
        }
        Template m = new Template();
        m.fromMap(params);
        if (m.save()) {
            output(0, "更新成功");
        } else {
            output(20005, "更新失败");
        }
    }

    @POST
    public void htmlStr() throws Exception {
        if(null != param("id"))
        {
            output(20001, "参数异常");
        }

        int id = Integer.parseInt(param("id"));
        String fileURL = cookFileURL(id);
        String htmlStr = "";
        File file=new File(fileURL);
        if(file.exists())
        {
            htmlStr = fileToString(fileURL);
        }

        output(0, htmlStr);
    }
    private String cookFileURL(int id) throws Exception{
        String folder = "templates/"+id;
        File f=new File(folder);
        if (f.isDirectory()){
            return f+"/tpl_"+id+"_newest.html";
        }
        return "templates/templates_"+id+".html";
    }
    private String fileToString(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null)
        {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }

    @POST
    public void save() throws Exception {
        String request = decode(param("tplData"));
        System.out.println(request);
        if(null != request) output(20001, "参数异常");
        Map tplData = JsonHelper.toMap(request);
        if (null == tplData.get("id")) {
            output(20002, "参数异常,ID不能为空");
        }
        if(null != tplData.get("elesInfo")){
            Map[] eleInfos = JsonHelper.toMaps(tplData.get("elesInfo").toString());
            for(Map item :eleInfos)
            {
                TplElement tplElement = TplElement.findById(item.get("ID"));
                Map t = tplElement.toMap();
                t.remove("TINC");t.remove("SORTER");t.remove("HELE");
                t.put("TINC",item.get("TINC"));t.put("SORTER",item.get("SORTER"));t.put("HELE",item.get("HELE"));
                if(item.get("HVAL") != "") {t.remove("HVAL");t.put("HVAL",item.get("HVAL"));}
                tplElement.fromMap(t);
                tplElement.save();
            }
        }
        String id = tplData.get("id").toString();
        String userName= tplData.get("userName").toString();
        String htmlStr = tplData.get("htmlStr").toString();
        String logicJSON = ("dataJSON").toString();
        String fileURL = saveFileURL(id, userName);
        boolean resTpl = stringToFile(fileURL, htmlStr);
        if(!resTpl) output(200010, "系统错误");
        String previewStr = tplData.get("previewStr").toString();
        Template template= Template.findById(id);
        Map t = template.toMap();
        t.remove("HTMLSTR");t.remove("LOGICJSON");
        t.put("HTMLSTR",previewStr);t.put("LOGICJSON",logicJSON);
        template.fromMap(t);
        template.save();
        String previewURL = savePreviewURL(id);
        boolean resDw = stringToFile(previewURL, previewStr);
        if(resDw){
            output(0, "保存成功");
        }else{
            output(200010, "系统错误");
        }
    }
    public String  saveFileURL(String id,String userName) throws Exception
    {
        long  timestamp = System.currentTimeMillis();
        String folder = "templates/"+id;
        File f = new File(folder);
        if (!f.isDirectory())   f.mkdir();
        String newfile = folder+"/tpl_"+id+"_newest.html";
        File nf = new File(newfile);
        if (nf.isFile()){
            File renameFile =new File(folder+"/tpl_"+id+"_"+timestamp+"_"+userName+".html");
            nf.renameTo(renameFile);
        }
        return newfile;
    }
    private boolean stringToFile (String fileUrl, String htmlStr){
        try {
            OutputStreamWriter out = new OutputStreamWriter(
                    new FileOutputStream(fileUrl),"UTF-8");
            out.write(htmlStr);
            out.flush();
            out.close();
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }
    public String savePreviewURL(String id)
    {
        String folder = "templates/"+id;
        File f=new File(folder);
        if (!f.isDirectory()){
            f.mkdir();
        }
        String  file = folder+"/public_"+id+".html";
        return file;
    }

}
